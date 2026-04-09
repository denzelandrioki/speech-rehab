package ru.techlabhub.speechrehab.ui.training

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ru.techlabhub.speechrehab.R
import ru.techlabhub.speechrehab.domain.model.ImageCard
import ru.techlabhub.speechrehab.domain.repository.TrainingRepository
import ru.techlabhub.speechrehab.domain.repository.UserPreferencesRepository
import ru.techlabhub.speechrehab.domain.usecase.GetNextTrainingCardUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.NonCancellable
import javax.inject.Inject

data class TrainingUiState(
    val loading: Boolean = false,
    val card: ImageCard? = null,
    val errorMessage: String? = null,
    /** Уже отмечен результат для текущей карточки — до «Следующая». */
    val lockedAfterAnswer: Boolean = false,
    val showWordHint: Boolean = true,
)

@HiltViewModel
class TrainingViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val getNextTrainingCard: GetNextTrainingCardUseCase,
    private val trainingRepository: TrainingRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    private val _ui = MutableStateFlow(TrainingUiState())
    val ui: StateFlow<TrainingUiState> = _ui.asStateFlow()

    private var sessionId: Long? = null
    private var lastWordId: Long? = null

    init {
        viewModelScope.launch {
            sessionId = trainingRepository.startSession()
            loadNextCard()
        }
    }

    fun loadNextCard() {
        viewModelScope.launch {
            _ui.update { it.copy(loading = true, errorMessage = null, lockedAfterAnswer = false) }
            val prefs = userPreferencesRepository.preferencesFlow.first()
            _ui.update { it.copy(showWordHint = prefs.showWordHint) }
            val card =
                try {
                    getNextTrainingCard(prefs, lastWordId)
                } catch (e: Exception) {
                    _ui.update {
                        it.copy(
                            loading = false,
                            card = null,
                            errorMessage = e.message ?: appContext.getString(R.string.training_error_load),
                        )
                    }
                    return@launch
                }
            lastWordId = card?.word?.id
            _ui.update {
                it.copy(
                    loading = false,
                    card = card,
                    errorMessage =
                        if (card == null) {
                            appContext.getString(R.string.training_error_no_card)
                        } else {
                            null
                        },
                )
            }
        }
    }

    fun onCorrect() {
        val c = _ui.value.card ?: return
        if (_ui.value.lockedAfterAnswer) return
        viewModelScope.launch {
            trainingRepository.recordAnswer(
                sessionId = sessionId,
                wordId = c.word.id,
                isCorrect = true,
                assistantNote = null,
            )
            _ui.update { it.copy(lockedAfterAnswer = true) }
        }
    }

    fun onIncorrect() {
        val c = _ui.value.card ?: return
        if (_ui.value.lockedAfterAnswer) return
        viewModelScope.launch {
            trainingRepository.recordAnswer(
                sessionId = sessionId,
                wordId = c.word.id,
                isCorrect = false,
                assistantNote = null,
            )
            _ui.update { it.copy(lockedAfterAnswer = true) }
        }
    }

    override fun onCleared() {
        val id = sessionId
        if (id != null) {
            runBlocking {
                withContext(NonCancellable) {
                    trainingRepository.endSession(id, assistantNote = null)
                }
            }
        }
        super.onCleared()
    }
}
