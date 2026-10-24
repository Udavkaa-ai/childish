package ru.sovushka.app.ui

import android.content.Intent
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import ru.sovushka.app.BuildConfigInfo
import ru.sovushka.app.data.Prefs
import ru.sovushka.app.graph
import ru.sovushka.app.voice.Speaker
import ru.sovushka.core.SpeechText
import ru.sovushka.core.ai.ChatMessage
import ru.sovushka.core.ai.YandexSpeech

private const val MIXED_SAMPLE = "Привет! Меня зовут Ника. Hello! My name is Nika. I like apples and bananas. Давай учить английский!"

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    CardBox {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun Radio(text: String, selected: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick), verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected, onClick)
        Text(text)
    }
}

@Composable
private fun SecretField(value: String, label: String, onChange: (String) -> Unit) {
    var show by remember { mutableStateOf(false) }
    OutlinedTextField(
        value, { onChange(it.trim()) }, label = { Text(label) }, singleLine = true, modifier = Modifier.fillMaxWidth(),
        visualTransformation = if (show) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = { Text(if (show) "🙈" else "👁", Modifier.clickable { show = !show }.padding(8.dp)) },
    )
}

@Composable
fun SettingsScreen(vm: AppViewModel) {
    val ctx = LocalContext.current
    val g = ctx.graph
    val p by g.settings.prefs.collectAsState()
    val missingVoice by g.speaker.missingVoice.collectAsState()
    val yandexError by g.speaker.yandexError.collectAsState()
    val scope = rememberCoroutineScope()
    // Результаты проверки, экспорта и импорта показываем окном: экран длинный, текст наверху не видно.
    var status by remember { mutableStateOf<String?>(null) }
    var checking by remember { mutableStateOf(false) }
    var voiceDialog by remember { mutableStateOf<SpeechText.Lang?>(null) }
    var certInfo by remember { mutableStateOf(if (g.gigaCertInstalled()) "встроен + загружен дополнительный ✅" else "встроен в приложение ✅") }
    fun set(f: (Prefs) -> Prefs) = g.settings.update(f)

    val pickCert = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            certInfo = try {
                val bytes = withContext(Dispatchers.IO) { ctx.contentResolver.openInputStream(uri)!!.use { it.readBytes() } }
                "установлен ✅ (" + g.installGigaCert(bytes) + ")"
            } catch (e: Exception) {
                "не подошёл: ${e.message}"
            }
        }
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            status = runCatching {
                withContext(Dispatchers.IO) { ctx.contentResolver.openOutputStream(uri)!!.use { it.write(g.store.exportJson().toByteArray()) } }
                "Резервная копия сохранена ✅"
            }.getOrElse { "Не получилось сохранить: ${it.message}" }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            status = runCatching {
                val text = withContext(Dispatchers.IO) { ctx.contentResolver.openInputStream(uri)!!.use { it.readBytes().decodeToString() } }
                g.store.importJson(text)
                "Данные восстановлены ✅"
            }.getOrElse { "Это не резервная копия приложения: ${it.message}" }
        }
    }

    GradientScreen {
        Column(Modifier.fillMaxSize()) {
            TopBar("Настройки", onBack = { vm.back() })
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {

                Section("👧 Дети") {
                    OutlinedTextField(p.olderName, { v -> set { it.copy(olderName = v) } }, label = { Text("Старшая") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(p.olderAge.toString(), { v -> v.filter(Char::isDigit).take(2).toIntOrNull()?.let { a -> set { it.copy(olderAge = a) } } },
                        label = { Text("Возраст") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(p.youngerName, { v -> set { it.copy(youngerName = v) } }, label = { Text("Младшая (пусто — скрыть)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }

                Section("🧠 Нейросеть") {
                    Radio("GigaChat (Сбер) — есть бесплатный лимит для физлиц", p.provider == Prefs.PROVIDER_GIGACHAT) { set { it.copy(provider = Prefs.PROVIDER_GIGACHAT) } }
                    Radio("YandexGPT (Yandex Cloud)", p.provider == Prefs.PROVIDER_YANDEX) { set { it.copy(provider = Prefs.PROVIDER_YANDEX) } }
                    Radio("Без нейросети (только готовые темы и ручные слова)", p.provider == Prefs.PROVIDER_NONE) { set { it.copy(provider = Prefs.PROVIDER_NONE) } }
                    if (p.provider == Prefs.PROVIDER_GIGACHAT) {
                        Hint("developers.sber.ru → Studio → проект GigaChat API → «Настройки API» → «Получить ключ». Скопируйте Authorization Key.")
                        SecretField(p.gigaKey, "Authorization Key") { v -> set { it.copy(gigaKey = v) } }
                        Text("Тип доступа:")
                        listOf("GIGACHAT_API_PERS" to "Физлицо (PERS)", "GIGACHAT_API_B2B" to "Юрлицо, пакеты (B2B)", "GIGACHAT_API_CORP" to "Юрлицо, оплата по факту (CORP)").forEach { (v, t) ->
                            Radio(t, p.gigaScope == v) { set { it.copy(gigaScope = v) } }
                        }
                        OutlinedTextField(p.gigaModel, { v -> set { it.copy(gigaModel = v.trim()) } }, label = { Text("Модель (GigaChat, GigaChat-Pro, GigaChat-Max…)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        Text("Сертификат НУЦ Минцифры: $certInfo")
                        Hint("Российский корневой сертификат для GigaChat уже встроен (действует до 2032 г.) и используется только для запросов к GigaChat. Загружать свой нужно, только если Минцифры выпустит новый.")
                        TextButton(onClick = { pickCert.launch(arrayOf("*/*")) }) { Text("📄 Загрузить другой сертификат") }
                    }
                    if (p.provider == Prefs.PROVIDER_YANDEX) YandexFields(p) { f -> set(f) }
                    if (p.provider != Prefs.PROVIDER_NONE) {
                        PillButton(if (checking) "⏳ Проверяю…" else "🔌 Проверить связь", brush = Palette.bluish, enabled = !checking) {
                            checking = true
                            scope.launch {
                                status = try {
                                    val llm = g.llm() ?: throw IllegalStateException("заполните ключ авторизации")
                                    val r = withTimeoutOrNull(45_000) {
                                        llm.chat(listOf(ChatMessage(ChatMessage.Role.USER, "Ответь одним словом по-английски: как будет «кошка»?")), maxTokens = 20)
                                    } ?: throw IllegalStateException("нет ответа 45 секунд. Проверьте интернет (VPN лучше выключить).")
                                    "${llm.name} отвечает: «$r» ✅\n\nВсё работает!"
                                } catch (e: Throwable) {
                                    if (e is kotlinx.coroutines.CancellationException) throw e
                                    "Не получилось ❌\n\n${e.message ?: e.javaClass.simpleName}"
                                } finally {
                                    checking = false
                                }
                            }
                        }
                        if (checking) LinearProgressIndicator(Modifier.fillMaxWidth())
                    }
                }

                Section("🔊 Голос") {
                    Radio("Голос телефона (работает без интернета)", p.voice == Prefs.VOICE_ANDROID) { set { it.copy(voice = Prefs.VOICE_ANDROID) } }
                    Radio("Yandex SpeechKit (нужен ключ Yandex Cloud)", p.voice == Prefs.VOICE_YANDEX) { set { it.copy(voice = Prefs.VOICE_YANDEX) } }
                    if (p.voice == Prefs.VOICE_YANDEX) {
                        if (p.provider != Prefs.PROVIDER_YANDEX) YandexFields(p) { f -> set(f) }
                        Text("Голос Ники (один голос читает и русский, и английский):")
                        YandexSpeech.GIRL_VOICES.forEach { v ->
                            Radio(v.title, p.yaVoice == v.name && p.yaRole == v.role) {
                                set { it.copy(yaVoice = v.name, yaRole = v.role, yaVoiceModel = v.model) }
                                g.speaker.speak(MIXED_SAMPLE)
                            }
                        }
                        Text("Скорость речи: ${"%.2f".format(p.yaSpeed)}")
                        Slider(p.yaSpeed, { v -> set { it.copy(yaSpeed = v) } }, valueRange = 0.7f..1.5f)
                        Text("Высота голоса: ${Math.round(p.yaPitch)}")
                        Slider(p.yaPitch, { v -> set { it.copy(yaPitch = v) } }, valueRange = -300f..300f)
                        yandexError?.let { Text("⚠️ Яндекс не ответил, говорит голос телефона: $it", color = Palette.Bad, fontSize = 14.sp) }
                        PillButton("🎧 Послушать вместе", brush = Palette.button) { g.speaker.speak(MIXED_SAMPLE) }
                    }
                    if (p.voice == Prefs.VOICE_ANDROID) {
                        val engines = remember { g.speaker.engines() }
                        if (engines.size > 1) {
                            Text("Синтезатор:")
                            Radio("По умолчанию в телефоне", p.ttsEngine.isBlank()) {
                                set { it.copy(ttsEngine = "", voiceEn = "", voiceRu = "") }; g.speaker.setEngine("")
                            }
                            engines.forEach { (pkg, label) ->
                                Radio(label, p.ttsEngine == pkg) {
                                    set { it.copy(ttsEngine = pkg, voiceEn = "", voiceRu = "") }; g.speaker.setEngine(pkg)
                                }
                            }
                        }
                        Hint("Лучше всего звучат нейросетевые голоса «Синтезатора речи Google» (Speech Services by Google, есть в RuStore и Google Play). После установки скачайте в его настройках английский (Великобритания) и русский голоса.")
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            PillButton("🇬🇧 Английский голос", Modifier.weight(1f), brush = Palette.pinkish) { voiceDialog = SpeechText.Lang.EN }
                            PillButton("🇷🇺 Русский голос", Modifier.weight(1f), brush = Palette.bluish) { voiceDialog = SpeechText.Lang.RU }
                        }
                        Hint("Чтобы Ника звучала как один человек, выберите два женских голоса похожего тембра и подровняйте высоту.")
                        Text("Высота английского голоса: ${"%.2f".format(p.pitchEn)}")
                        Slider(p.pitchEn, { v -> set { it.copy(pitchEn = v) } }, valueRange = 0.7f..1.5f)
                        Text("Высота русского голоса: ${"%.2f".format(p.pitchRu)}")
                        Slider(p.pitchRu, { v -> set { it.copy(pitchRu = v) } }, valueRange = 0.7f..1.5f)
                        PillButton("🎧 Послушать вместе", brush = Palette.button) {
                            g.speaker.speak(MIXED_SAMPLE)
                        }
                    }
                    Text("Произношение:")
                    Radio("Британское (как в школьных учебниках)", p.accent == "en-GB") { set { it.copy(accent = "en-GB") } }
                    Radio("Американское", p.accent == "en-US") { set { it.copy(accent = "en-US") } }
                    Text("Скорость речи: ${"%.2f".format(p.rate)}")
                    Slider(p.rate, { v -> set { it.copy(rate = v) } }, valueRange = 0.5f..1.2f)
                    PillButton("▶️ Проверить голос", brush = Palette.bluish) { g.speaker.speak("Привет! I am Nika. Let's learn English!") }
                    if (missingVoice) {
                        Hint("На телефоне нет английского или русского голоса. Установите голосовые данные:")
                        TextButton(onClick = {
                            runCatching { ctx.startActivity(Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                        }) { Text("Установить голоса") }
                    }
                }

                Section("🏫 Дневник МЭШ") {
                    Text(if (p.meshToken.isBlank()) "Не подключён" else "Подключён ✅")
                    PillButton(if (p.meshToken.isBlank()) "Подключить" else "Открыть дневник") { vm.go(Screen.Diary) }
                }

                Section("💾 Данные") {
                    Hint("Все уроки и прогресс хранятся только на телефоне. Сохраните резервную копию перед сменой телефона или переустановкой.")
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        PillButton("Сохранить копию") { exportLauncher.launch("english-nika-backup.json") }
                        PillButton("Восстановить", brush = Palette.bluish) { importLauncher.launch(arrayOf("application/json", "*/*")) }
                    }
                }
                Hint("English с Никой ${BuildConfigInfo.versionName(ctx)} · Ника придумана Вероникой 💙💗")
            }
        }
    }

    voiceDialog?.let { lang -> VoicePicker(lang) { voiceDialog = null } }

    status?.let { msg ->
        AlertDialog(
            onDismissRequest = { status = null },
            confirmButton = { TextButton(onClick = { status = null }) { Text("OK") } },
            text = { Text(msg, style = MaterialTheme.typography.bodyLarge) },
        )
    }
}

/** Выбор голоса Ники с прослушиванием. Выбор сохраняется сразу. */
@Composable
private fun VoicePicker(lang: SpeechText.Lang, onClose: () -> Unit) {
    val g = LocalContext.current.graph
    val p by g.settings.prefs.collectAsState()
    val voices by produceState<List<Speaker.VoiceOption>?>(null, lang, p.ttsEngine, p.accent) { value = g.speaker.voices(lang) }
    val sample = if (lang == SpeechText.Lang.EN) "Hello! I am Nika. Let's learn English together!" else "Привет! Я Ника. Давай учить английский!"
    val selected = if (lang == SpeechText.Lang.EN) p.voiceEn else p.voiceRu
    fun choose(name: String) {
        g.settings.update { if (lang == SpeechText.Lang.EN) it.copy(voiceEn = name) else it.copy(voiceRu = name) }
    }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(if (lang == SpeechText.Lang.EN) "Английский голос Ники" else "Русский голос Ники") },
        text = {
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                val list = voices
                when {
                    list == null -> LinearProgressIndicator(Modifier.fillMaxWidth())
                    list.isEmpty() -> Text("В этом синтезаторе нет голосов для языка. Установите голосовые данные или выберите другой синтезатор.")
                    else -> {
                        Radio("Автоматически — лучший доступный", selected.isBlank()) { choose(""); g.speaker.speak(sample, lang) }
                        list.forEach { v ->
                            Radio(v.title, selected == v.name) { choose(v.name); g.speaker.speak(sample, lang) }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text("Готово") } },
        dismissButton = { TextButton(onClick = { g.speaker.speak(sample, lang) }) { Text("▶️ Послушать") } },
    )
}

@Composable
private fun YandexFields(p: Prefs, set: ((Prefs) -> Prefs) -> Unit) {
    Hint("console.yandex.cloud → сервисный аккаунт с ролями ai.languageModels.user и ai.speechkit-*.user → API-ключ. ID каталога — на странице каталога.")
    SecretField(p.yaKey, "API-ключ") { v -> set { it.copy(yaKey = v) } }
    OutlinedTextField(p.yaFolder, { v -> set { it.copy(yaFolder = v.trim()) } }, label = { Text("ID каталога (b1g…)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
    if (p.provider == Prefs.PROVIDER_YANDEX) {
        Radio("YandexGPT Lite (дешевле)", p.yaModel == "yandexgpt-lite") { set { it.copy(yaModel = "yandexgpt-lite") } }
        Radio("YandexGPT Pro (умнее)", p.yaModel == "yandexgpt") { set { it.copy(yaModel = "yandexgpt") } }
    }
}
