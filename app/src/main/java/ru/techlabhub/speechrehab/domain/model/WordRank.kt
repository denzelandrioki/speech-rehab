package ru.techlabhub.speechrehab.domain.model

data class WordRank(
    val wordId: Long,
    val canonicalText: String,
    val displayTextRu: String,
    val displayTextEn: String,
    val attempts: Int,
    val accuracyPercent: Float,
)
