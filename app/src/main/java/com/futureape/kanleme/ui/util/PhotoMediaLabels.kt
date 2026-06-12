package com.futureape.kanleme.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.futureape.kanleme.data.local.PhotoEntity
import com.futureape.kanleme.R
import java.util.Locale

@Composable
fun photoMediaKindLabel(photo: PhotoEntity): String = when {
    photo.isGif -> stringResource(R.string.media_kind_gif)
    photo.isMotionPhoto && photo.isSeparateVideo -> stringResource(R.string.media_kind_motion_separate)
    photo.isMotionPhoto -> stringResource(R.string.media_kind_motion)
    photo.isRaw -> "RAW"
    photo.isLongImage -> stringResource(R.string.media_kind_long_image)
    else -> photo.mimeType.substringAfter('/', stringResource(R.string.media_kind_fallback_image)).uppercase(Locale.ROOT)
}

@Composable
fun photoMediaBadges(photo: PhotoEntity): List<String> = buildList {
    if (photo.isGif) add("GIF")
    if (photo.isMotionPhoto) add(if (photo.isSeparateVideo) stringResource(R.string.media_badge_motion_video) else stringResource(R.string.media_badge_motion))
    if (photo.isRaw) add("RAW")
    if (photo.isLongImage && isEmpty()) add(stringResource(R.string.media_kind_long_image))
}
