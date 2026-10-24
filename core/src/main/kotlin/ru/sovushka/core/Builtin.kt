package ru.sovushka.core

/** Встроенные наборы слов с картинками — работают без интернета и без нейросети. */
object Builtin {
    data class PackWord(val en: String, val ru: String, val emoji: String)
    data class Pack(val id: String, val emoji: String, val title: String, val words: List<PackWord>)

    private fun pack(id: String, emoji: String, title: String, vararg w: Triple<String, String, String>) =
        Pack(id, emoji, title, w.map { PackWord(it.first, it.second, it.third) })

    private infix fun String.to2(ru: String) = this to ru
    private infix fun Pair<String, String>.with(emoji: String) = Triple(first, second, emoji)

    val packs: List<Pack> = listOf(
        pack(
            "animals", "🐶", "Животные",
            "dog" to2 "собака" with "🐶", "cat" to2 "кошка" with "🐱", "mouse" to2 "мышка" with "🐭",
            "rabbit" to2 "кролик" with "🐰", "bear" to2 "медведь" with "🐻", "fox" to2 "лиса" with "🦊",
            "frog" to2 "лягушка" with "🐸", "elephant" to2 "слон" with "🐘", "lion" to2 "лев" with "🦁",
            "monkey" to2 "обезьяна" with "🐵", "cow" to2 "корова" with "🐮", "pig" to2 "свинка" with "🐷",
            "horse" to2 "лошадь" with "🐴", "fish" to2 "рыбка" with "🐟", "bird" to2 "птичка" with "🐦",
        ),
        pack(
            "food", "🍎", "Еда",
            "apple" to2 "яблоко" with "🍎", "banana" to2 "банан" with "🍌", "orange" to2 "апельсин" with "🍊",
            "pear" to2 "груша" with "🍐", "grapes" to2 "виноград" with "🍇", "strawberry" to2 "клубника" with "🍓",
            "bread" to2 "хлеб" with "🍞", "cheese" to2 "сыр" with "🧀", "milk" to2 "молоко" with "🥛",
            "ice cream" to2 "мороженое" with "🍦", "cake" to2 "торт" with "🍰", "egg" to2 "яйцо" with "🥚",
            "pizza" to2 "пицца" with "🍕", "sweets" to2 "конфеты" with "🍬", "carrot" to2 "морковка" with "🥕",
        ),
        pack(
            "colours", "🎨", "Цвета",
            "red" to2 "красный" with "🔴", "orange" to2 "оранжевый" with "🟠", "yellow" to2 "жёлтый" with "🟡",
            "green" to2 "зелёный" with "🟢", "blue" to2 "синий" with "🔵", "purple" to2 "фиолетовый" with "🟣",
            "brown" to2 "коричневый" with "🟤", "black" to2 "чёрный" with "⚫", "white" to2 "белый" with "⚪",
            "pink" to2 "розовый" with "💗",
        ),
        pack(
            "numbers", "🔢", "Цифры",
            "one" to2 "один" with "1️⃣", "two" to2 "два" with "2️⃣", "three" to2 "три" with "3️⃣",
            "four" to2 "четыре" with "4️⃣", "five" to2 "пять" with "5️⃣", "six" to2 "шесть" with "6️⃣",
            "seven" to2 "семь" with "7️⃣", "eight" to2 "восемь" with "8️⃣", "nine" to2 "девять" with "9️⃣",
            "ten" to2 "десять" with "🔟",
        ),
        pack(
            "family", "👪", "Семья",
            "mother" to2 "мама" with "👩", "father" to2 "папа" with "👨", "sister" to2 "сестра" with "👧",
            "brother" to2 "брат" with "👦", "grandmother" to2 "бабушка" with "👵", "grandfather" to2 "дедушка" with "👴",
            "baby" to2 "малыш" with "👶", "family" to2 "семья" with "👪",
        ),
        pack(
            "body", "👀", "Тело",
            "eyes" to2 "глаза" with "👀", "ear" to2 "ухо" with "👂", "nose" to2 "нос" with "👃",
            "mouth" to2 "рот" with "👄", "hand" to2 "рука" with "✋", "foot" to2 "ступня" with "🦶",
            "leg" to2 "нога" with "🦵", "tooth" to2 "зуб" with "🦷",
        ),
        pack(
            "toys", "🧸", "Игрушки",
            "ball" to2 "мяч" with "⚽", "teddy bear" to2 "плюшевый мишка" with "🧸", "balloon" to2 "воздушный шарик" with "🎈",
            "kite" to2 "воздушный змей" with "🪁", "drum" to2 "барабан" with "🥁", "robot" to2 "робот" with "🤖",
            "doll" to2 "кукла" with "🪆", "puzzle" to2 "пазл" with "🧩",
        ),
        pack(
            "transport", "🚗", "Транспорт",
            "car" to2 "машина" with "🚗", "bus" to2 "автобус" with "🚌", "train" to2 "поезд" with "🚂",
            "plane" to2 "самолёт" with "✈️", "ship" to2 "корабль" with "🚢", "bike" to2 "велосипед" with "🚲",
            "helicopter" to2 "вертолёт" with "🚁", "rocket" to2 "ракета" with "🚀", "tractor" to2 "трактор" with "🚜",
        ),
        pack(
            "clothes", "👕", "Одежда",
            "T-shirt" to2 "футболка" with "👕", "jeans" to2 "джинсы" with "👖", "dress" to2 "платье" with "👗",
            "cap" to2 "кепка" with "🧢", "trainers" to2 "кроссовки" with "👟", "socks" to2 "носки" with "🧦",
            "gloves" to2 "перчатки" with "🧤", "scarf" to2 "шарф" with "🧣", "coat" to2 "пальто" with "🧥",
        ),
        pack(
            "nature", "🌈", "Природа",
            "sun" to2 "солнце" with "☀️", "moon" to2 "луна" with "🌙", "star" to2 "звезда" with "⭐",
            "cloud" to2 "облако" with "☁️", "rain" to2 "дождь" with "🌧️", "snow" to2 "снег" with "❄️",
            "rainbow" to2 "радуга" with "🌈", "tree" to2 "дерево" with "🌳", "flower" to2 "цветок" with "🌸",
            "sea" to2 "море" with "🌊",
        ),
        pack(
            "school", "🎒", "Школа",
            "book" to2 "книга" with "📕", "pencil" to2 "карандаш" with "✏️", "pen" to2 "ручка" with "🖊️",
            "schoolbag" to2 "портфель" with "🎒", "ruler" to2 "линейка" with "📏", "scissors" to2 "ножницы" with "✂️",
            "crayon" to2 "мелок" with "🖍️", "school" to2 "школа" with "🏫", "computer" to2 "компьютер" with "💻",
        ),
        pack(
            "home", "🏠", "Дом",
            "house" to2 "дом" with "🏠", "door" to2 "дверь" with "🚪", "bed" to2 "кровать" with "🛏️",
            "chair" to2 "стул" with "🪑", "bath" to2 "ванна" with "🛁", "sofa" to2 "диван" with "🛋️",
            "lamp" to2 "лампа" with "💡", "key" to2 "ключ" with "🔑", "clock" to2 "часы" with "🕐",
        ),
    )

    fun pack(id: String): Pack? = packs.firstOrNull { it.id == id }
}
