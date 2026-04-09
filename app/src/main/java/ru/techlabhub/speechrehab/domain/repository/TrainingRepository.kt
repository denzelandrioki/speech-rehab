package ru.techlabhub.speechrehab.domain.repository

interface TrainingRepository {
    suspend fun startSession(): Long

    suspend fun endSession(sessionId: Long, assistantNote: String?)

    suspend fun recordAnswer(
        sessionId: Long?,
        wordId: Long,
        isCorrect: Boolean,
        assistantNote: String?,
    )
}
