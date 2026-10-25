package ru.sovushka.core

/**
 * Пройденные в начальной школе темы (2–4 класс, как в учебниках «Spotlight» / «Английский в фокусе»):
 * короткое правило по-русски, примеры и упражнения, которые работают без интернета.
 */
object GrammarBank {
    private fun gap(prompt: String, answer: String, vararg wrong: String, explain: String = "") =
        Drill(Drill.GAP, prompt, listOf(answer) + wrong, answer, explain = explain)

    private fun choose(answer: String, vararg wrong: String, explain: String = "") =
        Drill(Drill.CHOOSE, "Выбери правильное предложение", listOf(answer) + wrong, answer, explain = explain)

    private fun order(ru: String, en: String, vararg extra: String, explain: String = "") =
        Drill(Drill.ORDER, ru, emptyList(), en, extra.toList(), explain)

    private fun ex(en: String, ru: String) = Sentence(en, ru)

    private fun topic(id: String, grade: Int, title: String, rule: String, examples: List<Sentence>, vararg drills: Drill) =
        GrammarTopic(id, grade, title, rule, examples, drills.map { it.copy(topic = id) })

    val topics: List<GrammarTopic> = listOf(
        // ---------- 2 класс ----------
        topic(
            "to_be", 2, "Глагол to be: am, is, are",
            "Глагол to be значит «быть, являться». С I — am, с he, she, it — is, с we, you, they — are. " +
                "Отрицание: добавляем not — I am not, he isn't, they aren't. Вопрос: ставим глагол вперёд — Is she happy? Are you ten?",
            listOf(ex("I am ten.", "Мне десять лет."), ex("She is my sister.", "Она моя сестра."), ex("Are you happy?", "Ты счастлива?")),
            gap("I ___ ten years old.", "am", "is", "are", explain = "С I всегда am."),
            gap("My cat ___ black.", "is", "am", "are", explain = "My cat — это it, значит is."),
            gap("We ___ friends.", "are", "is", "am", explain = "С we — are."),
            gap("___ your mum a teacher?", "Is", "Are", "Am", explain = "Your mum — she, вопрос начинаем с Is."),
            choose("They aren't at school.", "They isn't at school.", "They not are at school.", explain = "They — are, отрицание aren't."),
            order("Она моя лучшая подруга.", "She is my best friend", "are", "am"),
            order("Ты дома?", "Are you at home", "Is", "am"),
        ),
        topic(
            "have_got", 2, "have got / has got — у меня есть",
            "Чтобы сказать «у меня есть», говорим I have got (I've got). С he, she, it — has got (she's got). " +
                "Отрицание: haven't got / hasn't got. Вопрос: Have you got a dog? — Yes, I have. / No, I haven't.",
            listOf(ex("I have got a dog.", "У меня есть собака."), ex("She has got blue eyes.", "У неё голубые глаза."), ex("Have you got a pen?", "У тебя есть ручка?")),
            gap("I ___ got a new bike.", "have", "has", "am", explain = "С I — have got."),
            gap("My brother ___ got a robot.", "has", "have", "is", explain = "My brother — he, значит has got."),
            gap("___ you got a sister?", "Have", "Has", "Are", explain = "Вопрос с you: Have you got…?"),
            gap("She ___ got a cat. She's got a dog.", "hasn't", "haven't", "isn't", explain = "С she отрицание hasn't got."),
            choose("He has got long hair.", "He have got long hair.", "He has get long hair.", explain = "He — has got."),
            order("У нас есть большой дом.", "We have got a big house", "has", "is"),
            order("У тебя есть ручка?", "Have you got a pen", "Has", "are"),
        ),
        topic(
            "can", 2, "can / can't — умею, могу",
            "Can значит «умею, могу». Оно одинаковое для всех: I can, she can, they can. После can глагол без to и без -s: She can swim. " +
                "Отрицание: can't (cannot). Вопрос: Can you swim? — Yes, I can. / No, I can't.",
            listOf(ex("I can swim.", "Я умею плавать."), ex("He can't ride a bike.", "Он не умеет кататься на велосипеде."), ex("Can you sing?", "Ты умеешь петь?")),
            gap("My dog can ___ fast.", "run", "runs", "to run", explain = "После can глагол без -s и без to."),
            gap("Penguins ___ fly.", "can't", "can", "cans", explain = "Пингвины не летают — can't."),
            gap("___ you play the piano?", "Can", "Do", "Are", explain = "Вопрос про умение: Can you…?"),
            choose("She can dance very well.", "She cans dance very well.", "She can dances very well.", explain = "Can не меняется, глагол после него без -s."),
            choose("Can you swim? — Yes, I can.", "Can you swim? — Yes, I swim.", "Can you swim? — Yes, I do.", explain = "Короткий ответ: Yes, I can."),
            order("Я умею кататься на велосипеде.", "I can ride a bike", "to", "rides"),
            order("Мой брат не умеет плавать.", "My brother can't swim", "swims", "isn't"),
        ),
        topic(
            "plurals", 2, "Много предметов: -s, -es и исключения",
            "Чтобы предметов стало много, добавляем -s: a cat — cats. После s, sh, ch, x, o — -es: a box — boxes, a tomato — tomatoes. " +
                "Если в конце согласная + y, y меняется на -ies: a baby — babies. Запомни исключения: man — men, woman — women, child — children, " +
                "mouse — mice, tooth — teeth, foot — feet, sheep — sheep.",
            listOf(ex("two cats", "две кошки"), ex("three boxes", "три коробки"), ex("five children", "пять детей")),
            gap("I've got three ___.", "boxes", "boxs", "box", explain = "После x добавляем -es."),
            gap("There are two ___ in the garden.", "mice", "mouses", "mouse", explain = "Mouse — исключение: mice."),
            gap("The ___ are playing in the park.", "children", "childs", "childrens", explain = "Child — children."),
            gap("I brush my ___ every day.", "teeth", "tooths", "tooth", explain = "Tooth — teeth."),
            choose("I can see five babies.", "I can see five babys.", "I can see five baby.", explain = "Согласная + y → -ies."),
            choose("Look at those men!", "Look at those mans!", "Look at those mens!", explain = "Man — men."),
            order("У меня две собаки и три кошки.", "I have got two dogs and three cats", "dog", "cat"),
        ),
        topic(
            "a_an", 2, "Артикли a и an",
            "Перед одним предметом ставим a или an. An — если слово начинается с гласного звука: an apple, an egg, an elephant, an orange. " +
                "В остальных случаях — a: a cat, a banana, a dog. Перед словами во множественном числе a и an не ставим.",
            listOf(ex("an apple", "яблоко"), ex("a banana", "банан"), ex("an elephant", "слон")),
            gap("I've got ___ apple.", "an", "a", "two", explain = "Apple начинается с гласной — an."),
            gap("It's ___ big dog.", "a", "an", "are", explain = "Big начинается с согласной — a."),
            gap("She has got ___ orange cat.", "an", "a", "is", explain = "Orange начинается с гласной — an."),
            choose("This is an elephant.", "This is a elephant.", "This is an elephants.", explain = "Elephant — гласная в начале, an."),
            choose("I can see two birds.", "I can see a two birds.", "I can see an two birds.", explain = "Во множественном числе a/an не нужен."),
            order("Это яйцо.", "It is an egg", "a", "eggs"),
        ),
        topic(
            "this_these", 2, "this, that, these, those",
            "This — это (рядом, один предмет), these — эти (рядом, много). That — то (далеко, один), those — те (далеко, много). " +
                "This is a book. These are books. That is a tree. Those are trees.",
            listOf(ex("This is my bag.", "Это моя сумка."), ex("These are my pencils.", "Это мои карандаши."), ex("Look at those birds!", "Посмотри на тех птиц!")),
            gap("___ are my toys.", "These", "This", "That", explain = "Toys — много, рядом: these."),
            gap("___ is my teddy bear.", "This", "These", "Those", explain = "Один предмет рядом: this."),
            gap("Look at ___ stars in the sky!", "those", "this", "that", explain = "Звёзды далеко и их много: those."),
            choose("These are my friends.", "These is my friends.", "This are my friends.", explain = "These + are."),
            order("Это мой новый велосипед.", "This is my new bike", "These", "are"),
            order("Те кошки очень милые.", "Those cats are very cute", "That", "is"),
        ),

        // ---------- 3 класс ----------
        topic(
            "possessive", 3, "my, his, her… и 's — чьё это",
            "Чьё? my — мой, your — твой, his — его, her — её, its — его/её (о животном или предмете), our — наш, their — их. " +
                "Чтобы сказать «папина машина», добавляем 's: Dad's car, Mary's cat.",
            listOf(ex("This is my mum.", "Это моя мама."), ex("Her name is Anna.", "Её зовут Анна."), ex("It's Tom's ball.", "Это мяч Тома.")),
            gap("This is Kate. ___ hair is long.", "Her", "His", "Its", explain = "Kate — она, значит her."),
            gap("My brother loves ___ bike.", "his", "her", "its", explain = "Brother — он, значит his."),
            gap("We love ___ school.", "our", "we", "their", explain = "We — наш: our."),
            choose("It's my sister's doll.", "It's my sisters doll.", "It's my sister doll.", explain = "Чей предмет — добавляем 's."),
            choose("The dog is wagging its tail.", "The dog is wagging it's tail.", "The dog is wagging his's tail.", explain = "Its — его (о животном), без апострофа."),
            order("Как зовут твоего брата?", "What is your brother's name", "you", "brother"),
        ),
        topic(
            "present_simple", 3, "Present Simple — обычно, всегда, каждый день",
            "Present Simple — про то, что бывает обычно. I/you/we/they play, а he/she/it — play + s: she plays, he watches, it goes. " +
                "Отрицание: don't + глагол (I don't like), doesn't + глагол без -s (she doesn't like). " +
                "Вопрос: Do you like…? Does she like…? — Yes, she does. / No, she doesn't.",
            listOf(ex("I go to school every day.", "Я хожу в школу каждый день."), ex("She likes ice cream.", "Она любит мороженое."), ex("Does he play football?", "Он играет в футбол?")),
            gap("My sister ___ milk every morning.", "drinks", "drink", "drinking", explain = "Sister — she, добавляем -s."),
            gap("I ___ like spiders.", "don't", "doesn't", "isn't", explain = "С I — don't."),
            gap("___ your dad work in an office?", "Does", "Do", "Is", explain = "Dad — he, вопрос с Does."),
            gap("Tom ___ play tennis. He plays football.", "doesn't", "don't", "isn't", explain = "Tom — he, отрицание doesn't."),
            choose("She doesn't like carrots.", "She don't like carrots.", "She doesn't likes carrots.", explain = "После doesn't глагол без -s."),
            choose("He watches TV in the evening.", "He watch TV in the evening.", "He watchs TV in the evening.", explain = "После ch добавляем -es: watches."),
            order("Мы играем в парке по воскресеньям.", "We play in the park on Sundays", "plays", "are"),
            order("Твой кот любит рыбу?", "Does your cat like fish", "Do", "likes"),
        ),
        topic(
            "there_is", 3, "there is / there are — есть, находится",
            "Когда говорим, что где-то что-то есть: There is — про один предмет (There is a lamp on the table), " +
                "There are — про много (There are two chairs in the room). Вопрос: Is there…? Are there…? Отрицание: There isn't / There aren't.",
            listOf(ex("There is a cat on the sofa.", "На диване кошка."), ex("There are five apples in the bag.", "В пакете пять яблок."), ex("Is there a park near your house?", "Рядом с твоим домом есть парк?")),
            gap("There ___ a big tree in our garden.", "is", "are", "am", explain = "Одно дерево — there is."),
            gap("There ___ three books on the shelf.", "are", "is", "be", explain = "Книг много — there are."),
            gap("___ there any milk in the fridge?", "Is", "Are", "Do", explain = "Milk не считается по штукам — Is there."),
            choose("There aren't any chairs in the kitchen.", "There isn't any chairs in the kitchen.", "There not are any chairs in the kitchen.", explain = "Chairs — много, there aren't."),
            order("В моей комнате есть кровать.", "There is a bed in my room", "are", "have"),
            order("В классе двадцать детей.", "There are twenty children in the class", "is", "childs"),
        ),
        topic(
            "present_continuous", 3, "Present Continuous — происходит сейчас",
            "Present Continuous — про то, что происходит прямо сейчас. am / is / are + глагол с -ing: I am reading. She is dancing. They are playing. " +
                "Если слово кончается на e, её убираем: dance — dancing. Короткие слова удваивают букву: swim — swimming, run — running.",
            listOf(ex("I am reading a book now.", "Я сейчас читаю книгу."), ex("She is dancing.", "Она танцует."), ex("Are they swimming?", "Они плавают?")),
            gap("Look! The baby ___ sleeping.", "is", "are", "am", explain = "Baby — it/she, значит is + -ing."),
            gap("We are ___ a cake now.", "making", "make", "makeing", explain = "Make → making (e убираем)."),
            gap("My friends are ___ in the pool.", "swimming", "swiming", "swim", explain = "Swim → swimming (удваиваем m)."),
            choose("She is watching TV now.", "She watching TV now.", "She is watch TV now.", explain = "Нужно и is, и -ing."),
            choose("What are you doing?", "What you are doing?", "What are you do?", explain = "Вопрос: are ставим перед you."),
            order("Мама сейчас готовит ужин.", "Mum is cooking dinner now", "cooks", "are"),
            order("Мы играем в футбол.", "We are playing football", "is", "plays"),
        ),
        topic(
            "prepositions", 3, "Предлоги места: in, on, under, next to…",
            "Где? in — в, внутри; on — на; under — под; next to — рядом с; behind — за, позади; in front of — перед; between — между. " +
                "The cat is under the table. The ball is in the box.",
            listOf(ex("The book is on the desk.", "Книга на парте."), ex("The dog is under the bed.", "Собака под кроватью."), ex("I sit next to Mary.", "Я сижу рядом с Мэри.")),
            gap("The fish is ___ the water.", "in", "on", "under", explain = "Рыба внутри воды — in."),
            gap("My bag is ___ the chair. I can't see it.", "under", "on", "in", explain = "Под стулом — under."),
            gap("The lamp is ___ the bed and the desk.", "between", "next", "in front", explain = "Между двумя предметами — between."),
            choose("The cat is in front of the door.", "The cat is in front the door.", "The cat is front of the door.", explain = "Целиком: in front of."),
            order("Мяч рядом с коробкой.", "The ball is next to the box", "on", "in"),
            order("Кошка за диваном.", "The cat is behind the sofa", "under", "are"),
        ),
        topic(
            "some_any", 3, "some и any",
            "Some — несколько, немного — в утвердительных предложениях: I've got some apples. " +
                "Any — в вопросах и отрицаниях: Have you got any milk? There isn't any juice.",
            listOf(ex("There are some eggs in the fridge.", "В холодильнике есть немного яиц."), ex("Have you got any sweets?", "У тебя есть конфеты?"), ex("We haven't got any bread.", "У нас нет хлеба.")),
            gap("I'd like ___ water, please.", "some", "any", "a", explain = "Утвердительное предложение — some."),
            gap("Are there ___ bananas?", "any", "some", "an", explain = "Вопрос — any."),
            gap("There isn't ___ cheese left.", "any", "some", "a", explain = "Отрицание — any."),
            choose("We need some flour for the cake.", "We need any flour for the cake.", "We need a flour for the cake.", explain = "В утверждении — some."),
            order("У меня нет никаких домашних животных.", "I haven't got any pets", "some", "has"),
        ),

        // ---------- 4 класс ----------
        topic(
            "frequency", 4, "always, usually, often, sometimes, never",
            "Как часто? always — всегда, usually — обычно, often — часто, sometimes — иногда, never — никогда. " +
                "Эти слова ставим перед основным глаголом (I always get up at 7), но после am/is/are (She is never late).",
            listOf(ex("I always brush my teeth.", "Я всегда чищу зубы."), ex("He is never late.", "Он никогда не опаздывает."), ex("We sometimes go to the cinema.", "Мы иногда ходим в кино.")),
            gap("I ___ eat breakfast at 8 o'clock. Every day!", "always", "never", "sometimes", explain = "Каждый день — always."),
            gap("My cat ___ swims. It hates water!", "never", "always", "often", explain = "Ненавидит воду — never."),
            choose("She usually walks to school.", "She walks usually to school.", "Usually she walk to school.", explain = "Usually — перед глаголом, а глагол с -s."),
            choose("Tom is always happy.", "Tom always is happy.", "Tom is happy always.", explain = "После is: is always."),
            order("Я иногда играю в шахматы.", "I sometimes play chess", "plays", "am"),
            order("Мой папа обычно работает дома.", "My dad usually works at home", "work", "is"),
        ),
        topic(
            "must", 4, "must / mustn't — надо и нельзя",
            "Must — «должен, надо»: You must wash your hands. Mustn't — «нельзя»: You mustn't run in the classroom. " +
                "Must одинаковое для всех, после него глагол без to.",
            listOf(ex("You must do your homework.", "Ты должна сделать домашнее задание."), ex("We mustn't shout in the library.", "В библиотеке нельзя кричать.")),
            gap("You ___ cross the road at a red light.", "mustn't", "must", "can", explain = "На красный нельзя — mustn't."),
            gap("I must ___ my room today.", "tidy", "to tidy", "tidies", explain = "После must глагол без to и без -s."),
            choose("She must go to bed early.", "She musts go to bed early.", "She must to go to bed early.", explain = "Must не меняется, to не нужен."),
            order("В классе нельзя есть.", "You mustn't eat in the classroom", "must", "to"),
            order("Мы должны помогать маме.", "We must help our mum", "helps", "to"),
        ),
        topic(
            "was_were", 4, "was / were — был, были",
            "Прошедшее время глагола to be: I, he, she, it — was; we, you, they — were. " +
                "Отрицание: wasn't, weren't. Вопрос: Were you at home yesterday? — Yes, I was.",
            listOf(ex("I was at the zoo yesterday.", "Я вчера была в зоопарке."), ex("They were happy.", "Они были счастливы."), ex("Was it cold?", "Было холодно?")),
            gap("Yesterday I ___ at my granny's.", "was", "were", "am", explain = "I в прошедшем — was."),
            gap("We ___ in the park last Sunday.", "were", "was", "are", explain = "We — were."),
            gap("___ you at school yesterday?", "Were", "Was", "Are", explain = "Вопрос с you — Were."),
            choose("The film wasn't funny.", "The film weren't funny.", "The film not was funny.", explain = "Film — it, отрицание wasn't."),
            order("Где ты была вчера?", "Where were you yesterday", "was", "are"),
            order("Погода была хорошая.", "The weather was nice", "were", "is"),
        ),
        topic(
            "past_simple", 4, "Past Simple — что было вчера",
            "Past Simple — про то, что было в прошлом (yesterday, last week). Правильные глаголы + -ed: play — played, watch — watched. " +
                "Неправильные надо запомнить: go — went, see — saw, have — had, eat — ate, do — did, make — made, swim — swam. " +
                "Отрицание и вопрос с did, а глагол — обычный: I didn't go. Did you see it?",
            listOf(ex("I played football yesterday.", "Вчера я играла в футбол."), ex("We went to the seaside.", "Мы ездили на море."), ex("Did you see the film?", "Ты видела фильм?")),
            gap("Last Sunday we ___ to the zoo.", "went", "goed", "go", explain = "Go — went (неправильный)."),
            gap("She ___ TV yesterday evening.", "watched", "watch", "watchs", explain = "Правильный глагол — -ed."),
            gap("I didn't ___ my homework.", "do", "did", "done", explain = "После didn't — обычная форма глагола."),
            gap("___ you have a nice holiday?", "Did", "Do", "Were", explain = "Вопрос в прошедшем — Did."),
            choose("We saw a big elephant.", "We seed a big elephant.", "We see a big elephant yesterday.", explain = "See — saw."),
            order("Вчера мы испекли торт.", "We made a cake yesterday", "make", "did"),
            order("Я не ходила в школу в понедельник.", "I didn't go to school on Monday", "went", "don't"),
        ),
        topic(
            "comparatives", 4, "Сравнение: bigger, the biggest",
            "Короткие слова: + -er (больше) и the + -est (самый): big — bigger — the biggest, tall — taller — the tallest. " +
                "Длинные слова: more / the most: beautiful — more beautiful — the most beautiful. " +
                "Исключения: good — better — the best, bad — worse — the worst. Сравниваем с помощью than: An elephant is bigger than a mouse.",
            listOf(ex("A giraffe is taller than a horse.", "Жираф выше лошади."), ex("This is the best day!", "Это лучший день!"), ex("My cat is fatter than your cat.", "Моя кошка толще твоей.")),
            gap("An elephant is ___ than a dog.", "bigger", "biger", "more big", explain = "Big — bigger (удваиваем g)."),
            gap("This is the ___ cake in the world!", "best", "goodest", "better", explain = "Good — better — the best."),
            gap("A cheetah is ___ than a turtle.", "faster", "fast", "more fast", explain = "Короткое слово — -er."),
            choose("My book is more interesting than yours.", "My book is interestinger than yours.", "My book is more interesting that yours.", explain = "Длинное слово — more, а сравнение — than."),
            order("Кит — самое большое животное.", "The whale is the biggest animal", "bigger", "most"),
            order("Мой брат выше меня.", "My brother is taller than me", "tallest", "then"),
        ),
        topic(
            "question_words", 4, "Вопросительные слова",
            "What — что, какой; Where — где, куда; When — когда; Who — кто; Why — почему; How — как; How many — сколько (штук); How old — сколько лет. " +
                "В вопросе сначала вопросительное слово, потом is/are/do/does/can: Where do you live? What is your name?",
            listOf(ex("Where do you live?", "Где ты живёшь?"), ex("How old are you?", "Сколько тебе лет?"), ex("Why are you sad?", "Почему ты грустишь?")),
            gap("___ is your birthday? — In May.", "When", "Where", "Who", explain = "Ответ про время — When."),
            gap("___ is your best friend? — Anna.", "Who", "What", "Where", explain = "Ответ — человек, значит Who."),
            gap("___ pencils have you got? — Five.", "How many", "How old", "What", explain = "Сколько штук — How many."),
            choose("Where does your granny live?", "Where your granny lives?", "Where does your granny lives?", explain = "Where + does + глагол без -s."),
            order("Как тебя зовут?", "What is your name", "Who", "are"),
            order("Почему ты смеёшься?", "Why are you laughing", "What", "is"),
        ),
    )

    fun topic(id: String): GrammarTopic? = topics.firstOrNull { it.id == id }

    /** Список тем для подсказки нейросети: «id — название». */
    fun catalog(): String = topics.joinToString("\n") { "${it.id} — ${it.title} (${it.grade} класс)" }
}
