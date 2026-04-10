package ru.techlabhub.speechrehab.domain.model

/**
 * Порядок приоритета локальных и удалённых источников изображений.
 * Расширяемо под prefetch / backend без смены публичного контракта.
 */
enum class PreferredImageMode {
    BUNDLED_FIRST,
    CACHED_FIRST,
    LOCAL_ONLY,
    LOCAL_THEN_REMOTE,
}
