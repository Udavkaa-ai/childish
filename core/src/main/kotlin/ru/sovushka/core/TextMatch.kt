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
     * Распознаватель часто слышит слово внутри фразы ("a dog", "dog dog") или похожее по звучанию
     * ("dock" вместо "dog" — русские дети оглушают конечные согласные), поэтому засчитываем
     * вхождение, близкое написание и одинаковое звучание.
     */
    fun spokenMatch(expected: String, heard: List<String>, threshold: Float = 0.7f): Boolean {
        val e = stripArticle(normalize(expected))
        if (e.isEmpty()) return false
        val eKey = soundKey(e)
        val eWords = e.split(' ').size
        return heard.any { h ->
            val n = stripArticle(normalize(h))
            if (n == e || " $n ".contains(" $e ") || similarity(e, n) >= threshold) return@any true
            // Звучание: вся фраза или любой кусок из стольких же слов.
            val words = n.split(' ').filter { it.isNotEmpty() }
            (listOf(n) + words.windowed(eWords, partialWindows = false).map { it.joinToString(" ") }).any { part ->
                val k = soundKey(part)
                k == eKey || (eKey.length >= 4 && levenshtein(k, eKey) <= 1)
            }
        }
    }

    /**
     * Упрощённый «звуковой ключ» английского слова: одинаково звучащие написания и типичные
     * ошибки русскоговорящих (звонкие/глухие согласные, w/v, th) дают один ключ.
     * Гласные сворачиваются в одну метку — важен «скелет» слова.
     */
    fun soundKey(text: String): String = text.lowercase().split(' ').filter { it.isNotBlank() }.joinToString("") { word ->
        var w = word.filter { it in 'a'..'z' }
        if (w.isEmpty()) return@joinToString ""
        w = w.replace(Regex("^(kn|gn|pn|wr|ps)")) { it.value.substring(1) }
            .replace(Regex("^wh"), "w")
            .replace(Regex("^x"), "s")
            .replace("x", "ks")
            .replace("tch", "ch").replace("sch", "sk").replace("ph", "f").replace("ck", "k").replace("dg", "j")
            .replace(Regex("(?<=.)gh"), "").replace("th", "t").replace("sh", "9").replace("ch", "9")
            .replace(Regex("c(?=[eiy])"), "s").replace("c", "k").replace("q", "k")
            .replace(Regex("g(?=[eiy])"), "j")
            .replace('d', 't').replace('b', 'p').replace('g', 'k').replace('v', 'f').replace('w', 'f').replace('z', 's').replace('j', '9')
            .replace("h", "")
            .replace(Regex("(?<=.)y"), "a")
            .replace(Regex("[aeiouy]+"), "a")
            .replace(Regex("(.)\\1+"), "$1")
            .replace(Regex("(?<=.)a$"), "")
        w
    }

    private fun stripArticle(s: String): String =
        s.removePrefix("the ").removePrefix("an ").removePrefix("a ").removePrefix("to ")
}
