package com.futureape.kanleme.data.media

import android.app.PendingIntent
import android.content.IntentSender
import android.net.Uri
import java.util.Collections

enum class MediaOperationKind {
    WRITE,
    TRASH,
    RESTORE,
    PERMANENT_DELETE,
}

data class MediaOperationRequest(
    val operationId: String,
    val kind: MediaOperationKind,
    val uris: List<Uri>,
    val pendingIntent: PendingIntent,
) {
    init {
        require(operationId.isNotBlank()) { "operationId must not be blank" }
        require(uris.isNotEmpty()) { "uris must not be empty" }
    }

    val intentSender: IntentSender
        get() = pendingIntent.intentSender
}

sealed interface MediaAuthorizationResult {
    data object Completed : MediaAuthorizationResult

    data class NeedsUserConsent(
        val request: MediaOperationRequest,
    ) : MediaAuthorizationResult

    data class PartialFailure(
        val operationId: String,
        val succeededUris: List<Uri>,
        val failedUris: List<Uri>,
    ) : MediaAuthorizationResult

    data class Cancelled(val operationId: String) : MediaAuthorizationResult

    data class Failed(val reason: String) : MediaAuthorizationResult
}

fun interface MediaOperationResultCallback {
    fun onResult(result: MediaAuthorizationResult)
}

/**
 * Tracks authorization operations while the system confirmation UI is open.
 * A result is consumed at most once, even if the Activity callback is repeated.
 */
class MediaOperationTracker {
    private val pendingOperationIds = Collections.synchronizedSet(mutableSetOf<String>())

    fun register(operationId: String): Boolean {
        if (operationId.isBlank()) return false
        return pendingOperationIds.add(operationId)
    }

    fun consume(operationId: String): Boolean = pendingOperationIds.remove(operationId)

    fun clear(operationId: String) {
        pendingOperationIds.remove(operationId)
    }
}

internal object MediaAuthorizationRequestBuilder {
    fun build(
        kind: MediaOperationKind,
        mediaUris: Collection<String>,
        failureReason: String,
        operationId: String,
        createPendingIntent: (List<Uri>) -> PendingIntent,
    ): MediaAuthorizationResult {
        val parsedUris = mediaUris.map { rawUri ->
            if (rawUri.isBlank()) {
                null
            } else {
                runCatching { Uri.parse(rawUri) }.getOrNull()
            }
        }
        val uris = parsedUris.filterNotNull().distinct()
        val allUrisAreContentUris = parsedUris.size == mediaUris.size &&
            uris.isNotEmpty() &&
            uris.all { it.scheme == "content" && !it.authority.isNullOrBlank() }
        if (!allUrisAreContentUris) {
            return MediaAuthorizationResult.Failed(failureReason)
        }

        return try {
            MediaAuthorizationResult.NeedsUserConsent(
                MediaOperationRequest(
                    operationId = operationId,
                    kind = kind,
                    uris = uris,
                    pendingIntent = createPendingIntent(uris),
                ),
            )
        } catch (security: SecurityException) {
            MediaAuthorizationResult.Failed(security.message ?: failureReason)
        } catch (throwable: Throwable) {
            MediaAuthorizationResult.Failed(throwable.message ?: failureReason)
        }
    }
}
