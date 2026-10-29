package ru.sovushka.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.sovushka.app.AppGraph
import ru.sovushka.app.graph
import ru.sovushka.core.abc.Abc
import kotlin.math.roundToInt
import kotlin.random.Random

/** Цвета букваря: гласные — красные, согласные — синие, знаки — серые (как в букваре Жуковой). */
private val VowelRed = Color(0xFFE53950)
private val ConsonantBlue = Color(0xFF2F6FD6)
private val SignGrey = Color(0xFF7A7499)
private val AbcBrush = Brush.linearGradient(listOf(Color(0xD9FF9A3C), Color(0xD9F25C8A)))
private val AbcBrushSoft = Brush.linearGradient(listOf(Color(0xCCFFC15A), Color(0xCCFF8A65)))

private fun colorOf(c: Char): Color {
    val i = Abc.indexOf(c)
    if (i < 0) return Palette.Ink
    return when (Abc.LETTERS[i].kind) {
        Abc.Kind.VOWEL -> VowelRed
        Abc.Kind.CONSONANT -> ConsonantBlue
        Abc.Kind.SIGN -> SignGrey
    }
}

/** Текст, где каждая буква раскрашена: гласные красные, согласные синие. */
@Composable
private fun AbcText(text: String, size: TextUnit, modifier: Modifier = Modifier) {
    Text(
        buildAnnotatedString {
            text.forEach { ch -> withStyle(SpanStyle(color = colorOf(ch))) { append(ch) } }
        },
        fontSize = size, fontWeight = FontWeight.Black, textAlign = TextAlign.Center, modifier = modifier,
    )
}

/* ======================= Дорожка букв ======================= */

/** Буквы для младшей: следующая буква, повторение и все 33 буквы дорожкой. */
@Composable
fun AbcHomeScreen(vm: AppViewModel) {
    val g = LocalContext.current.graph
    val prefs by g.settings.prefs.collectAsState()
    val data by g.store.data.collectAsState()
    val talking by g.speaker.speaking.collectAsState()
    val learned = data.abcLearned.coerceIn(0, Abc.COUNT)
    val next = Abc.LETTERS.getOrNull(learned)
    val hello = remember(learned) {
        val name = prefs.youngerName.ifBlank { "малышка" }
        when {
            learned == 0 -> "Привет, $name! Давай учить буквы! Нажми на первую букву."
            next == null -> "$name, ты знаешь все буквы! Давай читать слова!"
            else -> "$name, ты знаешь уже ${letters(learned)}! Сегодня новая буква — ${next.name}."
        }
    }
    LaunchedEffect(Unit) { g.speaker.say(hello) }
    GradientScreen {
        Column(Modifier.fillMaxSize()) {
            TopBar("", onBack = { vm.back() }) {
                KidAvatar(false, "👧", 44.dp, Modifier.padding(end = 10.dp))
                Text("⭐ ${data.younger.stars}", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Palette.Ink, modifier = Modifier.padding(end = 12.dp))
            }
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).navigationBarsPadding().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(Modifier.fillMaxWidth().clickable { g.speaker.speak(hello) }, horizontalArrangement = Arrangement.Center) {
                    Nika(if (talking) NikaMood.TALK else NikaMood.WAVE, Modifier.height(150.dp), talking = talking)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    if (next != null) {
                        GlassButton(
                            { vm.go(Screen.AbcLesson(learned)) }, Modifier.weight(1f).aspectRatio(1f), brush = AbcBrush,
                            shape = RoundedCornerShape(30.dp), depth = 8.dp, fillWidth = true, contentPadding = PaddingValues(8.dp),
                        ) {
                            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(next.pair, fontSize = 72.sp, fontWeight = FontWeight.Black, color = Color.White, style = androidx.compose.ui.text.TextStyle(shadow = GlassTextShadow))
                                Text("новая буква", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                    if (learned >= 2 || next == null) {
                        GlassButton(
                            { vm.go(Screen.AbcLesson(-1)) }, Modifier.weight(1f).aspectRatio(1f), brush = Palette.bluish,
                            shape = RoundedCornerShape(30.dp), depth = 8.dp, fillWidth = true, contentPadding = PaddingValues(8.dp),
                        ) {
                            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                Sticker("📖", 76.dp)
                                Text("повторяем", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(top = 6.dp))
                            }
                        }
                    }
                }
                // Все буквы: знакомые — яркие (можно повторить урок), следующая — с рамкой, остальные — бледные.
                Abc.LETTERS.chunked(6).forEachIndexed { row, chunk ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        chunk.forEachIndexed { col, l ->
                            val i = row * 6 + col
                            val open = i <= learned && i < Abc.COUNT
                            Box(
                                Modifier.weight(1f).aspectRatio(1f)
                                    .glass(
                                        RoundedCornerShape(16.dp),
                                        when {
                                            i < learned -> Color.White.copy(alpha = 0.9f)
                                            i == learned -> Color(0xFFFFE0B2).copy(alpha = 0.95f)
                                            else -> Color.White.copy(alpha = 0.35f)
                                        },
                                        rim = if (i == learned) 3.dp else 1.dp,
                                    )
                                    .clickable(enabled = open) { vm.go(Screen.AbcLesson(i)) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    "${l.upper}", fontSize = 26.sp, fontWeight = FontWeight.Black,
                                    color = if (open) colorOf(l.upper) else Palette.Muted.copy(alpha = 0.45f),
                                )
                            }
                        }
                        repeat(6 - chunk.size) { Box(Modifier.weight(1f)) }
                    }
                }
                Hint("Порядок букв — как в букваре Н. С. Жуковой: гласные красные, согласные синие. Сначала буква, потом слоги «паровозиком», потом слова.")
            }
        }
    }
}

