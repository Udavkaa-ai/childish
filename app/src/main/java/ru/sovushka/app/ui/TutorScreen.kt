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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import ru.sovushka.app.graph
import ru.sovushka.app.voice.Talk
import ru.sovushka.app.voice.ListenException
import ru.sovushka.core.GrammarBank
import ru.sovushka.core.Srs
import ru.sovushka.core.ai.Tutor
import ru.sovushka.core.math.MathBank
import ru.sovushka.core.mesh.DiaryInsights
import java.time.LocalDate

private data class ChatLine(val fromNika: Boolean, val text: String)

/** Разговор с Никой голосом или текстом. */
@Composable
fun TutorScreen(vm: AppViewModel, lessonId: String?, math: Boolean = false) {
    val g = LocalContext.current.graph
    val mic = LocalMic.current
    val scope = rememberCoroutineScope()
    val talking by g.speaker.speaking.collectAsState()
    val listening by g.listener.listening.collectAsState()
    val level by g.listener.level.collectAsState()
    val prefs = g.settings.value
    val llm = remember { g.llm() }
    val tutor = remember {
        if (math) return@remember llm?.let { mathTutor(it, g.store.value, lessonId, prefs.olderName, prefs.olderAge) }
        llm?.let {
            val d = g.store.value
            val lesson = lessonId?.let { id -> d.lessons.firstOrNull { it.id == id } }
            val words = (if (lesson != null) d.wordsOf(lesson.id) else d.words.filter { it.box < Srs.LEARNED_BOX }.takeLast(40))
                .map { "${it.en} (${it.ru})" }
            val ctx = buildString {
                if (lesson != null) {
                    append(lesson.title).append(". ")
                    if (lesson.grammar.isNotBlank()) append("Правило: ").append(lesson.grammar).append(' ')
                }
                append(DiaryInsights.tutorContext(d.diary, LocalDate.now().toString()))
                val weak = d.grammar.filter { it.value.wrong > it.value.right }.keys.mapNotNull { GrammarBank.topic(it)?.title }
                if (weak.isNotEmpty()) append(" Темы, где она чаще ошибается: ").append(weak.joinToString("; ")).append('.')
            }
            Tutor(it, prefs.olderName.ifBlank { "подружка" }, prefs.olderAge, words, ctx)
        }
    }
    val lines = remember { mutableStateListOf<ChatLine>() }
    var thinking by remember { mutableStateOf(false) }
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    fun send(text: String) {
        if (text.isBlank() || tutor == null || thinking) return
        lines.add(ChatLine(false, text))
        thinking = true
        scope.launch {
            val answer = try {
                tutor.ask(text)
            } catch (e: Exception) {
                "Ой, у меня не получилось ответить: ${e.message}"
            }
            thinking = false
            lines.add(ChatLine(true, answer))
            g.speaker.say(answer, kind = Talk.TASK)
        }
    }

    fun listen(lang: String) = scope.launch {
        if (listening || thinking) return@launch
        if (!mic.ensureMic()) return@launch
        g.speaker.stop()
        try {
            val heard = g.listener.listen(lang).firstOrNull().orEmpty()
            if (heard.isNotBlank()) send(heard)
        } catch (e: ListenException) {
            lines.add(ChatLine(true, e.message.orEmpty()))
        }
    }

    LaunchedEffect(Unit) {
        val hello = if (tutor == null) {
            "Чтобы я могла разговаривать, попроси родителей подключить нейросеть в настройках."
        } else if (lessonId != null) {
            "Давай поговорим про этот урок! Спроси меня что-нибудь или скажи «проверь меня»."
        } else if (math) {
            "Привет! Спроси, как решить пример или задачу, или скажи «проверь меня» — дам пример."
        } else {
            "Привет! Спроси меня, как сказать что-нибудь по-английски, или скажи «проверь меня»."
        }
        lines.add(ChatLine(true, hello))
        g.speaker.say(hello)
    }
    LaunchedEffect(lines.size, thinking) { if (lines.isNotEmpty()) listState.animateScrollToItem(lines.size) }

    val mood = when {
        thinking -> NikaMood.THINK
        listening -> NikaMood.LISTEN
        talking -> NikaMood.TALK
        else -> NikaMood.IDLE
    }
    GradientScreen {
        Column(Modifier.fillMaxSize().imePadding()) {
            TopBar("Ника", onBack = { vm.back() })
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Nika(mood, Modifier.height(150.dp), talking = talking)
            }
            LazyColumn(Modifier.weight(1f).padding(horizontal = 12.dp), state = listState, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(lines) { l ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (l.fromNika) Arrangement.Start else Arrangement.End) {
                        Box(
                            Modifier.widthIn(max = 300.dp).clip(RoundedCornerShape(18.dp))
                                .background(if (l.fromNika) Color.White else Palette.PinkLight)
                                .clickable(enabled = l.fromNika) { g.speaker.speak(l.text) }
                                .padding(12.dp),
                        ) { Text(if (l.fromNika) "🔊 ${l.text}" else l.text, fontSize = 17.sp) }
                    }
                }
                if (thinking) item { Text("Ника думает…", color = Palette.Muted, modifier = Modifier.padding(8.dp)) }
            }
            if (tutor != null) {
                Row(
                    Modifier.fillMaxWidth().navigationBarsPadding().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        MicButton(listening, level, size = 64.dp) { listen("ru-RU") }
                        Text("RU", fontSize = 12.sp, color = Palette.Muted)
                    }
                    if (!math) Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        MicButton(listening, level, size = 64.dp, label = "🇬🇧") { listen("en-US") }
                        Text("EN", fontSize = 12.sp, color = Palette.Muted)
                    }
                    OutlinedTextField(
                        input, { input = it }, modifier = Modifier.weight(1f), placeholder = { Text("или напиши…") }, maxLines = 3,
                    )
                    PillButton("➤") { send(input.trim()); input = "" }
                }
            } else {
                Box(Modifier.navigationBarsPadding().padding(16.dp)) {
                    BigButton("⚙️", "Настройки (для родителей)") { vm.go(Screen.ParentGate(Screen.Settings)) }
                }
            }
            Spacer(Modifier.width(1.dp))
        }
    }
}

/** Ника-помощница по математике: знает урок, темы, где бывают ошибки, и школьные оценки. */
private fun mathTutor(llm: ru.sovushka.core.ai.LlmClient, d: ru.sovushka.core.AppData, lessonId: String?, name: String, age: Int): Tutor {
    val lesson = lessonId?.let { id -> d.mathLessons.firstOrNull { it.id == id } }
    val topics = (lesson?.topics.orEmpty() + d.mathTopics.keys).distinct().mapNotNull { MathBank.topic(it)?.title }
    val ctx = buildString {
        if (lesson != null) {
            append(lesson.title).append(". ")
            if (lesson.rule.isNotBlank()) append("Правило: ").append(lesson.rule).append(' ')
            lesson.problems.take(5).joinTo(this, prefix = "Задачи урока: ", separator = "; ", postfix = ". ") { "${it.prompt} (ответ ${it.answer})" }
        }
        append(DiaryInsights.mathTutorContext(d.diary, LocalDate.now().toString()))
        val weak = d.mathTopics.filter { it.value.wrong > it.value.right }.keys.mapNotNull { MathBank.topic(it)?.title }
        if (weak.isNotEmpty()) append(" Темы, где она чаще ошибается: ").append(weak.joinToString("; ")).append('.')
    }
    return Tutor(llm, name.ifBlank { "подружка" }, age, topics, ctx, math = true)
}
