package ru.techlabhub.speechrehab.domain.analytics

import ru.techlabhub.speechrehab.domain.model.TrainingMode
import ru.techlabhub.speechrehab.domain.model.WordItem
import kotlin.random.Random

/**
 * Взвешенный выбор следующего слова. Позже можно заменить стратегию через интерфейс.
 */
object CardWeightEngine {

    data class WeightedWord(
        val word: WordItem,
        val weight: Float,
    )
    fun computeWeight(
        word: WordItem,
        incorrectAttemptsEstimate: Int,
        baseWeight: Float = 10f,
        incorrectFactor: Float = 2f,
        correctStreakFactor: Float = 1f,
        newWordBoost: Float = 6f,
    ): Float {
        val incorrectComponent = incorrectAttemptsEstimate.coerceAtLeast(0) * incorrectFactor
        val streakPenalty = word.consecutiveCorrect.coerceAtLeast(0) * correctStreakFactor
        val wrongStreakBoost = word.consecutiveIncorrect.coerceAtLeast(0) * (incorrectFactor * 1.5f)
        val newBoost = if (incorrectAttemptsEstimate == 0) newWordBoost else 0f
        return (baseWeight + incorrectComponent + wrongStreakBoost + newBoost - streakPenalty)
            .coerceAtLeast(1f)
    }

    fun pickWeighted(
        items: List<WeightedWord>,
        random: Random = Random.Default,
    ): WordItem? {
        if (items.isEmpty()) return null
        val total = items.sumOf { it.weight.toDouble() }.toFloat()
        if (total <= 0f) return items.random(random).word
        var r = random.nextFloat() * total
        for (it in items) {
            r -= it.weight
            if (r <= 0f) return it.word
        }
        return items.last().word
    }

    fun filterByMode(
        words: List<WordItem>,
        mode: TrainingMode,
        hardWordIds: Set<Long>,
        lowAttemptWordIds: Set<Long>,
    ): List<WordItem> {
        return when (mode) {
            TrainingMode.RANDOM, TrainingMode.BY_CATEGORY -> words
            TrainingMode.HARD_WORDS -> words.filter { it.id in hardWordIds }.ifEmpty { words }
            TrainingMode.NEW_ONLY -> words.filter { it.id in lowAttemptWordIds }.ifEmpty { words }
            TrainingMode.MIXED -> {
                val hard = words.filter { it.id in hardWordIds }
                val pool = (hard + words).distinctBy { it.id }
                if (pool.isEmpty()) words else pool
            }
        }
    }
}
