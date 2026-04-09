package ru.techlabhub.speechrehab.domain.model

/**
 * Режим подбора карточек (настройки + use case).
 */
enum class TrainingMode {
    RANDOM,
    BY_CATEGORY,
    HARD_WORDS,
    NEW_ONLY,
    MIXED,
}
