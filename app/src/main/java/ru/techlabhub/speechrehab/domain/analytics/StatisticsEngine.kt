package ru.techlabhub.speechrehab.domain.analytics

import ru.techlabhub.speechrehab.domain.model.TrendDirection
import ru.techlabhub.speechrehab.domain.model.TrendResult
import kotlin.math.abs

/**
 * Чистые функции для тестов: тренд по упорядоченному списку результатов (true=верно).
 */
object StatisticsEngine {
    fun accuracyPercent(correct: Int, attempts: Int): Float =
        if (attempts <= 0) 0f else (correct * 100f) / attempts

    fun trendFromOrderedResults(
        orderedResults: List<Boolean>,
        windowDays: Int,
        stableEpsilonPercent: Float = 3f,
    ): TrendResult {
        if (orderedResults.size < 4) {
            return TrendResult(
                windowDays = windowDays,
                direction = TrendDirection.STABLE,
                firstHalfAccuracy = 0f,
                secondHalfAccuracy = 0f,
            )
        }
        val mid = orderedResults.size / 2
        val first = orderedResults.take(mid)
        val second = orderedResults.drop(mid)
        val a1 = accuracyPercent(first.count { it }, first.size)
        val a2 = accuracyPercent(second.count { it }, second.size)
        val delta = a2 - a1
        val direction =
            when {
                delta > stableEpsilonPercent -> TrendDirection.IMPROVING
                delta < -stableEpsilonPercent -> TrendDirection.DECLINING
                else -> TrendDirection.STABLE
            }
        return TrendResult(
            windowDays = windowDays,
            direction = direction,
            firstHalfAccuracy = a1,
            secondHalfAccuracy = a2,
        )
    }

    fun weeklyBucketsFromDaily(
        daily: List<Triple<Long, Int, Int>>,
    ): List<Triple<Long, Int, Int>> {
        val map = linkedMapOf<Long, Pair<Int, Int>>()
        daily.forEach { (day, att, cor) ->
            val weekStart = weekStartEpochDay(day)
            val cur = map.getOrDefault(weekStart, 0 to 0)
            map[weekStart] = (cur.first + att) to (cur.second + cor)
        }
        return map.entries.map { (k, v) -> Triple(k, v.first, v.second) }
            .sortedBy { it.first }
    }

    fun weekStartEpochDay(epochDay: Long): Long {
        val day = java.time.LocalDate.ofEpochDay(epochDay)
        val monday = day.with(java.time.DayOfWeek.MONDAY)
        return monday.toEpochDay()
    }

    fun isMostlyStable(trends: List<TrendResult>): Boolean =
        trends.all { it.direction == TrendDirection.STABLE || abs(it.secondHalfAccuracy - it.firstHalfAccuracy) < 1f }
}
