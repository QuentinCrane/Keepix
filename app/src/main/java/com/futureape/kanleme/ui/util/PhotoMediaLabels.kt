package com.futureape.kanleme.ui.util

import com.futureape.kanleme.data.local.PhotoEntity
import java.util.Locale

fun photoMediaKindLabel(photo: PhotoEntity): String = when {
    photo.isGif -> "GIF 动图"
    photo.isMotionPhoto && photo.isSeparateVideo -> "实况照片 · 独立视频段"
    photo.isMotionPhoto -> "实况照片"
    photo.isRaw -> "RAW"
    photo.isLongImage -> "长图"
    else -> photo.mimeType.substringAfter('/', "图片").uppercase(Locale.ROOT)
}

fun photoMediaBadges(photo: PhotoEntity): List<String> = buildList {
    if (photo.isGif) add("GIF")
    if (photo.isMotionPhoto) add(if (photo.isSeparateVideo) "实况+视频" else "实况")
    if (photo.isRaw) add("RAW")
    if (photo.isLongImage && isEmpty()) add("长图")
}
