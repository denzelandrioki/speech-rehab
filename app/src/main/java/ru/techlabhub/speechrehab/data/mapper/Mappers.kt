package ru.techlabhub.speechrehab.data.mapper

import ru.techlabhub.speechrehab.data.local.dao.WordWithCategoryRow
import ru.techlabhub.speechrehab.data.local.entity.CategoryEntity
import ru.techlabhub.speechrehab.domain.model.Category
import ru.techlabhub.speechrehab.domain.model.WordItem

fun CategoryEntity.toCategory(): Category = Category(id = id, name = name)

fun WordWithCategoryRow.toWordItem(): WordItem =
    WordItem(
        id = id,
        text = text,
        displayText = displayText,
        categoryId = categoryId,
        categoryName = categoryName,
        enabled = enabled,
        isCustom = isCustom,
        consecutiveCorrect = consecutiveCorrect,
        consecutiveIncorrect = consecutiveIncorrect,
    )
