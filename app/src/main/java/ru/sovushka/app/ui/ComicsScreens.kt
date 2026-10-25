package ru.sovushka.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.sovushka.app.R
import ru.sovushka.app.graph
import ru.sovushka.core.Comic
import ru.sovushka.core.ComicBook
import ru.sovushka.core.GrammarBank
import ru.sovushka.core.SpeechText

/** Картинки комиксов: страница целиком и обложка (верхний ряд кадров) для списка. */
private fun comicPage(id: String): Int = when (id) {
    "family" -> R.drawable.comic_family
    "this_these" -> R.drawable.comic_this_these
    "pet" -> R.drawable.comic_pet
    "can" -> R.drawable.comic_can
    "plurals" -> R.drawable.comic_plurals
    "morning" -> R.drawable.comic_morning
    "room" -> R.drawable.comic_room
    "now" -> R.drawable.comic_now
    "cat" -> R.drawable.comic_cat
    "sea" -> R.drawable.comic_sea
    else -> R.drawable.comic_zoo
}

private fun comicCover(id: String): Int = when (id) {
    "family" -> R.drawable.cover_family
    "this_these" -> R.drawable.cover_this_these
    "pet" -> R.drawable.cover_pet
    "can" -> R.drawable.cover_can
    "plurals" -> R.drawable.cover_plurals
    "morning" -> R.drawable.cover_morning
    "room" -> R.drawable.cover_room
    "now" -> R.drawable.cover_now
    "cat" -> R.drawable.cover_cat
    "sea" -> R.drawable.cover_sea
    else -> R.drawable.cover_zoo
}

/** Пропорции страницы: первый комикс — квадрат 2×2, остальные — 2×3 кадра. */
private fun pageRatio(id: String): Float = if (id == "this_these") 1f else 1000f / 1490f

/** Обложка комикса: картинка, название и тема. wide — во всю ширину (на экране правила). */
@Composable
fun ComicCard(c: Comic, read: Boolean, wide: Boolean = false, onClick: () -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    Column(
        Modifier.fillMaxWidth().glass(shape, Color.White.copy(alpha = 0.7f), rim = 2.dp).clickable(onClick = onClick).padding(8.dp),
    ) {
        Box {
            Image(
                painterResource(comicCover(c.id)), c.title,
                Modifier.fillMaxWidth().aspectRatio(if (wide) 2f else 1f).clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop,
                // Обложка — верхний ряд кадров; в узкой карточке виден первый кадр.
                alignment = Alignment.CenterStart,
            )
            if (!read) {
                Text(
                    "Новый", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black,
                    modifier = Modifier.align(Alignment.TopEnd).padding(6.dp)
                        .glass(RoundedCornerShape(50), Palette.Pink.copy(alpha = 0.85f)).padding(horizontal = 10.dp, vertical = 3.dp),
                )
            }
        }
        Row(Modifier.padding(top = 8.dp, start = 4.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            if (wide) { AppIcon(AppIcons.GALLERY, size = 28.dp); Spacer(Modifier.width(8.dp)) }
            Column {
                Text(if (wide) "Комикс: ${c.title}" else c.title, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = Palette.Ink)
                Text(GrammarBank.topic(c.topic)?.title.orEmpty(), fontSize = 12.sp, color = Palette.Muted, maxLines = 2)
            }
        }
    }
}

/** Раздел «Комиксы с Никой»: обложки в две колонки. */
@Composable
fun ComicsScreen(vm: AppViewModel) {
    val g = LocalContext.current.graph
    val data by g.store.data.collectAsState()
    val talking by g.speaker.speaking.collectAsState()
    LaunchedEffect(Unit) { g.speaker.say("Выбирай комикс — почитаем вместе!") }
    GradientScreen {
        Column(Modifier.fillMaxSize()) {
            TopBar("Комиксы с Никой", onBack = { vm.back() })
            LazyVerticalGrid(
                GridCells.Fixed(2),
                Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                    NikaSays("Это мои истории! Нажми на комикс — я прочитаю его по-английски и переведу.", NikaMood.WAVE, talking, nikaHeight = 120.dp)
                }
                items(ComicBook.comics, key = { it.id }) { c ->
                    ComicCard(c, read = c.id in data.comicsRead) { vm.go(Screen.Comic(c.id)) }
                }
            }
        }
    }
}

