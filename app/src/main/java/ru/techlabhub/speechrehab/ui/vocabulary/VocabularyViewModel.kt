package ru.techlabhub.speechrehab.ui.vocabulary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ru.techlabhub.speechrehab.domain.repository.WordRepository
import ru.techlabhub.speechrehab.domain.model.WordItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VocabularyViewModel @Inject constructor(
    private val wordRepository: WordRepository,
) : ViewModel() {

    val words: StateFlow<List<WordItem>> =
        wordRepository
            .observeAllWords()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            wordRepository.ensureSeededIfEmpty()
        }
    }

    fun setEnabled(
        wordId: Long,
        enabled: Boolean,
    ) {
        viewModelScope.launch {
            wordRepository.setWordEnabled(wordId, enabled)
        }
    }
}
