package com.futureape.kanleme.ui.util

import android.net.Uri
import android.provider.DocumentsContract
import com.futureape.kanleme.data.media.PhotoFolderKey
import com.futureape.kanleme.data.media.photoFolderFromDocumentId

fun advancedPhotoFolderFromTree(uri: Uri): PhotoFolderKey? {
    if (uri.scheme != "content" || uri.authority != "com.android.externalstorage.documents" ||
        !DocumentsContract.isTreeUri(uri)) return null
    return runCatching { photoFolderFromDocumentId(DocumentsContract.getTreeDocumentId(uri)) }.getOrNull()
}

fun advancedPhotoInitialFolder(folder: PhotoFolderKey? = null): Uri {
    val volume = folder?.volume?.takeUnless { it == "external_primary" } ?: "primary"
    val parent = folder?.relativePath?.trimEnd('/')?.substringBeforeLast('/', "Pictures") ?: "Pictures"
    return DocumentsContract.buildDocumentUri("com.android.externalstorage.documents", volume + ":" + parent)
}
