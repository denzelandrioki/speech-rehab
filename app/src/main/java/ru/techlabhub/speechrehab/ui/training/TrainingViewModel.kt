package ru.techlabhub.speechrehab.ui.training

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ru.techlabhub.speechrehab.R
import ru.techlabhub.speechrehab.di.ApplicationScope
import ru.techlabhub.speechrehab.domain.model.ImageCard
import ru.techlabhub.speechrehab.domain.model.TrainingTextLanguage
import ru.techlabhub.speechrehab.domain.repository.TrainingRepository
import ru.techlabhub.speechrehab.domain.repository.UserPreferencesRepository
import ru.techlabhub.speechrehab.domain.usecase.GetNextTrainingCardUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Состояние экрана тренировки: индикатор загрузки, текущая [ImageCard], текст ошибки,
 * блокировка кнопок после ответа до «Следующая», видимость письменной подсказки слова (из настроек).
 */
data class TrainingUiState(
    val loading: Boolean = false,
    val card: ImageCard? = null,
    val errorMessage: String? = null,
    /** Уже отмечен результат для текущей карточки — до «Следующая». */
    val lockedAfterAnswer: Boolean = false,
    val showWordHint: Boolean = true,
    /** Режим подписи слова на карточке (независимо от языка интерфейса). */
    val trainingTextLanguage: TrainingTextLanguage = TrainingTextLanguage.RUSSIAN,
)

/**
 * ViewModel экрана тренировки.
 *
 * При создании открывает новую [TrainingRepository.startSession], затем подгружает карточки через
 * [GetNextTrainingCardUseCase] с учётом [UserPreferencesRepository] (режим, категории, источники картинок).
 * Запоминает [lastWordId], чтобы реже подряд показывать одно и то же слово (если в пуле больше одного).
 *
 * Закрытие сессии при уходе с экрана выполняется в [onCleared] через [applicationScope]: неблокирующий
 * [kotlinx.coroutines.launch] на IO-потоке. Это убирает [kotlinx.coroutines.runBlocking] с главного потока.
 * При аварийном завершении процесса запись `endSession` теоретически может не успеть (как и при любом async);
 * для MVP это приемлемый компромисс.
 */
@HiltViewModel
class TrainingViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    @ApplicationScope private val applicationScope: CoroutineScope,
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
            _ui.update {
                it.copy(
                    showWordHint = prefs.showWordHint,
                    trainingTextLanguage = prefs.trainingTextLanguage,
                )
            }
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
                            appContext.getString(R.string.training_error_no_words)
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
            applicationScope.launch {
                trainingRepository.endSession(id, assistantNote = null)
            }
        }
        super.onCleared()
    }
}
