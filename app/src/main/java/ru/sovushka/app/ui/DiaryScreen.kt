package ru.sovushka.app.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.launch
import ru.sovushka.app.AppGraph
import ru.sovushka.app.graph
import ru.sovushka.core.mesh.DiaryInsights
import ru.sovushka.core.mesh.MeshAuthException
import ru.sovushka.core.mesh.MeshClient
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Date
import java.util.Locale

/** Загрузить дневник МЭШ и сохранить снимок. При просроченном токене пробуем обновить его один раз. */
suspend fun syncDiary(g: AppGraph): String? {
    val client = g.mesh() ?: return "Сначала войдите в МЭШ"
    suspend fun load() {
        val acc = client.account()
        val prefs = g.settings.value
        val student = MeshClient.pickStudent(acc, prefs.meshStudentId.takeIf { it > 0 }, prefs.olderName)
        if (student.id != prefs.meshStudentId) g.settings.update { it.copy(meshStudentId = student.id) }
        val snap = client.snapshot(acc, student, LocalDate.now())
        g.store.update { it.copy(diary = snap) }
    }
    return try {
        try {
            load()
        } catch (e: MeshAuthException) {
            val fresh = client.refresh() ?: throw e
            g.settings.update { it.copy(meshToken = fresh) }
            load()
        }
        null
    } catch (e: MeshAuthException) {
        val exp = MeshClient.tokenExpiry(g.settings.value.meshToken)
        val expired = exp != null && exp < System.currentTimeMillis()
        (if (expired) "Ключ МЭШ истёк " + SimpleDateFormat("d MMMM, HH:mm", Locale("ru")).format(Date(exp!!)) + "." else "МЭШ не принял ключ.") +
            " Скопируйте свежий aupd_token с school.mos.ru и подключите заново.\n\nПодробности: ${e.message}"
    } catch (e: Exception) {
        e.message ?: e.toString()
    }
}