private fun letters(n: Int): String = when {
    n % 10 == 1 && n % 100 != 11 -> "$n букву"
    n % 10 in 2..4 && n % 100 !in 12..14 -> "$n буквы"
    else -> "$n букв"
}

/* ======================= Урок ======================= */

/** Урок буквы (index ≥ 0) или повторение (index = -1). */
@Composable
fun AbcLessonScreen(vm: AppViewModel, screen: Screen.AbcLesson) {
    val g = LocalContext.current.graph
    val data by g.store.data.collectAsState()
    val talking by g.speaker.speaking.collectAsState()
    val learnedAtStart = remember { data.abcLearned }
    val tasks = remember(screen) {
        if (screen.index >= 0) Abc.lesson(screen.index, Random(screen.run)) else Abc.review(learnedAtStart.coerceAtLeast(1), Random(screen.run))
    }
    var step by remember { mutableIntStateOf(0) }
    var mistakes by remember { mutableIntStateOf(0) }
    val done = step >= tasks.size
    val stars = if (mistakes <= 1) 3 else if (mistakes <= 4) 2 else 1
    val letter = Abc.LETTERS.getOrNull(screen.index)

    LaunchedEffect(done) {
        if (!done) return@LaunchedEffect
        rewardYounger(g, stars)
        if (screen.index >= 0) g.store.update { it.copy(abcLearned = maxOf(it.abcLearned, screen.index + 1)) }
        g.speaker.say(
            if (letter != null) "Ура! Теперь ты знаешь букву ${letter.name}!" else "Ура! Ты отлично читаешь!",
        )
    }

    GradientScreen {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            TopBar(if (done) "" else "${step + 1} / ${tasks.size}", onBack = { vm.back() })
            if (done) {
                Nika(NikaMood.DANCE, Modifier.height(260.dp), talking = talking)
                if (letter != null) AbcText(letter.pair, 96.sp)
                Stars(stars, size = 56)
                Box(Modifier.padding(20.dp)) { BigTile("🏠", AbcBrush, Modifier.size(140.dp)) { vm.back() } }
                return@Column
            }
            val task = tasks[step]
            val next: (Boolean) -> Unit = { ok ->
                if (!ok) mistakes++
                step++
            }
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).navigationBarsPadding().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                androidx.compose.runtime.key(step) {
                    when (task) {
                        is Abc.Task.Meet -> MeetLetter(g, task, talking) { step++ }
                        is Abc.Task.Trace -> TraceLetter(g, task, talking) { step++ }
                        is Abc.Task.FindLetter -> Choice(
                            g, talking,
                            ask = "Где буква ${task.target.name}?",
                            options = task.options.map { if (task.lowercase) "${it.lower}" else "${it.upper}" },
                            right = if (task.lowercase) "${task.target.lower}" else "${task.target.upper}",
                            praise = "Да! Это буква ${task.target.name}!",
                            wrong = { picked -> "Это буква ${Abc.LETTERS[Abc.indexOf(picked[0])].name}. А нам нужна буква ${task.target.name}." },
                            big = true, onDone = next,
                        )
                        is Abc.Task.FindSyllable -> Choice(
                            g, talking,
                            ask = "Где слог ${task.target.lowercase()}?",
                            options = task.options, right = task.target,
                            praise = "Да! ${task.target.lowercase()}!",
                            wrong = { picked -> "Это ${picked.lowercase()}. А нам нужно ${task.target.lowercase()}." },
                            big = true, onDone = next,
                        )
                        is Abc.Task.FirstSound -> PictureChoice(
                            g, talking,
                            ask = if (task.letter.upper == 'Ы' || task.letter.kind == Abc.Kind.SIGN) "В каком слове есть ${task.letter.name}?"
                            else "Какое слово начинается с буквы ${task.letter.name}?",
                            show = { AbcText(task.letter.pair, 64.sp) },
                            options = task.options, right = task.right,
                            praise = "Да! ${task.right.word.replaceFirstChar { it.uppercase() }}!",
                            wrong = { p -> "Это ${p.word}. Послушай: ${task.right.word}!" },
                            onDone = next,
                        )
                        is Abc.Task.Merge -> MergeTrain(g, task, talking) { step++ }
                        is Abc.Task.ReadWord -> ReadWord(g, task, talking, lesson = if (screen.index >= 0) screen.index else learnedAtStart - 1, onDone = next)
                    }
                }
            }
        }
    }
}

