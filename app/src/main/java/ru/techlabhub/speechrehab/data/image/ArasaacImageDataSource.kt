package ru.techlabhub.speechrehab.data.image

import ru.techlabhub.speechrehab.data.remote.ArasaacImageUrlBuilder
import ru.techlabhub.speechrehab.data.remote.api.ArasaacApi
import ru.techlabhub.speechrehab.domain.model.ImageSource
import retrofit2.HttpException
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** Удалённый поиск пиктограммы ARASAAC по английскому термину. */
interface ArasaacImageDataSource {
    suspend fun fetchImageUrl(term: String): String?

    suspend fun fetchImageCandidates(
        term: String,
        limit: Int,
    ): List<RemoteImageCandidate>
}

@Singleton
class DefaultArasaacImageDataSource @Inject constructor(
    private val arasaacApi: ArasaacApi,
) : ArasaacImageDataSource {

    override suspend fun fetchImageUrl(term: String): String? =
        fetchImageCandidates(term, 1).firstOrNull()?.imageUrl

    override suspend fun fetchImageCandidates(
        term: String,
        limit: Int,
    ): List<RemoteImageCandidate> {
        val t = term.trim()
        if (t.isEmpty() || limit <= 0) return emptyList()
        val capped = limit.coerceAtMost(40)
        return try {
            val list = arasaacApi.searchEnglishV1(t)
            val fromV1 =
                list.take(capped).mapNotNull { dto ->
                    val id = dto.id ?: dto.idAlt ?: return@mapNotNull null
                    RemoteImageCandidate(
                        imageUrl = ArasaacImageUrlBuilder.pictogramPngUrl(id),
                        source = ImageSource.ARASAAC,
                        externalId = id.toString(),
                    )
                }
            if (fromV1.isNotEmpty()) fromV1 else legacyCandidates(t, capped)
        } catch (e: HttpException) {
            Timber.w(e, "ARASAAC v1 HTTP, trying legacy candidates")
            legacyCandidates(t, capped)
        } catch (e: IOException) {
            Timber.w(e, "ARASAAC v1 IO")
            emptyList()
        }
    }

    private suspend fun legacyCandidates(
        term: String,
        limit: Int,
    ): List<RemoteImageCandidate> =
        try {
            val list = arasaacApi.searchEnglishLegacy(term)
            list.take(limit).mapNotNull { dto ->
                val id = dto.id ?: dto.idAlt ?: return@mapNotNull null
                RemoteImageCandidate(
                    imageUrl = ArasaacImageUrlBuilder.pictogramPngUrl(id),
                    source = ImageSource.ARASAAC,
                    externalId = id.toString(),
                )
            }
        } catch (e: Exception) {
            Timber.w(e, "ARASAAC legacy failed")
            emptyList()
        }
}
