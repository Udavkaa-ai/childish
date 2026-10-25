package ru.sovushka.app.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.sovushka.app.AppGraph
import ru.sovushka.app.graph
import ru.sovushka.app.voice.Talk
import ru.sovushka.app.today
import ru.sovushka.core.MathLesson
import ru.sovushka.core.SessionBuilder
import ru.sovushka.core.Srs
import ru.sovushka.core.TopicStats
import ru.sovushka.core.math.MathBank
import ru.sovushka.core.math.MathCheck
import ru.sovushka.core.math.MathProblem
import ru.sovushka.core.math.MathSession
import ru.sovushka.core.mesh.DiaryInsights
import java.io.File
import java.time.LocalDate
import java.util.UUID

/* ======================= Выбор предмета ======================= */

/** После выбора профиля: английский или математика. На карточках — Ника в нужном наряде. */
@Composable
fun SubjectChoiceScreen(vm: AppViewModel, older: Boolean) {
    val g = LocalContext.current.graph
    val prefs by g.settings.prefs.collectAsState()
    val talking by g.speaker.speaking.collectAsState()
    val name = if (older) prefs.olderName else prefs.youngerName
    val hello = "$name, чем займёмся? Английским или математикой?"
    LaunchedEffect(Unit) { g.speaker.say(hello) }
    GradientScreen {
        Column(Modifier.fillMaxSize()) {
            TopBar(name.ifBlank { "Привет!" }, onBack = { vm.back() }) { KidAvatar(older, "👧", 40.dp, Modifier.padding(end = 10.dp)) }
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                NikaSays(hello, if (talking) NikaMood.TALK else NikaMood.WAVE, talking, nikaHeight = 130.dp)
                SubjectCard(
                    "English", if (older) "Учебник, правила, комиксы, слова" else "Картинки и слова на слух",
                    Palette.pinkish, NikaOutfit.CASUAL, appear = 0,
                ) { vm.go(if (older) Screen.OlderHome else Screen.YoungerHome) }
                SubjectCard(
                    "Математика", if (older) "Учебник Моро, программа 3–4 класса" else "Считаем до 20",
                    MathPalette.primary, NikaOutfit.STRICT, appear = 1,
                ) { vm.go(if (older) Screen.MathHome else Screen.YoungerMath) }
            }
        }
    }
}

@Composable
private fun SubjectCard(title: String, subtitle: String, brush: Brush, outfit: NikaOutfit, appear: Int, onClick: () -> Unit) {
    GlassButton(
        onClick, Modifier.fillMaxWidth(), brush = brush, shape = RoundedCornerShape(30.dp), depth = 8.dp, appear = appear, fillWidth = true,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp, vertical = 10.dp),
    ) {
        CompositionLocalProvider(LocalNikaOutfit provides outfit) {
            Nika(if (outfit == NikaOutfit.STRICT) NikaMood.THINK else NikaMood.HAPPY, Modifier.height(130.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Black, style = TextStyle(shadow = GlassTextShadow))
            Text(subtitle, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, style = TextStyle(shadow = GlassTextShadow))
        }
    }
}

/* ======================= Главная математики ======================= */

