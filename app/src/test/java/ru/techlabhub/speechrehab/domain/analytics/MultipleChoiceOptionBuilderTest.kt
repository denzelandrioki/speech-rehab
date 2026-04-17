package ru.techlabhub.speechrehab.domain.analytics

import ru.techlabhub.speechrehab.domain.model.WordItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class MultipleChoiceOptionBuilderTest {
    private fun word(
        id: Long,
        categoryId: Long = 1L,
    ) = WordItem(
        id = id,
        text = "w$id",
        displayTextRu = "р$id",
        displayTextEn = "e$id",
        categoryId = categoryId,
        categoryName = "c$categoryId",
        enabled = true,
        isCustom = false,
    )

    @Test
    fun returnsNull_whenPoolSmallerThanFour() {
        val correct = word(1)
        val pool = listOf(correct, word(2), word(3))
        assertNull(MultipleChoiceOptionBuilder.buildOptions(correct, pool, Random(0)))
    }

    @Test
    fun returnsNull_whenCorrectNotInPool() {
        val correct = word(99)
        val pool = (1L..4L).map { word(it) }
        assertNull(MultipleChoiceOptionBuilder.buildOptions(correct, pool, Random(0)))
    }

    @Test
    fun returnsFourDistinctIds_includingCorrect() {
        val correct = word(1, categoryId = 1)
        val pool = listOf(correct) + (2L..6L).map { word(it, if (it <= 3) 1 else 2) }
        val r = MultipleChoiceOptionBuilder.buildOptions(correct, pool, Random(42))
        assertNotNull(r)
        assertEquals(4, r!!.size)
        assertEquals(4, r.distinctBy { it.id }.size)
        assertTrue(r.any { it.id == correct.id })
    }

    @Test
    fun deterministic_withFixedRandom() {
        val correct = word(10, categoryId = 5)
        val pool = (10L..20L).map { word(it, categoryId = if (it <= 13) 5 else 9) }
        val a = MultipleChoiceOptionBuilder.buildOptions(correct, pool, Random(7))!!.map { it.id }.toSet()
        val b = MultipleChoiceOptionBuilder.buildOptions(correct, pool, Random(7))!!.map { it.id }.toSet()
        assertEquals(a, b)
    }
}
