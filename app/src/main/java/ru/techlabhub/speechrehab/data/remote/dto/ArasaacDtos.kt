package ru.techlabhub.speechrehab.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ArasaacPictogramDto(
    @SerialName("_id") val id: Long? = null,
    @SerialName("id") val idAlt: Long? = null,
)
