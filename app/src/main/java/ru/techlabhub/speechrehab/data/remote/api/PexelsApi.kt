package ru.techlabhub.speechrehab.data.remote.api

import ru.techlabhub.speechrehab.data.remote.dto.PexelsSearchResponse
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

/**
 * Документация: https://www.pexels.com/api/documentation/
 * Ключ: local.properties pexels.api.key=...
 */
interface PexelsApi {
    @GET("v1/search")
    suspend fun search(
        @Header("Authorization") authorization: String,
        @Query("query") query: String,
        @Query("per_page") perPage: Int = 5,
    ): PexelsSearchResponse
}
