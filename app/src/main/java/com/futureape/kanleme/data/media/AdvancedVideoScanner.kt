package com.futureape.kanleme.data.media

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.CancellationSignal
import android.provider.MediaStore
import com.futureape.kanleme.R
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class AdvancedVideoScanner @Inject constructor(
    private val resolver: ContentResolver,
    @param:ApplicationContext private val context: Context,
) {
    private val visibleSelection = "is_pending = 0 AND is_trashed = 0"

    suspend fun folders(): List<VideoFolder> = withContext(Dispatchers.IO) {
        requireVideoAccess()
        val counts = mutableMapOf<VideoFolderKey, Int>()
        for (volume in MediaStore.getExternalVolumeNames(context).sorted()) {
            query(MediaStore.Video.Media.getContentUri(volume), arrayOf(MediaStore.MediaColumns.RELATIVE_PATH),
                visibleSelection, emptyArray()) { cursor, signal ->
                val pathColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.RELATIVE_PATH)
                while (cursor.moveToNext()) {
                    signal.throwIfCanceled()
                    val key = VideoFolderKey(volume, cursor.getString(pathColumn).orEmpty())
                    counts[key] = (counts[key] ?: 0) + 1
                }
            }
        }
        counts.map { VideoFolder(it.key, it.value) }
            .sortedWith(compareBy<VideoFolder> { it.key.volume }.thenBy { it.key.relativePath })
    }

    /** null selects all volumes; an empty selection must never silently scan everything. */
    suspend fun scan(folders: Set<VideoFolderKey>?, includeSubfolders: Boolean = true, onProgress: (Int) -> Unit): List<AdvancedVideo> = withContext(Dispatchers.IO) {
        requireVideoAccess()
        require(folders == null || folders.isNotEmpty())
        val result = mutableListOf<AdvancedVideo>()
        val projection = arrayOf("_id", "_display_name", "relative_path", "_size", "width", "height",
            "duration", "date_added", "mime_type", "date_modified")
        for (volume in MediaStore.getExternalVolumeNames(context).sorted()) {
            val selectedPaths = folders?.filter { it.volume == volume }?.map { it.relativePath }?.sorted()
            val paths = if (!includeSubfolders) selectedPaths else selectedPaths?.fold(mutableListOf<String>()) { accepted, path ->
                if (accepted.lastOrNull()?.let { path.startsWith(it) } != true) accepted.add(path)
                accepted
            }
            if (paths != null && paths.isEmpty()) continue
            // Limit bind variables even when the user selects hundreds of folders.
            val batches: List<List<String>?> = paths?.chunked(100) ?: listOf(null)
            val collection = MediaStore.Video.Media.getContentUri(volume)
            for (batch in batches) {
                val selection = visibleSelection + when {
                    batch == null -> ""
                    includeSubfolders -> " AND (" + batch.joinToString(" OR ") { "COALESCE(relative_path, '') LIKE ? ESCAPE '\\'" } + ")"
                    else -> " AND COALESCE(relative_path, '') IN (" + batch.joinToString(",") { "?" } + ")"
                }
                val args = batch?.map { path -> if (!includeSubfolders) path else
                    path.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%" }?.toTypedArray() ?: emptyArray()
                query(collection, projection, selection, args) { cursor, signal ->
                    // Resolve projection indices once per cursor, never once per row/field.
                    val columns = projection.associateWith { cursor.getColumnIndex(it) }
                    fun number(column: String): Long = columns.getValue(column).let {
                        if (it < 0 || cursor.isNull(it)) 0 else cursor.getLong(it)
                    }
                    fun text(column: String): String = columns.getValue(column).let {
                        if (it < 0 || cursor.isNull(it)) "" else cursor.getString(it)
                    }
                    while (cursor.moveToNext()) {
                        signal.throwIfCanceled()
                        val width = number("width").toInt().coerceAtLeast(0)
                        val height = number("height").toInt().coerceAtLeast(0)
                        result += AdvancedVideo(
                            uri = ContentUris.withAppendedId(collection, number("_id")).toString(),
                            name = text("_display_name"),
                            folder = VideoFolderKey(volume, text("relative_path")),
                            sizeBytes = number("_size").coerceAtLeast(0),
                            width = width,
                            height = height,
                            durationMs = number("duration").coerceAtLeast(0),
                            dateAddedMs = number("date_added") * 1000L,
                            mimeType = text("mime_type"),
                            dateModifiedMs = number("date_modified") * 1000L,
                        )
                        if (result.size % 100 == 0) onProgress(result.size)
                    }
                }
            }
        }
        onProgress(result.size)
        result
    }

    private fun requireVideoAccess() {
        val access = MediaAccessSnapshot.current(context)
        if (!access.canReadVideos && !access.canReadSelectedVisualMedia) {
            throw SecurityException(context.getString(R.string.advanced_video_permission))
        }
    }

    private suspend fun <T> query(
        uri: Uri, projection: Array<String>, selection: String, args: Array<String>,
        read: (Cursor, CancellationSignal) -> T,
    ): T = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine { continuation ->
            val signal = CancellationSignal()
            continuation.invokeOnCancellation { signal.cancel() }
            try {
                val result = resolver.query(uri, projection, selection, args, null, signal)?.use {
                    read(it, signal)
                } ?: throw IllegalStateException(context.getString(R.string.advanced_scan_error))
                continuation.resume(result)
            } catch (error: Exception) {
                if (continuation.isActive) continuation.resumeWithException(error)
            }
        }
    }
}
