package ru.techlabhub.speechrehab.domain.repository

import ru.techlabhub.speechrehab.domain.model.WordItem
import kotlinx.coroutines.flow.Flow

interface WordRepository {
    suspend fun ensureSeededIfEmpty()

    fun observeEnabledWords(): Flow<List<WordItem>>

    /** Все слова (включая отключённые) — экран словаря. */
    fun observeAllWords(): Flow<List<WordItem>>

    suspend fun setWordEnabled(wordId: Long, enabled: Boolean)

    suspend fun getEnabledWordsForTraining(enabledCategoryIds: Set<Long>): List<WordItem>
}
