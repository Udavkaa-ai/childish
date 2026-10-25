package ru.sovushka.core

import kotlin.random.Random

/** Одно задание тренировки. */
sealed interface Task {
    val word: Word?

    /** Знакомство: картинка, слово, транскрипция, перевод и озвучка. */
    data class Intro(override val word: Word) : Task

    /** Слушаем английское слово — выбираем перевод. */
    data class ListenPick(override val word: Word, val options: List<String>) : Task

    /** Видим русское слово — выбираем английское. */
    data class ReadPick(override val word: Word, val options: List<String>) : Task

    /** Собираем слово из букв по звуку и переводу. */
    data class Spell(override val word: Word, val letters: List<Char>) : Task

    /** Произносим слово в микрофон. */
    data class Say(override val word: Word) : Task

    /**
     * Грамматика: вставить слово, выбрать правильное предложение или собрать предложение по-русски.
     * choices — варианты вперемешку (для gap/choose), tiles — слова вперемешку с лишними (для order).
     */
    data class Grammar(val drill: Drill, val choices: List<String>, val tiles: List<String>) : Task {
        override val word: Word? get() = null
    }

    /** Слушаем предложение — собираем его из слов. */
    data class Build(val sentence: Sentence, val tokens: List<String>) : Task {
        override val word: Word? get() = null
    }
}

object SessionBuilder {
    const val LESSON_WORDS = 6
    const val REVIEW_WORDS = 10

    const val LESSON_DRILLS = 8

    fun grammarTask(d: Drill, random: Random): Task.Grammar = Task.Grammar(
        d,
        d.options.distinct().shuffled(random),
        if (d.kind == Drill.ORDER) (d.answerTokens + d.extra).shuffled(random) else emptyList(),
    )

    /** Тренировка по грамматической теме: упражнения вперемешку. */
    fun grammar(drills: List<Drill>, count: Int = 8, random: Random = Random.Default): List<Task> =
        drills.filter { it.isValid() }.shuffled(random).take(count).map { grammarTask(it, random) }

    /**
     * Тренировка урока: знакомство с новыми словами, упражнения на слова, а затем грамматика:
     * упражнения страницы и повторение пройденных тем (reviewDrills).
     */
    fun lesson(
        words: List<Word>,
        sentences: List<Sentence>,
        allWords: List<Word>,
        canSpeak: Boolean,
        random: Random = Random.Default,
        drills: List<Drill> = emptyList(),
        reviewDrills: List<Drill> = emptyList(),
    ): List<Task> {
        if (words.isEmpty()) return emptyList()
        // Берём в первую очередь хуже всего выученные слова.
        val picked = words.sortedWith(compareBy<Word> { it.box }.thenBy { it.right - it.wrong })
            .take(LESSON_WORDS)
            .shuffled(random)
        val pool = (allWords + words).distinctBy { it.id }
        val intro = picked.filter { it.box == 0 }.map { Task.Intro(it) }
        // Если у урока есть грамматика, на слова оставляем по два задания (правописание — всегда).
        val perWord = if (drills.isNotEmpty() || reviewDrills.isNotEmpty()) 2 else 3
        val wordTasks = picked.flatMap { w ->
            val kinds = mutableListOf(Kind.LISTEN, Kind.READ, Kind.SPELL)
            if (canSpeak) kinds += Kind.SAY
            kinds.shuffled(random).take(perWord).let { if (Kind.SPELL in it) it else it.dropLast(1) + Kind.SPELL }
                .map { make(it, w, pool, random) }
        }
        val builds = sentences.filter { tokenize(it.en).size in 2..9 }
            .shuffled(random).take(2)
            .map { Task.Build(it, tokenize(it.en).shuffled(random)) }
        val grammar = grammar(drills, LESSON_DRILLS - minOf(3, reviewDrills.size), random) + grammar(reviewDrills, 3, random)
        return intro + spread(wordTasks.shuffled(random)) + builds + grammar.shuffled(random)
    }

