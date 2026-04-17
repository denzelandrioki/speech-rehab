package ru.techlabhub.speechrehab.data.repository

import ru.techlabhub.speechrehab.data.local.SpeechRehabDatabase
import ru.techlabhub.speechrehab.data.local.dao.WordAggregateRow
import ru.techlabhub.speechrehab.domain.analytics.StatisticsEngine
import ru.techlabhub.speechrehab.domain.model.CategoryAggregate
import ru.techlabhub.speechrehab.domain.model.ConfusionPairStat
import ru.techlabhub.speechrehab.domain.model.DailyStats
import ru.techlabhub.speechrehab.domain.model.MultipleChoiceStatsSnapshot
import ru.techlabhub.speechrehab.domain.model.TrendResult
import ru.techlabhub.speechrehab.domain.model.TrainingTextLanguage
import ru.techlabhub.speechrehab.domain.model.WeeklyStats
import ru.techlabhub.speechrehab.domain.model.WordDisplayFormatter
import ru.techlabhub.speechrehab.domain.model.WordRank
import ru.techlabhub.speechrehab.domain.model.WrongSelectionStat
import ru.techlabhub.speechrehab.domain.repository.StatisticsRepository
import ru.techlabhub.speechrehab.domain.repository.StatisticsSnapshot
import ru.techlabhub.speechrehab.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Собирает [StatisticsSnapshot]: assisted + отдельный блок multiple choice.
 */
@Singleton
class DefaultStatisticsRepository @Inject constructor(
    db: SpeechRehabDatabase,
    private val userPreferencesRepository: UserPreferencesRepository,
) : StatisticsRepository {
    private val attempts = db.answerAttemptDao()
    private val mc = db.multipleChoiceAttemptDao()
    private val words = db.wordDao()

    override suspend fun loadSnapshot(): StatisticsSnapshot {
        val cardTextMode =
            userPreferencesRepository.preferencesFlow.first().trainingTextLanguage

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

        val cat =
            attempts.byCategory().map { c ->
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

        val mcSnap = loadMultipleChoiceSnapshot(now, cardTextMode)

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
            multipleChoice = mcSnap,
        )
    }

    private suspend fun loadMultipleChoiceSnapshot(
        now: Long,
        cardTextMode: TrainingTextLanguage,
    ): MultipleChoiceStatsSnapshot? {
        val totals = mc.overallTotals() ?: return null
        val totalAttempts = totals.attempts?.toInt() ?: 0
        if (totalAttempts == 0) return null
        val totalCorrect = totals.correct?.toInt() ?: 0
        val overall = StatisticsEngine.accuracyPercent(totalCorrect, totalAttempts)
        val avg = mc.averageResponseTimeMillis()?.toFloat()

        val dailyRows = mc.dailyAggregatesLastDays(60).reversed()
        val daily =
            dailyRows.map { r ->
                DailyStats(
                    dayEpochDay = r.dayEpoch,
                    attempts = r.attempts,
                    correct = r.correct.toInt(),
                )
            }

        val hardest = mc.hardestWords(minAttempts = 2, limit = 8).map { it.toRank() }
        val cat =
            mc.byCategory().map { c ->
                CategoryAggregate(
                    categoryId = c.categoryId,
                    name = c.categoryName,
                    attempts = c.attempts,
                    accuracyPercent = StatisticsEngine.accuracyPercent(c.correct.toInt(), c.attempts),
                )
            }

        val trend7Mc = mcTrend(now, 7)
        val confusion =
            mc.topConfusionPairs(12).map { row ->
                ConfusionPairStat(
                    correctLabel =
                        WordDisplayFormatter.formatRank(
                            row.correctCanonical,
                            row.correctRu,
                            row.correctEn,
                            cardTextMode,
                        ),
                    wrongLabel =
                        WordDisplayFormatter.formatRank(
                            row.wrongCanonical,
                            row.wrongRu,
                            row.wrongEn,
                            cardTextMode,
                        ),
                    count = row.cnt,
                )
            }
        val wrongSelections =
            mc.topWrongSelections(8).mapNotNull { row ->
                val w = words.getById(row.wordId) ?: return@mapNotNull null
                WrongSelectionStat(
                    label =
                        WordDisplayFormatter.formatRank(
                            w.text,
                            w.displayTextRu,
                            w.displayTextEn,
                            cardTextMode,
                        ),
                    count = row.cnt,
                )
            }

        return MultipleChoiceStatsSnapshot(
            totalAttempts = totalAttempts,
            totalCorrect = totalCorrect,
            accuracyPercent = overall,
            avgResponseTimeMillis = avg,
            daily = daily,
            hardestWords = hardest,
            categoryProgress = cat,
            trend7 = trend7Mc,
            confusionPairs = confusion,
            topWrongSelections = wrongSelections,
        )
    }

    private suspend fun trend(
        now: Long,
        days: Int,
    ): TrendResult {
        val from = now - days.toLong() * 86_400_000L
        val list = attempts.resultsInWindow(from)
        return StatisticsEngine.trendFromOrderedResults(list, windowDays = days)
    }

    private suspend fun mcTrend(
        now: Long,
        days: Int,
    ): TrendResult {
        val from = now - days.toLong() * 86_400_000L
        val list = mc.resultsInWindow(from)
        return StatisticsEngine.trendFromOrderedResults(list, windowDays = days)
    }

    private fun WordAggregateRow.toRank(): WordRank {
        val acc = StatisticsEngine.accuracyPercent(correct.toInt(), attempts)
        return WordRank(
            wordId = wordId,
            canonicalText = canonicalText,
            displayTextRu = displayTextRu,
            displayTextEn = displayTextEn,
            attempts = attempts,
            accuracyPercent = acc,
        )
    }
}
