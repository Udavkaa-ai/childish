package ru.sovushka.core.mesh

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.time.LocalDate

/** Токен МЭШ просрочен или неверен — нужно войти заново. */
class MeshAuthException(message: String) : Exception(message)

class MeshException(message: String, cause: Throwable? = null) : Exception(message, cause)

@Serializable
data class MeshStudent(val id: Long, val personGuid: String, val name: String, val className: String = "")

@Serializable
data class MeshAccount(val profileId: Long, val role: String, val students: List<MeshStudent>)

/**
 * Клиент мобильного API «Московской электронной школы» (school.mos.ru).
 * Это неофициальный API, тот же, что использует приложение «Дневник МЭШ»;
 * протокол взят из открытого проекта OctoDiary (MIT).
 */
class MeshClient(
    private var token: String,
    private val http: OkHttpClient,
    private val school: String = "https://school.mos.ru",
    private val dnevnik: String = "https://dnevnik.mos.ru",
) {
    val currentToken: String get() = token

    private val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true; explicitNulls = false }

    // --- DTO ответов сервера (только нужные поля) ---
    @Serializable private data class ProfileInfo(val id: Long, val type: String = "")
    @Serializable private data class School(val name: String? = null, val short_name: String? = null)
    @Serializable private data class Person(
        val id: Long? = null,
        val first_name: String? = null,
        val last_name: String? = null,
        val class_name: String? = null,
        val contingent_guid: String? = null,
        val type: String? = null,
    )
    @Serializable private data class FamilyProfile(val profile: Person? = null, val children: List<Person> = emptyList())
    @Serializable private data class EvHomework(val descriptions: List<String> = emptyList())
    @Serializable private data class EvMark(val value: String? = null)
    @Serializable private data class EvItem(
        val start_at: String? = null,
        val finish_at: String? = null,
        val subject_name: String? = null,
        val room_number: String? = null,
        val room_name: String? = null,
        val lesson_theme: String? = null,
        val lesson_name: String? = null,
        val source: String? = null,
        val cancelled: Boolean? = null,
        val homework: EvHomework? = null,
        val marks: List<EvMark>? = null,
    )
    @Serializable private data class Events(val response: List<EvItem> = emptyList())
    @Serializable private data class MarkItem(
        val value: String? = null,
        val weight: Int? = null,
        val control_form_name: String? = null,
        val comment: String? = null,
        val date: String? = null,
        val subject_name: String? = null,
    )
    @Serializable private data class Marks(val payload: List<MarkItem> = emptyList())
    @Serializable private data class ShortMark(val value: String? = null)
    @Serializable private data class SubjectShort(val subject_name: String? = null, val average: String? = null, val marks: List<ShortMark>? = null)
    @Serializable private data class SubjectsShort(val payload: List<SubjectShort> = emptyList())
    @Serializable private data class HwItem(val description: String? = null, val subject_name: String? = null, val date: String? = null, val is_done: Boolean? = null)
    @Serializable private data class Homeworks(val payload: List<HwItem> = emptyList())

    private fun request(url: String, params: Map<String, String> = emptyMap(), headers: Map<String, String> = emptyMap()): Request {
        val u = url.toHttpUrl().newBuilder().apply { params.forEach { (k, v) -> addQueryParameter(k, v) } }.build()
        return Request.Builder().url(u)
            .header("Authorization", "Bearer $token")
            .header("Auth-Token", token)
            .header("Cookie", "aupd_token=$token; aupdtoken=$token")
            .header("Accept", "application/json, text/plain, */*")
            .header("User-Agent", USER_AGENT)
            .apply { headers.forEach { (k, v) -> header(k, v) } }
            .get().build()
    }

    private suspend fun get(req: Request): String = withContext(Dispatchers.IO) {
        try {
            http.newCall(req).execute().use { r ->
                val body = r.body?.string().orEmpty()
                when {
                    r.code == 401 || r.code == 403 -> throw MeshAuthException("МЭШ: нужно войти заново (${r.code})")
                    !r.isSuccessful -> throw MeshException("МЭШ: ошибка ${r.code} ${body.take(200)}")
                    else -> body
                }
            }
        } catch (e: IOException) {
            throw MeshException("МЭШ: нет связи с сервером (${e.message})", e)
        }
    }

    private inline fun <reified T> parse(body: String): T = try {
        json.decodeFromString<T>(body)
    } catch (e: Exception) {
        throw MeshException("МЭШ: неожиданный ответ сервера: ${body.take(200)}", e)
    }

    private fun family(profileId: Long) = mapOf(
        "x-mes-subsystem" to "familymp",
        "client-type" to "diary-mobile",
        "profile-id" to profileId.toString(),
    )

    /** Обновить токен. Возвращает новый токен или null, если сервер отказал. */
    suspend fun refresh(): String? = try {
        val t = get(request("$school/v2/token/refresh")).trim().trim('"')
        if (t.count { it == '.' } == 2) t.also { token = it } else null
    } catch (e: Exception) {
        null
    }

    suspend fun account(): MeshAccount {
        val infos = parse<List<ProfileInfo>>(get(request("$dnevnik/acl/api/users/profile_info", headers = mapOf("partner-source-id" to "MOBILE"))))
        val info = infos.firstOrNull() ?: throw MeshException("МЭШ: у аккаунта нет профиля")
        val fp = parse<FamilyProfile>(get(request("$school/api/family/mobile/v1/profile", headers = family(info.id))))
        val people = fp.children.ifEmpty { listOfNotNull(fp.profile) }
        val students = people.mapNotNull { p ->
            val id = p.id ?: return@mapNotNull null
            MeshStudent(
                id = id,
                personGuid = p.contingent_guid.orEmpty(),
                name = listOfNotNull(p.first_name, p.last_name).joinToString(" ").ifBlank { "Ученик $id" },
                className = p.class_name.orEmpty(),
            )
        }
        if (students.isEmpty()) throw MeshException("МЭШ: в профиле не найдено учеников")
        return MeshAccount(info.id, info.type.ifBlank { fp.profile?.type.orEmpty() }, students)
    }

    /** Загрузить расписание, оценки, средние баллы и домашние задания. */
    suspend fun snapshot(
        account: MeshAccount,
        student: MeshStudent,
        today: LocalDate,
        now: Long = System.currentTimeMillis(),
    ): DiarySnapshot {
        val scheduleFrom = today.minusDays(7).toString()
        val scheduleTo = today.plusDays(14).toString()
        val marksFrom = today.minusDays(60).toString()
        val h = family(account.profileId)

        val lessons = if (student.personGuid.isNotBlank()) {
            val ev = parse<Events>(get(request(
                "$school/api/eventcalendar/v1/api/events",
                mapOf("person_ids" to student.personGuid, "begin_date" to scheduleFrom, "end_date" to scheduleTo, "expand" to "marks,homework"),
                h + ("x-mes-role" to account.role.ifBlank { "parent" }),
            )))
            ev.response.filter { !it.subject_name.isNullOrBlank() && !it.start_at.isNullOrBlank() }.map {
                DiaryLesson(
                    start = it.start_at!!,
                    end = it.finish_at.orEmpty(),
                    subject = it.subject_name!!,
                    room = (it.room_number ?: it.room_name).orEmpty(),
                    theme = (it.lesson_theme ?: it.lesson_name).orEmpty(),
                    homework = it.homework?.descriptions.orEmpty().filter { d -> d.isNotBlank() },
                    marks = it.marks.orEmpty().mapNotNull { m -> m.value },
                    cancelled = it.cancelled == true,
                )
            }.sortedBy { it.start }
        } else emptyList()

        val marks = parse<Marks>(get(request(
            "$school/api/family/mobile/v1/marks",
            mapOf("student_id" to student.id.toString(), "from" to marksFrom, "to" to today.toString()),
            h,
        ))).payload.mapNotNull {
            DiaryMark(
                date = it.date?.take(10) ?: return@mapNotNull null,
                subject = it.subject_name ?: return@mapNotNull null,
                value = it.value ?: return@mapNotNull null,
                weight = it.weight ?: 1,
                form = it.control_form_name.orEmpty(),
                comment = it.comment.orEmpty(),
            )
        }

        // Средние и домашка — дополнительные данные: если сервер их не отдал, расписание и оценки всё равно сохраним.
        val averages = runCatching {
            parse<SubjectsShort>(get(request("$school/api/family/mobile/v1/subject_marks/short", mapOf("student_id" to student.id.toString()), h)))
                .payload.mapNotNull { s ->
                    SubjectAverage(s.subject_name ?: return@mapNotNull null, s.average.orEmpty(), s.marks.orEmpty().mapNotNull { it.value })
                }
        }.getOrElse { if (it is MeshAuthException) throw it else emptyList() }

        val homework = runCatching {
            parse<Homeworks>(get(request(
                "$school/api/family/mobile/v1/homeworks/short",
                mapOf("student_id" to student.id.toString(), "from" to today.toString(), "to" to today.plusDays(14).toString()),
                h,
            ))).payload.mapNotNull {
                DiaryHomework(
                    date = it.date?.take(10) ?: return@mapNotNull null,
                    subject = it.subject_name ?: return@mapNotNull null,
                    text = it.description.orEmpty(),
                    done = it.is_done == true,
                )
            }
        }.getOrElse { if (it is MeshAuthException) throw it else emptyList() }

        return DiarySnapshot(
            studentName = student.name,
            className = student.className,
            syncedAt = now,
            lessons = lessons,
            marks = marks.sortedByDescending { it.date },
            averages = averages,
            homework = homework,
        )
    }

    companion object {
        const val LOGIN_URL = "https://school.mos.ru/"
        const val TOKEN_COOKIE = "aupd_token"
        const val USER_AGENT = "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36"

        /** Срок действия токена (мс с 1970 г.) из поля exp JWT, или null, если не удалось прочитать. */
        fun tokenExpiry(token: String): Long? = runCatching {
            val payload = token.trim().split('.')[1]
            val text = String(java.util.Base64.getUrlDecoder().decode(payload.padEnd((payload.length + 3) / 4 * 4, '=')))
            Regex("\"exp\"\\s*:\\s*(\\d+)").find(text)!!.groupValues[1].toLong() * 1000
        }.getOrNull()

        /** Похоже ли на токен МЭШ (JWT из трёх частей). */
        fun looksLikeToken(t: String): Boolean = t.trim().split('.').let { it.size == 3 && it.all { p -> p.length > 4 } }

        /** Выбрать ученика: сначала по имени, иначе первого. */
        fun pickStudent(account: MeshAccount, preferredId: Long?, preferredName: String): MeshStudent =
            account.students.firstOrNull { it.id == preferredId }
                ?: account.students.firstOrNull { preferredName.isNotBlank() && it.name.contains(preferredName, ignoreCase = true) }
                ?: account.students.first()
    }
}
