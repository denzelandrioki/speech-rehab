package ru.techlabhub.speechrehab.domain.model

/**
 * Слово из словаря для тренировки и отображения.
 *
 * @property text Канонический ключ / строка для поиска изображений (англ.).
 * @property displayTextRu Русская подпись.
 * @property displayTextEn Английская подпись для карточки (часто совпадает с [text]).
 * @property bundledAssetName Имя файла в assets (например `bundled/table.png`), без префикса `assets/`.
 */
data class WordItem(
    val id: Long,
    val text: String,
    val displayTextRu: String = "",
    val displayTextEn: String = "",
    val bundledAssetName: String = "",
    val categoryId: Long,
    val categoryName: String,
    val enabled: Boolean,
    val isCustom: Boolean,
    val consecutiveCorrect: Int = 0,
    val consecutiveIncorrect: Int = 0,
)
