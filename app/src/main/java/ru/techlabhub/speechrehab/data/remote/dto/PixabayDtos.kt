package ru.techlabhub.speechrehab.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PixabaySearchResponse(
    val hits: List<PixabayHitDto> = emptyList(),
)

@Serializable
data class PixabayHitDto(
    val id: Long? = null,
    @SerialName("largeImageURL") val largeImageUrl: String? = null,
    @SerialName("webformatURL") val webformatUrl: String? = null,
)
