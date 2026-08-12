package com.futureape.kanleme.data.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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

    @Test
    fun operationTracker_consumesEachOperationOnlyOnce() {
        val tracker = MediaOperationTracker()

        assertTrue(tracker.register("operation-1"))
        assertFalse(tracker.register("operation-1"))
        assertTrue(tracker.consume("operation-1"))
        assertFalse(tracker.consume("operation-1"))
    }

    @Test
    fun authorizationBuilder_rejectsNonContentUrisBeforeCreatingPendingIntent() {
        var factoryCalled = false

        val result = MediaAuthorizationRequestBuilder.build(
            kind = MediaOperationKind.WRITE,
            mediaUris = listOf("file:///private/photo.jpg"),
            failureReason = "invalid",
            operationId = "operation-2",
        ) {
            factoryCalled = true
            error("should not be called")
        }

        assertEquals(MediaAuthorizationResult.Failed("invalid"), result)
        assertFalse(factoryCalled)
    }

}
