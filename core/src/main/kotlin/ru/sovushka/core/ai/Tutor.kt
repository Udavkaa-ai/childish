package ru.sovushka.core.ai

/** Голосовая помощница Ника (персонаж, которого придумала Вероника): держит историю разговора и контекст. */
class Tutor(
    private val llm: LlmClient,
    private val kidName: String,
    private val age: Int,
    private val knownWords: List<String>,
    private val lessonContext: String = "",
) {
    private val history = mutableListOf<ChatMessage>()

    fun systemPrompt(): String = buildString {
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

    suspend fun ask(text: String): String {
        history += ChatMessage(ChatMessage.Role.USER, text)
        // Держим последние реплики, чтобы запрос не разрастался.
        val recent = history.takeLast(12).dropWhile { it.role == ChatMessage.Role.ASSISTANT }
        val answer = llm.chat(listOf(ChatMessage(ChatMessage.Role.SYSTEM, systemPrompt())) + recent, temperature = 0.6, maxTokens = 400)
        history += ChatMessage(ChatMessage.Role.ASSISTANT, answer)
        return answer
    }
}
