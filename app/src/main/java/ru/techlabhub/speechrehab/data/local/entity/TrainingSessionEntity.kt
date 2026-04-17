package ru.techlabhub.speechrehab.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "training_sessions")
data class TrainingSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startedAtEpochMillis: Long,
    val endedAtEpochMillis: Long? = null,
    val assistantNote: String? = null,
    /** [ru.techlabhub.speechrehab.domain.model.SessionKind] в виде строки. */
    val sessionKind: String = "ASSISTED",
)
