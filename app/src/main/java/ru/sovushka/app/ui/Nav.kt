package ru.sovushka.app.ui

import android.app.Application
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import ru.sovushka.app.graph
import ru.sovushka.core.ai.GeneratedLesson
import ru.sovushka.core.ai.LessonGenerator
import java.io.File

/** Экраны приложения. Навигация — простой стек в ViewModel. */
sealed interface Screen {
    data object Setup : Screen
    data object Profiles : Screen
    data object OlderHome : Screen
    data object Capture : Screen
    data object Draft : Screen
    data object Lessons : Screen
    data object Packs : Screen
    data class Lesson(val id: String) : Screen
    /** lessonId = null и topicId = null — повторение; topicId — тренировка по грамматической теме. */
    /** fresh — начать с заданий, которые нейросеть только что придумала по теме. */
    data class Practice(val lessonId: String?, val run: Long = System.nanoTime(), val topicId: String? = null, val fresh: Int = 0) : Screen
    data object Grammar : Screen
    data class Topic(val id: String) : Screen
    data class Tutor(val lessonId: String? = null) : Screen
    data object Diary : Screen
    data object MeshLogin : Screen
    data object YoungerHome : Screen
    data class YoungerPack(val packId: String) : Screen
    data class YoungerGame(val packId: String, val find: Boolean, val run: Long = System.nanoTime()) : Screen
    data class ParentGate(val next: Screen) : Screen
    data object Settings : Screen
}

/** Выход к микрофону: разрешение запрашивает Activity. */
interface MicGate {
    suspend fun ensureMic(): Boolean
}

val LocalMic = staticCompositionLocalOf<MicGate> { error("MicGate не задан") }

/** Черновик урока: фото страниц → распознанный текст → урок от нейросети → проверка родителем/ребёнком. */
class CaptureState {
    val photos = mutableStateListOf<File>()
    var note by mutableStateOf("")
    var status by mutableStateOf<String?>(null)
    var error by mutableStateOf<String?>(null)
    var draft by mutableStateOf<GeneratedLesson?>(null)
    var sourceText by mutableStateOf("")
    var job: Job? = null
    val busy: Boolean get() = job?.isActive == true

    fun reset() {
        job?.cancel()
        photos.clear(); note = ""; status = null; error = null; draft = null; sourceText = ""
    }
}

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val graph = app.graph
    val stack = mutableStateListOf<Screen>(if (graph.settings.value.setupDone) Screen.Profiles else Screen.Setup)
    val capture = CaptureState()

    val current: Screen get() = stack.last()

    fun go(s: Screen) { graph.speaker.stop(); stack.add(s) }
    fun replace(s: Screen) { graph.speaker.stop(); stack[stack.lastIndex] = s }
    fun back(): Boolean {
        graph.speaker.stop()
        if (stack.size <= 1) return false
        stack.removeAt(stack.lastIndex)
        return true
    }
    fun home(s: Screen) { graph.speaker.stop(); stack.clear(); stack.add(s) }

    /** Распознать фото и попросить нейросеть собрать урок. Работает, даже если уйти с экрана. */
    fun generateLesson() {
        val c = capture
        if (c.busy || c.photos.isEmpty()) return
        val llm = graph.llm()
        c.error = null
        c.job = viewModelScope.launch {
            try {
                c.status = "Читаю страницу…"
                val text = c.photos.map { graph.pages.recognize(it) }.joinToString("\n\n")
                c.sourceText = text
                if (llm == null) {
                    c.error = "Нейросеть не подключена. Попроси родителей открыть настройки ⚙️ — или добавь слова вручную."
                    c.draft = GeneratedLesson(title = c.note.ifBlank { "Новый урок" })
                    return@launch
                }
                c.status = "Ника готовит урок…"
                val age = graph.settings.value.olderAge
                c.draft = LessonGenerator(llm).fromPage(text, c.note, age)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                c.error = e.message ?: e.toString()
            } finally {
                c.status = null
            }
        }
    }
}

@Composable
fun AppRoot(mic: MicGate) {
    val vm: AppViewModel = viewModel()
    CompositionLocalProvider(LocalMic provides mic) {
        BackHandler(enabled = vm.stack.size > 1) { vm.back() }
        when (val s = vm.current) {
            Screen.Setup -> SetupScreen(vm)
            Screen.Profiles -> ProfilesScreen(vm)
            Screen.OlderHome -> OlderHomeScreen(vm)
            Screen.Capture -> CaptureScreen(vm)
            Screen.Draft -> DraftScreen(vm)
            Screen.Lessons -> LessonsScreen(vm)
            Screen.Packs -> PacksScreen(vm)
            is Screen.Lesson -> LessonScreen(vm, s.id)
            is Screen.Practice -> PracticeScreen(vm, s)
            is Screen.Tutor -> TutorScreen(vm, s.lessonId)
            Screen.Diary -> DiaryScreen(vm)
            Screen.Grammar -> GrammarScreen(vm)
            is Screen.Topic -> TopicScreen(vm, s.id)
            Screen.MeshLogin -> MeshLoginScreen(vm)
            Screen.YoungerHome -> YoungerHomeScreen(vm)
            is Screen.YoungerPack -> YoungerPackScreen(vm, s.packId)
            is Screen.YoungerGame -> if (s.find) FindGameScreen(vm, s) else RepeatGameScreen(vm, s)
            is Screen.ParentGate -> ParentGateScreen(vm, s.next)
            Screen.Settings -> SettingsScreen(vm)
        }
    }
}
