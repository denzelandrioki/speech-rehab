package ru.techlabhub.speechrehab.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ru.techlabhub.speechrehab.data.local.entity.AnswerAttemptEntity

data class DayAggregateRow(
    val dayEpoch: Long,
    val attempts: Int,
    val correct: Long,
)

data class WordAggregateRow(
    val wordId: Long,
    val wordText: String,
    val categoryId: Long,
    val attempts: Int,
    val correct: Long,
)

data class CategoryAggregateRow(
    val categoryId: Long,
    val categoryName: String,
    val attempts: Int,
    val correct: Long,
)

@Dao
interface AnswerAttemptDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: AnswerAttemptEntity): Long

    @Query(
        """
        SELECT COUNT(*) AS attempts,
               COALESCE(SUM(CASE WHEN isCorrect THEN 1 ELSE 0 END), 0) AS correct
        FROM answer_attempts
        """,
    )
    suspend fun overallTotals(): OverallTotalsRow?

    @Query(
        """
        SELECT (shownAtEpochMillis / 86400000) AS dayEpoch,
               COUNT(*) AS attempts,
               COALESCE(SUM(CASE WHEN isCorrect THEN 1 ELSE 0 END), 0) AS correct
        FROM answer_attempts
        GROUP BY dayEpoch
        ORDER BY dayEpoch DESC
        LIMIT :limit
        """,
    )
    suspend fun dailyAggregatesLastDays(limit: Int): List<DayAggregateRow>

    @Query(
        """
        SELECT w.id AS wordId,
               CASE WHEN LENGTH(TRIM(COALESCE(w.displayText, ''))) > 0 THEN w.displayText ELSE w.text END AS wordText,
               w.categoryId AS categoryId,
               COUNT(a.id) AS attempts,
               SUM(CASE WHEN a.isCorrect THEN 1 ELSE 0 END) AS correct
        FROM answer_attempts a
        INNER JOIN words w ON w.id = a.wordId
        GROUP BY w.id
        HAVING COUNT(a.id) >= :minAttempts
        ORDER BY (CAST(SUM(CASE WHEN a.isCorrect THEN 1 ELSE 0 END) AS REAL) / COUNT(a.id)) ASC
        LIMIT :limit
        """,
    )
    suspend fun hardestWords(minAttempts: Int, limit: Int): List<WordAggregateRow>

    @Query(
        """
        SELECT w.id AS wordId,
               CASE WHEN LENGTH(TRIM(COALESCE(w.displayText, ''))) > 0 THEN w.displayText ELSE w.text END AS wordText,
               w.categoryId AS categoryId,
               COUNT(a.id) AS attempts,
               SUM(CASE WHEN a.isCorrect THEN 1 ELSE 0 END) AS correct
        FROM answer_attempts a
        INNER JOIN words w ON w.id = a.wordId
        GROUP BY w.id
        HAVING COUNT(a.id) >= :minAttempts
        ORDER BY (CAST(SUM(CASE WHEN a.isCorrect THEN 1 ELSE 0 END) AS REAL) / COUNT(a.id)) DESC
        LIMIT :limit
        """,
    )
    suspend fun easiestWords(minAttempts: Int, limit: Int): List<WordAggregateRow>

    @Query(
        """
        SELECT c.id AS categoryId,
               c.name AS categoryName,
               COUNT(a.id) AS attempts,
               SUM(CASE WHEN a.isCorrect THEN 1 ELSE 0 END) AS correct
        FROM answer_attempts a
        INNER JOIN words w ON w.id = a.wordId
        INNER JOIN categories c ON c.id = w.categoryId
        GROUP BY c.id
        ORDER BY c.name ASC
        """,
    )
    suspend fun byCategory(): List<CategoryAggregateRow>

    @Query(
        """
        SELECT COUNT(*) FROM answer_attempts
        WHERE wordId = :wordId
        """,
    )
    suspend fun attemptsForWord(wordId: Long): Int

    @Query(
        """
        SELECT isCorrect FROM answer_attempts
        WHERE wordId = :wordId
        ORDER BY shownAtEpochMillis DESC, id DESC
        LIMIT :limit
        """,
    )
    suspend fun recentResultsForWord(wordId: Long, limit: Int): List<Boolean>

    @Query(
        """
        SELECT isCorrect FROM answer_attempts
        WHERE shownAtEpochMillis >= :fromMillis
        ORDER BY shownAtEpochMillis ASC, id ASC
        """,
    )
    suspend fun resultsInWindow(fromMillis: Long): List<Boolean>

    @Query(
        """
        SELECT wordId, COUNT(*) AS cnt
        FROM answer_attempts
        WHERE wordId IN (:wordIds)
        GROUP BY wordId
        """,
    )
    suspend fun attemptCountsForWords(wordIds: List<Long>): List<WordAttemptCountRow>
}

data class WordAttemptCountRow(
    val wordId: Long,
    val cnt: Int,
)

data class OverallTotalsRow(
    val attempts: Long?,
    val correct: Long?,
)
