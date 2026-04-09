package ru.techlabhub.speechrehab.domain.model

/**
 * Карточка для экрана тренировки: слово + разрешённый для показа URI (сеть или файл).
 */
data class ImageCard(
    val word: WordItem,
    val imageUri: String,
    val remoteUrl: String?,
    val source: ImageSource,
    val fromOfflineCache: Boolean,
)
