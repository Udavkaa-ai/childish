package ru.sovushka.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.sovushka.app.AppGraph
import ru.sovushka.app.graph
import ru.sovushka.app.today
import ru.sovushka.app.voice.ListenException
import ru.sovushka.core.Drill
import ru.sovushka.core.GrammarBank
import ru.sovushka.core.SessionBuilder
import ru.sovushka.core.SpeechText
import ru.sovushka.core.Srs
import ru.sovushka.core.Task
import ru.sovushka.core.TextMatch
import ru.sovushka.core.TopicStats
import ru.sovushka.core.Word

/** Ход тренировки: очередь заданий, ошибки, счёт. Ошибочное задание один раз повторяется в конце. */
class PracticeState(tasks: List<Task>) {
    val queue = mutableStateListOf<Task>().apply { addAll(tasks) }
    var index by mutableIntStateOf(0)
    var result by mutableStateOf<Boolean?>(null)
    var feedback by mutableStateOf("")
    /** Счётчики для эффектов: вспышка звёзд на верный ответ, встряхивание на ошибку. */
    var burst by mutableIntStateOf(0)
    var shakes by mutableIntStateOf(0)
    var finished by mutableStateOf(false)
    val wrongs = mutableMapOf<String, Int>()
    /** Ответы на грамматические задания: упражнение → верно ли. */
    private val grammarLog = mutableListOf<Pair<Drill, Boolean>>()
    /** Тема → были ли ошибки (false — всё верно). */
    val topicErrors: Map<String, Boolean>
        get() = grammarLog.filter { it.first.topic.isNotBlank() }
            .groupBy({ it.first.topic }, { it.second }).mapValues { (_, oks) -> !oks.all { it } }
    private val requeued = mutableSetOf<Task>()
    var correct = 0
    var total = 0

    val task: Task? get() = queue.getOrNull(index)

    fun answer(ok: Boolean) {
        val t = task ?: return
        if (result != null) return
        result = ok
        total++
        if (t is Task.Grammar) grammarLog.add(t.drill to ok)
        if (ok) burst++ else shakes++
        if (ok) correct++ else {
            t.word?.let { wrongs[it.id] = (wrongs[it.id] ?: 0) + 1 }
            if (requeued.add(t)) queue.add(t)
        }
    }

    fun next() {
        result = null
        feedback = ""
        if (index + 1 >= queue.size) finished = true else index++
    }

    /** Ребёнок нашёл ошибку в задании: не считаем его ни верным, ни ошибкой и убираем повторы из очереди. */
    fun dropBad() {
        val t = task as? Task.Grammar ?: return
        val key = t.drill.key
        val mine = grammarLog.filter { it.first.key == key }
        total -= mine.size
        correct -= mine.count { it.second }
        grammarLog.removeAll { it.first.key == key }
        for (i in queue.indices.reversed()) {
            if (i > index && (queue[i] as? Task.Grammar)?.drill?.key == key) queue.removeAt(i)
        }
        next()
        feedback = "Спасибо! Я убрала это задание."
    }
}

private val PRAISE = listOf("Great!", "Молодец!", "Excellent!", "Супер!", "Well done!", "Умница!")

