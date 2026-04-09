package ru.techlabhub.speechrehab.domain.model

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
    val displayLabel: String get() = displayText.ifBlank { text }
}
