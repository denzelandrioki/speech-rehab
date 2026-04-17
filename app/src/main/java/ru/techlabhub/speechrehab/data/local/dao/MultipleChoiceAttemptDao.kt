package ru.techlabhub.speechrehab.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ru.techlabhub.speechrehab.data.local.entity.MultipleChoiceAttemptEntity

@Dao
interface MultipleChoiceAttemptDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: MultipleChoiceAttemptEntity): Long

    @Query(
        """
        SELECT COUNT(*) AS attempts,
               COALESCE(SUM(CASE WHEN isCorrect THEN 1 ELSE 0 END), 0) AS correct
        FROM multiple_choice_attempts
        """,
    )
    suspend fun overallTotals(): OverallTotalsRow?

    @Query("SELECT AVG(responseTimeMillis) FROM multiple_choice_attempts")
    suspend fun averageResponseTimeMillis(): Double?

    @Query(
        """
        SELECT (answeredAtEpochMillis / 86400000) AS dayEpoch,
               COUNT(*) AS attempts,
               COALESCE(SUM(CASE WHEN isCorrect THEN 1 ELSE 0 END), 0) AS correct
        FROM multiple_choice_attempts
        GROUP BY dayEpoch
        ORDER BY dayEpoch DESC
        LIMIT :limit
        """,
    )
    suspend fun dailyAggregatesLastDays(limit: Int): List<DayAggregateRow>

    @Query(
        """
        SELECT w.id AS wordId,
               w.text AS canonicalText,
               w.displayTextRu AS displayTextRu,
               w.displayTextEn AS displayTextEn,
               w.categoryId AS categoryId,
               COUNT(m.id) AS attempts,
               SUM(CASE WHEN m.isCorrect THEN 1 ELSE 0 END) AS correct
        FROM multiple_choice_attempts m
        INNER JOIN words w ON w.id = m.questionWordId
        GROUP BY w.id
        HAVING COUNT(m.id) >= :minAttempts
        ORDER BY (CAST(SUM(CASE WHEN m.isCorrect THEN 1 ELSE 0 END) AS REAL) / COUNT(m.id)) ASC
        LIMIT :limit
        """,
    )
    suspend fun hardestWords(
        minAttempts: Int,
        limit: Int,
    ): List<WordAggregateRow>

    @Query(
        """
        SELECT c.id AS categoryId,
               c.name AS categoryName,
               COUNT(m.id) AS attempts,
               SUM(CASE WHEN m.isCorrect THEN 1 ELSE 0 END) AS correct
        FROM multiple_choice_attempts m
        INNER JOIN words w ON w.id = m.questionWordId
        INNER JOIN categories c ON c.id = w.categoryId
        GROUP BY c.id
        ORDER BY c.name ASC
        """,
    )
    suspend fun byCategory(): List<CategoryAggregateRow>

    @Query(
        """
        SELECT m.questionWordId AS correctWordId,
               m.selectedWordId AS wrongWordId,
               COUNT(*) AS cnt,
               wc.text AS correctCanonical,
               wc.displayTextRu AS correctRu,
               wc.displayTextEn AS correctEn,
               ws.text AS wrongCanonical,
               ws.displayTextRu AS wrongRu,
               ws.displayTextEn AS wrongEn
        FROM multiple_choice_attempts m
        INNER JOIN words wc ON wc.id = m.questionWordId
        INNER JOIN words ws ON ws.id = m.selectedWordId
        WHERE m.isCorrect = 0 AND m.selectedWordId != m.questionWordId
        GROUP BY m.questionWordId, m.selectedWordId
        ORDER BY cnt DESC
        LIMIT :limit
        """,
    )
    suspend fun topConfusionPairs(limit: Int): List<McConfusionRow>

    @Query(
        """
        SELECT m.selectedWordId AS wordId,
               COUNT(*) AS cnt
        FROM multiple_choice_attempts m
        WHERE m.isCorrect = 0
        GROUP BY m.selectedWordId
        ORDER BY cnt DESC
        LIMIT :limit
        """,
    )
    suspend fun topWrongSelections(limit: Int): List<WrongPickCountRow>

    @Query(
        """
        SELECT isCorrect FROM multiple_choice_attempts
        WHERE answeredAtEpochMillis >= :fromMillis
        ORDER BY answeredAtEpochMillis ASC, id ASC
        """,
    )
    suspend fun resultsInWindow(fromMillis: Long): List<Boolean>
}

data class McConfusionRow(
    val correctWordId: Long,
    val wrongWordId: Long,
    val cnt: Int,
    val correctCanonical: String,
    val correctRu: String,
    val correctEn: String,
    val wrongCanonical: String,
    val wrongRu: String,
    val wrongEn: String,
)

data class WrongPickCountRow(
    val wordId: Long,
    val cnt: Int,
)
