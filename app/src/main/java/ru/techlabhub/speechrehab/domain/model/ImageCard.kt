package ru.techlabhub.speechrehab.domain.model

/**
 * Карточка для экрана тренировки: слово + изображение (или отсутствие).
 *
 * @property imageUri Для Coil: `file://`, `content://`, `http(s)://` или `asset:///...`; `null` — показать заглушку.
 */
data class ImageCard(
    val word: WordItem,
    val imageUri: String?,
    val remoteUrl: String?,
    val source: ImageSource,
    val fromOfflineCache: Boolean,
    /** Строка в [word_image_variants] для отметки показа; null у bundled / без файла. */
    val wordImageVariantId: Long? = null,
)
