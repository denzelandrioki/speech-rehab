package ru.techlabhub.speechrehab.data.image

import ru.techlabhub.speechrehab.domain.model.ImageSource

/**
 * Один кандидат URL из ответа API до скачивания и записи в [word_image_variants].
 */
data class RemoteImageCandidate(
    val imageUrl: String,
    val source: ImageSource,
    val externalId: String?,
) {
    fun fetchSignature(): String {
        val key = externalId?.takeIf { it.isNotBlank() } ?: imageUrl.trim()
        return "${source.name}:$key"
    }
}
