package com.futureape.kanleme.data.media

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import com.futureape.kanleme.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import java.util.UUID

sealed interface MediaOperationResult {
    data object Success : MediaOperationResult
    data class Failed(val reason: String) : MediaOperationResult
}

@Singleton
class MediaStoreActions @Inject constructor(
    private val resolver: ContentResolver,
    @ApplicationContext private val context: Context,
) {
    fun createWriteAuthorizationRequest(mediaUris: Collection<String>): MediaAuthorizationResult =
        createAuthorizationRequest(MediaOperationKind.WRITE, mediaUris) { uris ->
            MediaStore.createWriteRequest(resolver, uris)
        }

    fun createTrashAuthorizationRequest(
        mediaUris: Collection<String>,
        isTrashed: Boolean,
    ): MediaAuthorizationResult = createAuthorizationRequest(
        kind = if (isTrashed) MediaOperationKind.TRASH else MediaOperationKind.RESTORE,
        mediaUris = mediaUris,
    ) { uris ->
        MediaStore.createTrashRequest(resolver, uris, isTrashed)
    }

    fun createDeleteAuthorizationRequest(mediaUris: Collection<String>): MediaAuthorizationResult {
        val parsedUrisWithDuplicates = mediaUris.mapNotNull { rawUri ->
            rawUri.takeIf(String::isNotBlank)?.let { runCatching { Uri.parse(it) }.getOrNull() }
        }
        if (parsedUrisWithDuplicates.size != mediaUris.size) {
            return MediaAuthorizationResult.Failed(context.getString(R.string.media_error_invalid_uri))
        }
        val parsedUris = parsedUrisWithDuplicates.distinct()
        if (parsedUris.isEmpty() || parsedUris.any { !isValidMediaStoreUri(it) }) {
            return MediaAuthorizationResult.Failed(context.getString(R.string.media_error_invalid_uri))
        }
        return createDeleteRequestWithStaleUriRecovery(parsedUris)
    }

    fun verifyMediaDeleted(mediaUris: Collection<String>): MediaAuthorizationResult {
        val uris = mediaUris.mapNotNull { rawUri ->
            runCatching { Uri.parse(rawUri) }.getOrNull()
        }.distinct()
        if (uris.isEmpty() || uris.any { !isValidMediaStoreUri(it) }) {
            return MediaAuthorizationResult.Failed(context.getString(R.string.media_error_invalid_uri))
        }

        val deletedUris = mutableListOf<Uri>()
        val stillPresentUris = mutableListOf<Uri>()
        uris.forEach { uri ->
            try {
                if (mediaItemExists(uri)) stillPresentUris += uri else deletedUris += uri
            } catch (_: SecurityException) {
                stillPresentUris += uri
            } catch (_: Throwable) {
                stillPresentUris += uri
            }
        }
        return if (stillPresentUris.isEmpty()) {
            MediaAuthorizationResult.Completed
        } else {
            MediaAuthorizationResult.PartialFailure(
                operationId = "delete-verification-${UUID.randomUUID()}",
                succeededUris = deletedUris,
                failedUris = stillPresentUris,
            )
        }
    }

    fun moveImageToFolder(mediaUri: String, targetRelativePath: String): MediaOperationResult {
        return moveMediaToFolder(mediaUri, targetRelativePath)
    }

    fun moveMediaToFolder(mediaUri: String, targetRelativePath: String): MediaOperationResult {
        val normalizedPath = normalizeRelativePath(targetRelativePath)
        val uri = runCatching { Uri.parse(mediaUri) }.getOrNull()
            ?: return MediaOperationResult.Failed(context.getString(R.string.media_error_invalid_uri))
        if (!isValidMediaStoreUri(uri)) {
            return MediaOperationResult.Failed(context.getString(R.string.media_error_invalid_uri))
        }

        return try {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.RELATIVE_PATH, normalizedPath)
            }
            val rows = resolver.update(uri, values, null, null)
            if (rows > 0) {
                MediaOperationResult.Success
            } else {
                MediaOperationResult.Failed(context.getString(R.string.media_error_move_no_rows))
            }
        } catch (security: SecurityException) {
            MediaOperationResult.Failed(context.getString(R.string.media_error_move_security))
        } catch (throwable: Throwable) {
            MediaOperationResult.Failed(throwable.message ?: context.getString(R.string.media_error_move_folder_failed))
        }
    }

    private fun createAuthorizationRequest(
        kind: MediaOperationKind,
        mediaUris: Collection<String>,
        createPendingIntent: (List<Uri>) -> android.app.PendingIntent,
    ): MediaAuthorizationResult = MediaAuthorizationRequestBuilder.build(
        kind = kind,
        mediaUris = mediaUris,
        failureReason = context.getString(R.string.media_error_authorization_failed),
        operationId = UUID.randomUUID().toString(),
        createPendingIntent = createPendingIntent,
    )

    /** Includes system-trashed rows so preflight and post-delete checks don't mistake them for missing. */
    private fun mediaItemExists(uri: Uri): Boolean {
        val cursor = resolver.query(uri, arrayOf(MediaStore.MediaColumns._ID), mediaStateQueryArgs(), null)
            ?: throw IllegalStateException("Unable to query MediaStore item")
        return cursor.use { it.moveToFirst() }
    }

    private fun createDeleteRequestWithStaleUriRecovery(initialUris: List<Uri>): MediaAuthorizationResult {
        var uris = initialUris
        repeat(2) {
            if (uris.isEmpty()) return MediaAuthorizationResult.Completed
            val pendingIntent = try {
                MediaStore.createDeleteRequest(resolver, uris)
            } catch (_: IllegalArgumentException) {
                // MediaProvider can reject a URI whose row was removed outside the app after it
                // entered the local trash list. Prune only rows confirmed absent, then retry.
                val stillExisting = try {
                    uris.filter(::mediaItemExists)
                } catch (security: SecurityException) {
                    return MediaAuthorizationResult.Failed(
                        security.message ?: context.getString(R.string.media_error_authorization_failed),
                    )
                } catch (error: Exception) {
                    return MediaAuthorizationResult.Failed(
                        error.message ?: context.getString(R.string.media_error_authorization_failed),
                    )
                }
                if (stillExisting.size == uris.size) {
                    return MediaAuthorizationResult.Failed(context.getString(R.string.media_error_invalid_uri))
                }
                uris = stillExisting
                return@repeat
            } catch (security: SecurityException) {
                return MediaAuthorizationResult.Failed(
                    security.message ?: context.getString(R.string.media_error_authorization_failed),
                )
            } catch (error: Exception) {
                return MediaAuthorizationResult.Failed(
                    error.message ?: context.getString(R.string.media_error_authorization_failed),
                )
            }
            return MediaAuthorizationResult.NeedsUserConsent(
                MediaOperationRequest(
                    operationId = UUID.randomUUID().toString(),
                    kind = MediaOperationKind.PERMANENT_DELETE,
                    uris = uris,
                    pendingIntent = pendingIntent,
                ),
            )
        }
        return if (uris.isEmpty()) MediaAuthorizationResult.Completed
        else MediaAuthorizationResult.Failed(context.getString(R.string.media_error_invalid_uri))
    }

    private fun mediaStateQueryArgs(): Bundle = Bundle().apply {
        putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_INCLUDE)
        putInt(MediaStore.QUERY_ARG_MATCH_PENDING, MediaStore.MATCH_INCLUDE)
    }

    companion object {
        internal fun isValidMediaStoreUri(uri: Uri): Boolean =
            uri.scheme == "content" && uri.authority == MediaStore.AUTHORITY &&
                runCatching { android.content.ContentUris.parseId(uri) > 0L }.getOrDefault(false)

        fun normalizeRelativePath(path: String): String {
            val clean = path.trim().replace('\\', '/').trim('/')
            return when {
                clean.isBlank() -> "Pictures/Kanleme/"
                clean.endsWith("/") -> clean
                else -> "$clean/"
            }
        }

        fun folderNameOf(relativePath: String): String =
            normalizeRelativePath(relativePath).trimEnd('/').substringAfterLast('/').ifBlank { "Kanleme" }
    }
}
