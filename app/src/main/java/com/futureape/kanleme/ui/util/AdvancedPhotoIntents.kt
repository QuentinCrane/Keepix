package com.futureape.kanleme.ui.util

import android.content.Context
import android.net.Uri
import com.futureape.kanleme.data.media.AdvancedPhoto

fun openAdvancedPhotoExternally(context: Context, photo: AdvancedPhoto) {
    openMediaInSystemGallery(
        context = context,
        uri = Uri.parse(photo.uri),
        mimeType = photo.mimeType.ifBlank { "image/*" },
        title = photo.name,
    )
}
