package ru.techlabhub.speechrehab.data.remote

import okhttp3.Interceptor
import okhttp3.Response
import timber.log.Timber
import java.io.IOException

/**
 * Простой retry для нестабильной сети (без бесконечного цикла).
 */
class RetryInterceptor(
    private val maxRetries: Int = 2,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var response: Response? = null
        var lastIo: IOException? = null
        repeat(maxRetries + 1) { attempt ->
            try {
                response?.close()
                response = chain.proceed(request)
                if (response.isSuccessful) return response
                if (response.code in 400..499) return response
                Timber.w("RetryInterceptor: HTTP ${response.code}, attempt=$attempt")
            } catch (e: IOException) {
                lastIo = e
                Timber.w(e, "RetryInterceptor: IO error, attempt=$attempt")
            }
        }
        lastIo?.let { throw it }
        return checkNotNull(response)
    }
}
