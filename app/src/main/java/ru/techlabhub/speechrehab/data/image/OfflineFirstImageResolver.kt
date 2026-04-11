package ru.techlabhub.speechrehab.data.image

import ru.techlabhub.speechrehab.data.cache.LocalImageCacheStore
import ru.techlabhub.speechrehab.data.local.SpeechRehabDatabase
import ru.techlabhub.speechrehab.data.local.entity.WordImageVariantEntity
import ru.techlabhub.speechrehab.data.network.NetworkCapabilitiesHelper
import ru.techlabhub.speechrehab.domain.image.ImageRotationRemotePolicy
import ru.techlabhub.speechrehab.domain.model.ImageCard
import ru.techlabhub.speechrehab.domain.model.ImageFetchPolicy
import ru.techlabhub.speechrehab.domain.model.ImageRotationMode
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
 * Offline-first: bundled → файловый кэш → (опционально) удалённые API с сохранением в [word_image_variants].
 *
 * [ImageRotationMode.PREFER_NEW_REMOTE] / [ImageRotationMode.ALWAYS_TRY_NEW_REMOTE]: после asset’ов
 * сначала запрос к API за **новым** URL относительно уже известных [WordImageVariantEntity.fetchSignature],
 * затем fallback на сохранённые файлы. Не связано с [ru.techlabhub.speechrehab.domain.model.TrainingMode].
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
    private val variantDao get() = db.wordImageVariantDao()

    suspend fun resolve(
        word: WordItem,
        prefs: UserTrainingPreferences,
        fetchPolicy: ImageFetchPolicy,
    ): ImageCard {
        val term = word.text.trim()
        if (term.isEmpty()) return none(word)

        val tryNewRemoteBeforeCache =
            fetchPolicy == ImageFetchPolicy.NORMAL &&
                prefs.imageRotationMode != ImageRotationMode.REUSE_LOCAL_FIRST &&
                remoteRotationFetchAllowed(prefs)

        Timber.d(
            "ImageResolve: wordId=%d text=%s rotation=%s tryNewRemoteFirst=%s policy=%s",
            word.id,
            term,
            prefs.imageRotationMode.name,
            tryNewRemoteBeforeCache,
            fetchPolicy.name,
        )

        val localOrder = localSteps(prefs.preferredImageMode)
        for (step in localOrder) {
            when (step) {
                LocalImageStep.BUNDLED -> {
                    val uri = bundled.tryAssetUri(word)
                    if (uri != null) {
                        Timber.d("ImageResolve: bundled wordId=%d", word.id)
                        return ImageCard(
                            word = word,
                            imageUri = uri,
                            remoteUrl = null,
                            source = ImageSource.BUNDLED,
                            fromOfflineCache = true,
                            wordImageVariantId = null,
                        )
                    }
                }
                LocalImageStep.CACHE -> {
                    if (!tryNewRemoteBeforeCache) {
                        val hit = cached.tryLoadCached(word.id)
                        if (hit != null) {
                            Timber.d(
                                "ImageResolve: reuse local variantId=%d localCandidatesReadable=yes",
                                hit.variantId,
                            )
                            return cardFromCachedHit(word, hit)
                        }
                    }
                }
            }
        }

        val rows = variantDao.listForWord(word.id)
        val knownSignatures = variantDao.fetchSignaturesForWord(word.id).toSet()
        Timber.d(
            "ImageResolve: dbRows=%d knownSignatures=%d",
            rows.size,
            knownSignatures.size,
        )

        if (tryNewRemoteBeforeCache) {
            val remoteCandidates = collectRemoteCandidates(term, prefs)
            Timber.d("ImageResolve: remoteCandidatesCount=%d (new-first)", remoteCandidates.size)
            val newCard =
                downloadFirstNewCandidate(
                    word = word,
                    knownSignatures = knownSignatures,
                    candidates = remoteCandidates,
                )
            if (newCard != null) {
                Timber.i(
                    "ImageResolve: chose NEW remote variantId=%s source=%s",
                    newCard.wordImageVariantId,
                    newCard.source.name,
                )
                return newCard
            }
            Timber.i("ImageResolve: no new remote — fallback local")
            val hit = cached.tryLoadCached(word.id)
            if (hit != null) {
                Timber.d("ImageResolve: fallback local variantId=%d", hit.variantId)
                return cardFromCachedHit(word, hit)
            }
            return none(word)
        }

        if (!remoteAllowedAfterLocalMiss(prefs)) {
            Timber.d("ImageResolve: remote blocked after local miss")
            return none(word)
        }

        val remoteCandidates = collectRemoteCandidates(term, prefs)
        Timber.d("ImageResolve: remoteCandidatesCount=%d (classic)", remoteCandidates.size)
        val newCard =
            downloadFirstNewCandidate(
                word = word,
                knownSignatures = knownSignatures,
                candidates = remoteCandidates,
            )
        if (newCard != null) return newCard
        Timber.w("ImageResolve: no image wordId=%d", word.id)
        return none(word)
    }

    suspend fun markVariantShown(variantId: Long) {
        variantDao.markShown(variantId, System.currentTimeMillis())
    }

    private suspend fun downloadFirstNewCandidate(
        word: WordItem,
        knownSignatures: Set<String>,
        candidates: List<RemoteImageCandidate>,
    ): ImageCard? {
        val fresh =
            RemoteImageCandidatePicker.firstUnknownCandidate(candidates, knownSignatures)
                ?: run {
                    Timber.d("ImageResolve: no unknown candidate (empty or all known)")
                    return null
                }
        val file = cacheStore.fileForWord(word.id, fresh.imageUrl)
        val ok = cacheStore.downloadToFile(fresh.imageUrl, file)
        if (!ok) {
            Timber.w("ImageResolve: download failed, return remote URL without variant")
            return ImageCard(
                word = word,
                imageUri = fresh.imageUrl,
                remoteUrl = fresh.imageUrl,
                source = fresh.source,
                fromOfflineCache = false,
                wordImageVariantId = null,
            )
        }
        val now = System.currentTimeMillis()
        val entity =
            WordImageVariantEntity(
                wordId = word.id,
                remoteUrl = fresh.imageUrl,
                localFilePath = file.absolutePath,
                sourceName = fresh.source.name,
                fetchSignature = fresh.fetchSignature(),
                wasShown = false,
                lastShownAtEpochMillis = 0L,
                createdAtEpochMillis = now,
            )
        val id = variantDao.insertOrExistingId(entity)
        return ImageCard(
            word = word,
            imageUri = file.absolutePath,
            remoteUrl = fresh.imageUrl,
            source = fresh.source,
            fromOfflineCache = false,
            wordImageVariantId = id,
        )
    }

    private suspend fun collectRemoteCandidates(
        term: String,
        prefs: UserTrainingPreferences,
    ): List<RemoteImageCandidate> {
        val maxPer = 15
        return buildList {
            if (prefs.arasaacEnabled) addAll(arasaac.fetchImageCandidates(term, maxPer))
            if (prefs.pixabayEnabled) addAll(pixabay.fetchImageCandidates(term, maxPer))
            if (prefs.pexelsEnabled) addAll(pexels.fetchImageCandidates(term, maxPer))
        }
    }

    private fun cardFromCachedHit(
        word: WordItem,
        hit: CachedImageResult,
    ): ImageCard =
        ImageCard(
            word = word,
            imageUri = hit.localFilePath,
            remoteUrl = hit.remoteUrl,
            source = mapCachedSource(hit.sourceName),
            fromOfflineCache = true,
            wordImageVariantId = hit.variantId,
        )

    private fun none(word: WordItem): ImageCard =
        ImageCard(
            word = word,
            imageUri = null,
            remoteUrl = null,
            source = ImageSource.NONE,
            fromOfflineCache = false,
            wordImageVariantId = null,
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
     * Классическая дозагрузка: только если локально совсем нет и [UserTrainingPreferences.refreshRemoteWhenNoLocalImage].
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

    /** Новый кадр из API: не требует refreshRemoteWhenNoLocalImage — только сеть и не LOCAL_ONLY. */
    private fun remoteRotationFetchAllowed(prefs: UserTrainingPreferences): Boolean =
        ImageRotationRemotePolicy.rotationFetchAllowed(
            preferredImageMode = prefs.preferredImageMode,
            onlineMode = prefs.onlineImageFetchingMode,
            arasaacEnabled = prefs.arasaacEnabled,
            pixabayEnabled = prefs.pixabayEnabled,
            pexelsEnabled = prefs.pexelsEnabled,
            isOnline = network.isOnline(),
            isWifi = network.isWifi(),
        )
}