/** Комикс: страница (нажми — откроется на весь экран), чтение вместе с Никой и реплики с переводом. */
@Composable
fun ComicScreen(vm: AppViewModel, id: String) {
    val g = LocalContext.current.graph
    val c = ComicBook.byId(id) ?: return
    val scope = rememberCoroutineScope()
    val talking by g.speaker.speaking.collectAsState()
    var current by remember { mutableIntStateOf(-1) }
    var reading by remember { mutableStateOf<Job?>(null) }
    var zoom by remember { mutableStateOf(false) }
    LaunchedEffect(id) { g.store.update { it.copy(comicsRead = it.comicsRead + id) } }

    fun readAll() {
        reading?.cancel()
        val job = scope.launch {
            try {
                c.lines.forEachIndexed { i, line ->
                    current = i
                    g.speaker.say(line.en, SpeechText.Lang.EN)
                    delay(250)
                    g.speaker.say(line.ru, SpeechText.Lang.RU)
                    delay(450)
                }
            } finally {
                current = -1
            }
        }
        reading = job
        job.invokeOnCompletion { if (reading === job) reading = null }
    }

    GradientScreen {
        Column(Modifier.fillMaxSize()) {
            TopBar(c.title, onBack = { reading?.cancel(); vm.back() })
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Image(
                    painterResource(comicPage(id)), c.title,
                    Modifier.fillMaxWidth().aspectRatio(pageRatio(id))
                        .glass(RoundedCornerShape(22.dp), Color.White.copy(alpha = 0.85f), rim = 3.dp).padding(4.dp)
                        .clip(RoundedCornerShape(18.dp)).clickable { zoom = true },
                )
                Text("Нажми на комикс, чтобы рассмотреть поближе", color = Palette.Muted, fontSize = 13.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                if (reading == null) {
                    BigButton("🔊", "Читаем вместе с Никой", subtitle = "По-английски и с переводом", brush = Palette.pinkish) { readAll() }
                } else {
                    BigButton("⏸", "Стоп", brush = Palette.bluish) { reading?.cancel(); g.speaker.stop() }
                }
                c.lines.forEachIndexed { i, line ->
                    val active = i == current
                    Row(
                        Modifier.fillMaxWidth()
                            .glass(RoundedCornerShape(18.dp), if (active) Palette.PinkLight.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.7f), rim = 2.dp)
                            .clickable { reading?.cancel(); scope.launch { current = i; g.speaker.say(line.en, SpeechText.Lang.EN); current = -1 } }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier.size(32.dp).clip(CircleShape).background(if (active) Palette.Pink else Palette.Lilac),
                            contentAlignment = Alignment.Center,
                        ) { Text("${i + 1}", color = Color.White, fontWeight = FontWeight.Black) }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(line.en, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, color = Palette.Ink)
                            Text(line.ru, fontSize = 15.sp, color = Palette.Muted)
                        }
                        AppIcon(AppIcons.SPEAKER, size = 28.dp)
                    }
                }
                GrammarBank.topic(c.topic)?.let { t ->
                    BigButton("▶️", "Потренировать правило", subtitle = t.title, brush = Palette.button) {
                        reading?.cancel(); vm.go(Screen.Practice(null, topicId = t.id))
                    }
                    BigButton("📘", "Правило простыми словами", subtitle = "Ника объяснит тему комикса", brush = Palette.buttonSoft) {
                        reading?.cancel(); vm.go(Screen.Topic(t.id))
                    }
                }
                Spacer(Modifier.navigationBarsPadding())
            }
        }
        if (talking && current >= 0) {
            // Ника «читает» в уголке, пока идёт озвучка.
            Nika(NikaMood.TALK, Modifier.align(Alignment.BottomEnd).padding(8.dp).size(70.dp, 112.dp), talking = true)
        }
    }
    if (zoom) ZoomDialog(comicPage(id), pageRatio(id)) { zoom = false }
}

/** Комикс на весь экран: щипок — увеличить, двойное касание — вернуть, крестик — закрыть. */
@Composable
private fun ZoomDialog(res: Int, ratio: Float, onClose: () -> Unit) {
    Dialog(onClose, DialogProperties(usePlatformDefaultWidth = false)) {
        var scale by remember { mutableFloatStateOf(1f) }
        var offset by remember { mutableStateOf(Offset.Zero) }
        Box(Modifier.fillMaxSize().background(Color(0xF2140E24))) {
            Image(
                painterResource(res), null,
                Modifier.fillMaxWidth().aspectRatio(ratio).align(Alignment.Center)
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoomBy, _ ->
                            scale = (scale * zoomBy).coerceIn(1f, 4f)
                            offset = if (scale == 1f) Offset.Zero else offset + pan
                        }
                    }
                    .pointerInput(Unit) { detectTapGestures(onDoubleTap = { scale = 1f; offset = Offset.Zero }) }
                    .graphicsLayer { scaleX = scale; scaleY = scale; translationX = offset.x; translationY = offset.y },
            )
            GlassButton(
                onClose, Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(12.dp),
                brush = GlassWhite, shape = CircleShape, depth = 4.dp, contentPadding = PaddingValues(10.dp),
            ) { AppIcon(AppIcons.CROSS, size = 28.dp) }
        }
    }
}
