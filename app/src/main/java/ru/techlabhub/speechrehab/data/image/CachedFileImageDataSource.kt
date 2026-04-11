package ru.techlabhub.speechrehab.data.image

import ru.techlabhub.speechrehab.data.cache.LocalImageCacheStore
import ru.techlabhub.speechrehab.data.local.SpeechRehabDatabase
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Локальные файлы из [word_image_variants] + проверка читаемости на диске.
 */
interface CachedFileImageDataSource {
    suspend fun tryLoadCached(wordId: Long): CachedImageResult?
}

data class CachedImageResult(
    val variantId: Long,
    val localFilePath: String,
    val remoteUrl: String?,
    val sourceName: String,
)

@Singleton
class DefaultCachedFileImageDataSource @Inject constructor(
    private val db: SpeechRehabDatabase,
    private val cacheStore: LocalImageCacheStore,
) : CachedFileImageDataSource {

    private val dao get() = db.wordImageVariantDao()

    override suspend fun tryLoadCached(wordId: Long): CachedImageResult? {
        val rows = dao.listForWord(wordId)
        if (rows.isEmpty()) return null
        val picked =
            RemoteImageCandidatePicker.pickLocalFallbackVariant(rows) { path ->
                cacheStore.existsReadable(path)
            } ?: return null
        return CachedImageResult(
            variantId = picked.id,
            localFilePath = picked.localFilePath,
            remoteUrl = picked.remoteUrl,
            sourceName = picked.sourceName,
        )
    }
}
