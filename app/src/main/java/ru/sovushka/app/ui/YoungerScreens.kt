package ru.sovushka.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.sovushka.app.AppGraph
import ru.sovushka.app.graph
import ru.sovushka.app.today
import ru.sovushka.app.voice.ListenException
import ru.sovushka.core.Builtin
import ru.sovushka.core.SpeechText
import ru.sovushka.core.TextMatch
import kotlin.math.roundToInt

/** Набор слов для младшей: встроенные темы + слова старшей сестры с картинками. */
const val SISTER_PACK = "sister"

private fun packFor(g: AppGraph, id: String): Builtin.Pack? =
    if (id == SISTER_PACK) {
        val words = g.store.value.words.filter { it.emoji.isNotBlank() && it.en.split(' ').size <= 2 }
            .distinctBy { it.en.lowercase() }.takeLast(30)
            .map { Builtin.PackWord(it.en, it.ru, it.emoji) }
        if (words.size >= 4) Builtin.Pack(SISTER_PACK, "⭐", "Слова сестры", words) else null
    } else Builtin.pack(id)

/** Главный экран младшей: только картинки, всё объясняет голос. */
@Composable
fun YoungerHomeScreen(vm: AppViewModel) {
    val g = LocalContext.current.graph
    val prefs by g.settings.prefs.collectAsState()
    val data by g.store.data.collectAsState()
    val talking by g.speaker.speaking.collectAsState()
    val packs = remember(data.words.size) { listOfNotNull(packFor(g, SISTER_PACK)) + Builtin.packs }
    LaunchedEffect(Unit) { g.speaker.say("Привет, ${prefs.youngerName}! Нажми на картинку!") }
    GradientScreen {
        Column(Modifier.fillMaxSize()) {
            TopBar("", onBack = { vm.back() }) {
                Text("⭐ ${data.younger.stars}", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Palette.Ink, modifier = Modifier.padding(end = 12.dp))
            }
            Row(Modifier.fillMaxWidth().clickable { g.speaker.speak("Привет, ${prefs.youngerName}! Нажми на картинку!") }, horizontalArrangement = Arrangement.Center) {
                Nika(if (talking) NikaMood.TALK else NikaMood.WAVE, Modifier.height(150.dp), talking = talking)
            }
            LazyVerticalGrid(
                GridCells.Fixed(3),
                Modifier.weight(1f).navigationBarsPadding().padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(packs, key = { it.id }) { p ->
                    BigTile(p.emoji, if (p.id == SISTER_PACK) Palette.pinkish else Palette.buttonSoft) {
                        g.speaker.speak(p.title)
                        vm.go(Screen.YoungerPack(p.id))
                    }
                }
            }
        }
    }
}

@Composable
private fun BigTile(emoji: String, brush: Brush, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier.aspectRatio(1f).shadow(6.dp, RoundedCornerShape(26.dp)).clip(RoundedCornerShape(26.dp)).background(brush).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(emoji, fontSize = 52.sp) }
}

/** Выбор игры: попугай — повторять, лупа — искать. */
@Composable
fun YoungerPackScreen(vm: AppViewModel, packId: String) {
    val g = LocalContext.current.graph
    val talking by g.speaker.speaking.collectAsState()
    val pack = remember { packFor(g, packId) }
    if (pack == null) { LaunchedEffect(Unit) { vm.back() }; return }
    val intro = "Попугай — повторяй слова за мной. Лупа — найди картинку!"
    LaunchedEffect(Unit) { g.speaker.say(intro) }
    GradientScreen {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            TopBar(pack.emoji, onBack = { vm.back() })
            Nika(if (talking) NikaMood.TALK else NikaMood.IDLE, Modifier.height(170.dp).clickable { g.speaker.speak(intro) }, talking = talking)
            Column(Modifier.padding(24.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                BigTile("🦜", Palette.pinkish, Modifier.size(170.dp)) {
                    vm.go(Screen.YoungerGame(packId, find = false))
                }
                BigTile("🔍", Palette.bluish, Modifier.size(170.dp)) {
                    vm.go(Screen.YoungerGame(packId, find = true))
                }
            }
        }
    }
}

