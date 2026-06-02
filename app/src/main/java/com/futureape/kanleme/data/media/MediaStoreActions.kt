package com.futureape.kanleme.data.media

import android.content.ContentResolver
import android.content.ContentValues
import android.net.Uri
import android.provider.MediaStore
import javax.inject.Inject
import javax.inject.Singleton

sealed interface MediaOperationResult {
    data object Success : MediaOperationResult
    data class Failed(val reason: String) : MediaOperationResult
}

@Singleton
class MediaStoreActions @Inject constructor(
    private val resolver: ContentResolver,
) {
    fun moveImageToFolder(mediaUri: String, targetRelativePath: String): MediaOperationResult {
        val normalizedPath = normalizeRelativePath(targetRelativePath)
        val uri = runCatching { Uri.parse(mediaUri) }.getOrNull()
            ?: return MediaOperationResult.Failed("媒体地址无效")

        return try {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.RELATIVE_PATH, normalizedPath)
            }
            val rows = resolver.update(uri, values, null, null)
            if (rows > 0) {
                MediaOperationResult.Success
            } else {
                MediaOperationResult.Failed("系统没有移动该媒体，可能需要先授予完整照片权限")
            }
        } catch (security: SecurityException) {
            MediaOperationResult.Failed("系统要求单独授权修改这张照片。当前版本已保留接口位置，后续可接入 MediaStore 写入授权弹窗")
        } catch (throwable: Throwable) {
            MediaOperationResult.Failed(throwable.message ?: "移动文件夹失败")
        }
    }

    fun moveMediaToSystemTrash(mediaUri: String): MediaOperationResult {
        val uri = runCatching { Uri.parse(mediaUri) }.getOrNull()
            ?: return MediaOperationResult.Failed("媒体地址无效")
        return try {
            val rows = resolver.update(
                uri,
                ContentValues().apply { put(MediaStore.MediaColumns.IS_TRASHED, 1) },
                null,
                null,
            )
            if (rows > 0) {
                MediaOperationResult.Success
            } else {
                MediaOperationResult.Failed("系统没有移动到回收站，可能需要针对该媒体单独授权")
            }
        } catch (security: SecurityException) {
            MediaOperationResult.Failed("系统要求单独授权修改该媒体。请先使用待删确认模式，或后续接入 MediaStore 写入授权弹窗")
        } catch (throwable: Throwable) {
            MediaOperationResult.Failed(throwable.message ?: "移动到系统回收站失败")
        }
    }

    fun restoreFromSystemTrash(mediaUri: String): MediaOperationResult {
        val uri = runCatching { Uri.parse(mediaUri) }.getOrNull()
            ?: return MediaOperationResult.Failed("媒体地址无效")
        return try {
            val rows = resolver.update(
                uri,
                ContentValues().apply { put(MediaStore.MediaColumns.IS_TRASHED, 0) },
                null,
                null,
            )
            if (rows > 0) MediaOperationResult.Success else MediaOperationResult.Failed("系统没有恢复该媒体，可能需要单独授权")
        } catch (security: SecurityException) {
            MediaOperationResult.Failed("系统要求单独授权恢复该媒体")
        } catch (throwable: Throwable) {
            MediaOperationResult.Failed(throwable.message ?: "恢复媒体失败")
        }
    }

    fun permanentlyDeleteMedia(mediaUri: String): MediaOperationResult {
        val uri = runCatching { Uri.parse(mediaUri) }.getOrNull()
            ?: return MediaOperationResult.Failed("媒体地址无效")
        return try {
            val rows = resolver.delete(uri, null, null)
            if (rows > 0) MediaOperationResult.Success else MediaOperationResult.Failed("系统没有永久删除该媒体，可能需要单独授权")
        } catch (security: SecurityException) {
            MediaOperationResult.Failed("系统要求单独授权永久删除该媒体")
        } catch (throwable: Throwable) {
            MediaOperationResult.Failed(throwable.message ?: "永久删除失败")
        }
    }

    companion object {
        fun normalizeRelativePath(path: String): String {
            val clean = path.trim().replace('\\', '/').trim('/')
            return when {
                clean.isBlank() -> "Pictures/回留/"
                clean.endsWith("/") -> clean
                else -> "$clean/"
            }
        }

        fun folderNameOf(relativePath: String): String =
            normalizeRelativePath(relativePath).trimEnd('/').substringAfterLast('/').ifBlank { "回留" }
    }
}
