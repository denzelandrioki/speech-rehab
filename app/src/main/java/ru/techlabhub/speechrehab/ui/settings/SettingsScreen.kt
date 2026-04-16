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
import androidx.compose.material3.OutlinedButton
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
import ru.techlabhub.speechrehab.BuildConfig
import ru.techlabhub.speechrehab.R
import ru.techlabhub.speechrehab.domain.model.AppLanguage
import ru.techlabhub.speechrehab.domain.model.ImageRotationMode
import ru.techlabhub.speechrehab.domain.model.OnlineImageFetchingMode
import ru.techlabhub.speechrehab.domain.model.PreferredImageMode
import ru.techlabhub.speechrehab.domain.model.TrainingMode
import ru.techlabhub.speechrehab.domain.model.TrainingTextLanguage
import ru.techlabhub.speechrehab.ui.common.categoryTitle
import ru.techlabhub.speechrehab.ui.common.trainingModeLabel

/**
 * Настройки: язык UI, текст карточки, подсказка, пакет, режим, источники и приоритет картинок, категории.
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
            Text(stringResource(R.string.settings_app_language_section), style = MaterialTheme.typography.titleLarge)
            AppLanguage.values().forEach { lang ->
                FilterChip(
                    selected = prefs.appLanguage == lang,
                    onClick = { vm.setAppLanguage(lang) },
                    label = { Text(appLanguageLabel(lang)) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Text(stringResource(R.string.settings_card_text_section), style = MaterialTheme.typography.titleLarge)
            TrainingTextLanguage.values().forEach { mode ->
                FilterChip(
                    selected = prefs.trainingTextLanguage == mode,
                    onClick = { vm.setTrainingTextLanguage(mode) },
                    label = { Text(trainingTextLanguageLabel(mode)) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

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
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Text(stringResource(R.string.settings_preferred_image_section), style = MaterialTheme.typography.titleLarge)
            PreferredImageMode.values().forEach { mode ->
                FilterChip(
                    selected = prefs.preferredImageMode == mode,
                    onClick = { vm.setPreferredImageMode(mode) },
                    label = { Text(preferredImageModeLabel(mode)) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Text(stringResource(R.string.settings_image_rotation_section), style = MaterialTheme.typography.titleLarge)
            Text(
                stringResource(R.string.settings_image_rotation_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ImageRotationMode.values().forEach { mode ->
                FilterChip(
                    selected = prefs.imageRotationMode == mode,
                    onClick = { vm.setImageRotationMode(mode) },
                    label = { Text(imageRotationModeLabel(mode)) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Text(stringResource(R.string.settings_online_images_section), style = MaterialTheme.typography.titleLarge)
            OnlineImageFetchingMode.values().forEach { mode ->
                FilterChip(
                    selected = prefs.onlineImageFetchingMode == mode,
                    onClick = { vm.setOnlineImageFetchingMode(mode) },
                    label = { Text(onlineFetchLabel(mode)) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (prefs.onlineImageFetchingMode == OnlineImageFetchingMode.WIFI_ONLY) {
                Text(
                    stringResource(R.string.online_fetch_wifi_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    stringResource(R.string.settings_refresh_remote_when_no_local),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                )
                Switch(
                    checked = prefs.refreshRemoteWhenNoLocalImage,
                    onCheckedChange = { vm.setRefreshRemoteWhenNoLocalImage(it) },
                )
            }
            Text(
                stringResource(R.string.settings_refresh_remote_when_no_local_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            val prefetchRunning by vm.prefetchRunning.collectAsState()
            val lastPrefetch by vm.lastPrefetchResult.collectAsState()
            val prefetchEnabled =
                prefs.refreshRemoteWhenNoLocalImage &&
                    prefs.preferredImageMode != PreferredImageMode.LOCAL_ONLY &&
                    prefs.onlineImageFetchingMode != OnlineImageFetchingMode.DISABLED
            OutlinedButton(
                onClick = { vm.prefetchMissingImagesNow() },
                enabled = !prefetchRunning && prefetchEnabled,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    if (prefetchRunning) {
                        stringResource(R.string.settings_prefetch_running)
                    } else {
                        stringResource(R.string.settings_prefetch_missing_now)
                    },
                )
            }
            lastPrefetch?.let { r ->
                Text(
                    text =
                        if (r.wordsProcessed > 0) {
                            stringResource(
                                R.string.settings_prefetch_last_result,
                                r.wordsProcessed,
                                r.gainedLocalOrRemotePreview,
                                r.stillNoImage,
                            )
                        } else {
                            stringResource(R.string.settings_prefetch_skipped)
                        },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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

            Text(stringResource(R.string.settings_build_info_section), style = MaterialTheme.typography.titleLarge)
            Text(
                stringResource(
                    R.string.settings_build_details,
                    BuildConfig.VERSION_NAME,
                    BuildConfig.VERSION_CODE,
                    BuildConfig.BUILD_KIND,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun appLanguageLabel(l: AppLanguage): String =
    when (l) {
        AppLanguage.SYSTEM -> stringResource(R.string.lang_system)
        AppLanguage.RUSSIAN -> stringResource(R.string.lang_russian)
        AppLanguage.ENGLISH -> stringResource(R.string.lang_english)
    }

@Composable
private fun trainingTextLanguageLabel(mode: TrainingTextLanguage): String =
    when (mode) {
        TrainingTextLanguage.RUSSIAN -> stringResource(R.string.card_text_russian)
        TrainingTextLanguage.ENGLISH -> stringResource(R.string.card_text_english)
        TrainingTextLanguage.BOTH -> stringResource(R.string.card_text_both)
        TrainingTextLanguage.NONE -> stringResource(R.string.card_text_none)
    }

@Composable
private fun preferredImageModeLabel(mode: PreferredImageMode): String =
    when (mode) {
        PreferredImageMode.BUNDLED_FIRST -> stringResource(R.string.pref_img_bundled_first)
        PreferredImageMode.CACHED_FIRST -> stringResource(R.string.pref_img_cached_first)
        PreferredImageMode.LOCAL_ONLY -> stringResource(R.string.pref_img_local_only)
        PreferredImageMode.LOCAL_THEN_REMOTE -> stringResource(R.string.pref_img_local_then_remote)
    }

@Composable
private fun imageRotationModeLabel(mode: ImageRotationMode): String =
    when (mode) {
        ImageRotationMode.REUSE_LOCAL_FIRST -> stringResource(R.string.image_rotation_reuse_local)
        ImageRotationMode.PREFER_NEW_REMOTE -> stringResource(R.string.image_rotation_prefer_new)
        ImageRotationMode.ALWAYS_TRY_NEW_REMOTE -> stringResource(R.string.image_rotation_always_new)
    }

@Composable
private fun onlineFetchLabel(mode: OnlineImageFetchingMode): String =
    when (mode) {
        OnlineImageFetchingMode.ENABLED -> stringResource(R.string.online_fetch_enabled)
        OnlineImageFetchingMode.DISABLED -> stringResource(R.string.online_fetch_disabled)
        OnlineImageFetchingMode.WIFI_ONLY -> stringResource(R.string.online_fetch_wifi_only)
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
