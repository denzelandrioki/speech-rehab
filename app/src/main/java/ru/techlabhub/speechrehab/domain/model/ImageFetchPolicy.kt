package ru.techlabhub.speechrehab.domain.model

/**
 * Политика одного вызова [ru.techlabhub.speechrehab.domain.repository.ImageRepository.resolveCard].
 * [NORMAL] — тренировка / multiple choice: учитывается [ru.techlabhub.speechrehab.domain.model.ImageRotationMode]
 * (в т.ч. попытка нового remote до bundled).
 * [PREFER_EXISTING_LOCAL] — prefetch: не форсировать «новый remote» на каждое слово (значение не [NORMAL]).
 */
enum class ImageFetchPolicy {
    NORMAL,
    PREFER_EXISTING_LOCAL,
}
