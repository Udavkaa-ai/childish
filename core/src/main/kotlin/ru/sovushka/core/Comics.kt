package ru.sovushka.core

/** Реплика из облачка комикса — с переводом, чтобы Ника прочитала и объяснила. */
data class ComicLine(val en: String, val ru: String)

/** Комикс с Никой к грамматической теме. Картинка лежит в приложении под тем же id. */
data class Comic(val id: String, val title: String, val topic: String, val lines: List<ComicLine>)

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
                l("My cat is fluffy.", "Моя кошка пушистая."),
                l("We are a happy family!", "Мы счастливая семья!"),
                l("Are you happy too?", "А ты тоже счастлив?"),
            ),
        ),
        Comic(
            "this_these", "Яблоки", "this_these",
            listOf(
                l("Hello! I'm Nika!", "Привет! Я Ника!"),
                l("This is an apple.", "Это яблоко."),
                l("These are apples!", "Это яблоки!"),
                l("Great job!", "Отлично!"),
            ),
        ),
        Comic(
            "pet", "Мой питомец", "have_got",
            listOf(
                l("I have got a new pet!", "У меня есть новый питомец!"),
                l("It has got big ears.", "У него большие уши."),
                l("It has got a short tail.", "У него короткий хвост."),
                l("Has it got wings?", "А у него есть крылья?"),
                l("No, it hasn't!", "Нет!"),
                l("It's a bunny! Meet Bob!", "Это кролик! Знакомьтесь, Боб!"),
            ),
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
        ),
    )

    fun byId(id: String): Comic? = comics.firstOrNull { it.id == id }

    fun forTopic(topic: String): Comic? = comics.firstOrNull { it.topic == topic }
}
