package ru.sovushka.core

/** Реплика из облачка комикса — с переводом, чтобы Ника прочитала и объяснила. */
data class ComicLine(val en: String, val ru: String)

/**
 * Комикс с Никой к грамматической теме. Картинка лежит в приложении под тем же id.
 * lesson — чему учит комикс, простыми словами; focus — слова правила, которые подсвечиваются в репликах.
 */
data class Comic(
    val id: String,
    val title: String,
    val topic: String,
    val lines: List<ComicLine>,
    val lesson: String = "",
    val focus: List<String> = emptyList(),
) {
    /** Где в реплике слова правила (без учёта регистра, только целые слова) — чтобы выделить их цветом. */
    fun highlights(text: String): List<IntRange> {
        val found = focus.sortedByDescending { it.length }.flatMap { f ->
            Regex("(?<![A-Za-z'])" + Regex.escape(f) + "(?![A-Za-z'])", RegexOption.IGNORE_CASE).findAll(text).map { it.range }.toList()
        }.sortedBy { it.first }
        // Пересечения убираем: длинная фраза важнее слова внутри неё.
        val out = mutableListOf<IntRange>()
        found.forEach { r -> if (out.none { it.first <= r.last && r.first <= it.last }) out.add(r) }
        return out.sortedBy { it.first }
    }
}

/** Комиксы с Никой (нарисованы в OpenArt по референсу персонажа). Тексты совпадают с облачками на картинках. */
object ComicBook {
    private fun l(en: String, ru: String) = ComicLine(en, ru)

