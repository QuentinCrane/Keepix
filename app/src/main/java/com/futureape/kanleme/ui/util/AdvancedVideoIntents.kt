package com.futureape.kanleme.ui.util

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import android.widget.Toast
import com.futureape.kanleme.R
import com.futureape.kanleme.data.media.AdvancedVideo

internal fun advancedVideoViewIntent(video: AdvancedVideo): Intent {
    val uri = video.uri.toUri()
    return Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, video.mimeType.takeIf { it.startsWith("video/") } ?: "video/*")
        clipData = ClipData.newRawUri(video.name, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
}

fun openAdvancedVideoExternally(context: Context, video: AdvancedVideo) {
    val intent = advancedVideoViewIntent(video)
    try {
        if (context.packageManager.queryIntentActivities(intent, 0).isEmpty()) {
            Toast.makeText(context, R.string.advanced_external_missing, Toast.LENGTH_LONG).show()
            return
        }
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.advanced_external)))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, R.string.advanced_external_missing, Toast.LENGTH_LONG).show()
    } catch (_: SecurityException) {
        Toast.makeText(context, R.string.advanced_external_error, Toast.LENGTH_LONG).show()
    } catch (_: IllegalArgumentException) {
        Toast.makeText(context, R.string.advanced_external_error, Toast.LENGTH_LONG).show()
    }
}
