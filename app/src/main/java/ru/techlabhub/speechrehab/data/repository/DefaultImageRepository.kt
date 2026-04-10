package ru.techlabhub.speechrehab.data.repository

import ru.techlabhub.speechrehab.data.image.OfflineFirstImageResolver
import ru.techlabhub.speechrehab.domain.model.ImageCard
import ru.techlabhub.speechrehab.domain.model.WordItem
import ru.techlabhub.speechrehab.domain.repository.ImageRepository
import ru.techlabhub.speechrehab.domain.repository.UserTrainingPreferences
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultImageRepository @Inject constructor(
    private val offlineFirstImageResolver: OfflineFirstImageResolver,
) : ImageRepository {

    override suspend fun resolveCard(
        word: WordItem,
        prefs: UserTrainingPreferences,
    ): ImageCard = offlineFirstImageResolver.resolve(word, prefs)
}
