package ru.techlabhub.speechrehab.data.image

import ru.techlabhub.speechrehab.BuildConfig
import ru.techlabhub.speechrehab.data.remote.api.PexelsApi
import ru.techlabhub.speechrehab.domain.model.ImageSource
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

interface PexelsImageDataSource {
    suspend fun fetchImageUrl(term: String): String?

    suspend fun fetchImageCandidates(
        term: String,
        limit: Int,
    ): List<RemoteImageCandidate>
}

@Singleton
class DefaultPexelsImageDataSource @Inject constructor(
    private val pexelsApi: PexelsApi,
) : PexelsImageDataSource {

    override suspend fun fetchImageUrl(term: String): String? =
        fetchImageCandidates(term, 1).firstOrNull()?.imageUrl

    override suspend fun fetchImageCandidates(
        term: String,
        limit: Int,
    ): List<RemoteImageCandidate> {
        val key = BuildConfig.PEXELS_API_KEY.trim()
        if (key.isEmpty() || limit <= 0) return emptyList()
        val perPage = limit.coerceIn(1, 20)
        return try {
            val resp = pexelsApi.search(authorization = key, query = term.trim(), perPage = perPage)
            resp.photos.mapNotNull { p ->
                val url = p.src?.large ?: p.src?.medium ?: return@mapNotNull null
                RemoteImageCandidate(
                    imageUrl = url,
                    source = ImageSource.PEXELS,
                    externalId = p.id?.toString(),
                )
            }
        } catch (e: Exception) {
            Timber.w(e, "Pexels failed")
            emptyList()
        }
    }
}
