package ru.sovushka.core.abc

import kotlin.random.Random

/**
 * Буквы и чтение для дошкольницы — по методике букваря Н. С. Жуковой:
 * буквы по одной (сначала гласные А, У, О, потом согласные), сразу же — слияние в слоги
 * («буква бежит к букве»: М + А = МА) и чтение слов только из уже знакомых букв.
 * Картинки — эмодзи, всё объясняет голос Ники: малышка ещё не читает.
 */
object Abc {
    enum class Kind { VOWEL, CONSONANT, SIGN }

    /** Картинка со словом. */
    data class Pic(val word: String, val emoji: String)

    /**
     * Буква. name — как Ника называет букву вслух; hear — как звучит её звук (для гласных и
     * протяжных согласных — протяжно, для коротких — через слово); look — на что похожа.
     */
    data class Letter(
        val upper: Char,
        val name: String,
        val kind: Kind,
        val hear: String,
        val look: String,
        val pics: List<Pic>,
    ) {
        val lower: Char get() = upper.lowercaseChar()
        val pair: String get() = "$upper$lower"
    }

    private fun v(c: Char, name: String, hear: String, look: String, vararg pics: Pic) = Letter(c, name, Kind.VOWEL, hear, look, pics.toList())
    private fun c(c: Char, name: String, hear: String, look: String, vararg pics: Pic) = Letter(c, name, Kind.CONSONANT, hear, look, pics.toList())
    private fun p(word: String, emoji: String) = Pic(word, emoji)

