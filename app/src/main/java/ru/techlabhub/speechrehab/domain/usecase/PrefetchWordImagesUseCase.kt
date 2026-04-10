package ru.techlabhub.speechrehab.domain.usecase

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Зарезервировано под предзагрузку изображений для набора слов (seed 100–300 слов, фоновый prefetch).
 * Реализация: вызвать [ru.techlabhub.speechrehab.domain.repository.ImageRepository.resolveCard] для каждого слова.
 */
@Singleton
class PrefetchWordImagesUseCase @Inject constructor() {
    suspend operator fun invoke() {
        // TODO: обход словаря + политика сети/батчинг
    }
}
