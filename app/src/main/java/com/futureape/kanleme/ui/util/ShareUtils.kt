package com.futureape.kanleme.ui.util

import com.futureape.kanleme.R
import android.content.ActivityNotFoundException
import android.content.ContentUris
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import com.futureape.kanleme.data.local.PhotoEntity
import com.futureape.kanleme.data.local.VideoEntity

fun shareMedia(context: Context, uri: Uri, mimeType: String, title: String) {
    val resolvedType = mimeType.ifBlank { "*/*" }
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = resolvedType
        putExtra(Intent.EXTRA_STREAM, uri)
        clipData = ClipData.newUri(context.contentResolver, title, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    val chooser = Intent.createChooser(sendIntent, context.getString(R.string.share_chooser_title, title))
    context.startActivity(chooser)
}

fun sharePhoto(context: Context, photo: PhotoEntity) {
    shareMedia(context, Uri.parse(photo.uri), photo.mimeType.ifBlank { "image/*" }, photo.displayName)
}

fun shareVideo(context: Context, video: VideoEntity) {
    shareMedia(context, Uri.parse(video.uri), video.mimeType.ifBlank { "video/*" }, video.displayName)
}

fun openPhotoInSystemGallery(context: Context, photo: PhotoEntity) {
    val mediaStoreUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, photo.mediaStoreId)
    val storedUri = runCatching { Uri.parse(photo.uri) }.getOrElse { mediaStoreUri }
    val companionVideoUri = photo.motionVideoUri
        ?.takeIf { photo.isSeparateVideo && it.isNotBlank() }
        ?.let { runCatching { Uri.parse(it) }.getOrNull() }
    if (companionVideoUri != null) {
        openMediaInSystemGallery(
            context = context,
            uri = companionVideoUri,
            mimeType = "video/*",
            title = context.getString(R.string.motion_clip_title, photo.displayName),
            fallbackUri = mediaStoreUri,
        )
        return
    }
    openMediaInSystemGallery(
        context = context,
        uri = mediaStoreUri,
        mimeType = photo.mimeType.ifBlank { "image/*" },
        title = photo.displayName,
        fallbackUri = storedUri,
    )
}

fun openVideoInSystemGallery(context: Context, video: VideoEntity) {
    val mediaStoreUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, video.mediaStoreId)
    val storedUri = runCatching { Uri.parse(video.uri) }.getOrElse { mediaStoreUri }
    openMediaInSystemGallery(
        context = context,
        uri = mediaStoreUri,
        mimeType = video.mimeType.ifBlank { "video/*" },
        title = video.displayName,
        fallbackUri = storedUri,
    )
}

fun openMediaInSystemGallery(
    context: Context,
    uri: Uri,
    mimeType: String,
    title: String = context.getString(R.string.media_file_title),
    fallbackUri: Uri = uri,
) {
    val resolvedType = when {
        mimeType.isNotBlank() -> mimeType
        uri.toString().contains("video", ignoreCase = true) -> "video/*"
        else -> "image/*"
    }
    val broadType = when {
        resolvedType.startsWith("video/") -> "video/*"
        resolvedType.startsWith("image/") -> "image/*"
        else -> "*/*"
    }

    fun Intent.withMediaGrant(targetUri: Uri): Intent = apply {
        putExtra(Intent.EXTRA_STREAM, targetUri)
        putExtra(Intent.EXTRA_TITLE, title)
        putExtra("android.intent.extra.STREAM", targetUri)
        clipData = ClipData.newUri(context.contentResolver, title, targetUri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    fun viewIntent(targetUri: Uri, type: String?): Intent = Intent(Intent.ACTION_VIEW).apply {
        if (type == null) data = targetUri else setDataAndType(targetUri, type)
    }.withMediaGrant(targetUri)

    val reviewAction = "com.android.camera.action.REVIEW"
    val candidates = listOf(
        viewIntent(uri, broadType),
        viewIntent(uri, resolvedType),
        Intent(reviewAction).apply { setDataAndType(uri, broadType) }.withMediaGrant(uri),
        viewIntent(uri, null),
        viewIntent(fallbackUri, broadType),
        viewIntent(fallbackUri, resolvedType),
        viewIntent(fallbackUri, null),
    )

    val resolver = context.packageManager
    val firstResolvable = candidates.firstOrNull { it.resolveActivity(resolver) != null }
    if (firstResolvable != null) {
        try {
            context.startActivity(firstResolvable)
            return
        } catch (_: ActivityNotFoundException) {
            // Fall through to chooser.
        } catch (_: SecurityException) {
            // Fall through to chooser.
        } catch (_: IllegalArgumentException) {
            // Some vendor gallery apps reject otherwise-valid MediaStore Uris.
        }
    }

    val chooser = Intent.createChooser(candidates.first(), context.getString(R.string.gallery_chooser_title)).apply {
        putExtra(Intent.EXTRA_INITIAL_INTENTS, candidates.drop(1).toTypedArray())
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        clipData = ClipData.newUri(context.contentResolver, title, uri)
    }
    try {
        context.startActivity(chooser)
    } catch (_: Exception) {
        // Last resort: let Android route the broadest VIEW intent.
        context.startActivity(viewIntent(uri, broadType))
    }
}
