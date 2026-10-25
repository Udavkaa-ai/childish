package ru.sovushka.core

import ru.sovushka.core.ai.MathLessonGenerator
import ru.sovushka.core.math.MathBank
import ru.sovushka.core.math.MathCheck
import ru.sovushka.core.math.MathEval
import ru.sovushka.core.math.MathProblem
import ru.sovushka.core.math.MathSession
import ru.sovushka.core.math.MathSpeech
import ru.sovushka.core.math.fmt
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MathTest {
    private fun p(prompt: String, answer: String, kind: String = MathProblem.INPUT) =
        MathProblem(kind, prompt, answer, options = if (kind == MathProblem.CHOOSE) MathProblem.SIGNS else emptyList())

    @Test
    fun everyGeneratedProblemIsCorrect() {
        assertTrue(MathBank.topics.size >= 18)
        assertEquals(MathBank.topics.size, MathBank.topics.map { it.id }.toSet().size)
        MathBank.topics.forEach { t ->
            val r = Random(t.id.hashCode())
            repeat(300) {
                val q = t.problem(r)
                assertTrue(q.isValid(), "${t.id}: $q")
                assertEquals(t.id, q.topic)
                assertTrue(q.explain.isNotBlank(), t.id)
                assertTrue(MathEval.verify(q) != false, "${t.id}: неверный ответ в $q")
                val n = MathCheck.normalize(q.answer)
                if (q.kind == MathProblem.INPUT) {
                    assertTrue(n.matches(Regex("\\d+(r\\d+)?")), "${t.id}: ответ не набрать на клавиатуре: ${q.answer}")
                    // Объяснение должно приходить к тому же ответу.
                    if ('r' !in n) assertTrue(q.explain.contains(q.answer) || q.explain.contains(fmt(q.answer.toLong())), "${t.id}: $q")
                } else {
                    assertTrue(q.answer in MathProblem.SIGNS, t.id)
                }
            }
            assertEquals(10, t.problems(10, Random(1)).size, t.id)
        }
    }

    @Test
    fun evaluatorAndChecks() {
        assertEquals(14L, MathEval.eval("2 + 3 × 4"))
        assertEquals(4L, MathEval.eval("(20 + 4) : 6"))
        assertEquals(12350L, MathEval.eval("12 345 + 5"))
        assertEquals(12350L, MathEval.eval("12 345 + 5"))
        assertNull(MathEval.eval("7 : 2"))
        assertNull(MathEval.eval("три + 2"))

        assertEquals(true, MathEval.verify(p("48 : 6 = ?", "8")))
        assertEquals(false, MathEval.verify(p("48 : 6 = ?", "9")))
        assertEquals(true, MathEval.verify(p("Вычисли: 47 : 5 = □", "9 ост. 2")))
        assertEquals(false, MathEval.verify(p("47 : 5 = □", "9 ост. 3")))
        assertEquals(true, MathEval.verify(p("х + 25 = 60", "35")))
        assertEquals(false, MathEval.verify(p("х + 25 = 60", "30")))
        assertEquals(true, MathEval.verify(p("72 : х = 8", "9")))
        assertEquals(false, MathEval.verify(p("72 : х = 8", "7")))
        assertEquals(true, MathEval.verify(p("7 × □ = 56", "8")))
        assertEquals(true, MathEval.verify(p("Сравни: 45 × 2 ○ 100 − 9", "<", MathProblem.CHOOSE)))
        assertEquals(false, MathEval.verify(p("Сравни: 45 × 2 ○ 100 − 9", ">", MathProblem.CHOOSE)))
        assertNull(MathEval.verify(p("Тетрадь стоит 12 руб. Сколько стоят 4 тетради?", "48")))
        assertNull(MathEval.verify(p("3 м 25 см = □ см", "325")))

        assertTrue(MathCheck.same("9 (ост.2)", "9 ост. 2"))
        assertTrue(MathCheck.same("0305", "305"))
        assertTrue(MathCheck.same("305", "305 см"))
        assertTrue(MathCheck.same("12 345", "12345"))
        assertFalse(MathCheck.same("9 ост. 1", "9 ост. 2"))
        assertFalse(MathCheck.same("", "5"))
        assertEquals(listOf("ост."), MathCheck.extraKeys("9 ост. 2"))
    }

    @Test
    fun speechReadsMathAloud() {
        assertEquals("47 разделить на 5 равно сколько", MathSpeech.forSpeech("47 : 5 = □"))
        assertEquals("Найди одну четвёртую от 20.", MathSpeech.forSpeech("Найди 1/4 от 20."))
        assertEquals("Найди одну третью от 9.", MathSpeech.forSpeech("Найди 1/3 от 9."))
        assertEquals("одна пятая числа", MathSpeech.forSpeech("1/5 числа"))
        assertEquals("5 м 30 см равно сколько сантиметров", MathSpeech.forSpeech("5 м 30 см = □ см"))
        assertEquals("2 года равно сколько месяцев", MathSpeech.forSpeech("2 года = □ мес."))
        assertTrue(MathSpeech.forSpeech("3/4").contains("три четвёртых"))
        assertEquals("12345 минус 5", MathSpeech.forSpeech("12 345 − 5"))
        assertTrue(MathSpeech.forSpeech("S = 6 × 4 = 24 см².").contains("квадратных сантиметров"))
        assertEquals("икс плюс 25 равно 60", MathSpeech.forSpeech("х + 25 = 60"))
        assertEquals("9 остаток 2", MathSpeech.forSpeech("9 ост. 2"))
    }

    @Test
    fun columnExplanations() {
        assertTrue(MathBank.columnAdd(458, 276).endsWith("Ответ: 734."))
        val sub = MathBank.columnSub(603, 258)
        assertTrue(sub.contains("занимаем") && sub.endsWith("Ответ: 345."), sub)
        val div = MathBank.longDivision(972, 4)
        assertTrue(div.contains("17 : 4 = 4, остаток 1") && div.endsWith("Ответ: 243."), div)
        assertTrue(MathBank.columnMul(234, 3).endsWith("Ответ: 702."))
        assertEquals("30 485", fmt(30485L))
        assertEquals("4560", fmt(4560L))
    }

    @Test
    fun llmLessonIsCheckedAndCleaned() {
        val raw = """
            Вот урок: {"title":"Деление на однозначное число","rule":"Делим уголком.",
             "examples":[{"problem":"972 : 4","solution":"243"}],
             "problems":[
              {"kind":"input","prompt":"972 : 4 = □","answer":"243","explain":"…","topic":"div_big"},
              {"kind":"input","prompt":"856 : 4 = ...","answer":"213","explain":"ошибка в счёте"},
              {"kind":"input","prompt":"Поезд ехал 3 ч со скоростью 60 км/ч. Какое расстояние он прошёл?","answer":"180 км","unit":"км","topic":"speed"},
              {"kind":"input","prompt":"Сколько будет?","answer":"много"},
              {"kind":"choose","prompt":"Сравни: 4 км ○ 3900 м","options":["<","=",">"],"answer":">"},
              {"kind":"input","prompt":"12 x 3 = □","answer":"36"}
             ],
             "topics":["div_big","нет такой"]}
        """.trimIndent()
        val l = assertNotNull(MathLessonGenerator.parse(raw))
        val ps = l.validProblems()
        assertEquals(listOf("972 : 4 = □", "Поезд ехал 3 ч со скоростью 60 км/ч. Какое расстояние он прошёл?", "Сравни: 4 км ○ 3900 м", "12 × 3 = □"), ps.map { it.prompt })
        assertEquals("км", ps[1].unit)
        assertTrue(MathCheck.same("180", ps[1].answer))
        assertEquals(listOf("div_big"), l.topics)
        assertEquals(1, l.validExamples().size)
        assertTrue(MathLessonGenerator.systemPrompt(10).contains("div_rem"))
    }

    @Test
    fun sessionsAndBadProblems() {
        assertEquals(10, MathSession.topic("mult_table", 10, Random(3)).size)
        val lessonProblems = listOf(p("2 + 2 = □", "4"), p("3 + 3 = □", "6"))
        val s = MathSession.lesson(lessonProblems, listOf("equations", "speed"), Random(4))
        assertEquals(6, s.size)
        assertTrue(MathSession.review(listOf("div_rem", "speed"), Random(5)).size in 4..6)

        val data = AppData(mathLessons = listOf(MathLesson("m1", "t", 0, problems = lessonProblems)))
        val clean = data.withoutMath(lessonProblems[0])
        assertEquals(1, clean.mathLessons.single().problems.size)
        assertTrue(lessonProblems[0].key in clean.badMath)
    }
}
