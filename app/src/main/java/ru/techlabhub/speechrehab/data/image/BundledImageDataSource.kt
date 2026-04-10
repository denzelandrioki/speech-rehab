package ru.techlabhub.speechrehab.data.image

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import ru.techlabhub.speechrehab.domain.model.WordItem
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Встроенные изображения в `assets/` (offline-first).
 * Имя файла: [WordItem.bundledAssetName] или по умолчанию `bundled/<canonical>.png`.
 */
interface BundledImageDataSource {
    fun tryAssetUri(word: WordItem): String?
}

@Singleton
class DefaultBundledImageDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
) : BundledImageDataSource {

    override fun tryAssetUri(word: WordItem): String? {
        val candidates =
            buildList {
                val custom = word.bundledAssetName.trim()
                if (custom.isNotEmpty()) add(custom)
                add("bundled/${word.text.trim()}.png")
                add("bundled/${word.text.trim()}.webp")
            }
        for (path in candidates) {
            if (assetExists(path)) {
                return "file:///android_asset/$path"
            }
        }
        return null
    }

    private fun assetExists(relativePath: String): Boolean =
        try {
            context.assets.open(relativePath).use { true }
        } catch (_: Exception) {
            false
        }
}
