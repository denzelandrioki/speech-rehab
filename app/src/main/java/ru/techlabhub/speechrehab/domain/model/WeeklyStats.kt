package ru.techlabhub.speechrehab.domain.model

/**
 * Агрегат по ISO-неделе (год + weekOfYear) или скользящим 7 дням — здесь фиксированная неделя.
 */
data class WeeklyStats(
    val weekStartEpochDay: Long,
    val attempts: Int,
    val correct: Int,
) {
    val accuracyPercent: Float
        get() = if (attempts == 0) 0f else (correct * 100f) / attempts
}
