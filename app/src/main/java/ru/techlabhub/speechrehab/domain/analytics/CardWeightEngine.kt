package ru.techlabhub.speechrehab.domain.analytics

import ru.techlabhub.speechrehab.domain.model.TrainingMode
import ru.techlabhub.speechrehab.domain.model.WordItem
import kotlin.random.Random

/**
 * Движок весов для подбора следующего слова в тренировке.
 *
 * - [computeWeight] — чем выше вес, тем чаще слово попадёт в выбор (при [pickWeighted]).
 * - [pickWeighted] — классический roulette wheel по списку весов.
 * - [filterByMode] — сужает пул по [TrainingMode] (например, только «сложные» или только «без попыток»).
 *
 * При необходимости стратегию можно вынести за интерфейс без смены остального кода.
 */
object CardWeightEngine {

    /** Пара «слово → положительный вес» для взвешенной случайной выборки. */
    data class WeightedWord(
        val word: WordItem,
        val weight: Float,
    )

    /**
     * Считает вес слова: учитывает оценку числа ошибок, серии верных/неверных ответов,
     * бонус для слов без попыток ([newWordBoost]).
     */
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

    /**
     * Выбирает одно слово из списка с вероятностью, пропорциональной [WeightedWord.weight].
     * При неположительной сумме весов берёт случайный элемент (fallback).
     */
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

    /**
     * Оставляет в пуле только те слова, которые соответствуют режиму тренировки.
     *
     * @param freshAttemptIds id слов с суммарным числом попыток &lt; 2 (0 или 1) — только для [TrainingMode.FRESH_WORDS].
     * @param zeroAttemptWordIds id слов ровно с 0 попыток — только для [TrainingMode.NEW_ONLY]; с [freshAttemptIds] не смешивать.
     *
     * Для [TrainingMode.HARD_WORDS] / [TrainingMode.FRESH_WORDS] при пустом результате возвращается исходный пул.
     * [TrainingMode.NEW_ONLY] **не** делает такого fallback.
     */
    fun filterByMode(
        words: List<WordItem>,
        mode: TrainingMode,
        hardWordIds: Set<Long>,
        freshAttemptIds: Set<Long>,
        zeroAttemptWordIds: Set<Long>,
    ): List<WordItem> {
        return when (mode) {
            TrainingMode.RANDOM, TrainingMode.BY_CATEGORY -> words
            TrainingMode.HARD_WORDS -> words.filter { it.id in hardWordIds }.ifEmpty { words }
            TrainingMode.NEW_ONLY -> words.filter { it.id in zeroAttemptWordIds }
            TrainingMode.FRESH_WORDS -> words.filter { it.id in freshAttemptIds }.ifEmpty { words }
            TrainingMode.MIXED -> {
                val hard = words.filter { it.id in hardWordIds }
                val pool = (hard + words).distinctBy { it.id }
                if (pool.isEmpty()) words else pool
            }
        }
    }
}
