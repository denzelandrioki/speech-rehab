package ru.techlabhub.speechrehab.data.image

import ru.techlabhub.speechrehab.data.cache.LocalImageCacheStore
import ru.techlabhub.speechrehab.data.local.entity.CachedImageEntity
import ru.techlabhub.speechrehab.data.local.SpeechRehabDatabase
import ru.techlabhub.speechrehab.data.network.NetworkCapabilitiesHelper
import ru.techlabhub.speechrehab.domain.model.ImageCard
import ru.techlabhub.speechrehab.domain.model.ImageSource
import ru.techlabhub.speechrehab.domain.model.OnlineImageFetchingMode
import ru.techlabhub.speechrehab.domain.model.PreferredImageMode
import ru.techlabhub.speechrehab.domain.model.WordItem
import ru.techlabhub.speechrehab.domain.repository.UserTrainingPreferences
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

private enum class LocalImageStep {
    BUNDLED,
    CACHE,
}

/**
 * Offline-first: bundled → файловый кэш → (опционально) удалённые API с сохранением в кэш.
 * Режим тренировки ([ru.techlabhub.speechrehab.domain.model.TrainingMode]) сюда не передаётся:
 * «новое слово» по попыткам и «нет локальной картинки» — разные вещи; для любого выбранного слова
 * при отсутствии локального изображения выполняется та же цепочка remote (если настройки разрешают).
 *
 * Точка расширения для prefetch / backend без смены контракта [ImageCard].
 */
@Singleton
class OfflineFirstImageResolver @Inject constructor(
    private val bundled: DefaultBundledImageDataSource,
    private val cached: DefaultCachedFileImageDataSource,
    private val arasaac: DefaultArasaacImageDataSource,
    private val pixabay: DefaultPixabayImageDataSource,
    private val pexels: DefaultPexelsImageDataSource,
    private val db: SpeechRehabDatabase,
    private val cacheStore: LocalImageCacheStore,
    private val network: NetworkCapabilitiesHelper,
) {
    private val cachedImageDao get() = db.cachedImageDao()

    suspend fun resolve(
        word: WordItem,
        prefs: UserTrainingPreferences,
    ): ImageCard {
        val term = word.text.trim()
        if (term.isEmpty()) {
            return none(word)
        }

        val localOrder = localSteps(prefs.preferredImageMode)
        for (step in localOrder) {
            when (step) {
                LocalImageStep.BUNDLED -> {
                    val uri = bundled.tryAssetUri(word)
                    if (uri != null) {
                        return ImageCard(
                            word = word,
                            imageUri = uri,
                            remoteUrl = null,
                            source = ImageSource.BUNDLED,
                            fromOfflineCache = true,
                        )
                    }
                }
                LocalImageStep.CACHE -> {
                    val hit = cached.tryLoadCached(word.id)
                    if (hit != null) {
                        return ImageCard(
                            word = word,
                            imageUri = hit.localFilePath,
                            remoteUrl = hit.remoteUrl,
                            source = mapCachedSource(hit.sourceName),
                            fromOfflineCache = true,
                        )
                    }
                }
            }
        }

        if (!remoteAllowedAfterLocalMiss(prefs)) {
            Timber.d("Remote image skipped (policy, network, or refreshRemoteWhenNoLocalImage=false) word=%s", word.text)
            return none(word)
        }

        var chosenUrl: String? = null
        var chosenSource = ImageSource.ARASAAC

        if (prefs.arasaacEnabled) {
            val u = arasaac.fetchImageUrl(term)
            if (!u.isNullOrBlank()) {
                chosenUrl = u
                chosenSource = ImageSource.ARASAAC
            }
        }

        if (chosenUrl.isNullOrBlank() && prefs.pixabayEnabled) {
            val u = pixabay.fetchImageUrl(term)
            if (!u.isNullOrBlank()) {
                chosenUrl = u
                chosenSource = ImageSource.PIXABAY
            }
        }

        if (chosenUrl.isNullOrBlank() && prefs.pexelsEnabled) {
            val u = pexels.fetchImageUrl(term)
            if (!u.isNullOrBlank()) {
                chosenUrl = u
                chosenSource = ImageSource.PEXELS
            }
        }

        if (chosenUrl.isNullOrBlank()) {
            Timber.w("No remote image for word=%s", word.text)
            return none(word)
        }

        val file = cacheStore.fileForWord(word.id, chosenUrl)
        val ok = cacheStore.downloadToFile(chosenUrl, file)
        if (!ok) {
            return ImageCard(
                word = word,
                imageUri = chosenUrl,
                remoteUrl = chosenUrl,
                source = chosenSource,
                fromOfflineCache = false,
            )
        }

        cachedImageDao.upsert(
            CachedImageEntity(
                wordId = word.id,
                remoteUrl = chosenUrl,
                localFilePath = file.absolutePath,
                sourceName = chosenSource.name,
                updatedAtEpochMillis = System.currentTimeMillis(),
            ),
        )

        return ImageCard(
            word = word,
            imageUri = file.absolutePath,
            remoteUrl = chosenUrl,
            source = chosenSource,
            fromOfflineCache = false,
        )
    }

    private fun none(word: WordItem): ImageCard =
        ImageCard(
            word = word,
            imageUri = null,
            remoteUrl = null,
            source = ImageSource.NONE,
            fromOfflineCache = false,
        )

    private fun mapCachedSource(name: String): ImageSource =
        runCatching { ImageSource.valueOf(name) }.getOrDefault(ImageSource.LOCAL_CACHE)

    private fun localSteps(mode: PreferredImageMode): List<LocalImageStep> =
        when (mode) {
            PreferredImageMode.CACHED_FIRST ->
                listOf(LocalImageStep.CACHE, LocalImageStep.BUNDLED)
            PreferredImageMode.BUNDLED_FIRST,
            PreferredImageMode.LOCAL_ONLY,
            PreferredImageMode.LOCAL_THEN_REMOTE,
            -> listOf(LocalImageStep.BUNDLED, LocalImageStep.CACHE)
        }

    /**
     * Сеть после неудачи локальных шагов: отдельно от выбора слова в тренировке.
     * [UserTrainingPreferences.refreshRemoteWhenNoLocalImage] выключает дозагрузку, не меняя offline-first порядок.
     */
    private fun remoteAllowedAfterLocalMiss(prefs: UserTrainingPreferences): Boolean {
        if (!prefs.refreshRemoteWhenNoLocalImage) {
            return false
        }
        if (prefs.preferredImageMode == PreferredImageMode.LOCAL_ONLY) {
            return false
        }
        return when (prefs.onlineImageFetchingMode) {
            OnlineImageFetchingMode.DISABLED -> false
            OnlineImageFetchingMode.ENABLED -> network.isOnline()
            OnlineImageFetchingMode.WIFI_ONLY -> network.isOnline() && network.isWifi()
        }
    }
}
