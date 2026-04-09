package ru.techlabhub.speechrehab.data.local.seed

/**
 * Предустановленный словарь для первого запуска и сидирования Room.
 *
 * - [WordSeed.textEn] — запрос к ARASAAC/Pixabay/Pexels; [WordSeed.textRu] — подпись в UI и колонка `displayText` в БД.
 * - Внешних «свободных» списков нет: только этот контролируемый набор категорий и слов.
 * - [englishToRussianMap] используется при миграции БД с версии 1 на 2 (колонка русской подписи).
 */
object VocabularyCatalog {
    data class WordSeed(val textEn: String, val textRu: String)

    data class CategorySeed(val name: String, val words: List<WordSeed>)

    val categories: List<CategorySeed> =
        listOf(
            CategorySeed(
                "Furniture",
                listOf(
                    WordSeed("table", "стол"),
                    WordSeed("chair", "стул"),
                    WordSeed("bed", "кровать"),
                    WordSeed("sofa", "диван"),
                    WordSeed("wardrobe", "шкаф"),
                ),
            ),
            CategorySeed(
                "Home",
                listOf(
                    WordSeed("house", "дом"),
                    WordSeed("window", "окно"),
                    WordSeed("door", "дверь"),
                    WordSeed("roof", "крыша"),
                    WordSeed("kitchen", "кухня"),
                ),
            ),
            CategorySeed(
                "Nature",
                listOf(
                    WordSeed("sky", "небо"),
                    WordSeed("sun", "солнце"),
                    WordSeed("cloud", "облако"),
                    WordSeed("tree", "дерево"),
                    WordSeed("grass", "трава"),
                    WordSeed("flower", "цветок"),
                ),
            ),
            CategorySeed(
                "Animals",
                listOf(
                    WordSeed("cat", "кошка"),
                    WordSeed("dog", "собака"),
                    WordSeed("horse", "лошадь"),
                    WordSeed("cow", "корова"),
                    WordSeed("bird", "птица"),
                    WordSeed("fish", "рыба"),
                ),
            ),
            CategorySeed(
                "Food",
                listOf(
                    WordSeed("apple", "яблоко"),
                    WordSeed("bread", "хлеб"),
                    WordSeed("milk", "молоко"),
                    WordSeed("soup", "суп"),
                    WordSeed("tea", "чай"),
                ),
            ),
            CategorySeed(
                "Transport",
                listOf(
                    WordSeed("car", "машина"),
                    WordSeed("bus", "автобус"),
                    WordSeed("train", "поезд"),
                    WordSeed("bicycle", "велосипед"),
                ),
            ),
            CategorySeed(
                "Household",
                listOf(
                    WordSeed("spoon", "ложка"),
                    WordSeed("cup", "чашка"),
                    WordSeed("plate", "тарелка"),
                    WordSeed("phone", "телефон"),
                    WordSeed("book", "книга"),
                ),
            ),
        )

    /** Для миграции БД: английское слово → русская подпись. */
    fun englishToRussianMap(): Map<String, String> =
        categories
            .asSequence()
            .flatMap { it.words.asSequence() }
            .associate { it.textEn to it.textRu }
}
