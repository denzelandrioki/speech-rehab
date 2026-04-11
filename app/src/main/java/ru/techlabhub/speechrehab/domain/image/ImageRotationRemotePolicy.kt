package ru.techlabhub.speechrehab.domain.image

import ru.techlabhub.speechrehab.domain.model.OnlineImageFetchingMode
import ru.techlabhub.speechrehab.domain.model.PreferredImageMode

/**
 * Можно ли для режима «новая картинка» ходить в сеть (без привязки к [UserTrainingPreferences.refreshRemoteWhenNoLocalImage]).
 */
object ImageRotationRemotePolicy {
    fun rotationFetchAllowed(
        preferredImageMode: PreferredImageMode,
        onlineMode: OnlineImageFetchingMode,
        arasaacEnabled: Boolean,
        pixabayEnabled: Boolean,
        pexelsEnabled: Boolean,
        isOnline: Boolean,
        isWifi: Boolean,
    ): Boolean {
        if (preferredImageMode == PreferredImageMode.LOCAL_ONLY) {
            return false
        }
        if (!arasaacEnabled && !pixabayEnabled && !pexelsEnabled) {
            return false
        }
        return when (onlineMode) {
            OnlineImageFetchingMode.DISABLED -> false
            OnlineImageFetchingMode.ENABLED -> isOnline
            OnlineImageFetchingMode.WIFI_ONLY -> isOnline && isWifi
        }
    }
}
