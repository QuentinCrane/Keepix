package com.futureape.kanleme.data.media

import android.content.ContentResolver
import android.content.ContentUris
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import com.futureape.kanleme.data.local.PhotoEntity
import com.futureape.kanleme.data.local.VideoEntity
import javax.inject.Inject
import javax.inject.Singleton

class MediaStoreAccessException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

@Singleton
class MediaStoreScanner @Inject constructor(
    private val resolver: ContentResolver,
) {
    fun scanImages(limit: Int? = null): List<PhotoEntity> {
        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.DATE_MODIFIED,
            MediaStore.Images.Media.RELATIVE_PATH,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
        )
        val result = mutableListOf<PhotoEntity>()
        queryMedia(
            collection = collection,
            projection = projection,
            sortColumn = MediaStore.Images.Media.DATE_TAKEN,
            fallbackSortColumn = MediaStore.Images.Media.DATE_ADDED,
            limit = limit,
            mediaLabel = "照片",
        )?.use { cursor ->
            val idCol = cursor.require(MediaStore.Images.Media._ID)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val relativePath = cursor.string(MediaStore.Images.Media.RELATIVE_PATH)
                val displayName = cursor.string(MediaStore.Images.Media.DISPLAY_NAME)?.ifBlank { "IMG_" + id } ?: "IMG_" + id
                val uri = ContentUris.withAppendedId(collection, id).toString()
                val folderName = relativePath?.trimEnd('/')?.substringAfterLast('/')?.ifBlank { "相册" } ?: "相册"
                val dateAddedSeconds = cursor.long(MediaStore.Images.Media.DATE_ADDED)
                val dateTaken = cursor.long(MediaStore.Images.Media.DATE_TAKEN).takeIf { it > 0 }
                    ?: dateAddedSeconds.takeIf { it > 0 }?.times(1000)
                    ?: cursor.long(MediaStore.Images.Media.DATE_MODIFIED).takeIf { it > 0 }?.times(1000)
                    ?: 0L
                val width = cursor.int(MediaStore.Images.Media.WIDTH)
                val height = cursor.int(MediaStore.Images.Media.HEIGHT)
                result += PhotoEntity(
                    mediaStoreId = id,
                    stableMediaKey = "image:" + id + ":" + cursor.long(MediaStore.Images.Media.DATE_MODIFIED),
                    uri = uri,
                    displayName = displayName,
                    size = cursor.long(MediaStore.Images.Media.SIZE),
                    dateTaken = dateTaken,
                    dateAdded = dateAddedSeconds,
                    dateModified = cursor.long(MediaStore.Images.Media.DATE_MODIFIED),
                    relativePath = relativePath,
                    folderPath = relativePath ?: "",
                    folderName = folderName,
                    mimeType = cursor.string(MediaStore.Images.Media.MIME_TYPE) ?: "image/*",
                    width = width,
                    height = height,
                    isScreenshot = isScreenshot(relativePath, displayName),
                    isSelfie = isSelfie(relativePath, displayName),
                    isGif = displayName.endsWith(".gif", ignoreCase = true),
                    isRaw = isRaw(displayName),
                    isLongImage = isLongImage(width, height),
                )
            }
        }
        return result
    }

    fun scanVideos(limit: Int? = null): List<VideoEntity> {
        val collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.DATE_TAKEN,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.DATE_MODIFIED,
            MediaStore.Video.Media.RELATIVE_PATH,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
        )
        val result = mutableListOf<VideoEntity>()
        queryMedia(
            collection = collection,
            projection = projection,
            sortColumn = MediaStore.Video.Media.DATE_TAKEN,
            fallbackSortColumn = MediaStore.Video.Media.DATE_ADDED,
            limit = limit,
            mediaLabel = "视频",
        )?.use { cursor ->
            val idCol = cursor.require(MediaStore.Video.Media._ID)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val relativePath = cursor.string(MediaStore.Video.Media.RELATIVE_PATH)
                val displayName = cursor.string(MediaStore.Video.Media.DISPLAY_NAME)?.ifBlank { "VID_" + id } ?: "VID_" + id
                val folderName = relativePath?.trimEnd('/')?.substringAfterLast('/')?.ifBlank { "视频" } ?: "视频"
                val dateAddedSeconds = cursor.long(MediaStore.Video.Media.DATE_ADDED)
                val dateTaken = cursor.long(MediaStore.Video.Media.DATE_TAKEN).takeIf { it > 0 }
                    ?: dateAddedSeconds.takeIf { it > 0 }?.times(1000)
                    ?: cursor.long(MediaStore.Video.Media.DATE_MODIFIED).takeIf { it > 0 }?.times(1000)
                    ?: 0L
                result += VideoEntity(
                    mediaStoreId = id,
                    stableMediaKey = "video:" + id + ":" + cursor.long(MediaStore.Video.Media.DATE_MODIFIED),
                    uri = ContentUris.withAppendedId(collection, id).toString(),
                    displayName = displayName,
                    size = cursor.long(MediaStore.Video.Media.SIZE),
                    duration = cursor.long(MediaStore.Video.Media.DURATION),
                    dateTaken = dateTaken,
                    dateAdded = dateAddedSeconds,
                    dateModified = cursor.long(MediaStore.Video.Media.DATE_MODIFIED),
                    folderPath = relativePath ?: "",
                    folderName = folderName,
                    mimeType = cursor.string(MediaStore.Video.Media.MIME_TYPE) ?: "video/*",
                    width = cursor.int(MediaStore.Video.Media.WIDTH),
                    height = cursor.int(MediaStore.Video.Media.HEIGHT),
                )
            }
        }
        return result
    }

    private fun queryMedia(
        collection: Uri,
        projection: Array<String>,
        sortColumn: String,
        fallbackSortColumn: String,
        limit: Int?,
        mediaLabel: String,
    ): Cursor? {
        val queryArgs = Bundle().apply {
            putStringArray(ContentResolver.QUERY_ARG_SORT_COLUMNS, arrayOf(sortColumn, fallbackSortColumn))
            putInt(ContentResolver.QUERY_ARG_SORT_DIRECTION, ContentResolver.QUERY_SORT_DIRECTION_DESCENDING)
            if (limit != null && limit > 0) putInt(ContentResolver.QUERY_ARG_LIMIT, limit)
        }
        return try {
            resolver.query(collection, projection, queryArgs, null)
        } catch (security: SecurityException) {
            throw MediaStoreAccessException("没有读取" + mediaLabel + "的系统权限，请重新授权相册访问", security)
        } catch (_: IllegalArgumentException) {
            // Some OEM MediaStore providers are picky about Bundle sort columns.
            // Fallback to a simple SQL sort order, without embedding LIMIT in the SQL string.
            val sortOrder = sortColumn + " DESC"
            try {
                resolver.query(collection, projection, null, null, sortOrder)
            } catch (security: SecurityException) {
                throw MediaStoreAccessException("没有读取" + mediaLabel + "的系统权限，请重新授权相册访问", security)
            }
        }
    }

    private fun isScreenshot(path: String?, name: String): Boolean {
        val text = (path.orEmpty() + "/" + name).lowercase()
        return listOf("screenshot", "screenshots", "截屏", "截图").any { text.contains(it) }
    }

    private fun isSelfie(path: String?, name: String): Boolean {
        val text = (path.orEmpty() + "/" + name).lowercase()
        return listOf("selfie", "front", "自拍").any { text.contains(it) }
    }

    private fun isRaw(name: String): Boolean {
        val n = name.lowercase()
        return listOf(".dng", ".nef", ".cr2", ".arw", ".raf", ".rw2").any { n.endsWith(it) }
    }

    private fun isLongImage(width: Int, height: Int): Boolean = width > 0 && height > width * 3
}

private fun Cursor.require(column: String): Int = getColumnIndexOrThrow(column)
private fun Cursor.string(column: String): String? = getColumnIndex(column).takeIf { it >= 0 }?.let { if (isNull(it)) null else getString(it) }
private fun Cursor.long(column: String): Long = getColumnIndex(column).takeIf { it >= 0 }?.let { if (isNull(it)) 0L else getLong(it) } ?: 0L
private fun Cursor.int(column: String): Int = getColumnIndex(column).takeIf { it >= 0 }?.let { if (isNull(it)) 0 else getInt(it) } ?: 0
