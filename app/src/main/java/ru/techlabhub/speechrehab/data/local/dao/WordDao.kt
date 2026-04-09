package ru.techlabhub.speechrehab.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ru.techlabhub.speechrehab.data.local.entity.WordEntity
import kotlinx.coroutines.flow.Flow

data class WordWithCategoryRow(
    val id: Long,
    val text: String,
    val displayText: String,
    val categoryId: Long,
    val enabled: Boolean,
    val isCustom: Boolean,
    val consecutiveCorrect: Int,
    val consecutiveIncorrect: Int,
    val categoryName: String,
)

@Dao
interface WordDao {
    @Query("SELECT COUNT(*) FROM words")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(items: List<WordEntity>): List<Long>

    @Query(
        """
        SELECT w.id, w.text, w.displayText, w.categoryId, w.enabled, w.isCustom, w.consecutiveCorrect, w.consecutiveIncorrect,
               c.name AS categoryName
        FROM words w
        INNER JOIN categories c ON c.id = w.categoryId
        ORDER BY c.name ASC, w.displayText ASC, w.text ASC
        """,
    )
    fun observeAllWordsWithCategory(): Flow<List<WordWithCategoryRow>>

    @Query(
        """
        SELECT w.id, w.text, w.displayText, w.categoryId, w.enabled, w.isCustom, w.consecutiveCorrect, w.consecutiveIncorrect,
               c.name AS categoryName
        FROM words w
        INNER JOIN categories c ON c.id = w.categoryId
        WHERE w.enabled = 1
        ORDER BY w.displayText ASC, w.text ASC
        """,
    )
    fun observeEnabledWordsWithCategory(): Flow<List<WordWithCategoryRow>>

    @Query(
        """
        SELECT w.id, w.text, w.displayText, w.categoryId, w.enabled, w.isCustom, w.consecutiveCorrect, w.consecutiveIncorrect,
               c.name AS categoryName
        FROM words w
        INNER JOIN categories c ON c.id = w.categoryId
        WHERE w.enabled = 1 AND w.categoryId IN (:categoryIds)
        ORDER BY w.displayText ASC, w.text ASC
        """,
    )
    suspend fun getEnabledWordsInCategories(categoryIds: List<Long>): List<WordWithCategoryRow>

    @Query(
        """
        SELECT w.id, w.text, w.displayText, w.categoryId, w.enabled, w.isCustom, w.consecutiveCorrect, w.consecutiveIncorrect,
               c.name AS categoryName
        FROM words w
        INNER JOIN categories c ON c.id = w.categoryId
        WHERE w.enabled = 1
        ORDER BY w.displayText ASC, w.text ASC
        """,
    )
    suspend fun getAllEnabledWordsWithCategory(): List<WordWithCategoryRow>

    @Query("SELECT * FROM words WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): WordEntity?

    @Query("UPDATE words SET enabled = :enabled WHERE id = :wordId")
    suspend fun setEnabled(wordId: Long, enabled: Boolean)

    @Query(
        """
        UPDATE words SET consecutiveCorrect = :correct, consecutiveIncorrect = :incorrect
        WHERE id = :wordId
        """,
    )
    suspend fun updateStreaks(wordId: Long, correct: Int, incorrect: Int)

    @Query(
        """
        SELECT w.id FROM words w
        WHERE w.enabled = 1
        AND (
          SELECT COUNT(*) FROM answer_attempts a WHERE a.wordId = w.id
        ) < :maxAttempts
        """,
    )
    suspend fun wordIdsWithTotalAttemptsBelow(maxAttempts: Int): List<Long>
}