@Composable
fun MathHomeScreen(vm: AppViewModel) {
    val g = LocalContext.current.graph
    val data by g.store.data.collectAsState()
    val prefs by g.settings.prefs.collectAsState()
    val talking by g.speaker.speaking.collectAsState()
    val t = today()
    val due = data.dueMathTopics(t).size
    val learned = data.mathTopics.count { it.value.box >= Srs.LEARNED_BOX }
    val greeting = remember {
        val n = prefs.olderName.ifBlank { "подружка" }
        listOf("$n, привет! Решим пару примеров?", "Привет, $n! Сегодня тренируем математику.", "$n, рада тебя видеть! Начнём с разминки?").random()
    }
    LaunchedEffect(Unit) { g.speaker.say(greeting) }
    val diary = data.diary
    val homework = diary?.let { DiaryInsights.mathHomework(it, LocalDate.now().toString()).firstOrNull() }

    GradientScreen {
        Column(Modifier.fillMaxSize()) {
            TopBar("Математика", onBack = { vm.back() }) {
                KidAvatar(true, "👧", 40.dp, Modifier.padding(end = 10.dp))
                CounterBadge(AppIcons.STAR, data.older.stars)
                Spacer(Modifier.width(6.dp))
                CounterBadge(AppIcons.FLAME, data.older.visibleStreak(t), flicker = data.older.visibleStreak(t) > 0)
            }
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                NikaSays(greeting, if (talking) NikaMood.TALK else NikaMood.IDLE, talking, nikaHeight = 150.dp)
                if (diary != null) {
                    CardBox {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("🏫 Математика в школе", style = MaterialTheme.typography.titleMedium)
                            DiaryInsights.mathAverage(diary)?.let { Text("Средний балл: $it") }
                            val marks = DiaryInsights.mathMarks(diary).take(6)
                            if (marks.isNotEmpty()) Row { marks.forEach { MarkBadge(it.value) } }
                            if (homework != null) {
                                Text("Задано на ${shortDate(homework.date)}: ${homework.text}", fontSize = 15.sp)
                                DiaryInsights.pages(homework.text).firstOrNull()?.let { page ->
                                    PillButton("📸 Сфоткать страницу $page") {
                                        vm.mathCapture.reset(); vm.mathCapture.note = "стр. $page"; vm.go(Screen.MathCapture)
                                    }
                                }
                            }
                        }
                    }
                }
                BigButton("📸", "Новая страница учебника", subtitle = "Сфотографируй — Ника разберёт задачи", brush = MathPalette.primary, appear = 0) {
                    if (vm.mathCapture.mathDraft == null && !vm.mathCapture.busy) vm.mathCapture.reset()
                    vm.go(Screen.MathCapture)
                }
                BigButton(
                    "🔁", "Повторение",
                    subtitle = if (due > 0) "Пора повторить тем: $due" else "Разминка из пройденного",
                    brush = MathPalette.burgundy, appear = 1,
                ) { vm.go(Screen.MathPractice()) }
                BigButton(
                    "📘", "Программа 3–4 класса",
                    subtitle = "Темы по Моро · освоено $learned из ${MathBank.topics.size}",
                    brush = MathPalette.teal, appear = 2,
                ) { vm.go(Screen.MathTopics) }
                BigButton("📚", "Мои уроки", subtitle = "Страниц учебника: ${data.mathLessons.size}", brush = MathPalette.indigo, appear = 3) { vm.go(Screen.MathLessons) }
                BigButton("💬", "Спросить Нику", subtitle = "Как решить пример или задачу", brush = MathPalette.slate, appear = 4) { vm.go(Screen.Tutor(math = true)) }
                BigButton("🏫", "Дневник МЭШ", subtitle = "Расписание, оценки и домашка", brush = MathPalette.primary, appear = 5) { vm.go(Screen.Diary) }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

/* ======================= Программа ======================= */

@Composable
fun MathTopicsScreen(vm: AppViewModel) {
    val g = LocalContext.current.graph
    val data by g.store.data.collectAsState()
    GradientScreen {
        Column(Modifier.fillMaxSize()) {
            TopBar("Программа 3–4 класса", onBack = { vm.back() })
            LazyColumn(
                Modifier.weight(1f).navigationBarsPadding().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                listOf(3, 4).forEach { grade ->
                    item(key = "g$grade") {
                        Text(if (grade == 3) "3 класс — повторяем" else "4 класс", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp))
                    }
                    items(MathBank.topics.filter { it.grade == grade }, key = { it.id }) { t ->
                        val st = data.mathTopics[t.id]
                        Row(
                            Modifier.fillMaxWidth().glass(RoundedCornerShape(18.dp), Palette.Card).clickable { vm.go(Screen.MathTopicPage(t.id)) }.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(t.title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Text(t.examples.first().problem, color = Palette.Muted, fontSize = 14.sp)
                            }
                            Text(
                                when {
                                    st == null -> "🆕"
                                    st.box >= Srs.LEARNED_BOX -> "✅"
                                    st.due <= today() -> "🔁"
                                    else -> "🌱"
                                },
                                fontSize = 22.sp,
                            )
                        }
                    }
                }
                item { Spacer(Modifier.height(12.dp)) }
            }
        }
    }
}

@Composable
fun MathTopicScreen(vm: AppViewModel, id: String) {
    val g = LocalContext.current.graph
    val talking by g.speaker.speaking.collectAsState()
    val topic = MathBank.topic(id) ?: return
    GradientScreen {
        Column(Modifier.fillMaxSize()) {
            TopBar(topic.title, onBack = { vm.back() })
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                NikaSays("", if (talking) NikaMood.TALK else NikaMood.IDLE, talking, nikaHeight = 120.dp)
                CardBox {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("📝 Правило", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            SoundButton({ g.speaker.speak(topic.rule) })
                        }
                        Text(topic.rule, fontSize = 16.sp)
                    }
                }
                CardBox {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("✏️ Примеры", style = MaterialTheme.typography.titleMedium)
                        topic.examples.forEach { e ->
                            Column(Modifier.fillMaxWidth().clickable { g.speaker.speak("${e.problem}. ${e.solution}") }) {
                                Text(e.problem, fontWeight = FontWeight.ExtraBold, fontSize = 19.sp, color = MathPalette.Navy)
                                Text(e.solution, color = Palette.Muted, fontSize = 15.sp)
                            }
                        }
                    }
                }
            }
            Column(Modifier.navigationBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                BigButton("▶️", "Решать", subtitle = "8 новых задач по теме", brush = MathPalette.primary) {
                    vm.go(Screen.MathPractice(topicId = id))
                }
            }
        }
    }
}

