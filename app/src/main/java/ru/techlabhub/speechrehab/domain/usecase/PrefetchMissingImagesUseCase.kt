package ru.techlabhub.speechrehab.domain.usecase

import ru.techlabhub.speechrehab.domain.model.ImageSource
import ru.techlabhub.speechrehab.domain.model.OnlineImageFetchingMode
import ru.techlabhub.speechrehab.domain.model.PreferredImageMode
import ru.techlabhub.speechrehab.domain.repository.ImageRepository
import ru.techlabhub.speechrehab.domain.repository.UserTrainingPreferences
import ru.techlabhub.speechrehab.domain.repository.WordRepository
import kotlinx.coroutines.yield
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Фоновая дозагрузка картинок для слов без локального изображения.
 * Не связано с [ru.techlabhub.speechrehab.domain.model.TrainingMode.NEW_ONLY]: обходит все включённые для тренировки слова.
 * Для каждого слова вызывается [ImageRepository.resolveCard] (offline-first, затем сеть по prefs).
 */
data class PrefetchMissingImagesResult(
    val wordsProcessed: Int,
    val gainedLocalOrRemotePreview: Int,
    val stillNoImage: Int,
)

@Singleton
class PrefetchMissingImagesUseCase @Inject constructor(
    private val wordRepository: WordRepository,
    private val imageRepository: ImageRepository,
) {
    suspend operator fun invoke(prefs: UserTrainingPreferences): PrefetchMissingImagesResult {
        if (!prefs.refreshRemoteWhenNoLocalImage ||
            prefs.preferredImageMode == PreferredImageMode.LOCAL_ONLY ||
            prefs.onlineImageFetchingMode == OnlineImageFetchingMode.DISABLED
        ) {
            Timber.i("PrefetchMissingImages: skipped (prefs disallow remote prefetch)")
            return PrefetchMissingImagesResult(0, 0, 0)
        }
        wordRepository.ensureSeededIfEmpty()
        val words = wordRepository.getEnabledWordsForTraining(prefs.enabledCategoryIds)
        Timber.i("PrefetchMissingImages: started words=%d", words.size)
        var gained = 0
        var stillNone = 0
        for (word in words) {
            yield()
            val card = imageRepository.resolveCard(word, prefs)
            if (card.source == ImageSource.NONE && card.imageUri == null) {
                stillNone++
            } else {
                gained++
            }
        }
        val result =
            PrefetchMissingImagesResult(
                wordsProcessed = words.size,
                gainedLocalOrRemotePreview = gained,
                stillNoImage = stillNone,
            )
        Timber.i(
            "PrefetchMissingImages: finished processed=%d gained=%d stillMissing=%d",
            result.wordsProcessed,
            result.gainedLocalOrRemotePreview,
            result.stillNoImage,
        )
        return result
    }
}
