package ru.sovushka.app.ui

import android.app.Application
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.remember
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
import ru.sovushka.core.ai.GeneratedMathLesson
import ru.sovushka.core.ai.MathLessonGenerator
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
    data object Comics : Screen
    data class Comic(val id: String) : Screen
    data class Tutor(val lessonId: String? = null, val math: Boolean = false) : Screen
    data object Diary : Screen
    data object MeshLogin : Screen
    data object YoungerHome : Screen
    data class YoungerPack(val packId: String) : Screen
    data class YoungerGame(val packId: String, val find: Boolean, val run: Long = System.nanoTime()) : Screen
    data class ParentGate(val next: Screen) : Screen
    data object Settings : Screen

    /** Выбор предмета после профиля: английский или математика. */
    data class SubjectChoice(val older: Boolean) : Screen

    // Математика (строгий стиль, Ника в очках).
    data object MathHome : Screen, MathScreen
    data object MathTopics : Screen, MathScreen
    data class MathTopicPage(val id: String) : Screen, MathScreen
    /** lessonId — урок по фото, topicId — тема программы, оба null — повторение. */
    data class MathPractice(val lessonId: String? = null, val topicId: String? = null, val run: Long = System.nanoTime()) : Screen, MathScreen
    data object MathCapture : Screen, MathScreen
    data object MathDraft : Screen, MathScreen
    data object MathLessons : Screen, MathScreen
    data class MathLessonPage(val id: String) : Screen, MathScreen
    data object YoungerMath : Screen, MathScreen
    data class YoungerMathGame(val kind: String, val run: Long = System.nanoTime()) : Screen, MathScreen
    data object Abc : Screen
    /** Урок буквы с номером index или повторение (index = -1). */
    data class AbcLesson(val index: Int, val run: Long = System.nanoTime()) : Screen
}

/** Отметка экранов математики. */
interface MathScreen

val Screen.isMath: Boolean get() = this is MathScreen || (this is Screen.Tutor && math)

/** Экраны младшей: она не читает, поэтому там Ника говорит всегда. */
val Screen.isYounger: Boolean
    get() = this is Screen.YoungerHome || this is Screen.YoungerPack || this is Screen.YoungerGame ||
        this is Screen.YoungerMath || this is Screen.YoungerMathGame || this is Screen.Abc || this is Screen.AbcLesson ||
        (this is Screen.SubjectChoice && !older)

/** Где показывать кнопку «сколько говорит Ника». */
val Screen.hasTalkToggle: Boolean
    get() = !isYounger && this !is Screen.Setup && this !is Screen.Settings && this !is Screen.ParentGate

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
    var mathDraft by mutableStateOf<GeneratedMathLesson?>(null)
    var sourceText by mutableStateOf("")
    var job: Job? = null
    val busy: Boolean get() = job?.isActive == true

    fun reset() {
        job?.cancel()
        photos.clear(); note = ""; status = null; error = null; draft = null; mathDraft = null; sourceText = ""
    }
}

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val graph = app.graph
    val stack = mutableStateListOf<Screen>(if (graph.settings.value.setupDone) Screen.Profiles else Screen.Setup)
    val capture = CaptureState()
    val mathCapture = CaptureState()

    val current: Screen get() = stack.last()

    /** Голос и озвучка формул — по предмету открытого экрана. */
    private fun sync() {
        graph.speaker.mathMode = current.isMath
        graph.speaker.youngerMode = current.isYounger
        graph.speaker.forgetTask()
    }

    fun go(s: Screen) { graph.speaker.stop(); stack.add(s); sync() }
    fun replace(s: Screen) { graph.speaker.stop(); stack[stack.lastIndex] = s; sync() }
    fun back(): Boolean {
        graph.speaker.stop()
        if (stack.size <= 1) return false
        stack.removeAt(stack.lastIndex)
        sync()
        return true
    }
    fun home(s: Screen) { graph.speaker.stop(); stack.clear(); stack.add(s); sync() }

    /** Распознать фото страницы учебника математики и попросить нейросеть собрать урок. */
    fun generateMathLesson() {
        val c = mathCapture
        if (c.busy || c.photos.isEmpty()) return
        val llm = graph.llm()
        c.error = null
        c.job = viewModelScope.launch {
            try {
                c.status = "Читаю страницу…"
                val text = c.photos.map { graph.pages.recognize(it) }.joinToString("\n\n")
                c.sourceText = text
                if (llm == null) {
                    c.error = "Нейросеть не подключена. Попроси родителей открыть настройки ⚙️ — а пока потренируйся по темам программы."
                    return@launch
                }
                c.status = "Ника разбирает задачи…"
                c.mathDraft = MathLessonGenerator(llm).fromPage(text, c.note, graph.settings.value.olderAge)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                c.error = e.message ?: e.toString()
            } finally {
                c.status = null
            }
        }
    }

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
        // Плавная смена экранов: вперёд — въезд справа, назад — слева.
        val lastDepth = remember { intArrayOf(vm.stack.size) }
        val forward = vm.stack.size >= lastDepth[0]
        lastDepth[0] = vm.stack.size
        AnimatedContent(
            targetState = vm.current,
            transitionSpec = {
                val dir = if (forward) 1 else -1
                (fadeIn(tween(220)) + slideInHorizontally(tween(260)) { w -> dir * w / 6 }) togetherWith
                    (fadeOut(tween(160)) + slideOutHorizontally(tween(260)) { w -> -dir * w / 10 })
            },
            label = "screens",
        ) { s ->
            // У математики свой стиль: тетрадь в клетку, строгие кнопки и Ника в очках.
            CompositionLocalProvider(
                LocalMathStyle provides s.isMath,
                LocalTalkToggle provides s.hasTalkToggle,
                LocalNikaOutfit provides if (s.isMath) NikaOutfit.STRICT else NikaOutfit.CASUAL,
            ) { ScreenContent(vm, s) }
        }
    }
}

