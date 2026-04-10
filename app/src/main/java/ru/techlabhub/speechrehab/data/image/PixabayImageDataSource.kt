package ru.techlabhub.speechrehab.data.image

import ru.techlabhub.speechrehab.BuildConfig
import ru.techlabhub.speechrehab.data.remote.api.PixabayApi
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

interface PixabayImageDataSource {
    suspend fun fetchImageUrl(term: String): String?
}

@Singleton
class DefaultPixabayImageDataSource @Inject constructor(
    private val pixabayApi: PixabayApi,
) : PixabayImageDataSource {

    override suspend fun fetchImageUrl(term: String): String? {
        if (BuildConfig.PIXABAY_API_KEY.isBlank()) return null
        return try {
            val resp = pixabayApi.search(apiKey = BuildConfig.PIXABAY_API_KEY, query = term.trim())
            val hit = resp.hits.firstOrNull()
            hit?.largeImageUrl ?: hit?.webformatUrl
        } catch (e: Exception) {
            Timber.w(e, "Pixabay failed")
            null
        }
    }
}
