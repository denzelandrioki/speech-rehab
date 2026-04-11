package ru.techlabhub.speechrehab.data.repository

import ru.techlabhub.speechrehab.data.image.OfflineFirstImageResolver
import ru.techlabhub.speechrehab.domain.model.ImageCard
import ru.techlabhub.speechrehab.domain.model.WordItem
import ru.techlabhub.speechrehab.domain.repository.ImageRepository
import ru.techlabhub.speechrehab.domain.repository.UserTrainingPreferences
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultImageRepository @Inject constructor(
    private val offlineFirstImageResolver: OfflineFirstImageResolver,
) : ImageRepository {

    override suspend fun resolveCard(
        word: WordItem,
        prefs: UserTrainingPreferences,
    ): ImageCard {
        Timber.d("ImageRepository.resolveCard started wordId=%d text=%s", word.id, word.text)
        return offlineFirstImageResolver.resolve(word, prefs)
    }
}