private suspend fun rewardYounger(g: AppGraph, stars: Int) {
    g.store.update { it.copy(younger = it.younger.addSession(today(), stars)) }
}

/** «Повторяй за мной»: Ника называет слово, малышка повторяет в микрофон. */
@Composable
fun RepeatGameScreen(vm: AppViewModel, screen: Screen.YoungerGame) {
    val g = LocalContext.current.graph
    val mic = LocalMic.current
    val scope = rememberCoroutineScope()
    val talking by g.speaker.speaking.collectAsState()
    val listening by g.listener.listening.collectAsState()
    val level by g.listener.level.collectAsState()
    val words = remember(screen) { packFor(g, screen.packId)?.words.orEmpty().shuffled().take(8) }
    var index by remember { mutableIntStateOf(0) }
    var stars by remember { mutableIntStateOf(0) }
    var tries by remember { mutableIntStateOf(0) }
    var mood by remember { mutableStateOf(NikaMood.IDLE) }
    var done by remember { mutableStateOf(false) }
    val canListen = remember { g.listener.available() }
    val w = words.getOrNull(index)

    suspend fun present() {
        val x = words.getOrNull(index) ?: return
        mood = NikaMood.IDLE
        g.speaker.say("${x.ru}.")
        g.speaker.say(x.en, SpeechText.Lang.EN)
        if (canListen) g.speaker.say("Повтори!") else g.speaker.say(x.en, SpeechText.Lang.EN, slow = true)
    }

    fun next() = scope.launch {
        tries = 0
        if (index + 1 >= words.size) {
            done = true; mood = NikaMood.HAPPY
            rewardYounger(g, stars.coerceAtMost(3).coerceAtLeast(1))
            g.speaker.say("Ура! Ты молодец! Hooray!")
        } else {
            index++
            present()
        }
    }

    LaunchedEffect(screen) { present() }

    GradientScreen {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            TopBar("⭐".repeat(stars.coerceAtMost(8)), onBack = { vm.back() })
            if (w == null || done) {
                Nika(NikaMood.DANCE, Modifier.height(280.dp), talking = talking)
                Stars(3, size = 60)
                Box(Modifier.padding(24.dp)) { BigTile("🏠", Palette.button, Modifier.size(140.dp)) { vm.back() } }
                return@Column
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp)) {
                Nika(if (talking) NikaMood.TALK else if (listening) NikaMood.LISTEN else mood, Modifier.height(170.dp), talking = talking)
                Box(
                    Modifier.size(200.dp).shadow(8.dp, CircleShape).clip(CircleShape).background(Color.White)
                        .clickable { g.speaker.speak(w.en, SpeechText.Lang.EN) },
                    contentAlignment = Alignment.Center,
                ) { Text(w.emoji, fontSize = 110.sp) }
            }
            Text(w.en, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, color = Palette.Pink, modifier = Modifier.padding(top = 12.dp))
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                if (canListen) {
                    MicButton(listening, level, size = 130.dp) {
                        if (listening) return@MicButton
                        scope.launch {
                            if (!mic.ensureMic()) return@launch
                            g.speaker.stop()
                            val heard = try { g.listener.listen("en-US") } catch (e: ListenException) { emptyList() }
                            tries++
                            if (TextMatch.spokenMatch(w.en, heard, threshold = 0.55f)) {
                                stars++; mood = NikaMood.HAPPY
                                g.speaker.say("Молодец! Great job!")
                                next()
                            } else if (tries >= 2) {
                                mood = NikaMood.HAPPY
                                g.speaker.say("Хорошо! Идём дальше!")
                                next()
                            } else {
                                mood = NikaMood.THINK
                                g.speaker.say("Попробуй ещё раз:")
                                g.speaker.say(w.en, SpeechText.Lang.EN, slow = true)
                            }
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(20.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                BigTile("🔊", Palette.bluish, Modifier.size(90.dp)) { g.speaker.speak(w.en, SpeechText.Lang.EN, slow = true) }
                BigTile("➡️", Palette.pinkish, Modifier.size(90.dp)) { next() }
            }
        }
    }
}

/** «Найди картинку»: Ника называет слово по-английски, нужно нажать на верную картинку. */
@Composable
fun FindGameScreen(vm: AppViewModel, screen: Screen.YoungerGame) {
    val g = LocalContext.current.graph
    val scope = rememberCoroutineScope()
    val talking by g.speaker.speaking.collectAsState()
    val pack = remember(screen) { packFor(g, screen.packId) }
    val all = pack?.words.orEmpty()
    val rounds = 6
    var round by remember { mutableIntStateOf(0) }
    var stars by remember { mutableIntStateOf(0) }
    var mood by remember { mutableStateOf(NikaMood.IDLE) }
    var locked by remember { mutableStateOf(false) }
    var mistakes by remember { mutableIntStateOf(0) }
    val targets = remember(screen) { if (all.size < 3) emptyList() else all.shuffled().let { s -> List(rounds) { s[it % s.size] } } }
    val target = targets.getOrNull(round)
    val options = remember(round) {
        if (target == null) emptyList() else (all.filter { it.en != target.en }.shuffled().take(2) + target).shuffled()
    }
    val shake = remember { Animatable(0f) }
    var shaking by remember { mutableStateOf<String?>(null) }

    suspend fun ask() {
        val t = targets.getOrNull(round) ?: return
        mood = NikaMood.IDLE
        g.speaker.say("Где")
        g.speaker.say("${t.en}?", SpeechText.Lang.EN)
    }
    LaunchedEffect(round) {
        if (round < rounds) ask() else {
            mood = NikaMood.HAPPY
            rewardYounger(g, if (mistakes <= 1) 3 else if (mistakes <= 4) 2 else 1)
            g.speaker.say("Ура! Все картинки найдены! Hooray!")
        }
    }

    GradientScreen {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            TopBar("⭐".repeat(stars), onBack = { vm.back() })
            if (target == null || all.size < 3) {
                Nika(NikaMood.DANCE, Modifier.height(280.dp), talking = talking)
                Stars(if (mistakes <= 1) 3 else if (mistakes <= 4) 2 else 1, size = 60)
                Box(Modifier.padding(24.dp)) { BigTile("🏠", Palette.button, Modifier.size(140.dp)) { vm.back() } }
                return@Column
            }
            Nika(if (talking) NikaMood.TALK else mood, Modifier.height(200.dp).clickable { scope.launch { ask() } }, talking = talking)
            Column(
                Modifier.weight(1f).navigationBarsPadding().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            ) {
                options.forEach { o ->
                    val dx = if (shaking == o.en) shake.value else 0f
                    Box(
                        Modifier.fillMaxWidth().height(120.dp).offset { IntOffset(dx.roundToInt(), 0) }
                            .shadow(6.dp, RoundedCornerShape(26.dp)).clip(RoundedCornerShape(26.dp)).background(Color.White)
                            .clickable(enabled = !locked) {
                                scope.launch {
                                    locked = true
                                    if (o.en == target.en) {
                                        stars++; mood = NikaMood.HAPPY
                                        g.speaker.say("Yes! ${o.en}!", SpeechText.Lang.EN)
                                        g.speaker.say("Молодец!")
                                        delay(300)
                                        round++
                                    } else {
                                        mistakes++; mood = NikaMood.SURPRISED
                                        shaking = o.en
                                        launch {
                                            for (x in listOf(24f, -24f, 16f, -16f, 0f)) shake.animateTo(x, tween(60))
                                            shaking = null
                                        }
                                        g.speaker.say("Это")
                                        g.speaker.say("${o.en}.", SpeechText.Lang.EN)
                                        g.speaker.say("Найди")
                                        g.speaker.say("${target.en}!", SpeechText.Lang.EN)
                                        mood = NikaMood.IDLE
                                    }
                                    locked = false
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) { Text(o.emoji, fontSize = 80.sp) }
                }
            }
        }
    }
}
