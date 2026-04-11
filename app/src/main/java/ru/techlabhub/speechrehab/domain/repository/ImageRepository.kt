package ru.techlabhub.speechrehab.domain.repository

import ru.techlabhub.speechrehab.domain.model.ImageCard
import ru.techlabhub.speechrehab.domain.model.WordItem

/**
 * Картинка для слова: offline-first (bundled → файловый кэш → сеть по настройкам).
 * Всегда возвращает [ImageCard]; при отсутствии изображения — [ru.techlabhub.speechrehab.domain.model.ImageSource.NONE].
 * Не зависит от режима подбора слов в тренировке ([ru.techlabhub.speechrehab.domain.model.TrainingMode]):
 * «новое слово» по попыткам и «нет локальной картинки» разделены.
 */
interface ImageRepository {
    suspend fun resolveCard(
        word: WordItem,
        prefs: UserTrainingPreferences,
    ): ImageCard
}
