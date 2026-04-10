package ru.techlabhub.speechrehab.domain.repository

import ru.techlabhub.speechrehab.domain.model.ImageCard
import ru.techlabhub.speechrehab.domain.model.WordItem

/**
 * Картинка для слова: offline-first (bundled → файловый кэш → сеть по настройкам).
 * Всегда возвращает [ImageCard]; при отсутствии изображения — [ru.techlabhub.speechrehab.domain.model.ImageSource.NONE].
 */
interface ImageRepository {
    suspend fun resolveCard(
        word: WordItem,
        prefs: UserTrainingPreferences,
    ): ImageCard
}
