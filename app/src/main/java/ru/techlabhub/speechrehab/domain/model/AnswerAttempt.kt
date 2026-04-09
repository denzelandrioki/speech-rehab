package ru.techlabhub.speechrehab.domain.model

data class AnswerAttempt(
    val id: Long,
    val sessionId: Long?,
    val wordId: Long,
    val wordText: String,
    val shownAtEpochMillis: Long,
    val isCorrect: Boolean,
    val assistantNote: String?,
)
