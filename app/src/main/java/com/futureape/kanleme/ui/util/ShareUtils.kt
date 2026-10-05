package com.futureape.kanleme.ui.util

import com.futureape.kanleme.R
import android.content.ActivityNotFoundException
import android.app.Activity
import android.content.ContentUris
import android.content.ClipData
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast
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
    val title = context.getString(R.string.daily_report_share_title)
    val uri = withContext(Dispatchers.IO) {
        val bitmap = createDailyReportBitmap(context, todayPhotos, todayVideos, todayActions)
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
    // The stored URI identifies the selected original, including its storage volume.
    // A motion photo's companion video is not a substitute for that photo.
    val uri = galleryMediaUri(photo.uri, photo.mediaStoreId, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
    if (uri == null) {
        showGalleryOpenError(context)
        return
    }
    openMediaInSystemGallery(
        context = context,
        uri = uri,
        mimeType = photo.mimeType.ifBlank { "image/*" },
        title = photo.displayName,
    )
}

fun openVideoInSystemGallery(context: Context, video: VideoEntity) {
    val uri = galleryMediaUri(video.uri, video.mediaStoreId, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
    if (uri == null) {
        showGalleryOpenError(context)
        return
    }
    openMediaInSystemGallery(
        context = context,
        uri = uri,
        mimeType = video.mimeType.ifBlank { "video/*" },
        title = video.displayName,
    )
}

private fun galleryMediaUri(storedUri: String, mediaStoreId: Long, collection: Uri): Uri? {
    val parsed = Uri.parse(storedUri)
    if (parsed.scheme == "content" && !parsed.authority.isNullOrBlank()) return parsed
    return mediaStoreId.takeIf { it > 0 }?.let { ContentUris.withAppendedId(collection, it) }
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
        clipData = ClipData.newRawUri(title, targetUri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        if (!context.hasActivity()) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    fun viewIntent(targetUri: Uri, type: String?): Intent = Intent(Intent.ACTION_VIEW).apply {
        if (type == null) data = targetUri else setDataAndType(targetUri, type)
    }.withMediaGrant(targetUri)

    val candidates = listOf(uri, fallbackUri).distinct().flatMap { target ->
        listOf(
            Intent(MediaStore.ACTION_REVIEW).apply { setDataAndType(target, resolvedType) }.withMediaGrant(target),
            viewIntent(target, resolvedType),
            viewIntent(target, broadType),
            Intent("com.android.camera.action.REVIEW").apply { setDataAndType(target, broadType) }.withMediaGrant(target),
        ).distinctBy { it.action to it.type }
    }

    // Try each supported protocol on the same original. Do not open the gallery's
    // home page or create a cache copy: either would lose the selected media item.
    for (candidate in candidates) {
        try {
            context.startActivity(candidate)
            return
        } catch (_: ActivityNotFoundException) {
            // The gallery may support VIEW but not REVIEW.
        } catch (_: SecurityException) {
            // Try another gallery protocol with the same read grant.
        } catch (_: IllegalArgumentException) {
            // Some vendor viewers accept only a broader MIME type.
        }
    }
    showGalleryOpenError(context)
}

private fun Context.hasActivity(): Boolean = when (this) {
    is Activity -> true
    is ContextWrapper -> baseContext !== this && baseContext.hasActivity()
    else -> false
}

private fun showGalleryOpenError(context: Context) {
    Toast.makeText(context, R.string.gallery_open_failed, Toast.LENGTH_LONG).show()
}

private fun createDailyReportBitmap(
    context: Context,
    todayPhotos: Int,
    todayVideos: Int,
    todayActions: Int,
): Bitmap {
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
    canvas.drawText(context.getString(R.string.app_name), 154f, 192f, paint)

    paint.color = ink
    paint.textSize = 76f
    canvas.drawText(context.getString(R.string.daily_report_heading), 154f, 286f, paint)

    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    paint.color = muted
    paint.textSize = 32f
    canvas.drawText(
        SimpleDateFormat(context.getString(R.string.daily_report_date_pattern), Locale.getDefault()).format(Date()),
        154f,
        344f,
        paint,
    )

    paint.textAlign = Paint.Align.CENTER
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    paint.color = primary
    paint.textSize = 152f
    canvas.drawText(formatReportCount(total), width / 2f, 620f, paint)

    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    paint.color = muted
    paint.textSize = 36f
    val summary = if (total > 0) {
        context.getString(R.string.daily_report_processed, total)
    } else {
        context.getString(R.string.daily_report_not_started)
    }
    canvas.drawText(summary, width / 2f, 684f, paint)

    drawReportMetric(canvas, 120f, 770f, context.getString(R.string.daily_report_photo), todayPhotos, primary, ink, muted, line, paint)
    drawReportMetric(canvas, 390f, 770f, context.getString(R.string.daily_report_video), todayVideos, primary, ink, muted, line, paint)
    drawReportMetric(canvas, 660f, 770f, context.getString(R.string.daily_report_action), todayActions, primary, ink, muted, line, paint)

    paint.color = line
    canvas.drawRoundRect(RectF(132f, 1064f, width - 132f, 1070f), 3f, 3f, paint)

    paint.color = ink
    paint.textSize = 42f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    canvas.drawText(
        context.getString(if (total > 0) R.string.daily_report_motto_active else R.string.daily_report_motto_empty),
        width / 2f,
        1160f,
        paint,
    )

    paint.color = muted
    paint.textSize = 30f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    canvas.drawText(context.getString(R.string.daily_report_local_only), width / 2f, 1240f, paint)

    paint.color = primary
    paint.textSize = 34f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    canvas.drawText(context.getString(R.string.app_name), width / 2f, 1322f, paint)
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
