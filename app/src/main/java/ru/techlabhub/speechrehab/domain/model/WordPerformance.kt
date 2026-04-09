package ru.techlabhub.speechrehab.domain.model

/**
 * Сводка по слову для аналитики и взвешенного подбора.
 */
data class WordPerformance(
    val wordId: Long,
    val wordText: String,
    val categoryId: Long,
    val attempts: Int,
    val correctCount: Int,
    val incorrectCount: Int,
    val correctStreak: Int,
)