/* ======================= Тренировка ======================= */

/** Ход тренировки: очередь задач, вторая попытка, нерешённые задачи повторяются в конце. */
class MathPracticeState(problems: List<MathProblem>) {
    val queue = mutableStateListOf<MathProblem>().apply { addAll(problems) }
    var index by mutableIntStateOf(0)
    var input by mutableStateOf("")
    var picked by mutableStateOf<String?>(null)
    var attempts by mutableIntStateOf(0)
    /** null — ещё решаем, true/false — итог задачи. */
    var result by mutableStateOf<Boolean?>(null)
    var feedback by mutableStateOf("")
    var burst by mutableIntStateOf(0)
    var shakes by mutableIntStateOf(0)
    var finished by mutableStateOf(false)
    var correct = 0
    var total = 0
    /** Решённые задачи: задача → решена ли с первой попытки. */
    val log = mutableListOf<Pair<MathProblem, Boolean>>()
    private val requeued = mutableSetOf<String>()

    val task: MathProblem? get() = queue.getOrNull(index)

    /** Итог задачи. firstTry — решена с первого раза. */
    fun finish(ok: Boolean) {
        val t = task ?: return
        if (result != null) return
        val firstTry = ok && attempts == 0
        result = ok
        total++
        if (firstTry) correct++
        log += t to firstTry
        if (ok) burst++ else shakes++
        // В конце ещё раз — только задачи, которые так и не решились (со второй попытки — уже не повторяем).
        if (!ok && requeued.add(t.key)) queue.add(t)
    }

    fun next() {
        result = null; feedback = ""; input = ""; picked = null; attempts = 0
        if (index + 1 >= queue.size) finished = true else index++
    }

    /** В задаче ошибка: не считаем её и убираем повторы. */
    fun dropBad() {
        val t = task ?: return
        val mine = log.filter { it.first.key == t.key }
        total -= mine.size
        correct -= mine.count { it.second }
        log.removeAll { it.first.key == t.key }
        for (i in queue.indices.reversed()) if (i > index && queue[i].key == t.key) queue.removeAt(i)
        next()
        feedback = "Спасибо! Я убрала эту задачу."
    }

    /** Тема → были ли ошибки. */
    val topicErrors: Map<String, Boolean>
        get() = log.filter { it.first.topic.isNotBlank() }.groupBy({ it.first.topic }, { it.second }).mapValues { (_, oks) -> !oks.all { it } }
}

private val PRAISE_MATH = listOf("Верно!", "Отлично!", "Правильно!", "Умница!", "Так держать!", "Молодец!")