@Composable
fun PracticeScreen(vm: AppViewModel, screen: Screen.Practice) {
    val g = LocalContext.current.graph
    val scope = rememberCoroutineScope()
    val talking by g.speaker.speaking.collectAsState()
    val listening by g.listener.listening.collectAsState()
    val state = remember(screen) {
        val d = g.store.value
        val tasks = when {
            screen.topicId != null && screen.fresh > 0 ->
                SessionBuilder.grammar(d.extraDrills[screen.topicId].orEmpty().takeLast(screen.fresh), screen.fresh)
            screen.topicId != null -> SessionBuilder.grammar(d.drillsOf(screen.topicId), 8)
            screen.lessonId != null -> {
                val lesson = d.lessons.firstOrNull { it.id == screen.lessonId }
                SessionBuilder.lesson(
                    d.wordsOf(screen.lessonId), lesson?.sentences.orEmpty(), d.words, g.listener.available(),
                    drills = lesson?.drills.orEmpty(),
                    reviewDrills = lesson?.topics.orEmpty().flatMap { d.drillsOf(it) },
                )
            }
            else -> SessionBuilder.review(
                d.dueWords(today()), d.words, g.listener.available(),
                drills = d.dueTopics(today()).flatMap { d.drillsOf(it) },
            )
        }
        PracticeState(tasks)
    }
    var stars by remember(screen) { mutableIntStateOf(0) }

    fun finish() = scope.launch {
        val t = today()
        stars = SessionBuilder.stars(state.correct, state.total)
        val ids = state.queue.mapNotNull { it.word?.id }.toSet()
        g.store.update { d ->
            d.copy(
                words = d.words.map { w -> if (w.id in ids) Srs.review(w, (state.wrongs[w.id] ?: 0) == 0, t) else w },
                lessons = d.lessons.map { l ->
                    if (l.id == screen.lessonId) l.copy(runs = l.runs + 1, bestStars = maxOf(l.bestStars, stars)) else l
                },
                older = d.older.addSession(t, stars),
                grammar = d.grammar + state.topicErrors.map { (id, hadError) ->
                    id to (d.grammar[id] ?: TopicStats()).review(!hadError, t)
                },
            )
        }
        g.speaker.say(if (stars == 3) "Ура! Три звезды! You are a star!" else "Отличная тренировка! Молодец!")
    }

    LaunchedEffect(state.finished) { if (state.finished) finish() }

    GradientScreen {
        Column(Modifier.fillMaxSize()) {
            TopBar(
                when {
                    screen.topicId != null -> GrammarBank.topic(screen.topicId)?.title ?: "Правило"
                    screen.lessonId == null -> "Повторение"
                    else -> "Тренировка"
                },
                onBack = { vm.back() },
            )
            if (state.queue.isEmpty()) {
                NikaSays("Здесь пока нечего тренировать.", NikaMood.SAD, false, Modifier.padding(16.dp))
                return@Column
            }
            if (state.finished) {
                Results(state, stars, talking, onAgain = { vm.replace(Screen.Practice(screen.lessonId, topicId = screen.topicId)) }, onHome = { vm.back() })
                return@Column
            }
            GlassProgress(state.index.toFloat() / state.queue.size, Modifier.padding(horizontal = 16.dp))
            val mood = when {
                state.result == true -> NikaMood.HAPPY
                state.result == false -> NikaMood.SAD
                listening -> NikaMood.LISTEN
                talking -> NikaMood.TALK
                else -> NikaMood.IDLE
            }
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                NikaSays(state.feedback, mood, talking, nikaHeight = 120.dp)
                Column(Modifier.shake(state.shakes), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                key(state.index) {
                    when (val t = state.task) {
                        is Task.Intro -> IntroTask(g, t.word) { state.next() }
                        is Task.ListenPick -> PickTask(g, state, t.word, t.options, prompt = "Послушай и выбери перевод", listenFirst = true, correct = t.word.ru)
                        is Task.ReadPick -> PickTask(g, state, t.word, t.options, prompt = "Как это по-английски?", listenFirst = false, correct = t.word.en)
                        is Task.Spell -> SpellTask(g, state, t.word, t.letters)
                        is Task.Say -> SayTask(g, state, t.word)
                        is Task.Build -> BuildTask(g, state, t)
                        is Task.Grammar -> GrammarTask(g, state, t)
                        null -> {}
                    }
                }
                }
            }
            if (state.result != null) {
                Box(Modifier.navigationBarsPadding().padding(16.dp)) {
                    BigButton(if (state.result == true) "🎉" else "💪", "Дальше", brush = if (state.result == true) Palette.button else Palette.bluish) { state.next() }
                }
            }
        }
        // Верный ответ — вспышка звёздочек.
        ConfettiBurst(state.burst, origin = Offset(0.5f, 0.42f), count = 18, stars = true, durationMs = 1100)
    }
}

