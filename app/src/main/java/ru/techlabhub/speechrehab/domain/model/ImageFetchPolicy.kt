package ru.techlabhub.speechrehab.domain.model

/**
 * Политика одного вызова [ru.techlabhub.speechrehab.domain.repository.ImageRepository.resolveCard].
 * [PREFER_EXISTING_LOCAL] — для prefetch: не форсировать «новый remote» на каждое слово.
 */
enum class ImageFetchPolicy {
    NORMAL,
    PREFER_EXISTING_LOCAL,
}
