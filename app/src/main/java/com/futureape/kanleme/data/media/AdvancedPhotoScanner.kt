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
class AdvancedPhotoScanner @Inject constructor(
    private val resolver: ContentResolver,
    @param:ApplicationContext private val context: Context,
) {
    private val visibleSelection = "is_pending = 0 AND is_trashed = 0"

    suspend fun folders(): List<PhotoFolder> = withContext(Dispatchers.IO) {
        requirePhotoAccess()
        val counts = mutableMapOf<PhotoFolderKey, Int>()
        for (volume in MediaStore.getExternalVolumeNames(context).sorted()) {
            query(MediaStore.Images.Media.getContentUri(volume), arrayOf(MediaStore.MediaColumns.RELATIVE_PATH),
                visibleSelection, emptyArray()) { cursor, signal ->
                val pathColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.RELATIVE_PATH)
                while (cursor.moveToNext()) {
                    signal.throwIfCanceled()
                    val key = PhotoFolderKey(volume, if (cursor.isNull(pathColumn)) "" else cursor.getString(pathColumn).orEmpty())
                    counts[key] = (counts[key] ?: 0) + 1
                }
            }
        }
        counts.map { PhotoFolder(it.key, it.value) }
            .sortedWith(compareBy<PhotoFolder> { it.key.volume }.thenBy { it.key.relativePath })
    }

    /** null selects all volumes; an empty selection must never silently scan everything. */
    suspend fun scan(folders: Set<PhotoFolderKey>?, includeSubfolders: Boolean = true, onProgress: (Int) -> Unit): List<AdvancedPhoto> = withContext(Dispatchers.IO) {
        requirePhotoAccess()
        require(folders == null || folders.isNotEmpty())
        val result = mutableListOf<AdvancedPhoto>()
        val projection = arrayOf("_id", "_display_name", "relative_path", "_size", "width", "height",
            "date_added", "mime_type", "date_modified")
        for (volume in MediaStore.getExternalVolumeNames(context).sorted()) {
            val selectedPaths = folders?.filter { it.volume == volume }?.map { it.relativePath }?.sorted()
            val paths = if (!includeSubfolders) selectedPaths else selectedPaths?.fold(mutableListOf<String>()) { accepted, path ->
                if (accepted.lastOrNull()?.let { path.startsWith(it) } != true) accepted.add(path)
                accepted
            }
            if (paths != null && paths.isEmpty()) continue
            val batches: List<List<String>?> = paths?.chunked(100) ?: listOf(null)
            val collection = MediaStore.Images.Media.getContentUri(volume)
            for (batch in batches) {
                val selection = visibleSelection + when {
                    batch == null -> ""
                    includeSubfolders -> " AND (" + batch.joinToString(" OR ") { "COALESCE(relative_path, '') LIKE ? ESCAPE '\\'" } + ")"
                    else -> " AND COALESCE(relative_path, '') IN (" + batch.joinToString(",") { "?" } + ")"
                }
                val args = batch?.map { path ->
                    if (!includeSubfolders) path else path.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%"
                }?.toTypedArray() ?: emptyArray()
                query(collection, projection, selection, args) { cursor, signal ->
                    val columns = projection.associateWith { cursor.getColumnIndex(it) }
                    fun number(column: String): Long = columns.getValue(column).let {
                        if (it < 0 || cursor.isNull(it)) 0 else cursor.getLong(it)
                    }
                    fun text(column: String): String = columns.getValue(column).let {
                        if (it < 0 || cursor.isNull(it)) "" else cursor.getString(it)
                    }
                    while (cursor.moveToNext()) {
                        signal.throwIfCanceled()
                        val id = number("_id")
                        val name = text("_display_name").ifBlank { "IMG_" + id }
                        val pathColumn = columns.getValue("relative_path")
                        val relativePath = if (pathColumn < 0 || cursor.isNull(pathColumn)) "" else cursor.getString(pathColumn).orEmpty()
                        val mimeType = text("mime_type")
                        val width = number("width").toInt().coerceAtLeast(0)
                        val height = number("height").toInt().coerceAtLeast(0)
                        result += AdvancedPhoto(
                            uri = ContentUris.withAppendedId(collection, id).toString(),
                            name = name,
                            folder = PhotoFolderKey(volume, relativePath),
                            sizeBytes = number("_size").coerceAtLeast(0L),
                            width = width,
                            height = height,
                            dateAddedMs = number("date_added").coerceAtLeast(0L) * 1000L,
                            mimeType = mimeType,
                            dateModifiedMs = number("date_modified").coerceAtLeast(0L) * 1000L,
                            isScreenshot = isScreenshot(relativePath, name),
                            isSelfie = isSelfie(relativePath, name),
                            isMotion = DynamicPhotoDetector.hasMotionNameHint(name, relativePath),
                            isGif = DynamicPhotoDetector.isGif(mimeType, name),
                            isRaw = isRaw(name, mimeType),
                            isLongImage = width > 0 && height > width * 3,
                        )
                        if (result.size % 100 == 0) onProgress(result.size)
                    }
                }
            }
        }
        onProgress(result.size)
        result
    }

    private fun requirePhotoAccess() {
        val access = MediaAccessSnapshot.current(context)
        if (!access.canReadImages && !access.canReadSelectedVisualMedia) {
            throw SecurityException(context.getString(R.string.advanced_photo_permission))
        }
    }

    private fun isScreenshot(path: String, name: String): Boolean {
        val text = (path + "/" + name).lowercase()
        return listOf("screenshot", "screenshots", "截屏", "截图").any { text.contains(it) }
    }

    private fun isSelfie(path: String, name: String): Boolean {
        val text = (path + "/" + name).lowercase()
        return listOf("selfie", "front", "自拍").any { text.contains(it) }
    }

    private fun isRaw(name: String, mimeType: String): Boolean {
        val extension = name.substringAfterLast('.', "").lowercase()
        return extension in setOf("dng", "nef", "cr2", "cr3", "arw", "raf", "rw2", "orf", "pef") ||
            mimeType.lowercase() in setOf("image/x-adobe-dng", "image/x-nikon-nef", "image/x-canon-cr2", "image/x-sony-arw")
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
                } ?: throw IllegalStateException(context.getString(R.string.advanced_photo_scan_error))
                continuation.resume(result)
            } catch (error: Exception) {
                if (continuation.isActive) continuation.resumeWithException(error)
            }
        }
    }
}
