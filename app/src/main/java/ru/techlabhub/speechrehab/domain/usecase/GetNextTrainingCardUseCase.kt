package ru.techlabhub.speechrehab.domain.usecase

import ru.techlabhub.speechrehab.data.local.SpeechRehabDatabase
import ru.techlabhub.speechrehab.domain.analytics.CardWeightEngine
import ru.techlabhub.speechrehab.domain.model.ImageCard
import ru.techlabhub.speechrehab.domain.model.ImageFetchPolicy
import ru.techlabhub.speechrehab.domain.model.TrainingMode
import ru.techlabhub.speechrehab.domain.model.WordItem
import ru.techlabhub.speechrehab.domain.repository.ImageRepository
import ru.techlabhub.speechrehab.domain.repository.UserTrainingPreferences
import ru.techlabhub.speechrehab.domain.repository.WordRepository
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/** Почему карточка не получена (при [GetNextTrainingCardOutcome.imageCard] == null). */
enum class NextTrainingCardEmptyReason {
    /** Карточка есть или причина не классифицирована. */
    NONE,
    /** Нет включённых слов в выбранных категориях. */
    NO_ENABLED_WORDS,
    /** Режим [TrainingMode.NEW_ONLY]: не осталось слов с 0 попыток. */
    NEW_ONLY_EXHAUSTED,
}

data class GetNextTrainingCardOutcome(
    val imageCard: ImageCard?,
    val emptyReason: NextTrainingCardEmptyReason = NextTrainingCardEmptyReason.NONE,
)

/**
 * Сценарий выбора следующей карточки для тренировки.
 *
 * **Шаги:** см. [pickNextWord] + загрузка изображения.
 */
@Singleton
class GetNextTrainingCardUseCase @Inject constructor(
    private val db: SpeechRehabDatabase,
    private val wordRepository: WordRepository,
    private val imageRepository: ImageRepository,
) {
    private val answers = db.answerAttemptDao()
    private val words = db.wordDao()

    /**
     * Выбор следующего слова по тем же правилам, что и для карточки (без загрузки картинки).
     * Используется assisted-экраном и режимом multiple choice.
     */
    suspend fun pickNextWord(
        prefs: UserTrainingPreferences,
        lastWordId: Long?,
    ): Pair<WordItem?, NextTrainingCardEmptyReason> {
        wordRepository.ensureSeededIfEmpty()

        val pool = wordRepository.getEnabledWordsForTraining(prefs.enabledCategoryIds)
        if (pool.isEmpty()) {
            Timber.d(
                "NextTrainingCard: selectedMode=%s poolSize=0 (no enabled words in categories)",
                prefs.trainingMode.name,
            )
            return null to NextTrainingCardEmptyReason.NO_ENABLED_WORDS
        }

        val hardIds =
            answers.hardestWords(minAttempts = 2, limit = 40)
                .map { it.wordId }
                .toSet()
        val zeroAttemptIds = words.wordIdsWithTotalAttemptsBelow(maxAttempts = 1).toSet()
        val freshAttemptIds = words.wordIdsWithTotalAttemptsBelow(maxAttempts = 2).toSet()
        val zeroInPool = pool.count { it.id in zeroAttemptIds }
        Timber.d(
            "NextTrainingCard: selectedMode=%s poolSize=%d zeroAttemptIdsCount=%d freshAttemptIdsCount=%d zeroInPool=%d",
            prefs.trainingMode.name,
            pool.size,
            zeroAttemptIds.size,
            freshAttemptIds.size,
            zeroInPool,
        )

        val modeAdjusted =
            CardWeightEngine.filterByMode(
                words = pool,
                mode = prefs.trainingMode,
                hardWordIds = hardIds,
                freshAttemptIds = freshAttemptIds,
                zeroAttemptWordIds = zeroAttemptIds,
            )
        Timber.d("NextTrainingCard: filteredPoolSize(modeAdjusted)=%d", modeAdjusted.size)

        if (prefs.trainingMode == TrainingMode.NEW_ONLY && modeAdjusted.isEmpty()) {
            Timber.i("NextTrainingCard: NEW_ONLY exhausted (no zero-attempt words in enabled pool)")
            return null to NextTrainingCardEmptyReason.NEW_ONLY_EXHAUSTED
        }

        val candidates =
            if (lastWordId != null && modeAdjusted.size > 1) {
                modeAdjusted.filter { it.id != lastWordId }
            } else {
                modeAdjusted
            }
        Timber.d("NextTrainingCard: candidatesForPick=%d lastWordId=%s", candidates.size, lastWordId)

        if (candidates.isEmpty()) {
            return null to NextTrainingCardEmptyReason.NONE
        }

        val ids = candidates.map { it.id }
        val counts = answers.attemptCountsForWords(ids).associate { it.wordId to it.cnt }
        val weighted =
            candidates.map { w ->
                val incorrectEstimate =
                    counts[w.id]?.let { total ->
                        (total - w.consecutiveCorrect).coerceAtLeast(0)
                    } ?: 0
                val wgt =
                    CardWeightEngine.computeWeight(
                        word = w,
                        incorrectAttemptsEstimate = incorrectEstimate,
                    )
                CardWeightEngine.WeightedWord(word = w, weight = wgt)
            }

        val picked =
            CardWeightEngine.pickWeighted(weighted)
                ?: return null to NextTrainingCardEmptyReason.NONE

        Timber.d(
            "NextTrainingCard: selectedWord id=%d text=%s categoryId=%s imageRotation=%s preferredImage=%s onlineFetching=%s refreshRemoteWhenNoLocal=%s",
            picked.id,
            picked.text,
            picked.categoryId,
            prefs.imageRotationMode.name,
            prefs.preferredImageMode.name,
            prefs.onlineImageFetchingMode.name,
            prefs.refreshRemoteWhenNoLocalImage,
        )
        return picked to NextTrainingCardEmptyReason.NONE
    }

    suspend operator fun invoke(
        prefs: UserTrainingPreferences,
        lastWordId: Long?,
    ): GetNextTrainingCardOutcome {
        val (picked, emptyReason) = pickNextWord(prefs, lastWordId)
        if (picked == null) {
            return GetNextTrainingCardOutcome(null, emptyReason)
        }
        Timber.d(
            "NextTrainingCard: resolveCard prefs snapshot rotation=%s preferred=%s online=%s arasaac=%s pixabay=%s pexels=%s",
            prefs.imageRotationMode.name,
            prefs.preferredImageMode.name,
            prefs.onlineImageFetchingMode.name,
            prefs.arasaacEnabled,
            prefs.pixabayEnabled,
            prefs.pexelsEnabled,
        )
        val card: ImageCard = imageRepository.resolveCard(picked, prefs, ImageFetchPolicy.NORMAL)
        return GetNextTrainingCardOutcome(card, NextTrainingCardEmptyReason.NONE)
    }
}
