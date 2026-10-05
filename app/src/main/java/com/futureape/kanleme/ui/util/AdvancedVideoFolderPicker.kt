package com.futureape.kanleme.ui.util

import android.net.Uri
import android.provider.DocumentsContract
import com.futureape.kanleme.data.media.VideoFolderKey
import com.futureape.kanleme.data.media.videoFolderFromDocumentId

fun advancedVideoFolderFromTree(uri: Uri): VideoFolderKey? {
    if (uri.scheme != "content" || uri.authority != "com.android.externalstorage.documents" ||
        !DocumentsContract.isTreeUri(uri)) return null
    return runCatching { videoFolderFromDocumentId(DocumentsContract.getTreeDocumentId(uri)) }.getOrNull()
}

fun advancedVideoInitialFolder(folder: VideoFolderKey? = null): Uri {
    val volume = folder?.volume?.takeUnless { it == "external_primary" } ?: "primary"
    val parent = folder?.relativePath?.trimEnd('/')?.substringBeforeLast('/', "Movies") ?: "Movies"
    return DocumentsContract.buildDocumentUri("com.android.externalstorage.documents", volume + ":" + parent)
}
