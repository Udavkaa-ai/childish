package ru.sovushka.core.ai

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/** YandexGPT (Yandex Cloud Foundation Models). Нужны API-ключ сервисного аккаунта и ID каталога. */
class YandexGptClient(
    private val apiKey: String,
    private val folderId: String,
    private val model: String = "yandexgpt-lite",
    private val http: OkHttpClient,
    private val url: String = "https://llm.api.cloud.yandex.net/foundationModels/v1/completion",
) : LlmClient {
    override val name = "YandexGPT"

    @Serializable
    private data class Options(val stream: Boolean, val temperature: Double, val maxTokens: String)

    @Serializable
    private data class Msg(val role: String, val text: String)

    @Serializable
    private data class Req(val modelUri: String, val completionOptions: Options, val messages: List<Msg>)

    @Serializable
    private data class Alternative(val message: Msg)

    @Serializable
    private data class Result(val alternatives: List<Alternative> = emptyList())

    @Serializable
    private data class Resp(val result: Result)

    override suspend fun chat(messages: List<ChatMessage>, temperature: Double, maxTokens: Int): String {
        if (apiKey.isBlank() || folderId.isBlank()) throw LlmException("YandexGPT: укажите API-ключ и ID каталога")
        val payload = Req(
            modelUri = "gpt://${folderId.trim()}/${model.trim()}/latest",
            completionOptions = Options(false, temperature, maxTokens.toString()),
            messages = messages.map {
                Msg(
                    when (it.role) {
                        ChatMessage.Role.SYSTEM -> "system"
                        ChatMessage.Role.USER -> "user"
                        ChatMessage.Role.ASSISTANT -> "assistant"
                    },
                    it.text,
                )
            },
        )
        val req = Request.Builder()
            .url(url)
            .header("Authorization", "Api-Key ${apiKey.trim()}")
            .header("x-folder-id", folderId.trim())
            .post(json.encodeToString(payload).toRequestBody("application/json".toMediaType()))
            .build()
        val body = http.fetch(req, "YandexGPT")
        val resp = try {
            json.decodeFromString<Resp>(body)
        } catch (e: Exception) {
            throw LlmException("YandexGPT: непонятный ответ: ${body.take(200)}", e)
        }
        return resp.result.alternatives.firstOrNull()?.message?.text?.trim()
            ?: throw LlmException("YandexGPT вернул пустой ответ")
    }
}
