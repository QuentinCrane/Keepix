package com.futureape.kanleme.ui.util

import com.futureape.kanleme.R
import android.content.ActivityNotFoundException
import android.content.ContentUris
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.futureape.kanleme.data.local.PhotoEntity
import com.futureape.kanleme.data.local.VideoEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

suspend fun shareDailyReportImage(
    context: Context,
    todayPhotos: Int,
    todayVideos: Int,
    todayActions: Int,
) {
    val title = "今日整理日报"
    val uri = withContext(Dispatchers.IO) {
        val bitmap = createDailyReportBitmap(todayPhotos, todayVideos, todayActions)
        val reportsDir = File(context.cacheDir, "daily-reports").apply { mkdirs() }
        val file = File(reportsDir, "kanleme-daily-report.png")
        file.outputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        }
        bitmap.recycle()
        FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
    }
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TITLE, title)
        clipData = ClipData.newUri(context.contentResolver, title, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    val chooser = Intent.createChooser(sendIntent, context.getString(R.string.share_chooser_title, title))
    context.startActivity(chooser)
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

private fun createDailyReportBitmap(todayPhotos: Int, todayVideos: Int, todayActions: Int): Bitmap {
    val width = 1080
    val height = 1440
    val total = todayPhotos + todayVideos
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val primary = Color.rgb(32, 124, 224)
    val primarySoft = Color.rgb(224, 239, 255)
    val ink = Color.rgb(21, 27, 36)
    val muted = Color.rgb(104, 116, 132)
    val line = Color.rgb(226, 232, 240)
    val paper = Color.rgb(251, 253, 255)

    canvas.drawColor(Color.rgb(238, 244, 251))
    paint.color = paper
    canvas.drawRoundRect(RectF(54f, 54f, width - 54f, height - 54f), 64f, 64f, paint)

    paint.color = primarySoft
    canvas.drawRoundRect(RectF(96f, 100f, width - 96f, 424f), 46f, 46f, paint)
    paint.color = Color.WHITE
    canvas.drawRoundRect(RectF(118f, 122f, width - 118f, 402f), 38f, 38f, paint)

    paint.textAlign = Paint.Align.LEFT
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    paint.color = primary
    paint.textSize = 42f
    canvas.drawText("看了么", 154f, 192f, paint)

    paint.color = ink
    paint.textSize = 76f
    canvas.drawText("今日整理", 154f, 286f, paint)

    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    paint.color = muted
    paint.textSize = 32f
    canvas.drawText(SimpleDateFormat("M月d日 HH:mm", Locale.getDefault()).format(Date()), 154f, 344f, paint)

    paint.textAlign = Paint.Align.CENTER
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    paint.color = primary
    paint.textSize = 152f
    canvas.drawText(formatReportCount(total), width / 2f, 620f, paint)

    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    paint.color = muted
    paint.textSize = 36f
    val summary = if (total > 0) {
        "今天已处理 " + total + " 个媒体文件"
    } else {
        "今天还没有开始整理"
    }
    canvas.drawText(summary, width / 2f, 684f, paint)

    drawReportMetric(canvas, 120f, 770f, "照片", todayPhotos, primary, ink, muted, line, paint)
    drawReportMetric(canvas, 390f, 770f, "视频", todayVideos, primary, ink, muted, line, paint)
    drawReportMetric(canvas, 660f, 770f, "动作", todayActions, primary, ink, muted, line, paint)

    paint.color = line
    canvas.drawRoundRect(RectF(132f, 1064f, width - 132f, 1070f), 3f, 3f, paint)

    paint.color = ink
    paint.textSize = 42f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    canvas.drawText(if (total > 0) "小步整理，也是在腾出生活空间" else "开一小局，从几张照片开始", width / 2f, 1160f, paint)

    paint.color = muted
    paint.textSize = 30f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    canvas.drawText("本地统计 · 不上传照片或视频", width / 2f, 1240f, paint)

    paint.color = primary
    paint.textSize = 34f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    canvas.drawText("kanleme", width / 2f, 1322f, paint)
    return bitmap
}

private fun drawReportMetric(
    canvas: Canvas,
    left: Float,
    top: Float,
    label: String,
    value: Int,
    primary: Int,
    ink: Int,
    muted: Int,
    line: Int,
    paint: Paint,
) {
    paint.color = Color.WHITE
    canvas.drawRoundRect(RectF(left, top, left + 240f, top + 204f), 34f, 34f, paint)
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 3f
    paint.color = line
    canvas.drawRoundRect(RectF(left, top, left + 240f, top + 204f), 34f, 34f, paint)
    paint.style = Paint.Style.FILL

    paint.textAlign = Paint.Align.CENTER
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    paint.color = ink
    paint.textSize = 58f
    canvas.drawText(formatReportCount(value), left + 120f, top + 92f, paint)

    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    paint.color = muted
    paint.textSize = 30f
    canvas.drawText(label, left + 120f, top + 146f, paint)

    paint.color = primary
    canvas.drawRoundRect(RectF(left + 88f, top + 166f, left + 152f, top + 174f), 4f, 4f, paint)
}

private fun formatReportCount(value: Int): String =
    if (value >= 1000) "%.1fK".format(Locale.US, value / 1000.0).replace(".0", "") else value.toString()
