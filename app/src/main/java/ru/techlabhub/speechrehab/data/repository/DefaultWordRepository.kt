package ru.techlabhub.speechrehab.data.repository

import ru.techlabhub.speechrehab.data.local.SpeechRehabDatabase
import ru.techlabhub.speechrehab.data.local.entity.CategoryEntity
import ru.techlabhub.speechrehab.data.local.entity.WordEntity
import ru.techlabhub.speechrehab.data.local.seed.VocabularyCatalog
import ru.techlabhub.speechrehab.data.mapper.toWordItem
import ru.techlabhub.speechrehab.domain.model.WordItem
import ru.techlabhub.speechrehab.domain.repository.WordRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Слова и категории: первичное заполнение из [VocabularyCatalog], наблюдение за списками через Flow,
 * выбор включённых слов для тренировки с учётом «пустой набор id = все категории» в вызывающем коде.
 */
@Singleton
class DefaultWordRepository @Inject constructor(
    db: SpeechRehabDatabase,
) : WordRepository {
    private val categoryDao = db.categoryDao()
    private val wordDao = db.wordDao()

    override suspend fun ensureSeededIfEmpty() {
        if (categoryDao.count() > 0 && wordDao.count() > 0) return

        if (categoryDao.count() == 0) {
            val cats = VocabularyCatalog.categories.map { CategoryEntity(name = it.name) }
            categoryDao.insertAll(cats)
        }

        if (wordDao.count() == 0) {
            val dbCategories = categoryDao.getAll()
            val byName = dbCategories.associateBy { it.name }
            val words = mutableListOf<WordEntity>()
            VocabularyCatalog.categories.forEach { seed ->
                val cid = byName[seed.name]?.id ?: return@forEach
                seed.words.forEach { w ->
                    words.add(
                        WordEntity(
                            text = w.textEn,
                            displayText = w.textRu,
                            categoryId = cid,
                            enabled = true,
                            isCustom = false,
                        ),
                    )
                }
            }
            if (words.isNotEmpty()) {
                wordDao.insertAll(words)
            }
        }
    }

    override fun observeEnabledWords(): Flow<List<WordItem>> =
        wordDao.observeEnabledWordsWithCategory().map { list -> list.map { it.toWordItem() } }

    override fun observeAllWords(): Flow<List<WordItem>> =
        wordDao.observeAllWordsWithCategory().map { list -> list.map { it.toWordItem() } }

    override suspend fun setWordEnabled(wordId: Long, enabled: Boolean) {
        wordDao.setEnabled(wordId, enabled)
    }

    override suspend fun getEnabledWordsForTraining(enabledCategoryIds: Set<Long>): List<WordItem> {
        val rows =
            if (enabledCategoryIds.isEmpty()) {
                wordDao.getAllEnabledWordsWithCategory()
            } else {
                wordDao.getEnabledWordsInCategories(enabledCategoryIds.toList())
            }
        return rows.map { it.toWordItem() }
    }
}
