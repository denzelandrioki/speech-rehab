package ru.techlabhub.speechrehab.domain.model

/**
 * Слово из словаря для тренировки и отображения.
 *
 * @property text Лексема на английском — используется при запросах к ARASAAC/Pixabay/Pexels.
 * @property displayText Русская подпись; если пусто, в UI используется [text].
 * @property consecutiveCorrect Серия подряд верных ответов (дублирует поле в БД для весов).
 * @property consecutiveIncorrect Серия подряд неверных ответов.
 */
data class WordItem(
    val id: Long,
    /** Английское слово для поиска изображений по API. */
    val text: String,
    /** Русская подпись для интерфейса; если пусто — показывается [text]. */
    val displayText: String = "",
    val categoryId: Long,
    val categoryName: String,
    val enabled: Boolean,
    val isCustom: Boolean,
    val consecutiveCorrect: Int = 0,
    val consecutiveIncorrect: Int = 0,
) {
    /** Текст, который видит пользователь: русский или, при отсутствии, [text]. */
    val displayLabel: String get() = displayText.ifBlank { text }
}
