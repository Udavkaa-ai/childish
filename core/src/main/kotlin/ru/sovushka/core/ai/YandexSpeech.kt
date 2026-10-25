package ru.sovushka.core.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
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
    private val v3Url: String = "https://tts.api.cloud.yandex.net/tts/v3/utteranceSynthesis",
) {
    /** Голос Яндекса для Ники: название, амплуа и модель (для LiveTTS). */
    data class Voice(val name: String, val title: String, val role: String, val model: String = "")

    companion object {
        /**
         * Женские голоса SpeechKit, которые сносно читают и английские слова.
         * Голоса LiveTTS живее, но английский не читают (даже «Hello»), поэтому их здесь нет.
         */
        val GIRL_VOICES = listOf(
            Voice("dasha", "Даша — дружелюбная", "friendly"),
            Voice("dasha", "Даша — радостная", "good"),
            Voice("masha", "Маша — радостная", "good"),
            Voice("masha", "Маша — дружелюбная", "friendly"),
            Voice("lera", "Лера — дружелюбная", "friendly"),
            Voice("marina", "Марина — дружелюбная", "friendly"),
            Voice("jane", "Джейн — радостная", "good"),
        )

        /**
         * Голоса для математики: там нет английского, поэтому можно живые голоса LiveTTS
         * (они звучат естественнее всего, но английские слова не читают).
         */
        val MATH_VOICES = listOf(
            Voice("sofia", "София (LiveTTS) — живая, разговорная", "casual", "livetts"),
            Voice("vera", "Вера (LiveTTS) — живая, разговорная", "casual", "livetts"),
            Voice("irina", "Ирина (LiveTTS) — рассказчик", "narrator", "livetts"),
            Voice("dasha", "Даша — дружелюбная (как в английском)", "friendly"),
            Voice("marina", "Марина — дружелюбная", "friendly"),
        )

        /** Достаём аудио из ответа v3: несколько JSON-объектов подряд (по строкам или слитно). */
        fun parseV3Audio(raw: String): ByteArray {
            val out = java.io.ByteArrayOutputStream()
            var depth = 0
            var start = -1
            var inString = false
            var escape = false
            for ((i, c) in raw.withIndex()) {
                if (inString) {
                    if (escape) escape = false else if (c == '\\') escape = true else if (c == '"') inString = false
                    continue
                }
                when (c) {
                    '"' -> inString = true
                    '{' -> { if (depth == 0) start = i; depth++ }
                    '}' -> {
                        depth--
                        if (depth == 0 && start >= 0) {
                            val obj = runCatching { json.parseToJsonElement(raw.substring(start, i + 1)).jsonObject }.getOrNull()
                            val data = obj?.get("result")?.jsonObject?.get("audioChunk")?.jsonObject?.get("data")?.jsonPrimitive?.content
                            if (data != null) out.write(java.util.Base64.getDecoder().decode(data))
                            start = -1
                        }
                    }
                }
            }
            return out.toByteArray()
        }
    }

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

    /**
     * Синтез через API v3 (новые голоса, амплуа и LiveTTS). Сервер отвечает потоком JSON-объектов
     * {"result":{"audioChunk":{"data":"<base64>"}}} — склеиваем куски в один mp3.
     * model = "livetts" для голосов LiveTTS (sofia, vera, irina…), пусто — обычные голоса general.
     */
    suspend fun synthesizeV3(
        text: String,
        voice: String,
        role: String = "",
        speed: Double = 1.0,
        model: String = "",
        pitchShift: Double = 0.0,
    ): ByteArray {
        val hints = buildJsonArray {
            add(buildJsonObject { put("voice", voice.trim()) })
            if (role.isNotBlank()) add(buildJsonObject { put("role", role.trim()) })
            add(buildJsonObject { put("speed", speed) })
            // Высота голоса в Гц (в Playground — «Высота голоса», от -1000 до 1000).
            if (pitchShift != 0.0) add(buildJsonObject { put("pitchShift", pitchShift) })
        }
        val body = buildJsonObject {
            if (model.isNotBlank()) put("model", model)
            put("text", text)
            put("hints", hints)
            put("outputAudioSpec", buildJsonObject {
                put("containerAudio", buildJsonObject { put("containerAudioType", "MP3") })
            })
            put("loudnessNormalizationType", "LUFS")
        }
        val req = Request.Builder()
            .url(v3Url)
            .header("Authorization", "Api-Key ${apiKey.trim()}")
            .apply { if (folderId.isNotBlank()) header("x-folder-id", folderId.trim()) }
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()
        val raw = http.fetch(req, "SpeechKit")
        return parseV3Audio(raw).also {
            if (it.isEmpty()) throw LlmException("SpeechKit: пустой ответ синтеза: ${raw.take(200)}")
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