/** Знакомство с буквой: большая буква, как звучит, на что похожа, картинка. */
@Composable
private fun MeetLetter(g: AppGraph, task: Abc.Task.Meet, talking: Boolean, onNext: () -> Unit) {
    val l = task.letter
    val scope = rememberCoroutineScope()
    val pic = l.pics.firstOrNull()
    val text = "Это буква ${l.name}. ${l.hear} ${l.look}"
    LaunchedEffect(l) { g.speaker.say(text) }
    Nika(if (talking) NikaMood.TALK else NikaMood.HAPPY, Modifier.height(120.dp).clickable { scope.launch { g.speaker.say(text) } }, talking = talking)
    Box(
        Modifier.fillMaxWidth().glass(RoundedCornerShape(32.dp), Color.White.copy(alpha = 0.85f), rim = 3.dp)
            .clickable { g.speaker.speak(l.hear) }.padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) { AbcText(l.pair, 150.sp) }
    if (pic != null) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Sticker(pic.emoji, 90.dp, modifier = Modifier.clickable { g.speaker.speak(pic.word) })
            AbcText(pic.word.uppercase(), 34.sp)
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        BigTile("🔊", Palette.bluish, Modifier.weight(1f)) { g.speaker.speak(text) }
        BigTile("➡️", AbcBrush, Modifier.weight(1f)) { onNext() }
    }
}

