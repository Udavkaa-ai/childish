package ru.sovushka.core

/** Сравнение ответов ребёнка: опечатки и неточно распознанная речь. */
object TextMatch {
    private val NUMBERS = listOf(
        "zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten",
        "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen",
        "nineteen", "twenty",
    )

    fun normalize(s: String): String {
        val lower = s.lowercase()
            .replace('ё', 'е')
            .replace('’', '\'')
            .replace('`', '\'')
        val cleaned = buildString {
            for (c in lower) append(if (c.isLetterOrDigit() || c == '\'') c else ' ')
        }
        return cleaned.split(' ')
            .filter { it.isNotBlank() }
            .joinToString(" ") { tok -> tok.toIntOrNull()?.let { NUMBERS.getOrNull(it) } ?: tok }
    }

    fun levenshtein(a: String, b: String): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length
        var prev = IntArray(b.length + 1) { it }
        var cur = IntArray(b.length + 1)
        for (i in 1..a.length) {
            cur[0] = i
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                cur[j] = minOf(prev[j] + 1, cur[j - 1] + 1, prev[j - 1] + cost)
            }
            val t = prev; prev = cur; cur = t
        }
        return prev[b.length]
    }

    /** Похожесть 0..1 после нормализации. */
    fun similarity(a: String, b: String): Float {
        val x = normalize(a)
        val y = normalize(b)
        if (x.isEmpty() && y.isEmpty()) return 1f
        val max = maxOf(x.length, y.length)
        return 1f - levenshtein(x, y).toFloat() / max
    }

    /** Ответ при наборе: регистр и пунктуация не важны, артикль в начале можно пропустить. */
    fun typedCorrect(expected: String, typed: String): Boolean {
        val e = normalize(expected)
        val t = normalize(typed)
        return e == t || stripArticle(e) == stripArticle(t)
    }

    /**
     * Проверка произношения по вариантам распознавания.
     * Распознаватель часто слышит слово внутри фразы ("a dog", "dog dog"), поэтому
     * засчитываем и вхождение ожидаемого слова в услышанное.
     */
    fun spokenMatch(expected: String, heard: List<String>, threshold: Float = 0.7f): Boolean {
        val e = stripArticle(normalize(expected))
        if (e.isEmpty()) return false
        return heard.any { h ->
            val n = stripArticle(normalize(h))
            n == e || " $n ".contains(" $e ") || similarity(e, n) >= threshold
        }
    }

    private fun stripArticle(s: String): String =
        s.removePrefix("the ").removePrefix("an ").removePrefix("a ").removePrefix("to ")
}
