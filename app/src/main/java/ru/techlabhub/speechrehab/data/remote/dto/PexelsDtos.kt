package ru.techlabhub.speechrehab.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class PexelsSearchResponse(
    val photos: List<PexelsPhotoDto> = emptyList(),
)

@Serializable
data class PexelsPhotoDto(
    val id: Long? = null,
    val src: PexelsSrcDto? = null,
)

@Serializable
data class PexelsSrcDto(
    val large: String? = null,
    val medium: String? = null,
)