/** Обведи букву пальчиком: бледная буква, по ней рисуем. */
@Composable
private fun TraceLetter(g: AppGraph, task: Abc.Task.Trace, talking: Boolean, onNext: () -> Unit) {
    val l = task.letter
    val strokes = remember(l) { mutableStateListOf<List<Offset>>() }
    var current by remember(l) { mutableStateOf<List<Offset>>(emptyList()) }
    val ask = "Обведи букву ${l.name} пальчиком!"
    LaunchedEffect(l) { g.speaker.say(ask) }
    Nika(if (talking) NikaMood.TALK else NikaMood.IDLE, Modifier.height(110.dp).clickable { g.speaker.speak(ask) }, talking = talking)
    Box(
        Modifier.fillMaxWidth().aspectRatio(1f).glass(RoundedCornerShape(32.dp), Color.White.copy(alpha = 0.9f), rim = 3.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text("${l.upper}", fontSize = 230.sp, fontWeight = FontWeight.Black, color = colorOf(l.upper).copy(alpha = 0.18f))
        val ink = colorOf(l.upper)
        Canvas(
            Modifier.fillMaxSize().pointerInput(l) {
                detectDragGestures(
                    onDragStart = { current = listOf(it) },
                    onDragEnd = { if (current.size > 1) strokes.add(current); current = emptyList() },
                    onDragCancel = { current = emptyList() },
                ) { change, _ -> current = current + change.position }
            },
        ) {
            (strokes + listOf(current)).forEach { pts ->
                if (pts.size > 1) {
                    val p = Path().apply { moveTo(pts[0].x, pts[0].y); pts.drop(1).forEach { lineTo(it.x, it.y) } }
                    drawPath(p, ink.copy(alpha = 0.85f), style = Stroke(width = 22.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
            }
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        BigTile("🧽", Palette.bluish, Modifier.weight(1f)) { strokes.clear(); current = emptyList() }
        BigTile("✅", AbcBrush, Modifier.weight(1f)) {
            if (strokes.isNotEmpty()) g.speaker.speak("Красиво! Молодец!")
            onNext()
        }
    }
}

/** Паровозик: согласная «бежит» к гласной, и получается слог. */
@Composable
private fun MergeTrain(g: AppGraph, task: Abc.Task.Merge, talking: Boolean, onNext: () -> Unit) {
    val scope = rememberCoroutineScope()
    val run = remember(task) { Animatable(0f) }
    var merged by remember(task) { mutableStateOf(false) }
    val c = task.consonant
    val syllable = "${c.upper}${task.vowel}"
    suspend fun play() {
        merged = false
        run.snapTo(0f)
        g.speaker.say("Буква ${c.name} бежит к букве ${Abc.LETTERS[Abc.indexOf(task.vowel)].name}.")
        run.animateTo(1f, tween(1100, easing = FastOutSlowInEasing))
        merged = true
        g.speaker.say(Abc.sayMerge(c, task.vowel))
    }
    LaunchedEffect(task) { play() }
    Nika(if (talking) NikaMood.TALK else NikaMood.HAPPY, Modifier.height(110.dp), talking = talking)
    Box(Modifier.fillMaxWidth().height(150.dp).glass(RoundedCornerShape(30.dp), Color.White.copy(alpha = 0.85f), rim = 3.dp)) {
        // Дорожка.
        Canvas(Modifier.fillMaxSize()) {
            val y = size.height * 0.78f
            drawLine(Color(0xFFFFB74D), Offset(size.width * 0.15f, y), Offset(size.width * 0.72f, y), strokeWidth = 6.dp.toPx(), cap = StrokeCap.Round)
        }
        val dx = (run.value * 120f).roundToInt()
        Text(
            "${c.upper}", fontSize = 90.sp, fontWeight = FontWeight.Black, color = ConsonantBlue,
            modifier = Modifier.align(Alignment.CenterStart).padding(start = 28.dp).offset { IntOffset((dx * density).roundToInt(), 0) },
        )
        Text(
            "${task.vowel}", fontSize = 90.sp, fontWeight = FontWeight.Black, color = VowelRed,
            modifier = Modifier.align(Alignment.CenterStart).padding(start = 210.dp),
        )
    }
    if (merged) AbcText(syllable, 96.sp, Modifier.clickable { g.speaker.speak(Abc.sayMerge(c, task.vowel)) })
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        BigTile("🔁", Palette.bluish, Modifier.weight(1f)) { scope.launch { play() } }
        BigTile("➡️", AbcBrush, Modifier.weight(1f)) { onNext() }
    }
}

/** Выбор из букв или слогов. */
@Composable
private fun Choice(
    g: AppGraph,
    talking: Boolean,
    ask: String,
    options: List<String>,
    right: String,
    praise: String,
    wrong: (String) -> String,
    big: Boolean,
    onDone: (Boolean) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var missed by remember { mutableStateOf(false) }
    var locked by remember { mutableStateOf(false) }
    var mood by remember { mutableStateOf(NikaMood.IDLE) }
    var shaking by remember { mutableStateOf<String?>(null) }
    val shake = remember { Animatable(0f) }
    LaunchedEffect(ask) { g.speaker.say(ask) }
    Nika(if (talking) NikaMood.TALK else mood, Modifier.height(130.dp).clickable { g.speaker.speak(ask) }, talking = talking)
    options.chunked(2).forEach { row ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            row.forEach { o ->
                GlassButton(
                    {
                        if (locked) return@GlassButton
                        locked = true
                        scope.launch {
                            if (o == right) {
                                mood = NikaMood.HAPPY
                                g.speaker.say(praise)
                                delay(250)
                                onDone(!missed)
                            } else {
                                missed = true
                                mood = NikaMood.SURPRISED
                                shaking = o
                                launch {
                                    for (x in listOf(24f, -24f, 16f, -16f, 0f)) shake.animateTo(x, tween(60))
                                    shaking = null
                                }
                                g.speaker.say(wrong(o))
                                mood = NikaMood.IDLE
                            }
                            locked = false
                        }
                    },
                    Modifier.weight(1f).offset { IntOffset(if (shaking == o) shake.value.roundToInt() else 0, 0) },
                    brush = GlassWhite, shape = RoundedCornerShape(26.dp), depth = 6.dp, fillWidth = true,
                    contentPadding = PaddingValues(vertical = 12.dp),
                ) {
                    AbcText(o, if (big) 64.sp else 40.sp, Modifier.weight(1f))
                }
            }
            if (row.size == 1) Box(Modifier.weight(1f))
        }
    }
}

/** Выбор картинки. */
@Composable
private fun PictureChoice(
    g: AppGraph,
    talking: Boolean,
    ask: String,
    show: @Composable () -> Unit,
    options: List<Abc.Pic>,
    right: Abc.Pic,
    praise: String,
    wrong: (Abc.Pic) -> String,
    onDone: (Boolean) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var missed by remember { mutableStateOf(false) }
    var locked by remember { mutableStateOf(false) }
    var mood by remember { mutableStateOf(NikaMood.IDLE) }
    LaunchedEffect(ask) { g.speaker.say(ask) }
    Nika(if (talking) NikaMood.TALK else mood, Modifier.height(120.dp).clickable { g.speaker.speak(ask) }, talking = talking)
    show()
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        options.forEach { p ->
            Box(
                Modifier.weight(1f).aspectRatio(0.85f)
                    .glass(RoundedCornerShape(24.dp), Color.White.copy(alpha = 0.8f), rim = 2.dp)
                    .clickable {
                        if (locked) return@clickable
                        locked = true
                        scope.launch {
                            if (p == right) {
                                mood = NikaMood.HAPPY
                                g.speaker.say(praise)
                                delay(250)
                                onDone(!missed)
                            } else {
                                missed = true
                                mood = NikaMood.SURPRISED
                                g.speaker.say(wrong(p))
                                mood = NikaMood.IDLE
                            }
                            locked = false
                        }
                    },
                contentAlignment = Alignment.Center,
            ) { Sticker(p.emoji, 76.dp, seed = p.word.hashCode()) }
        }
    }
}

/** Прочитай слово (по слогам) и найди к нему картинку. */
@Composable
private fun ReadWord(g: AppGraph, task: Abc.Task.ReadWord, talking: Boolean, lesson: Int, onDone: (Boolean) -> Unit) {
    val scope = rememberCoroutineScope()
    val word = task.word.word
    val parts = remember(word) { Abc.syllables(word) }
    val help = parts.joinToString("… ") + "… " + word + "!"
    var helped by remember { mutableStateOf(false) }
    PictureChoice(
        g, talking,
        ask = "Прочитай слово и найди картинку!",
        show = {
            // Слоги через дефис, как в букваре; нажатие на слог — Ника его читает.
            Row(
                Modifier.fillMaxWidth().glass(RoundedCornerShape(28.dp), Color.White.copy(alpha = 0.88f), rim = 3.dp).padding(vertical = 10.dp, horizontal = 8.dp),
                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
            ) {
                parts.forEachIndexed { i, s ->
                    if (i > 0) Text("-", fontSize = 40.sp, color = Palette.Muted, modifier = Modifier.padding(horizontal = 2.dp))
                    AbcText(Abc.shown(s, lesson), if (word.length > 7) 40.sp else 52.sp, Modifier.clickable { g.speaker.speak(s) })
                }
            }
            Box(
                Modifier.glass(RoundedCornerShape(50), Color.White.copy(alpha = 0.7f), rim = 2.dp)
                    .clickable { helped = true; scope.launch { g.speaker.say(help, slow = true) } }.padding(horizontal = 16.dp, vertical = 8.dp),
            ) { Text("🔊 Ника, помоги", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Palette.Ink) }
        },
        options = task.options, right = task.word,
        praise = "${word.replaceFirstChar { it.uppercase() }}! Молодец, ты прочитала!",
        wrong = { p -> "Это ${p.word}. Давай прочитаем вместе: $help" },
        onDone = { ok -> onDone(ok && !helped) },
    )
}
