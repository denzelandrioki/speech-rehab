package ru.techlabhub.speechrehab.data.remote

object ArasaacImageUrlBuilder {
    fun pictogramPngUrl(pictogramId: Long, size: Int = 500): String =
        "https://static.arasaac.org/pictograms/$pictogramId/${pictogramId}_$size.png"
}
