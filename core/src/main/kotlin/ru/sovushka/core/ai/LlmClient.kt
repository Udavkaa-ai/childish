package ru.sovushka.core.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

data class ChatMessage(val role: Role, val text: String) {
    enum class Role { SYSTEM, USER, ASSISTANT }
}

/** Ошибка с понятным родителю текстом. */
class LlmException(message: String, cause: Throwable? = null) : Exception(message, cause)

interface LlmClient {
    val name: String
    suspend fun chat(messages: List<ChatMessage>, temperature: Double = 0.4, maxTokens: Int = 2000): String
}

internal val json = Json { ignoreUnknownKeys = true; isLenient = true; explicitNulls = false }

fun defaultHttpClient(): OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(20, TimeUnit.SECONDS)
    .readTimeout(120, TimeUnit.SECONDS)
    .writeTimeout(60, TimeUnit.SECONDS)
    .build()

/** Выполнить запрос в IO-потоке и вернуть тело; HTTP-ошибки превращаются в LlmException. */
internal suspend fun OkHttpClient.fetch(request: Request, service: String): String = withContext(Dispatchers.IO) {
    val response: Response = try {
        newCall(request).execute()
    } catch (e: javax.net.ssl.SSLException) {
        throw LlmException("$service: не удалось установить защищённое соединение (${e.message}). " +
            "Если это GigaChat — возможно, Минцифры сменило сертификат: загрузите новый с gosuslugi.ru/crt в настройках.", e)
    } catch (e: IOException) {
        throw LlmException("$service: нет связи с сервером. Проверьте интернет. (${e.message})", e)
    }
    response.use {
        val body = it.body?.string().orEmpty()
        if (!it.isSuccessful) {
            val hint = when (it.code) {
                401, 403 -> "проверьте ключ доступа в настройках"
                402 -> "закончились токены или не подключена оплата"
                404 -> "неверное имя модели или адрес"
                429 -> "слишком много запросов, подождите минуту"
                else -> "ошибка сервера"
            }
            throw LlmException("$service: ${it.code} — $hint. ${body.take(300)}")
        }
        body
    }
}
