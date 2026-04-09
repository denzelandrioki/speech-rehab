package ru.techlabhub.speechrehab.data.repository

import ru.techlabhub.speechrehab.BuildConfig
import ru.techlabhub.speechrehab.data.cache.LocalImageCacheStore
import ru.techlabhub.speechrehab.data.local.SpeechRehabDatabase
import ru.techlabhub.speechrehab.data.local.entity.CachedImageEntity
import ru.techlabhub.speechrehab.data.remote.ArasaacImageUrlBuilder
import ru.techlabhub.speechrehab.data.remote.api.ArasaacApi
import ru.techlabhub.speechrehab.data.remote.api.PexelsApi
import ru.techlabhub.speechrehab.data.remote.api.PixabayApi
import ru.techlabhub.speechrehab.domain.model.ImageCard
import ru.techlabhub.speechrehab.domain.model.ImageSource
import ru.techlabhub.speechrehab.domain.model.WordItem
import ru.techlabhub.speechrehab.domain.repository.ImageRepository
import ru.techlabhub.speechrehab.domain.repository.UserTrainingPreferences
import retrofit2.HttpException
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultImageRepository @Inject constructor(
    private val db: SpeechRehabDatabase,
    private val arasaacApi: ArasaacApi,
    private val pixabayApi: PixabayApi,
    private val pexelsApi: PexelsApi,
    private val cacheStore: LocalImageCacheStore,
) : ImageRepository {

    private val cachedImageDao get() = db.cachedImageDao()

    override suspend fun resolveCard(
        word: WordItem,
        prefs: UserTrainingPreferences,
    ): ImageCard? {
        val cached = cachedImageDao.getForWord(word.id)
        if (cached != null && cacheStore.existsReadable(cached.localFilePath)) {
            return ImageCard(
                word = word,
                imageUri = cached.localFilePath,
                remoteUrl = cached.remoteUrl,
                source = ImageSource.LOCAL_CACHE,
                fromOfflineCache = true,
            )
        }

        val term = word.text.trim()
        if (term.isEmpty()) return null

        var chosenUrl: String? = null
        var chosenSource = ImageSource.ARASAAC

        if (prefs.arasaacEnabled) {
            val ar = resolveArasaac(term)
            if (ar != null) {
                chosenUrl = ar.first
                chosenSource = ar.second
            }
        }

        if (chosenUrl.isNullOrBlank() && prefs.pixabayEnabled && BuildConfig.PIXABAY_API_KEY.isNotBlank()) {
            val u = resolvePixabay(term)
            if (u != null) {
                chosenUrl = u
                chosenSource = ImageSource.PIXABAY
            }
        }

        if (chosenUrl.isNullOrBlank() && prefs.pexelsEnabled && BuildConfig.PEXELS_API_KEY.isNotBlank()) {
            val u = resolvePexels(term)
            if (u != null) {
                chosenUrl = u
                chosenSource = ImageSource.PEXELS
            }
        }

        if (chosenUrl.isNullOrBlank()) {
            Timber.w("No remote image for word=${word.text}")
            return null
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

    private suspend fun resolveArasaac(term: String): Pair<String, ImageSource>? {
        return try {
            val list = arasaacApi.searchEnglishV1(term)
            val id = list.firstOrNull()?.let { it.id ?: it.idAlt }
            if (id != null) {
                ArasaacImageUrlBuilder.pictogramPngUrl(id) to ImageSource.ARASAAC
            } else {
                tryLegacyArasaac(term)
            }
        } catch (e: HttpException) {
            Timber.w(e, "ARASAAC v1 HTTP, trying legacy")
            tryLegacyArasaac(term)
        } catch (e: IOException) {
            Timber.w(e, "ARASAAC v1 IO")
            null
        }
    }

    private suspend fun tryLegacyArasaac(term: String): Pair<String, ImageSource>? {
        return try {
            val list = arasaacApi.searchEnglishLegacy(term)
            val id = list.firstOrNull()?.let { it.id ?: it.idAlt } ?: return null
            ArasaacImageUrlBuilder.pictogramPngUrl(id) to ImageSource.ARASAAC
        } catch (e: Exception) {
            Timber.w(e, "ARASAAC legacy failed")
            null
        }
    }

    private suspend fun resolvePixabay(term: String): String? {
        return try {
            val resp = pixabayApi.search(apiKey = BuildConfig.PIXABAY_API_KEY, query = term)
            val hit = resp.hits.firstOrNull()
            hit?.largeImageUrl ?: hit?.webformatUrl
        } catch (e: Exception) {
            Timber.w(e, "Pixabay failed")
            null
        }
    }

    private suspend fun resolvePexels(term: String): String? {
        return try {
            val key = BuildConfig.PEXELS_API_KEY.trim()
            val resp = pexelsApi.search(authorization = key, query = term)
            val p = resp.photos.firstOrNull()
            p?.src?.large ?: p?.src?.medium
        } catch (e: Exception) {
            Timber.w(e, "Pexels failed")
            null
        }
    }
}
