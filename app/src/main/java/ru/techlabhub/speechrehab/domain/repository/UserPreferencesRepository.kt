package ru.techlabhub.speechrehab.domain.repository

import ru.techlabhub.speechrehab.domain.model.AppLanguage
import ru.techlabhub.speechrehab.domain.model.ImageRotationMode
import ru.techlabhub.speechrehab.domain.model.OnlineImageFetchingMode
import ru.techlabhub.speechrehab.domain.model.PreferredImageMode
import ru.techlabhub.speechrehab.domain.model.TrainingMode
import ru.techlabhub.speechrehab.domain.model.TrainingTextLanguage
import kotlinx.coroutines.flow.Flow

/** Настройки тренировки и отображения, собранные для use case и UI; хранятся в DataStore. */
data class UserTrainingPreferences(
    val showWordHint: Boolean = true,
    val batchSize: Int = 12,
    val trainingMode: TrainingMode = TrainingMode.MIXED,
    /** Пустой набор = все категории. */
    val enabledCategoryIds: Set<Long> = emptySet(),
    val arasaacEnabled: Boolean = true,
    val pixabayEnabled: Boolean = true,
    val pexelsEnabled: Boolean = true,
    /** Язык интерфейса (независимо от подписи карточки). */
    val appLanguage: AppLanguage = AppLanguage.SYSTEM,
    /** Как показывать текст на карточке тренировки. */
    val trainingTextLanguage: TrainingTextLanguage = TrainingTextLanguage.RUSSIAN,
    /** Разрешение сетевой подгрузки изображений (после локальных источников). */
    val onlineImageFetchingMode: OnlineImageFetchingMode = OnlineImageFetchingMode.ENABLED,
    /** Порядок локальных источников и политика «только локально». */
    val preferredImageMode: PreferredImageMode = PreferredImageMode.LOCAL_THEN_REMOTE,
    /**
     * Если локальной картинки нет (bundled + кэш), пытаться загрузить из сети при разрешённом online fetch.
     * Не связано с [TrainingMode.NEW_ONLY] (новые слова по попыткам — отдельный конвейер).
     */
    val refreshRemoteWhenNoLocalImage: Boolean = true,
    /** Подбор новой иллюстрации для того же слова (не путать с [TrainingMode]). */
    val imageRotationMode: ImageRotationMode = ImageRotationMode.REUSE_LOCAL_FIRST,
)

/** Чтение и запись пользовательских настроек (реактивно через [preferencesFlow]). */
interface UserPreferencesRepository {
    val preferencesFlow: Flow<UserTrainingPreferences>

    suspend fun setShowWordHint(value: Boolean)

    suspend fun setBatchSize(value: Int)

    suspend fun setTrainingMode(mode: TrainingMode)

    suspend fun setEnabledCategoryIds(ids: Set<Long>)

    suspend fun setSourceEnabled(sourceArasaac: Boolean, sourcePixabay: Boolean, sourcePexels: Boolean)

    suspend fun setAppLanguage(language: AppLanguage)

    suspend fun setTrainingTextLanguage(mode: TrainingTextLanguage)

    suspend fun setOnlineImageFetchingMode(mode: OnlineImageFetchingMode)

    suspend fun setPreferredImageMode(mode: PreferredImageMode)

    suspend fun setRefreshRemoteWhenNoLocalImage(value: Boolean)

    suspend fun setImageRotationMode(mode: ImageRotationMode)
}
