package ru.sovushka.core.ai

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ru.sovushka.core.Drill
import ru.sovushka.core.GrammarBank

/** Урок, который нейросеть собрала из страницы учебника (ещё не сохранён). */
@Serializable
data class GeneratedLesson(
    val title: String = "",
    val grammar: String = "",
    val words: List<GeneratedWord> = emptyList(),
    val sentences: List<GeneratedSentence> = emptyList(),
    val drills: List<GeneratedDrill> = emptyList(),
    val topics: List<String> = emptyList(),
) {
    /** Годные упражнения: проверенные и с подставленной темой. */
    fun validDrills(): List<Drill> = drills.mapNotNull { it.toDrill() }
}

/** Упражнение от нейросети: все поля необязательные, проверяем и чистим сами. */
@Serializable
data class GeneratedDrill(
    val kind: String = "",
    val prompt: String = "",
    val options: List<String> = emptyList(),
    val answer: String = "",
    val extra: List<String> = emptyList(),
    val explain: String = "",
    val topic: String = "",
) {
    fun toDrill(): Drill? {
        val d = Drill(
            kind = kind.trim().lowercase(),
            prompt = prompt.trim().replace(Regex("_{2,}|…{1,}|\\.\\.\\.+"), "___"),
            options = options.map { it.trim() }.filter { it.isNotEmpty() }.distinct().take(4),
            answer = answer.trim(),
            extra = extra.map { it.trim() }.filter { it.isNotEmpty() && ' ' !in it }.take(3),
            explain = explain.trim(),
            topic = topic.trim().takeIf { GrammarBank.topic(it) != null }.orEmpty(),
        )
        // В «order» правильный ответ не должен совпадать с лишними словами.
        val fixed = if (d.kind == Drill.ORDER) d.copy(extra = d.extra.filter { e -> d.answerTokens.none { it.equals(e, true) } }) else d
        return fixed.takeIf { it.isValid() }
    }
}

@Serializable
data class GeneratedWord(
    val en: String = "",
    val ru: String = "",
    val tr: String = "",
    val emoji: String = "",
    val example: String = "",
    @SerialName("example_ru") val exampleRu: String = "",
)

@Serializable
data class GeneratedSentence(val en: String = "", val ru: String = "")

class LessonGenerator(private val llm: LlmClient) {

    suspend fun fromPage(ocrText: String, note: String, age: Int): GeneratedLesson {
        val text = ocrText.trim().take(MAX_SOURCE)
        if (text.length < 10) throw LlmException("На фото почти не нашлось текста. Сфотографируй страницу ближе и при хорошем свете.")
        val messages = listOf(
            ChatMessage(ChatMessage.Role.SYSTEM, systemPrompt(age)),
            ChatMessage(ChatMessage.Role.USER, userPrompt(text, note)),
        )
        val first = llm.chat(messages, temperature = 0.2, maxTokens = 4500)
        parse(first)?.let { return it }
        // Вторая попытка: напоминаем, что нужен только JSON.
        val retry = llm.chat(
            messages + ChatMessage(ChatMessage.Role.ASSISTANT, first) +
                ChatMessage(ChatMessage.Role.USER, "Ответ не разобрался. Верни только один JSON-объект по схеме, без текста вокруг."),
            temperature = 0.1, maxTokens = 4500,
        )
        return parse(retry) ?: throw LlmException("Нейросеть ответила не в том формате. Попробуйте ещё раз.")
    }

