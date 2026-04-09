package ru.techlabhub.speechrehab.domain.model

data class CategoryAggregate(
    val categoryId: Long,
    val name: String,
    val attempts: Int,
    val accuracyPercent: Float,
)
