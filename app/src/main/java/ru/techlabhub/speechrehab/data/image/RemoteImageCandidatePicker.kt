package ru.techlabhub.speechrehab.data.image

import ru.techlabhub.speechrehab.data.local.entity.WordImageVariantEntity

/**
 * Чистая логика выбора кандидата / fallback без сети и Room.
 */
object RemoteImageCandidatePicker {
    fun firstUnknownCandidate(
        orderedCandidates: List<RemoteImageCandidate>,
        knownSignatures: Set<String>,
    ): RemoteImageCandidate? =
        orderedCandidates.firstOrNull { it.fetchSignature() !in knownSignatures }

    /**
     * Локальный fallback: читаемые файлы; сначала ещё не показывавшиеся, затем с наименьшим [WordImageVariantEntity.lastShownAtEpochMillis].
     */
    fun pickLocalFallbackVariant(
        variants: List<WordImageVariantEntity>,
        fileExists: (String) -> Boolean,
    ): WordImageVariantEntity? {
        val readable = variants.filter { fileExists(it.localFilePath) }
        if (readable.isEmpty()) return null
        return readable.minWithOrNull(
            compareBy({ it.wasShown }, { it.lastShownAtEpochMillis }),
        )
    }
}
