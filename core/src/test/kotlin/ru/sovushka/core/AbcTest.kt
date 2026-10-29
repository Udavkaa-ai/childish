package ru.sovushka.core

import ru.sovushka.core.abc.Abc
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AbcTest {
    @Test
    fun alphabetIsComplete() {
        assertEquals(33, Abc.COUNT)
        assertEquals("АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯ".toSet(), Abc.LETTERS.map { it.upper }.toSet())
        assertEquals(listOf('А', 'У', 'О', 'М'), Abc.LETTERS.take(4).map { it.upper })
        Abc.LETTERS.forEach { l ->
            assertTrue(l.name.isNotBlank() && l.hear.isNotBlank() && l.look.isNotBlank(), "${l.upper}")
            l.pics.forEach { p ->
                val ok = if (l.upper == 'Ы' || l.kind == Abc.Kind.SIGN) l.lower in p.word.lowercase() else p.word[0].uppercaseChar() == l.upper
                assertTrue(ok, "${l.upper}: ${p.word}")
            }
            if (l.kind != Abc.Kind.SIGN) assertTrue(l.pics.isNotEmpty(), "${l.upper}")
        }
    }

    @Test
    fun wordsAndSyllables() {
        assertEquals(Abc.WORDS.size, Abc.WORDS.map { it.word }.toSet().size)
        Abc.WORDS.forEach { w ->
            assertTrue(w.word.all { Abc.indexOf(it) >= 0 }, w.word)
            assertEquals(w.word, Abc.syllables(w.word).joinToString(""))
        }
        assertEquals(listOf("ма", "ма"), Abc.syllables("мама"))
        assertEquals(listOf("ко", "шка"), Abc.syllables("кошка"))
        assertEquals(listOf("сум", "ка"), Abc.syllables("сумка"))
        assertEquals(listOf("лам", "па"), Abc.syllables("лампа"))
        assertEquals(listOf("май", "ка"), Abc.syllables("майка"))
        assertEquals(listOf("кот"), Abc.syllables("кот"))
        assertEquals(listOf("я", "бло", "ко"), Abc.syllables("яблоко"))
        // На уроке М уже можно прочитать «мама», но ещё нельзя «сом».
        val m = Abc.readable(4).map { it.word }
        assertTrue("мама" in m && "сом" !in m, "$m")
        assertTrue(Abc.readable(Abc.COUNT).size == Abc.WORDS.size)
        assertEquals(listOf('А', 'О', 'У'), Abc.vowelsFor('М', 4).sorted())
        assertTrue('Ы' !in Abc.vowelsFor('Ш', 20) && 'И' in Abc.vowelsFor('Ш', 20))
    }

    @Test
    fun everyLessonIsPlayable() {
        for (i in 0 until Abc.COUNT) {
            repeat(5) { seed ->
                val tasks = Abc.lesson(i, Random(seed * 31 + i))
                assertTrue(tasks.first() is Abc.Task.Meet, "$i")
                assertTrue(tasks.size >= 4, "$i: ${tasks.size}")
                tasks.forEach { t ->
                    when (t) {
                        is Abc.Task.FindLetter -> assertTrue(t.target in t.options && t.options.toSet().size == t.options.size, "$i")
                        is Abc.Task.FirstSound -> assertTrue(t.right in t.options && t.options.map { it.emoji }.toSet().size == 3, "$i")
                        is Abc.Task.FindSyllable -> assertTrue(t.target in t.options && t.options.toSet().size == t.options.size && t.options.size >= 2, "$i ${t.options}")
                        is Abc.Task.ReadWord -> {
                            assertTrue(t.word in t.options && t.options.map { it.emoji }.toSet().size == t.options.size, "$i")
                            assertTrue(Abc.unlockOf(t.word.word) <= i, "$i: ${t.word.word}")
                        }
                        is Abc.Task.Merge -> assertTrue(Abc.indexOf(t.vowel) <= i && Abc.indexOf(t.consonant.upper) <= i, "$i")
                        else -> {}
                    }
                }
            }
        }
        for (k in 1..Abc.COUNT) assertEquals(10, Abc.review(k, Random(k)).size)
    }
}
