package ru.techlabhub.speechrehab.domain.usecase

import ru.techlabhub.speechrehab.data.local.SpeechRehabDatabase
import ru.techlabhub.speechrehab.domain.analytics.CardWeightEngine
import ru.techlabhub.speechrehab.domain.model.ImageCard
import ru.techlabhub.speechrehab.domain.model.TrainingMode
import ru.techlabhub.speechrehab.domain.repository.ImageRepository
import ru.techlabhub.speechrehab.domain.repository.UserTrainingPreferences
import ru.techlabhub.speechrehab.domain.repository.WordRepository
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
 * **Шаги:**
 * 1. Убедиться, что словарь засеян ([WordRepository.ensureSeededIfEmpty]).
 * 2. Получить пул слов по включённым категориям из настроек.
 *    Режим [TrainingMode.NEW_ONLY] фильтрует только этот пул (0 попыток); картинки не при чём —
 *    при пустом пуле [ImageRepository] не вызывается. Дозагрузка изображений для «старых» слов
 *    выполняется в [ImageRepository.resolveCard] после того, как слово уже выбрано.
 * 3. Загрузить из БД множество «сложных» слов, id с 0 попыток и id с &lt; 2 попыток — для режимов [TrainingMode].
 * 4. Отфильтровать пул через [CardWeightEngine.filterByMode], исключить предыдущее слово при возможности.
 * 5. Назначить каждому кандидату вес: чем больше суммарных попыток при слабой серии верных — тем выше приоритет
 *    (эвристика вместо отдельного счётчика ошибок).
 * 6. Случайно выбрать слово с учётом весов ([CardWeightEngine.pickWeighted]).
 * 7. Получить картинку: [ImageRepository.resolveCard] (offline-first; при отсутствии файла — заглушка в UI).
 */
@Singleton
class GetNextTrainingCardUseCase @Inject constructor(
    private val db: SpeechRehabDatabase,
    private val wordRepository: WordRepository,
    private val imageRepository: ImageRepository,
) {
    private val answers = db.answerAttemptDao()
    private val words = db.wordDao()

    suspend operator fun invoke(
        prefs: UserTrainingPreferences,
        lastWordId: Long?,
    ): GetNextTrainingCardOutcome {
        wordRepository.ensureSeededIfEmpty()

        val pool = wordRepository.getEnabledWordsForTraining(prefs.enabledCategoryIds)
        if (pool.isEmpty()) {
            return GetNextTrainingCardOutcome(null, NextTrainingCardEmptyReason.NO_ENABLED_WORDS)
        }

        val hardIds =
            answers.hardestWords(minAttempts = 2, limit = 40)
                .map { it.wordId }
                .toSet()
        // COUNT(*) < 1 ⇒ только 0 попыток
        val zeroAttemptIds = words.wordIdsWithTotalAttemptsBelow(maxAttempts = 1).toSet()
        // COUNT(*) < 2 ⇒ 0 или 1 попытка (режим FRESH_WORDS)
        val freshWordIds = words.wordIdsWithTotalAttemptsBelow(maxAttempts = 2).toSet()

        val modeAdjusted =
            CardWeightEngine.filterByMode(
                words = pool,
                mode = prefs.trainingMode,
                hardWordIds = hardIds,
                freshWordIds = freshWordIds,
                zeroAttemptWordIds = zeroAttemptIds,
            )

        if (prefs.trainingMode == TrainingMode.NEW_ONLY && modeAdjusted.isEmpty()) {
            return GetNextTrainingCardOutcome(null, NextTrainingCardEmptyReason.NEW_ONLY_EXHAUSTED)
        }

        val candidates =
            if (lastWordId != null && modeAdjusted.size > 1) {
                modeAdjusted.filter { it.id != lastWordId }
            } else {
                modeAdjusted
            }

        if (candidates.isEmpty()) {
            return GetNextTrainingCardOutcome(null, NextTrainingCardEmptyReason.NONE)
        }

        val ids = candidates.map { it.id }
        val counts = answers.attemptCountsForWords(ids).associate { it.wordId to it.cnt }
        val weighted =
            candidates.map { w ->
                val incorrectEstimate = counts[w.id]?.let { total ->
                    // Отдельного поля «число ошибок» в БД нет: оцениваем как (все попытки − подряд идущие верные),
                    // чтобы чаще показывать слова с нестабильным результатом.
                    (total - w.consecutiveCorrect).coerceAtLeast(0)
                } ?: 0
                val wgt =
                    CardWeightEngine.computeWeight(
                        word = w,
                        incorrectAttemptsEstimate = incorrectEstimate,
                    )
                CardWeightEngine.WeightedWord(word = w, weight = wgt)
            }

        val picked = CardWeightEngine.pickWeighted(weighted) ?: return GetNextTrainingCardOutcome(null, NextTrainingCardEmptyReason.NONE)

        val card: ImageCard = imageRepository.resolveCard(picked, prefs)
        return GetNextTrainingCardOutcome(card, NextTrainingCardEmptyReason.NONE)
    }
}
