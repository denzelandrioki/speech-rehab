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
 * Offline-first: при обычной политике — bundled → файловый кэш → (опционально) удалённые API.
 *
 * [ImageRotationMode.PREFER_NEW_REMOTE] / [ImageRotationMode.ALWAYS_TRY_NEW_REMOTE]:
 * после пропуска кэша (см. ниже) запрос к API за **новым** URL относительно [WordImageVariantEntity.fetchSignature],
 * затем fallback на сохранённые файлы и в конце — **встроенный asset**, если remote не дал нового кандидата.
 *
 * Важно: при активной «ротации remote» **нельзя** сразу возвращать bundled — иначе API-ветка никогда
 * не выполняется для слов с картинкой из assets.
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

        val rotationFetchAllowed = remoteRotationFetchAllowed(prefs)
        val tryNewRemoteBeforeCache =
            fetchPolicy == ImageFetchPolicy.NORMAL &&
                prefs.imageRotationMode != ImageRotationMode.REUSE_LOCAL_FIRST &&
                rotationFetchAllowed

        if (!rotationFetchAllowed &&
            prefs.imageRotationMode != ImageRotationMode.REUSE_LOCAL_FIRST &&
            fetchPolicy == ImageFetchPolicy.NORMAL
        ) {
            Timber.i(
                "ImageResolve: rotation remote blocked preferred=%s online=%s arasaac=%s pixabay=%s pexels=%s online=%s wifi=%s",
                prefs.preferredImageMode.name,
                prefs.onlineImageFetchingMode.name,
                prefs.arasaacEnabled,
                prefs.pixabayEnabled,
                prefs.pexelsEnabled,
                network.isOnline(),
                network.isWifi(),
            )
        }

        Timber.d(
            "ImageResolve: start wordId=%d text=%s fetchPolicy=%s rotation=%s tryNewRemoteFirst=%s preferred=%s online=%s refreshRemoteWhenNoLocal=%s rotationFetchAllowed=%s",
            word.id,
            term,
            fetchPolicy.name,
            prefs.imageRotationMode.name,
            tryNewRemoteBeforeCache,
            prefs.preferredImageMode.name,
            prefs.onlineImageFetchingMode.name,
            prefs.refreshRemoteWhenNoLocalImage,
            rotationFetchAllowed,
        )

        val localOrder = localSteps(prefs.preferredImageMode)
        for (step in localOrder) {
            when (step) {
                LocalImageStep.BUNDLED -> {
                    // Приоритет «новая картинка из сети»: не отдаём bundled до попытки remote + fallback кэша.
                    if (tryNewRemoteBeforeCache) {
                        Timber.d("ImageResolve: skip bundled (rotation new-remote-first) wordId=%d", word.id)
                    } else {
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
                }
                LocalImageStep.CACHE -> {
                    if (!tryNewRemoteBeforeCache) {
                        val hit = cached.tryLoadCached(word.id)
                        if (hit != null) {
                            Timber.d(
                                "ImageResolve: reuse local variantId=%d",
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
        val shownCount = rows.count { it.wasShown }
        Timber.d(
            "ImageResolve: db variants total=%d wasShown=%d knownSignatures=%d",
            rows.size,
            shownCount,
            knownSignatures.size,
        )

        if (tryNewRemoteBeforeCache) {
            Timber.i(
                "ImageResolve: rotation remote attempt started wordId=%d mode=%s",
                word.id,
                prefs.imageRotationMode.name,
            )
            val remoteCandidates = collectRemoteCandidates(term, prefs)
            val newBySig = remoteCandidates.count { it.fetchSignature() !in knownSignatures }
            Timber.d(
                "ImageResolve: remote candidates total=%d notYetInDb(newBySignature)=%d",
                remoteCandidates.size,
                newBySig,
            )
            val newCard =
                downloadFirstNewCandidate(
                    word = word,
                    knownSignatures = knownSignatures,
                    candidates = remoteCandidates,
                )
            if (newCard != null) {
                Timber.i(
                    "ImageResolve: chose NEW remote variantId=%s source=%s uriPrefix=%s",
                    newCard.wordImageVariantId,
                    newCard.source.name,
                    newCard.imageUri?.take(48) ?: newCard.remoteUrl?.take(48) ?: "null",
                )
                return newCard
            }
            Timber.i("ImageResolve: no new remote candidates — fallback local cache wordId=%d", word.id)
            val hit = cached.tryLoadCached(word.id)
            if (hit != null) {
                Timber.d("ImageResolve: fallback local variantId=%d", hit.variantId)
                return cardFromCachedHit(word, hit)
            }
            val bundleUri = bundled.tryAssetUri(word)
            if (bundleUri != null) {
                Timber.i("ImageResolve: fallback bundled after rotation miss wordId=%d", word.id)
                return ImageCard(
                    word = word,
                    imageUri = bundleUri,
                    remoteUrl = null,
                    source = ImageSource.BUNDLED,
                    fromOfflineCache = true,
                    wordImageVariantId = null,
                )
            }
            Timber.w("ImageResolve: no image after rotation (no remote, no cache, no bundled) wordId=%d", word.id)
            return none(word)
        }

        if (!remoteAllowedAfterLocalMiss(prefs)) {
            Timber.d("ImageResolve: remote blocked after local miss (classic path)")
            return none(word)
        }

        val remoteCandidates = collectRemoteCandidates(term, prefs)
        val newBySig = remoteCandidates.count { it.fetchSignature() !in knownSignatures }
        Timber.d(
            "ImageResolve: classic remote total=%d newBySignature=%d",
            remoteCandidates.size,
            newBySig,
        )
        val newCard =
            downloadFirstNewCandidate(
                word = word,
                knownSignatures = knownSignatures,
                candidates = remoteCandidates,
            )
        if (newCard != null) {
            Timber.i(
                "ImageResolve: classic chose remote variantId=%s source=%s",
                newCard.wordImageVariantId,
                newCard.source.name,
            )
            return newCard
        }
        Timber.w("ImageResolve: no image wordId=%d (classic path)", word.id)
        return none(word)
    }

    suspend fun markVariantShown(variantId: Long) {
        val now = System.currentTimeMillis()
        variantDao.markShown(variantId, now)
        Timber.d("ImageResolve: markVariantShown variantId=%d lastShownAtEpochMillis=%d", variantId, now)
    }

    private suspend fun downloadFirstNewCandidate(
        word: WordItem,
        knownSignatures: Set<String>,
        candidates: List<RemoteImageCandidate>,
    ): ImageCard? {
        val fresh =
            RemoteImageCandidatePicker.firstUnknownCandidate(candidates, knownSignatures)
                ?: run {
                    Timber.d("ImageResolve: firstUnknownCandidate=null (empty list or all signatures known)")
                    return null
                }
        val sig = fresh.fetchSignature()
        Timber.d(
            "ImageResolve: picked unknown candidate source=%s sig=%s urlPrefix=%s",
            fresh.source.name,
            sig,
            fresh.imageUrl.take(64),
        )
        val file = cacheStore.fileForWord(word.id, fresh.imageUrl)
        val ok = cacheStore.downloadToFile(fresh.imageUrl, file)
        if (!ok) {
            Timber.w("ImageResolve: download failed — returning remote URL without DB variant urlPrefix=%s", fresh.imageUrl.take(64))
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
                fetchSignature = sig,
                wasShown = false,
                lastShownAtEpochMillis = 0L,
                createdAtEpochMillis = now,
            )
        val id = variantDao.insertOrExistingId(entity)
        Timber.i(
            "ImageResolve: saved word_image_variant id=%d wordId=%d sig=%s",
            id,
            word.id,
            sig,
        )
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
        val a = if (prefs.arasaacEnabled) arasaac.fetchImageCandidates(term, maxPer) else emptyList()
        val p = if (prefs.pixabayEnabled) pixabay.fetchImageCandidates(term, maxPer) else emptyList()
        val pe = if (prefs.pexelsEnabled) pexels.fetchImageCandidates(term, maxPer) else emptyList()
        Timber.d(
            "ImageResolve: provider raw counts arasaac=%d pixabay=%d pexels=%d (maxPer=%d)",
            a.size,
            p.size,
            pe.size,
            maxPer,
        )
        return buildList { addAll(a); addAll(p); addAll(pe) }
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
     * Не используется, когда уже сработала ветка rotation (там отдельный [ImageRotationRemotePolicy]).
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
