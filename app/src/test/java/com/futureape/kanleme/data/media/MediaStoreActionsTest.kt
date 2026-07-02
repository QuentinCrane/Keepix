package com.futureape.kanleme.data.media

import org.junit.Assert.assertEquals
import org.junit.Test

class MediaStoreActionsTest {
    @Test
    fun normalizeRelativePath_addsTrailingSlashAndNormalizesSeparators() {
        assertEquals(
            "Pictures/Keepix/",
            MediaStoreActions.normalizeRelativePath("Pictures\\Keepix"),
        )
    }

    @Test
    fun normalizeRelativePath_usesDefaultFolderForBlankInput() {
        assertEquals(
            "Pictures/Kanleme/",
            MediaStoreActions.normalizeRelativePath("  "),
        )
    }

    @Test
    fun folderNameOf_returnsLastPathSegment() {
        assertEquals(
            "Keepix",
            MediaStoreActions.folderNameOf("Pictures/Keepix"),
        )
    }
}
