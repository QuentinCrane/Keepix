package com.futureape.kanleme.data.media

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MediaOperationContractTest {
    @Test
    fun authorizationBuilder_convertsSecurityExceptionToFailedResult() {
        val result = MediaAuthorizationRequestBuilder.build(
            kind = MediaOperationKind.TRASH,
            mediaUris = listOf("content://media/external/images/media/1"),
            failureReason = "authorization failed",
            operationId = "operation-3",
        ) {
            throw SecurityException("denied")
        }

        assertEquals(MediaAuthorizationResult.Failed("denied"), result)
    }

    @Test
    fun partialFailure_preservesSuccessfulAndFailedUris() {
        val succeeded = Uri.parse("content://media/external/images/media/1")
        val failed = Uri.parse("content://media/external/images/media/2")

        val result = MediaAuthorizationResult.PartialFailure(
            operationId = "operation-4",
            succeededUris = listOf(succeeded),
            failedUris = listOf(failed),
        )

        assertEquals(listOf(succeeded), result.succeededUris)
        assertEquals(listOf(failed), result.failedUris)
    }
}
