package ru.sovushka.app

import android.app.Application
import android.content.Context
import ru.sovushka.app.data.Prefs
import ru.sovushka.app.data.Settings
import ru.sovushka.app.data.Store
import ru.sovushka.app.ocr.PageReader
import ru.sovushka.app.voice.Listener
import ru.sovushka.app.voice.Speaker
import ru.sovushka.core.ai.GigaChatClient
import ru.sovushka.core.ai.LlmClient
import ru.sovushka.core.ai.Tls
import ru.sovushka.core.ai.YandexGptClient
import ru.sovushka.core.ai.YandexSpeech
import ru.sovushka.core.ai.defaultHttpClient
import ru.sovushka.core.mesh.MeshClient
import java.io.File
import java.time.LocalDate

class SovushkaApp : Application() {
    lateinit var graph: AppGraph
        private set

    override fun onCreate() {
        super.onCreate()
        graph = AppGraph(this)
    }
}

val Context.graph: AppGraph get() = (applicationContext as SovushkaApp).graph

fun today(): Long = LocalDate.now().toEpochDay()

/** Все зависимости приложения в одном месте. */
class AppGraph(val context: Context) {
    val settings = Settings(context)
    val store = Store(File(context.filesDir, "data.json"))
    private val http = defaultHttpClient()
    val pages = PageReader(context)
    val speaker = Speaker(context, settings) { yandexSpeech() }
    val listener = Listener(context, settings) { yandexSpeech() }

    /** Встроенные сертификаты НУЦ Минцифры для GigaChat. */
    private val bundledCerts by lazy { runCatching { Tls.russianTrustedCa() }.getOrDefault(emptyList()) }

    /** Дополнительный сертификат, если Минцифры его сменит (родитель может загрузить новый в настройках). */
    val gigaCertFile: File get() = File(context.filesDir, "russian_trusted_root_ca.crt")

    fun gigaCertInstalled(): Boolean = gigaCertFile.exists()

    /** Сохранить сертификат после проверки, что это действительно X.509. */
    fun installGigaCert(bytes: ByteArray): String {
        val certs = Tls.parseCertificates(bytes)
        gigaCertFile.writeBytes(bytes)
        cachedKey = null
        return certs.joinToString { it.subjectX500Principal.name.substringAfter("CN=").substringBefore(',') }
    }

    private var cachedKey: String? = null
    private var cachedLlm: LlmClient? = null

    /** Клиент нейросети по текущим настройкам или null, если не настроено. */
    @Synchronized
    fun llm(): LlmClient? {
        val p = settings.value
        val key = listOf(p.provider, p.gigaKey, p.gigaScope, p.gigaModel, p.yaKey, p.yaFolder, p.yaModel, gigaCertInstalled()).joinToString("|")
        if (key == cachedKey) return cachedLlm
        val client = when (p.provider) {
            Prefs.PROVIDER_GIGACHAT -> if (p.gigaKey.isBlank()) null else {
                val imported = if (gigaCertInstalled()) runCatching { Tls.parseCertificates(gigaCertFile.readBytes()) }.getOrDefault(emptyList()) else emptyList()
                val extra = bundledCerts + imported
                GigaChatClient(p.gigaKey, p.gigaScope, p.gigaModel.ifBlank { "GigaChat" }, Tls.withExtraCertificates(http, extra))
            }
            Prefs.PROVIDER_YANDEX -> if (p.yaKey.isBlank() || p.yaFolder.isBlank()) null else YandexGptClient(p.yaKey, p.yaFolder, p.yaModel, http)
            else -> null
        }
        cachedKey = key
        cachedLlm = client
        return client
    }

    fun yandexSpeech(): YandexSpeech? {
        val p = settings.value
        return if (p.yaKey.isBlank()) null else YandexSpeech(p.yaKey, p.yaFolder, http)
    }

    fun mesh(): MeshClient? = settings.value.meshToken.takeIf { it.isNotBlank() }?.let { MeshClient(it, http) }
}