@Composable
fun MathPracticeScreen(vm: AppViewModel, screen: Screen.MathPractice) {
    val g = LocalContext.current.graph
    val scope = rememberCoroutineScope()
    val talking by g.speaker.speaking.collectAsState()
    val lessonKeys = remember(screen) { g.store.value.mathLessons.firstOrNull { it.id == screen.lessonId }?.problems.orEmpty().map { it.key }.toSet() }
    val state = remember(screen) {
        val d = g.store.value
        val problems = when {
            screen.topicId != null -> MathSession.topic(screen.topicId, 8)
            screen.lessonId != null -> {
                val l = d.mathLessons.firstOrNull { it.id == screen.lessonId }
                MathSession.lesson(l?.problems.orEmpty().filter { it.key !in d.badMath }, l?.topics.orEmpty())
            }
            else -> {
                val due = d.dueMathTopics(today())
                // Нечего повторять — разминка: сначала темы, где были ошибки, потом случайные.
                val topics = due.ifEmpty {
                    val weak = d.mathTopics.filter { it.value.wrong > 0 }.keys.toList()
                    (weak + MathBank.topics.map { it.id }.shuffled()).distinct().take(5)
                }
                MathSession.review(topics)
            }
        }
        MathPracticeState(problems)
    }
    var stars by remember(screen) { mutableIntStateOf(0) }

    fun finishAll() = scope.launch {
        val t = today()
        stars = SessionBuilder.stars(state.correct, state.total)
        g.store.update { d ->
            d.copy(
                mathLessons = d.mathLessons.map { l ->
                    if (l.id == screen.lessonId) l.copy(runs = l.runs + 1, bestStars = maxOf(l.bestStars, stars)) else l
                },
                older = d.older.addSession(t, stars),
                mathTopics = d.mathTopics + state.topicErrors.map { (id, hadError) -> id to (d.mathTopics[id] ?: TopicStats()).review(!hadError, t) },
            )
        }
        g.speaker.say(if (stars == 3) "Великолепно! Три звезды!" else "Хорошая тренировка! Ты молодец!")
    }
    LaunchedEffect(state.finished) { if (state.finished) finishAll() }

    GradientScreen {
        Column(Modifier.fillMaxSize()) {
            TopBar(
                when {
                    screen.topicId != null -> MathBank.topic(screen.topicId)?.title ?: "Тренировка"
                    screen.lessonId != null -> "Урок"
                    else -> "Повторение"
                },
                onBack = { vm.back() },
            )
            if (state.queue.isEmpty()) {
                NikaSays("Здесь пока нечего решать.", NikaMood.SAD, false, Modifier.padding(16.dp))
                return@Column
            }
            if (state.finished) {
                MathResults(state, stars, talking, onAgain = { vm.replace(screen.copy(run = System.nanoTime())) }, onHome = { vm.back() })
                return@Column
            }
            GlassProgress(state.index.toFloat() / state.queue.size, Modifier.padding(horizontal = 16.dp))
            val mood = when {
                state.result == true -> NikaMood.HAPPY
                state.result == false -> NikaMood.SAD
                talking -> NikaMood.TALK
                else -> NikaMood.IDLE
            }
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                NikaSays(state.feedback, mood, talking, nikaHeight = 110.dp)
                val t = state.task ?: return@Column
                androidx.compose.runtime.key(state.index) {
                    Column(Modifier.shake(state.shakes), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        ProblemCard(g, t)
                        if (t.kind == MathProblem.CHOOSE) ChooseAnswer(g, state, t) else InputAnswer(g, state, t)
                    }
                }
                if (state.result != null && t.explain.isNotBlank()) {
                    CardBox {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("💡 Как решать", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                                SoundButton({ g.speaker.speak(t.explain) })
                            }
                            Text(t.explain, fontSize = 16.sp)
                        }
                    }
                }
                if (t.key in lessonKeys) {
                    ReportBadProblem {
                        scope.launch { g.store.update { it.withoutMath(t) } }
                        g.speaker.stop()
                        state.dropBad()
                    }
                }
            }
            if (state.result != null) {
                Box(Modifier.navigationBarsPadding().padding(16.dp)) {
                    BigButton(if (state.result == true) "🎉" else "💪", "Дальше", brush = if (state.result == true) MathPalette.teal else MathPalette.primary) { state.next() }
                }
            }
        }
        ConfettiBurst(state.burst, origin = Offset(0.5f, 0.42f), count = 18, stars = true, durationMs = 1100)
    }
}

/** Условие задачи крупно; примеры — очень крупно, текстовые задачи — обычным шрифтом. */
@Composable
private fun ProblemCard(g: AppGraph, t: MathProblem) {
    LaunchedEffect(t.key) { g.speaker.say(t.prompt, kind = Talk.TASK) }
    val short = t.prompt.length <= 28
    CardBox {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                t.prompt,
                fontSize = if (short) 32.sp else 20.sp,
                fontWeight = if (short) FontWeight.Black else FontWeight.Bold,
                color = MathPalette.Navy,
                textAlign = if (short) TextAlign.Center else TextAlign.Start,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            SoundButton({ g.speaker.speak(t.prompt) })
        }
    }
}

