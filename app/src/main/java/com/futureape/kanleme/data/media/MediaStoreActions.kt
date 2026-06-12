package com.futureape.kanleme.data.media

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.futureape.kanleme.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

sealed interface MediaOperationResult {
    data object Success : MediaOperationResult
    data class Failed(val reason: String) : MediaOperationResult
}

@Singleton
class MediaStoreActions @Inject constructor(
    private val resolver: ContentResolver,
    @ApplicationContext private val context: Context,
) {
    fun moveImageToFolder(mediaUri: String, targetRelativePath: String): MediaOperationResult {
        return moveMediaToFolder(mediaUri, targetRelativePath)
    }

    fun moveMediaToFolder(mediaUri: String, targetRelativePath: String): MediaOperationResult {
        val normalizedPath = normalizeRelativePath(targetRelativePath)
        val uri = runCatching { Uri.parse(mediaUri) }.getOrNull()
            ?: return MediaOperationResult.Failed(context.getString(R.string.media_error_invalid_uri))

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

    fun moveMediaToSystemTrash(mediaUri: String): MediaOperationResult {
        val uri = runCatching { Uri.parse(mediaUri) }.getOrNull()
            ?: return MediaOperationResult.Failed(context.getString(R.string.media_error_invalid_uri))
        return try {
            val rows = resolver.update(
                uri,
                ContentValues().apply { put(MediaStore.MediaColumns.IS_TRASHED, 1) },
                null,
                null,
            )
            if (rows > 0) {
                MediaOperationResult.Success
            } else {
                MediaOperationResult.Failed(context.getString(R.string.media_error_trash_no_rows))
            }
        } catch (security: SecurityException) {
            MediaOperationResult.Failed(context.getString(R.string.media_error_trash_security))
        } catch (throwable: Throwable) {
            MediaOperationResult.Failed(throwable.message ?: context.getString(R.string.media_error_trash_failed))
        }
    }

    fun restoreFromSystemTrash(mediaUri: String): MediaOperationResult {
        val uri = runCatching { Uri.parse(mediaUri) }.getOrNull()
            ?: return MediaOperationResult.Failed(context.getString(R.string.media_error_invalid_uri))
        return try {
            val rows = resolver.update(
                uri,
                ContentValues().apply { put(MediaStore.MediaColumns.IS_TRASHED, 0) },
                null,
                null,
            )
            if (rows > 0) MediaOperationResult.Success else MediaOperationResult.Failed(context.getString(R.string.media_error_restore_no_rows))
        } catch (security: SecurityException) {
            MediaOperationResult.Failed(context.getString(R.string.media_error_restore_security))
        } catch (throwable: Throwable) {
            MediaOperationResult.Failed(throwable.message ?: context.getString(R.string.media_error_restore_failed))
        }
    }

    fun permanentlyDeleteMedia(mediaUri: String): MediaOperationResult {
        val uri = runCatching { Uri.parse(mediaUri) }.getOrNull()
            ?: return MediaOperationResult.Failed(context.getString(R.string.media_error_invalid_uri))
        return try {
            val rows = resolver.delete(uri, null, null)
            if (rows > 0) MediaOperationResult.Success else MediaOperationResult.Failed(context.getString(R.string.media_error_delete_no_rows))
        } catch (security: SecurityException) {
            MediaOperationResult.Failed(context.getString(R.string.media_error_delete_security))
        } catch (throwable: Throwable) {
            MediaOperationResult.Failed(throwable.message ?: context.getString(R.string.media_error_delete_failed))
        }
    }

    companion object {
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
