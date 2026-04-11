package ru.techlabhub.speechrehab.domain.analytics

import ru.techlabhub.speechrehab.domain.model.TrainingMode
import ru.techlabhub.speechrehab.domain.model.WordItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class CardWeightEngineTest {
    private fun word(
        id: Long,
        cc: Int = 0,
        ci: Int = 0,
    ) = WordItem(
        id = id,
        text = "w$id",
        categoryId = 1L,
        categoryName = "c",
        enabled = true,
        isCustom = false,
        consecutiveCorrect = cc,
        consecutiveIncorrect = ci,
    )

    @Test
    fun weight_increasesWithIncorrectHistory() {
        val wLow = CardWeightEngine.computeWeight(word(1), incorrectAttemptsEstimate = 0)
        val wHigh = CardWeightEngine.computeWeight(word(1), incorrectAttemptsEstimate = 5)
        assertTrue(wHigh > wLow)
    }

    @Test
    fun pickWeighted_deterministicWithFixedSeed() {
        val items =
            listOf(
                CardWeightEngine.WeightedWord(word(1), 1f),
                CardWeightEngine.WeightedWord(word(2), 0f),
            )
        val rnd = Random(42)
        val picked = CardWeightEngine.pickWeighted(items, rnd)
        assertEquals(1L, picked?.id)
    }

    @Test
    fun filter_hardMode_prefersHardPool() {
        val words = listOf(word(1), word(2), word(3))
        val hard = setOf(2L)
        val out =
            CardWeightEngine.filterByMode(
                words,
                TrainingMode.HARD_WORDS,
                hardWordIds = hard,
                freshWordIds = emptySet(),
                zeroAttemptWordIds = emptySet(),
            )
        assertEquals(1, out.size)
        assertEquals(2L, out.first().id)
    }

    @Test
    fun filter_newOnly_strictZeroAttempts_noFallbackWhenEmpty() {
        val words = listOf(word(1), word(2))
        val out =
            CardWeightEngine.filterByMode(
                words,
                TrainingMode.NEW_ONLY,
                hardWordIds = emptySet(),
                freshWordIds = setOf(1L, 2L),
                zeroAttemptWordIds = emptySet(),
            )
        assertTrue(out.isEmpty())
    }

    @Test
    fun filter_newOnly_keepsOnlyZeroAttemptIds() {
        val words = listOf(word(1), word(2), word(3))
        val out =
            CardWeightEngine.filterByMode(
                words,
                TrainingMode.NEW_ONLY,
                hardWordIds = emptySet(),
                freshWordIds = setOf(1L, 2L),
                zeroAttemptWordIds = setOf(2L),
            )
        assertEquals(1, out.size)
        assertEquals(2L, out.first().id)
    }

    @Test
    fun filter_freshWords_fallbackWhenNoFresh() {
        val words = listOf(word(1), word(2))
        val out =
            CardWeightEngine.filterByMode(
                words,
                TrainingMode.FRESH_WORDS,
                hardWordIds = emptySet(),
                freshWordIds = emptySet(),
                zeroAttemptWordIds = emptySet(),
            )
        assertEquals(2, out.size)
    }
}
