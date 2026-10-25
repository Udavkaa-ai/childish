package ru.sovushka.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import ru.sovushka.app.R
import ru.sovushka.app.graph
import ru.sovushka.app.today
import ru.sovushka.core.GrammarBank
import ru.sovushka.core.SpeechText
import ru.sovushka.core.Srs
import ru.sovushka.core.TopicStats
import ru.sovushka.core.ai.DrillGenerator

/** Значок прогресса по теме. */
private fun topicBadge(s: TopicStats?, today: Long): String = when {
    s == null || s.box == 0 -> "🆕"
    s.due <= today -> "🔁"
    s.box >= Srs.LEARNED_BOX -> "⭐"
    else -> "🌱"
}

/** Все пройденные темы 2–4 класса с прогрессом. */
@Composable
fun GrammarScreen(vm: AppViewModel) {
    val g = LocalContext.current.graph
    val data by g.store.data.collectAsState()
    val t = today()
    GradientScreen {
        Column(Modifier.fillMaxSize()) {
            TopBar("📘 Правила", onBack = { vm.back() })
            LazyColumn(
                Modifier.weight(1f).navigationBarsPadding().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    Hint("⭐ — выучено, 🌱 — учим, 🔁 — пора повторить, 🆕 — ещё не тренировали. Темы урока по фото Ника повторяет сама.")
                }
                GrammarBank.topics.groupBy { it.grade }.forEach { (grade, topics) ->
                    item(key = "grade-$grade") {
                        Text("$grade класс", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp))
                    }
                    items(topics, key = { it.id }) { topic ->
                        val st = data.grammar[topic.id]
                        Row(
                            Modifier.fillMaxWidth().glass(RoundedCornerShape(16.dp), Palette.Card)
                                .clickable { vm.go(Screen.Topic(topic.id)) }.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            val badge = topicBadge(st, t)
                            AppIcon(
                                when (badge) { "⭐" -> AppIcons.STAR; "🔁" -> AppIcons.REPEAT; "🌱" -> AppIcons.HEART; else -> AppIcons.SPARKLE },
                                size = 34.dp,
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(topic.title, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                                if (st != null && st.right + st.wrong > 0) {
                                    Text("верно ${st.right} · ошибок ${st.wrong}", color = Palette.Muted, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Тема: правило (Ника читает вслух), примеры и тренировка. */
@Composable
fun TopicScreen(vm: AppViewModel, id: String) {
    val g = LocalContext.current.graph
    val data by g.store.data.collectAsState()
    val talking by g.speaker.speaking.collectAsState()
    val scope = rememberCoroutineScope()
    val topic = GrammarBank.topic(id) ?: return
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

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
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("💬 Примеры", style = MaterialTheme.typography.titleMedium)
                        topic.examples.forEach { e ->
                            Column(Modifier.fillMaxWidth().clickable { g.speaker.speak(e.en, SpeechText.Lang.EN) }) {
                                Text("🔊 ${e.en}", fontWeight = FontWeight.SemiBold)
                                Text(e.ru, color = Palette.Muted, fontSize = 14.sp)
                            }
                        }
                    }
                }
                TOPIC_COMICS[id]?.let { res ->
                    Image(
                        painterResource(res), "Комикс с Никой",
                        Modifier.fillMaxWidth().aspectRatio(1f).glass(RoundedCornerShape(22.dp), Color.White.copy(alpha = 0.8f), rim = 3.dp).padding(4.dp)
                            .clip(RoundedCornerShape(18.dp)),
                    )
                }
                message?.let { Text(it, color = Palette.Muted) }
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            }
            Column(Modifier.navigationBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                BigButton("▶️", "Тренировать", subtitle = "Заданий в теме: ${data.drillsOf(id).size}") {
                    vm.go(Screen.Practice(null, topicId = id))
                }
                BigButton("✨", "Ещё задания от Ники", subtitle = "Нейросеть придумает новые упражнения", brush = Palette.buttonSoft, enabled = !busy) {
                    val llm = g.llm()
                    if (llm == null) {
                        message = "Нейросеть не подключена — попросите родителей открыть настройки."
                        return@BigButton
                    }
                    busy = true; message = "Ника придумывает задания…"
                    scope.launch {
                        message = try {
                            val words = g.store.value.words.filter { it.box < Srs.LEARNED_BOX }.takeLast(40).map { it.en }
                            val fresh = DrillGenerator(llm).forTopic(topic, g.settings.value.olderAge, words)
                            if (fresh.isEmpty()) {
                                "Не получилось придумать задания, попробуй ещё раз."
                            } else {
                                g.store.update { d ->
                                    val known = d.extraDrills[id].orEmpty()
                                    val merged = (known + fresh).distinctBy { it.prompt + it.answer }.takeLast(60)
                                    d.copy(extraDrills = d.extraDrills + (id to merged))
                                }
                                vm.go(Screen.Practice(null, topicId = id, fresh = fresh.size))
                                "Новых заданий: ${fresh.size}"
                            }
                        } catch (e: Exception) {
                            "Ошибка: ${e.message}"
                        } finally {
                            busy = false
                        }
                    }
                }
            }
        }
    }
}

/** Комиксы с Никой к темам (нарисованы в OpenArt по референсу персонажа). */
private val TOPIC_COMICS = mapOf(
    "this_these" to R.drawable.comic_this_these,
)
