package ru.techlabhub.speechrehab.domain.repository

import ru.techlabhub.speechrehab.domain.model.ImageCard
import ru.techlabhub.speechrehab.domain.model.ImageFetchPolicy
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
        fetchPolicy: ImageFetchPolicy = ImageFetchPolicy.NORMAL,
    ): ImageCard

    /** Отметить показ варианта из [word_image_variants] (см. [ImageCard.wordImageVariantId]). */
    suspend fun markImageVariantShown(variantId: Long?)
}
