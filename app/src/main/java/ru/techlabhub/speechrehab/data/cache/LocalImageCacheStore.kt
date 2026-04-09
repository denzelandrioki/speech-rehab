package ru.techlabhub.speechrehab.data.cache

import android.content.Context
import ru.techlabhub.speechrehab.di.ImageDownloadClient
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Сохраняет байты изображения в filesDir, чтобы не hotlink'ать и работать офлайн.
 */
@Singleton
class LocalImageCacheStore @Inject constructor(
    @ApplicationContext private val context: Context,
    @ImageDownloadClient private val okHttpClient: OkHttpClient,
) {
    private val root: File by lazy {
        File(context.filesDir, "image_cache").apply { mkdirs() }
    }

    fun fileForWord(wordId: Long, remoteUrl: String): File {
        val hash = sha256(remoteUrl).take(16)
        return File(root, "w_${wordId}_$hash.img")
    }

    fun existsReadable(path: String): Boolean {
        val f = File(path)
        return f.isFile && f.length() > 0L
    }

    suspend fun downloadToFile(remoteUrl: String, target: File): Boolean =
        withContext(Dispatchers.IO) {
            repeat(MAX_DOWNLOAD_ATTEMPTS) { attempt ->
                try {
                    if (target.exists()) {
                        target.delete()
                    }
                    val req =
                        Request.Builder()
                            .url(remoteUrl)
                            .header("Accept", "image/*")
                            .build()
                    okHttpClient.newCall(req).execute().use { resp ->
                        if (!resp.isSuccessful) {
                            throw IOException("HTTP ${resp.code} for $remoteUrl")
                        }
                        val body = resp.body ?: throw IOException("empty body: $remoteUrl")
                        target.outputStream().use { out -> body.byteStream().copyTo(out) }
                    }
                    if (target.isFile && target.length() > 0L) {
                        return@withContext true
                    }
                } catch (e: IOException) {
                    Timber.w(e, "downloadToFile attempt ${attempt + 1}/$MAX_DOWNLOAD_ATTEMPTS: $remoteUrl")
                }
                if (attempt < MAX_DOWNLOAD_ATTEMPTS - 1) {
                    delay(BASE_RETRY_DELAY_MS * (attempt + 1))
                }
            }
            false
        }

    private companion object {
        const val MAX_DOWNLOAD_ATTEMPTS = 4
        const val BASE_RETRY_DELAY_MS = 400L
    }

    private fun sha256(input: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { b -> "%02x".format(b) }
    }
}