/** Ответ цифрами: экранная клавиатура, вторая попытка, после ошибки — правильный ответ и решение. */
@Composable
private fun InputAnswer(g: AppGraph, state: MathPracticeState, t: MathProblem) {
    val scope = rememberCoroutineScope()
    val extra = remember(t.key) { MathCheck.extraKeys(t.answer) }

    fun check() {
        if (state.result != null || state.input.isBlank()) return
        if (MathCheck.same(state.input, t.answer)) {
            state.finish(true)
            val p = PRAISE_MATH.random()
            state.feedback = p
            scope.launch { g.speaker.say(p) }
        } else if (state.attempts == 0) {
            state.attempts = 1
            state.shakes++
            state.input = ""
            state.feedback = "Не совсем. Проверь ещё раз!"
            scope.launch { g.speaker.say("Не совсем. Проверь ещё раз!") }
        } else {
            state.finish(false)
            val right = t.answer + if (t.unit.isNotBlank()) " ${t.unit}" else ""
            state.feedback = "Правильный ответ: $right"
            scope.launch {
                g.speaker.say("Правильный ответ: $right.")
                if (t.explain.isNotBlank()) g.speaker.say(t.explain)
            }
        }
    }

    // Поле ответа.
    val shown = state.input.ifEmpty { if (state.result == null) "?" else "" }
    val color = when (state.result) {
        true -> Palette.Good
        false -> Palette.Bad
        null -> MathPalette.Navy
    }
    Row(
        Modifier.fillMaxWidth().glass(RoundedCornerShape(20.dp), Color.White.copy(alpha = 0.85f), rim = 2.dp).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Text("Ответ: ", fontSize = 20.sp, color = Palette.Muted, fontWeight = FontWeight.Bold)
        Text(shown, fontSize = 34.sp, fontWeight = FontWeight.Black, color = if (state.input.isEmpty()) Palette.Muted.copy(alpha = 0.5f) else color)
        if (t.unit.isNotBlank()) Text(" ${t.unit}", fontSize = 20.sp, color = Palette.Muted, fontWeight = FontWeight.Bold)
    }
    if (state.result == false) {
        Text(
            "Верно: ${t.answer}${if (t.unit.isNotBlank()) " ${t.unit}" else ""}",
            fontSize = 22.sp, fontWeight = FontWeight.Black, color = Palette.Good,
            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
        )
    }
    if (state.result == null) {
        Keypad(
            extra = extra,
            onKey = { k ->
                state.input = when (k) {
                    "ост." -> if ("ост" in state.input || state.input.isBlank()) state.input else state.input.trimEnd() + " ост. "
                    else -> (state.input + k).take(14)
                }
            },
            onBackspace = { state.input = state.input.trimEnd().removeSuffix("ост.").trimEnd().let { if (it == state.input.trimEnd()) it.dropLast(1) else it } },
            onOk = { check() },
        )
    }
}

/** Цифровая клавиатура: 1–9, 0, стереть, проверить и особые кнопки (например, «ост.»). */
@Composable
private fun Keypad(extra: List<String>, onKey: (String) -> Unit, onBackspace: () -> Unit, onOk: () -> Unit) {
    val rows = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("⌫", "0", "✓"))
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (extra.isNotEmpty()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                extra.forEach { k -> KeyButton(k, MathPalette.slate, Modifier.weight(1f)) { onKey(k) } }
            }
        }
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { k ->
                    when (k) {
                        "⌫" -> KeyButton(k, MathPalette.slate, Modifier.weight(1f), onBackspace)
                        "✓" -> KeyButton(k, MathPalette.teal, Modifier.weight(1f), onOk)
                        else -> KeyButton(k, GlassWhite, Modifier.weight(1f)) { onKey(k) }
                    }
                }
            }
        }
    }
}

@Composable
private fun KeyButton(label: String, brush: Brush, modifier: Modifier, onClick: () -> Unit) {
    val light = brush === GlassWhite
    GlassButton(
        onClick, modifier, brush = brush, shape = RoundedCornerShape(18.dp), depth = 4.dp, fillWidth = true,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp),
    ) {
        Text(
            label, fontSize = 28.sp, fontWeight = FontWeight.Black, color = if (light) MathPalette.Navy else Color.White,
            textAlign = TextAlign.Center, modifier = Modifier.weight(1f),
            style = if (light) TextStyle.Default else TextStyle(shadow = GlassTextShadow),
        )
    }
}

