package ru.sovushka.core.ai

import kotlinx.serialization.Serializable
import ru.sovushka.core.math.MathBank
import ru.sovushka.core.math.MathCheck
import ru.sovushka.core.math.MathEval
import ru.sovushka.core.math.MathExample
import ru.sovushka.core.math.MathProblem

/** Урок математики, который нейросеть собрала из страницы учебника (ещё не сохранён). */
@Serializable
data class GeneratedMathLesson(
    val title: String = "",
    val rule: String = "",
    val examples: List<GeneratedExample> = emptyList(),
    val problems: List<GeneratedProblem> = emptyList(),
    val topics: List<String> = emptyList(),
) {
    /** Годные задачи: с ответом и без арифметических ошибок. */
    fun validProblems(): List<MathProblem> = problems.mapNotNull { it.toProblem() }.distinctBy { it.prompt }

    fun validExamples(): List<MathExample> = examples.map { MathExample(it.problem.trim(), it.solution.trim()) }
        .filter { it.problem.isNotEmpty() && it.solution.isNotEmpty() }
}

@Serializable
data class GeneratedExample(val problem: String = "", val solution: String = "")

/** Задача от нейросети: все поля необязательные, проверяем и чистим сами. */
@Serializable
data class GeneratedProblem(
    val kind: String = "",
    val prompt: String = "",
    val answer: String = "",
    val unit: String = "",
    val options: List<String> = emptyList(),
    val explain: String = "",
    val topic: String = "",
) {
    fun toProblem(): MathProblem? {
        val k = if (kind.trim().lowercase() == MathProblem.CHOOSE || options.size >= 2) MathProblem.CHOOSE else MathProblem.INPUT
        val p = MathProblem(
            kind = k,
            prompt = prompt.trim()
                .replace(Regex("\\s*\\*\\s*"), " × ")
                .replace(Regex("(?<=\\d)\\s*[xX]\\s*(?=\\d)"), " × ")
                .replace(Regex("_{2,}|\\.\\.\\.+|…"), "□"),
            answer = answer.trim().trimEnd('.'),
            unit = unit.trim(),
            options = if (k == MathProblem.CHOOSE) options.map { it.trim() }.filter { it.isNotEmpty() }.distinct().take(4) else emptyList(),
            explain = explain.trim(),
            topic = topic.trim().takeIf { MathBank.topic(it) != null }.orEmpty(),
        )
        if (!p.isValid()) return null
        // Ответ на ввод — число (или «число ост. число»): иначе ребёнок не сможет набрать его на клавиатуре.
        if (k == MathProblem.INPUT && !MathCheck.normalize(p.answer).matches(Regex("-?\\d+([.]\\d+)?(r\\d+)?|\\d+/\\d+"))) return null
        // Нейросети ошибаются в счёте: всё, что можно проверить, проверяем.
        if (MathEval.verify(p) == false) return null
        return p
    }
}

class MathLessonGenerator(private val llm: LlmClient) {

    suspend fun fromPage(ocrText: String, note: String, age: Int): GeneratedMathLesson {
        val text = ocrText.trim().take(LessonGenerator.MAX_SOURCE)
        if (text.length < 10) throw LlmException("На фото почти не нашлось текста. Сфотографируй страницу ближе и при хорошем свете.")
        val messages = listOf(
            ChatMessage(ChatMessage.Role.SYSTEM, systemPrompt(age)),
            ChatMessage(ChatMessage.Role.USER, LessonGenerator.userPrompt(text, note)),
        )
        val first = llm.chat(messages, temperature = 0.2, maxTokens = 4500)
        parse(first)?.let { return it }
        val retry = llm.chat(
            messages + ChatMessage(ChatMessage.Role.ASSISTANT, first) +
                ChatMessage(ChatMessage.Role.USER, "Ответ не разобрался. Верни только один JSON-объект по схеме, без текста вокруг."),
            temperature = 0.1, maxTokens = 4500,
        )
        return parse(retry) ?: throw LlmException("Нейросеть ответила не в том формате. Попробуйте ещё раз.")
    }

    companion object {
        const val MAX_PROBLEMS = 14

        fun systemPrompt(age: Int) = """
Ты — опытный и добрый учитель начальной школы в России, программа «Школа России», учебник математики М. И. Моро.
Тебе дают текст страницы учебника математики, распознанный с фотографии (OCR). В нём бывают ошибки распознавания:
перепутанные знаки (× и х, : и ;), лишние символы, разорванные столбики. Восстанови смысл.
Сделай из страницы урок для ребёнка $age лет.

Ответь СТРОГО одним JSON-объектом, без пояснений и без markdown, по схеме:
{
  "title": "тема страницы по-русски, 2–6 слов",
  "rule": "правило или способ решения со страницы простыми словами для ребёнка, по-русски, 2–5 предложений",
  "examples": [{"problem": "пример со страницы", "solution": "решение по шагам"}],
  "problems": [
    {"kind": "input", "prompt": "задание", "answer": "только число", "unit": "единица ответа или пустая строка", "explain": "решение по шагам, по-русски", "topic": "id темы"},
    {"kind": "choose", "prompt": "Сравни: 4 км ○ 3900 м", "options": ["<", "=", ">"], "answer": ">", "explain": "почему", "topic": "id темы"}
  ],
  "topics": ["id тем из списка ниже, к которым относится страница"]
}

Правила:
- 2–3 examples — разобранные примеры именно этой страницы.
- 8–$MAX_PROBLEMS problems: большинство — на тему этой страницы (такие же, как в учебнике, но с другими числами),
  2–3 — на повторение связанных тем из списка ниже. Есть и примеры, и короткие текстовые задачи.
- Знаки: × — умножение, : — деление, − — минус, □ — что нужно найти, ○ — место знака сравнения, х — неизвестное в уравнении.
  Пример записи: «84 : 4 = □», «х + 125 = 400», «3 м 5 см = □ см».
- answer — ТОЛЬКО число, без единиц и слов; деление с остатком: «9 ост. 2». Единицы измерения — в unit («см», «кг», «руб.», «км/ч»).
- Пересчитай каждый ответ дважды — ошибок в ответах быть не должно. Числа — такие, как положено в 4 классе.
- explain — решение по шагам, как в тетради, коротко и понятно ребёнку.
- topics — 1–3 id из списка, только реально связанные со страницей.

Темы программы 3–4 класса:
${MathBank.catalog()}
""".trim()

        fun parse(raw: String): GeneratedMathLesson? {
            val start = raw.indexOf('{')
            val end = raw.lastIndexOf('}')
            if (start < 0 || end <= start) return null
            val lesson = try {
                json.decodeFromString<GeneratedMathLesson>(raw.substring(start, end + 1))
            } catch (e: Exception) {
                return null
            }
            val problems = lesson.problems.filter { it.toProblem() != null }.take(MAX_PROBLEMS)
            if (problems.isEmpty() && lesson.rule.isBlank()) return null
            return lesson.copy(
                title = lesson.title.trim().ifEmpty { "Новый урок" },
                rule = lesson.rule.trim(),
                problems = problems,
                topics = lesson.topics.map { it.trim() }.filter { MathBank.topic(it) != null }.distinct().take(3),
            )
        }
    }
}
