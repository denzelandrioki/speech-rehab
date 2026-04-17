package ru.techlabhub.speechrehab.domain.repository

import ru.techlabhub.speechrehab.domain.model.SessionKind

/** Сессия тренировки и запись ответов пользователя (и обновление серий по словам внутри реализации). */
interface TrainingRepository {
    suspend fun startSession(kind: SessionKind = SessionKind.ASSISTED): Long

    suspend fun endSession(sessionId: Long, assistantNote: String?)

    suspend fun recordAnswer(
        sessionId: Long?,
        wordId: Long,
        isCorrect: Boolean,
        assistantNote: String?,
    )
}
