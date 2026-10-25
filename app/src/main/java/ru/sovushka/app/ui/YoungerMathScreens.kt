package ru.sovushka.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.sovushka.app.graph
import kotlin.math.roundToInt
import kotlin.random.Random

/** Счётные предметы: картинка и как сказать «сколько …?». */
private data class Thing(val emoji: String, val many: String)

private val THINGS = listOf(
    Thing("🍎", "яблок"), Thing("🐱", "котиков"), Thing("⭐", "звёздочек"), Thing("🎈", "шариков"), Thing("🐟", "рыбок"),
    Thing("🌸", "цветочков"), Thing("🚗", "машинок"), Thing("🐶", "собачек"), Thing("🍓", "клубничек"), Thing("🦋", "бабочек"),
)

private const val COUNT = "count"
private const val FIND = "find"
private const val MORE = "more"
private const val ADD = "add"

/** Счёт для младшей: четыре игры без чтения, всё объясняет голос. */
@Composable
fun YoungerMathScreen(vm: AppViewModel) {
    val g = LocalContext.current.graph
    val prefs by g.settings.prefs.collectAsState()
    val data by g.store.data.collectAsState()
    val talking by g.speaker.speaking.collectAsState()
    val hello = "Привет, ${prefs.youngerName}! Давай считать! Выбирай игру."
    LaunchedEffect(Unit) { g.speaker.say(hello) }
    GradientScreen {
        Column(Modifier.fillMaxSize()) {
            TopBar("", onBack = { vm.back() }) {
                KidAvatar(false, "👧", 44.dp, Modifier.padding(end = 10.dp))
                Text("⭐ ${data.younger.stars}", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Palette.Ink, modifier = Modifier.padding(end = 12.dp))
            }
            Row(Modifier.fillMaxWidth().clickable { g.speaker.speak(hello) }, horizontalArrangement = Arrangement.Center) {
                Nika(if (talking) NikaMood.TALK else NikaMood.WAVE, Modifier.height(170.dp), talking = talking)
            }
            Column(
                Modifier.weight(1f).navigationBarsPadding().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                val games = listOf(
                    Triple("🍎", COUNT, "Сколько? Посчитай картинки!") to MathPalette.teal,
                    Triple("🔍", FIND, "Найди число!") to MathPalette.primary,
                    Triple("⚖️", MORE, "Где больше?") to MathPalette.indigo,
                    Triple("➕", ADD, "Сложи картинки!") to MathPalette.burgundy,
                )
                games.chunked(2).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        row.forEach { (game, brush) ->
                            BigTile(game.first, brush, Modifier.weight(1f).aspectRatio(1.15f)) {
                                g.speaker.speak(game.third)
                                vm.go(Screen.YoungerMathGame(game.second))
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Один раунд игры. */
private class MathRound(val thing: Thing, val a: Int, val b: Int, val target: Int, val options: List<Int>)

private fun makeRound(kind: String, r: Random): MathRound {
    val thing = THINGS[r.nextInt(THINGS.size)]
    /** Правильное число и рядом с ним похожие (на 1–3 больше или меньше). */
    fun around(n: Int, count: Int, lo: Int, hi: Int): List<Int> {
        val set = linkedSetOf(n)
        while (set.size < count) {
            val d = r.nextInt(1, 4) * (if (r.nextBoolean()) 1 else -1)
            val x = (n + d).coerceIn(lo, hi)
            if (x != n) set.add(x)
        }
        return set.shuffled(r)
    }
    return when (kind) {
        COUNT -> { val n = r.nextInt(2, 13); MathRound(thing, n, 0, n, around(n, 3, 1, 20)) }
        FIND -> { val n = r.nextInt(0, 21); MathRound(thing, 0, 0, n, around(n, 4, 0, 20)) }
        MORE -> {
            val a = r.nextInt(1, 10)
            var b: Int
            do { b = r.nextInt(1, 10) } while (b == a)
            MathRound(thing, a, b, maxOf(a, b), listOf(a, b))
        }
        else -> { val a = r.nextInt(1, 6); val b = r.nextInt(1, 6); MathRound(thing, a, b, a + b, around(a + b, 3, 1, 12)) }
    }
}

private fun question(kind: String, round: MathRound): String = when (kind) {
    COUNT -> "Сколько здесь ${round.thing.many}? Посчитай!"
    FIND -> "Найди число ${round.target}!"
    MORE -> "Где больше ${round.thing.many}?"
    else -> "Здесь ${round.a} и ещё ${round.b}. Сколько всего ${round.thing.many}?"
}

@Composable
fun YoungerMathGameScreen(vm: AppViewModel, screen: Screen.YoungerMathGame) {
    val g = LocalContext.current.graph
    val scope = rememberCoroutineScope()
    val talking by g.speaker.speaking.collectAsState()
    val kind = screen.kind
    val rounds = 6
    var round by remember { mutableIntStateOf(0) }
    var stars by remember { mutableIntStateOf(0) }
    var mistakes by remember { mutableIntStateOf(0) }
    var mood by remember { mutableStateOf(NikaMood.IDLE) }
    var locked by remember { mutableStateOf(false) }
    val r = remember(screen) { Random(screen.run) }
    val current = remember(round) { makeRound(kind, r) }
    // Какие картинки уже посчитали пальчиком (в «Сколько?»).
    val counted = remember(round) { mutableStateListOf<Int>() }
    val shake = remember { Animatable(0f) }
    var shaking by remember { mutableStateOf<Int?>(null) }
    val done = round >= rounds

    suspend fun ask() {
        mood = NikaMood.IDLE
        g.speaker.say(question(kind, current))
    }

    suspend fun countAloud(n: Int) {
        for (i in 1..n) {
            if (kind == COUNT) { if (i - 1 !in counted) counted.add(i - 1) }
            g.speaker.say("$i")
        }
    }

    LaunchedEffect(round) {
        if (!done) ask() else {
            mood = NikaMood.HAPPY
            rewardYounger(g, if (mistakes <= 1) 3 else if (mistakes <= 3) 2 else 1)
            g.speaker.say("Ура! Ты отлично считаешь!")
        }
    }

    fun answer(value: Int, key: Int) = scope.launch {
        if (locked) return@launch
        locked = true
        if (value == current.target) {
            stars++; mood = NikaMood.HAPPY
            when (kind) {
                MORE -> g.speaker.say("Да! ${maxOf(current.a, current.b)} больше, чем ${minOf(current.a, current.b)}!")
                FIND -> g.speaker.say("Да! Это ${current.target}! Молодец!")
                else -> g.speaker.say("Да! ${current.target}! Молодец!")
            }
            delay(300)
            round++
        } else {
            mistakes++; mood = NikaMood.SURPRISED
            shaking = key
            launch {
                for (x in listOf(24f, -24f, 16f, -16f, 0f)) shake.animateTo(x, tween(60))
                shaking = null
            }
            when (kind) {
                COUNT -> { g.speaker.say("Давай посчитаем вместе!"); countAloud(current.target); g.speaker.say("Всего ${current.target}!") }
                FIND -> { g.speaker.say("Это число $value. А нам нужно ${current.target}!") }
                MORE -> { g.speaker.say("Посчитай ещё раз! Здесь ${current.a}, а здесь ${current.b}.") }
                else -> { g.speaker.say("Давай посчитаем все вместе!"); countAloud(current.target); g.speaker.say("Всего ${current.target}!") }
            }
            mood = NikaMood.IDLE
        }
        locked = false
    }

    GradientScreen {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            TopBar("⭐".repeat(stars), onBack = { vm.back() })
            if (done) {
                Nika(NikaMood.DANCE, Modifier.height(280.dp), talking = talking)
                Stars(if (mistakes <= 1) 3 else if (mistakes <= 3) 2 else 1, size = 60)
                Box(Modifier.padding(24.dp)) { BigTile("🏠", MathPalette.primary, Modifier.size(140.dp)) { vm.back() } }
                return@Column
            }
            Nika(if (talking) NikaMood.TALK else mood, Modifier.height(130.dp).clickable { scope.launch { ask() } }, talking = talking)
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).navigationBarsPadding().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                val dx = { key: Int -> if (shaking == key) shake.value else 0f }
                when (kind) {
                    COUNT -> {
                        ThingsGrid(current.thing, current.a, counted) { i ->
                            if (i !in counted) {
                                counted.add(i)
                                g.speaker.speak("${counted.size}")
                            }
                        }
                        NumberRow(current.options, dx) { v -> answer(v, v) }
                    }
                    FIND -> {
                        current.options.chunked(2).forEach { row ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                row.forEach { v ->
                                    NumberTile(v, Modifier.weight(1f).offset { IntOffset(dx(v).roundToInt(), 0) }, 64.sp) { answer(v, v) }
                                }
                            }
                        }
                    }
                    MORE -> {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            listOf(current.a, current.b).forEachIndexed { i, n ->
                                Box(
                                    Modifier.weight(1f).offset { IntOffset(dx(i).roundToInt(), 0) }
                                        .glass(RoundedCornerShape(26.dp), Color.White.copy(alpha = 0.8f), rim = 3.dp)
                                        .clickable { answer(n, i) }.padding(10.dp),
                                    contentAlignment = Alignment.Center,
                                ) { ThingsGrid(current.thing, n, emptyList(), perRow = 3, size = 40.dp) {} }
                            }
                        }
                    }
                    else -> {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(Modifier.weight(1f)) { ThingsGrid(current.thing, current.a, emptyList(), perRow = 3, size = 40.dp) {} }
                            Text("+", fontSize = 48.sp, fontWeight = FontWeight.Black, color = MathPalette.Navy)
                            Box(Modifier.weight(1f)) { ThingsGrid(current.thing, current.b, emptyList(), perRow = 3, size = 40.dp) {} }
                        }
                        NumberRow(current.options, dx) { v -> answer(v, v) }
                    }
                }
            }
        }
    }
}

