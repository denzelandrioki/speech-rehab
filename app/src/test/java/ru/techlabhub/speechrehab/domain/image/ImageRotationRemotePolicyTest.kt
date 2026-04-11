package ru.techlabhub.speechrehab.domain.image

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.techlabhub.speechrehab.domain.model.OnlineImageFetchingMode
import ru.techlabhub.speechrehab.domain.model.PreferredImageMode

class ImageRotationRemotePolicyTest {

    @Test
    fun localOnly_neverAllowsRotationRemote() {
        assertFalse(
            ImageRotationRemotePolicy.rotationFetchAllowed(
                preferredImageMode = PreferredImageMode.LOCAL_ONLY,
                onlineMode = OnlineImageFetchingMode.ENABLED,
                arasaacEnabled = true,
                pixabayEnabled = true,
                pexelsEnabled = true,
                isOnline = true,
                isWifi = true,
            ),
        )
    }

    @Test
    fun onlineEnabled_allSources_whenOnline_allows() {
        assertTrue(
            ImageRotationRemotePolicy.rotationFetchAllowed(
                preferredImageMode = PreferredImageMode.LOCAL_THEN_REMOTE,
                onlineMode = OnlineImageFetchingMode.ENABLED,
                arasaacEnabled = true,
                pixabayEnabled = false,
                pexelsEnabled = false,
                isOnline = true,
                isWifi = false,
            ),
        )
    }

    @Test
    fun allSourcesDisabled_notAllowed() {
        assertFalse(
            ImageRotationRemotePolicy.rotationFetchAllowed(
                preferredImageMode = PreferredImageMode.LOCAL_THEN_REMOTE,
                onlineMode = OnlineImageFetchingMode.ENABLED,
                arasaacEnabled = false,
                pixabayEnabled = false,
                pexelsEnabled = false,
                isOnline = true,
                isWifi = true,
            ),
        )
    }

    @Test
    fun wifiOnly_offline_notAllowed() {
        assertFalse(
            ImageRotationRemotePolicy.rotationFetchAllowed(
                preferredImageMode = PreferredImageMode.LOCAL_THEN_REMOTE,
                onlineMode = OnlineImageFetchingMode.WIFI_ONLY,
                arasaacEnabled = true,
                pixabayEnabled = false,
                pexelsEnabled = false,
                isOnline = false,
                isWifi = false,
            ),
        )
    }
}
