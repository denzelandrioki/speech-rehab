package ru.techlabhub.speechrehab.domain.model

enum class TrendDirection {
    IMPROVING,
    STABLE,
    DECLINING,
}

data class TrendResult(
    val windowDays: Int,
    val direction: TrendDirection,
    val firstHalfAccuracy: Float,
    val secondHalfAccuracy: Float,
)
