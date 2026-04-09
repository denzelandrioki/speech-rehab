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
        val low = setOf(3L)
        val out = CardWeightEngine.filterByMode(words, TrainingMode.HARD_WORDS, hard, low)
        assertEquals(1, out.size)
        assertEquals(2L, out.first().id)
    }
}
