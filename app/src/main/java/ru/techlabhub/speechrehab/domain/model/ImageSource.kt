package ru.techlabhub.speechrehab.domain.model

/**
 * Источник изображения (расширяемо под новые провайдеры).
 */
enum class ImageSource {
    /** Встроенный asset (offline). */
    BUNDLED,
    /** Файл из файлового кэша / запись Room word_image_variants. */
    LOCAL_CACHE,
    ARASAAC,
    PIXABAY,
    PEXELS,
    /** Картинка недоступна; карточка показывает заглушку. */
    NONE,
}
