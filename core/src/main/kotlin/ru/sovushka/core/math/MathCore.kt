package ru.sovushka.core.math

import kotlinx.serialization.Serializable

/**
 * Задача по математике. input — ответ вводится с цифровой клавиатуры, choose — выбор из вариантов
 * (в том числе знаки сравнения <, =, >). unit — подпись после поля ответа («см», «руб.»).
 * explain — решение по шагам, его Ника читает после ошибки.
 */
@Serializable
data class MathProblem(
    val kind: String = INPUT,
    val prompt: String,
    val answer: String,
    val unit: String = "",
    val options: List<String> = emptyList(),
    val explain: String = "",
    val topic: String = "",
) {
    /** Отпечаток задачи — чтобы помнить «плохие» задания. */
    val key: String get() = "$kind|${prompt.trim()}|${answer.trim()}"

    fun isValid(): Boolean = prompt.isNotBlank() && MathCheck.normalize(answer).isNotEmpty() && when (kind) {
        INPUT -> true
        CHOOSE -> options.size >= 2 && options.any { MathCheck.same(it, answer) }
        else -> false
    }

    companion object {
        const val INPUT = "input"
        const val CHOOSE = "choose"
        val SIGNS = listOf("<", "=", ">")
    }
}

/** Разобранный пример к правилу: задание и решение. */
@Serializable
data class MathExample(val problem: String, val solution: String)

/** Проверка ответа ребёнка: «9 ост. 2» = «9 (ост.2)», «0305» = «305», единицы измерения не мешают. */
object MathCheck {
    fun normalize(s: String): String {
        var t = s.lowercase().replace(' ', ' ').replace(' ', ' ')
        t = t.replace(Regex("ост(аток|\\.)?"), " r ")
        t = t.replace("(", " ").replace(")", " ")
        // Единицы измерения и прочие слова не важны: остаются цифры, знаки сравнения, дробь и «r» — остаток.
        t = t.replace(Regex("[а-яёa-qs-z²³]+\\.?"), " ")
        t = t.replace('−', '-').replace('–', '-').replace(',', '.')
        t = t.replace(Regex("\\s+"), "").trimEnd('.')
        return t
    }

    fun same(given: String, answer: String): Boolean {
        val a = normalize(given)
        val b = normalize(answer)
        if (a.isEmpty() || b.isEmpty()) return false
        if (a == b) return true
        val x = a.toBigDecimalOrNull()
        val y = b.toBigDecimalOrNull()
        if (x != null && y != null) return x.compareTo(y) == 0
        // «9r2» и «09r02»
        val ra = a.split('r')
        val rb = b.split('r')
        return ra.size == 2 && rb.size == 2 &&
            ra.zip(rb).all { (p, q) -> p.toBigDecimalOrNull()?.let { u -> q.toBigDecimalOrNull()?.let { u.compareTo(it) == 0 } } == true }
    }

    /** Дополнительные кнопки клавиатуры, которые понадобятся для этого ответа. */
    fun extraKeys(answer: String): List<String> {
        val n = normalize(answer)
        return buildList {
            if ('r' in n) add("ост.")
            if ('/' in n) add("/")
            if ('.' in n) add(",")
            if (n.startsWith("-")) add("−")
        }
    }
}

/**
 * Небольшой вычислитель для заданий нейросети: целые числа, + − × · : ÷ / и скобки.
 * Нужен, чтобы отбрасывать задачи с арифметическими ошибками — нейросети в счёте ошибаются.
 */
object MathEval {
    private class Fail : RuntimeException()

    /** Значение выражения или null, если это не чистая арифметика или деление не нацело. */
    fun eval(expr: String): Long? {
        val s = prepare(expr) ?: return null
        if (s.isEmpty()) return null
        return try {
            val p = Parser(s)
            val v = p.expr()
            if (p.pos != s.length) null else v
        } catch (e: Fail) {
            null
        } catch (e: ArithmeticException) {
            null
        }
    }

    private fun prepare(expr: String): String? {
        var s = expr.replace(' ', ' ').replace(' ', ' ').trim()
        s = joinGroups(s)
        s = s.replace('×', '*').replace('·', '*').replace('−', '-').replace('–', '-').replace(':', '/').replace('÷', '/')
        s = s.replace(" ", "")
        return if (s.matches(Regex("[0-9+\\-*/()]*"))) s else null
    }

    /** «12 345» → «12345»: пробел внутри числа разделяет классы. */
    fun joinGroups(s: String): String = s.replace(Regex("(?<=\\d)[   ](?=\\d{3}(?!\\d))"), "")

