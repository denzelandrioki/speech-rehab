package ru.techlabhub.speechrehab.domain.model

data class WordRank(
    val wordId: Long,
    val text: String,
    val attempts: Int,
    val accuracyPercent: Float,
)
