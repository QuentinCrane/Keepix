package com.futureape.kanleme.data.media

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import android.os.CancellationSignal
import android.provider.DocumentsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class FolderPlaybackVideo(val uri: String, val name: String, val mimeType: String)

data class FolderVideoScan(val folderName: String, val videos: List<FolderPlaybackVideo>)

/** Only system providers backed by the device's shared storage are accepted. */
fun isLocalVideoFolderTree(uri: Uri): Boolean =
    uri.scheme == ContentResolver.SCHEME_CONTENT && DocumentsContract.isTreeUri(uri) &&
        uri.authority in setOf("com.android.externalstorage.documents", "com.android.providers.downloads.documents")

/** Reads the chosen tree directly; unindexed videos do not need broad MediaStore permission. */
suspend fun scanFolderVideos(
    resolver: ContentResolver,
    tree: Uri,
    includeSubfolders: Boolean,
    onProgress: (Int) -> Unit = {},
): FolderVideoScan = withContext(Dispatchers.IO) {
    require(isLocalVideoFolderTree(tree))
    val rootId = DocumentsContract.getTreeDocumentId(tree)
    val root = DocumentsContract.buildDocumentUriUsingTree(tree, rootId)
    val fallbackName = rootId.substringAfter(':', rootId).trimEnd('/').substringAfterLast('/')
    val name = runCatching {
        resolver.query(root, arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }.getOrNull()?.takeIf { it.isNotBlank() } ?: fallbackName
    val queue = ArrayDeque<String>().apply { add(rootId) }
    val visitedFolders = mutableSetOf<String>()
    val seenVideos = mutableSetOf<String>()
    val videos = mutableListOf<FolderPlaybackVideo>()
    val columns = arrayOf(
        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
        DocumentsContract.Document.COLUMN_MIME_TYPE,
        DocumentsContract.Document.COLUMN_FLAGS,
    )
    while (queue.isNotEmpty()) {
        val folderId = queue.removeFirst()
        if (!visitedFolders.add(folderId)) continue
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, folderId)
        queryFolderChildren(resolver, children, columns) { cursor, signal ->
            val idColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val mimeColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
            val flagsColumn = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_FLAGS)
            while (cursor.moveToNext()) {
                signal.throwIfCanceled()
                val id = cursor.getString(idColumn) ?: continue
                val mime = cursor.getString(mimeColumn).orEmpty()
                if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                    if (includeSubfolders) queue.addLast(id)
                    continue
                }
                val flags = if (flagsColumn >= 0 && !cursor.isNull(flagsColumn)) cursor.getInt(flagsColumn) else 0
                if (flags and DocumentsContract.Document.FLAG_VIRTUAL_DOCUMENT != 0) continue
                val displayName = cursor.getString(nameColumn).orEmpty().ifBlank { id.substringAfterLast('/') }
                if (!isVideoDocument(mime, displayName)) continue
                val uri = DocumentsContract.buildDocumentUriUsingTree(tree, id).toString()
                if (seenVideos.add(uri)) {
                    videos += FolderPlaybackVideo(uri, displayName, mime)
                    if (videos.size % 100 == 0) onProgress(videos.size)
                }
            }
        }
    }
    onProgress(videos.size)
    FolderVideoScan(name, videos)
}

private fun isVideoDocument(mime: String, name: String): Boolean {
    if (mime.startsWith("video/", ignoreCase = true)) return true
    if (mime.isNotBlank() && !mime.equals("application/octet-stream", ignoreCase = true)) return false
    return name.substringAfterLast('.', "").lowercase(Locale.ROOT) in setOf(
        "mp4", "m4v", "mkv", "webm", "mov", "3gp", "3g2", "avi", "mpeg", "mpg", "ts", "m2ts", "mts", "wmv", "flv", "ogv",
    )
}

private suspend fun queryFolderChildren(
    resolver: ContentResolver,
    uri: Uri,
    projection: Array<String>,
    read: (Cursor, CancellationSignal) -> Unit,
) = suspendCancellableCoroutine<Unit> { continuation ->
    val signal = CancellationSignal()
    continuation.invokeOnCancellation { signal.cancel() }
    try {
        val cursor = resolver.query(uri, projection, null, null, null, signal)
            ?: throw IllegalStateException("Folder query returned no cursor")
        cursor.use { read(it, signal) }
        if (continuation.isActive) continuation.resume(Unit)
    } catch (error: Exception) {
        if (continuation.isActive) continuation.resumeWithException(error)
    }
}
