package ru.techlabhub.speechrehab.data.repository

import ru.techlabhub.speechrehab.data.local.SpeechRehabDatabase
import ru.techlabhub.speechrehab.data.local.dao.WordAggregateRow
import ru.techlabhub.speechrehab.domain.analytics.StatisticsEngine
import ru.techlabhub.speechrehab.domain.model.CategoryAggregate
import ru.techlabhub.speechrehab.domain.model.DailyStats
import ru.techlabhub.speechrehab.domain.model.TrendResult
import ru.techlabhub.speechrehab.domain.model.WeeklyStats
import ru.techlabhub.speechrehab.domain.model.WordRank
import ru.techlabhub.speechrehab.domain.repository.StatisticsRepository
import ru.techlabhub.speechrehab.domain.repository.StatisticsSnapshot
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultStatisticsRepository @Inject constructor(
    db: SpeechRehabDatabase,
) : StatisticsRepository {
    private val attempts = db.answerAttemptDao()

    override suspend fun loadSnapshot(): StatisticsSnapshot {
        val totals = attempts.overallTotals()
        val totalAttempts = totals?.attempts?.toInt() ?: 0
        val totalCorrect = totals?.correct?.toInt() ?: 0
        val overall = StatisticsEngine.accuracyPercent(totalCorrect, totalAttempts)

        val dailyRows = attempts.dailyAggregatesLastDays(60).reversed()
        val daily =
            dailyRows.map { r ->
                DailyStats(
                    dayEpochDay = r.dayEpoch,
                    attempts = r.attempts,
                    correct = r.correct.toInt(),
                )
            }

        val weeklyTriples =
            StatisticsEngine.weeklyBucketsFromDaily(
                dailyRows.map { Triple(it.dayEpoch, it.attempts, it.correct.toInt()) },
            )
        val weekly =
            weeklyTriples.map { (weekStart, att, cor) ->
                WeeklyStats(
                    weekStartEpochDay = weekStart,
                    attempts = att,
                    correct = cor,
                )
            }

        val hardest = attempts.hardestWords(minAttempts = 2, limit = 12)
        val easiest = attempts.easiestWords(minAttempts = 2, limit = 12)

        val cat = attempts.byCategory().map { c ->
            CategoryAggregate(
                categoryId = c.categoryId,
                name = c.categoryName,
                attempts = c.attempts,
                accuracyPercent = StatisticsEngine.accuracyPercent(c.correct.toInt(), c.attempts),
            )
        }

        val now = System.currentTimeMillis()
        val trend7 = trend(now, 7)
        val trend14 = trend(now, 14)
        val trend30 = trend(now, 30)

        return StatisticsSnapshot(
            totalAttempts = totalAttempts,
            totalCorrect = totalCorrect,
            overallAccuracyPercent = overall,
            daily = daily,
            weekly = weekly,
            hardestWords = hardest.map { it.toRank() },
            easiestWords = easiest.map { it.toRank() },
            categoryProgress = cat,
            trend7 = trend7,
            trend14 = trend14,
            trend30 = trend30,
        )
    }

    private suspend fun trend(now: Long, days: Int): TrendResult {
        val from = now - days.toLong() * 86_400_000L
        val list = attempts.resultsInWindow(from)
        return StatisticsEngine.trendFromOrderedResults(list, windowDays = days)
    }

    private fun WordAggregateRow.toRank(): WordRank {
        val acc = StatisticsEngine.accuracyPercent(correct.toInt(), attempts)
        return WordRank(
            wordId = wordId,
            text = wordText,
            attempts = attempts,
            accuracyPercent = acc,
        )
    }
}
