package ru.techlabhub.speechrehab.domain.repository

import ru.techlabhub.speechrehab.domain.model.TrainingMode
import kotlinx.coroutines.flow.Flow

/** Настройки тренировки, собранные для use case и UI; хранятся в DataStore. */
data class UserTrainingPreferences(
    val showWordHint: Boolean = true,
    val batchSize: Int = 12,
    val trainingMode: TrainingMode = TrainingMode.MIXED,
    /** Пустой набор = все категории. */
    val enabledCategoryIds: Set<Long> = emptySet(),
    val arasaacEnabled: Boolean = true,
    val pixabayEnabled: Boolean = true,
    val pexelsEnabled: Boolean = true,
)

/** Чтение и запись пользовательских настроек (реактивно через [preferencesFlow]). */
interface UserPreferencesRepository {
    val preferencesFlow: Flow<UserTrainingPreferences>

    suspend fun setShowWordHint(value: Boolean)

    suspend fun setBatchSize(value: Int)

    suspend fun setTrainingMode(mode: TrainingMode)

    suspend fun setEnabledCategoryIds(ids: Set<Long>)

    suspend fun setSourceEnabled(sourceArasaac: Boolean, sourcePixabay: Boolean, sourcePexels: Boolean)
}