@Composable
fun DiaryScreen(vm: AppViewModel) {
    val g = LocalContext.current.graph
    val prefs by g.settings.prefs.collectAsState()
    val data by g.store.data.collectAsState()
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var manualToken by remember { mutableStateOf("") }
    val clipboard = LocalClipboardManager.current
    val diary = data.diary
    val todayIso = LocalDate.now().toString()

    fun refresh() = scope.launch {
        busy = true; error = null
        error = syncDiary(g)
        busy = false
    }
    LaunchedEffect(prefs.meshToken) {
        val stale = diary == null || System.currentTimeMillis() - diary.syncedAt > 3 * 3600_000
        if (prefs.meshToken.isNotBlank() && stale) refresh()
    }

    GradientScreen {
        Column(Modifier.fillMaxSize()) {
            TopBar("Дневник МЭШ", onBack = { vm.back() }) {
                if (prefs.meshToken.isNotBlank()) Text("🔄", fontSize = 24.sp, modifier = Modifier.clickable(enabled = !busy) { refresh() }.padding(10.dp))
            }
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 16.dp))
            LazyColumn(
                Modifier.weight(1f).navigationBarsPadding().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                error?.let { e ->
                    item {
                        CardBox {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("⚠️ $e", color = Palette.Bad)
                                if (prefs.meshToken.isNotBlank()) {
                                    PillButton("🔑 Подключить заново") {
                                        g.settings.update { it.copy(meshToken = "", meshStudentId = -1) }
                                        error = null
                                    }
                                }
                            }
                        }
                    }
                }
                if (prefs.meshToken.isBlank()) {
                    item {
                        NikaSays("Подключи школьный дневник — я буду знать, что задали по английскому!", NikaMood.WAVE, false, nikaHeight = 150.dp)
                    }
                    item {
                        CardBox {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("💻 Подключение через компьютер", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "1. На компьютере откройте school.mos.ru в Chrome или Яндекс Браузере и войдите как обычно.\n" +
                                        "2. Нажмите F12 → вкладка «Application» («Приложение») → Cookies → https://school.mos.ru.\n" +
                                        "3. Найдите строку aupd_token и скопируйте её значение (длинная строка, начинается с eyJ).\n" +
                                        "4. Отправьте её себе на телефон (например, в «Избранное» мессенджера), скопируйте и нажмите кнопку ниже.",
                                    fontSize = 15.sp,
                                )
                                OutlinedTextField(
                                    manualToken, { manualToken = it.trim() }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                                    placeholder = { Text("eyJ…") },
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    PillButton("📋 Вставить из буфера", brush = Palette.bluish) {
                                        manualToken = clipboard.getText()?.text?.trim().orEmpty()
                                    }
                                    PillButton("Сохранить", enabled = MeshClient.looksLikeToken(manualToken)) {
                                        g.settings.update { it.copy(meshToken = manualToken.trim(), meshStudentId = -1) }
                                        manualToken = ""
                                    }
                                }
                                if (manualToken.isNotBlank() && !MeshClient.looksLikeToken(manualToken)) {
                                    Text("Это не похоже на ключ входа: нужна длинная строка из трёх частей через точку.", color = Palette.Bad, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                    item {
                        Column {
                            TextButton(onClick = { vm.go(Screen.MeshLogin) }) { Text("🔑 Попробовать войти прямо в приложении") }
                            Hint("mos.ru может блокировать вход через сторонние приложения — тогда страница входа будет пустой. В этом случае используйте способ через компьютер.")
                        }
                    }
                    item { Hint("Используется неофициальный API дневника МЭШ (как в OctoDiary). Пароль приложение не видит — вход идёт на сайте mos.ru.") }
                    return@LazyColumn
                }
                if (diary == null) {
                    if (!busy) item { Text("Нажмите 🔄, чтобы загрузить дневник.") }
                    return@LazyColumn
                }
                item {
                    Text(
                        "${diary.studentName} ${diary.className}".trim() + " · обновлено " +
                            SimpleDateFormat("d MMM, HH:mm", Locale("ru")).format(Date(diary.syncedAt)),
                        color = Palette.Muted, fontSize = 14.sp,
                    )
                }
                MeshClient.tokenExpiry(prefs.meshToken)?.let { exp ->
                    item {
                        val left = (exp - System.currentTimeMillis()) / 86_400_000
                        Text(
                            if (left < 0) "⚠️ Вход в МЭШ истёк — подключите заново" else "Вход действует до " +
                                SimpleDateFormat("d MMMM", Locale("ru")).format(Date(exp)),
                            color = if (left < 2) Palette.Bad else Palette.Muted, fontSize = 13.sp,
                        )
                    }
                }
                if (diary.problems.isNotEmpty()) {
                    item {
                        Text(
                            "Часть данных МЭШ не отдал: " + diary.problems.joinToString("; "),
                            color = Palette.Muted, fontSize = 12.sp,
                        )
                    }
                }
                item { EnglishCard(vm, diary, todayIso) }
                val days = diary.lessons.filter { it.date >= todayIso }.groupBy { it.date }.toSortedMap().entries.take(5)
                if (days.isNotEmpty()) item { Text("📅 Расписание", style = MaterialTheme.typography.titleLarge) }
                days.forEach { (date, lessons) ->
                    item(key = "day-$date") {
                        CardBox {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(dayTitle(date, todayIso), fontWeight = FontWeight.Bold, color = Palette.Pink)
                                lessons.forEach { l ->
                                    val eng = DiaryInsights.isEnglish(l.subject)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(l.time, color = Palette.Muted, fontSize = 14.sp, modifier = Modifier.width(48.dp))
                                        Text(
                                            (if (eng) "🇬🇧 " else "") + l.subject + (if (l.cancelled) " (отменён)" else ""),
                                            fontWeight = if (eng) FontWeight.Bold else FontWeight.Normal,
                                            modifier = Modifier.weight(1f),
                                        )
                                        l.marks.forEach { MarkBadge(it) }
                                    }
                                    l.homework.forEach { Text("📝 $it", fontSize = 14.sp, color = Palette.Muted, modifier = Modifier.padding(start = 48.dp)) }
                                }
                            }
                        }
                    }
                }
                if (diary.averages.isNotEmpty()) {
                    item { Text("📊 Средние баллы", style = MaterialTheme.typography.titleLarge) }
                    item {
                        CardBox {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                diary.averages.sortedBy { it.subject }.forEach { a ->
                                    Row {
                                        Text(a.subject, modifier = Modifier.weight(1f), fontWeight = if (DiaryInsights.isEnglish(a.subject)) FontWeight.Bold else FontWeight.Normal)
                                        Text(a.average, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
                if (diary.marks.isNotEmpty()) {
                    item { Text("✏️ Последние оценки", style = MaterialTheme.typography.titleLarge) }
                    items(diary.marks.take(25)) { m ->
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color.White).padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            MarkBadge(m.value)
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(m.subject, fontWeight = FontWeight.SemiBold)
                                Text(listOf(shortDate(m.date), m.form, m.comment).filter { it.isNotBlank() }.joinToString(" · "), fontSize = 13.sp, color = Palette.Muted)
                            }
                        }
                    }
                }
                item {
                    TextButton(onClick = {
                        g.settings.update { it.copy(meshToken = "", meshStudentId = -1) }
                        CookieManager.getInstance().removeAllCookies(null)
                        scope.launch { g.store.update { it.copy(diary = null) } }
                    }) { Text("Выйти из МЭШ", color = Palette.Bad) }
                }
            }
        }
    }
}

@Composable
private fun EnglishCard(vm: AppViewModel, diary: ru.sovushka.core.mesh.DiarySnapshot, todayIso: String) {
    CardBox {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("🇬🇧 Английский", style = MaterialTheme.typography.titleMedium)
            DiaryInsights.englishAverage(diary)?.let { Text("Средний балл: $it", fontWeight = FontWeight.Bold) }
            val marks = DiaryInsights.englishMarks(diary).take(8)
            if (marks.isNotEmpty()) Row { marks.forEach { MarkBadge(it.value) } }
            DiaryInsights.nextEnglishLesson(diary, todayIso)?.let { l ->
                Text("Следующий урок: ${dayTitle(l.date, todayIso)}, ${l.time}" + (if (l.theme.isNotBlank()) " — ${l.theme}" else ""))
            }
            DiaryInsights.englishHomework(diary, todayIso).take(3).forEach { h ->
                Text("📝 ${shortDate(h.date)}: ${h.text}", fontSize = 15.sp)
                DiaryInsights.pages(h.text).firstOrNull()?.let { page ->
                    PillButton("📸 Сфоткать стр. $page и выучить") {
                        vm.capture.reset(); vm.capture.note = "стр. $page"; vm.go(Screen.Capture)
                    }
                }
            }
        }
    }
}

private fun dayTitle(date: String, today: String): String {
    if (date == today) return "Сегодня"
    if (runCatching { LocalDate.parse(today).plusDays(1).toString() == date }.getOrDefault(false)) return "Завтра"
    return runCatching {
        val d = LocalDate.parse(date)
        val dow = listOf("пн", "вт", "ср", "чт", "пт", "сб", "вс")[d.dayOfWeek.value - 1]
        "$dow, ${shortDate(date)}"
    }.getOrDefault(date)
}

/** Вход на school.mos.ru во встроенном браузере: после входа забираем токен (cookie aupd_token). */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MeshLoginScreen(vm: AppViewModel) {
    val g = LocalContext.current.graph
    var loading by remember { mutableStateOf(true) }
    var url by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var web by remember { mutableStateOf<WebView?>(null) }
    var done by remember { mutableStateOf(false) }

    fun accept(token: String) {
        if (done) return
        done = true
        g.settings.update { it.copy(meshToken = token, meshStudentId = -1) }
        vm.back()
    }

    GradientScreen {
        Column(Modifier.fillMaxSize()) {
            TopBar("Вход в МЭШ", onBack = { vm.back() }) {
                Text("⟳", fontSize = 26.sp, modifier = Modifier.clickable { error = null; web?.reload() }.padding(10.dp))
            }
            Hint("Войдите как обычно на mos.ru (можно через Госуслуги). Пароль вводится на сайте — приложение его не видит.", Modifier.padding(horizontal = 16.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(url.removePrefix("https://").take(60), fontSize = 12.sp, color = Palette.Muted, modifier = Modifier.weight(1f))
            }
            error?.let { Text("⚠️ $it", color = Palette.Bad, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 16.dp)) }
            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp))
            AndroidView(
                modifier = Modifier.weight(1f).fillMaxWidth().navigationBarsPadding(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        web = this
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        @Suppress("DEPRECATION")
                        settings.databaseEnabled = true
                        settings.javaScriptCanOpenWindowsAutomatically = true
                        settings.loadWithOverviewMode = true
                        settings.useWideViewPort = true
                        settings.builtInZoomControls = true
                        settings.displayZoomControls = false
                        // Браузер не маскируем: mos.ru вправе блокировать вход через встроенные браузеры.
                        CookieManager.getInstance().setAcceptCookie(true)
                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                        webChromeClient = WebChromeClient()
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                val u = request?.url ?: return false
                                // Ссылки на приложения (intent://, dnevnik-mes://) встроенный браузер открыть не может — пропускаем.
                                return u.scheme != "http" && u.scheme != "https"
                            }

                            override fun onPageStarted(view: WebView?, u: String?, favicon: Bitmap?) {
                                loading = true; url = u.orEmpty()
                            }

                            override fun onPageFinished(view: WebView?, u: String?) {
                                loading = false; url = u.orEmpty()
                                findToken()?.let { accept(it); return }
                                // Запасной путь: токен может лежать в localStorage сайта школы.
                                if (u.orEmpty().contains("school.mos.ru") || u.orEmpty().contains("dnevnik.mos.ru")) {
                                    view?.evaluateJavascript(FIND_TOKEN_JS) { v ->
                                        val t = v?.trim('"').orEmpty()
                                        if (t.count { it == '.' } == 2 && t.length > 40) accept(t)
                                    }
                                }
                            }

                            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, err: WebResourceError?) {
                                if (request?.isForMainFrame == true) {
                                    loading = false
                                    error = "страница не загрузилась: ${err?.description} (${err?.errorCode})"
                                }
                            }

                            override fun onReceivedHttpError(view: WebView?, request: WebResourceRequest?, resp: WebResourceResponse?) {
                                if (request?.isForMainFrame != true) return
                                val code = resp?.statusCode ?: 0
                                if (code >= 400 && request.url.toString().startsWith(LOGIN_URLS.first())) {
                                    view?.loadUrl(LOGIN_URLS.last()) // прямой адрес входа не сработал — открываем главную
                                } else {
                                    error = "сайт ответил ошибкой $code"
                                }
                            }
                        }
                        loadUrl(LOGIN_URLS.first())
                    }
                },
                onRelease = { it.destroy() },
            )
        }
    }
}

/** Сразу на вход через mos.ru (СУДИР); главная school.mos.ru — запасной вариант. */
private val LOGIN_URLS = listOf("https://school.mos.ru/v3/auth/sudir/login", MeshClient.LOGIN_URL)

private const val FIND_TOKEN_JS = """(function(){try{for(var i=0;i<localStorage.length;i++){var k=localStorage.key(i);var v=localStorage.getItem(k)||'';if(/token/i.test(k)&&/^eyJ[\w-]+\.[\w-]+\.[\w-]+$/.test(v))return v;}}catch(e){}return '';})()"""

private fun findToken(): String? {
    val cm = CookieManager.getInstance()
    return listOf("https://school.mos.ru", "https://dnevnik.mos.ru").firstNotNullOfOrNull { host ->
        cm.getCookie(host)?.split(';')?.map { it.trim() }
            ?.firstOrNull { it.startsWith(MeshClient.TOKEN_COOKIE + "=") }
            ?.substringAfter('=')
            ?.takeIf { it.count { c -> c == '.' } == 2 }
    }
}