    /** Порядок букв как в букваре Жуковой. */
    val LETTERS: List<Letter> = listOf(
        v('А', "а", "А-а-а! Как в слове арбуз.", "Буква А — как шалашик с перекладинкой.",
            p("арбуз", "🍉"), p("ананас", "🍍"), p("автобус", "🚌"), p("акула", "🦈")),
        v('У', "у", "У-у-у! Так гудит паровоз. Как в слове утка.", "Буква У — как сучок на дереве.",
            p("утка", "🦆"), p("улитка", "🐌"), p("удочка", "🎣"), p("ухо", "👂"), p("утюг", "🧺")),
        v('О', "о", "О-о-о! Как в слове облако.", "Буква О — круглая, как бублик.",
            p("облако", "☁️"), p("огурец", "🥒"), p("осьминог", "🐙"), p("очки", "👓"), p("окно", "🪟")),
        c('М', "эм", "Ммм… Мммышка. Слышишь, как мычит? Ммм.", "Буква М — как две горки.",
            p("мышка", "🐭"), p("мяч", "⚽"), p("морковка", "🥕"), p("машина", "🚗"), p("мёд", "🍯"), p("муха", "🪰")),
        c('С', "эс", "Ссс… Так свистит ветер. Как в слове сыр.", "Буква С — как месяц.",
            p("сыр", "🧀"), p("слон", "🐘"), p("сова", "🦉"), p("солнце", "☀️"), p("сок", "🧃"), p("сумка", "👜"), p("сани", "🛷")),
        c('Х', "ха", "Ххх… Так греем ладошки. Как в слове хлеб.", "Буква Х — как две палочки крестиком.",
            p("хлеб", "🍞"), p("хомяк", "🐹"), p("хот-дог", "🌭")),
        c('Р', "эр", "Ррр! Так рычит тигрёнок. Как в слове рыба.", "Буква Р — как флажок.",
            p("рыба", "🐟"), p("ракета", "🚀"), p("роза", "🌹"), p("радуга", "🌈")),
        c('Ш', "ша", "Шшш… Так шипит змейка. Как в слове шар.", "Буква Ш — как расчёска зубчиками вверх.",
            p("шар", "🎈"), p("шапка", "🧢"), p("шоколад", "🍫")),
        v('Ы', "ы", "Ы-ы-ы! С неё слова не начинаются, она живёт внутри: сыр, мышка.", "Буква Ы — палочка с животиком и ещё палочка.",
            p("сыр", "🧀"), p("мышка", "🐭"), p("рыба", "🐟")),
        c('Л', "эль", "Ллл… Как в слове лимон.", "Буква Л — как домик без пола.",
            p("лимон", "🍋"), p("лиса", "🦊"), p("лодка", "🛶"), p("луна", "🌙"), p("лев", "🦁")),
        c('Н', "эн", "Ннн… Как в слове нос.", "Буква Н — как лесенка с одной ступенькой.",
            p("нос", "👃"), p("ножницы", "✂️"), p("носки", "🧦")),
        c('К', "ка", "Короткий звук: к! С него начинается слово кот.", "Буква К — палочка, а к ней две веточки.",
            p("кот", "🐱"), p("кит", "🐳"), p("корова", "🐄"), p("ключ", "🔑"), p("книга", "📚")),
        c('Т', "тэ", "Короткий звук: т! Как стучит молоточек. С него начинается слово торт.", "Буква Т — как молоточек.",
            p("торт", "🎂"), p("тигр", "🐯"), p("трактор", "🚜"), p("тыква", "🎃"), p("тюлень", "🦭")),
        v('И', "и", "И-и-и! Как в слове индюк.", "Буква И — две палочки и ниточка наискосок.",
            p("индюк", "🦃"), p("ирис", "🌷")),
        c('П', "пэ", "Короткий звук: п! Как в слове пицца.", "Буква П — как ворота.",
            p("пицца", "🍕"), p("пингвин", "🐧"), p("пчела", "🐝"), p("помидор", "🍅"), p("пила", "🪚")),
        c('З', "зэ", "Ззз… Так звенит комарик. Как в слове зебра.", "Буква З — как цифра три.",
            p("зебра", "🦓"), p("зонт", "☂️"), p("звезда", "⭐"), p("змея", "🐍")),
        c('Й', "и краткое", "Короткое й, как в конце слова чай.", "Буква Й — это И с шапочкой.",
            p("йо-йо", "🪀"), p("йогурт", "🥛")),
        c('Г', "гэ", "Короткий звук: г! Как в слове гриб.", "Буква Г — как кочерга.",
            p("гриб", "🍄"), p("груша", "🍐"), p("гитара", "🎸"), p("гора", "⛰️")),
        c('В', "вэ", "Ввв… Как в слове волк.", "Буква В — палочка с двумя животиками.",
            p("волк", "🐺"), p("виноград", "🍇"), p("велосипед", "🚲"), p("вертолёт", "🚁"), p("ведро", "🪣")),
        c('Д', "дэ", "Короткий звук: д! Как в слове дом.", "Буква Д — как домик на ножках.",
            p("дом", "🏠"), p("дерево", "🌳"), p("дельфин", "🐬"), p("дракон", "🐉")),
        c('Б', "бэ", "Короткий звук: б! Как в слове банан.", "Буква Б — с животиком и с кепкой.",
            p("банан", "🍌"), p("бабочка", "🦋"), p("барабан", "🥁"), p("белка", "🐿️"), p("бант", "🎀")),
        c('Ж', "жэ", "Жжж… Так жужжит жук.", "Буква Ж — как жучок с лапками.",
            p("жук", "🐞"), p("жираф", "🦒")),
        v('Е', "е", "Е! Как в слове ель.", "Буква Е — как гребешок с тремя зубчиками.",
            p("енот", "🦝"), p("ель", "🌲")),
        Letter('Ь', "мягкий знак", Kind.SIGN, "Мягкий знак звука не говорит. Он делает букву перед собой мягкой: конь, соль.",
            "Мягкий знак — палочка с животиком.", listOf(p("конь", "🐴"), p("соль", "🧂"), p("медведь", "🐻"))),
        v('Я', "я", "Я! Как в слове яблоко.", "Буква Я — как Р, которая смотрит в другую сторону.",
            p("яблоко", "🍎"), p("яйцо", "🥚"), p("ящерица", "🦎"), p("якорь", "⚓")),
        v('Ю', "ю", "Ю! Как в слове Юпитер.", "Буква Ю — палочка держит колечко.",
            p("юла", "🪀"), p("юбка", "👗"), p("Юпитер", "🪐")),
        v('Ё', "ё", "Ё! Как в слове ёжик.", "Буква Ё — это Е с двумя точками.",
            p("ёжик", "🦔"), p("ёлка", "🎄")),
        c('Ч', "че", "Короткий звук: ч! Как в слове чай.", "Буква Ч — как стульчик вверх ногами.",
            p("чай", "🍵"), p("черепаха", "🐢"), p("часы", "⌚"), p("чемодан", "🧳")),
        c('Ф', "эф", "Ффф… Так пыхтит ёжик. Как в слове флаг.", "Буква Ф — колечко, а через него палочка.",
            p("флаг", "🚩"), p("фламинго", "🦩"), p("фея", "🧚")),
        v('Э', "э", "Э-э-э! Как в слове эльф.", "Буква Э — как С с язычком.",
            p("эльф", "🧝"), p("эскимо", "🍦"), p("этаж", "🏢")),
        c('Ц', "цэ", "Короткий звук: ц! Как в слове цирк.", "Буква Ц — как П вверх ногами с хвостиком.",
            p("цирк", "🎪"), p("цыплёнок", "🐥"), p("цветок", "🌷")),
        c('Щ', "ща", "Щщщ… Как в слове щенок.", "Буква Щ — как Ш с хвостиком.",
            p("щенок", "🐶"), p("щука", "🐟")),
        Letter('Ъ', "твёрдый знак", Kind.SIGN, "Твёрдый знак звука не говорит. Он разделяет буквы: подъезд, съел.",
            "Твёрдый знак — мягкий знак с флажком.", emptyList()),
    )

