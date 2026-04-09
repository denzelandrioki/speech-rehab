package ru.techlabhub.speechrehab.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ru.techlabhub.speechrehab.data.local.entity.TrainingSessionEntity

@Dao
interface TrainingSessionDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: TrainingSessionEntity): Long

    @Query(
        """
        UPDATE training_sessions SET endedAtEpochMillis = :endedAt, assistantNote = COALESCE(:note, assistantNote)
        WHERE id = :sessionId
        """,
    )
    suspend fun endSession(sessionId: Long, endedAt: Long, note: String?)
}
