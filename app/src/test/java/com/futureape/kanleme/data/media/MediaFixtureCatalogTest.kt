package com.futureape.kanleme.data.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private enum class FixtureAvailability {
    SYNTHETIC_CONTRACT,
    LICENSED_SAMPLE_REQUIRED,
}

private data class MediaFixtureSpec(
    val id: String,
    val format: String,
    val availability: FixtureAvailability,
    val source: String,
    val license: String,
    val containsPersonalData: Boolean = false,
)

private object MediaFixtureCatalog {
    val entries = listOf(
        MediaFixtureSpec("jpeg_standard", "JPEG", FixtureAvailability.SYNTHETIC_CONTRACT, "generated test bytes", "CC0-like generated data"),
        MediaFixtureSpec("jpeg_long_image", "long JPEG", FixtureAvailability.LICENSED_SAMPLE_REQUIRED, "licensed fixture intake", "record with sample"),
        MediaFixtureSpec("gif_animation", "GIF", FixtureAvailability.SYNTHETIC_CONTRACT, "generated test bytes", "CC0-like generated data"),
        MediaFixtureSpec("raw_dng", "RAW/DNG", FixtureAvailability.LICENSED_SAMPLE_REQUIRED, "licensed fixture intake", "record with sample"),
        MediaFixtureSpec("heif_photo", "HEIF/HEIC", FixtureAvailability.LICENSED_SAMPLE_REQUIRED, "licensed fixture intake", "record with sample"),
        MediaFixtureSpec("corrupt_image", "corrupt image", FixtureAvailability.SYNTHETIC_CONTRACT, "generated invalid bytes", "CC0-like generated data"),
        MediaFixtureSpec("zero_byte", "zero-byte media", FixtureAvailability.SYNTHETIC_CONTRACT, "generated empty bytes", "CC0-like generated data"),
        MediaFixtureSpec("motion_photo_tail", "JPEG motion photo", FixtureAvailability.SYNTHETIC_CONTRACT, "generated ftyp/moov tail", "CC0-like generated data"),
        MediaFixtureSpec("motion_vendor_variant", "vendor motion photo", FixtureAvailability.LICENSED_SAMPLE_REQUIRED, "licensed fixture intake", "record with sample"),
        MediaFixtureSpec("long_tail_video", "long-tail embedded video", FixtureAvailability.LICENSED_SAMPLE_REQUIRED, "licensed fixture intake", "record with sample"),
        MediaFixtureSpec("unreadable_uri", "unreadable content URI", FixtureAvailability.SYNTHETIC_CONTRACT, "generated URI contract", "CC0-like generated data"),
    )
}

class MediaFixtureCatalogTest {
    @Test
    fun catalogCoversTheE09FormatContract() {
        val requiredFormats = setOf(
            "JPEG",
            "long JPEG",
            "GIF",
            "RAW/DNG",
            "HEIF/HEIC",
            "corrupt image",
            "zero-byte media",
            "JPEG motion photo",
            "vendor motion photo",
            "long-tail embedded video",
            "unreadable content URI",
        )

        assertEquals(requiredFormats, MediaFixtureCatalog.entries.map { it.format }.toSet())
        assertEquals(requiredFormats.size, MediaFixtureCatalog.entries.size)
    }

    @Test
    fun everyEntryHasExplicitProvenanceAndPrivacyDeclaration() {
        assertTrue(MediaFixtureCatalog.entries.isNotEmpty())
        MediaFixtureCatalog.entries.forEach { fixture ->
            assertTrue(fixture.id.isNotBlank())
            assertTrue(fixture.source.isNotBlank())
            assertTrue(fixture.license.isNotBlank())
            assertFalse("Fixture must not contain personal data: ${fixture.id}", fixture.containsPersonalData)
        }
    }

    @Test
    fun realDecoderSamplesRemainExplicitlyNotRunUntilLicensedFilesAreAdded() {
        val requiredIntake = MediaFixtureCatalog.entries.count {
            it.availability == FixtureAvailability.LICENSED_SAMPLE_REQUIRED
        }

        assertTrue("Licensed OEM/format intake must remain visible", requiredIntake > 0)
        assertTrue(
            MediaFixtureCatalog.entries.any { it.availability == FixtureAvailability.SYNTHETIC_CONTRACT },
        )
    }
}
