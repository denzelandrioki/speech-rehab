package ru.techlabhub.speechrehab.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ru.techlabhub.speechrehab.data.local.SpeechRehabDatabase
import ru.techlabhub.speechrehab.data.mapper.toCategory
import ru.techlabhub.speechrehab.domain.model.AppLanguage
import ru.techlabhub.speechrehab.domain.model.Category
import ru.techlabhub.speechrehab.domain.model.ImageRotationMode
import ru.techlabhub.speechrehab.domain.model.OnlineImageFetchingMode
import ru.techlabhub.speechrehab.domain.model.PreferredImageMode
import ru.techlabhub.speechrehab.domain.model.TrainingMode
import ru.techlabhub.speechrehab.domain.model.TrainingTextLanguage
import ru.techlabhub.speechrehab.domain.repository.UserPreferencesRepository
import ru.techlabhub.speechrehab.domain.repository.UserTrainingPreferences
import ru.techlabhub.speechrehab.domain.repository.WordRepository
import ru.techlabhub.speechrehab.domain.usecase.PrefetchMissingImagesResult
import ru.techlabhub.speechrehab.domain.usecase.PrefetchMissingImagesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel настроек тренировки.
 *
 * Поднимает список категорий из Room и текущие [UserTrainingPreferences] из DataStore (reactive `StateFlow`).
 * Семантика выбора категорий: пустой [UserTrainingPreferences.enabledCategoryIds] означает «все категории включены»;
 * при снятии последней галочки логика в [setCategoryEnabled] восстанавливает полный набор id, чтобы не остаться без слов.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val db: SpeechRehabDatabase,
    private val prefs: UserPreferencesRepository,
    private val wordRepository: WordRepository,
    private val prefetchMissingImagesUseCase: PrefetchMissingImagesUseCase,
) : ViewModel() {

    private val _prefetchRunning = MutableStateFlow(false)
    val prefetchRunning: StateFlow<Boolean> = _prefetchRunning.asStateFlow()

    private val _lastPrefetchResult = MutableStateFlow<PrefetchMissingImagesResult?>(null)
    val lastPrefetchResult: StateFlow<PrefetchMissingImagesResult?> = _lastPrefetchResult.asStateFlow()

    val categories: StateFlow<List<Category>> =
        db.categoryDao()
            .observeCategories()
            .map { list -> list.map { it.toCategory() } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val prefsFlow: StateFlow<UserTrainingPreferences> =
        prefs.preferencesFlow
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserTrainingPreferences())

    init {
        viewModelScope.launch {
            wordRepository.ensureSeededIfEmpty()
        }
    }

    fun setShowHint(v: Boolean) {
        viewModelScope.launch { prefs.setShowWordHint(v) }
    }

    fun setBatchSize(v: Int) {
        viewModelScope.launch { prefs.setBatchSize(v) }
    }

    fun setMode(mode: TrainingMode) {
        viewModelScope.launch { prefs.setTrainingMode(mode) }
    }

    fun setSources(
        arasaac: Boolean,
        pixabay: Boolean,
        pexels: Boolean,
    ) {
        viewModelScope.launch { prefs.setSourceEnabled(arasaac, pixabay, pexels) }
    }

    fun setAppLanguage(language: AppLanguage) {
        viewModelScope.launch { prefs.setAppLanguage(language) }
    }

    fun setTrainingTextLanguage(mode: TrainingTextLanguage) {
        viewModelScope.launch { prefs.setTrainingTextLanguage(mode) }
    }

    fun setOnlineImageFetchingMode(mode: OnlineImageFetchingMode) {
        viewModelScope.launch { prefs.setOnlineImageFetchingMode(mode) }
    }

    fun setPreferredImageMode(mode: PreferredImageMode) {
        viewModelScope.launch { prefs.setPreferredImageMode(mode) }
    }

    fun setImageRotationMode(mode: ImageRotationMode) {
        Timber.i("Settings: imageRotationMode selected=%s (persisting)", mode.name)
        viewModelScope.launch {
            prefs.setImageRotationMode(mode)
            Timber.i("Settings: imageRotationMode saved=%s", mode.name)
        }
    }

    fun setRefreshRemoteWhenNoLocalImage(value: Boolean) {
        viewModelScope.launch {
            Timber.d("Settings: refreshRemoteWhenNoLocalImage=%s", value)
            prefs.setRefreshRemoteWhenNoLocalImage(value)
        }
    }

    fun prefetchMissingImagesNow() {
        viewModelScope.launch {
            _prefetchRunning.value = true
            Timber.i("Settings: prefetch missing images button — started")
            try {
                val p = prefs.preferencesFlow.first()
                _lastPrefetchResult.value = prefetchMissingImagesUseCase(p)
            } finally {
                _prefetchRunning.value = false
                Timber.i("Settings: prefetch missing images button — finished")
            }
        }
    }

    fun setCategoryEnabled(
        categoryId: Long,
        enabled: Boolean,
        allCategoryIds: Set<Long>,
        current: UserTrainingPreferences,
    ) {
        viewModelScope.launch {
            val enabledNow =
                if (current.enabledCategoryIds.isEmpty()) {
                    allCategoryIds
                } else {
                    current.enabledCategoryIds
                }
            val newEnabled =
                if (enabled) {
                    enabledNow + categoryId
                } else {
                    enabledNow - categoryId
                }
            val toSave =
                when {
                    newEnabled.isEmpty() -> allCategoryIds
                    newEnabled == allCategoryIds -> emptySet()
                    else -> newEnabled
                }
            prefs.setEnabledCategoryIds(toSave)
        }
    }
}