    private class Parser(val s: String) {
        var pos = 0
        fun expr(): Long {
            var v = term()
            while (pos < s.length && (s[pos] == '+' || s[pos] == '-')) {
                val op = s[pos++]
                val r = term()
                v = if (op == '+') Math.addExact(v, r) else Math.subtractExact(v, r)
            }
            return v
        }

        fun term(): Long {
            var v = factor()
            while (pos < s.length && (s[pos] == '*' || s[pos] == '/')) {
                val op = s[pos++]
                val r = factor()
                v = if (op == '*') Math.multiplyExact(v, r) else {
                    if (r == 0L || v % r != 0L) throw Fail()
                    v / r
                }
            }
            return v
        }

        fun factor(): Long {
            if (pos >= s.length) throw Fail()
            if (s[pos] == '(') {
                pos++
                val v = expr()
                if (pos >= s.length || s[pos] != ')') throw Fail()
                pos++
                return v
            }
            val start = pos
            while (pos < s.length && s[pos].isDigit()) pos++
            if (start == pos || pos - start > 15) throw Fail()
            return s.substring(start, pos).toLong()
        }
    }

    private val UNKNOWN = Regex("[хx□?…]|_+")

    /**
     * Проверка задачи: true — ответ точно верный, false — точно неверный, null — проверить нельзя
     * (текстовая задача, величины и т. п.). Понимает «48 : 6 = ?», «х + 25 = 60», «47 : 5» с остатком,
     * «7 × □ = 56» и сравнение «45 × 2 ○ 100».
     */
    fun verify(p: MathProblem): Boolean? {
        val text = p.prompt.replaceFirst(Regex("^[^0-9(хx□?_…]+"), "").trim()
        if (p.kind == MathProblem.CHOOSE) {
            val sign = p.answer.trim()
            if (sign !in MathProblem.SIGNS) return null
            val parts = text.split(Regex("[○◯⚪?…]|_+|□")).filter { it.isNotBlank() }
            if (parts.size != 2) return null
            val a = eval(parts[0]) ?: return null
            val b = eval(parts[1]) ?: return null
            val real = if (a < b) "<" else if (a > b) ">" else "="
            return real == sign
        }
        val ans = MathCheck.normalize(p.answer)
        if ('=' !in text) {
            val e = text.trimEnd('?', ' ', '=')
            return checkValue(e, eval(e), ans)
        }
        val sides = text.split('=')
        if (sides.size != 2) return null
        val left = sides[0].trim()
        val right = sides[1].trim().trimEnd('.', ' ')
        if (right.isEmpty() || right.matches(UNKNOWN)) return checkValue(left, eval(left), ans)
        // Уравнение: одно неизвестное, подставляем ответ.
        val unknowns = UNKNOWN.findAll(left).count() + UNKNOWN.findAll(right).count()
        if (unknowns != 1) return null
        val x = ans.toLongOrNull() ?: return null
        val l = side(left, x) ?: return null
        val r = side(right, x) ?: return null
        return l.isNotEmpty() && r.isNotEmpty() && l == r
    }

    /**
     * Значение стороны уравнения после подстановки ответа. Пустой список — подстановка дала деление
     * не нацело (значит, ответ неверный), null — это вообще не арифметика.
     */
    private fun side(expr: String, x: Long): List<Long>? {
        val v = eval(expr.replace(UNKNOWN, x.toString()))
        if (v != null) return listOf(v)
        return if (expr.contains(UNKNOWN) && eval(expr.replace(UNKNOWN, "1")) != null) emptyList() else null
    }

    /** Сравнить значение выражения с ответом; для «a : b» понимает ответ с остатком. */
    private fun checkValue(expr: String, value: Long?, ans: String): Boolean? {
        // Просто число — это не пример («Сколько сотен в числе 12 675?»).
        if (!expr.contains(Regex("[+×·*:÷/−-]"))) return null
        if ('r' in ans) {
            val m = Regex("^\\s*(\\d+)\\s*[:÷/]\\s*(\\d+)\\s*$").find(joinGroups(expr)) ?: return null
            val a = m.groupValues[1].toLong()
            val b = m.groupValues[2].toLong()
            val (q, r) = ans.split('r').map { it.toLongOrNull() ?: return false }
            return b > 0 && r in 0 until b && q * b + r == a
        }
        if (value == null) return null
        val n = ans.toBigDecimalOrNull() ?: return null
        return n.compareTo(value.toBigDecimal()) == 0
    }
}

