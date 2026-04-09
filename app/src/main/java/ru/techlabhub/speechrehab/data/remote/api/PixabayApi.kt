package ru.techlabhub.speechrehab.data.remote.api

import ru.techlabhub.speechrehab.data.remote.dto.PixabaySearchResponse
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Документация: https://pixabay.com/api/docs/
 * Ключ задаётся в local.properties: pixabay.api.key=...
 */
interface PixabayApi {
    @GET("api/")
    suspend fun search(
        @Query("key") apiKey: String,
        @Query("q") query: String,
        @Query("image_type") imageType: String = "photo",
        @Query("per_page") perPage: Int = 5,
        @Query("safesearch") safeSearch: Boolean = true,
    ): PixabaySearchResponse
}
