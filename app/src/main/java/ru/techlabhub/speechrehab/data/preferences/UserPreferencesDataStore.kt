package ru.techlabhub.speechrehab.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import ru.techlabhub.speechrehab.domain.model.AppLanguage
import ru.techlabhub.speechrehab.domain.model.ImageRotationMode
import ru.techlabhub.speechrehab.domain.model.OnlineImageFetchingMode
import ru.techlabhub.speechrehab.domain.model.PreferredImageMode
import ru.techlabhub.speechrehab.domain.model.TrainingMode
import ru.techlabhub.speechrehab.domain.model.TrainingTextLanguage
import ru.techlabhub.speechrehab.domain.repository.UserPreferencesRepository
import ru.techlabhub.speechrehab.domain.repository.UserTrainingPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.userPrefsDataStore by preferencesDataStore(name = "user_training_prefs")

/**
 * Реализация [UserPreferencesRepository] на DataStore Preferences.
 *
 * Ключи API для Pixabay/Pexels задаются в `local.properties` проекта ([BuildConfig]), не здесь.
 */
@Singleton
class UserPreferencesRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : UserPreferencesRepository {
    private val ds = context.userPrefsDataStore

    private object Keys {
        val showHint = booleanPreferencesKey("show_word_hint")
        val batchSize = intPreferencesKey("batch_size")
        val trainingMode = stringPreferencesKey("training_mode")
        val enabledCategories = stringSetPreferencesKey("enabled_category_ids")
        val arasaac = booleanPreferencesKey("src_arasaac")
        val pixabay = booleanPreferencesKey("src_pixabay")
        val pexels = booleanPreferencesKey("src_pexels")
        val appLanguage = stringPreferencesKey("app_language")
        val trainingTextLanguage = stringPreferencesKey("training_text_language")
        val onlineImageFetching = stringPreferencesKey("online_image_fetching")
        val preferredImageMode = stringPreferencesKey("preferred_image_mode")
        val refreshRemoteWhenNoLocalImage = booleanPreferencesKey("refresh_remote_when_no_local_image")
        val imageRotationMode = stringPreferencesKey("image_rotation_mode")
    }

    override val preferencesFlow: Flow<UserTrainingPreferences> =
        ds.data.map { p ->
            val modeName = p[Keys.trainingMode] ?: TrainingMode.MIXED.name
            val mode = runCatching { TrainingMode.valueOf(modeName) }.getOrDefault(TrainingMode.MIXED)
            val catStrings = p[Keys.enabledCategories].orEmpty()
            val appLang =
                runCatching { AppLanguage.valueOf(p[Keys.appLanguage] ?: "") }.getOrDefault(AppLanguage.SYSTEM)
            val cardText =
                runCatching { TrainingTextLanguage.valueOf(p[Keys.trainingTextLanguage] ?: "") }
                    .getOrDefault(TrainingTextLanguage.RUSSIAN)
            val onlineFetch =
                runCatching { OnlineImageFetchingMode.valueOf(p[Keys.onlineImageFetching] ?: "") }
                    .getOrDefault(OnlineImageFetchingMode.ENABLED)
            val prefImg =
                runCatching { PreferredImageMode.valueOf(p[Keys.preferredImageMode] ?: "") }
                    .getOrDefault(PreferredImageMode.LOCAL_THEN_REMOTE)
            val rotation =
                runCatching { ImageRotationMode.valueOf(p[Keys.imageRotationMode] ?: "") }
                    .getOrDefault(ImageRotationMode.REUSE_LOCAL_FIRST)
            UserTrainingPreferences(
                showWordHint = p[Keys.showHint] ?: true,
                batchSize = (p[Keys.batchSize] ?: 12).coerceIn(4, 40),
                trainingMode = mode,
                enabledCategoryIds = catStrings.mapNotNull { it.toLongOrNull() }.toSet(),
                arasaacEnabled = p[Keys.arasaac] ?: true,
                pixabayEnabled = p[Keys.pixabay] ?: true,
                pexelsEnabled = p[Keys.pexels] ?: true,
                appLanguage = appLang,
                trainingTextLanguage = cardText,
                onlineImageFetchingMode = onlineFetch,
                preferredImageMode = prefImg,
                refreshRemoteWhenNoLocalImage = p[Keys.refreshRemoteWhenNoLocalImage] ?: true,
                imageRotationMode = rotation,
            )
        }

    override suspend fun setShowWordHint(value: Boolean) {
        ds.edit { it[Keys.showHint] = value }
    }

    override suspend fun setBatchSize(value: Int) {
        ds.edit { it[Keys.batchSize] = value.coerceIn(4, 40) }
    }

    override suspend fun setTrainingMode(mode: TrainingMode) {
        ds.edit { it[Keys.trainingMode] = mode.name }
    }

    override suspend fun setEnabledCategoryIds(ids: Set<Long>) {
        ds.edit { it[Keys.enabledCategories] = ids.map { id -> id.toString() }.toSet() }
    }

    override suspend fun setSourceEnabled(
        sourceArasaac: Boolean,
        sourcePixabay: Boolean,
        sourcePexels: Boolean,
    ) {
        ds.edit {
            it[Keys.arasaac] = sourceArasaac
            it[Keys.pixabay] = sourcePixabay
            it[Keys.pexels] = sourcePexels
        }
    }

    override suspend fun setAppLanguage(language: AppLanguage) {
        ds.edit { it[Keys.appLanguage] = language.name }
    }

    override suspend fun setTrainingTextLanguage(mode: TrainingTextLanguage) {
        ds.edit { it[Keys.trainingTextLanguage] = mode.name }
    }

    override suspend fun setOnlineImageFetchingMode(mode: OnlineImageFetchingMode) {
        ds.edit { it[Keys.onlineImageFetching] = mode.name }
    }

    override suspend fun setPreferredImageMode(mode: PreferredImageMode) {
        ds.edit { it[Keys.preferredImageMode] = mode.name }
    }

    override suspend fun setRefreshRemoteWhenNoLocalImage(value: Boolean) {
        ds.edit { it[Keys.refreshRemoteWhenNoLocalImage] = value }
    }

    override suspend fun setImageRotationMode(mode: ImageRotationMode) {
        ds.edit { it[Keys.imageRotationMode] = mode.name }
    }
}
