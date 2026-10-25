package ru.sovushka.core

import ru.sovushka.core.ai.DrillGenerator
import ru.sovushka.core.ai.LessonGenerator
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GrammarTest {
    @Test
    fun bankIsConsistent() {
        val topics = GrammarBank.topics
        assertEquals(18, topics.size)
        assertEquals(topics.size, topics.map { it.id }.toSet().size)
        assertEquals(setOf(2, 3, 4), topics.map { it.grade }.toSet())
        topics.forEach { t ->
            assertTrue(t.drills.size >= 5, t.id)
            assertTrue(t.examples.isNotEmpty(), t.id)
            t.drills.forEach { d ->
                assertTrue(d.isValid(), "${t.id}: $d")
                assertEquals(t.id, d.topic)
                when (d.kind) {
                    Drill.GAP, Drill.CHOOSE -> assertEquals(d.options.size, d.options.toSet().size, "${t.id}: повтор варианта $d")
                    Drill.ORDER -> d.extra.forEach { e -> assertFalse(d.answerTokens.any { it.equals(e, true) }, "${t.id}: лишнее слово есть в ответе $d") }
                }
            }
        }
    }

    @Test
    fun orderCheck() {
        val d = GrammarBank.topic("to_be")!!.drills.first { it.kind == Drill.ORDER }
        assertTrue(SessionBuilder.orderCorrect(d, d.answerTokens))
        assertFalse(SessionBuilder.orderCorrect(d, d.answerTokens.reversed()))
        val c = Drill(Drill.ORDER, "Он не умеет плавать", answer = "He can't swim.")
        assertTrue(SessionBuilder.orderCorrect(c, listOf("he", "can't", "swim")))
        assertEquals("She is my best friend", GrammarBank.topic("to_be")!!.drills.first { it.kind == Drill.ORDER }.fullSentence)
        assertEquals("I am ten years old.", GrammarBank.topic("to_be")!!.drills.first().fullSentence)
    }

    @Test
    fun sessionsMixGrammar() {
        val words = (1..6).map { Word("$it", "l", "w$it", "с$it") }
        val drills = GrammarBank.topic("present_simple")!!.drills
        val review = GrammarBank.topic("have_got")!!.drills
        val tasks = SessionBuilder.lesson(words, emptyList(), words, canSpeak = false, Random(1), drills, review)
        val g = tasks.filterIsInstance<Task.Grammar>()
        assertEquals(SessionBuilder.LESSON_DRILLS, g.size)
        assertEquals(3, g.count { it.drill.topic == "have_got" })
        g.filter { it.drill.kind == Drill.ORDER }.forEach { assertEquals((it.drill.answerTokens + it.drill.extra).sorted(), it.tiles.sorted()) }
        g.filter { it.drill.kind != Drill.ORDER }.forEach { assertTrue(it.drill.answer in it.choices) }
        // На каждое слово — по два задания (с грамматикой урок не должен раздуваться).
        assertEquals(12, tasks.count { it.word != null && it !is Task.Intro })
        val r = SessionBuilder.review(emptyList(), words, false, Random(2), GrammarBank.topic("can")!!.drills)
        assertEquals(6, r.size)
    }

    @Test
    fun topicStatsAndDue() {
        val s = TopicStats().review(true, 100).review(true, 101)
        assertEquals(2, s.box); assertEquals(103, s.due)
        val d = AppData(grammar = mapOf("can" to s, "to_be" to TopicStats(box = 1, due = 200), "a_an" to TopicStats()))
        assertEquals(listOf("can"), d.dueTopics(103))
    }

    @Test
    fun parsesGeneratedDrills() {
        val raw = """
            {"title":"Семья","words":[{"en":"mother","ru":"мама"}],
             "drills":[
              {"kind":"gap","prompt":"She .... got a brother.","options":["has","have","is"],"answer":"has","explain":"she — has","topic":"have_got"},
              {"kind":"choose","options":["He likes dogs.","He like dogs."],"answer":"He likes dogs.","topic":"нет такой"},
              {"kind":"order","prompt":"У меня есть сестра.","answer":"I have got a sister.","extra":["has","sister","two words"]},
              {"kind":"gap","prompt":"No gap here","options":["a","b"],"answer":"a"},
              {"kind":"choose","options":["A","B"],"answer":"C"}
             ],
             "topics":["have_got","unknown","have_got"]}
        """.trimIndent()
        val l = assertNotNull(LessonGenerator.parse(raw))
        val d = l.validDrills()
        assertEquals(3, d.size)
        assertEquals("She ___ got a brother.", d[0].prompt)
        assertEquals("", d[1].topic, "неизвестная тема отбрасывается")
        assertEquals(listOf("has"), d[2].extra, "лишние слова из ответа и фразы убираются")
        assertEquals(listOf("have_got"), l.topics)
        assertTrue(LessonGenerator.systemPrompt(10).contains("present_simple"))
        assertEquals(1, DrillGenerator.parse("""Вот: {"drills":[{"kind":"gap","prompt":"I ___ ten.","options":["am","is"],"answer":"am"}]}""").size)
    }
}
