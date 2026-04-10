package ru.techlabhub.speechrehab.data.image

import ru.techlabhub.speechrehab.BuildConfig
import ru.techlabhub.speechrehab.data.remote.api.PexelsApi
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

interface PexelsImageDataSource {
    suspend fun fetchImageUrl(term: String): String?
}

@Singleton
class DefaultPexelsImageDataSource @Inject constructor(
    private val pexelsApi: PexelsApi,
) : PexelsImageDataSource {

    override suspend fun fetchImageUrl(term: String): String? {
        val key = BuildConfig.PEXELS_API_KEY.trim()
        if (key.isEmpty()) return null
        return try {
            val resp = pexelsApi.search(authorization = key, query = term.trim())
            val p = resp.photos.firstOrNull()
            p?.src?.large ?: p?.src?.medium
        } catch (e: Exception) {
            Timber.w(e, "Pexels failed")
            null
        }
    }
}