/** Картинки рядами (по 5, как в счётной рамке). Нажатую картинку отмечаем номером. */
@Composable
private fun ThingsGrid(thing: Thing, n: Int, counted: List<Int>, perRow: Int = 5, size: Dp = 52.dp, onTap: (Int) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        (0 until n).chunked(perRow).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { i ->
                    Box(Modifier.clickable { onTap(i) }, contentAlignment = Alignment.TopEnd) {
                        Sticker(thing.emoji, size, seed = i * 7 + thing.emoji.hashCode())
                        val at = counted.indexOf(i)
                        if (at >= 0) {
                            Box(
                                Modifier.size(22.dp).glass(CircleShape, MathPalette.Teal.copy(alpha = 0.9f)),
                                contentAlignment = Alignment.Center,
                            ) { Text("${at + 1}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NumberRow(options: List<Int>, dx: (Int) -> Float, onPick: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        options.forEach { v -> NumberTile(v, Modifier.weight(1f).offset { IntOffset(dx(v).roundToInt(), 0) }, 52.sp) { onPick(v) } }
    }
}

/** Большая стеклянная плитка с числом. */
@Composable
private fun NumberTile(value: Int, modifier: Modifier, size: androidx.compose.ui.unit.TextUnit, onClick: () -> Unit) {
    GlassButton(
        onClick, modifier, brush = GlassWhite, shape = RoundedCornerShape(26.dp), depth = 6.dp, fillWidth = true,
        contentPadding = PaddingValues(vertical = 14.dp),
    ) {
        Text("$value", fontSize = size, fontWeight = FontWeight.Black, color = MathPalette.Navy, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
    }
}
