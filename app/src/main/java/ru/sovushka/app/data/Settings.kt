package ru.sovushka.app.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Настройки семьи. Хранятся только на телефоне (SharedPreferences приложения). */
data class Prefs(
    val setupDone: Boolean = false,
    val olderName: String = "",
    val olderAge: Int = 10,
    val youngerName: String = "",
    val youngerAge: Int = 5,
    val provider: String = PROVIDER_GIGACHAT,
    val gigaKey: String = "",
    val gigaScope: String = "GIGACHAT_API_PERS",
    val gigaModel: String = "GigaChat",
    val yaKey: String = "",
    val yaFolder: String = "",
    val yaModel: String = "yandexgpt-lite",
    val voice: String = VOICE_ANDROID,
    val accent: String = "en-GB",
    val rate: Float = 0.9f,
    /** Синтезатор речи (пакет движка); пусто — системный по умолчанию. */
    val ttsEngine: String = "",
    /** Выбранные голоса; пусто — лучший доступный автоматически. */
    val voiceEn: String = "",
    val voiceRu: String = "",
    /** Высота голоса отдельно для языков — чтобы два голоса звучали как один персонаж. */
    /** Голос Яндекса (API v3): название, амплуа и модель (livetts для голосов LiveTTS). */
    val yaVoice: String = "dasha",
    val yaRole: String = "friendly",
    val yaVoiceModel: String = "",
    val yaSpeed: Float = 1.05f,
    val yaPitch: Float = 29f,
    /** Голос Яндекса для математики (по умолчанию живой голос LiveTTS). */
    val mathVoice: String = "sofia",
    val mathRole: String = "casual",
    val mathVoiceModel: String = "livetts",
    val mathSpeed: Float = 1.0f,
    val pitchEn: Float = 1.08f,
    val pitchRu: Float = 1.08f,
    val meshToken: String = "",
    val meshStudentId: Long = -1,
    /** Меняется при смене фото детей — чтобы экраны перечитали аватарки. */
    val avatarStamp: Long = 0,
) {
    companion object {
        const val PROVIDER_NONE = "none"
        const val PROVIDER_GIGACHAT = "gigachat"
        const val PROVIDER_YANDEX = "yandex"
        const val VOICE_ANDROID = "android"
        const val VOICE_YANDEX = "yandex"
    }
}

class Settings(context: Context) {
    private val sp = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val _prefs = MutableStateFlow(read())
    val prefs: StateFlow<Prefs> = _prefs
    val value: Prefs get() = _prefs.value

    private fun read(): Prefs {
        val d = Prefs()
        return Prefs(
            setupDone = sp.getBoolean("setupDone", d.setupDone),
            olderName = sp.getString("olderName", d.olderName)!!,
            olderAge = sp.getInt("olderAge", d.olderAge),
            youngerName = sp.getString("youngerName", d.youngerName)!!,
            youngerAge = sp.getInt("youngerAge", d.youngerAge),
            provider = sp.getString("provider", d.provider)!!,
            gigaKey = sp.getString("gigaKey", d.gigaKey)!!,
            gigaScope = sp.getString("gigaScope", d.gigaScope)!!,
            gigaModel = sp.getString("gigaModel", d.gigaModel)!!,
            yaKey = sp.getString("yaKey", d.yaKey)!!,
            yaFolder = sp.getString("yaFolder", d.yaFolder)!!,
            yaModel = sp.getString("yaModel", d.yaModel)!!,
            voice = sp.getString("voice", d.voice)!!,
            accent = sp.getString("accent", d.accent)!!,
            rate = sp.getFloat("rate", d.rate),
            ttsEngine = sp.getString("ttsEngine", d.ttsEngine)!!,
            voiceEn = sp.getString("voiceEn", d.voiceEn)!!,
            voiceRu = sp.getString("voiceRu", d.voiceRu)!!,
            yaVoice = sp.getString("yaVoice", d.yaVoice)!!,
            yaRole = sp.getString("yaRole", d.yaRole)!!,
            yaVoiceModel = sp.getString("yaVoiceModel", d.yaVoiceModel)!!,
            yaSpeed = sp.getFloat("yaSpeed", d.yaSpeed),
            yaPitch = sp.getFloat("yaPitch", d.yaPitch),
            mathVoice = sp.getString("mathVoice", d.mathVoice)!!,
            mathRole = sp.getString("mathRole", d.mathRole)!!,
            mathVoiceModel = sp.getString("mathVoiceModel", d.mathVoiceModel)!!,
            mathSpeed = sp.getFloat("mathSpeed", d.mathSpeed),
            pitchEn = sp.getFloat("pitchEn", d.pitchEn),
            pitchRu = sp.getFloat("pitchRu", d.pitchRu),
            meshToken = sp.getString("meshToken", d.meshToken)!!,
            meshStudentId = sp.getLong("meshStudentId", d.meshStudentId),
            avatarStamp = sp.getLong("avatarStamp", d.avatarStamp),
        )
    }

    fun update(f: (Prefs) -> Prefs) {
        val p = f(_prefs.value)
        sp.edit()
            .putBoolean("setupDone", p.setupDone)
            .putString("olderName", p.olderName).putInt("olderAge", p.olderAge)
            .putString("youngerName", p.youngerName).putInt("youngerAge", p.youngerAge)
            .putString("provider", p.provider)
            .putString("gigaKey", p.gigaKey).putString("gigaScope", p.gigaScope).putString("gigaModel", p.gigaModel)
            .putString("yaKey", p.yaKey).putString("yaFolder", p.yaFolder).putString("yaModel", p.yaModel)
            .putString("voice", p.voice).putString("accent", p.accent).putFloat("rate", p.rate)
            .putString("ttsEngine", p.ttsEngine).putString("voiceEn", p.voiceEn).putString("voiceRu", p.voiceRu)
            .putString("yaVoice", p.yaVoice).putString("yaRole", p.yaRole).putString("yaVoiceModel", p.yaVoiceModel)
            .putFloat("yaSpeed", p.yaSpeed).putFloat("yaPitch", p.yaPitch)
            .putFloat("pitchEn", p.pitchEn).putFloat("pitchRu", p.pitchRu)
            .putString("mathVoice", p.mathVoice).putString("mathRole", p.mathRole).putString("mathVoiceModel", p.mathVoiceModel)
            .putFloat("mathSpeed", p.mathSpeed)
            .putString("meshToken", p.meshToken).putLong("meshStudentId", p.meshStudentId)
            .putLong("avatarStamp", p.avatarStamp)
            .apply()
        _prefs.value = p
    }
}