    val COUNT: Int get() = LETTERS.size
    private val INDEX: Map<Char, Int> = LETTERS.withIndex().associate { (i, l) -> l.upper to i }

    fun indexOf(c: Char): Int = INDEX[c.uppercaseChar()] ?: -1

    /** Слова для чтения: картинка подтверждает, что ребёнок понял прочитанное. */
    val WORDS: List<Pic> = listOf(
        p("мама", "👩"), p("оса", "🐝"), p("сом", "🐟"), p("муха", "🪰"),
        p("рама", "🖼️"),
        p("шар", "🎈"), p("сыр", "🧀"), p("усы", "👨"),
        p("мыло", "🧼"), p("лама", "🦙"), p("луна", "🌙"), p("нос", "👃"), p("сон", "😴"), p("слон", "🐘"),
        p("сок", "🧃"), p("лук", "🧅"), p("рак", "🦞"), p("кошка", "🐈"), p("рука", "✋"), p("мак", "🌺"), p("окно", "🪟"),
        p("кот", "🐱"), p("торт", "🎂"), p("утка", "🦆"), p("рот", "👄"), p("стол", "🪑"),
        p("кит", "🐳"), p("лиса", "🦊"), p("нитки", "🧵"), p("мишка", "🧸"), p("рис", "🍚"), p("лист", "🍃"),
        p("суп", "🍲"), p("лампа", "💡"), p("пила", "🪚"), p("сумка", "👜"), p("сани", "🛷"), p("паук", "🕷️"), p("пони", "🐴"), p("парк", "🏞️"),
        p("зонт", "☂️"), p("коза", "🐐"), p("роза", "🌹"), p("зима", "❄️"),
        p("зайка", "🐰"), p("майка", "👕"),
        p("гора", "⛰️"), p("нога", "🦵"), p("магнит", "🧲"),
        p("сова", "🦉"), p("волк", "🐺"), p("вагон", "🚃"), p("трава", "🌿"), p("корова", "🐄"), p("ваза", "🏺"),
        p("дом", "🏠"), p("вода", "💧"), p("дракон", "🐉"), p("подарок", "🎁"), p("дудка", "🎺"),
        p("банан", "🍌"), p("зубы", "🦷"), p("рыба", "🐟"), p("бант", "🎀"), p("гриб", "🍄"), p("собака", "🐕"), p("барабан", "🥁"),
        p("жук", "🐞"), p("лыжи", "🎿"), p("лужа", "💧"),
        p("лев", "🦁"), p("белка", "🐿️"), p("енот", "🦝"), p("велосипед", "🚲"), p("дерево", "🌳"), p("ведро", "🪣"),
        p("конь", "🐴"), p("соль", "🧂"), p("ель", "🌲"), p("медведь", "🐻"), p("лось", "🦌"),
        p("яблоко", "🍎"), p("дыня", "🍈"), p("земляника", "🍓"), p("якорь", "⚓"),
        p("салют", "🎆"), p("клюв", "🐦"), p("тюлень", "🦭"), p("утюг", "🧺"), p("юла", "🪀"), p("юбка", "👗"),
        p("ёжик", "🦔"), p("ёлка", "🎄"), p("самолёт", "✈️"), p("вертолёт", "🚁"),
        p("мяч", "⚽"), p("пчела", "🐝"), p("чай", "🍵"), p("ключ", "🔑"), p("бабочка", "🦋"), p("часы", "⌚"), p("очки", "👓"),
        p("жираф", "🦒"), p("флаг", "🚩"), p("шарф", "🧣"), p("фонарь", "🔦"),
        p("эльф", "🧝"), p("эскимо", "🍦"),
        p("заяц", "🐇"), p("яйцо", "🥚"), p("цирк", "🎪"), p("огурец", "🥒"), p("курица", "🐔"), p("птица", "🐦"),
        p("щенок", "🐶"), p("щука", "🐟"), p("ящик", "📦"),
    )

