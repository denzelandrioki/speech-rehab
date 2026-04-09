package ru.techlabhub.speechrehab.domain.repository

import ru.techlabhub.speechrehab.domain.model.ImageCard
import ru.techlabhub.speechrehab.domain.model.WordItem

/** Получение картинки для слова: кэш, затем внешние API согласно [UserTrainingPreferences]. */
interface ImageRepository {
    suspend fun resolveCard(
        word: WordItem,
        prefs: UserTrainingPreferences,
    ): ImageCard?
}