/** Общая реакция на ответ: Ника хвалит или спокойно подсказывает правильный вариант. */
private fun react(g: AppGraph, state: PracticeState, ok: Boolean, w: Word?, scope: kotlinx.coroutines.CoroutineScope, correctText: String? = null) {
    state.answer(ok)
    scope.launch {
        if (ok) {
            val p = PRAISE.random()
            state.feedback = p
            if (w != null) g.speaker.say("${w.en}!", SpeechText.Lang.EN)
            g.speaker.say(p)
        } else {
            val right = correctText ?: w?.let { "${it.en} — ${it.ru}" }.orEmpty()
            state.feedback = "Почти! Правильно: $right"
            g.speaker.say("Почти! Правильно: $right")
        }
    }
}

@Composable
private fun WordPicture(w: Word, big: Boolean = true) {
    if (w.emoji.isNotBlank()) {
        Box(Modifier.fillMaxWidth().padding(vertical = 4.dp), contentAlignment = Alignment.Center) {
            Sticker(w.emoji, if (big) 120.dp else 84.dp, seed = w.en.hashCode())
        }
    }
}

@Composable
private fun IntroTask(g: AppGraph, w: Word, onNext: () -> Unit) {
    LaunchedEffect(w.id) {
        g.speaker.say(w.en, SpeechText.Lang.EN)
        delay(300)
        g.speaker.say(w.ru, SpeechText.Lang.RU)
    }
    CardBox {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Новое слово", color = Palette.Muted)
            WordPicture(w)
            Text(w.en, fontSize = 40.sp, fontWeight = FontWeight.ExtraBold, color = Palette.Pink)
            if (w.tr.isNotBlank()) Text(w.tr, fontSize = 20.sp, color = Palette.Muted)
            Text(w.ru, fontSize = 24.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                SoundButton({ g.speaker.speak(w.en, SpeechText.Lang.EN) })
                SoundButton({ g.speaker.speak(w.en, SpeechText.Lang.EN, slow = true) }, slow = true)
            }
            if (w.example.isNotBlank()) {
                Text(
                    "🔊 ${w.example}", fontSize = 17.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { g.speaker.speak(w.example, SpeechText.Lang.EN) }.padding(top = 6.dp),
                    textAlign = TextAlign.Center,
                )
                Text(w.exampleRu, color = Palette.Muted, fontSize = 15.sp, textAlign = TextAlign.Center)
            }
        }
    }
    BigButton("👍", "Запомнила!", brush = Palette.pinkish, onClick = onNext)
}

@Composable
private fun PickTask(g: AppGraph, state: PracticeState, w: Word, options: List<String>, prompt: String, listenFirst: Boolean, correct: String) {
    val scope = rememberCoroutineScope()
    var picked by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(w.id) {
        if (listenFirst) g.speaker.say(w.en, SpeechText.Lang.EN) else g.speaker.say(w.ru, SpeechText.Lang.RU)
    }
    Text(prompt, style = MaterialTheme.typography.titleMedium)
    if (listenFirst) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            SoundButton({ g.speaker.speak(w.en, SpeechText.Lang.EN) })
            Spacer(Modifier.width(16.dp))
            SoundButton({ g.speaker.speak(w.en, SpeechText.Lang.EN, slow = true) }, slow = true)
        }
    } else {
        WordPicture(w, big = false)
        Text(w.ru, fontSize = 32.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
    }
    options.forEach { o ->
        val st = when {
            picked == null -> ChoiceState.NORMAL
            o == correct -> ChoiceState.RIGHT
            o == picked -> ChoiceState.WRONG
            else -> ChoiceState.DIM
        }
        ChoiceButton(o, st) {
            picked = o
            react(g, state, o == correct, w, scope)
        }
    }
}

