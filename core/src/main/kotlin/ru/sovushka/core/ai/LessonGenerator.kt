package ru.sovushka.core.ai

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Урок, который нейросеть собрала из страницы учебника (ещё не сохранён). */
@Serializable
data class GeneratedLesson(
    val title: String = "",
    val grammar: String = "",
    val words: List<GeneratedWord> = emptyList(),
    val sentences: List<GeneratedSentence> = emptyList(),
)

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
        val first = llm.chat(messages, temperature = 0.2, maxTokens = 3000)
        parse(first)?.let { return it }
        // Вторая попытка: напоминаем, что нужен только JSON.
        val retry = llm.chat(
            messages + ChatMessage(ChatMessage.Role.ASSISTANT, first) +
                ChatMessage(ChatMessage.Role.USER, "Ответ не разобрался. Верни только один JSON-объект по схеме, без текста вокруг."),
            temperature = 0.1, maxTokens = 3000,
        )
        return parse(retry) ?: throw LlmException("Нейросеть ответила не в том формате. Попробуйте ещё раз.")
    }

    companion object {
        const val MAX_SOURCE = 6000
        const val MAX_WORDS = 15
        const val MAX_SENTENCES = 6

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
  ]
}

Правила:
- 6–$MAX_WORDS самых важных слов страницы: новая лексика и слова темы. Не бери служебные слова (the, a, is) и слова из заданий (read, listen, match), если урок не про них.
- Исправляй ошибки распознавания. Не придумывай слова, которых нет в теме страницы.
- Английские слова пиши с маленькой буквы, кроме имён и «I». Перевод — самый простой и частый.
- 3–$MAX_SENTENCES предложений, лучше из текста страницы, упрощённых при необходимости.
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
            )
        }

        private fun normalizeTr(tr: String): String {
            val t = tr.trim().trim('/', '[', ']').trim()
            return if (t.isEmpty()) "" else "[$t]"
        }
    }
}
