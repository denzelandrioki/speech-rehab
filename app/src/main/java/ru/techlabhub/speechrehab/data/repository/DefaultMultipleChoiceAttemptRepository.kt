package ru.techlabhub.speechrehab.data.repository

import ru.techlabhub.speechrehab.data.local.SpeechRehabDatabase
import ru.techlabhub.speechrehab.data.local.entity.MultipleChoiceAttemptEntity
import ru.techlabhub.speechrehab.domain.repository.MultipleChoiceAttemptRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultMultipleChoiceAttemptRepository @Inject constructor(
    db: SpeechRehabDatabase,
) : MultipleChoiceAttemptRepository {
    private val dao = db.multipleChoiceAttemptDao()

    override suspend fun insertAttempt(entity: MultipleChoiceAttemptEntity): Long = dao.insert(entity)
}