    /** С какого урока слово можно прочитать: номер самой поздней буквы в нём. */
    fun unlockOf(word: String): Int = word.uppercase().filter { it.isLetter() }.maxOf { indexOf(it) }

    /** Слова, которые можно прочитать, зная первые [known] букв. */
    fun readable(known: Int): List<Pic> = WORDS.filter { unlockOf(it.word) < known }

    /**
     * Деление на слоги для чтения: слог заканчивается на гласную; согласные между гласными
     * уходят в следующий слог, кроме Й, Ь, Ъ и звонкой Л/М/Н/Р перед другой согласной («сум-ка», «пал-ка»).
     */
    fun syllables(word: String): List<String> {
        val w = word.lowercase()
        fun vowel(c: Char) = c in "аоуыиэеёюя"
        val vowels = w.indices.filter { vowel(w[it]) }
        if (vowels.size <= 1) return listOf(word)
        val cuts = mutableListOf<Int>()
        for (k in 0 until vowels.size - 1) {
            val a = vowels[k]
            val b = vowels[k + 1]
            val between = b - a - 1
            var cut = a + 1
            if (between >= 1) {
                // Й, Ь, Ъ остаются с предыдущим слогом.
                while (cut < b && w[cut] in "йьъ") cut++
                if (between >= 2 && cut < b - 1 && w[cut] in "лмнр") cut++
            }
            cuts += cut
        }
        val parts = mutableListOf<String>()
        var from = 0
        for (cut in cuts) { parts += word.substring(from, cut); from = cut }
        parts += word.substring(from)
        return parts
    }

    /** Гласные, с которыми согласная сливается в слоги (по правилам письма: ЖИ-ШИ, ЧА-ЩА, ЧУ-ЩУ). */
    fun vowelsFor(consonant: Char, known: Int): List<Char> {
        val vs = LETTERS.take(known).filter { it.kind == Kind.VOWEL }.map { it.upper }
        val banned = when (consonant) {
            'Ж', 'Ш' -> "ЫЯЮЭ"
            'Ч', 'Щ' -> "ЫЯЮЭ"
            'Ц' -> "ЯЮЁЭ"
            'К', 'Г', 'Х' -> "ЫЯЮЁЭ"
            else -> "Э"
        }
        return vs.filter { it !in banned }
    }

