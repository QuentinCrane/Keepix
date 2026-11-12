package com.futureape.kanleme.data.media

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import com.futureape.kanleme.data.local.PhotoEntity
import com.futureape.kanleme.data.local.VideoEntity
import com.futureape.kanleme.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

class MediaStoreAccessException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

@Singleton
class MediaStoreScanner @Inject constructor(
    private val resolver: ContentResolver,
    @ApplicationContext private val context: Context,
) {
    fun currentSyncSnapshot(): MediaStoreSyncSnapshot = MediaStoreSyncSnapshot(
        volumeName = MediaStore.VOLUME_EXTERNAL,
        mediaStoreVersion = MediaStore.getVersion(context),
        generation = MediaStore.getGeneration(context, MediaStore.VOLUME_EXTERNAL),
    )

    fun scanImages(
        limit: Int? = null,
        afterGeneration: Long? = null,
        throughGeneration: Long? = null,
        itemUri: String? = null,
    ): List<PhotoEntity> {
        val collection = itemUri?.let(Uri::parse) ?: MediaStore.Images.Media.EXTERNAL_CONTENT_URI
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
            MediaStore.Images.Media.ORIENTATION,
            MediaStore.MediaColumns.VOLUME_NAME,
        )
        val result = mutableListOf<PhotoEntity>()
        queryMedia(
            collection = collection,
            projection = projection,
            sortColumn = MediaStore.Images.Media.DATE_TAKEN,
            fallbackSortColumn = MediaStore.Images.Media.DATE_ADDED,
            limit = limit,
            afterGeneration = afterGeneration,
            throughGeneration = throughGeneration,
            mediaLabel = context.getString(R.string.media_label_photo),
        )?.use { cursor ->
            val idCol = cursor.require(MediaStore.Images.Media._ID)
            while (cursor.moveToNext() && (limit == null || limit <= 0 || result.size < limit)) {
                val id = cursor.getLong(idCol)
                val relativePath = cursor.string(MediaStore.Images.Media.RELATIVE_PATH)
                val displayName = cursor.string(MediaStore.Images.Media.DISPLAY_NAME)?.ifBlank { "IMG_" + id } ?: "IMG_" + id
                val uriObject = if (itemUri == null) ContentUris.withAppendedId(collection, id) else
                    ContentUris.withAppendedId(MediaStore.Images.Media.getContentUri(
                        cursor.string(MediaStore.MediaColumns.VOLUME_NAME) ?: collection.pathSegments.first()), id)
                val uri = uriObject.toString()
                val mimeType = cursor.string(MediaStore.Images.Media.MIME_TYPE) ?: "image/*"
                val size = cursor.long(MediaStore.Images.Media.SIZE)
                val folderName = relativePath?.trimEnd('/')?.substringAfterLast('/')?.ifBlank { context.getString(R.string.media_default_album) } ?: context.getString(R.string.media_default_album)
                val dateAddedSeconds = cursor.long(MediaStore.Images.Media.DATE_ADDED)
                val dateTaken = cursor.long(MediaStore.Images.Media.DATE_TAKEN).takeIf { it > 0 }
                    ?: dateAddedSeconds.takeIf { it > 0 }?.times(1000)
                    ?: cursor.long(MediaStore.Images.Media.DATE_MODIFIED).takeIf { it > 0 }?.times(1000)
                    ?: 0L
                val width = cursor.int(MediaStore.Images.Media.WIDTH)
                val height = cursor.int(MediaStore.Images.Media.HEIGHT)
                val orientation = cursor.int(MediaStore.Images.Media.ORIENTATION).takeIf { it != 0 }
                // Keep scanning strictly lightweight: never open or sniff the media file here.
                // The previous APK-reference build tried to parse Motion Photo XMP/MP4 during
                // MediaStore scanning, which can block large libraries and make the app look as
                // if no photos/videos were detected after permission was granted. Motion playback
                // is resolved lazily only for the currently visible card.
                val isGif = DynamicPhotoDetector.isGif(mimeType, displayName)
                val isMotionHint = DynamicPhotoDetector.hasMotionNameHint(displayName, relativePath)
                val needsMotionDetection = DynamicPhotoDetector.isPossiblyAnimatedStill(mimeType, displayName) || isMotionHint
                result += PhotoEntity(
                    mediaStoreId = id,
                    stableMediaKey = "image:" + id + ":" + cursor.long(MediaStore.Images.Media.DATE_MODIFIED),
                    uri = uri,
                    displayName = displayName,
                    size = size,
                    dateTaken = dateTaken,
                    dateAdded = dateAddedSeconds,
                    dateModified = cursor.long(MediaStore.Images.Media.DATE_MODIFIED),
                    relativePath = relativePath,
                    folderPath = relativePath ?: "",
                    folderName = folderName,
                    mimeType = mimeType,
                    width = width,
                    height = height,
                    exifOrientation = orientation,
                    isScreenshot = isScreenshot(relativePath, displayName),
                    isSelfie = isSelfie(relativePath, displayName),
                    isMotionPhoto = isMotionHint,
                    motionVideoOffset = 0L,
                    motionVideoSize = 0L,
                    isGif = isGif,
                    motionPhotoNeedsDetection = needsMotionDetection,
                    isRaw = isRaw(displayName),
                    isLongImage = isLongImage(width, height),
                )
            }
        }
        return result
    }

    fun scanVideos(
        limit: Int? = null,
        afterGeneration: Long? = null,
        throughGeneration: Long? = null,
        itemUri: String? = null,
    ): List<VideoEntity> {
        val collection = itemUri?.let(Uri::parse) ?: MediaStore.Video.Media.EXTERNAL_CONTENT_URI
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
            MediaStore.MediaColumns.VOLUME_NAME,
        )
        val result = mutableListOf<VideoEntity>()
        queryMedia(
            collection = collection,
            projection = projection,
            sortColumn = MediaStore.Video.Media.DATE_TAKEN,
            fallbackSortColumn = MediaStore.Video.Media.DATE_ADDED,
            limit = limit,
            afterGeneration = afterGeneration,
            throughGeneration = throughGeneration,
            mediaLabel = context.getString(R.string.media_label_video),
        )?.use { cursor ->
            val idCol = cursor.require(MediaStore.Video.Media._ID)
            while (cursor.moveToNext() && (limit == null || limit <= 0 || result.size < limit)) {
                val id = cursor.getLong(idCol)
                val relativePath = cursor.string(MediaStore.Video.Media.RELATIVE_PATH)
                val displayName = cursor.string(MediaStore.Video.Media.DISPLAY_NAME)?.ifBlank { "VID_" + id } ?: "VID_" + id
                val folderName = relativePath?.trimEnd('/')?.substringAfterLast('/')?.ifBlank { context.getString(R.string.media_default_video_folder) } ?: context.getString(R.string.media_default_video_folder)
                val dateAddedSeconds = cursor.long(MediaStore.Video.Media.DATE_ADDED)
                val dateTaken = cursor.long(MediaStore.Video.Media.DATE_TAKEN).takeIf { it > 0 }
                    ?: dateAddedSeconds.takeIf { it > 0 }?.times(1000)
                    ?: cursor.long(MediaStore.Video.Media.DATE_MODIFIED).takeIf { it > 0 }?.times(1000)
                    ?: 0L
                result += VideoEntity(
                    mediaStoreId = id,
                    stableMediaKey = "video:" + id + ":" + cursor.long(MediaStore.Video.Media.DATE_MODIFIED),
                    uri = if (itemUri == null) ContentUris.withAppendedId(collection, id).toString() else
                        ContentUris.withAppendedId(MediaStore.Video.Media.getContentUri(
                            cursor.string(MediaStore.MediaColumns.VOLUME_NAME) ?: collection.pathSegments.first()), id).toString(),
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
        afterGeneration: Long?,
        throughGeneration: Long?,
        mediaLabel: String,
    ): Cursor? {
        val queryArgs = Bundle().apply {
            putStringArray(ContentResolver.QUERY_ARG_SORT_COLUMNS, arrayOf(sortColumn, fallbackSortColumn))
            putInt(ContentResolver.QUERY_ARG_SORT_DIRECTION, ContentResolver.QUERY_SORT_DIRECTION_DESCENDING)
            putInt(MediaStore.QUERY_ARG_MATCH_PENDING, MediaStore.MATCH_EXCLUDE)
            putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_EXCLUDE)
            if (afterGeneration != null) {
                val generationPredicate = if (throughGeneration == null) {
                    "(" + MediaStore.MediaColumns.GENERATION_ADDED + " > ? OR " +
                        MediaStore.MediaColumns.GENERATION_MODIFIED + " > ?)"
                } else {
                    "((" + MediaStore.MediaColumns.GENERATION_ADDED + " > ? AND " +
                        MediaStore.MediaColumns.GENERATION_ADDED + " <= ?) OR (" +
                        MediaStore.MediaColumns.GENERATION_MODIFIED + " > ? AND " +
                        MediaStore.MediaColumns.GENERATION_MODIFIED + " <= ?))"
                }
                putString(
                    ContentResolver.QUERY_ARG_SQL_SELECTION,
                    generationPredicate,
                )
                putStringArray(
                    ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS,
                    if (throughGeneration == null) {
                        arrayOf(afterGeneration.toString(), afterGeneration.toString())
                    } else {
                        arrayOf(
                            afterGeneration.toString(), throughGeneration.toString(),
                            afterGeneration.toString(), throughGeneration.toString(),
                        )
                    },
                )
            }
            if (limit != null && limit > 0) putInt(ContentResolver.QUERY_ARG_LIMIT, limit)
        }
        return try {
            resolver.query(collection, projection, queryArgs, null)
        } catch (security: SecurityException) {
            throw MediaStoreAccessException(context.getString(R.string.media_error_read_permission, mediaLabel), security)
        } catch (_: IllegalArgumentException) {
            // Some OEM MediaStore providers are picky about Bundle sort columns.
            // Fallback to SQL while preserving the generation predicate. Never fall back to a
            // full scan here: doing so would make the incremental path silently expensive.
            val sortOrder = sortColumn + " DESC"
            val selection = buildString {
                append(MediaStore.MediaColumns.IS_PENDING).append(" = 0 AND ")
                append(MediaStore.MediaColumns.IS_TRASHED).append(" = 0")
                if (afterGeneration != null) {
                    append(" AND (")
                    if (throughGeneration == null) {
                        append(MediaStore.MediaColumns.GENERATION_ADDED).append(" > ? OR ")
                        append(MediaStore.MediaColumns.GENERATION_MODIFIED).append(" > ?)")
                    } else {
                        append("(")
                        append(MediaStore.MediaColumns.GENERATION_ADDED).append(" > ? AND ")
                        append(MediaStore.MediaColumns.GENERATION_ADDED).append(" <= ?) OR (")
                        append(MediaStore.MediaColumns.GENERATION_MODIFIED).append(" > ? AND ")
                        append(MediaStore.MediaColumns.GENERATION_MODIFIED).append(" <= ?))")
                    }
                }
            }
            val selectionArgs = afterGeneration?.let {
                if (throughGeneration == null) {
                    arrayOf(it.toString(), it.toString())
                } else {
                    arrayOf(
                        it.toString(), throughGeneration.toString(),
                        it.toString(), throughGeneration.toString(),
                    )
                }
            }
            try {
                resolver.query(collection, projection, selection, selectionArgs, sortOrder)
            } catch (security: SecurityException) {
                throw MediaStoreAccessException(context.getString(R.string.media_error_read_permission, mediaLabel), security)
            }
        }
    }

    fun findUnavailableImageIds(mediaStoreIds: List<Long>): Set<Long> =
        findUnavailableIds(
            collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            mediaStoreIds = mediaStoreIds,
            mediaLabel = context.getString(R.string.media_label_photo),
        )

    fun findUnavailableVideoIds(mediaStoreIds: List<Long>): Set<Long> =
        findUnavailableIds(
            collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            mediaStoreIds = mediaStoreIds,
            mediaLabel = context.getString(R.string.media_label_video),
        )

    private fun findUnavailableIds(
        collection: Uri,
        mediaStoreIds: List<Long>,
        mediaLabel: String,
    ): Set<Long> {
        if (mediaStoreIds.isEmpty()) return emptySet()
        val unavailable = mutableSetOf<Long>()
        mediaStoreIds.chunked(900).forEach { chunk ->
            val placeholders = chunk.joinToString(",") { "?" }
            val selection = MediaStore.MediaColumns._ID + " IN (" + placeholders + ")"
            val selectionArgs = chunk.map(Long::toString).toTypedArray()
            val projection = arrayOf(MediaStore.MediaColumns._ID, MediaStore.MediaColumns.IS_TRASHED)
            val queryArgs = Bundle().apply {
                putInt(MediaStore.QUERY_ARG_MATCH_PENDING, MediaStore.MATCH_EXCLUDE)
                putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_INCLUDE)
                putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selection)
                putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, selectionArgs)
            }
            val present = mutableMapOf<Long, Boolean>()
            val cursor = try {
                resolver.query(collection, projection, queryArgs, null)
            } catch (_: IllegalArgumentException) {
                // If an OEM ignores the Bundle form, the normal provider query still gives a
                // safe answer: trashed rows are normally excluded and therefore count as absent.
                try {
                    resolver.query(collection, projection, selection, selectionArgs, null)
                } catch (_: IllegalArgumentException) {
                    try {
                        resolver.query(
                            collection,
                            arrayOf(MediaStore.MediaColumns._ID),
                            selection,
                            selectionArgs,
                            null,
                        )
                    } catch (security: SecurityException) {
                        throw MediaStoreAccessException(context.getString(R.string.media_error_read_permission, mediaLabel), security)
                    }
                } catch (security: SecurityException) {
                    throw MediaStoreAccessException(context.getString(R.string.media_error_read_permission, mediaLabel), security)
                }
            } catch (security: SecurityException) {
                throw MediaStoreAccessException(context.getString(R.string.media_error_read_permission, mediaLabel), security)
            }
            cursor?.use { rows ->
                val idColumn = rows.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                val trashedColumn = rows.getColumnIndex(MediaStore.MediaColumns.IS_TRASHED)
                while (rows.moveToNext()) {
                    val id = rows.getLong(idColumn)
                    val isTrashed = trashedColumn >= 0 && !rows.isNull(trashedColumn) && rows.getInt(trashedColumn) != 0
                    present[id] = isTrashed
                }
            }
            chunk.forEach { id ->
                if (present[id] != false) unavailable += id
            }
        }
        return unavailable
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