    companion object {
        const val MAX_SOURCE = 6000
        const val MAX_WORDS = 15
        const val MAX_SENTENCES = 6
        const val MAX_DRILLS = 10

        fun systemPrompt(age: Int) = """
Ты — опытный и добрый учитель английского языка для детей в России.
Тебе дают текст страницы школьного учебника английского, распознанный с фотографии (OCR).
В тексте бывают ошибки распознавания, номера упражнений и случайные символы.
Сделай из этой страницы короткий урок для ребёнка $age лет.

Ответь СТРОГО одним JSON-объектом, без пояснений и без markdown, по схеме:
{
  "title": "название темы по-русски, 2–5 слов",
  "grammar": "правило или тема страницы простыми словами для ребёнка, по-русски, 2–5 предложений, с 1–2 примерами на английском; если правила на странице нет — коротко о теме",
  "words": [
    {"en": "слово или короткая фраза на английском", "ru": "перевод", "tr": "британская транскрипция в квадратных скобках", "emoji": "один подходящий эмодзи или пустая строка", "example": "очень простое предложение с этим словом", "example_ru": "перевод примера"}
  ],
  "sentences": [
    {"en": "простое предложение по теме страницы, 3–8 слов", "ru": "перевод"}
  ],
  "drills": [
    {"kind": "gap", "prompt": "английское предложение, где пропущенное слово заменено на ___", "options": ["правильное слово", "неверный вариант", "неверный вариант"], "answer": "правильное слово", "explain": "почему так — одно короткое предложение по-русски", "topic": "id темы"},
    {"kind": "choose", "options": ["правильное предложение", "то же предложение с типичной ошибкой", "ещё одна ошибка"], "answer": "правильное предложение", "explain": "почему", "topic": "id темы"},
    {"kind": "order", "answer": "английское предложение, 3–9 слов", "prompt": "его ТОЧНЫЙ перевод на русский", "extra": ["лишнее слово-ловушка"], "explain": "почему", "topic": "id темы"}
  ],
  "topics": ["id пройденных тем из списка ниже, к которым относится страница"]
}

Правила:
- 6–$MAX_WORDS самых важных слов страницы: новая лексика и слова темы. Не бери служебные слова (the, a, is) и слова из заданий (read, listen, match), если урок не про них.
- Исправляй ошибки распознавания. Не придумывай слова, которых нет в теме страницы.
- Английские слова пиши с маленькой буквы, кроме имён и «I». Перевод — самый простой и частый.
- 3–$MAX_SENTENCES предложений, лучше из текста страницы, упрощённых при необходимости.
- 6–$MAX_DRILLS упражнений на грамматику и построение предложений: больше половины — на правило этой страницы,
  остальные — на повторение тем, которые ребёнок прошёл раньше и которые связаны со страницей (ребёнок учит английский с 2 класса).
  Используй слова страницы. Неверные варианты — типичные ошибки детей (he have, she don't, two mouses, I am go).
  Во всех упражнениях ровно один правильный ответ. Explain — по-русски, просто, для ребёнка.
- В «order» сначала придумай английское предложение (answer), потом переведи его на русский (prompt) слово в слово:
  те же люди и имена (Nell → Нелл, his sister → его сестра), то же число, время и смысл. Ребёнок собирает answer
  из слов по русскому prompt, поэтому если они не совпадают — задание нельзя решить. Лишнее слово (extra) — одно слово
  без пробелов, которого нет в answer.
- topics — id из списка пройденных тем, только реально связанные со страницей (1–4 штуки).

Список пройденных тем (2–4 класс):
${GrammarBank.catalog()}
""".trim()

        fun userPrompt(text: String, note: String): String = buildString {
            if (note.isNotBlank()) append("Пометка: ").append(note.trim()).append("\n")
            append("Распознанный текст страницы:\n\"\"\"\n").append(text).append("\n\"\"\"")
        }

        /** Достаём JSON из ответа (бывает обёрнут в ```json ... ``` или с текстом вокруг) и чистим. */
        fun parse(raw: String): GeneratedLesson? {
            val start = raw.indexOf('{')
            val end = raw.lastIndexOf('}')
            if (start < 0 || end <= start) return null
            val lesson = try {
                json.decodeFromString<GeneratedLesson>(raw.substring(start, end + 1))
            } catch (e: Exception) {
                return null
            }
            val words = lesson.words
                .map { it.copy(en = it.en.trim(), ru = it.ru.trim(), tr = normalizeTr(it.tr), emoji = it.emoji.trim().take(8)) }
                .filter { it.en.isNotEmpty() && it.ru.isNotEmpty() && it.en.any { c -> c in 'a'..'z' || c in 'A'..'Z' } }
                .distinctBy { it.en.lowercase() }
                .take(MAX_WORDS)
            if (words.isEmpty()) return null
            val sentences = lesson.sentences
                .map { GeneratedSentence(it.en.trim(), it.ru.trim()) }
                .filter { it.en.isNotEmpty() && it.ru.isNotEmpty() }
                .take(MAX_SENTENCES)
            return lesson.copy(
                title = lesson.title.trim().ifEmpty { "Новый урок" },
                grammar = lesson.grammar.trim(),
                words = words,
                sentences = sentences,
                drills = lesson.drills.filter { it.toDrill() != null }.take(MAX_DRILLS),
                topics = lesson.topics.map { it.trim() }.filter { GrammarBank.topic(it) != null }.distinct().take(4),
            )
        }

        private fun normalizeTr(tr: String): String {
            val t = tr.trim().trim('/', '[', ']').trim()
            return if (t.isEmpty()) "" else "[$t]"
        }
    }
}

/** Новые упражнения по теме от нейросети — «Ещё задания от Ники». */
class DrillGenerator(private val llm: LlmClient) {

    @Serializable
    private data class Resp(val drills: List<GeneratedDrill> = emptyList())

    suspend fun forTopic(topic: ru.sovushka.core.GrammarTopic, age: Int, knownWords: List<String>, count: Int = 8): List<Drill> {
        val system = """
Ты — учитель английского для детей в России. Придумай $count новых упражнений для ребёнка $age лет на тему:
«${topic.title}». Правило: ${topic.rule}

Ответь СТРОГО одним JSON-объектом без markdown:
{"drills": [
  {"kind": "gap", "prompt": "предложение с ___ на месте пропуска", "options": ["правильный", "неверный", "неверный"], "answer": "правильный", "explain": "почему, по-русски"},
  {"kind": "choose", "options": ["правильное предложение", "с типичной ошибкой", "с другой ошибкой"], "answer": "правильное предложение", "explain": "почему"},
  {"kind": "order", "answer": "English sentence, 3–9 words", "prompt": "его ТОЧНЫЙ перевод на русский", "extra": ["лишнее слово-ловушка"], "explain": "почему"}
]}
Правила: примерно поровну gap, choose и order; простые слова (можно из списка ниже); ровно один правильный ответ;
в order prompt — дословный перевод answer: те же люди и имена, то же время (иначе задание нельзя решить);
неверные варианты — типичные ошибки детей; предложения про жизнь ребёнка: школа, семья, животные, игры.
""".trim()
        val user = if (knownWords.isEmpty()) "Сделай упражнения." else "Слова, которые ребёнок сейчас учит: " + knownWords.take(40).joinToString(", ")
        val raw = llm.chat(listOf(ChatMessage(ChatMessage.Role.SYSTEM, system), ChatMessage(ChatMessage.Role.USER, user)), temperature = 0.7, maxTokens = 2500)
        return parse(raw).map { it.copy(topic = topic.id) }
    }

    companion object {
        fun parse(raw: String): List<Drill> {
            val start = raw.indexOf('{')
            val end = raw.lastIndexOf('}')
            if (start < 0 || end <= start) return emptyList()
            return runCatching { json.decodeFromString<Resp>(raw.substring(start, end + 1)) }.getOrNull()
                ?.drills?.mapNotNull { it.toDrill() }.orEmpty()
        }
    }
}