    /** Как произносить слог голосом: «ммма» звучит как слияние, а не как название буквы. */
    fun sayMerge(consonant: Letter, vowel: Char): String {
        val v = vowel.lowercaseChar()
        val c = consonant.lower
        val stretch = consonant.upper in "МСХРШЛНЗВЖФЩ"
        return if (stretch) "$c$c$c$v… $c$v!" else "$c$v!"
    }

    /** Первые десять уроков слова и слоги — заглавными буквами (их малыши узнают легче), дальше — как в книжке. */
    fun shown(text: String, lesson: Int): String = if (lesson < 10) text.uppercase() else text

    // ---------- Задания ----------

    sealed interface Task {
        /** Знакомство: буква, как звучит, на что похожа, картинка. */
        data class Meet(val letter: Letter) : Task
        /** Обвести букву пальчиком. */
        data class Trace(val letter: Letter) : Task
        /** «Где буква …?» */
        data class FindLetter(val target: Letter, val options: List<Letter>, val lowercase: Boolean) : Task
        /** «Какая картинка начинается на …?» (для Ы — «в каком слове есть …?») */
        data class FirstSound(val letter: Letter, val right: Pic, val options: List<Pic>) : Task
        /** Паровозик: согласная бежит к гласной. */
        data class Merge(val consonant: Letter, val vowel: Char) : Task
        /** «Где слог …?» */
        data class FindSyllable(val target: String, val options: List<String>) : Task
        /** Прочитай слово и найди картинку. */
        data class ReadWord(val word: Pic, val options: List<Pic>) : Task
    }

    /** Урок новой буквы с номером [index] (с нуля): знакомство, обводилка, поиск, слоги и слова. */
    fun lesson(index: Int, random: Random = Random.Default): List<Task> {
        val letter = LETTERS[index]
        val known = index + 1
        val tasks = mutableListOf<Task>(Task.Meet(letter), Task.Trace(letter))
        repeat(if (index == 0) 2 else 3) { tasks += findLetter(letter, known, index, random) }
        letter.pics.shuffled(random).take(2).forEach { pic -> firstSound(letter, random, pic)?.let { tasks += it } }
        if (letter.kind != Kind.SIGN) {
            val merges = merges(letter, known)
            merges.shuffled(random).take(2).forEach { (cons, vowel) -> tasks += Task.Merge(cons, vowel) }
            syllablesOf(letter, known).takeIf { it.size >= 2 }?.let { pool ->
                // Каждый раз — другой слог.
                pool.shuffled(random).take(3).forEach { tasks += findSyllable(pool, known, random, index, it) }
            }
        }
        val words = readable(known)
        // Сначала слова с новой буквой, потом — любые знакомые.
        val fresh = words.filter { letter.upper in it.word.uppercase() }.shuffled(random)
        val pickWords = (fresh + words.shuffled(random)).distinct().take(3)
        pickWords.forEach { tasks += readWord(it, words, random) }
        return tasks
    }

    /** Повторение: вперемешку все знакомые буквы, слоги и слова. */
    fun review(known: Int, random: Random = Random.Default, count: Int = 10): List<Task> {
        if (known <= 0) return emptyList()
        val letters = LETTERS.take(known)
        val out = mutableListOf<Task>()
        val words = readable(known)
        val sylPool = letters.filter { it.kind == Kind.CONSONANT }.flatMap { syllablesOf(it, known) }.distinct()
        while (out.size < count) {
            val roll = random.nextInt(10)
            val task: Task? = when {
                words.size >= 3 && roll < 4 -> readWord(words.random(random), words, random)
                sylPool.size >= 3 && roll < 7 -> findSyllable(sylPool, known, random, known - 1)
                roll < 9 -> findLetter(letters.random(random), known, known - 1, random)
                else -> firstSound(letters.random(random), random)
            }
            if (task != null && task != out.lastOrNull()) out += task
        }
        return out
    }

