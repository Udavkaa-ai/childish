package ru.sovushka.app.ui

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import kotlinx.coroutines.launch
import ru.sovushka.app.graph
import ru.sovushka.core.Builtin
import ru.sovushka.core.GrammarBank
import ru.sovushka.core.Lesson
import ru.sovushka.core.Sentence
import ru.sovushka.core.SpeechText
import ru.sovushka.core.Srs
import ru.sovushka.core.Word
import ru.sovushka.core.ai.GeneratedLesson
import ru.sovushka.core.ai.GeneratedWord
import java.io.File
import java.util.UUID

@Composable
fun PhotoThumb(path: String, size: Int = 96, onClick: (() -> Unit)? = null) {
    val g = LocalContext.current.graph
    val bmp by produceState<Bitmap?>(null, path) { value = g.pages.thumbnail(path) }
    Box(
        Modifier.size(size.dp).clip(RoundedCornerShape(14.dp)).background(Palette.PinkLight)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        bmp?.let { Image(it.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) } ?: Text("📄", fontSize = 30.sp)
    }
}

/** Фото страниц учебника → урок. */
@Composable
fun CaptureScreen(vm: AppViewModel) {
    val ctx = LocalContext.current
    val g = ctx.graph
    val c = vm.capture
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

    LaunchedEffect(c.draft) { if (c.draft != null) vm.replace(Screen.Draft) }
    LaunchedEffect(Unit) {
        if (c.photos.isEmpty()) g.speaker.say("Сфотографируй страницу учебника. Можно несколько страниц.")
    }

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
                NikaSays(c.status ?: c.error ?: "Сфотографируй страницу учебника. Можно несколько страниц.", mood, talking, nikaHeight = 150.dp)
                if (c.photos.isNotEmpty()) {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        c.photos.forEach { f -> PhotoThumb(f.path) { if (!c.busy) { c.photos.remove(f); f.delete() } } }
                    }
                    Hint("Нажми на фото, чтобы убрать его.")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PillButton("📷 Камера", Modifier.weight(1f), enabled = !c.busy) {
                        val dir = File(ctx.cacheDir, "camera").apply { mkdirs() }
                        val file = File(dir, "page-${System.currentTimeMillis()}.jpg")
                        val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".files", file)
                        pendingUri = uri
                        takePicture.launch(uri)
                    }
                    PillButton("🖼 Галерея", Modifier.weight(1f), brush = Palette.bluish, enabled = !c.busy) {
                        pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                }
                OutlinedTextField(
                    c.note, { c.note = it }, label = { Text("Страница или тема (необязательно)") },
                    placeholder = { Text("например: стр. 45, Module 3") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    enabled = !c.busy,
                )
                if (c.busy) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                } else {
                    BigButton("✨", "Сделать урок", enabled = c.photos.isNotEmpty(), subtitle = "Ника прочитает страницу и подготовит задания") {
                        vm.generateLesson()
                    }
                }
                TextButton(onClick = { c.draft = GeneratedLesson(title = c.note.ifBlank { "Мои слова" }) }) {
                    Text("✍️ Добавить слова вручную, без фото")
                }
            }
        }
    }
}

