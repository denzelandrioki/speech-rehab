package ru.techlabhub.speechrehab.domain.model

/**
 * Агрегат по календарному дню (обычно вычисляется из попыток).
 */
data class DailyStats(
    val dayEpochDay: Long,
    val attempts: Int,
    val correct: Int,
) {
    val accuracyPercent: Float
        get() = if (attempts == 0) 0f else (correct * 100f) / attempts
}
