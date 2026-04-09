package ru.techlabhub.speechrehab.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ru.techlabhub.speechrehab.R
import ru.techlabhub.speechrehab.domain.model.TrainingMode
import ru.techlabhub.speechrehab.ui.common.categoryTitle
import ru.techlabhub.speechrehab.ui.common.trainingModeLabel

/**
 * Настройки: подсказка слова, размер пакета, режим тренировки, включение источников картинок, фильтр категорий.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    vm: SettingsViewModel = hiltViewModel(),
) {
    val categories by vm.categories.collectAsState()
    val prefs by vm.prefsFlow.collectAsState()
    val allIds = categories.map { it.id }.toSet()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.screen_settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(stringResource(R.string.settings_word_hint_section), style = MaterialTheme.typography.titleLarge)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(stringResource(R.string.settings_show_written_word), style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = prefs.showWordHint,
                    onCheckedChange = { vm.setShowHint(it) },
                )
            }

            Text(stringResource(R.string.settings_batch_section), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.settings_batch_cards, prefs.batchSize), style = MaterialTheme.typography.bodyLarge)
            Slider(
                value = prefs.batchSize.toFloat(),
                onValueChange = { vm.setBatchSize(it.toInt()) },
                valueRange = 4f..40f,
                steps = 35,
            )

            Text(stringResource(R.string.settings_training_mix), style = MaterialTheme.typography.titleLarge)
            TrainingMode.values().forEach { mode ->
                FilterChip(
                    selected = prefs.trainingMode == mode,
                    onClick = { vm.setMode(mode) },
                    label = { Text(trainingModeLabel(mode)) },
                )
            }

            Text(stringResource(R.string.settings_sources_section), style = MaterialTheme.typography.titleLarge)
            SourceRow(stringResource(R.string.source_arasaac), prefs.arasaacEnabled) { v -> vm.setSources(v, prefs.pixabayEnabled, prefs.pexelsEnabled) }
            SourceRow(stringResource(R.string.source_pixabay), prefs.pixabayEnabled) { v -> vm.setSources(prefs.arasaacEnabled, v, prefs.pexelsEnabled) }
            SourceRow(stringResource(R.string.source_pexels), prefs.pexelsEnabled) { v -> vm.setSources(prefs.arasaacEnabled, prefs.pixabayEnabled, v) }

            Text(stringResource(R.string.settings_categories_section), style = MaterialTheme.typography.titleLarge)
            categories.forEach { c ->
                val checked =
                    if (prefs.enabledCategoryIds.isEmpty()) {
                        true
                    } else {
                        c.id in prefs.enabledCategoryIds
                    }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = checked,
                        onCheckedChange = { v -> vm.setCategoryEnabled(c.id, v, allIds, prefs) },
                    )
                    Text(categoryTitle(c.name), style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

@Composable
private fun SourceRow(
    label: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
