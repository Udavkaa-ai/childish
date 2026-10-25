package ru.sovushka.core

import kotlinx.serialization.Serializable
import ru.sovushka.core.mesh.DiarySnapshot

/** Слово из урока старшей дочки. Хранит и содержимое, и состояние интервального повторения. */
@Serializable
data class Word(
    val id: String,
    val lessonId: String,
    val en: String,
    val ru: String,
    val tr: String = "",
    val emoji: String = "",
    val example: String = "",
    val exampleRu: String = "",
    /** Коробка Лейтнера: 0 — новое слово, чем больше — тем лучше выучено. */
    val box: Int = 0,
    /** День (epochDay), когда слово пора повторить. */
    val due: Long = 0,
    val right: Int = 0,
    val wrong: Int = 0,
)

@Serializable
data class Sentence(val en: String, val ru: String)

@Serializable
data class Lesson(
    val id: String,
    val title: String,
    val createdAt: Long,
    val note: String = "",
    val grammar: String = "",
    val sentences: List<Sentence> = emptyList(),
    val sourceText: String = "",
    val photos: List<String> = emptyList(),
    val bestStars: Int = 0,
    val runs: Int = 0,
    /** Упражнения на грамматику и построение предложений по этой странице. */
    val drills: List<Drill> = emptyList(),
    /** Пройденные темы (id из GrammarBank), к которым относится страница. */
    val topics: List<String> = emptyList(),
)

@Serializable
data class KidStats(
    val stars: Int = 0,
    val streak: Int = 0,
    val lastDay: Long = 0,
    val sessions: Int = 0,
) {
    /** Засчитать занятие в день [today]: звёзды и серия дней подряд. */
    fun addSession(today: Long, earned: Int): KidStats {
        val newStreak = when (lastDay) {
            today -> streak.coerceAtLeast(1)
            today - 1 -> streak + 1
            else -> 1
        }
        return copy(stars = stars + earned, streak = newStreak, lastDay = today, sessions = sessions + 1)
    }

    /** Серия, которую видно сегодня: если вчера не занимались — она сгорела. */
    fun visibleStreak(today: Long): Int = if (lastDay >= today - 1) streak else 0
}

@Serializable
data class AppData(
    val version: Int = 1,
    val lessons: List<Lesson> = emptyList(),
    val words: List<Word> = emptyList(),
    val older: KidStats = KidStats(),
    val younger: KidStats = KidStats(),
    /** Последний снимок школьного дневника МЭШ. */
    val diary: DiarySnapshot? = null,
    /** Успехи по грамматическим темам (id темы → интервальное повторение). */
    val grammar: Map<String, TopicStats> = emptyMap(),
    /** Дополнительные упражнения по темам от нейросети («Ещё задания от Ники»). */
    val extraDrills: Map<String, List<Drill>> = emptyMap(),
    /** Упражнения, в которых ребёнок нашёл ошибку («🚩 В задании ошибка») — больше не показываем. */
    val badDrills: Set<String> = emptySet(),
) {
    /** Все упражнения темы: встроенные и придуманные нейросетью. */
    fun drillsOf(topicId: String): List<Drill> =
        (GrammarBank.topic(topicId)?.drills.orEmpty() + extraDrills[topicId].orEmpty()).filter { it.key !in badDrills }

    /** Убрать ошибочное упражнение отовсюду: из уроков, из дополнительных и из встроенных (через список). */
    fun withoutDrill(d: Drill): AppData = copy(
        lessons = lessons.map { l -> if (l.drills.any { it.key == d.key }) l.copy(drills = l.drills.filter { it.key != d.key }) else l },
        extraDrills = extraDrills.mapValues { (_, list) -> list.filter { it.key != d.key } },
        badDrills = badDrills + d.key,
    )

    /** Темы, которые пора повторить. */
    fun dueTopics(today: Long): List<String> = grammar.filter { (_, s) -> s.box > 0 && s.due <= today }.keys.toList()

    fun wordsOf(lessonId: String): List<Word> = words.filter { it.lessonId == lessonId }
    fun dueWords(today: Long): List<Word> = words.filter { Srs.isDue(it, today) }
}
