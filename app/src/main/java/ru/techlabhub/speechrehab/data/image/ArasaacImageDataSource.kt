package ru.techlabhub.speechrehab.data.image

import ru.techlabhub.speechrehab.data.remote.ArasaacImageUrlBuilder
import ru.techlabhub.speechrehab.data.remote.api.ArasaacApi
import retrofit2.HttpException
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** Удалённый поиск пиктограммы ARASAAC по английскому термину. */
interface ArasaacImageDataSource {
    suspend fun fetchImageUrl(term: String): String?
}

@Singleton
class DefaultArasaacImageDataSource @Inject constructor(
    private val arasaacApi: ArasaacApi,
) : ArasaacImageDataSource {

    override suspend fun fetchImageUrl(term: String): String? {
        val t = term.trim()
        if (t.isEmpty()) return null
        return try {
            val list = arasaacApi.searchEnglishV1(t)
            val id = list.firstOrNull()?.let { it.id ?: it.idAlt }
            if (id != null) {
                ArasaacImageUrlBuilder.pictogramPngUrl(id)
            } else {
                tryLegacy(t)
            }
        } catch (e: HttpException) {
            Timber.w(e, "ARASAAC v1 HTTP, trying legacy")
            tryLegacy(t)
        } catch (e: IOException) {
            Timber.w(e, "ARASAAC v1 IO")
            null
        }
    }

    private suspend fun tryLegacy(term: String): String? =
        try {
            val list = arasaacApi.searchEnglishLegacy(term)
            val id = list.firstOrNull()?.let { it.id ?: it.idAlt } ?: return null
            ArasaacImageUrlBuilder.pictogramPngUrl(id)
        } catch (e: Exception) {
            Timber.w(e, "ARASAAC legacy failed")
            null
        }
}
