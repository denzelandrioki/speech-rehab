package ru.techlabhub.speechrehab.domain.analytics

import ru.techlabhub.speechrehab.domain.model.TrendDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StatisticsEngineTest {
    @Test
    fun accuracyPercent_zeroAttempts() {
        assertEquals(0f, StatisticsEngine.accuracyPercent(0, 0), 0.001f)
    }

    @Test
    fun accuracyPercent_half() {
        assertEquals(50f, StatisticsEngine.accuracyPercent(1, 2), 0.001f)
    }

    @Test
    fun trend_improving() {
        val first = List(10) { false } + List(10) { true }
        val t = StatisticsEngine.trendFromOrderedResults(first, windowDays = 7, stableEpsilonPercent = 5f)
        assertEquals(TrendDirection.IMPROVING, t.direction)
    }

    @Test
    fun weeklyBuckets_groupsByMonday() {
        val monday = java.time.LocalDate.of(2026, 4, 6).toEpochDay() // Monday
        val tuesday = monday + 1
        val weekly =
            StatisticsEngine.weeklyBucketsFromDaily(
                listOf(
                    Triple(monday, 2, 1),
                    Triple(tuesday, 2, 2),
                ),
            )
        assertTrue(weekly.size == 1)
        assertEquals(4, weekly[0].second) // attempts
        assertEquals(3, weekly[0].third) // correct
    }
}
