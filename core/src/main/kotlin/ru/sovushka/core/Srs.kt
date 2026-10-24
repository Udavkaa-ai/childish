package ru.sovushka.core

/**
 * Простое интервальное повторение по системе Лейтнера.
 * Правильный ответ переносит слово в следующую коробку (повтор реже),
 * ошибка возвращает его в первую (повтор завтра).
 */
object Srs {
    /** Через сколько дней повторять слово из коробки с индексом i. */
    val INTERVALS = intArrayOf(0, 1, 2, 4, 7, 14, 30, 60)
    const val MAX_BOX = 7
    /** С какой коробки слово считается выученным. */
    const val LEARNED_BOX = 4

    fun isDue(word: Word, today: Long): Boolean = word.box > 0 && word.due <= today

    fun review(word: Word, correct: Boolean, today: Long): Word =
        if (correct) {
            val box = (word.box + 1).coerceAtMost(MAX_BOX)
            word.copy(box = box, due = today + INTERVALS[box], right = word.right + 1)
        } else {
            word.copy(box = 1, due = today + INTERVALS[1], wrong = word.wrong + 1)
        }

    /** Доля выученных слов, 0..1. */
    fun mastery(words: List<Word>): Float =
        if (words.isEmpty()) 0f else words.count { it.box >= LEARNED_BOX }.toFloat() / words.size
}
