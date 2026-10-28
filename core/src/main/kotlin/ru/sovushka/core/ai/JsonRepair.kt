package ru.sovushka.core.ai

/** Починка ответа нейросети: текст вокруг JSON и обрезанный по лимиту токенов хвост. */
internal object JsonRepair {
    /**
     * Первый JSON-объект из ответа. Если он оборван на середине, отрезаем незаконченный хвост
     * после последней запятой и закрываем открытые скобки — всё, что успело прийти, сохраняется.
     */
    fun extract(raw: String): String? {
        val start = raw.indexOf('{')
        if (start < 0) return null
        val open = ArrayDeque<Char>()
        var inString = false
        var escaped = false
        var cut = -1
        var cutClosers = ""
        for (i in start until raw.length) {
            val c = raw[i]
            if (inString) {
                when {
                    escaped -> escaped = false
                    c == '\\' -> escaped = true
                    c == '"' -> inString = false
                }
                continue
            }
            when (c) {
                '"' -> inString = true
                '{' -> open.addLast('}')
                '[' -> open.addLast(']')
                '}', ']' -> {
                    if (open.isEmpty()) return null
                    open.removeLast()
                    if (open.isEmpty()) return raw.substring(start, i + 1)
                    cut = i + 1
                    cutClosers = open.reversed().joinToString("")
                }
                ',' -> {
                    cut = i
                    cutClosers = open.reversed().joinToString("")
                }
            }
        }
        return if (cut < 0) null else raw.substring(start, cut) + cutClosers
    }
}