/** Выбор ответа: знаки сравнения — тремя большими кнопками в ряд, остальное — списком. */
@Composable
private fun ChooseAnswer(g: AppGraph, state: MathPracticeState, t: MathProblem) {
    val scope = rememberCoroutineScope()
    fun pick(o: String) {
        if (state.result != null) return
        state.picked = o
        val ok = MathCheck.same(o, t.answer)
        state.finish(ok)
        val p = PRAISE_MATH.random()
        state.feedback = if (ok) p else "Правильно: ${t.answer}"
        scope.launch {
            if (ok) g.speaker.say(p) else {
                g.speaker.say("Правильно так: ${t.answer}.")
                if (t.explain.isNotBlank()) g.speaker.say(t.explain)
            }
        }
    }
    fun stateOf(o: String) = when {
        state.picked == null -> ChoiceState.NORMAL
        MathCheck.same(o, t.answer) -> ChoiceState.RIGHT
        o == state.picked -> ChoiceState.WRONG
        else -> ChoiceState.DIM
    }
    if (t.options.all { it in MathProblem.SIGNS }) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            t.options.forEach { o ->
                val st = stateOf(o)
                val brush = when (st) {
                    ChoiceState.RIGHT -> GlassGood
                    ChoiceState.WRONG -> GlassBad
                    ChoiceState.DIM -> GlassDim
                    ChoiceState.NORMAL -> GlassWhite
                }
                GlassButton(
                    { pick(o) }, Modifier.weight(1f), brush = brush, shape = RoundedCornerShape(22.dp), depth = 5.dp, fillWidth = true,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 14.dp),
                ) {
                    Text(o, fontSize = 40.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center, modifier = Modifier.weight(1f),
                        color = if (st == ChoiceState.RIGHT || st == ChoiceState.WRONG) Color.White else MathPalette.Navy)
                }
            }
        }
    } else {
        t.options.forEach { o -> ChoiceButton(o, stateOf(o)) { pick(o) } }
    }
}

/** «В задаче ошибка» — для задач, которые придумала нейросеть. */
@Composable
private fun ReportBadProblem(onConfirm: () -> Unit) {
    var ask by remember { mutableStateOf(false) }
    TextButton(onClick = { ask = true }, modifier = Modifier.fillMaxWidth()) {
        Text("🚩 В задаче ошибка", color = Palette.Muted, fontSize = 15.sp)
    }
    if (ask) {
        AlertDialog(
            onDismissRequest = { ask = false },
            title = { Text("Задача с ошибкой?") },
            text = { Text("Если задачу нельзя решить правильно, Ника уберёт её и больше не покажет. Она не засчитается как ошибка.") },
            confirmButton = { TextButton(onClick = { ask = false; onConfirm() }) { Text("Да, убрать") } },
            dismissButton = { TextButton(onClick = { ask = false }) { Text("Нет, решу") } },
        )
    }
}

@Composable
private fun MathResults(state: MathPracticeState, stars: Int, talking: Boolean, onAgain: () -> Unit, onHome: () -> Unit) {
    var party by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { party = 1; delay(1300); party = 2 }
    val hard = state.log.filter { !it.second }.map { it.first.topic }.distinct().mapNotNull { MathBank.topic(it)?.title }
    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(if (stars == 3) "Великолепно!" else if (stars == 2) "Отлично!" else "Молодец, что старалась!", style = MaterialTheme.typography.headlineMedium)
            Nika(NikaMood.DANCE, Modifier.size(width = 150.dp, height = 240.dp), talking = talking)
            Stars(stars)
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIcon(AppIcons.TROPHY, size = 34.dp)
                Text("  С первой попытки: ${state.correct} из ${state.total}", style = MaterialTheme.typography.titleLarge)
            }
            if (hard.isNotEmpty()) {
                CardBox {
                    Column {
                        Text("Эти темы Ника повторит с тобой завтра:", fontWeight = FontWeight.SemiBold)
                        hard.forEach { Text("• $it", fontSize = 17.sp) }
                    }
                }
            }
            BigButton("🔁", "Ещё раз", brush = MathPalette.teal, appear = 2, onClick = onAgain)
            BigButton("🏠", "Готово", brush = MathPalette.primary, appear = 3, onClick = onHome)
        }
        ConfettiBurst(party, origin = Offset(0.5f, 0.3f), count = 80)
    }
}

/* ======================= Урок по фото ======================= */

