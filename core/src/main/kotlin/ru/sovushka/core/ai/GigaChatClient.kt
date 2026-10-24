package ru.sovushka.core.ai

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.UUID

/**
 * GigaChat (Сбер). Авторизация: «ключ авторизации» из личного кабинета (base64 от client_id:secret)
 * обменивается на токен доступа на 30 минут.
 */
class GigaChatClient(
    private val authKey: String,
    private val scope: String = "GIGACHAT_API_PERS",
    private val model: String = "GigaChat",
    private val http: OkHttpClient,
    private val oauthUrl: String = "https://ngw.devices.sberbank.ru:9443/api/v2/oauth",
    /** Адреса API по порядку: с 2026 года основной — api.giga.chat, старый оставлен запасным. */
    private val apiUrls: List<String> = listOf("https://api.giga.chat/v1", "https://gigachat.devices.sberbank.ru/api/v1"),
) : LlmClient {
    override val name = "GigaChat"

    private var token: String? = null
    private var tokenExpiresAt: Long = 0

    @Serializable
    private data class TokenResponse(val access_token: String, val expires_at: Long = 0)

    @Serializable
    private data class Msg(val role: String, val content: String)

    @Serializable
    private data class Req(
        val model: String,
        val messages: List<Msg>,
        val temperature: Double,
        val max_tokens: Int,
    )

    @Serializable
    private data class Choice(val message: Msg)

    @Serializable
    private data class Resp(val choices: List<Choice> = emptyList())

    private var workingUrl: String? = null

    /** Пробуем адреса по очереди, пока не ответит тот, что доступен (ошибки связи и 404). */
    private suspend fun withFallback(call: suspend (String) -> String): String {
        val order = listOfNotNull(workingUrl) + apiUrls.filter { it != workingUrl }
        var last: LlmException? = null
        for (base in order) {
            try {
                return call(base).also { workingUrl = base }
            } catch (e: LlmException) {
                last = e
                val retryable = e.cause is java.io.IOException || e.message?.contains(": 404") == true
                if (!retryable) throw e
            }
        }
        throw last ?: LlmException("GigaChat: нет адресов API")
    }

    private suspend fun accessToken(): String {
        val now = System.currentTimeMillis()
        token?.let { if (now < tokenExpiresAt - 60_000) return it }
        val key = authKey.trim().removePrefix("Basic ").trim()
        if (key.isEmpty()) throw LlmException("GigaChat: не указан ключ авторизации")
        val req = Request.Builder()
            .url(oauthUrl)
            .header("Authorization", "Basic $key")
            .header("RqUID", UUID.randomUUID().toString())
            .header("Accept", "application/json")
            .post(FormBody.Builder().add("scope", scope).build())
            .build()
        val body = http.fetch(req, "GigaChat (авторизация)")
        val parsed = try {
            json.decodeFromString<TokenResponse>(body)
        } catch (e: Exception) {
            throw LlmException("GigaChat: непонятный ответ авторизации: ${body.take(200)}", e)
        }
        token = parsed.access_token
        tokenExpiresAt = if (parsed.expires_at > 0) parsed.expires_at else now + 25 * 60_000
        return parsed.access_token
    }

    override suspend fun chat(messages: List<ChatMessage>, temperature: Double, maxTokens: Int): String {
        val payload = Req(
            model = model,
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
            temperature = temperature,
            max_tokens = maxTokens,
        )
        val bearer = accessToken()
        val body = withFallback { base ->
            val req = Request.Builder()
                .url("$base/chat/completions")
                .header("Authorization", "Bearer $bearer")
                .header("Accept", "application/json")
                .post(json.encodeToString(payload).toRequestBody("application/json".toMediaType()))
                .build()
            http.fetch(req, "GigaChat")
        }
        val resp = try {
            json.decodeFromString<Resp>(body)
        } catch (e: Exception) {
            throw LlmException("GigaChat: непонятный ответ: ${body.take(200)}", e)
        }
        return resp.choices.firstOrNull()?.message?.content?.trim()
            ?: throw LlmException("GigaChat вернул пустой ответ")
    }
}
