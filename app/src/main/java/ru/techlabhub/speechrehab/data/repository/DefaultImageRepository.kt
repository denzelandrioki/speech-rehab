package ru.techlabhub.speechrehab.data.repository

import ru.techlabhub.speechrehab.data.image.OfflineFirstImageResolver
import ru.techlabhub.speechrehab.domain.model.ImageCard
import ru.techlabhub.speechrehab.domain.model.ImageFetchPolicy
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
        fetchPolicy: ImageFetchPolicy,
    ): ImageCard {
        Timber.d(
            "ImageRepository.resolveCard wordId=%d text=%s fetchPolicy=%s rotation=%s preferred=%s online=%s refreshRemoteWhenNoLocal=%s",
            word.id,
            word.text,
            fetchPolicy.name,
            prefs.imageRotationMode.name,
            prefs.preferredImageMode.name,
            prefs.onlineImageFetchingMode.name,
            prefs.refreshRemoteWhenNoLocalImage,
        )
        return offlineFirstImageResolver.resolve(word, prefs, fetchPolicy)
    }

    override suspend fun markImageVariantShown(variantId: Long?) {
        if (variantId == null) {
            Timber.v("ImageRepository.markImageVariantShown skipped (null — bundled or remote-only card)")
            return
        }
        Timber.d("ImageRepository.markImageVariantShown variantId=%d", variantId)
        offlineFirstImageResolver.markVariantShown(variantId)
    }
}
