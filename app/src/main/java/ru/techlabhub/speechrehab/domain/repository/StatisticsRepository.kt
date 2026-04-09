package ru.techlabhub.speechrehab.domain.repository

import ru.techlabhub.speechrehab.domain.model.CategoryAggregate
import ru.techlabhub.speechrehab.domain.model.DailyStats
import ru.techlabhub.speechrehab.domain.model.TrendResult
import ru.techlabhub.speechrehab.domain.model.WeeklyStats
import ru.techlabhub.speechrehab.domain.model.WordRank

data class StatisticsSnapshot(
    val totalAttempts: Int,
    val totalCorrect: Int,
    val overallAccuracyPercent: Float,
    val daily: List<DailyStats>,
    val weekly: List<WeeklyStats>,
    val hardestWords: List<WordRank>,
    val easiestWords: List<WordRank>,
    val categoryProgress: List<CategoryAggregate>,
    val trend7: TrendResult,
    val trend14: TrendResult,
    val trend30: TrendResult,
)

interface StatisticsRepository {
    suspend fun loadSnapshot(): StatisticsSnapshot
}