    val comics: List<Comic> = listOf(
        Comic(
            "family", "Моя семья", "to_be",
            listOf(
                l("Hi! I am Nika.", "Привет! Я Ника."),
                l("This is my mum. She is a doctor.", "Это моя мама. Она врач."),
                l("This is my dad. He is a pilot.", "Это мой папа. Он лётчик."),
                l("My cat is fluffy.", "Мой кот пушистый."),
                l("We are a happy family!", "Мы счастливая семья!"),
                l("Are you happy too?", "А ты тоже счастлив?"),
            ),
            lesson = "Глагол to be значит «быть». По-русски мы его пропускаем («Я Ника»), а по-английски он нужен всегда: I am, he / she / it is, we / you / they are. Найди в комиксе: I am Nika, She is a doctor, We are a happy family. В вопросе он встаёт вперёд: Are you happy?",
            focus = listOf("am", "is", "are"),
        ),
        Comic(
            "this_these", "Яблоки", "this_these",
            listOf(
                l("Hello! I'm Nika!", "Привет! Я Ника!"),
                l("This is an apple.", "Это яблоко."),
                l("These are apples!", "Это яблоки!"),
                l("Great job!", "Отлично!"),
            ),
            lesson = "This — «это, этот», когда вещь одна, these — «эти», когда их много. This is an apple — одно яблоко, These are apples — много яблок. После this ставим is, после these — are.",
            focus = listOf("This", "These"),
        ),
        Comic(
            "pet", "Мой питомец", "have_got",
            listOf(
                l("I have got a new pet!", "У меня есть новый питомец!"),
                l("It has got big ears.", "У него большие уши."),
                l("It has got a short tail.", "У него короткий хвост."),
                l("Has it got wings?", "А у него есть крылья?"),
                l("No, it hasn't!", "Нет, у него их нет!"),
                l("It's a bunny! Meet Bob!", "Это кролик! Знакомьтесь, Боб!"),
            ),
            lesson = "Have got значит «у меня есть». С I, you, we, they — have got, а с he, she, it — has got: It has got big ears (питомец — это it). В вопросе has встаёт вперёд: Has it got wings? Короткий ответ: No, it hasn't.",
            focus = listOf("have got", "has got", "Has", "hasn't"),
        ),
        Comic(
            "can", "Что я умею", "can",
            listOf(
                l("I can run fast!", "Я умею быстро бегать!"),
                l("I can jump high!", "Я умею высоко прыгать!"),
                l("Can you swim?", "А ты умеешь плавать?"),
                l("Yes, I can!", "Да, умею!"),
                l("Can a fish fly?", "А рыба умеет летать?"),
                l("No, it can't! Ha-ha!", "Нет, не умеет! Ха-ха!"),
            ),
            lesson = "Can — «умею, могу». После can глагол не меняется и без to: I can run, I can jump. В вопросе can встаёт вперёд: Can you swim? Ответ: Yes, I can или No, it can't — can't значит «не умеет».",
            focus = listOf("can", "can't"),
        ),
        Comic(
            "plurals", "Хитрые слова", "plurals",
            listOf(
                l("One mouse, two mice!", "Одна мышь, две мыши!"),
                l("One sheep, three sheep!", "Одна овца, три овцы!"),
                l("One child...", "Один ребёнок…"),
                l("...five children!", "…пять детей!"),
                l("One foot, two feet!", "Одна нога, две ноги!"),
                l("Plurals are fun!", "Множественное число — это весело!"),
            ),
            lesson = "Когда предметов много, обычно добавляем -s: an apple — apples. Но есть слова-хитрецы, их надо просто запомнить: mouse — mice, sheep — sheep (не меняется!), child — children, foot — feet.",
            focus = listOf("mice", "sheep", "children", "feet"),
        ),
        Comic(
            "morning", "Моё утро", "present_simple",
            listOf(
                l("I get up at seven.", "Я встаю в семь."),
                l("I brush my teeth.", "Я чищу зубы."),
                l("I go to school.", "Я иду в школу."),
                l("My cat sleeps all day.", "Мой кот спит весь день."),
                l("He doesn't go to school!", "Он не ходит в школу!"),
                l("Lucky cat!", "Везёт же коту!"),
            ),
            lesson = "Present Simple — про то, что бывает всегда или каждый день. I get up, I go — глагол как в словаре. Но с he, she, it (и с котом!) добавляем -s: My cat sleeps. В отрицании с he, she, it — doesn't, и -s пропадает: He doesn't go to school.",
            focus = listOf("get up", "brush", "go", "sleeps", "doesn't"),
        ),
        Comic(
            "room", "Моя комната", "there_is",
            listOf(
                l("This is my room.", "Это моя комната."),
                l("There is a bed.", "Здесь есть кровать."),
                l("There are two windows.", "Здесь два окна."),
                l("There is a lamp on the desk.", "На столе стоит лампа."),
                l("Is there a monster under the bed?", "Под кроватью есть чудовище?"),
                l("No! There is my cat!", "Нет! Там мой кот!"),
            ),
            lesson = "There is / there are — «есть, находится». Одна вещь — There is a bed, много — There are two windows. Вопрос — Is there a monster? Где находится — говорим в конце: There is a lamp on the desk.",
            focus = listOf("There is", "There are", "Is there"),
        ),
        Comic(
            "now", "Что все делают", "present_continuous",
            listOf(
                l("Hello, Mum!", "Привет, мама!"),
                l("I am painting a picture.", "Я рисую картину."),
                l("Dad is cooking dinner.", "Папа готовит ужин."),
                l("The cat is sleeping.", "Кот спит."),
                l("The dog is eating my picture!", "Собака ест мою картину!"),
                l("Oh no!", "Ой, нет!"),
            ),
            lesson = "Present Continuous — то, что происходит прямо сейчас. Строится так: am / is / are + глагол с -ing. I am painting — я сейчас рисую, Dad is cooking — папа сейчас готовит. Сравни: My cat sleeps — кот вообще спит (всегда), The cat is sleeping — кот спит сейчас.",
            focus = listOf("am painting", "is cooking", "is sleeping", "is eating"),
        ),
        Comic(
            "cat", "Где кот?", "prepositions",
            listOf(
                l("Where is my cat?", "Где мой кот?"),
                l("Is it under the table?", "Он под столом?"),
                l("Is it on the sofa?", "Он на диване?"),
                l("Is it in the box?", "Он в коробке?"),
                l("Is it behind the door?", "Он за дверью?"),
                l("It's on my head!", "Он у меня на голове!"),
            ),
            lesson = "Предлоги места подсказывают, где что-то находится: under — под, on — на, in — в, behind — за. Ника ищет кота: under the table, on the sofa, in the box, behind the door — а он on my head, у неё на голове!",
            focus = listOf("under", "on", "in", "behind"),
        ),
        Comic(
            "sea", "Вчера на море", "past_simple",
            listOf(
                l("Yesterday I went to the sea.", "Вчера я ездила на море."),
                l("I swam in the water.", "Я плавала в воде."),
                l("I built a sandcastle.", "Я построила песочный замок."),
                l("I saw a big crab!", "Я увидела большого краба!"),
                l("The crab took my hat!", "Краб утащил мою шляпу!"),
                l("It was a funny day!", "Это был весёлый день!"),
            ),
            lesson = "Past Simple — про то, что уже было: вчера, летом, на прошлой неделе. Обычные глаголы получают -ed (play — played), а в этом комиксе все глаголы неправильные, их запоминаем парами: go — went, swim — swam, build — built, see — saw, take — took, is — was.",
            focus = listOf("went", "swam", "built", "saw", "took", "was"),
        ),
        Comic(
            "zoo", "Кто больше?", "comparatives",
            listOf(
                l("The cat is big.", "Кот большой."),
                l("The dog is bigger than the cat.", "Собака больше кота."),
                l("The horse is bigger than the dog.", "Лошадь больше собаки."),
                l("The elephant is the biggest!", "Слон самый большой!"),
                l("Who is the smallest?", "А кто самый маленький?"),
                l("Me! But I'm the fastest!", "Я! Зато я самая быстрая!"),
            ),
            lesson = "Чтобы сравнить, к короткому слову добавляем -er и говорим than («чем»): big — bigger than the dog. Самый-самый — the + -est: the biggest, the smallest, the fastest. Заметь: big — bigger — the biggest, буква g удваивается.",
            focus = listOf("bigger", "than", "the biggest", "the smallest", "the fastest"),
        ),
    )

    fun byId(id: String): Comic? = comics.firstOrNull { it.id == id }

    fun forTopic(topic: String): Comic? = comics.firstOrNull { it.topic == topic }
}
