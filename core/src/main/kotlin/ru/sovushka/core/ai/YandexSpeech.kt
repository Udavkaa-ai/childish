package ru.sovushka.core.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/** Yandex SpeechKit: синтез и распознавание речи (REST v1). */
class YandexSpeech(
    private val apiKey: String,
    private val folderId: String,
    private val http: OkHttpClient,
) {
    @Serializable
    private data class SttResp(val result: String = "")

    /** Синтез в mp3. lang: "en-US" или "ru-RU". */
    suspend fun synthesize(text: String, lang: String, voice: String, speed: Double = 1.0): ByteArray {
        val form = FormBody.Builder()
            .add("text", text)
            .add("lang", lang)
            .add("voice", voice)
            .add("speed", "%.2f".format(java.util.Locale.US, speed))
            .add("format", "mp3")
            .add("folderId", folderId.trim())
            .build()
        val req = Request.Builder()
            .url("https://tts.api.cloud.yandex.net/speech/v1/tts:synthesize")
            .header("Authorization", "Api-Key ${apiKey.trim()}")
            .post(form)
            .build()
        return withContext(Dispatchers.IO) {
            http.newCall(req).execute().use {
                if (!it.isSuccessful) throw LlmException("SpeechKit: ${it.code} ${it.body?.string()?.take(200)}")
                it.body?.bytes() ?: ByteArray(0)
            }
        }
    }

    /** Распознавание 16 кГц моно PCM16 (до 30 секунд). */
    suspend fun recognize(pcm: ByteArray, lang: String, sampleRate: Int = 16000): String {
        val url = "https://stt.api.cloud.yandex.net/speech/v1/stt:recognize".toHttpUrl().newBuilder()
            .addQueryParameter("lang", lang)
            .addQueryParameter("format", "lpcm")
            .addQueryParameter("sampleRateHertz", sampleRate.toString())
            .addQueryParameter("folderId", folderId.trim())
            .build()
        val req = Request.Builder()
            .url(url)
            .header("Authorization", "Api-Key ${apiKey.trim()}")
            .post(pcm.toRequestBody("application/octet-stream".toMediaType()))
            .build()
        val body = http.fetch(req, "SpeechKit")
        return json.decodeFromString<SttResp>(body).result
    }
}
