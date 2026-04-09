package ru.techlabhub.speechrehab.domain.model

/**
 * Карточка для экрана тренировки: слово + изображение.
 *
 * @property imageUri То, что отдаёт Coil: абсолютный путь к файлу кэша или `http(s)://…`.
 * @property remoteUrl Исходный URL картинки (для отладки и при отображении без файла).
 * @property source Откуда взята картинка (кэш / ARASAAC / Pixabay / Pexels).
 * @property fromOfflineCache true, если показан локальный файл из предыдущего скачивания.
 */
data class ImageCard(
    val word: WordItem,
    val imageUri: String,
    val remoteUrl: String?,
    val source: ImageSource,
    val fromOfflineCache: Boolean,
)
