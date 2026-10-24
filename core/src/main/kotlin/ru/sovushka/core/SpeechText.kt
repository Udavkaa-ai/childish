package ru.sovushka.core

/** Подготовка текста к озвучке: русские и английские куски читаются разными голосами. */
object SpeechText {
    enum class Lang { RU, EN }

    data class Segment(val text: String, val lang: Lang)

    private fun isCyrillic(c: Char) = c in 'Ѐ'..'ӿ'
    private fun isLatin(c: Char) = c in 'a'..'z' || c in 'A'..'Z'

    /** Убирает эмодзи, markdown и транскрипции в квадратных скобках. */
    fun clean(text: String): String {
        val noTr = text.replace(Regex("\\[[^\\]]*]"), " ")
        val sb = StringBuilder()
        var i = 0
        while (i < noTr.length) {
            val cp = noTr.codePointAt(i)
            val ok = Character.isLetterOrDigit(cp) || Character.isWhitespace(cp) ||
                cp.toChar() in ".,!?;:'\"()-–—«»…"
            sb.append(if (ok) String(Character.toChars(cp)) else " ")
            i += Character.charCount(cp)
        }
        return sb.toString().replace(Regex("\\s+"), " ").replace(Regex(" ([.,!?;:…])"), "$1").trim()
    }

    /** Делит текст на куски одного языка; знаки и цифры приклеиваются к текущему куску. */
    fun split(text: String, default: Lang = Lang.RU): List<Segment> {
        val src = clean(text)
        val out = mutableListOf<Segment>()
        val cur = StringBuilder()
        var lang: Lang? = null
        for (c in src) {
            val l = when {
                isCyrillic(c) -> Lang.RU
                isLatin(c) -> Lang.EN
                else -> null
            }
            if (l != null && lang != null && l != lang) {
                // Пробелы и знаки между кусками оставляем предыдущему куску.
                val s = cur.toString().trim()
                if (s.isNotEmpty()) out += Segment(s, lang)
                cur.clear()
            }
            if (l != null) lang = l
            cur.append(c)
        }
        val s = cur.toString().trim()
        if (s.isNotEmpty()) out += Segment(s, lang ?: default)
        return out.filter { seg -> seg.text.any { it.isLetterOrDigit() } }
    }
}