@Composable
fun MathCaptureScreen(vm: AppViewModel) {
    val ctx = LocalContext.current
    val g = ctx.graph
    val c = vm.mathCapture
    val scope = rememberCoroutineScope()
    val talking by g.speaker.speaking.collectAsState()
    var pendingUri by remember { mutableStateOf<Uri?>(null) }

    fun addPhoto(uri: Uri) = scope.launch {
        runCatching { g.pages.importPhoto(uri) }
            .onSuccess { c.photos.add(it) }
            .onFailure { c.error = "Не получилось открыть фото: ${it.message}" }
    }
    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val uri = pendingUri
        if (ok && uri != null) addPhoto(uri)
    }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> if (uri != null) addPhoto(uri) }

    LaunchedEffect(c.mathDraft) { if (c.mathDraft != null) vm.replace(Screen.MathDraft) }
    LaunchedEffect(Unit) { if (c.photos.isEmpty()) g.speaker.say("Сфотографируй страницу учебника математики. Можно несколько страниц.") }

    GradientScreen {
        Column(Modifier.fillMaxSize()) {
            TopBar("Новая страница", onBack = { vm.back() })
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                val mood = when {
                    c.busy -> NikaMood.THINK
                    c.error != null -> NikaMood.SAD
                    talking -> NikaMood.TALK
                    else -> NikaMood.IDLE
                }
                NikaSays(c.status ?: c.error ?: "Сфотографируй страницу учебника математики. Можно несколько страниц.", mood, talking, nikaHeight = 150.dp)
                if (c.photos.isNotEmpty()) {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        c.photos.forEach { f -> PhotoThumb(f.path) { if (!c.busy) { c.photos.remove(f); f.delete() } } }
                    }
                    Hint("Нажми на фото, чтобы убрать его.")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PillButton("📷 Камера", Modifier.weight(1f), brush = MathPalette.primary, enabled = !c.busy) {
                        val dir = File(ctx.cacheDir, "camera").apply { mkdirs() }
                        val file = File(dir, "page-${System.currentTimeMillis()}.jpg")
                        val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".files", file)
                        pendingUri = uri
                        takePicture.launch(uri)
                    }
                    PillButton("🖼 Галерея", Modifier.weight(1f), brush = MathPalette.teal, enabled = !c.busy) {
                        pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                }
                OutlinedTextField(
                    c.note, { c.note = it }, label = { Text("Страница или тема (необязательно)") },
                    placeholder = { Text("например: стр. 34, деление") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    enabled = !c.busy,
                )
                if (c.busy) {
                    LinearProgressIndicator(Modifier.fillMaxWidth(), color = MathPalette.Teal)
                } else {
                    BigButton("✨", "Сделать урок", enabled = c.photos.isNotEmpty(), subtitle = "Ника разберёт правило и придумает задачи", brush = MathPalette.primary) {
                        vm.generateMathLesson()
                    }
                }
                Hint("Фотографируй ровно и при хорошем свете — так цифры и знаки распознаются точнее.")
            }
        }
    }
}

/** Проверка урока перед сохранением: можно убрать лишние задачи. */
@Composable
fun MathDraftScreen(vm: AppViewModel) {
    val g = LocalContext.current.graph
    val c = vm.mathCapture
    val draft = c.mathDraft
    val scope = rememberCoroutineScope()
    if (draft == null) {
        LaunchedEffect(Unit) { vm.replace(Screen.MathCapture) }
        return
    }
    var title by remember { mutableStateOf(draft.title) }
    var problems by remember { mutableStateOf(draft.validProblems()) }
    val examples = remember { draft.validExamples() }

    GradientScreen {
        Column(Modifier.fillMaxSize()) {
            TopBar("Проверь урок", onBack = { c.mathDraft = null; vm.replace(Screen.MathCapture) })
            LazyColumn(Modifier.weight(1f).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(title, { title = it }, label = { Text("Название") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        if (draft.rule.isNotBlank()) CardBox { Text("📝 ${draft.rule}", fontSize = 15.sp) }
                        if (examples.isNotEmpty()) CardBox {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("✏️ Примеры со страницы", fontWeight = FontWeight.Bold)
                                examples.forEach { Text("${it.problem} → ${it.solution}", fontSize = 15.sp) }
                            }
                        }
                        draft.topics.mapNotNull { MathBank.topic(it) }.takeIf { it.isNotEmpty() }?.let { ts ->
                            Text("🔁 Повторим: " + ts.joinToString(", ") { it.title }, color = MathPalette.Navy, fontSize = 15.sp)
                        }
                        Hint("Задачи, где нейросеть ошиблась в счёте, Ника уже убрала сама. ✕ — убрать задачу.")
                    }
                }
                items(problems, key = { it.key }) { p ->
                    Row(
                        Modifier.fillMaxWidth().glass(RoundedCornerShape(14.dp), Palette.Card).padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(p.prompt, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = MathPalette.Navy)
                            Text("Ответ: ${p.answer}${if (p.unit.isNotBlank()) " ${p.unit}" else ""}", color = Palette.Muted)
                        }
                        Text("✕", fontSize = 22.sp, color = Palette.Bad, modifier = Modifier.clickable { problems = problems - p }.padding(6.dp))
                    }
                }
            }
            Box(Modifier.navigationBarsPadding().padding(16.dp)) {
                BigButton("💾", "Сохранить и решать", subtitle = "Задач: ${problems.size}", brush = MathPalette.primary, enabled = problems.isNotEmpty() || draft.rule.isNotBlank()) {
                    scope.launch {
                        val id = UUID.randomUUID().toString()
                        val lesson = MathLesson(
                            id = id,
                            title = title.ifBlank { "Новый урок" },
                            createdAt = System.currentTimeMillis(),
                            note = c.note,
                            rule = draft.rule,
                            examples = examples,
                            problems = problems,
                            topics = draft.topics,
                            sourceText = c.sourceText,
                            photos = c.photos.map { it.path },
                        )
                        g.store.update { d -> d.copy(mathLessons = listOf(lesson) + d.mathLessons) }
                        c.photos.clear(); c.reset()
                        vm.replace(Screen.MathLessonPage(id))
                    }
                }
            }
        }
    }
}