/** Как Нике прочитать задачу вслух: знаки словами, дроби, единицы, «ост.» и «□». */
object MathSpeech {
    private val ORD = mapOf(
        2 to ("вторая" to "вторых"), 3 to ("третья" to "третьих"), 4 to ("четвёртая" to "четвёртых"),
        5 to ("пятая" to "пятых"), 6 to ("шестая" to "шестых"), 7 to ("седьмая" to "седьмых"),
        8 to ("восьмая" to "восьмых"), 9 to ("девятая" to "девятых"), 10 to ("десятая" to "десятых"),
        100 to ("сотая" to "сотых"),
    )
    private val UNITS = mapOf(
        "мм" to "миллиметров", "см" to "сантиметров", "дм" to "дециметров", "м" to "метров", "км" to "километров",
        "г" to "граммов", "кг" to "килограммов", "т" to "тонн", "ц" to "центнеров",
        "мин" to "минут", "ч" to "часов", "с" to "секунд", "сут." to "суток", "мес." to "месяцев",
    )
    private val FEM = mapOf(
        1 to "одна", 2 to "две", 3 to "три", 4 to "четыре", 5 to "пять", 6 to "шесть", 7 to "семь", 8 to "восемь", 9 to "девять",
    )

    fun forSpeech(text: String): String {
        var t = MathEval.joinGroups(text)
        t = Regex("(\\d+)\\s*/\\s*(\\d+)").replace(t) { m ->
            val n = m.groupValues[1].toInt()
            val d = m.groupValues[2].toInt()
            val o = ORD[d] ?: return@replace "${m.groupValues[1]} ${m.groupValues[2]}-х"
            if (n != 1) return@replace "${FEM[n] ?: n.toString()} ${o.second}"
            // «Найди 1/4 от 20» — «найди одну четвёртую».
            val before = t.substring(0, m.range.first).trimEnd().lowercase()
            if (before.endsWith("найди")) "одну " + if (o.first.endsWith("ья")) o.first.dropLast(2) + "ью" else o.first.dropLast(2) + "ую" else "одна ${o.first}"
        }
        t = t.replace("км/ч", "км в час")
        // «□ см» — «сколько сантиметров»: синтезатор сам склоняет «5 см», а одиночное сокращение читает по буквам.
        t = Regex("□\\s*(мм|см|дм|км|м|кг|г|т|ц|мин|ч|с|сут\\.|мес\\.)(?![а-яё])").replace(t) { m ->
            " сколько " + (UNITS[m.groupValues[1]] ?: m.groupValues[1])
        }
        t = Regex("(мм|см|дм|км|м)²").replace(t) { m ->
            " квадратных " + when (m.groupValues[1]) {
                "мм" -> "миллиметров"; "см" -> "сантиметров"; "дм" -> "дециметров"; "км" -> "километров"; else -> "метров"
            }
        }
        t = t.replace(Regex("ост\\."), " остаток ")
        t = t.replace("×", " умножить на ").replace("·", " умножить на ").replace("÷", " разделить на ")
        t = t.replace(Regex("(?<=[\\d)□х])\\s*:\\s*(?=[\\d(□х])"), " разделить на ")
        t = t.replace(Regex("(?<=[\\d)□х ])\\s*[−-]\\s*(?=[\\d(□х])"), " минус ")
        t = t.replace("+", " плюс ").replace("=", " равно ")
        t = t.replace("□", " сколько ").replace(Regex("[○◯]"), " и ")
        t = t.replace(Regex("(?<![а-яё])х(?![а-яё])"), " икс ")
        return t.replace(Regex("\\s+"), " ").trim()
    }
}

/** Числа по-русски: классы разделяются неразрывным пробелом, начиная с десятков тысяч (4560, 30 485). */
fun fmt(n: Long): String {
    if (n < 10_000 && n > -10_000) return n.toString()
    val s = kotlin.math.abs(n).toString()
    val out = StringBuilder()
    s.forEachIndexed { i, c ->
        if (i > 0 && (s.length - i) % 3 == 0) out.append(' ')
        out.append(c)
    }
    return (if (n < 0) "−" else "") + out
}

fun fmt(n: Int): String = fmt(n.toLong())

/** 1 тетрадь, 2 тетради, 5 тетрадей. */
fun plural(n: Long, one: String, few: String, many: String): String {
    val m100 = n % 100
    val m10 = n % 10
    return when {
        m100 in 11..14 -> many
        m10 == 1L -> one
        m10 in 2..4 -> few
        else -> many
    }
}
