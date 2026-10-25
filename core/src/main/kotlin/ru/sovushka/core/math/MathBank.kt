package ru.sovushka.core.math

import kotlin.random.Random

/**
 * Тема программы по математике (Моро, «Школа России», 3–4 класс): правило простыми словами,
 * разобранные примеры и генератор задач — задачи каждый раз новые и работают без интернета.
 */
class MathTopic(
    val id: String,
    val grade: Int,
    val title: String,
    val rule: String,
    val examples: List<MathExample>,
    private val gen: (Random) -> MathProblem,
) {
    fun problem(r: Random): MathProblem = gen(r).copy(topic = id)

    /** n разных задач (без повторов условия). */
    fun problems(n: Int, r: Random = Random.Default): List<MathProblem> {
        val out = LinkedHashMap<String, MathProblem>()
        var tries = 0
        while (out.size < n && tries < n * 20) {
            val p = problem(r)
            out.putIfAbsent(p.prompt, p)
            tries++
        }
        return out.values.toList()
    }
}

object MathBank {
    private fun Random.between(a: Int, b: Int): Int = nextInt(a, b + 1)
    private fun ex(problem: String, solution: String) = MathExample(problem, solution)
    private fun input(prompt: String, answer: Long, explain: String, unit: String = "") =
        MathProblem(MathProblem.INPUT, prompt, answer.toString(), unit, explain = explain)

    private fun compare(prompt: String, a: Long, b: Long, explain: String) =
        MathProblem(MathProblem.CHOOSE, prompt, if (a < b) "<" else if (a > b) ">" else "=", options = MathProblem.SIGNS, explain = explain)

    private fun sign(a: Long, b: Long) = if (a < b) "<" else if (a > b) ">" else "="

    private val PLACES = listOf("единицы", "десятки", "сотни", "единицы тысяч", "десятки тысяч", "сотни тысяч")
    private val PLACES_GEN = listOf("единиц", "десятков", "сотен", "единиц тысяч", "десятков тысяч", "сотен тысяч")

    /** Сложение столбиком по разрядам. */
    fun columnAdd(a: Long, b: Long): String {
        val steps = mutableListOf<String>()
        var x = a; var y = b; var carry = 0L; var i = 0
        while (x > 0 || y > 0) {
            val da = x % 10; val db = y % 10
            val s = da + db + carry
            val plus = if (carry > 0) " + 1" else ""
            val last = x / 10 == 0L && y / 10 == 0L
            val name = PLACES.getOrElse(i) { "разряд" }
            steps += if (s >= 10 && !last) "$name: $da + $db$plus = $s, пишем ${s % 10}, 1 запоминаем" else "$name: $da + $db$plus = $s"
            carry = s / 10
            x /= 10; y /= 10; i++
        }
        return "Пишем столбиком и складываем по разрядам, начиная с единиц. ${steps.joinToString("; ").replaceFirstChar { it.uppercase() }}. Ответ: ${fmt(a + b)}."
    }

    /** Вычитание столбиком: если цифры не хватает — занимаем единицу у соседнего разряда. */
    fun columnSub(a: Long, b: Long): String {
        val steps = mutableListOf<String>()
        var x = a; var y = b; var borrow = 0L; var i = 0
        while (x > 0) {
            val da = x % 10 - borrow; val db = y % 10
            val name = PLACES.getOrElse(i) { "разряд" }
            if (da < db) {
                steps += "$name: занимаем 1 у соседнего разряда, ${da + 10} − $db = ${da + 10 - db}"
                borrow = 1
            } else {
                if (!(x / 10 == 0L && da == 0L && db == 0L)) steps += "$name: $da − $db = ${da - db}"
                borrow = 0
            }
            x /= 10; y /= 10; i++
        }
        return "Пишем столбиком и вычитаем по разрядам, начиная с единиц. ${steps.joinToString("; ").replaceFirstChar { it.uppercase() }}. Ответ: ${fmt(a - b)}."
    }

    /** Умножение столбиком на однозначное число. */
    fun columnMul(a: Long, b: Long): String {
        val steps = mutableListOf<String>()
        var x = a; var carry = 0L
        while (x > 0) {
            val d = x % 10
            val p = d * b + carry
            val plus = if (carry > 0) ", плюс $carry = $p" else ""
            steps += if (x / 10 > 0 && p >= 10) "$d × $b = ${d * b}$plus, пишем ${p % 10}, ${p / 10} запоминаем" else "$d × $b = ${d * b}$plus"
            carry = p / 10
            x /= 10
        }
        return "Умножаем каждую цифру на $b, начиная с единиц: ${steps.joinToString("; ")}. Ответ: ${fmt(a * b)}."
    }

    /** Деление уголком: берём неполное делимое и делим по разрядам. */
    fun longDivision(a: Long, b: Long): String {
        val digits = a.toString()
        val steps = mutableListOf<String>()
        var cur = 0L
        var started = false
        for (c in digits) {
            cur = cur * 10 + (c - '0')
            if (!started && cur < b) continue
            started = true
            val q = cur / b
            val rest = cur - q * b
            steps += if (rest > 0) "${fmt(cur)} : $b = $q, остаток $rest" else "${fmt(cur)} : $b = $q"
            cur = rest
        }
        return "Делим уголком, начиная со старших разрядов: ${steps.joinToString("; ")}. Ответ: ${fmt(a / b)}."
    }