/** Проверка урока перед сохранением: можно убрать лишнее и поправить перевод. */
@Composable
fun DraftScreen(vm: AppViewModel) {
    val g = LocalContext.current.graph
    val c = vm.capture
    val draft = c.draft
    val scope = rememberCoroutineScope()
    if (draft == null) {
        LaunchedEffect(Unit) { vm.replace(Screen.Capture) }
        return
    }
    var title by remember { mutableStateOf(draft.title) }
    var words by remember { mutableStateOf(draft.words) }
    var editing by remember { mutableStateOf<Int?>(null) }

    GradientScreen {
        Column(Modifier.fillMaxSize()) {
            TopBar("Проверь урок", onBack = { c.draft = null; vm.replace(Screen.Capture) })
            LazyColumn(
                Modifier.weight(1f).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    Column {
                        c.error?.let { Text(it, color = Palette.Bad, modifier = Modifier.padding(vertical = 6.dp)) }
                        OutlinedTextField(title, { title = it }, label = { Text("Название") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        if (draft.grammar.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            CardBox { Text("📝 ${draft.grammar}", fontSize = 15.sp) }
                        }
                        Spacer(Modifier.height(4.dp))
                        Hint("Нажми на слово, чтобы исправить. ✕ — убрать.")
                    }
                }
                items(words.size) { i ->
                    val w = words[i]
                    Row(
                        Modifier.fillMaxWidth().glass(RoundedCornerShape(14.dp), Palette.Card).clickable { editing = i }.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (w.emoji.isNotBlank()) Sticker(w.emoji, 44.dp, seed = w.en.hashCode()) else AppIcon(AppIcons.PENCIL, size = 38.dp)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(w.en, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text(w.ru, color = Palette.Muted)
                        }
                        Text("🔊", fontSize = 22.sp, modifier = Modifier.clickable { g.speaker.speak(w.en, SpeechText.Lang.EN) }.padding(6.dp))
                        Text("✕", fontSize = 22.sp, color = Palette.Bad, modifier = Modifier.clickable { words = words.filterIndexed { j, _ -> j != i } }.padding(6.dp))
                    }
                }
                item {
                    TextButton(onClick = { words = words + GeneratedWord(); editing = words.size - 1 }) { Text("➕ Добавить слово") }
                }
            }
            Box(Modifier.navigationBarsPadding().padding(16.dp)) {
                BigButton("💾", "Сохранить и учить", enabled = words.any { it.en.isNotBlank() && it.ru.isNotBlank() }) {
                    scope.launch {
                        val id = UUID.randomUUID().toString()
                        val lesson = Lesson(
                            id = id,
                            title = title.ifBlank { "Новый урок" },
                            createdAt = System.currentTimeMillis(),
                            note = c.note,
                            grammar = draft.grammar,
                            sentences = draft.sentences.map { Sentence(it.en, it.ru) },
                            drills = draft.validDrills(),
                            topics = draft.topics,
                            sourceText = c.sourceText,
                            photos = c.photos.map { it.path },
                        )
                        val newWords = words.filter { it.en.isNotBlank() && it.ru.isNotBlank() }.map {
                            Word(UUID.randomUUID().toString(), id, it.en.trim(), it.ru.trim(), it.tr, it.emoji, it.example, it.exampleRu)
                        }
                        g.store.update { d -> d.copy(lessons = listOf(lesson) + d.lessons, words = d.words + newWords) }
                        c.photos.clear(); c.reset()
                        vm.replace(Screen.Lesson(id))
                    }
                }
            }
        }
    }

    editing?.let { i ->
        val w = words.getOrNull(i) ?: return@let
        var en by remember(i) { mutableStateOf(w.en) }
        var ru by remember(i) { mutableStateOf(w.ru) }
        var emoji by remember(i) { mutableStateOf(w.emoji) }
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text("Слово") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(en, { en = it }, label = { Text("По-английски") }, singleLine = true)
                    OutlinedTextField(ru, { ru = it }, label = { Text("Перевод") }, singleLine = true)
                    OutlinedTextField(emoji, { emoji = it.take(8) }, label = { Text("Картинка-эмодзи") }, singleLine = true)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    words = words.mapIndexed { j, x -> if (j == i) x.copy(en = en.trim(), ru = ru.trim(), emoji = emoji.trim()) else x }
                    editing = null
                }) { Text("Готово") }
            },
            dismissButton = { TextButton(onClick = { editing = null }) { Text("Отмена") } },
        )
    }
}

