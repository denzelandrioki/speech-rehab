package ru.techlabhub.speechrehab.data.remote.api

import ru.techlabhub.speechrehab.data.remote.dto.ArasaacPictogramDto
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * ARASAAC REST (путь может отличаться между версиями API — при необходимости поправьте endpoint).
 * База: https://api.arasaac.org/
 */
interface ArasaacApi {
    @GET("v1/pictograms/en/search/{term}")
    suspend fun searchEnglishV1(@Path("term") term: String): List<ArasaacPictogramDto>

    /** Запасной путь на случай отличий версии API. */
    @GET("api/pictograms/en/search/{term}")
    suspend fun searchEnglishLegacy(@Path("term") term: String): List<ArasaacPictogramDto>
}
