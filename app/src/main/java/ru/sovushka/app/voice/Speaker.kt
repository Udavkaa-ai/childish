package ru.sovushka.app.voice

import android.content.Context
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import ru.sovushka.app.data.Prefs
import ru.sovushka.app.data.Settings
import ru.sovushka.core.SpeechText
import ru.sovushka.core.ai.YandexSpeech
import java.io.File
import java.security.MessageDigest
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume

/**
 * Голос Ники. По умолчанию — встроенный синтезатор Android (работает без интернета),
 * по желанию — Yandex SpeechKit (звучит естественнее, фразы кэшируются на телефоне).
 */
class Speaker(
    private val context: Context,
    private val settings: Settings,
    private val yandex: () -> YandexSpeech?,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var ready = CompletableDeferred<Boolean>()
    private val pending = ConcurrentHashMap<String, CompletableDeferred<Unit>>()
    private var job: Job? = null
    private var player: MediaPlayer? = null

    private val _speaking = MutableStateFlow(false)
    val speaking: StateFlow<Boolean> = _speaking

    /** Нет английского или русского голоса в синтезаторе — подсказать родителю. */
    private val _missingVoice = MutableStateFlow(false)
    val missingVoice: StateFlow<Boolean> = _missingVoice

    private val progress = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) {}
        override fun onDone(utteranceId: String?) { pending.remove(utteranceId)?.complete(Unit) }
        @Deprecated("Deprecated in Java")
        override fun onError(utteranceId: String?) { pending.remove(utteranceId)?.complete(Unit) }
        override fun onError(utteranceId: String?, errorCode: Int) { pending.remove(utteranceId)?.complete(Unit) }
    }

    private var tts: TextToSpeech = create(settings.value.ttsEngine)

    private fun create(engine: String): TextToSpeech {
        val r = CompletableDeferred<Boolean>()
        ready = r
        val listener = TextToSpeech.OnInitListener { status -> r.complete(status == TextToSpeech.SUCCESS) }
        val t = if (engine.isBlank()) TextToSpeech(context.applicationContext, listener)
        else TextToSpeech(context.applicationContext, listener, engine)
        t.setOnUtteranceProgressListener(progress)
        return t
    }

    init { checkVoices() }

    private fun checkVoices() {
        scope.launch {
            if (ready.await()) {
                val en = tts.isLanguageAvailable(english(settings.value))
                val ru = tts.isLanguageAvailable(RU)
                _missingVoice.value = en < TextToSpeech.LANG_AVAILABLE || ru < TextToSpeech.LANG_AVAILABLE
            }
        }
    }

    /** Синтезаторы речи на телефоне: пакет → название. */
    fun engines(): List<Pair<String, String>> = runCatching { tts.engines.map { it.name to it.label } }.getOrDefault(emptyList())

    /** Сменить синтезатор (например, на «Синтезатор речи Google» с нейросетевыми голосами). */
    fun setEngine(pkg: String) {
        stop()
        runCatching { tts.shutdown() }
        tts = create(pkg)
        checkVoices()
    }

    data class VoiceOption(val name: String, val title: String)

    /** Голоса для языка, лучшие сверху. */
    suspend fun voices(lang: SpeechText.Lang): List<VoiceOption> {
        if (withTimeoutOrNull(5000) { ready.await() } != true) return emptyList()
        val want = if (lang == SpeechText.Lang.EN) english(settings.value) else RU
        return candidates(want).mapIndexed { i, v ->
            val q = when {
                v.quality >= Voice.QUALITY_VERY_HIGH -> "отличное качество"
                v.quality >= Voice.QUALITY_HIGH -> "высокое качество"
                else -> "обычное качество"
            }
            val net = if (v.isNetworkConnectionRequired) "нужен интернет" else "без интернета"
            VoiceOption(v.name, "Голос ${i + 1} · ${v.locale.displayCountry.ifBlank { v.locale.displayLanguage }} · $q · $net")
        }
    }

    private fun candidates(want: Locale): List<Voice> =
        runCatching { tts.voices }.getOrNull().orEmpty()
            .filter { it.locale.language == want.language && !it.features.orEmpty().contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED) }
            .sortedWith(
                compareByDescending<Voice> { it.locale.country == want.country }
                    .thenByDescending { it.quality }
                    .thenBy { it.isNetworkConnectionRequired }
                    .thenBy { it.latency },
            )

    /** Выбранный родителем голос или лучший доступный. */
    private fun pickVoice(want: Locale, preferred: String): Voice? {
        val all = candidates(want)
        return all.firstOrNull { it.name == preferred } ?: all.firstOrNull()
    }

    private fun english(p: Prefs): Locale = if (p.accent == "en-US") Locale.US else Locale.UK

    /** Сказать и не ждать (предыдущая фраза прерывается). */
    fun speak(text: String, lang: SpeechText.Lang? = null, slow: Boolean = false) {
        job?.cancel()
        job = scope.launch { say(text, lang, slow) }
    }

    /** Сказать и дождаться конца. lang = null — язык определяется по кускам текста. */
    suspend fun say(text: String, lang: SpeechText.Lang? = null, slow: Boolean = false) {
        stopNow()
        val segments = if (lang != null) {
            listOf(SpeechText.Segment(SpeechText.clean(text), lang))
        } else {
            SpeechText.split(text)
        }.filter { it.text.isNotBlank() }
        if (segments.isEmpty()) return
        _speaking.value = true
        try {
            for (seg in segments) {
                val p = settings.value
                val ya = if (p.voice == Prefs.VOICE_YANDEX) yandex() else null
                val done = if (ya != null) {
                    runCatching { playYandex(ya, seg, p, slow) }.isSuccess
                } else false
                if (!done) speakAndroid(seg, p, slow)
            }
        } finally {
            _speaking.value = false
        }
    }

    private suspend fun speakAndroid(seg: SpeechText.Segment, p: Prefs, slow: Boolean) {
        if (withTimeoutOrNull(5000) { ready.await() } != true) return
        val locale = if (seg.lang == SpeechText.Lang.EN) english(p) else RU
        val voice = pickVoice(locale, if (seg.lang == SpeechText.Lang.EN) p.voiceEn else p.voiceRu)
        if (voice != null) tts.setVoice(voice) else tts.setLanguage(locale)
        tts.setSpeechRate(if (slow) 0.55f else p.rate)
        tts.setPitch(if (seg.lang == SpeechText.Lang.EN) p.pitchEn else p.pitchRu)
        val id = UUID.randomUUID().toString()
        val done = CompletableDeferred<Unit>()
        pending[id] = done
        tts.speak(seg.text, TextToSpeech.QUEUE_FLUSH, null, id)
        withTimeoutOrNull(20_000) { done.await() }
        pending.remove(id)
    }

    private suspend fun playYandex(ya: YandexSpeech, seg: SpeechText.Segment, p: Prefs, slow: Boolean) {
        val (langCode, voice) = if (seg.lang == SpeechText.Lang.EN) "en-US" to "john" else "ru-RU" to "alena"
        val speed = if (slow) 0.6 else p.rate.toDouble()
        val key = sha1("$voice|$speed|${seg.text}")
        val file = File(context.filesDir, "tts/$key.mp3")
        if (!file.exists()) {
            val bytes = ya.synthesize(seg.text, langCode, voice, speed)
            withContext(Dispatchers.IO) { file.parentFile?.mkdirs(); file.writeBytes(bytes) }
        }
        suspendCancellableCoroutine { cont ->
            val mp = MediaPlayer()
            player = mp
            mp.setOnCompletionListener { it.release(); if (player === it) player = null; if (cont.isActive) cont.resume(Unit) }
            mp.setOnErrorListener { m, _, _ -> m.release(); if (player === m) player = null; if (cont.isActive) cont.resume(Unit); true }
            mp.setDataSource(file.path)
            mp.prepare()
            mp.start()
            cont.invokeOnCancellation { runCatching { mp.stop(); mp.release() } }
        }
    }

    private fun stopNow() {
        runCatching { tts.stop() }
        pending.values.forEach { it.complete(Unit) }
        pending.clear()
        player?.let { runCatching { it.stop(); it.release() } }
        player = null
    }

    fun stop() {
        job?.cancel()
        stopNow()
        _speaking.value = false
    }

    private fun sha1(s: String): String =
        MessageDigest.getInstance("SHA-1").digest(s.toByteArray()).joinToString("") { "%02x".format(it) }

    companion object {
        private val RU = Locale("ru", "RU")
    }
}