/** Правописание: собрать слово из букв. Неверная буква не ставится, а подсвечивается. */
@Composable
private fun SpellTask(g: AppGraph, state: PracticeState, w: Word, letters: List<Char>) {
    val scope = rememberCoroutineScope()
    val target = remember(w.id) { SessionBuilder.spellTarget(w.en) }
    var typed by remember { mutableIntStateOf(0) }
    var mistakes by remember { mutableIntStateOf(0) }
    var wrongTile by remember { mutableStateOf(-1) }
    val used = remember { mutableStateListOf<Int>() }
    LaunchedEffect(w.id) { g.speaker.say(w.en, SpeechText.Lang.EN) }
    LaunchedEffect(wrongTile) { if (wrongTile >= 0) { delay(450); wrongTile = -1 } }

    fun place(i: Int) {
        if (state.result != null || i in used) return
        if (letters[i] == target[typed]) {
            used.add(i); typed++
            if (typed == target.length) react(g, state, mistakes <= 1, w, scope)
        } else {
            mistakes++; wrongTile = i
        }
    }

    Text("Собери слово по буквам", style = MaterialTheme.typography.titleMedium)
    Row(verticalAlignment = Alignment.CenterVertically) {
        WordPicture(w, big = false)
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        Text(w.ru, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(12.dp))
        SoundButton({ g.speaker.speak(w.en, SpeechText.Lang.EN, slow = true) }, slow = true)
    }
    Text(
        target.mapIndexed { i, c -> if (i < typed) c else '_' }.joinToString(" "),
        fontSize = 34.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = Palette.Pink,
        modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
    )
    if (state.result != null && w.en.lowercase() != target) {
        Text(w.en, fontSize = 22.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
    }
    letters.indices.chunked(6).forEach { row ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            row.forEach { i ->
                val isUsed = i in used
                Box(
                    Modifier.padding(4.dp).size(50.dp).clip(RoundedCornerShape(12.dp))
                        .background(
                            when {
                                isUsed -> Color(0xFFE9E4F2)
                                wrongTile == i -> Palette.Bad
                                else -> Color.White
                            },
                        )
                        .border(2.dp, if (isUsed) Color.Transparent else Palette.BlueLight, RoundedCornerShape(12.dp))
                        .clickable(enabled = !isUsed && state.result == null) { place(i) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (isUsed) "" else letters[i].toString(), fontSize = 26.sp, fontWeight = FontWeight.Bold,
                        color = if (wrongTile == i) Color.White else Palette.Ink,
                    )
                }
            }
        }
    }
    if (state.result == null) {
        TextButton(onClick = {
            val i = letters.indices.firstOrNull { it !in used && letters[it] == target[typed] } ?: return@TextButton
            mistakes++
            place(i)
        }) { Text("💡 Подсказать букву") }
    }
}