    private fun findLetter(target: Letter, known: Int, lesson: Int, random: Random): Task.FindLetter {
        val count = if (lesson < 3) 3 else 4
        // Отвлекающие — сначала знакомые буквы, если их мало — любые из букваря.
        val pool = (LETTERS.take(known) - target).shuffled(random) + (LETTERS.drop(known) - target).shuffled(random)
        val options = (listOf(target) + pool.take(count - 1)).shuffled(random)
        return Task.FindLetter(target, options, lowercase = lesson >= 10 && random.nextBoolean())
    }

    private fun firstSound(letter: Letter, random: Random, pic: Pic? = null): Task.FirstSound? {
        if (letter.pics.isEmpty()) return null
        val right = pic ?: letter.pics.random(random)
        val inside = letter.upper == 'Ы' || letter.kind == Kind.SIGN
        val others = LETTERS.filter { it != letter }.flatMap { it.pics }
            .filter { o -> o.word.isNotEmpty() && if (inside) letter.lower !in o.word else o.word[0].uppercaseChar() != letter.upper }
            .distinctBy { it.emoji }
            .filter { it.emoji != right.emoji }
            .shuffled(random).take(2)
        if (others.size < 2) return null
        return Task.FirstSound(letter, right, (others + right).shuffled(random))
    }

    /** Пары «согласная + гласная» урока: новая согласная со знакомыми гласными, новая гласная — со знакомыми согласными. */
    private fun merges(letter: Letter, known: Int): List<Pair<Letter, Char>> = when (letter.kind) {
        Kind.CONSONANT -> if (letter.upper == 'Й') emptyList() else vowelsFor(letter.upper, known).map { letter to it }
        Kind.VOWEL -> LETTERS.take(known).filter { it.kind == Kind.CONSONANT && letter.upper in vowelsFor(it.upper, known) }.map { it to letter.upper }
        Kind.SIGN -> emptyList()
    }

    private fun syllablesOf(letter: Letter, known: Int): List<String> = when (letter.kind) {
        Kind.SIGN -> emptyList()
        Kind.CONSONANT -> if (letter.upper == 'Й') vowelsFor('Й', known).filter { it in "АОУЕ" }.map { "${it}Й" }
        else vowelsFor(letter.upper, known).map { "${letter.upper}$it" }
        Kind.VOWEL -> merges(letter, known).filter { it.first.upper != 'Й' }.map { (c, v) -> "${c.upper}$v" }
    }

    private fun findSyllable(pool: List<String>, known: Int, random: Random, lesson: Int, pick: String? = null): Task.FindSyllable {
        val target = pick ?: pool.random(random)
        // Похожие слоги: та же согласная или та же гласная.
        val similar = LETTERS.take(known).filter { it.kind == Kind.CONSONANT && it.upper != 'Й' }
            .flatMap { c -> vowelsFor(c.upper, known).map { "${c.upper}$it" } }
            .filter { it != target && (it[0] == target[0] || it[1] == target[1]) }
            .shuffled(random)
        val opts = (listOf(target) + (similar + pool.filter { it != target }.shuffled(random)).distinct().take(2)).shuffled(random)
        fun cased(t: String) = if (lesson < 10) t.uppercase() else t.lowercase()
        return Task.FindSyllable(cased(target), opts.map { cased(it) })
    }

    private fun readWord(word: Pic, words: List<Pic>, random: Random): Task.ReadWord {
        val others = (words + LETTERS.flatMap { it.pics })
            .filter { it.emoji != word.emoji && it.word != word.word }
            .distinctBy { it.emoji }
            .shuffled(random).take(2)
        return Task.ReadWord(word, (others + word).shuffled(random))
    }
}
