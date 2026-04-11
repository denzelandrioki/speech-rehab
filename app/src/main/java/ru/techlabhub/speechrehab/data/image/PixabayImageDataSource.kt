package ru.techlabhub.speechrehab.data.image

import ru.techlabhub.speechrehab.BuildConfig
import ru.techlabhub.speechrehab.data.remote.api.PixabayApi
import ru.techlabhub.speechrehab.domain.model.ImageSource
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

interface PixabayImageDataSource {
    suspend fun fetchImageUrl(term: String): String?

    suspend fun fetchImageCandidates(
        term: String,
        limit: Int,
    ): List<RemoteImageCandidate>
}

@Singleton
class DefaultPixabayImageDataSource @Inject constructor(
    private val pixabayApi: PixabayApi,
) : PixabayImageDataSource {

    override suspend fun fetchImageUrl(term: String): String? =
        fetchImageCandidates(term, 1).firstOrNull()?.imageUrl

    override suspend fun fetchImageCandidates(
        term: String,
        limit: Int,
    ): List<RemoteImageCandidate> {
        if (BuildConfig.PIXABAY_API_KEY.isBlank() || limit <= 0) return emptyList()
        val perPage = limit.coerceIn(1, 20)
        return try {
            val resp =
                pixabayApi.search(
                    apiKey = BuildConfig.PIXABAY_API_KEY,
                    query = term.trim(),
                    perPage = perPage,
                )
            resp.hits.mapNotNull { hit ->
                val url = hit.largeImageUrl ?: hit.webformatUrl ?: return@mapNotNull null
                RemoteImageCandidate(
                    imageUrl = url,
                    source = ImageSource.PIXABAY,
                    externalId = hit.id?.toString(),
                )
            }
        } catch (e: Exception) {
            Timber.w(e, "Pixabay failed")
            emptyList()
        }
    }
}
