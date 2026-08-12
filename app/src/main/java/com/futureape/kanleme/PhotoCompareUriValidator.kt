package com.futureape.kanleme

import android.content.ContentResolver
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.IntentCompat

internal object PhotoCompareUriValidator {
    const val MAX_COMPARE_PHOTOS = 4

    private const val MAX_ENCODED_BYTES = 64L * 1024L * 1024L
    private const val MAX_IMAGE_DIMENSION = 12_000
    private const val MAX_IMAGE_PIXELS = 40_000_000L

    fun collectCandidateUris(intent: Intent?): List<Uri> {
        if (intent == null) return emptyList()
        val extraUris = when (intent.action) {
            Intent.ACTION_SEND -> runCatching {
                listOfNotNull(IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java))
            }.getOrDefault(emptyList())

            Intent.ACTION_SEND_MULTIPLE -> runCatching {
                IntentCompat.getParcelableArrayListExtra(intent, Intent.EXTRA_STREAM, Uri::class.java).orEmpty()
            }.getOrDefault(emptyList())

            else -> emptyList()
        }
        val clipUris = buildList {
            val clipData = intent.clipData ?: return@buildList
            repeat(clipData.itemCount) { index ->
                clipData.getItemAt(index).uri?.let(::add)
            }
        }
        return (extraUris + clipUris).distinct()
    }

    fun validate(resolver: ContentResolver, candidates: List<Uri>): List<Uri> {
        if (candidates.isEmpty() || candidates.size > MAX_COMPARE_PHOTOS) return emptyList()
        return candidates.filter { isSafeImageUri(resolver, it) }
    }

    fun isContentUri(uri: Uri): Boolean =
        uri.scheme.equals(ContentResolver.SCHEME_CONTENT, ignoreCase = true) &&
            !uri.authority.isNullOrBlank()

    private fun isSafeImageUri(resolver: ContentResolver, uri: Uri): Boolean {
        if (!isContentUri(uri)) return false
        val providerType = runCatching { resolver.getType(uri) }.getOrNull()
        if (providerType != null && !providerType.startsWith("image/", ignoreCase = true)) return false

        return runCatching {
            val encodedBytes = resolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: -1L
            if (encodedBytes > MAX_ENCODED_BYTES) return@runCatching false

            resolver.openInputStream(uri)?.use { input ->
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeStream(input, null, options)
                val width = options.outWidth
                val height = options.outHeight
                width > 0 &&
                    height > 0 &&
                    width <= MAX_IMAGE_DIMENSION &&
                    height <= MAX_IMAGE_DIMENSION &&
                    width.toLong() * height.toLong() <= MAX_IMAGE_PIXELS &&
                    (providerType != null || options.outMimeType?.startsWith("image/") == true)
            } ?: false
        }.getOrDefault(false)
    }
}
