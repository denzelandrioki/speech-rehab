package ru.techlabhub.speechrehab.domain.model

data class TrainingSession(
    val id: Long,
    val startedAtEpochMillis: Long,
    val endedAtEpochMillis: Long?,
    val assistantNote: String?,
)
