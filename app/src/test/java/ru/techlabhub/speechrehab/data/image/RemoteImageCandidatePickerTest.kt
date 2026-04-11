package ru.techlabhub.speechrehab.data.image

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import ru.techlabhub.speechrehab.data.local.entity.WordImageVariantEntity
import ru.techlabhub.speechrehab.domain.model.ImageSource

class RemoteImageCandidatePickerTest {

    private fun cand(
        url: String,
        source: ImageSource = ImageSource.PIXABAY,
        ext: String? = "1",
    ) = RemoteImageCandidate(imageUrl = url, source = source, externalId = ext)

    @Test
    fun firstUnknown_emptyKnown_returnsFirst() {
        val list = listOf(cand("https://a"), cand("https://b"))
        val picked = RemoteImageCandidatePicker.firstUnknownCandidate(list, emptySet())
        assertEquals("https://a", picked?.imageUrl)
    }

    @Test
    fun firstUnknown_allKnown_returnsNull() {
        val c = cand("https://a")
        val known = setOf(c.fetchSignature())
        assertNull(RemoteImageCandidatePicker.firstUnknownCandidate(listOf(c), known))
    }

    @Test
    fun firstUnknown_skipsKnownPicksNext() {
        val a = cand("https://a", ext = "1")
        val b = cand("https://b", ext = "2")
        val known = setOf(a.fetchSignature())
        val picked = RemoteImageCandidatePicker.firstUnknownCandidate(listOf(a, b), known)
        assertEquals("https://b", picked?.imageUrl)
    }

    @Test
    fun firstUnknown_sameUrlDifferentSource_distinctSignatures() {
        val url = "https://same"
        val p = cand(url, ImageSource.PIXABAY, "10")
        val x = cand(url, ImageSource.PEXELS, "20")
        val known = setOf(p.fetchSignature())
        val picked = RemoteImageCandidatePicker.firstUnknownCandidate(listOf(p, x), known)
        assertEquals(ImageSource.PEXELS, picked?.source)
    }

    @Test
    fun pickLocalFallback_prefersUnshown() {
        val shown =
            WordImageVariantEntity(
                id = 1,
                wordId = 1,
                remoteUrl = "u1",
                localFilePath = "/a",
                sourceName = "PIXABAY",
                fetchSignature = "s1",
                wasShown = true,
                lastShownAtEpochMillis = 100L,
                createdAtEpochMillis = 1L,
            )
        val unshown =
            shown.copy(
                id = 2,
                fetchSignature = "s2",
                localFilePath = "/b",
                wasShown = false,
                lastShownAtEpochMillis = 0L,
            )
        val picked =
            RemoteImageCandidatePicker.pickLocalFallbackVariant(listOf(shown, unshown)) { true }
        assertEquals(2L, picked?.id)
    }

    @Test
    fun pickLocalFallback_bothShown_oldestLastShown() {
        val older =
            WordImageVariantEntity(
                id = 1,
                wordId = 1,
                remoteUrl = "u1",
                localFilePath = "/a",
                sourceName = "PIXABAY",
                fetchSignature = "s1",
                wasShown = true,
                lastShownAtEpochMillis = 10L,
                createdAtEpochMillis = 1L,
            )
        val newer =
            older.copy(
                id = 2,
                fetchSignature = "s2",
                localFilePath = "/b",
                lastShownAtEpochMillis = 99L,
            )
        val picked =
            RemoteImageCandidatePicker.pickLocalFallbackVariant(listOf(newer, older)) { true }
        assertEquals(1L, picked?.id)
    }

    @Test
    fun pickLocalFallback_noReadable_returnsNull() {
        val e =
            WordImageVariantEntity(
                id = 1,
                wordId = 1,
                remoteUrl = "u",
                localFilePath = "/missing",
                sourceName = "PIXABAY",
                fetchSignature = "s",
                wasShown = false,
                lastShownAtEpochMillis = 0L,
                createdAtEpochMillis = 1L,
            )
        assertNull(RemoteImageCandidatePicker.pickLocalFallbackVariant(listOf(e)) { false })
    }
}
