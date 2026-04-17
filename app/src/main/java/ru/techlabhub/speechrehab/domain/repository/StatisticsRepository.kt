package ru.techlabhub.speechrehab.domain.repository

import ru.techlabhub.speechrehab.domain.model.CategoryAggregate
import ru.techlabhub.speechrehab.domain.model.DailyStats
import ru.techlabhub.speechrehab.domain.model.MultipleChoiceStatsSnapshot
import ru.techlabhub.speechrehab.domain.model.TrendResult
import ru.techlabhub.speechrehab.domain.model.WeeklyStats
import ru.techlabhub.speechrehab.domain.model.WordRank

/** Снимок данных для экрана статистики: одним объектом передаётся всё необходимое для отрисовки. */
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
    /** null, если ещё нет попыток в multiple choice. */
    val multipleChoice: MultipleChoiceStatsSnapshot? = null,
)

/** Загрузка агрегированной статистики из БД (реализация считает SQL и тренды в домене). */
interface StatisticsRepository {
    suspend fun loadSnapshot(): StatisticsSnapshot
}
