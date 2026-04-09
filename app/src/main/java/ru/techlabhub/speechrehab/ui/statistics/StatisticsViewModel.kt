package ru.techlabhub.speechrehab.ui.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ru.techlabhub.speechrehab.domain.repository.StatisticsRepository
import ru.techlabhub.speechrehab.domain.repository.StatisticsSnapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel экрана статистики: однократно и по запросу [refresh] загружает [StatisticsSnapshot]
 * из [StatisticsRepository] (агрегаты SQL по попыткам, тренды за 7/14/30 дней).
 */
@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val statisticsRepository: StatisticsRepository,
) : ViewModel() {
    private val _snapshot = MutableStateFlow<StatisticsSnapshot?>(null)
    val snapshot: StateFlow<StatisticsSnapshot?> = _snapshot.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _loading.value = true
            _snapshot.value = statisticsRepository.loadSnapshot()
            _loading.value = false
        }
    }
}
