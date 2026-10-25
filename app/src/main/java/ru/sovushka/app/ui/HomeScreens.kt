package ru.sovushka.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.sovushka.app.graph
import ru.sovushka.app.today
import ru.sovushka.core.ComicBook
import ru.sovushka.core.GrammarBank
import ru.sovushka.core.Srs
import ru.sovushka.core.mesh.DiaryInsights
import java.time.LocalDate
import kotlin.random.Random

/** Первое знакомство: Ника здоровается, родитель вписывает имена детей. */
@Composable
fun SetupScreen(vm: AppViewModel) {
    val g = LocalContext.current.graph
    val talking by g.speaker.speaking.collectAsState()
    var older by rememberSaveable { mutableStateOf(g.settings.value.olderName) }
    var olderAge by rememberSaveable { mutableStateOf(g.settings.value.olderAge.toString()) }
    var younger by rememberSaveable { mutableStateOf(g.settings.value.youngerName) }
    var youngerAge by rememberSaveable { mutableStateOf(g.settings.value.youngerAge.toString()) }
    LaunchedEffect(Unit) { g.speaker.say("Привет! Я Ника. Давай знакомиться!") }
    GradientScreen {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            NikaSays("Привет! Я Ника. Давай знакомиться! 👋", NikaMood.WAVE, talking)
            Text("Для родителей", style = MaterialTheme.typography.titleLarge)
            Hint("Имена нужны, чтобы Ника обращалась к детям. Всё хранится только на этом телефоне.")
            OutlinedTextField(older, { older = it }, label = { Text("Имя старшей (школьница)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(olderAge, { olderAge = it.filter(Char::isDigit).take(2) }, label = { Text("Возраст") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
            OutlinedTextField(younger, { younger = it }, label = { Text("Имя младшей (не читает)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(youngerAge, { youngerAge = it.filter(Char::isDigit).take(2) }, label = { Text("Возраст") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
            BigButton("✨", "Начать", enabled = older.isNotBlank()) {
                g.settings.update {
                    it.copy(
                        setupDone = true,
                        olderName = older.trim(), olderAge = olderAge.toIntOrNull() ?: 10,
                        youngerName = younger.trim(), youngerAge = youngerAge.toIntOrNull() ?: 5,
                    )
                }
                vm.home(Screen.Profiles)
            }
            Hint("Нейросеть, голос и дневник МЭШ подключаются потом в настройках ⚙️.")
        }
    }
}

/** Кто будет заниматься? Две большие карточки. */
@Composable
fun ProfilesScreen(vm: AppViewModel) {
    val g = LocalContext.current.graph
    val prefs by g.settings.prefs.collectAsState()
    val talking by g.speaker.speaking.collectAsState()
    LaunchedEffect(Unit) { g.speaker.say("Кто сегодня будет заниматься?") }
    GradientScreen {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            NikaSays("Кто сегодня будет заниматься?", NikaMood.WAVE, talking, nikaHeight = 190.dp)
            AuthorBadge()
            ProfileCard(true, "👧", prefs.olderName.ifBlank { "Старшая" }, "Учебник, правила, повторение", Palette.pinkish, appear = 0) { vm.go(Screen.OlderHome) }
            if (prefs.youngerName.isNotBlank()) {
                ProfileCard(false, "👧", prefs.youngerName, "Картинки и слова на слух", Palette.bluish, appear = 1) { vm.go(Screen.YoungerHome) }
            }
            Spacer(Modifier.weight(1f))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Row(
                    Modifier.glass(RoundedCornerShape(50), Color.White.copy(alpha = 0.6f))
                        .clickable { vm.go(Screen.ParentGate(Screen.Settings)) }.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AppIcon(AppIcons.GEAR, size = 24.dp)
                    Text("  Для родителей", color = Palette.Muted, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/** Подпись автора персонажа. */
@Composable
private fun AuthorBadge() {
    Row(
        Modifier.glass(RoundedCornerShape(50), Color.White.copy(alpha = 0.62f)).padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(AppIcons.SPARKLE, size = 22.dp)
        Text(
            "  Ника — персонаж, которого придумала Вероника Капырина",
            fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Palette.Ink,
        )
    }
}

@Composable
private fun ProfileCard(older: Boolean, emoji: String, name: String, subtitle: String, brush: androidx.compose.ui.graphics.Brush, appear: Int, onClick: () -> Unit) {
    GlassButton(onClick, Modifier.fillMaxWidth(), brush = brush, shape = RoundedCornerShape(30.dp), depth = 8.dp, appear = appear, fillWidth = true,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp)) {
        KidAvatar(older, emoji, 84.dp)
        Spacer(Modifier.width(16.dp))
        Column {
            Text(name, color = Color.White, fontSize = 31.sp, fontWeight = FontWeight.Black, style = androidx.compose.ui.text.TextStyle(shadow = GlassTextShadow))
            Text(subtitle, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, style = androidx.compose.ui.text.TextStyle(shadow = GlassTextShadow))
        }
    }
}

/** Простой «родительский замок»: пример на умножение, который пятилетка не решит. */
@Composable
fun ParentGateScreen(vm: AppViewModel, next: Screen) {
    val a = remember { Random.nextInt(4, 10) }
    val b = remember { Random.nextInt(4, 10) }
    var answer by remember { mutableStateOf("") }
    var wrong by remember { mutableIntStateOf(0) }
    GradientScreen {
        Column(Modifier.fillMaxSize()) {
            TopBar("Для родителей", onBack = { vm.back() })
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Сколько будет $a × $b?", style = MaterialTheme.typography.headlineMedium)
                OutlinedTextField(
                    answer, { answer = it.filter(Char::isDigit).take(3) }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(),
                    isError = wrong > 0,
                )
                PillButton("Открыть", Modifier.fillMaxWidth()) {
                    if (answer.toIntOrNull() == a * b) vm.replace(next) else { wrong++; answer = "" }
                }
            }
        }
    }
}

/** Главный экран школьницы. */
@Composable
fun OlderHomeScreen(vm: AppViewModel) {
    val g = LocalContext.current.graph
    val data by g.store.data.collectAsState()
    val prefs by g.settings.prefs.collectAsState()
    val talking by g.speaker.speaking.collectAsState()
    val t = today()
    val due = data.dueWords(t).size
    val dueTopics = data.dueTopics(t).size
    val learnedTopics = data.grammar.count { it.value.box >= Srs.LEARNED_BOX }
    val learned = data.words.count { it.box >= Srs.LEARNED_BOX }
    val greeting = remember { greetingFor(prefs.olderName) }
    LaunchedEffect(Unit) { g.speaker.say(greeting) }
    val diary = data.diary
    val todayIso = LocalDate.now().toString()
    val homework = diary?.let { DiaryInsights.englishHomework(it, todayIso).firstOrNull() }

    GradientScreen {
        Column(Modifier.fillMaxSize()) {
            TopBar(prefs.olderName.ifBlank { "Привет!" }, onBack = { vm.back() }) {
                KidAvatar(true, "👧", 40.dp, Modifier.padding(end = 10.dp))
                CounterBadge(AppIcons.STAR, data.older.stars)
                Spacer(Modifier.width(6.dp))
                CounterBadge(AppIcons.FLAME, data.older.visibleStreak(t), flicker = data.older.visibleStreak(t) > 0)
            }
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                NikaSays(greeting, if (talking) NikaMood.TALK else NikaMood.IDLE, talking, nikaHeight = 160.dp)
                if (diary != null) {
                    CardBox {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("🏫 Английский в школе", style = MaterialTheme.typography.titleMedium)
                            DiaryInsights.englishAverage(diary)?.let { Text("Средний балл: $it") }
                            val marks = DiaryInsights.englishMarks(diary).take(6)
                            if (marks.isNotEmpty()) Row { Text("Оценки: "); marks.forEach { MarkBadge(it.value) } }
                            if (homework != null) {
                                Text("Задано на ${shortDate(homework.date)}: ${homework.text}", fontSize = 15.sp)
                                DiaryInsights.pages(homework.text).firstOrNull()?.let { page ->
                                    PillButton("📸 Сфоткать страницу $page") {
                                        vm.capture.reset(); vm.capture.note = "стр. $page"; vm.go(Screen.Capture)
                                    }
                                }
                            }
                        }
                    }
                }
                BigButton("📸", "Новая страница учебника", subtitle = "Сфотографируй — Ника сделает урок", appear = 0) {
                    if (vm.capture.draft != null || vm.capture.busy) vm.go(Screen.Capture) else { vm.capture.reset(); vm.go(Screen.Capture) }
                }
                BigButton(
                    "🔁", "Повторение",
                    subtitle = when {
                        due > 0 && dueTopics > 0 -> "Слов: $due · правил: $dueTopics"
                        due > 0 -> "Пора повторить слов: $due"
                        dueTopics > 0 -> "Пора повторить правил: $dueTopics"
                        else -> "Всё повторено — молодец!"
                    },
                    brush = Palette.pinkish, enabled = due > 0 || dueTopics > 0, appear = 1,
                ) { vm.go(Screen.Practice(null)) }
                BigButton(
                    "📘", "Правила",
                    subtitle = "Темы 2–4 класса · выучено $learnedTopics из ${GrammarBank.topics.size}",
                    brush = Palette.buttonSoft, appear = 2,
                ) { vm.go(Screen.Grammar) }
                val unread = ComicBook.comics.count { it.id !in data.comicsRead }
                BigButton(
                    "🖼", "Комиксы с Никой",
                    subtitle = if (unread > 0) "Новых: $unread · читаем вместе с Никой" else "Все прочитаны — перечитай любимый!",
                    brush = Palette.pinkish, appear = 3,
                ) { vm.go(Screen.Comics) }
                BigButton("📚", "Мои уроки", subtitle = "Уроков: ${data.lessons.size} · выучено слов: $learned", brush = Palette.bluish, appear = 4) { vm.go(Screen.Lessons) }
                BigButton("💬", "Поговорить с Никой", subtitle = "Спроси что угодно или скажи «проверь меня»", brush = Palette.buttonSoft, appear = 5) { vm.go(Screen.Tutor()) }
                BigButton("🎒", "Готовые темы", subtitle = "Животные, еда, цвета и другие", brush = Palette.bluish, appear = 6) { vm.go(Screen.Packs) }
                BigButton("🏫", "Дневник МЭШ", subtitle = "Расписание, оценки и домашка", brush = Palette.pinkish, appear = 7) { vm.go(Screen.Diary) }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

private fun greetingFor(name: String): String {
    val n = name.ifBlank { "подружка" }
    return listOf(
        "Привет, $n! Что будем делать сегодня?",
        "$n, рада тебя видеть! Let's learn English!",
        "Hello, $n! Готова к приключениям?",
    ).random()
}

@Composable
fun MarkBadge(value: String) {
    val color = when (value.trim().take(1)) {
        "5" -> Palette.Good
        "4" -> Palette.Blue
        "3" -> Color(0xFFFFA000)
        "2", "1" -> Palette.Bad
        else -> Palette.Muted
    }
    Box(
        Modifier.padding(end = 4.dp).size(30.dp).clip(RoundedCornerShape(8.dp)).background(color),
        contentAlignment = Alignment.Center,
    ) { Text(value.take(2), color = Color.White, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center) }
}

fun shortDate(iso: String): String = runCatching {
    val d = LocalDate.parse(iso.take(10))
    val months = listOf("янв", "фев", "мар", "апр", "мая", "июн", "июл", "авг", "сен", "окт", "ноя", "дек")
    "${d.dayOfMonth} ${months[d.monthValue - 1]}"
}.getOrDefault(iso)
