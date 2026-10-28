package ru.sovushka.app.voice

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import ru.sovushka.app.data.Prefs
import ru.sovushka.app.data.Settings
import ru.sovushka.core.ai.YandexSpeech
import java.io.ByteArrayOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.sqrt

class ListenException(message: String) : Exception(message)

/**
 * Распознавание речи: системный распознаватель Android (обычно Google)
 * или Yandex SpeechKit, если он выбран в настройках — он работает и на телефонах без сервисов Google.
 */
class Listener(
    private val context: Context,
    private val settings: Settings,
    private val yandex: () -> YandexSpeech?,
) {
    private val _listening = MutableStateFlow(false)
    val listening: StateFlow<Boolean> = _listening
    private val _level = MutableStateFlow(0f)
    val level: StateFlow<Float> = _level

    fun available(): Boolean =
        (settings.value.voice == Prefs.VOICE_YANDEX && yandex() != null) || SpeechRecognizer.isRecognitionAvailable(context)

    /**
     * Слушать одну фразу. lang: "en-US" или "ru-RU". Возвращает варианты распознавания (может быть пусто).
     * hints — слова, которые ребёнок, скорее всего, скажет: Android 13+ охотнее слышит именно их.
     */
    suspend fun listen(lang: String, hints: List<String> = emptyList()): List<String> {
        _listening.value = true
        try {
            val ya = if (settings.value.voice == Prefs.VOICE_YANDEX) yandex() else null
            return if (ya != null) listenYandex(ya, lang) else listenAndroid(lang, hints)
        } finally {
            _listening.value = false
            _level.value = 0f
        }
    }

    private suspend fun listenAndroid(lang: String, hints: List<String>): List<String> = withContext(Dispatchers.Main) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            throw ListenException("На телефоне нет распознавания речи. Включите Yandex SpeechKit в настройках.")
        }
        suspendCancellableCoroutine { cont ->
            val sr = SpeechRecognizer.createSpeechRecognizer(context)
            fun finish(block: () -> Unit) {
                if (cont.isActive) block()
                runCatching { sr.destroy() }
            }
            sr.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) { _level.value = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f) }
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
                override fun onResults(results: Bundle?) {
                    val list = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
                    finish { cont.resume(list) }
                }
                override fun onError(error: Int) {
                    when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
                            finish { cont.resume(emptyList()) }
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
                            finish { cont.resumeWithException(ListenException("Нет разрешения на микрофон")) }
                        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT, SpeechRecognizer.ERROR_SERVER ->
                            finish { cont.resumeWithException(ListenException("Распознаванию нужен интернет")) }
                        else -> finish { cont.resumeWithException(ListenException("Не получилось послушать (код $error)")) }
                    }
                }
            })
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, lang)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, lang)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
                if (hints.isNotEmpty() && android.os.Build.VERSION.SDK_INT >= 33) {
                    putStringArrayListExtra(RecognizerIntent.EXTRA_BIASING_STRINGS, ArrayList(hints))
                }
            }
            sr.startListening(intent)
            cont.invokeOnCancellation { Handler(Looper.getMainLooper()).post { runCatching { sr.destroy() } } }
        }
    }

    /** Пишем звук до паузы после речи (или максимум 7 секунд) и отправляем в SpeechKit. */
    @SuppressLint("MissingPermission")
    private suspend fun listenYandex(ya: YandexSpeech, lang: String): List<String> {
        val pcm = withContext(Dispatchers.IO) {
            val rate = 16000
            val minBuf = AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
            val rec = AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, maxOf(minBuf, rate))
            if (rec.state != AudioRecord.STATE_INITIALIZED) throw ListenException("Нет доступа к микрофону")
            val out = ByteArrayOutputStream()
            val buf = ShortArray(rate / 10) // 100 мс
            var heard = false
            var silentMs = 0
            var totalMs = 0
            try {
                rec.startRecording()
                while (totalMs < 7000) {
                    ensureActive()
                    val n = rec.read(buf, 0, buf.size)
                    if (n <= 0) break
                    var sum = 0.0
                    for (i in 0 until n) {
                        sum += buf[i] * buf[i].toDouble()
                        out.write(buf[i].toInt() and 0xFF); out.write(buf[i].toInt() shr 8 and 0xFF)
                    }
                    val rms = sqrt(sum / n)
                    _level.value = (rms / 3000.0).toFloat().coerceIn(0f, 1f)
                    totalMs += 100
                    if (rms > 700) { heard = true; silentMs = 0 } else silentMs += 100
                    if (heard && silentMs >= 1000) break
                    if (!heard && totalMs >= 4000) break
                }
            } finally {
                runCatching { rec.stop() }; rec.release()
            }
            if (heard) out.toByteArray() else null
        } ?: return emptyList()
        val text = try {
            ya.recognize(pcm, lang)
        } catch (e: Exception) {
            throw ListenException(e.message ?: "SpeechKit не ответил")
        }
        return listOf(text).filter { it.isNotBlank() }
    }
}
