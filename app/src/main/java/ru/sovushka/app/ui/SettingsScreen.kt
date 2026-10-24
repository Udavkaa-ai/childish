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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import ru.sovushka.app.BuildConfigInfo
import ru.sovushka.app.data.Prefs
import ru.sovushka.app.graph
import ru.sovushka.core.ai.ChatMessage

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
    val scope = rememberCoroutineScope()
    // Результаты проверки, экспорта и импорта показываем окном: экран длинный, текст наверху не видно.
    var status by remember { mutableStateOf<String?>(null) }
    var checking by remember { mutableStateOf(false) }
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
                    if (p.voice == Prefs.VOICE_YANDEX && p.provider != Prefs.PROVIDER_YANDEX) YandexFields(p) { f -> set(f) }
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

    status?.let { msg ->
        AlertDialog(
            onDismissRequest = { status = null },
            confirmButton = { TextButton(onClick = { status = null }) { Text("OK") } },
            text = { Text(msg, style = MaterialTheme.typography.bodyLarge) },
        )
    }
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
