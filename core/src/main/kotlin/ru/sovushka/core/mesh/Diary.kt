package ru.sovushka.core.mesh

import kotlinx.serialization.Serializable

/** Урок из расписания МЭШ. Время — строка ISO, как приходит с сервера. */
@Serializable
data class DiaryLesson(
    val start: String,
    val end: String = "",
    val subject: String,
    val room: String = "",
    val theme: String = "",
    val homework: List<String> = emptyList(),
    val marks: List<String> = emptyList(),
    val cancelled: Boolean = false,
) {
    val date: String get() = start.take(10)
    val time: String get() = start.drop(11).take(5)
}

@Serializable
data class DiaryMark(
    val date: String,
    val subject: String,
    val value: String,
    val weight: Int = 1,
    val form: String = "",
    val comment: String = "",
)

@Serializable
data class SubjectAverage(val subject: String, val average: String, val marks: List<String> = emptyList())

@Serializable
data class DiaryHomework(val date: String, val subject: String, val text: String, val done: Boolean = false)

/** Снимок дневника, сохраняется на телефоне и показывается без интернета. */
@Serializable
data class DiarySnapshot(
    val studentName: String = "",
    val className: String = "",
    val syncedAt: Long = 0,
    val lessons: List<DiaryLesson> = emptyList(),
    val marks: List<DiaryMark> = emptyList(),
    val averages: List<SubjectAverage> = emptyList(),
    val homework: List<DiaryHomework> = emptyList(),
)

/** Выжимка про английский из дневника — для главного экрана и для Совушки. */
object DiaryInsights {
    fun isEnglish(subject: String): Boolean {
        val s = subject.lowercase()
        return "англ" in s || "english" in s
    }

    fun englishAverage(d: DiarySnapshot): String? = d.averages.firstOrNull { isEnglish(it.subject) }?.average?.takeIf { it.isNotBlank() }

    fun englishMarks(d: DiarySnapshot): List<DiaryMark> = d.marks.filter { isEnglish(it.subject) }.sortedByDescending { it.date }

    /** Ближайший урок английского начиная с сегодняшней даты (yyyy-MM-dd). */
    fun nextEnglishLesson(d: DiarySnapshot, today: String): DiaryLesson? =
        d.lessons.filter { isEnglish(it.subject) && !it.cancelled && it.date >= today }.minByOrNull { it.start }

    /** Домашка по английскому на ближайшие дни. */
    fun englishHomework(d: DiarySnapshot, today: String): List<DiaryHomework> {
        val fromList = d.homework.filter { isEnglish(it.subject) && it.date >= today && it.text.isNotBlank() }
        val fromLessons = d.lessons.filter { isEnglish(it.subject) && it.date >= today }
            .flatMap { l -> l.homework.map { DiaryHomework(l.date, l.subject, it) } }
        return (fromList + fromLessons).distinctBy { it.date + it.text.trim() }.sortedBy { it.date }
    }

    private val PAGE = Regex("""(?:стр(?:аниц[аыу])?|с)\.?\s*(\d{1,3})""", RegexOption.IGNORE_CASE)

    /** Номера страниц учебника из текста задания: «стр. 45 упр. 3» → [45]. */
    fun pages(text: String): List<Int> = PAGE.findAll(text).mapNotNull { it.groupValues[1].toIntOrNull() }.distinct().toList()

    /** Короткая справка для нейросети: как дела с английским в школе. */
    fun tutorContext(d: DiarySnapshot?, today: String): String {
        if (d == null) return ""
        val parts = mutableListOf<String>()
        englishAverage(d)?.let { parts += "средний балл по английскому в школе $it" }
        englishMarks(d).take(6).takeIf { it.isNotEmpty() }?.let { m -> parts += "последние оценки: " + m.joinToString(", ") { it.value } }
        d.lessons.filter { isEnglish(it.subject) && it.theme.isNotBlank() }.sortedByDescending { it.start }
            .map { it.theme }.distinct().take(3).takeIf { it.isNotEmpty() }?.let { parts += "темы уроков в школе: " + it.joinToString("; ") }
        englishHomework(d, today).take(2).takeIf { it.isNotEmpty() }?.let { h -> parts += "задано: " + h.joinToString("; ") { "${it.date}: ${it.text.take(200)}" } }
        return if (parts.isEmpty()) "" else "Из школьного дневника: " + parts.joinToString(". ") + "."
    }
}
