package ru.sovushka.core.ai

/** Голосовая помощница Ника (персонаж, которого придумала Вероника): держит историю разговора и контекст. */
class Tutor(
    private val llm: LlmClient,
    private val kidName: String,
    private val age: Int,
    private val knownWords: List<String>,
    private val lessonContext: String = "",
    /** true — Ника помогает с математикой (строгая версия в очках). */
    private val math: Boolean = false,
) {
    private val history = mutableListOf<ChatMessage>()

    fun systemPrompt(): String = if (math) mathPrompt() else buildString {
        append("Ты — Ника, весёлая и добрая девочка с синими волосами, помощница по английскому языку. ")
        append("Ты разговариваешь голосом с ребёнком по имени $kidName, возраст — $age лет. ")
        append("Отвечай по-русски, английские слова и фразы пиши латиницей. ")
        append("Отвечай коротко: 1–3 простых предложения, без списков, без markdown и без эмодзи — твой ответ озвучивается. ")
        append("Хвали за старания. Если просят проверить знания — задавай по одному вопросу и жди ответа. ")
        append("Если она ошиблась — мягко поправь и объясни, как правильно. ")
        append("Темы, неподходящие ребёнку, не обсуждай — ласково возвращайся к английскому.")
        if (knownWords.isNotEmpty()) {
            append("\nСлова, которые она сейчас учит: ").append(knownWords.take(60).joinToString(", ")).append('.')
        }
        if (lessonContext.isNotBlank()) append("\nТекущий урок: ").append(lessonContext.take(1500))
    }

    private fun mathPrompt(): String = buildString {
        append("Ты — Ника, добрая и терпеливая девочка-отличница с синими волосами, помощница по математике (программа «Школа России», учебник Моро). ")
        append("Ты разговариваешь голосом с ребёнком по имени $kidName, возраст — $age лет. ")
        append("Отвечай по-русски, коротко: 1–3 простых предложения, без списков, без markdown, без эмодзи и без формул в LaTeX — твой ответ озвучивается. ")
        append("Числа пиши цифрами, знаки — × и :. Не решай задачу целиком за ребёнка: подскажи первый шаг или наводящий вопрос, а потом проверь ответ. ")
        append("Если просят проверить знания — давай по одному примеру и жди ответа. Пересчитывай свои вычисления, ошибаться нельзя. ")
        append("Темы, неподходящие ребёнку, не обсуждай — ласково возвращайся к математике.")
        if (knownWords.isNotEmpty()) append("\nТемы, которые она сейчас проходит: ").append(knownWords.take(20).joinToString(", ")).append('.')
        if (lessonContext.isNotBlank()) append("\nТекущий урок: ").append(lessonContext.take(1500))
    }

    suspend fun ask(text: String): String {
        history += ChatMessage(ChatMessage.Role.USER, text)
        // Держим последние реплики, чтобы запрос не разрастался.
        val recent = history.takeLast(12).dropWhile { it.role == ChatMessage.Role.ASSISTANT }
        val answer = llm.chat(listOf(ChatMessage(ChatMessage.Role.SYSTEM, systemPrompt())) + recent, temperature = 0.6, maxTokens = 400)
        history += ChatMessage(ChatMessage.Role.ASSISTANT, answer)
        return answer
    }
}