@Composable
private fun ScreenContent(vm: AppViewModel, s: Screen) {
    when (s) {
            Screen.Setup -> SetupScreen(vm)
            Screen.Profiles -> ProfilesScreen(vm)
            Screen.OlderHome -> OlderHomeScreen(vm)
            Screen.Capture -> CaptureScreen(vm)
            Screen.Draft -> DraftScreen(vm)
            Screen.Lessons -> LessonsScreen(vm)
            Screen.Packs -> PacksScreen(vm)
            is Screen.Lesson -> LessonScreen(vm, s.id)
            is Screen.Practice -> PracticeScreen(vm, s)
            is Screen.Tutor -> TutorScreen(vm, s.lessonId, s.math)
            Screen.Diary -> DiaryScreen(vm)
            Screen.Grammar -> GrammarScreen(vm)
            is Screen.Topic -> TopicScreen(vm, s.id)
            Screen.Comics -> ComicsScreen(vm)
            is Screen.Comic -> ComicScreen(vm, s.id)
            Screen.MeshLogin -> MeshLoginScreen(vm)
            Screen.YoungerHome -> YoungerHomeScreen(vm)
            is Screen.YoungerPack -> YoungerPackScreen(vm, s.packId)
            is Screen.YoungerGame -> if (s.find) FindGameScreen(vm, s) else RepeatGameScreen(vm, s)
            is Screen.ParentGate -> ParentGateScreen(vm, s.next)
            Screen.Settings -> SettingsScreen(vm)
            is Screen.SubjectChoice -> SubjectChoiceScreen(vm, s.older)
            Screen.MathHome -> MathHomeScreen(vm)
            Screen.MathTopics -> MathTopicsScreen(vm)
            is Screen.MathTopicPage -> MathTopicScreen(vm, s.id)
            is Screen.MathPractice -> MathPracticeScreen(vm, s)
            Screen.MathCapture -> MathCaptureScreen(vm)
            Screen.MathDraft -> MathDraftScreen(vm)
            Screen.MathLessons -> MathLessonsScreen(vm)
            is Screen.MathLessonPage -> MathLessonScreen(vm, s.id)
            Screen.YoungerMath -> YoungerMathScreen(vm)
            is Screen.YoungerMathGame -> YoungerMathGameScreen(vm, s)
            Screen.Abc -> AbcHomeScreen(vm)
            is Screen.AbcLesson -> AbcLessonScreen(vm, s)
    }
}
