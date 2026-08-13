package com.futureape.kanleme.data.media

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/** Reads the current system media grant without persisting or inferring it. */
object MediaAccessSnapshot {
    fun current(context: Context): MediaAccessState {
        fun granted(permission: String): Boolean =
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

        return when {
            Build.VERSION.SDK_INT >= 34 -> MediaAccessState(
                canReadImages = granted(Manifest.permission.READ_MEDIA_IMAGES),
                canReadVideos = granted(Manifest.permission.READ_MEDIA_VIDEO),
                canReadSelectedVisualMedia = granted(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED),
            )
            Build.VERSION.SDK_INT >= 33 -> MediaAccessState(
                canReadImages = granted(Manifest.permission.READ_MEDIA_IMAGES),
                canReadVideos = granted(Manifest.permission.READ_MEDIA_VIDEO),
                canReadSelectedVisualMedia = false,
            )
            else -> {
                val canRead = granted(Manifest.permission.READ_EXTERNAL_STORAGE)
                MediaAccessState(canReadImages = canRead, canReadVideos = canRead, canReadSelectedVisualMedia = false)
            }
        }
    }
}
