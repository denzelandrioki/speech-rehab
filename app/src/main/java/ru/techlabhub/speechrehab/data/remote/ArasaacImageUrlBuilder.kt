package ru.techlabhub.speechrehab.data.remote

/**
 * Сборка прямых URL статики ARASAAC (PNG пиктограммы на CDN).
 *
 * Формат пути задан сервисом [arasaac.org](https://arasaac.org); при смене API достаточно поправить этот объект.
 */
object ArasaacImageUrlBuilder {
    fun pictogramPngUrl(pictogramId: Long, size: Int = 500): String =
        "https://static.arasaac.org/pictograms/$pictogramId/${pictogramId}_$size.png"
}