@Composable
fun MathLessonsScreen(vm: AppViewModel) {
    val g = LocalContext.current.graph
    val data by g.store.data.collectAsState()
    GradientScreen {
        Column(Modifier.fillMaxSize()) {
            TopBar("Мои уроки", onBack = { vm.back() })
            if (data.mathLessons.isEmpty()) {
                NikaSays("Пока уроков нет. Сфотографируй страницу учебника — я разберу её с тобой!", NikaMood.IDLE, false, Modifier.padding(16.dp))
            }
            LazyColumn(Modifier.weight(1f).navigationBarsPadding().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(data.mathLessons, key = { it.id }) { l ->
                    Row(
                        Modifier.fillMaxWidth().glass(RoundedCornerShape(18.dp), Palette.Card).clickable { vm.go(Screen.MathLessonPage(l.id)) }.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (l.photos.isNotEmpty()) PhotoThumb(l.photos.first(), 56) else AppIcon(AppIcons.PENCIL, size = 52.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(l.title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text("${l.problems.size} задач · ${l.note}".trimEnd(' ', '·'), color = Palette.Muted, fontSize = 14.sp)
                        }
                        Text("⭐".repeat(l.bestStars).ifEmpty { "▶️" }, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun MathLessonScreen(vm: AppViewModel, id: String) {
    val g = LocalContext.current.graph
    val data by g.store.data.collectAsState()
    val scope = rememberCoroutineScope()
    val lesson = data.mathLessons.firstOrNull { it.id == id }
    if (lesson == null) {
        LaunchedEffect(Unit) { vm.back() }
        return
    }
    var confirmDelete by remember { mutableStateOf(false) }

    GradientScreen {
        Column(Modifier.fillMaxSize()) {
            TopBar(lesson.title, onBack = { vm.back() }) {
                Text("🗑", fontSize = 22.sp, modifier = Modifier.clickable { confirmDelete = true }.padding(10.dp))
            }
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (lesson.photos.isNotEmpty()) {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        lesson.photos.forEach { p -> PhotoThumb(p, 80) }
                    }
                }
                if (lesson.rule.isNotBlank()) CardBox {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("📝 Правило", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            SoundButton({ g.speaker.speak(lesson.rule) })
                        }
                        Text(lesson.rule, fontSize = 16.sp)
                    }
                }
                if (lesson.examples.isNotEmpty()) CardBox {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("✏️ Разобранные примеры", style = MaterialTheme.typography.titleMedium)
                        lesson.examples.forEach { e ->
                            Column(Modifier.fillMaxWidth().clickable { g.speaker.speak("${e.problem}. ${e.solution}") }) {
                                Text(e.problem, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = MathPalette.Navy)
                                Text(e.solution, color = Palette.Muted, fontSize = 15.sp)
                            }
                        }
                    }
                }
                CardBox {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("🧮 Задач в уроке: ${lesson.problems.size}", fontWeight = FontWeight.SemiBold)
                        lesson.topics.mapNotNull { MathBank.topic(it) }.forEach { tp ->
                            Text("🔁 Повторим: ${tp.title}", fontSize = 15.sp, color = MathPalette.Navy, modifier = Modifier.clickable { vm.go(Screen.MathTopicPage(tp.id)) })
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
            Column(Modifier.navigationBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                BigButton("▶️", "Решать", subtitle = "Задачи со страницы и повторение", brush = MathPalette.primary, enabled = lesson.problems.isNotEmpty() || lesson.topics.isNotEmpty()) {
                    vm.go(Screen.MathPractice(lessonId = id))
                }
                BigButton("💬", "Спросить Нику про урок", brush = MathPalette.slate) { vm.go(Screen.Tutor(id, math = true)) }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Удалить урок?") },
            text = { Text("Фото и задачи этого урока удалятся.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    scope.launch {
                        lesson.photos.forEach { File(it).delete() }
                        g.store.update { d -> d.copy(mathLessons = d.mathLessons.filter { it.id != id }) }
                        vm.back()
                    }
                }) { Text("Удалить", color = Palette.Bad) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Отмена") } },
        )
    }
}