    private data class Item(val one: String, val few: String, val many: String)

    private val ITEMS = listOf(
        Item("тетрадь", "тетради", "тетрадей"), Item("ручка", "ручки", "ручек"), Item("книга", "книги", "книг"),
        Item("шоколадка", "шоколадки", "шоколадок"), Item("открытка", "открытки", "открыток"), Item("игрушка", "игрушки", "игрушек"),
    )

    private fun Item.n(k: Long) = plural(k, one, few, many)

    val topics: List<MathTopic> = listOf(
        MathTopic(
            "mult_table", 3, "Таблица умножения",
            "Умножение — это сложение одинаковых чисел: 4 × 3 = 4 + 4 + 4 = 12. От перестановки множителей " +
                "произведение не меняется: 3 × 4 = 4 × 3. Таблицу умножения нужно знать наизусть — тогда всё остальное считается быстро.",
            listOf(ex("7 × 8", "= 56"), ex("6 × □ = 54", "□ = 9, потому что 6 × 9 = 54")),
        ) { r ->
            val a = r.between(2, 9).toLong(); val b = r.between(2, 9).toLong()
            if (r.nextInt(3) == 0) input("$a × □ = ${a * b}", b, "Какое число умножить на $a, чтобы получить ${a * b}? Делим: ${a * b} : $a = $b. Проверка: $a × $b = ${a * b}.")
            else {
                val times = { k: Long -> "$k ${plural(k, "раз", "раза", "раз")}" }
                val also = if (a != b) " (или $a, взятое ${times(b)})" else ""
                input("$a × $b = □", a * b, "$a × $b — это $b, взятое ${times(a)}$also. По таблице умножения: $a × $b = ${a * b}.")
            }
        },
        MathTopic(
            "div_table", 3, "Табличное деление",
            "Деление — действие, обратное умножению. Чтобы разделить 56 на 8, вспомни: на что умножить 8, чтобы получилось 56? " +
                "8 × 7 = 56, значит 56 : 8 = 7. Делить на ноль нельзя, а 0 : 5 = 0.",
            listOf(ex("63 : 9", "= 7, потому что 9 × 7 = 63"), ex("48 : 6", "= 8, потому что 6 × 8 = 48")),
        ) { r ->
            val a = r.between(2, 9).toLong(); val b = r.between(2, 9).toLong()
            input("${a * b} : $a = □", b, "Вспоминаем таблицу: $a × $b = ${a * b}, значит ${a * b} : $a = $b.")
        },
        MathTopic(
            "mult_out", 3, "Внетабличное умножение",
            "Двузначное число раскладываем на десятки и единицы и умножаем каждое слагаемое: " +
                "23 × 4 = 20 × 4 + 3 × 4 = 80 + 12 = 92.",
            listOf(ex("17 × 5", "= 10 × 5 + 7 × 5 = 50 + 35 = 85"), ex("4 × 24", "= 4 × 20 + 4 × 4 = 80 + 16 = 96")),
        ) { r ->
            val b = r.between(2, 6).toLong()
            var a: Long
            do { a = r.between(11, 99).toLong() } while (a % 10 == 0L || a * b >= 100)
            val t = a / 10 * 10; val u = a % 10
            val explain = "Разложим $a на $t и $u: $t × $b = ${t * b}, $u × $b = ${u * b}, ${t * b} + ${u * b} = ${a * b}."
            if (r.nextBoolean()) input("$a × $b = □", a * b, explain) else input("$b × $a = □", a * b, explain)
        },
        MathTopic(
            "div_out", 3, "Внетабличное деление",
            "Делимое раскладываем на удобные слагаемые, каждое из которых делится: 84 : 4 = (80 + 4) : 4 = 20 + 1 = 21. " +
                "Если делим на двузначное число — подбираем частное и проверяем умножением: 96 : 16 = 6, потому что 16 × 6 = 96.",
            listOf(ex("72 : 3", "= (60 + 12) : 3 = 20 + 4 = 24"), ex("85 : 17", "= 5, потому что 17 × 5 = 85")),
        ) { r ->
            if (r.nextBoolean()) {
                val b = r.between(2, 6).toLong()
                var q: Long
                do { q = r.between(11, 49).toLong() } while (q * b >= 100 || q % 10 == 0L)
                val a = q * b
                val t = q / 10 * 10 * b
                val u = a - t
                input("${a} : $b = □", q, "Разложим $a на $t и $u: $t : $b = ${t / b}, $u : $b = ${u / b}, ${t / b} + ${u / b} = $q.")
            } else {
                var b: Long; var q: Long
                do { b = r.between(12, 48).toLong(); q = r.between(2, 7).toLong() } while (b * q >= 100 || b % 10 == 0L)
                input("${b * q} : $b = □", q, "Подбираем частное: $b × $q = ${b * q}, значит ${b * q} : $b = $q.")
            }
        },
        MathTopic(
            "div_rem", 3, "Деление с остатком",
            "Если число не делится нацело, остаётся остаток. 47 : 5 — ближайшее меньшее число, которое делится на 5, это 45. " +
                "45 : 5 = 9, а 47 − 45 = 2. Записываем: 47 : 5 = 9 (ост. 2). Остаток всегда меньше делителя!",
            listOf(ex("29 : 4", "= 7 (ост. 1), потому что 4 × 7 = 28 и 29 − 28 = 1"), ex("50 : 6", "= 8 (ост. 2)")),
        ) { r ->
            val b = r.between(2, 9).toLong(); val q = r.between(2, 9).toLong(); val rest = r.between(1, b.toInt() - 1).toLong()
            val a = b * q + rest
            MathProblem(
                MathProblem.INPUT, "$a : $b = □", "$q ост. $rest",
                explain = "Ближайшее меньшее число, которое делится на $b, — это ${b * q} ($b × $q). $a − ${b * q} = $rest. " +
                    "Значит, $a : $b = $q (ост. $rest). Остаток $rest меньше делителя $b — всё верно.",
            )
        },
        MathTopic(
            "order_ops", 3, "Порядок действий",
            "Сначала выполняем действия в скобках, потом умножение и деление, потом сложение и вычитание — по порядку слева направо. " +
                "Например, 20 + 3 × 4: сначала 3 × 4 = 12, потом 20 + 12 = 32.",
            listOf(ex("(20 + 4) : 6", "1) 20 + 4 = 24; 2) 24 : 6 = 4"), ex("30 − 4 × 5", "1) 4 × 5 = 20; 2) 30 − 20 = 10")),
        ) { r ->
            when (r.nextInt(6)) {
                0 -> {
                    val a = r.between(10, 60).toLong(); val b = r.between(2, 9).toLong(); val c = r.between(2, 9).toLong()
                    input("$a + $b × $c = □", a + b * c, "Сначала умножение: $b × $c = ${b * c}. Потом сложение: $a + ${b * c} = ${a + b * c}.")
                }
                1 -> {
                    val a = r.between(2, 9).toLong(); val b = r.between(1, 6).toLong(); val c = r.between(2, 9).toLong()
                    input("($a + $b) × $c = □", (a + b) * c, "Сначала действие в скобках: $a + $b = ${a + b}. Потом умножение: ${a + b} × $c = ${(a + b) * c}.")
                }
                2 -> {
                    val c = r.between(2, 9).toLong(); val k = r.between(2, 9).toLong(); val a = r.between(k.toInt() + 5, 90).toLong()
                    input("$a − ${c * k} : $c = □", a - k, "Сначала деление: ${c * k} : $c = $k. Потом вычитание: $a − $k = ${a - k}.")
                }
                3 -> {
                    var a: Long; var b: Long; var c: Long; var d: Long
                    do { a = r.between(2, 9).toLong(); b = r.between(2, 9).toLong(); c = r.between(2, 9).toLong(); d = r.between(2, 9).toLong() } while (a * b <= c * d)
                    input("$a × $b − $c × $d = □", a * b - c * d, "Сначала оба умножения: $a × $b = ${a * b}, $c × $d = ${c * d}. Потом вычитание: ${a * b} − ${c * d} = ${a * b - c * d}.")
                }
                4 -> {
                    val c = r.between(2, 9).toLong(); val k = r.between(2, 9).toLong(); val b = r.between(1, 30).toLong()
                    val a = c * k + b
                    input("($a − $b) : $c = □", k, "Сначала действие в скобках: $a − $b = ${a - b}. Потом деление: ${a - b} : $c = $k.")
                }
                else -> {
                    var a: Long; var b: Long; var c: Long; var d: Long
                    do { a = r.between(20, 70).toLong(); b = r.between(5, 30).toLong(); c = r.between(2, 9).toLong(); d = r.between(2, 9).toLong() } while (a + b <= c * d)
                    input("$a + $b − $c × $d = □", a + b - c * d, "Сначала умножение: $c × $d = ${c * d}. Потом по порядку слева направо: $a + $b = ${a + b}, ${a + b} − ${c * d} = ${a + b - c * d}.")
                }
            }
        },
        MathTopic(
            "add_sub_1000", 3, "Сложение и вычитание в пределах 1000",
            "Трёхзначные числа складываем и вычитаем столбиком: единицы под единицами, десятки под десятками, сотни под сотнями. " +
                "Начинаем с единиц. Если получилось 10 и больше — один десяток переносим в следующий разряд; если не хватает — занимаем.",
            listOf(ex("458 + 276", "= 734 (8 + 6 = 14, пишем 4, 1 запоминаем; 5 + 7 + 1 = 13, пишем 3, 1 запоминаем; 4 + 2 + 1 = 7)"), ex("603 − 258", "= 345")),
        ) { r ->
            if (r.nextBoolean()) {
                val a = r.between(120, 680).toLong(); val b = r.between(110, (999 - a).toInt().coerceAtLeast(111)).toLong()
                input("${a} + ${b} = □", a + b, columnAdd(a, b))
            } else {
                val a = r.between(300, 999).toLong(); val b = r.between(105, (a - 50).toInt()).toLong()
                input("$a − $b = □", a - b, columnSub(a, b))
            }
        },
        MathTopic(
            "length_units", 3, "Единицы длины",
            "1 см = 10 мм, 1 дм = 10 см, 1 м = 10 дм = 100 см, 1 км = 1000 м. Чтобы перевести в более мелкие единицы — умножаем, " +
                "в более крупные — делим. 3 м 25 см = 300 см + 25 см = 325 см.",
            listOf(ex("4 дм 6 см = □ см", "40 см + 6 см = 46 см"), ex("2 км 300 м = □ м", "2000 м + 300 м = 2300 м")),
        ) { r ->
            when (r.nextInt(6)) {
                0 -> { val a = r.between(2, 9).toLong(); input("$a м = □ см", a * 100, "1 м = 100 см, значит $a м = $a × 100 = ${a * 100} см.") }
                1 -> { val a = r.between(1, 9).toLong(); val b = r.between(5, 95).toLong(); input("$a м $b см = □ см", a * 100 + b, "$a м = ${a * 100} см, и ещё $b см: ${a * 100} + $b = ${a * 100 + b} см.") }
                2 -> { val a = r.between(1, 9).toLong(); val b = r.between(1, 9).toLong(); input("$a дм $b см = □ см", a * 10 + b, "1 дм = 10 см, $a дм = ${a * 10} см, ${a * 10} + $b = ${a * 10 + b} см.") }
                3 -> { val a = r.between(1, 9).toLong(); val b = r.between(1, 9).toLong(); input("$a см $b мм = □ мм", a * 10 + b, "1 см = 10 мм, $a см = ${a * 10} мм, ${a * 10} + $b = ${a * 10 + b} мм.") }
                4 -> { val a = r.between(1, 9).toLong(); val b = r.between(50, 950).toLong() / 10 * 10; input("$a км $b м = □ м", a * 1000 + b, "1 км = 1000 м, $a км = ${a * 1000} м, ${a * 1000} + $b = ${a * 1000 + b} м.") }
                else -> { val a = r.between(2, 9).toLong(); input("${a * 10} см = □ дм", a, "1 дм = 10 см, значит ${a * 10} см : 10 = $a дм.") }
            }
        },
        MathTopic(
            "mass_time", 3, "Единицы массы и времени",
            "Масса: 1 кг = 1000 г, 1 ц = 100 кг, 1 т = 1000 кг. Время: 1 мин = 60 с, 1 ч = 60 мин, 1 сут. = 24 ч, 1 год = 12 месяцев. " +
                "2 ч 15 мин = 120 мин + 15 мин = 135 мин.",
            listOf(ex("3 кг 200 г = □ г", "3000 г + 200 г = 3200 г"), ex("3 ч = □ мин", "60 × 3 = 180 мин")),
        ) { r ->
            when (r.nextInt(7)) {
                0 -> { val a = r.between(1, 9).toLong(); val b = r.between(1, 9).toLong() * 100; input("$a кг $b г = □ г", a * 1000 + b, "1 кг = 1000 г, $a кг = ${a * 1000} г, ${a * 1000} + $b = ${a * 1000 + b} г.") }
                1 -> { val a = r.between(2, 9).toLong(); input("$a т = □ кг", a * 1000, "1 т = 1000 кг, значит $a т = ${a * 1000} кг.") }
                2 -> { val a = r.between(2, 9).toLong(); input("$a ц = □ кг", a * 100, "1 ц = 100 кг, значит $a ц = ${a * 100} кг.") }
                3 -> { val a = r.between(2, 9).toLong(); input("$a ч = □ мин", a * 60, "1 ч = 60 мин, значит $a ч = 60 × $a = ${a * 60} мин.") }
                4 -> { val a = r.between(1, 5).toLong(); val b = r.between(5, 55).toLong(); input("$a ч $b мин = □ мин", a * 60 + b, "$a ч = ${a * 60} мин, и ещё $b мин: ${a * 60} + $b = ${a * 60 + b} мин.") }
                5 -> { val a = r.between(2, 7).toLong(); input("$a сут. = □ ч", a * 24, "1 сут. = 24 ч, значит $a сут. = 24 × $a = ${a * 24} ч.") }
                else -> { val a = r.between(2, 5).toLong(); input("$a ${plural(a, "год", "года", "лет")} = □ мес.", a * 12, "1 год = 12 месяцев, значит 12 × $a = ${a * 12} ${plural(a * 12, "месяц", "месяца", "месяцев")}.") }
            }
        },
        MathTopic(
            "perimeter_area", 3, "Периметр и площадь",
            "Периметр — длина границы фигуры, сумма длин всех сторон. У прямоугольника P = (a + b) × 2, у квадрата P = a × 4. " +
                "Площадь — сколько места занимает фигура внутри, её считают в квадратных единицах: у прямоугольника S = a × b, у квадрата S = a × a.",
            listOf(ex("Прямоугольник 6 см и 4 см", "P = (6 + 4) × 2 = 20 см, S = 6 × 4 = 24 см²"), ex("Квадрат со стороной 5 дм", "P = 5 × 4 = 20 дм, S = 5 × 5 = 25 дм²")),
        ) { r ->
            val a = r.between(3, 9).toLong(); val b = r.between(2, 8).toLong()
            when (r.nextInt(6)) {
                0 -> input("Найди периметр прямоугольника со сторонами $a см и $b см.", (a + b) * 2, "P = (a + b) × 2 = ($a + $b) × 2 = ${a + b} × 2 = ${(a + b) * 2} см.", "см")
                1 -> input("Найди площадь прямоугольника со сторонами $a см и $b см.", a * b, "S = a × b = $a × $b = ${a * b} см².", "см²")
                2 -> input("Сторона квадрата $a дм. Найди его периметр.", a * 4, "У квадрата все стороны равны: P = $a × 4 = ${a * 4} дм.", "дм")
                3 -> input("Сторона квадрата $a м. Найди его площадь.", a * a, "S = a × a = $a × $a = ${a * a} м².", "м²")
                4 -> input("Площадь прямоугольника ${a * b} см², одна сторона $a см. Найди другую сторону.", b, "S = a × b, значит другая сторона = S : a = ${a * b} : $a = $b см.", "см")
                else -> input("Периметр квадрата ${a * 4} см. Найди его сторону.", a, "У квадрата 4 равные стороны: ${a * 4} : 4 = $a см.", "см")
            }
        },
        MathTopic(
            "fractions", 3, "Доли",
            "Доля — равная часть целого. Чтобы найти 1/4 числа, делим его на 4: 1/4 от 20 = 20 : 4 = 5. " +
                "Чтобы найти 3/4 — делим на 4 и умножаем на 3: 20 : 4 × 3 = 15. Если 1/5 числа равна 6, то число = 6 × 5 = 30.",
            listOf(ex("1/3 от 24", "24 : 3 = 8"), ex("2/5 от 30", "30 : 5 × 2 = 12")),
        ) { r ->
            val d = r.between(2, 9).toLong(); val k = r.between(2, 9).toLong()
            when (r.nextInt(4)) {
                0 -> input("Найди 1/$d от ${d * k}.", k, "Чтобы найти 1/$d, делим на $d: ${d * k} : $d = $k.")
                1 -> {
                    val m = if (d == 2L) 1L else r.between(2, (d - 1).toInt()).toLong()
                    input("Найди $m/$d от ${d * k}.", k * m, "Делим на $d и умножаем на $m: ${d * k} : $d = $k, $k × $m = ${k * m}.")
                }
                2 -> input("1/$d числа равна $k. Найди всё число.", d * k, "Если одна $d-я часть равна $k, то всё число в $d раз больше: $k × $d = ${k * d}.")
                else -> input("В классе ${d * k} учеников, 1/$d из них — отличники. Сколько отличников?", k, "Ищем 1/$d от ${d * k}: ${d * k} : $d = $k.", "чел.")
            }
        },
        MathTopic(
            "equations", 3, "Уравнения",
            "Чтобы найти неизвестное слагаемое, из суммы вычитают известное слагаемое. Неизвестное уменьшаемое — к разности прибавляют вычитаемое. " +
                "Неизвестное вычитаемое — из уменьшаемого вычитают разность. Неизвестный множитель — произведение делят на известный множитель. " +
                "Неизвестное делимое — частное умножают на делитель. Неизвестный делитель — делимое делят на частное. И обязательно делаем проверку!",
            listOf(ex("х + 25 = 60", "х = 60 − 25 = 35; проверка: 35 + 25 = 60"), ex("72 : х = 8", "х = 72 : 8 = 9; проверка: 72 : 9 = 8")),
        ) { r ->
            when (r.nextInt(6)) {
                0 -> { val x = r.between(12, 400).toLong(); val a = r.between(11, 300).toLong(); input("х + $a = ${x + a}", x, "Неизвестное слагаемое: из суммы вычитаем известное слагаемое. х = ${x + a} − $a = $x. Проверка: $x + $a = ${x + a}.") }
                1 -> { val x = r.between(30, 500).toLong(); val a = r.between(11, (x - 5).toInt()).toLong(); input("х − $a = ${x - a}", x, "Неизвестное уменьшаемое: к разности прибавляем вычитаемое. х = ${x - a} + $a = $x. Проверка: $x − $a = ${x - a}.") }
                2 -> { val a = r.between(50, 600).toLong(); val x = r.between(11, (a - 10).toInt()).toLong(); input("$a − х = ${a - x}", x, "Неизвестное вычитаемое: из уменьшаемого вычитаем разность. х = $a − ${a - x} = $x. Проверка: $a − $x = ${a - x}.") }
                3 -> { val x = r.between(2, 12).toLong(); val a = r.between(2, 9).toLong(); input("х × $a = ${x * a}", x, "Неизвестный множитель: произведение делим на известный множитель. х = ${x * a} : $a = $x. Проверка: $x × $a = ${x * a}.") }
                4 -> { val b = r.between(2, 9).toLong(); val a = r.between(2, 9).toLong(); input("х : $a = $b", a * b, "Неизвестное делимое: частное умножаем на делитель. х = $b × $a = ${a * b}. Проверка: ${a * b} : $a = $b.") }
                else -> { val x = r.between(2, 9).toLong(); val b = r.between(2, 9).toLong(); input("${x * b} : х = $b", x, "Неизвестный делитель: делимое делим на частное. х = ${x * b} : $b = $x. Проверка: ${x * b} : $x = $b.") }
            }
        },
        MathTopic(
            "price", 3, "Задачи: цена, количество, стоимость",
            "Стоимость = цена × количество. Цена = стоимость : количество. Количество = стоимость : цена. " +
                "Например, 4 тетради по 12 рублей стоят 12 × 4 = 48 рублей.",
            listOf(ex("Ручка стоит 15 руб. Сколько стоят 3 ручки?", "15 × 3 = 45 руб."), ex("5 альбомов стоят 100 руб. Сколько стоит один?", "100 : 5 = 20 руб.")),
        ) { r ->
            val item = ITEMS[r.nextInt(ITEMS.size)]
            val p = r.between(3, 25).toLong(); val n = r.between(2, 9).toLong()
            when (r.nextInt(3)) {
                0 -> input("Одна ${item.one} стоит $p руб. Сколько стоят $n ${item.n(n)}?", p * n, "Стоимость = цена × количество: $p × $n = ${p * n} руб.", "руб.")
                1 -> input("За $n ${item.n(n)} заплатили ${p * n} руб. Сколько стоит одна ${item.one}?", p, "Цена = стоимость : количество: ${p * n} : $n = $p руб.", "руб.")
                else -> input("Одна ${item.one} стоит $p руб. Сколько ${item.many} можно купить на ${p * n} руб.?", n, "Количество = стоимость : цена: ${p * n} : $p = $n.", "шт.")
            }
        },

        MathTopic(
            "big_numbers", 4, "Многозначные числа",
            "В многозначном числе разряды объединяются в классы по три: класс единиц (единицы, десятки, сотни) и класс тысяч " +
                "(единицы тысяч, десятки тысяч, сотни тысяч). В числе 584 302: 5 сотен тысяч, 8 десятков тысяч, 4 единицы тысяч, 3 сотни, 0 десятков, 2 единицы. " +
                "Сравниваем числа по разрядам слева направо. Увеличить в 10 раз — приписать справа один ноль.",
            listOf(ex("Сколько всего сотен в числе 4 560?", "45 сотен"), ex("72 000 : 100", "= 720 — убираем два нуля")),
        ) { r ->
            when (r.nextInt(5)) {
                0 -> {
                    val n = r.between(100_000, 999_999).toLong(); val place = r.nextInt(6)
                    var d = n; repeat(place) { d /= 10 }; d %= 10
                    input("Какая цифра стоит в разряде ${PLACES_GEN[place]} в числе ${fmt(n)}?", d, "Считаем разряды справа налево: единицы, десятки, сотни, единицы тысяч, десятки тысяч, сотни тысяч. В разряде ${PLACES_GEN[place]} — цифра $d.")
                }
                1 -> {
                    val n = r.between(1000, 99_999).toLong(); val tens = r.nextBoolean()
                    if (tens) input("Сколько всего десятков в числе ${fmt(n)}?", n / 10, "Отбрасываем единицы: в числе ${fmt(n)} всего ${fmt(n / 10)} десятков.")
                    else input("Сколько всего сотен в числе ${fmt(n)}?", n / 100, "Отбрасываем десятки и единицы: в числе ${fmt(n)} всего ${fmt(n / 100)} сотен.")
                }
                2 -> {
                    val th = r.between(1, 99).toLong(); val h = r.between(0, 9).toLong(); val t = r.between(0, 9).toLong(); val u = r.between(1, 9).toLong()
                    val parts = listOfNotNull(
                        "$th ${plural(th, "тысяча", "тысячи", "тысяч")}",
                        if (h > 0) "$h ${plural(h, "сотня", "сотни", "сотен")}" else null,
                        if (t > 0) "$t ${plural(t, "десяток", "десятка", "десятков")}" else null,
                        "$u ${plural(u, "единица", "единицы", "единиц")}",
                    )
                    val n = th * 1000 + h * 100 + t * 10 + u
                    input("Запиши цифрами: ${parts.joinToString(" ")}", n, "Записываем по разрядам, пустые разряды заполняем нулями: ${fmt(n)}.")
                }
                3 -> {
                    val a = r.between(10_900, 98_000).toLong(); var b = a + r.between(-900, 900); if (b == a) b += 90
                    val sa = a.toString(); val sb = b.toString()
                    val i = sa.indices.first { sa[it] != sb[it] }
                    val placeName = PLACES_GEN[sa.length - 1 - i]
                    compare("Сравни: ${fmt(a)} ○ ${fmt(b)}", a, b,
                        "Сравниваем по разрядам слева направо. Первое отличие — в разряде $placeName: ${sa[i]} ${sign(sa[i].code.toLong(), sb[i].code.toLong())} ${sb[i]}, значит ${fmt(a)} ${sign(a, b)} ${fmt(b)}.")
                }
                else -> {
                    val k = r.between(12, 980).toLong()
                    if (r.nextBoolean()) {
                        val m = if (r.nextBoolean()) 10L else 100L
                        input("Увеличь ${fmt(k)} в $m раз.", k * m, "Чтобы увеличить в $m раз, приписываем справа ${if (m == 10L) "один ноль" else "два нуля"}: ${fmt(k * m)}.")
                    } else {
                        val m = if (r.nextBoolean()) 10L else 100L
                        input("Уменьши ${fmt(k * m)} в $m раз.", k, "Чтобы уменьшить в $m раз, убираем справа ${if (m == 10L) "один ноль" else "два нуля"}: ${fmt(k)}.")
                    }
                }
            }
        },
        MathTopic(
            "add_sub_big", 4, "Сложение и вычитание многозначных чисел",
            "Многозначные числа складываем и вычитаем столбиком, как трёхзначные: разряд под разрядом, начиная с единиц. " +
                "Не забывай про перенос и про «занимаем у соседнего разряда».",
            listOf(ex("4 586 + 2 397", "= 6 983"), ex("10 000 − 3 456", "= 6 544")),
        ) { r ->
            if (r.nextBoolean()) {
                val a = r.between(1200, 48_000).toLong(); val b = r.between(1100, 48_000).toLong()
                input("${fmt(a)} + ${fmt(b)} = □", a + b, columnAdd(a, b))
            } else {
                val a = r.between(5000, 90_000).toLong(); val b = r.between(1000, (a - 500).toInt()).toLong()
                input("${fmt(a)} − ${fmt(b)} = □", a - b, columnSub(a, b))
            }
        },
        MathTopic(
            "mult_big", 4, "Умножение многозначных чисел",
            "На однозначное число умножаем столбиком: каждую цифру, начиная с единиц, не забывая прибавлять то, что запомнили. " +
                "Круглые числа умножаем без нулей, а потом приписываем столько нулей, сколько их в обоих множителях: 120 × 30 → 12 × 3 = 36 → 3600.",
            listOf(ex("234 × 3", "= 702"), ex("40 × 60", "4 × 6 = 24, приписываем два нуля: 2400")),
        ) { r ->
            if (r.nextInt(3) != 0) {
                val a = r.between(102, 2999).toLong(); val b = r.between(2, 9).toLong()
                input("${fmt(a)} × $b = □", a * b, columnMul(a, b))
            } else {
                var a: Long
                do { a = r.between(2, 25).toLong() } while (a % 10 == 0L)
                val b = r.between(2, 9).toLong()
                val za = if (r.nextBoolean()) 10L else 100L
                val x = a * za; val y = b * 10
                val zeros = (if (za == 10L) 1 else 2) + 1
                input("${fmt(x)} × $y = □", x * y, "Умножаем без нулей: $a × $b = ${a * b}. Нулей в множителях всего $zeros — приписываем их: ${fmt(x * y)}.")
            }
        },
        MathTopic(
            "div_big", 4, "Деление на однозначное число",
            "Делим уголком: находим первое неполное делимое, делим, умножаем, вычитаем и сносим следующую цифру. " +
                "Сколько цифр в частном — столько, сколько разрядов осталось после первого неполного делимого плюс один. Проверяем умножением.",
            listOf(ex("972 : 4", "9 : 4 = 2 (ост. 1); 17 : 4 = 4 (ост. 1); 12 : 4 = 3; ответ 243"), ex("4800 : 6", "48 : 6 = 8, приписываем два нуля: 800")),
        ) { r ->
            if (r.nextInt(4) != 0) {
                val b = r.between(2, 9).toLong(); val q = r.between(105, 999).toLong()
                input("${fmt(q * b)} : $b = □", q, longDivision(q * b, b))
            } else {
                val b = r.between(2, 9).toLong(); val q = r.between(2, 9).toLong(); val z = if (r.nextBoolean()) 10L else 100L
                input("${fmt(q * b * z)} : $b = □", q * z, "Делим без нулей: ${q * b} : $b = $q, и приписываем нули: ${fmt(q * z)}.")
            }
        },
        MathTopic(
            "speed", 4, "Скорость, время, расстояние",
            "Расстояние = скорость × время (S = v × t). Скорость = расстояние : время (v = S : t). Время = расстояние : скорость (t = S : v). " +
                "Когда двое движутся навстречу, их скорости складываются — это скорость сближения.",
            listOf(ex("Автобус ехал 3 ч со скоростью 60 км/ч", "S = 60 × 3 = 180 км"), ex("Пешеход прошёл 12 км за 3 ч", "v = 12 : 3 = 4 км/ч")),
        ) { r ->
            when (r.nextInt(4)) {
                0 -> { val v = r.between(10, 20).toLong(); val t = r.between(2, 5).toLong(); input("Велосипедист ехал $t ч со скоростью $v км/ч. Какое расстояние он проехал?", v * t, "S = v × t = $v × $t = ${v * t} км.", "км") }
                1 -> { val v = r.between(40, 90).toLong() / 10 * 10; val t = r.between(2, 6).toLong(); input("Поезд прошёл ${v * t} км за $t ч. С какой скоростью он ехал?", v, "v = S : t = ${v * t} : $t = $v км/ч.", "км/ч") }
                2 -> { val v = r.between(3, 6).toLong(); val t = r.between(2, 5).toLong(); input("Пешеход идёт со скоростью $v км/ч. За сколько часов он пройдёт ${v * t} км?", t, "t = S : v = ${v * t} : $v = $t ч.", "ч") }
                else -> {
                    val a = r.between(3, 6).toLong(); val b = r.between(3, 6).toLong(); val t = r.between(2, 4).toLong()
                    input("Из двух сёл навстречу друг другу одновременно вышли два пешехода. Скорость одного $a км/ч, другого $b км/ч. Через $t ч они встретились. Какое расстояние между сёлами?",
                        (a + b) * t, "Скорость сближения: $a + $b = ${a + b} км/ч. Расстояние: ${a + b} × $t = ${(a + b) * t} км.", "км")
                }
            }
        },
        MathTopic(
            "compare", 4, "Сравнение величин и выражений",
            "Чтобы сравнить величины, переводим их в одинаковые единицы: 3 м 5 см = 305 см, 2 ч = 120 мин. " +
                "Чтобы сравнить выражения, сначала находим их значения.",
            listOf(ex("4 кг ○ 3900 г", "4 кг = 4000 г, 4000 > 3900, значит 4 кг > 3900 г"), ex("45 × 2 ○ 100 − 9", "90 < 91")),
        ) { r ->
            when (r.nextInt(4)) {
                0 -> {
                    val m = r.between(1, 9).toLong(); val c = r.between(1, 9).toLong(); val v = m * 100 + c
                    val other = v + listOf(0L, 10L, -10L, 90L, -90L)[r.nextInt(5)].let { if (v + it <= 0) 0 else it }
                    compare("Сравни: $m м $c см ○ $other см", v, other, "Переводим в сантиметры: $m м $c см = $v см. $v ${sign(v, other)} $other, значит $m м $c см ${sign(v, other)} $other см.")
                }
                1 -> {
                    val h = r.between(1, 4).toLong(); val other = h * 60 + listOf(0L, 20L, -20L, 40L)[r.nextInt(4)]
                    compare("Сравни: $h ч ○ $other мин", h * 60, other, "$h ч = ${h * 60} мин. ${h * 60} ${sign(h * 60, other)} $other.")
                }
                2 -> {
                    val k = r.between(2, 8).toLong(); val other = k * 1000 + listOf(0L, 100L, -100L, 900L)[r.nextInt(4)]
                    compare("Сравни: $k кг ○ ${fmt(other)} г", k * 1000, other, "$k кг = ${fmt(k * 1000)} г. ${fmt(k * 1000)} ${sign(k * 1000, other)} ${fmt(other)}.")
                }
                else -> {
                    val a = r.between(11, 49).toLong(); val b = r.between(2, 4).toLong(); val p = a * b
                    val c = (p + listOf(0, 1, -1, 5, -5)[r.nextInt(5)] + 9).coerceAtLeast(10); val d = 9L
                    compare("Сравни: $a × $b ○ $c − $d", p, c - d, "Находим значения: $a × $b = $p, $c − $d = ${c - d}. $p ${sign(p, c - d)} ${c - d}.")
                }
            }
        },
    )

    fun topic(id: String): MathTopic? = topics.firstOrNull { it.id == id }

    /** Список тем для нейросети: id — название. */
    fun catalog(): String = topics.joinToString("\n") { "${it.id} — ${it.title} (${it.grade} класс)" }
}

/** Наборы задач для тренировки. */
object MathSession {
    /** Тренировка темы. */
    fun topic(id: String, n: Int = 10, r: Random = Random.Default): List<MathProblem> = MathBank.topic(id)?.problems(n, r).orEmpty()

    /** Урок по фото: до 8 задач со страницы (каждый раз другие) и 3 на повторение связанных тем. */
    fun lesson(problems: List<MathProblem>, topics: List<String>, r: Random = Random.Default): List<MathProblem> {
        val review = topics.flatMap { topic(it, 2, r) }.shuffled(r).take(3)
        return problems.shuffled(r).take(8) + review
    }

    /** Повторение тем, которые пора освежить. */
    fun review(dueTopics: List<String>, r: Random = Random.Default): List<MathProblem> =
        dueTopics.shuffled(r).take(5).flatMap { topic(it, 2, r) }.shuffled(r).take(10)
}
