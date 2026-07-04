package com.futureape.kanleme.ui.util

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import coil.request.ImageRequest
import coil.size.Precision
import com.futureape.kanleme.data.local.PhotoEntity
import com.futureape.kanleme.data.local.TrashItemEntity
import com.futureape.kanleme.data.local.VideoEntity

private const val PHOTO_THUMB_CACHE_SUFFIX = "thumb"
private const val TRASH_THUMB_CACHE_SUFFIX = "thumb"
private const val PHOTO_THUMB_WIDTH = 420
private const val PHOTO_THUMB_HEIGHT = 560
private const val TRASH_THUMB_WIDTH = 420
private const val TRASH_THUMB_HEIGHT = 560

fun photoContentUri(photo: PhotoEntity): Uri =
    if (photo.mediaStoreId > 0L) {
        ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, photo.mediaStoreId)
    } else {
        Uri.parse(photo.uri)
    }

fun videoContentUri(video: VideoEntity): Uri =
    if (video.mediaStoreId > 0L) {
        ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, video.mediaStoreId)
    } else {
        Uri.parse(video.uri)
    }

fun trashContentUri(item: TrashItemEntity): Uri =
    if (item.mediaStoreId > 0L) {
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
    val cacheKey = photoCacheKey(photo, normalizedPhotoCacheSuffix(cacheSuffix))
    return mediaImageRequest(
        context = context,
        uri = uri,
        cacheKey = cacheKey,
        placeholderCacheKey = photoThumbnailCacheKey(photo),
        width = width,
        height = height,
    )
}

fun photoThumbnailImageRequest(
    context: Context,
    photo: PhotoEntity,
): ImageRequest {
    val uri = photoContentUri(photo)
    return mediaImageRequest(
        context = context,
        uri = uri,
        cacheKey = photoThumbnailCacheKey(photo),
        placeholderCacheKey = photoThumbnailCacheKey(photo),
        width = PHOTO_THUMB_WIDTH,
        height = PHOTO_THUMB_HEIGHT,
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
    return mediaImageRequest(
        context = context,
        uri = uri,
        cacheKey = trashThumbnailCacheKey(item),
        placeholderCacheKey = trashThumbnailCacheKey(item),
        width = TRASH_THUMB_WIDTH,
        height = TRASH_THUMB_HEIGHT,
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
    .precision(Precision.INEXACT)
    .crossfade(false)
    .build()

private fun photoThumbnailCacheKey(photo: PhotoEntity): String =
    photoCacheKey(photo, PHOTO_THUMB_CACHE_SUFFIX)

private fun trashThumbnailCacheKey(item: TrashItemEntity): String =
    trashCacheKey(item, TRASH_THUMB_CACHE_SUFFIX)

private fun photoCacheKey(photo: PhotoEntity, suffix: String): String =
    "photo:" + photo.mediaStoreId + ":" + photo.updatedAt + ":" + suffix

private fun trashCacheKey(item: TrashItemEntity, suffix: String): String =
    "trash:" + item.mediaType + ":" + item.mediaStoreId + ":" + item.trashedAt + ":" + suffix

private fun normalizedPhotoCacheSuffix(cacheSuffix: String): String = when (cacheSuffix) {
    "favorite_photo_tile", "photo_grid", "day_memory_grid" -> PHOTO_THUMB_CACHE_SUFFIX
    else -> cacheSuffix
}

private fun normalizedTrashCacheSuffix(cacheSuffix: String): String = when (cacheSuffix) {
    "trash_tile" -> TRASH_THUMB_CACHE_SUFFIX
    else -> cacheSuffix
}