/** Произношение: сказать слово в микрофон. Три попытки, можно пропустить. */
@Composable
private fun SayTask(g: AppGraph, state: PracticeState, w: Word) {
    val scope = rememberCoroutineScope()
    val mic = LocalMic.current
    val listening by g.listener.listening.collectAsState()
    val level by g.listener.level.collectAsState()
    var attempts by remember { mutableIntStateOf(0) }
    var heardText by remember { mutableStateOf("") }
    LaunchedEffect(w.id) { g.speaker.say("Скажи по-английски: ${w.en}") }

    Text("Скажи слово вслух", style = MaterialTheme.typography.titleMedium)
    WordPicture(w)
    Text(w.en, fontSize = 38.sp, fontWeight = FontWeight.ExtraBold, color = Palette.Pink, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
    if (w.tr.isNotBlank()) Text(w.tr, fontSize = 20.sp, color = Palette.Muted, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
        SoundButton({ g.speaker.speak(w.en, SpeechText.Lang.EN) })
        if (state.result == null) {
            MicButton(listening, level) {
                if (listening) return@MicButton
                scope.launch {
                    if (!mic.ensureMic()) { state.feedback = "Разреши микрофон, чтобы я тебя слышала"; return@launch }
                    g.speaker.stop()
                    try {
                        val heard = g.listener.listen("en-US")
                        attempts++
                        heardText = heard.firstOrNull().orEmpty()
                        when {
                            TextMatch.spokenMatch(w.en, heard) -> react(g, state, true, w, scope)
                            attempts >= 3 -> react(g, state, false, w, scope, correctText = w.en)
                            else -> {
                                state.feedback = if (heardText.isBlank()) "Не расслышала. Скажи ещё раз!" else "Я услышала «$heardText». Попробуй ещё!"
                                g.speaker.say(w.en, SpeechText.Lang.EN, slow = true)
                            }
                        }
                    } catch (e: ListenException) {
                        state.feedback = e.message.orEmpty()
                    }
                }
            }
        }
        SoundButton({ g.speaker.speak(w.en, SpeechText.Lang.EN, slow = true) }, slow = true)
    }
    if (state.result == null) {
        TextButton(onClick = { state.next() }, modifier = Modifier.fillMaxWidth()) { Text("Пропустить (не могу сейчас говорить)") }
    }
}

/** Собрать предложение из слов на слух. */
@Composable
private fun BuildTask(g: AppGraph, state: PracticeState, t: Task.Build) {
    val scope = rememberCoroutineScope()
    val chosen = remember { mutableStateListOf<Int>() }
    LaunchedEffect(t) { g.speaker.say(t.sentence.en, SpeechText.Lang.EN) }

    Text("Собери предложение", style = MaterialTheme.typography.titleMedium)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        SoundButton({ g.speaker.speak(t.sentence.en, SpeechText.Lang.EN) })
        Spacer(Modifier.width(16.dp))
        SoundButton({ g.speaker.speak(t.sentence.en, SpeechText.Lang.EN, slow = true) }, slow = true)
    }
    Text(t.sentence.ru, fontSize = 20.sp, color = Palette.Muted, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
    CardBox(Modifier.heightIn(min = 110.dp)) {
        TokenRows(chosen.map { t.tokens[it] }) { idx -> if (state.result == null) chosen.removeAt(idx) }
    }
    TokenRows(t.tokens.mapIndexed { i, s -> if (i in chosen) "" else s }) { i ->
        if (state.result != null || i in chosen) return@TokenRows
        chosen.add(i)
        if (chosen.size == t.tokens.size) {
            val built = chosen.joinToString(" ") { t.tokens[it] }
            val ok = TextMatch.normalize(built) == TextMatch.normalize(SessionBuilder.tokenize(t.sentence.en).joinToString(" "))
            react(g, state, ok, null, scope, correctText = t.sentence.en)
            if (ok) scope.launch { g.speaker.say(t.sentence.en, SpeechText.Lang.EN) }
        }
    }
}

/** Грамматика: вставить слово, выбрать правильное предложение или собрать предложение по-русски. */
@Composable
private fun GrammarTask(g: AppGraph, state: PracticeState, t: Task.Grammar) {
    val scope = rememberCoroutineScope()
    val d = t.drill
    var picked by remember { mutableStateOf<String?>(null) }
    val chosen = remember { mutableStateListOf<Int>() }

    fun done(ok: Boolean) {
        state.answer(ok)
        scope.launch {
            val p = PRAISE.random()
            state.feedback = if (ok) p else "Почти! Правильно: ${d.fullSentence}"
            if (!ok) g.speaker.say("Почти! Правильно:")
            g.speaker.say(d.fullSentence, SpeechText.Lang.EN)
            if (ok) g.speaker.say(p) else if (d.explain.isNotBlank()) g.speaker.say(d.explain)
        }
    }

    Text(
        when (d.kind) {
            Drill.GAP -> "Вставь пропущенное слово"
            Drill.CHOOSE -> "Выбери правильное предложение"
            else -> "Собери предложение по-английски"
        },
        style = MaterialTheme.typography.titleMedium,
    )
    when (d.kind) {
        Drill.GAP, Drill.CHOOSE -> {
            if (d.kind == Drill.GAP) {
                CardBox {
                    Text(
                        if (state.result == null) d.prompt.replace("___", "_____") else d.fullSentence,
                        fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
                    )
                }
            }
            t.choices.forEach { o ->
                val st = when {
                    picked == null -> ChoiceState.NORMAL
                    o == d.answer -> ChoiceState.RIGHT
                    o == picked -> ChoiceState.WRONG
                    else -> ChoiceState.DIM
                }
                ChoiceButton(o, st) {
                    picked = o
                    done(TextMatch.normalize(o) == TextMatch.normalize(d.answer))
                }
            }
        }
        else -> {
            Text(d.prompt, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            CardBox(Modifier.heightIn(min = 110.dp)) {
                TokenRows(chosen.map { t.tiles[it] }) { idx -> if (state.result == null) chosen.removeAt(idx) }
            }
            TokenRows(t.tiles.mapIndexed { i, s -> if (i in chosen) "" else s }) { i ->
                if (state.result != null || i in chosen) return@TokenRows
                chosen.add(i)
                // Проверяем, когда слов столько же, сколько в правильном ответе (лишние слова — ловушки).
                if (chosen.size == d.answerTokens.size) done(SessionBuilder.orderCorrect(d, chosen.map { t.tiles[it] }))
            }
            if (state.result != null && state.result == false) {
                Text(d.answer, fontSize = 20.sp, color = Palette.Good, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            }
        }
    }
    if (state.result != null && d.explain.isNotBlank()) {
        CardBox { Text("💡 ${d.explain}", fontSize = 16.sp) }
    }
    ReportBadDrill {
        scope.launch { g.store.update { it.withoutDrill(d) } }
        g.speaker.stop()
        state.dropBad()
    }
}

/** «В задании ошибка»: задания придумывает нейросеть, и иногда русская фраза не совпадает с английской. */
@Composable
private fun ReportBadDrill(onConfirm: () -> Unit) {
    var ask by remember { mutableStateOf(false) }
    TextButton(onClick = { ask = true }, modifier = Modifier.fillMaxWidth()) {
        Text("🚩 В задании ошибка", color = Palette.Muted, fontSize = 15.sp)
    }
    if (ask) {
        AlertDialog(
            onDismissRequest = { ask = false },
            title = { Text("Задание с ошибкой?") },
            text = { Text("Если задание нельзя решить правильно, Ника уберёт его и больше не покажет. Оно не засчитается как ошибка.") },
            confirmButton = { TextButton(onClick = { ask = false; onConfirm() }) { Text("Да, убрать") } },
            dismissButton = { TextButton(onClick = { ask = false }) { Text("Нет, решу") } },
        )
    }
}

@Composable
private fun TokenRows(tokens: List<String>, onTap: (Int) -> Unit) {
    // Простая раскладка «по строкам»: примерно 15 символов в строке.
    val rows = mutableListOf<MutableList<Int>>()
    var len = 0
    tokens.indices.forEach { i ->
        val l = tokens[i].length + 2
        if (rows.isEmpty() || len + l > 15) { rows.add(mutableListOf()); len = 0 }
        rows.last().add(i); len += l
    }
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        rows.forEach { r ->
            Row {
                r.forEach { i ->
                    val s = tokens[i]
                    Box(
                        Modifier.padding(4.dp)
                            .then(if (s.isEmpty()) Modifier else Modifier.glass(RoundedCornerShape(12.dp), Color.White.copy(alpha = 0.82f), rim = 2.dp))
                            .clickable(enabled = s.isNotEmpty()) { onTap(i) }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    ) { Text(s, fontSize = 20.sp, fontWeight = FontWeight.SemiBold) }
                }
            }
        }
    }
}

@Composable
private fun Results(state: PracticeState, stars: Int, talking: Boolean, onAgain: () -> Unit, onHome: () -> Unit) {
    val g = LocalContext.current.graph
    val hard = g.store.value.words.filter { (state.wrongs[it.id] ?: 0) > 0 }
    var party by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { party = 1; delay(1300); party = 2 }
    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                if (stars == 3) "Великолепно!" else if (stars == 2) "Отлично!" else "Молодец, что старалась!",
                style = MaterialTheme.typography.headlineMedium,
            )
            Nika(NikaMood.DANCE, Modifier.size(width = 150.dp, height = 240.dp), talking = talking)
            Stars(stars)
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIcon(AppIcons.TROPHY, size = 34.dp)
                Text("  Правильно: ${state.correct} из ${state.total}", style = MaterialTheme.typography.titleLarge)
            }
            if (hard.isNotEmpty()) {
                CardBox {
                    Column {
                        Text("Эти слова Ника повторит с тобой завтра:", fontWeight = FontWeight.SemiBold)
                        hard.forEach { Text("${it.emoji} ${it.en} — ${it.ru}", fontSize = 17.sp) }
                    }
                }
            }
            BigButton("🔁", "Ещё раз", brush = Palette.pinkish, appear = 2, onClick = onAgain)
            BigButton("🏠", "Готово", brush = Palette.bluish, appear = 3, onClick = onHome)
        }
        // Праздничное конфетти: два залпа.
        ConfettiBurst(party, origin = Offset(0.5f, 0.3f), count = 80)
    }
}
