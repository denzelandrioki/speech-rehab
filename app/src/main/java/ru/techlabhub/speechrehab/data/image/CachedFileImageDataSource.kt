package ru.techlabhub.speechrehab.data.image

import ru.techlabhub.speechrehab.data.cache.LocalImageCacheStore
import ru.techlabhub.speechrehab.data.local.SpeechRehabDatabase
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Локальный файловый кэш + запись в Room `cached_images`.
 */
interface CachedFileImageDataSource {
    suspend fun tryLoadCached(wordId: Long): CachedImageResult?
}

data class CachedImageResult(
    val localFilePath: String,
    val remoteUrl: String?,
    val sourceName: String,
)

@Singleton
class DefaultCachedFileImageDataSource @Inject constructor(
    private val db: SpeechRehabDatabase,
    private val cacheStore: LocalImageCacheStore,
) : CachedFileImageDataSource {

    private val dao get() = db.cachedImageDao()

    override suspend fun tryLoadCached(wordId: Long): CachedImageResult? {
        val row = dao.getForWord(wordId) ?: return null
        if (!cacheStore.existsReadable(row.localFilePath)) return null
        return CachedImageResult(
            localFilePath = row.localFilePath,
            remoteUrl = row.remoteUrl,
            sourceName = row.sourceName,
        )
    }
}
