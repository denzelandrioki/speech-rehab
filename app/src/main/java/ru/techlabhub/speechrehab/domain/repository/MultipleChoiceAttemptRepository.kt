package ru.techlabhub.speechrehab.domain.repository

import ru.techlabhub.speechrehab.data.local.entity.MultipleChoiceAttemptEntity

/** Запись попыток самостоятельного режима (отдельно от assisted [answer_attempts]). */
interface MultipleChoiceAttemptRepository {
    suspend fun insertAttempt(entity: MultipleChoiceAttemptEntity): Long
}
