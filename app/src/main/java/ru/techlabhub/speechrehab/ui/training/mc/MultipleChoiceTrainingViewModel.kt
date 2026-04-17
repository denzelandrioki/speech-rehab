package ru.techlabhub.speechrehab.ui.training.mc

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ru.techlabhub.speechrehab.R
import ru.techlabhub.speechrehab.di.ApplicationScope
import ru.techlabhub.speechrehab.data.local.entity.MultipleChoiceAttemptEntity
import ru.techlabhub.speechrehab.domain.model.MultipleChoiceOption
import ru.techlabhub.speechrehab.domain.model.MultipleChoiceQuestion
import ru.techlabhub.speechrehab.domain.model.SessionKind
import ru.techlabhub.speechrehab.domain.model.TrainingTextLanguage
import ru.techlabhub.speechrehab.domain.repository.ImageRepository
import ru.techlabhub.speechrehab.domain.repository.MultipleChoiceAttemptRepository
import ru.techlabhub.speechrehab.domain.repository.TrainingRepository
import ru.techlabhub.speechrehab.domain.repository.UserPreferencesRepository
import ru.techlabhub.speechrehab.domain.usecase.GetNextMultipleChoiceQuestionUseCase
import ru.techlabhub.speechrehab.domain.usecase.NextMultipleChoiceQuestionEmptyReason
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

enum class MultipleChoicePhase {
    CHOOSING,
    FEEDBACK,
}

data class MultipleChoiceUiState(
    val loading: Boolean = false,
    val question: MultipleChoiceQuestion? = null,
    val phase: MultipleChoicePhase = MultipleChoicePhase.CHOOSING,
    val selectedWordId: Long? = null,
    val errorMessage: String? = null,
    val trainingTextLanguage: TrainingTextLanguage = TrainingTextLanguage.RUSSIAN,
)

@HiltViewModel
class MultipleChoiceTrainingViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    @ApplicationScope private val applicationScope: CoroutineScope,
    private val getNextQuestion: GetNextMultipleChoiceQuestionUseCase,
    private val imageRepository: ImageRepository,
    private val mcAttemptRepository: MultipleChoiceAttemptRepository,
    private val trainingRepository: TrainingRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    private val _ui = MutableStateFlow(MultipleChoiceUiState())
    val ui: StateFlow<MultipleChoiceUiState> = _ui.asStateFlow()

    private var sessionId: Long? = null
    private var lastWordId: Long? = null
    private var questionShownAtMillis: Long = 0L

    init {
        viewModelScope.launch {
            sessionId = trainingRepository.startSession(SessionKind.MULTIPLE_CHOICE)
            loadNextQuestion()
        }
        viewModelScope.launch {
            userPreferencesRepository.preferencesFlow
                .map { it.trainingTextLanguage }
                .distinctUntilChanged()
                .collect { mode ->
                    _ui.update { it.copy(trainingTextLanguage = mode) }
                }
        }
    }

    fun loadNextQuestion() {
        viewModelScope.launch {
            _ui.update {
                it.copy(
                    loading = true,
                    errorMessage = null,
                    phase = MultipleChoicePhase.CHOOSING,
                    selectedWordId = null,
                    question = null,
                )
            }
            val prefs = userPreferencesRepository.preferencesFlow.first()
            _ui.update { it.copy(trainingTextLanguage = prefs.trainingTextLanguage) }
            val outcome =
                try {
                    getNextQuestion(prefs, lastWordId)
                } catch (e: Exception) {
                    Timber.e(e, "MultipleChoice load failed")
                    _ui.update {
                        it.copy(
                            loading = false,
                            question = null,
                            errorMessage = e.message ?: appContext.getString(R.string.mc_error_load),
                        )
                    }
                    return@launch
                }
            val q = outcome.question
            lastWordId = q?.correctWord?.id
            val err =
                when {
                    q != null -> null
                    outcome.emptyReason == NextMultipleChoiceQuestionEmptyReason.NEW_ONLY_EXHAUSTED ->
                        appContext.getString(R.string.training_error_new_only_exhausted)
                    outcome.emptyReason == NextMultipleChoiceQuestionEmptyReason.NO_ENABLED_WORDS ->
                        appContext.getString(R.string.training_error_no_words)
                    outcome.emptyReason == NextMultipleChoiceQuestionEmptyReason.NOT_ENOUGH_WORDS_FOR_OPTIONS ->
                        appContext.getString(R.string.mc_error_not_enough_words)
                    else -> appContext.getString(R.string.mc_error_load)
                }
            if (q != null) {
                imageRepository.markImageVariantShown(q.imageCard.wordImageVariantId)
                questionShownAtMillis = System.currentTimeMillis()
            }
            _ui.update {
                it.copy(
                    loading = false,
                    question = q,
                    errorMessage = err,
                )
            }
        }
    }

    fun onOptionSelected(option: MultipleChoiceOption) {
        val q = _ui.value.question ?: return
        if (_ui.value.phase != MultipleChoicePhase.CHOOSING) return
        val answeredAt = System.currentTimeMillis()
        val responseTime = (answeredAt - questionShownAtMillis).coerceAtLeast(0L)
        val correctId = q.correctWord.id
        val isCorrect = option.word.id == correctId
        viewModelScope.launch {
            try {
                mcAttemptRepository.insertAttempt(
                    MultipleChoiceAttemptEntity(
                        sessionId = sessionId,
                        questionWordId = correctId,
                        categoryId = q.correctWord.categoryId,
                        shownAtEpochMillis = questionShownAtMillis,
                        answeredAtEpochMillis = answeredAt,
                        responseTimeMillis = responseTime,
                        selectedWordId = option.word.id,
                        isCorrect = isCorrect,
                        displayLanguageEffective = q.displayLanguageEffective.name,
                        imageVariantId = q.imageCard.wordImageVariantId,
                        selectedLabelSnapshot = option.label,
                    ),
                )
            } catch (e: Exception) {
                Timber.e(e, "MultipleChoice insert failed")
            }
            _ui.update {
                it.copy(
                    phase = MultipleChoicePhase.FEEDBACK,
                    selectedWordId = option.word.id,
                )
            }
        }
    }

    fun onContinueAfterFeedback() {
        if (_ui.value.phase != MultipleChoicePhase.FEEDBACK) return
        loadNextQuestion()
    }

    override fun onCleared() {
        val id = sessionId
        if (id != null) {
            applicationScope.launch {
                trainingRepository.endSession(id, assistantNote = null)
            }
        }
        super.onCleared()
    }
}
