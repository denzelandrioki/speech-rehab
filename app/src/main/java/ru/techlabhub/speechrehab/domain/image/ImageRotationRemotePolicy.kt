package ru.techlabhub.speechrehab.domain.image

import ru.techlabhub.speechrehab.domain.model.OnlineImageFetchingMode
import ru.techlabhub.speechrehab.domain.model.PreferredImageMode

/**
 * Можно ли для режима «новая картинка» ходить в сеть.
 *
 * **Приоритеты (не конфликтуют с `refreshRemoteWhenNoLocalImage`):**
 * - [PreferredImageMode.LOCAL_ONLY] или `onlineMode == DISABLED` → remote для ротации **запрещён** всегда.
 * - Ротация (`ImageRotationMode` не `REUSE_LOCAL_FIRST`) в `OfflineFirstImageResolver` использует **только**
 *   этот объект + сеть/Wi‑Fi — флаг `refreshRemoteWhenNoLocalImage` на неё **не влияет** (он для «классического»
 *   пути после полного локального промаха).
 * - `PrefetchMissingImagesUseCase` вызывает `resolveCard` с `ImageFetchPolicy.PREFER_EXISTING_LOCAL` — ветка
 *   «форсировать новый remote» там **не активируется** намеренно.
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
