package com.futureape.kanleme.ui.util

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import coil3.ImageLoader
import coil3.imageLoader
import coil3.BitmapImage
import coil3.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.memory.MemoryCache
import coil3.size.Precision
import coil3.size.Scale
import com.futureape.kanleme.data.local.PhotoEntity
import com.futureape.kanleme.data.local.TrashItemEntity
import com.futureape.kanleme.data.local.VideoEntity

private const val PHOTO_THUMB_CACHE_SUFFIX = "thumb"
private const val TRASH_THUMB_CACHE_SUFFIX = "thumb"
private const val PHOTO_THUMB_WIDTH = 420
private const val PHOTO_THUMB_HEIGHT = 560

fun photoContentUri(photo: PhotoEntity): Uri =
    originalContentUri(photo.uri) ?: if (photo.mediaStoreId > 0L) {
        ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, photo.mediaStoreId)
    } else {
        Uri.parse(photo.uri)
    }

fun videoContentUri(video: VideoEntity): Uri =
    originalContentUri(video.uri) ?: if (video.mediaStoreId > 0L) {
        ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, video.mediaStoreId)
    } else {
        Uri.parse(video.uri)
    }

fun trashContentUri(item: TrashItemEntity): Uri =
    originalContentUri(item.uri) ?: if (item.mediaStoreId > 0L) {
        val baseUri = if (item.mediaType == "video") {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        ContentUris.withAppendedId(baseUri, item.mediaStoreId)
    } else {
        Uri.parse(item.uri)
    }

fun photoImageRequest(
    context: Context,
    photo: PhotoEntity,
    cacheSuffix: String,
    width: Int,
    height: Int,
): ImageRequest {
    val uri = photoContentUri(photo)
    val suffix = normalizedPhotoCacheSuffix(cacheSuffix, photo.isGif)
    if (suffix == PHOTO_THUMB_CACHE_SUFFIX) return photoThumbnailImageRequest(context, photo)
    val cacheKey = photoCacheKey(photo, suffix)
    val displayKey = photoCacheKey(photo, "display")
    val placeholderKey = if (suffix == "preview" && context.imageLoader.memoryCache?.get(MemoryCache.Key(displayKey)) != null)
        displayKey else photoThumbnailCacheKey(photo)
    return mediaImageRequest(
        context = context,
        uri = uri,
        cacheKey = cacheKey,
        placeholderCacheKey = placeholderKey,
        width = if (suffix == "display") 1080 else width,
        height = if (suffix == "display") 1440 else height,
    )
}

fun photoThumbnailImageRequest(
    context: Context,
    photo: PhotoEntity,
): ImageRequest {
    val uri = photoContentUri(photo)
    return localThumbnailImageRequest(
        context = context,
        uri = uri,
        versionKey = photoCacheKey(photo, PHOTO_THUMB_CACHE_SUFFIX),
    )
}

fun trashImageRequest(
    context: Context,
    item: TrashItemEntity,
    cacheSuffix: String,
    width: Int,
    height: Int,
): ImageRequest {
    val uri = trashContentUri(item)
    if (normalizedTrashCacheSuffix(cacheSuffix) == TRASH_THUMB_CACHE_SUFFIX) return trashThumbnailImageRequest(context, item)
    val cacheKey = trashCacheKey(item, normalizedTrashCacheSuffix(cacheSuffix))
    return mediaImageRequest(
        context = context,
        uri = uri,
        cacheKey = cacheKey,
        placeholderCacheKey = trashThumbnailCacheKey(item),
        width = width,
        height = height,
    )
}

fun trashThumbnailImageRequest(
    context: Context,
    item: TrashItemEntity,
): ImageRequest {
    val uri = trashContentUri(item)
    return localThumbnailImageRequest(
        context = context,
        uri = uri,
        versionKey = trashCacheKey(item, TRASH_THUMB_CACHE_SUFFIX),
    )
}

fun mediaImageRequest(
    context: Context,
    uri: Uri,
    cacheKey: String,
    placeholderCacheKey: String = cacheKey,
    width: Int,
    height: Int,
): ImageRequest = ImageRequest.Builder(context)
    .data(uri)
    .memoryCacheKey(cacheKey)
    .diskCacheKey(cacheKey)
    .placeholderMemoryCacheKey(placeholderCacheKey)
    .size(width, height)
    .precision(Precision.EXACT)
    .scale(Scale.FIT)
    .crossfade(false)
    .build()

/**
 * Prefetch one image without synchronously repeating a memory-cache hit.
 * The suspend call remains cancellable when the hosting Compose effect leaves.
 */
suspend fun ImageLoader.executeIfNotCached(request: ImageRequest): Boolean {
    val key = request.memoryCacheKey
    if (key != null && memoryCache?.get(MemoryCache.Key(key)) != null) return false
    val result = execute(request)
    if (result is SuccessResult) withContext(Dispatchers.IO) {
        (result.image as? BitmapImage)?.bitmap?.prepareToDraw()
    }
    return true
}

private fun photoThumbnailCacheKey(photo: PhotoEntity): String =
    LocalMediaThumbnail(photoContentUri(photo), photoCacheKey(photo, PHOTO_THUMB_CACHE_SUFFIX)).diskKey

private fun trashThumbnailCacheKey(item: TrashItemEntity): String =
    LocalMediaThumbnail(trashContentUri(item), trashCacheKey(item, TRASH_THUMB_CACHE_SUFFIX)).diskKey

private fun photoCacheKey(photo: PhotoEntity, suffix: String): String =
    "photo:" + photo.uri + ":" + photo.mediaStoreId + ":" + photo.dateModified + ":" + photo.size +
        ":" + photo.width + "x" + photo.height + ":" + photo.exifOrientation + ":" + suffix

private fun trashCacheKey(item: TrashItemEntity, suffix: String): String =
    "trash:" + item.mediaType + ":" + item.uri + ":" + item.mediaStoreId + ":" + item.trashedAt +
        ":" + item.size + ":" + suffix

internal fun normalizedPhotoCacheSuffix(cacheSuffix: String, isGif: Boolean = false): String = when (cacheSuffix) {
    "favorite_photo_tile", "photo_grid", "day_memory_grid" -> PHOTO_THUMB_CACHE_SUFFIX
    "photo_fit", "clean_prefetch" -> "display"
    "clean_background" -> if (isGif) "gif_background" else "display"
    "viewer_photo", "favorite_preview" -> "preview"
    else -> cacheSuffix
}

private fun originalContentUri(value: String): Uri? = Uri.parse(value)
    .takeIf { it.scheme == "content" && !it.authority.isNullOrBlank() }

private fun localThumbnailImageRequest(context: Context, uri: Uri, versionKey: String): ImageRequest {
    val model = LocalMediaThumbnail(uri, versionKey, PHOTO_THUMB_WIDTH, PHOTO_THUMB_HEIGHT)
    return ImageRequest.Builder(context).data(model)
        .memoryCacheKey(model.diskKey).diskCacheKey(model.diskKey).placeholderMemoryCacheKey(model.diskKey)
        .size(model.width, model.height).precision(Precision.INEXACT).crossfade(false).build()
}

private fun normalizedTrashCacheSuffix(cacheSuffix: String): String = when (cacheSuffix) {
    "trash_tile" -> TRASH_THUMB_CACHE_SUFFIX
    else -> cacheSuffix
}
