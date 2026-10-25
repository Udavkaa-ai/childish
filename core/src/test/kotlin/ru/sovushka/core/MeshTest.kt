package ru.sovushka.core

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import ru.sovushka.core.ai.defaultHttpClient
import ru.sovushka.core.mesh.DiaryInsights
import ru.sovushka.core.mesh.MeshAuthException
import ru.sovushka.core.mesh.MeshClient
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class MeshTest {
    private fun server(routes: Map<String, String>, code: Int = 200) = MockWebServer().apply {
        dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                if (code != 200) return MockResponse().setResponseCode(code)
                val path = request.requestUrl!!.encodedPath
                return routes[path]?.let { MockResponse().setBody(it) } ?: MockResponse().setResponseCode(404)
            }
        }
    }

    @Test
    fun loadsAccountAndSnapshot() = runBlocking {
        val routes = mapOf(
            "/acl/api/users/profile_info" to """[{"id":777,"type":"parent","extra":1}]""",
            "/api/family/mobile/v1/profile" to """{"profile":{"id":777,"type":"parent"},"children":[
                {"id":11,"first_name":"Алина","last_name":"И","class_name":null,"contingent_guid":"g-a"},
                {"id":12,"first_name":"Вероника","last_name":"И","class_name":"4-Б","contingent_guid":"g-v"}]}""",
            "/api/eventcalendar/v1/api/events" to """{"total_count":2,"response":[
                {"start_at":"2026-10-26T09:30:00+03:00","finish_at":"2026-10-26T10:15:00+03:00","subject_name":"Английский язык","room_number":12,
                 "lesson_theme":"My family","source":"PLAN","homework":{"descriptions":["стр. 45 упр. 3, выучить слова"]},"marks":[{"value":"4"}]},
                {"start_at":"2026-10-26T08:30:00+03:00","subject_name":"Математика","source":"PLAN"},
                {"start_at":"2026-10-27T00:00:00","title":"День рождения","source":"EC"}]}""",
            "/api/family/mobile/v1/marks" to """{"payload":[{"value":"3","weight":2,"date":"2026-10-20","subject_name":"Английский язык","control_form_name":"Диктант"},
                {"value":"5","date":"2026-10-21","subject_name":"Математика"}]}""",
            "/api/family/mobile/v1/subject_marks/short" to """{"payload":[{"subject_name":"Английский язык","average":"3.40","marks":[{"value":"3"},{"value":"4"}]}]}""",
            "/api/family/mobile/v1/homeworks/short" to """{"payload":[{"description":"с. 46 читать","subject_name":"Английский язык","date":"2026-10-28","is_done":false}]}""",
        )
        server(routes).use { s ->
            val base = s.url("/").toString().trimEnd('/')
            val c = MeshClient("a.b.c", defaultHttpClient(), school = base, dnevnik = base)
            val acc = c.account()
            assertEquals(777, acc.profileId); assertEquals("parent", acc.role); assertEquals(2, acc.students.size)
            val st = MeshClient.pickStudent(acc, null, "Вероника")
            assertEquals(12, st.id)
            val snap = c.snapshot(acc, st, LocalDate.of(2026, 10, 25), now = 1)
            assertEquals(listOf("Математика", "Английский язык"), snap.lessons.map { it.subject })
            val eng = snap.lessons[1]
            assertEquals("12", eng.room); assertEquals("09:30", eng.time); assertEquals(listOf("4"), eng.marks)
            assertEquals("3.40", DiaryInsights.englishAverage(snap))
            assertEquals(eng, DiaryInsights.nextEnglishLesson(snap, "2026-10-25"))
            val hw = DiaryInsights.englishHomework(snap, "2026-10-25")
            assertEquals(2, hw.size)
            assertEquals(listOf(45), DiaryInsights.pages(hw[0].text))
            assertEquals(listOf(46), DiaryInsights.pages("с. 46 читать"))
            val ctx = DiaryInsights.tutorContext(snap, "2026-10-25")
            assertTrue("3.40" in ctx && "My family" in ctx && "стр. 45" in ctx, ctx)

            val events = generateSequence { s.takeRequest(1, java.util.concurrent.TimeUnit.SECONDS) }
                .first { it.path!!.startsWith("/api/eventcalendar") }
            assertEquals("g-v", events.requestUrl!!.queryParameter("person_ids"))
            assertEquals("parent", events.getHeader("x-mes-role"))
            assertEquals("familymp", events.getHeader("x-mes-subsystem"))
            assertEquals("Bearer a.b.c", events.getHeader("Authorization"))
        }
    }

    @Test
    fun tokenHelpers() {
        val payload = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString("""{"sub":"1","exp":1893456000}""".toByteArray())
        val token = "eyJhbGciOiJSUzI1NiJ9.$payload.c2lnbmF0dXJl"
        assertEquals(1893456000000L, MeshClient.tokenExpiry(token))
        assertTrue(MeshClient.looksLikeToken(" $token "))
        assertEquals(null, MeshClient.tokenExpiry("abc"))
        assertTrue(!MeshClient.looksLikeToken("a.b"))
    }

    @Test
    fun fallsBackToWebApi() = runBlocking {
        val s = MockWebServer().apply {
            dispatcher = object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse = when (request.requestUrl!!.encodedPath) {
                    "/api/family/web/v1/profile" -> MockResponse().setBody("""{"profile":{"id":555,"type":"parent"},"children":[{"id":12,"first_name":"Вероника","class_name":"4-Б","contingent_guid":"g-v"}]}""")
                    "/api/eventcalendar/v1/api/events" ->
                        if (request.getHeader("X-Mes-Subsystem") == "familyweb") MockResponse().setBody("""{"response":[
                            {"start_at":"2026-10-20T09:30:00+03:00","subject_name":"Английский язык","marks":[{"value":"3"}]},
                            {"start_at":"2026-10-21T09:30:00+03:00","subject_name":"Английский язык","marks":[{"value":"5"}]},
                            {"start_at":"2026-10-27T09:30:00+03:00","subject_name":"Английский язык","homework":{"descriptions":["стр. 50"]}}]}""")
                        else MockResponse().setResponseCode(401)
                    else -> MockResponse().setResponseCode(401) // мобильный API ключ с сайта не принимает
                }
            }
        }
        s.use {
            val base = s.url("/").toString().trimEnd('/')
            val c = MeshClient("a.b.c", defaultHttpClient(), school = base, dnevnik = base)
            val acc = c.account()
            assertTrue(acc.web); assertEquals(555, acc.profileId)
            val snap = c.snapshot(acc, acc.students.single(), LocalDate.of(2026, 10, 25))
            assertEquals(listOf("5", "3"), snap.marks.map { it.value })
            assertEquals("4.00", DiaryInsights.englishAverage(snap))
            assertEquals(listOf("2026-10-20", "2026-10-21", "2026-10-27"), snap.lessons.map { it.date })
            assertEquals(listOf(50), DiaryInsights.englishHomework(snap, "2026-10-25").flatMap { DiaryInsights.pages(it.text) })
        }
    }

    @Test
    fun partialDataWhenScheduleRejected() = runBlocking {
        val s = MockWebServer().apply {
            dispatcher = object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse = when (request.requestUrl!!.encodedPath) {
                    "/acl/api/users/profile_info" -> MockResponse().setBody("""[{"id":777,"type":"parent"}]""")
                    "/api/family/mobile/v1/profile" -> MockResponse().setBody("""{"profile":{"id":777},"children":[{"id":12,"first_name":"Вероника","contingent_guid":"g-v"}]}""")
                    "/api/family/mobile/v1/marks" -> MockResponse().setBody("""{"payload":[{"value":"4","date":"2026-10-20","subject_name":"Английский язык"}]}""")
                    else -> MockResponse().setResponseCode(401) // расписание (и мобильное, и веб) отказало
                }
            }
        }
        s.use {
            val base = s.url("/").toString().trimEnd('/')
            val c = MeshClient("a.b.c", defaultHttpClient(), school = base, dnevnik = base)
            val acc = c.account()
            assertTrue(!acc.web)
            val snap = c.snapshot(acc, acc.students.single(), LocalDate.of(2026, 10, 25))
            assertEquals(listOf("4"), snap.marks.map { it.value })
            assertEquals("4.00", DiaryInsights.englishAverage(snap), "средние считаются сами")
            assertTrue(snap.problems.any { it.startsWith("расписание") }, snap.problems.toString())
        }
    }

    @Test
    fun authErrors() = runBlocking {
        server(emptyMap(), code = 401).use { s ->
            val base = s.url("/").toString().trimEnd('/')
            val c = MeshClient("t", defaultHttpClient(), school = base, dnevnik = base)
            assertFailsWith<MeshAuthException> { c.account() }
            assertEquals(null, c.refresh())
        }
    }
}
