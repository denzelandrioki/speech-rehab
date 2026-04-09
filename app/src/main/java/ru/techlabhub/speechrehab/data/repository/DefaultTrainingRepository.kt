package ru.techlabhub.speechrehab.data.repository

import ru.techlabhub.speechrehab.data.local.SpeechRehabDatabase
import ru.techlabhub.speechrehab.data.local.entity.AnswerAttemptEntity
import ru.techlabhub.speechrehab.data.local.entity.TrainingSessionEntity
import ru.techlabhub.speechrehab.domain.repository.TrainingRepository
import androidx.room.withTransaction
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Запись тренировочных сессий и ответов в одной транзакции Room: попытка + обновление серий
 * [WordEntity.consecutiveCorrect] / [WordEntity.consecutiveIncorrect].
 */
@Singleton
class DefaultTrainingRepository @Inject constructor(
    private val db: SpeechRehabDatabase,
) : TrainingRepository {
    private val sessions = db.trainingSessionDao()
    private val attempts = db.answerAttemptDao()
    private val words = db.wordDao()

    override suspend fun startSession(): Long {
        val id =
            sessions.insert(
                TrainingSessionEntity(
                    startedAtEpochMillis = System.currentTimeMillis(),
                ),
            )
        return id
    }

    override suspend fun endSession(sessionId: Long, assistantNote: String?) {
        sessions.endSession(
            sessionId = sessionId,
            endedAt = System.currentTimeMillis(),
            note = assistantNote,
        )
    }

    override suspend fun recordAnswer(
        sessionId: Long?,
        wordId: Long,
        isCorrect: Boolean,
        assistantNote: String?,
    ) {
        db.withTransaction {
            attempts.insert(
                AnswerAttemptEntity(
                    sessionId = sessionId,
                    wordId = wordId,
                    shownAtEpochMillis = System.currentTimeMillis(),
                    isCorrect = isCorrect,
                    assistantNote = assistantNote,
                ),
            )
            val w = words.getById(wordId) ?: return@withTransaction
            if (isCorrect) {
                words.updateStreaks(wordId, w.consecutiveCorrect + 1, 0)
            } else {
                words.updateStreaks(wordId, 0, w.consecutiveIncorrect + 1)
            }
        }
    }
}