@Composable
fun LessonsScreen(vm: AppViewModel) {
    val g = LocalContext.current.graph
    val data by g.store.data.collectAsState()
    GradientScreen {
        Column(Modifier.fillMaxSize()) {
            TopBar("Мои уроки", onBack = { vm.back() })
            if (data.lessons.isEmpty()) {
                NikaSays("Пока уроков нет. Сфотографируй страницу учебника или выбери готовую тему!", NikaMood.IDLE, false, Modifier.padding(16.dp))
            }
            LazyColumn(Modifier.weight(1f).navigationBarsPadding().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(data.lessons, key = { it.id }) { l ->
                    val ws = data.wordsOf(l.id)
                    val mastery = Srs.mastery(ws)
                    Row(
                        Modifier.fillMaxWidth().glass(RoundedCornerShape(18.dp), Palette.Card).clickable { vm.go(Screen.Lesson(l.id)) }.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (l.photos.isNotEmpty()) PhotoThumb(l.photos.first(), 56) else ws.firstOrNull()?.emoji?.ifBlank { null }?.let { Sticker(it, 56.dp) } ?: AppIcon(AppIcons.BOOKS, size = 52.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(l.title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text("${ws.size} слов · ${l.note}".trimEnd(' ', '·'), color = Palette.Muted, fontSize = 14.sp)
                            LinearProgressIndicator(progress = { mastery }, modifier = Modifier.fillMaxWidth().padding(top = 6.dp), color = Palette.Pink, trackColor = Palette.PinkLight)
                        }
                        Spacer(Modifier.width(8.dp))
                        Text("⭐".repeat(l.bestStars).ifEmpty { "▶️" }, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun LessonScreen(vm: AppViewModel, id: String) {
    val g = LocalContext.current.graph
    val data by g.store.data.collectAsState()
    val scope = rememberCoroutineScope()
    val lesson = data.lessons.firstOrNull { it.id == id }
    if (lesson == null) {
        LaunchedEffect(Unit) { vm.back() }
        return
    }
    val words = data.wordsOf(id)
    var confirmDelete by remember { mutableStateOf(false) }
    var photo by remember { mutableStateOf<String?>(null) }

    GradientScreen {
        Column(Modifier.fillMaxSize()) {
            TopBar(lesson.title, onBack = { vm.back() }) {
                Text("🗑", fontSize = 22.sp, modifier = Modifier.clickable { confirmDelete = true }.padding(10.dp))
            }
            LazyColumn(Modifier.weight(1f).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (lesson.photos.isNotEmpty()) item {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        lesson.photos.forEach { p -> PhotoThumb(p, 80) { photo = p } }
                    }
                }
                if (lesson.grammar.isNotBlank()) item {
                    CardBox {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("📝 Правило", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                                SoundButton({ g.speaker.speak(lesson.grammar) })
                            }
                            Text(lesson.grammar, fontSize = 16.sp)
                        }
                    }
                }
                if (lesson.drills.isNotEmpty() || lesson.topics.isNotEmpty()) item {
                    CardBox {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("🧩 Упражнений на правила: ${lesson.drills.size}", fontWeight = FontWeight.SemiBold)
                            lesson.topics.mapNotNull { GrammarBank.topic(it) }.forEach { tp ->
                                Text("🔁 Повторим: ${tp.title}", fontSize = 15.sp, color = Palette.Blue,
                                    modifier = Modifier.clickable { vm.go(Screen.Topic(tp.id)) })
                            }
                        }
                    }
                }
                items(words, key = { it.id }) { w ->
                    Row(
                        Modifier.fillMaxWidth().glass(RoundedCornerShape(14.dp), Palette.Card)
                            .clickable { g.speaker.speak(w.en, SpeechText.Lang.EN) }.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (w.emoji.isNotBlank()) Sticker(w.emoji, 46.dp, seed = w.en.hashCode()) else AppIcon(AppIcons.PENCIL, size = 40.dp)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("${w.en}  ${w.tr}", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text(w.ru, color = Palette.Muted)
                        }
                        Text(if (w.box >= Srs.LEARNED_BOX) "✅" else "🌱".takeIf { w.box > 0 } ?: "🆕", fontSize = 20.sp)
                    }
                }
                if (lesson.sentences.isNotEmpty()) item {
                    CardBox {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("💬 Фразы", style = MaterialTheme.typography.titleMedium)
                            lesson.sentences.forEach { s ->
                                Column(Modifier.fillMaxWidth().clickable { g.speaker.speak(s.en, SpeechText.Lang.EN) }) {
                                    Text("🔊 ${s.en}", fontWeight = FontWeight.SemiBold)
                                    Text(s.ru, color = Palette.Muted, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
            }
            Column(Modifier.navigationBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                BigButton("▶️", "Учить", subtitle = "Слушать, выбирать, писать и говорить", enabled = words.isNotEmpty()) { vm.go(Screen.Practice(id)) }
                BigButton("💬", "Спросить Нику про урок", brush = Palette.buttonSoft) { vm.go(Screen.Tutor(id)) }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Удалить урок?") },
            text = { Text("Слова и прогресс по этому уроку тоже удалятся.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    scope.launch {
                        lesson.photos.forEach { File(it).delete() }
                        g.store.update { d -> d.copy(lessons = d.lessons.filter { it.id != id }, words = d.words.filter { it.lessonId != id }) }
                        vm.back()
                    }
                }) { Text("Удалить", color = Palette.Bad) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Отмена") } },
        )
    }
    photo?.let { p ->
        AlertDialog(
            onDismissRequest = { photo = null },
            confirmButton = { TextButton(onClick = { photo = null }) { Text("Закрыть") } },
            text = { FullPhoto(p) },
        )
    }
}

@Composable
private fun FullPhoto(path: String) {
    val g = LocalContext.current.graph
    val bmp by produceState<Bitmap?>(null, path) { value = g.pages.thumbnail(path, 1200) }
    bmp?.let { Image(it.asImageBitmap(), null, Modifier.fillMaxWidth(), contentScale = ContentScale.FillWidth) } ?: CircularProgressIndicator()
}

/** Готовые темы: встроенные наборы слов с картинками превращаются в уроки. */
@Composable
fun PacksScreen(vm: AppViewModel) {
    val g = LocalContext.current.graph
    val data by g.store.data.collectAsState()
    val scope = rememberCoroutineScope()
    GradientScreen {
        Column(Modifier.fillMaxSize()) {
            TopBar("Готовые темы", onBack = { vm.back() })
            LazyColumn(Modifier.weight(1f).navigationBarsPadding().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(Builtin.packs, key = { it.id }) { p ->
                    val lessonId = "pack-${p.id}"
                    val exists = data.lessons.any { it.id == lessonId }
                    BigButton(p.emoji, p.title, subtitle = p.words.take(4).joinToString(", ") { it.en } + "…", brush = if (exists) Palette.bluish else Palette.button) {
                        scope.launch {
                            if (!exists) {
                                val lesson = Lesson(lessonId, p.title, System.currentTimeMillis(), note = "Готовая тема")
                                val words = p.words.map { Word(UUID.randomUUID().toString(), lessonId, it.en, it.ru, emoji = it.emoji) }
                                g.store.update { d -> d.copy(lessons = listOf(lesson) + d.lessons, words = d.words + words) }
                            }
                            vm.go(Screen.Lesson(lessonId))
                        }
                    }
                }
            }
        }
    }
}
