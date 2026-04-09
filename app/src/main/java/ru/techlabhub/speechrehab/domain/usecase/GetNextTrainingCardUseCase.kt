package ru.techlabhub.speechrehab.domain.usecase

import ru.techlabhub.speechrehab.data.local.SpeechRehabDatabase
import ru.techlabhub.speechrehab.domain.analytics.CardWeightEngine
import ru.techlabhub.speechrehab.domain.model.ImageCard
import ru.techlabhub.speechrehab.domain.repository.ImageRepository
import ru.techlabhub.speechrehab.domain.repository.UserTrainingPreferences
import ru.techlabhub.speechrehab.domain.repository.WordRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Сценарий выбора следующей карточки для тренировки.
 *
 * **Шаги:**
 * 1. Убедиться, что словарь засеян ([WordRepository.ensureSeededIfEmpty]).
 * 2. Получить пул слов по включённым категориям из настроек.
 * 3. Загрузить из БД множество «сложных» слов (низкая точность при достаточном числе попыток) и «новых»
 *    (мало попыток в целом) — для режимов [TrainingMode].
 * 4. Отфильтровать пул через [CardWeightEngine.filterByMode], исключить предыдущее слово при возможности.
 * 5. Назначить каждому кандидату вес: чем больше суммарных попыток при слабой серии верных — тем выше приоритет
 *    (эвристика вместо отдельного счётчика ошибок).
 * 6. Случайно выбрать слово с учётом весов ([CardWeightEngine.pickWeighted]).
 * 7. Получить картинку: [ImageRepository.resolveCard] (кэш → ARASAAC → Pixabay → Pexels по настройкам).
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
    ): ImageCard? {
        wordRepository.ensureSeededIfEmpty()

        val pool = wordRepository.getEnabledWordsForTraining(prefs.enabledCategoryIds)
        if (pool.isEmpty()) return null

        val hardIds =
            answers.hardestWords(minAttempts = 2, limit = 40)
                .map { it.wordId }
                .toSet()
        val lowAttemptIds = words.wordIdsWithTotalAttemptsBelow(maxAttempts = 2).toSet()

        val modeAdjusted =
            CardWeightEngine.filterByMode(
                words = pool,
                mode = prefs.trainingMode,
                hardWordIds = hardIds,
                lowAttemptWordIds = lowAttemptIds,
            )

        val candidates =
            if (lastWordId != null && modeAdjusted.size > 1) {
                modeAdjusted.filter { it.id != lastWordId }
            } else {
                modeAdjusted
            }

        if (candidates.isEmpty()) return null

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

        val picked = CardWeightEngine.pickWeighted(weighted) ?: return null

        return imageRepository.resolveCard(picked, prefs)
    }
}