    /** Повторение: по два упражнения на каждое слово, которому пора, и задания по темам, которые пора освежить. */
    fun review(
        due: List<Word>,
        allWords: List<Word>,
        canSpeak: Boolean,
        random: Random = Random.Default,
        drills: List<Drill> = emptyList(),
    ): List<Task> {
        val picked = due.sortedBy { it.due }.take(REVIEW_WORDS)
        val wordDrills = picked.flatMap { w ->
            val first = if (random.nextBoolean()) Kind.LISTEN else Kind.READ
            val second = if (canSpeak && random.nextInt(3) == 0) Kind.SAY else Kind.SPELL
            listOf(make(first, w, allWords, random), make(second, w, allWords, random))
        }
        return spread(wordDrills.shuffled(random)) + grammar(drills, 6, random)
    }

    private enum class Kind { LISTEN, READ, SPELL, SAY }

    private fun make(kind: Kind, w: Word, pool: List<Word>, random: Random): Task = when (kind) {
        Kind.LISTEN -> Task.ListenPick(w, options(w.ru, pool.map { it.ru }, random))
        Kind.READ -> Task.ReadPick(w, options(w.en, pool.map { it.en }, random))
        Kind.SPELL -> Task.Spell(w, letters(w.en, random))
        Kind.SAY -> Task.Say(w)
    }

    /** Правильный ответ + до трёх отвлекающих; если слов мало — добираем из запасного словаря. */
    fun options(correct: String, pool: List<String>, random: Random, count: Int = 4): List<String> {
        val key = TextMatch.normalize(correct)
        val others = pool.filter { TextMatch.normalize(it) != key }.distinctBy { TextMatch.normalize(it) }
            .shuffled(random)
            .toMutableList()
        if (others.size < count - 1) {
            val isRu = correct.any { it in 'Ѐ'..'ӿ' }
            val extra = Builtin.packs.flatMap { it.words }.map { if (isRu) it.ru else it.en }
                .filter { e -> TextMatch.normalize(e) != key && others.none { TextMatch.normalize(it) == TextMatch.normalize(e) } }
                .shuffled(random)
            others += extra.take(count - 1 - others.size)
        }
        return (others.take(count - 1) + correct).shuffled(random)
    }

    /** Буквы слова вперемешку (пробелы убираем, апостроф оставляем). */
    fun letters(en: String, random: Random): List<Char> {
        val chars = en.lowercase().filter { it.isLetter() || it == '\'' }.toList()
        if (chars.size < 2) return chars
        var shuffled = chars.shuffled(random)
        var tries = 0
        while (shuffled == chars && tries++ < 10) shuffled = chars.shuffled(random)
        return shuffled
    }

    /** Ожидаемая последовательность букв для задания Spell. */
    fun spellTarget(en: String): String = en.lowercase().filter { it.isLetter() || it == '\'' }

    fun tokenize(s: String): List<String> =
        s.split(' ').map { it.trim().trimEnd('.', '!', '?', ',', ';', ':') }.filter { it.isNotBlank() }

    /** Стараемся, чтобы два задания на одно слово не шли подряд. */
    private fun spread(tasks: List<Task>): List<Task> {
        val rest = tasks.toMutableList()
        val out = mutableListOf<Task>()
        while (rest.isNotEmpty()) {
            val lastId = out.lastOrNull()?.word?.id
            val idx = rest.indexOfFirst { it.word?.id != lastId }.takeIf { it >= 0 } ?: 0
            out += rest.removeAt(idx)
        }
        return out
    }

    /** Проверка ответа в «order»: регистр, пунктуация и сокращения (it's = it is) не важны. */
    fun orderCorrect(d: Drill, built: List<String>): Boolean {
        fun norm(s: String) = TextMatch.normalize(
            s.replace("n't", " not").replace("'m", " am").replace("'re", " are").replace("'ve", " have")
                .replace(Regex("\\b(it|he|she|that|what|there)'s\\b", RegexOption.IGNORE_CASE), "$1 is"),
        )
        return norm(built.joinToString(" ")) == norm(d.answer)
    }

    /** Звёзды за тренировку по доле правильных ответов. */
    fun stars(correct: Int, total: Int): Int {
        if (total == 0) return 0
        val p = correct.toFloat() / total
        return when {
            p >= 0.9f -> 3
            p >= 0.7f -> 2
            else -> 1
        }
    }
}
