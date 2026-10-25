package ru.sovushka.core

import kotlinx.serialization.Serializable

/**
 * Упражнение на грамматику и построение предложений.
 * kind:
 *  - "gap"    — вставить слово: prompt со «___», варианты options, правильный answer;
 *  - "choose" — выбрать правильное предложение: options (с ошибками), answer;
 *  - "order"  — собрать английское предложение по русскому (prompt) из слов answer + лишних extra.
 */
@Serializable
data class Drill(
    val kind: String,
    val prompt: String = "",
    val options: List<String> = emptyList(),
    val answer: String,
    val extra: List<String> = emptyList(),
    val explain: String = "",
    val topic: String = "",
) {
    /** Слова для «order» без знаков препинания в конце. */
    val answerTokens: List<String> get() = SessionBuilder.tokenize(answer)

    fun isValid(): Boolean = when (kind) {
        "gap" -> "___" in prompt && answer.isNotBlank() && options.any { TextMatch.normalize(it) == TextMatch.normalize(answer) } && options.size >= 2
        "choose" -> answer.isNotBlank() && options.any { it.trim() == answer.trim() } && options.size >= 2
        "order" -> prompt.isNotBlank() && answerTokens.size in 2..12
        else -> false
    }

    /** Отпечаток упражнения — чтобы помнить «плохие» задания. */
    val key: String get() = "$kind|${prompt.trim()}|${answer.trim()}"

    /** Предложение целиком — чтобы озвучить после ответа. */
    val fullSentence: String get() = if (kind == "gap") prompt.replace("___", answer) else answer

    companion object {
        const val GAP = "gap"
        const val CHOOSE = "choose"
        const val ORDER = "order"
    }
}

/** Успехи по грамматической теме — интервальное повторение, как у слов. */
@Serializable
data class TopicStats(val box: Int = 0, val due: Long = 0, val right: Int = 0, val wrong: Int = 0) {
    fun review(correct: Boolean, today: Long): TopicStats =
        if (correct) {
            val b = (box + 1).coerceAtMost(Srs.MAX_BOX)
            copy(box = b, due = today + Srs.INTERVALS[b], right = right + 1)
        } else {
            copy(box = 1, due = today + Srs.INTERVALS[1], wrong = wrong + 1)
        }
}

@Serializable
data class GrammarTopic(
    val id: String,
    val grade: Int,
    val title: String,
    val rule: String,
    val examples: List<Sentence>,
    val drills: List<Drill>,
)
