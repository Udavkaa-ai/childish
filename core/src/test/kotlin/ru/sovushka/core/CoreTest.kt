package ru.sovushka.core

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import ru.sovushka.core.ai.ChatMessage
import ru.sovushka.core.ai.GigaChatClient
import ru.sovushka.core.ai.LessonGenerator
import ru.sovushka.core.ai.LlmClient
import ru.sovushka.core.ai.YandexGptClient
import ru.sovushka.core.ai.defaultHttpClient
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CoreTest {
    private fun word(id: String, en: String, ru: String, box: Int = 0, due: Long = 0) =
        Word(id = id, lessonId = "l", en = en, ru = ru, box = box, due = due)

    @Test
    fun srsPromotesAndDemotes() {
        val w = word("1", "cat", "кошка")
        val a = Srs.review(w, true, 100)
        assertEquals(1, a.box); assertEquals(101, a.due)
        val b = Srs.review(a.copy(box = 5), true, 100)
        assertEquals(6, b.box); assertEquals(130, b.due)
        val c = Srs.review(b, false, 100)
        assertEquals(1, c.box); assertEquals(101, c.due); assertEquals(1, c.wrong)
        assertFalse(Srs.isDue(w, 100), "новое слово не попадает в повторение")
        assertTrue(Srs.isDue(c, 101))
    }

    @Test
    fun streak() {
        val s = KidStats().addSession(10, 3).addSession(10, 2).addSession(11, 1)
        assertEquals(2, s.streak); assertEquals(6, s.stars)
        assertEquals(1, s.addSession(15, 1).streak)
        assertEquals(0, s.visibleStreak(13))
    }

    @Test
    fun textMatch() {
        assertTrue(TextMatch.typedCorrect("Ice cream", "ice  cream!"))
        assertTrue(TextMatch.typedCorrect("a dog", "dog"))
        assertTrue(TextMatch.spokenMatch("two", listOf("2")))
        assertTrue(TextMatch.spokenMatch("dog", listOf("a dog")))
        assertTrue(TextMatch.spokenMatch("elephant", listOf("elefant")))
        assertFalse(TextMatch.spokenMatch("cat", listOf("dog")))
        assertEquals("ежик", TextMatch.normalize("Ёжик!"))
    }

    @Test
    fun speechSplit() {
        val seg = SpeechText.split("Кошка 🐱 по-английски — cat [kæt]. Скажи: cat!")
        assertEquals(listOf(SpeechText.Lang.RU, SpeechText.Lang.EN, SpeechText.Lang.RU, SpeechText.Lang.EN), seg.map { it.lang })
        assertEquals("cat", seg[1].text.trimEnd('.'))
        assertEquals("Hi", SpeechText.clean("**Hi** 😀"))
    }

    @Test
    fun sessionBuilder() {
        val words = (1..8).map { word("$it", "word$it", "слово$it", box = if (it > 4) 2 else 0) }
        val tasks = SessionBuilder.lesson(words, listOf(Sentence("I like apples.", "Я люблю яблоки.")), words, canSpeak = true, Random(1))
        val intro = tasks.filterIsInstance<Task.Intro>()
        assertEquals(4, intro.size)
        assertTrue(tasks.take(4).all { it is Task.Intro })
        val drilled = tasks.mapNotNull { it.word }.toSet()
        assertEquals(6, drilled.size)
        // У каждого слова есть задание на правописание.
        drilled.forEach { w -> assertTrue(tasks.any { it is Task.Spell && it.word == w }) }
        tasks.filterIsInstance<Task.ListenPick>().forEach { t ->
            assertEquals(4, t.options.size); assertTrue(t.word.ru in t.options)
        }
        val build = tasks.filterIsInstance<Task.Build>().single()
        assertEquals(listOf("I", "like", "apples").sorted(), build.tokens.sorted())
        // Если слов мало — варианты добираются из встроенного словаря.
        val opts = SessionBuilder.options("кошка", listOf("кошка"), Random(2))
        assertEquals(4, opts.size); assertEquals(4, opts.toSet().size)
        assertEquals("tshirt", SessionBuilder.spellTarget("T-shirt"))
    }

    @Test
    fun reviewSession() {
        val due = (1..12).map { word("$it", "w$it", "с$it", box = 2, due = it.toLong()) }
        val tasks = SessionBuilder.review(due, due, canSpeak = false, random = Random(3))
        assertEquals(20, tasks.size)
        assertTrue(tasks.none { it is Task.Say })
        tasks.zipWithNext().forEach { (a, b) -> assertTrue(a.word != b.word) }
    }

    @Test
    fun builtinPacks() {
        assertTrue(Builtin.packs.size >= 10)
        Builtin.packs.forEach { p -> assertTrue(p.words.size >= 8, p.id); assertEquals(p.words.size, p.words.distinctBy { it.en }.size) }
    }

    @Test
    fun parseLessonJson() {
        val raw = """
            Вот урок:
            ```json
            {"title":" Еда ","grammar":"Мы говорим I like ...","words":[
              {"en":"apple","ru":"яблоко","tr":"/ˈæpl/","emoji":"🍎","example":"I like apples.","example_ru":"Я люблю яблоки."},
              {"en":"Apple","ru":"яблоко"},
              {"en":"","ru":"пусто"},
              {"en":"молоко","ru":"молоко"},
              {"en":"milk","ru":"молоко","extra":1}
            ],"sentences":[{"en":"I like milk.","ru":"Я люблю молоко."}]}
            ```
        """.trimIndent()
        val l = assertNotNull(LessonGenerator.parse(raw))
        assertEquals("Еда", l.title)
        assertEquals(listOf("apple", "milk"), l.words.map { it.en })
        assertEquals("[ˈæpl]", l.words[0].tr)
        assertEquals("Я люблю яблоки.", l.words[0].exampleRu)
        assertNull(LessonGenerator.parse("не знаю"))
        assertNull(LessonGenerator.parse("""{"title":"x","words":[]}"""))
    }

    @Test
    fun generatorRetriesOnBadJson() = runBlocking {
        var calls = 0
        val llm = object : LlmClient {
            override val name = "fake"
            override suspend fun chat(messages: List<ChatMessage>, temperature: Double, maxTokens: Int): String {
                calls++
                return if (calls == 1) "Извините" else """{"title":"T","words":[{"en":"cat","ru":"кошка"}]}"""
            }
        }
        val l = LessonGenerator(llm).fromPage("This is a cat. The cat is black.", "стр. 5", 10)
        assertEquals(2, calls); assertEquals("cat", l.words.single().en)
    }

    @Test
    fun gigaChatProtocol() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody("""{"access_token":"tok","expires_at":${System.currentTimeMillis() + 1_800_000}}"""))
            server.enqueue(MockResponse().setResponseCode(404))
            server.enqueue(MockResponse().setBody("""{"choices":[{"message":{"role":"assistant","content":" Привет! "}}]}"""))
            server.enqueue(MockResponse().setBody("""{"choices":[{"message":{"role":"assistant","content":"Ещё"}}]}"""))
            val c = GigaChatClient("a2V5", http = defaultHttpClient(), oauthUrl = server.url("/oauth").toString(), apiUrls = listOf(server.url("/new/v1").toString().trimEnd('/'), server.url("/api/v1").toString().trimEnd('/')))
            assertEquals("Привет!", c.chat(listOf(ChatMessage(ChatMessage.Role.USER, "hi"))))
            assertEquals("Ещё", c.chat(listOf(ChatMessage(ChatMessage.Role.USER, "hi"))))
            val auth = server.takeRequest()
            assertEquals("Basic a2V5", auth.getHeader("Authorization"))
            assertEquals("scope=GIGACHAT_API_PERS", auth.body.readUtf8())
            assertEquals("/new/v1/chat/completions", server.takeRequest().path, "сначала новый адрес")
            val chat = server.takeRequest()
            assertEquals("/api/v1/chat/completions", chat.path, "затем запасной")
            assertEquals("Bearer tok", chat.getHeader("Authorization"))
            assertTrue(chat.body.readUtf8().contains("\"model\":\"GigaChat\""))
            assertEquals("/api/v1/chat/completions", server.takeRequest().path, "токен и рабочий адрес запоминаются")
        }
    }

    @Test
    fun bundledRussianCa() {
        val certs = ru.sovushka.core.ai.Tls.russianTrustedCa()
        assertEquals(2, certs.size)
        assertTrue(certs.any { it.subjectX500Principal.name.contains("Russian Trusted Root CA") })
        val fp = java.security.MessageDigest.getInstance("SHA-256").digest(certs.first { it.subjectX500Principal == it.issuerX500Principal }.encoded)
            .joinToString(":") { "%02X".format(it) }
        assertEquals("D2:6D:2D:02:31:B7:C3:9F:92:CC:73:85:12:BA:54:10:35:19:E4:40:5D:68:B5:BD:70:3E:97:88:CA:8E:CF:31", fp)
        // Клиент с дополнительными сертификатами собирается поверх системных.
        ru.sovushka.core.ai.Tls.withExtraCertificates(defaultHttpClient(), certs)
    }

    @Test
    fun yandexProtocol() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody("""{"result":{"alternatives":[{"message":{"role":"assistant","text":"Hello"},"status":"ALTERNATIVE_STATUS_FINAL"}],"usage":{}}}"""))
            val c = YandexGptClient("key", "b1g", http = defaultHttpClient(), url = server.url("/c").toString())
            assertEquals("Hello", c.chat(listOf(ChatMessage(ChatMessage.Role.USER, "hi"))))
            val r = server.takeRequest()
            assertEquals("Api-Key key", r.getHeader("Authorization"))
            assertTrue(r.body.readUtf8().contains("gpt://b1g/yandexgpt-lite/latest"))
        }
    }
}
