package ru.techlabhub.speechrehab.domain.usecase

import ru.techlabhub.speechrehab.domain.analytics.MultipleChoiceOptionBuilder
import ru.techlabhub.speechrehab.domain.model.ChoiceOptionLabelFormatter
import ru.techlabhub.speechrehab.domain.model.ImageFetchPolicy
import ru.techlabhub.speechrehab.domain.model.MultipleChoiceOption
import ru.techlabhub.speechrehab.domain.model.MultipleChoiceQuestion
import ru.techlabhub.speechrehab.domain.repository.ImageRepository
import ru.techlabhub.speechrehab.domain.repository.UserTrainingPreferences
import ru.techlabhub.speechrehab.domain.repository.WordRepository
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

enum class NextMultipleChoiceQuestionEmptyReason {
    NONE,
    NO_ENABLED_WORDS,
    NEW_ONLY_EXHAUSTED,
    NOT_ENOUGH_WORDS_FOR_OPTIONS,
}

data class GetNextMultipleChoiceQuestionOutcome(
    val question: MultipleChoiceQuestion?,
    val emptyReason: NextMultipleChoiceQuestionEmptyReason = NextMultipleChoiceQuestionEmptyReason.NONE,
)

/**
 * Следующий вопрос с 4 вариантами: тот же отбор слова, что и assisted ([GetNextTrainingCardUseCase.pickNextWord]),
 * затем дистракторы и картинка.
 */
@Singleton
class GetNextMultipleChoiceQuestionUseCase @Inject constructor(
    private val getNextTrainingCardUseCase: GetNextTrainingCardUseCase,
    private val wordRepository: WordRepository,
    private val imageRepository: ImageRepository,
) {
    suspend operator fun invoke(
        prefs: UserTrainingPreferences,
        lastWordId: Long?,
    ): GetNextMultipleChoiceQuestionOutcome {
        val (picked, reason) = getNextTrainingCardUseCase.pickNextWord(prefs, lastWordId)
        if (picked == null) {
            return GetNextMultipleChoiceQuestionOutcome(
                null,
                when (reason) {
                    NextTrainingCardEmptyReason.NO_ENABLED_WORDS ->
                        NextMultipleChoiceQuestionEmptyReason.NO_ENABLED_WORDS
                    NextTrainingCardEmptyReason.NEW_ONLY_EXHAUSTED ->
                        NextMultipleChoiceQuestionEmptyReason.NEW_ONLY_EXHAUSTED
                    NextTrainingCardEmptyReason.NONE -> NextMultipleChoiceQuestionEmptyReason.NOT_ENOUGH_WORDS_FOR_OPTIONS
                },
            )
        }
        val pool = wordRepository.getEnabledWordsForTraining(prefs.enabledCategoryIds)
        val fourWords = MultipleChoiceOptionBuilder.buildOptions(correct = picked, pool = pool)
        if (fourWords == null) {
            Timber.i("MultipleChoice: not enough distinct words in pool for 4 options")
            return GetNextMultipleChoiceQuestionOutcome(null, NextMultipleChoiceQuestionEmptyReason.NOT_ENOUGH_WORDS_FOR_OPTIONS)
        }
        val eff = ChoiceOptionLabelFormatter.effectiveChoiceLanguage(prefs.trainingTextLanguage)
        val options =
            fourWords.map { w ->
                MultipleChoiceOption(
                    word = w,
                    label = ChoiceOptionLabelFormatter.optionLabel(w, prefs.trainingTextLanguage),
                )
            }
        val imageCard = imageRepository.resolveCard(picked, prefs, ImageFetchPolicy.NORMAL)
        return GetNextMultipleChoiceQuestionOutcome(
            MultipleChoiceQuestion(
                imageCard = imageCard,
                correctWord = picked,
                options = options,
                displayLanguageEffective = eff,
            ),
            NextMultipleChoiceQuestionEmptyReason.NONE,
        )
    }
}
