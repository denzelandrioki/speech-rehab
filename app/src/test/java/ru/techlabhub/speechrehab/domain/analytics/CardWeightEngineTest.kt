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
                freshAttemptIds = emptySet(),
                zeroAttemptWordIds = emptySet(),
            )
        assertEquals(1, out.size)
        assertEquals(2L, out.first().id)
    }

    @Test
    fun filter_newOnly_emptyZeroSet_noFallbackToFullPool() {
        val words = listOf(word(1), word(2))
        val out =
            CardWeightEngine.filterByMode(
                words,
                TrainingMode.NEW_ONLY,
                hardWordIds = emptySet(),
                freshAttemptIds = setOf(1L, 2L),
                zeroAttemptWordIds = emptySet(),
            )
        assertTrue(out.isEmpty())
    }

    @Test
    fun filter_newOnly_keepsOnlyZeroAttemptIds_notFreshWiderSet() {
        val words = listOf(word(1), word(2), word(3))
        // id 2 — «новое» (0 попыток); id 1 и 3 — только во fresh (например 1 попытка), не в zero
        val zero = setOf(2L)
        val fresh = setOf(1L, 2L, 3L)
        val out =
            CardWeightEngine.filterByMode(
                words,
                TrainingMode.NEW_ONLY,
                hardWordIds = emptySet(),
                freshAttemptIds = fresh,
                zeroAttemptWordIds = zero,
            )
        assertEquals(1, out.size)
        assertEquals(2L, out.first().id)
    }

    @Test
    fun filter_newOnly_excludesIdThatIsOnlyInFreshAttemptIds() {
        val words = listOf(word(1), word(2))
        val zero = setOf(1L)
        val fresh = setOf(1L, 2L)
        val out =
            CardWeightEngine.filterByMode(
                words,
                TrainingMode.NEW_ONLY,
                hardWordIds = emptySet(),
                freshAttemptIds = fresh,
                zeroAttemptWordIds = zero,
            )
        assertEquals(1, out.size)
        assertEquals(1L, out.first().id)
    }

    @Test
    fun filter_freshWords_keepsZeroAndOneAttemptIds() {
        val words = listOf(word(1), word(2), word(3))
        val fresh = setOf(1L, 2L)
        val out =
            CardWeightEngine.filterByMode(
                words,
                TrainingMode.FRESH_WORDS,
                hardWordIds = emptySet(),
                freshAttemptIds = fresh,
                zeroAttemptWordIds = setOf(1L),
            )
        assertEquals(2, out.size)
        assertTrue(out.map { it.id }.toSet() == setOf(1L, 2L))
    }

    @Test
    fun filter_freshWords_fallbackWhenNoFresh() {
        val words = listOf(word(1), word(2))
        val out =
            CardWeightEngine.filterByMode(
                words,
                TrainingMode.FRESH_WORDS,
                hardWordIds = emptySet(),
                freshAttemptIds = emptySet(),
                zeroAttemptWordIds = emptySet(),
            )
        assertEquals(2, out.size)
    }
}
