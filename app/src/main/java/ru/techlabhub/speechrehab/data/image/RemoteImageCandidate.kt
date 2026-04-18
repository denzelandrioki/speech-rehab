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
    /**
     * Уникальность в пределах слова в БД: провайдер + стабильный внешний id, иначе нормализованный URL.
     * Разные картинки с разными id/url не должны схлопываться; один и тот же объект из API — тот же ключ.
     */
    fun fetchSignature(): String {
        val key = externalId?.takeIf { it.isNotBlank() } ?: imageUrl.trim()
        return "${source.name}:$key"
    }
}
