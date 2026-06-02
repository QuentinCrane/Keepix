package com.futureape.kanleme.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.futureape.kanleme.ui.components.GlassSurface

/**
 * Android 13+ uses granular media permissions. Android 14+ can return either full
 * access or partial Selected Photos Access. This gate never persists permission
 * state; it reads the real system permission every time the composable enters.
 */
data class MediaAccessState(
    val canReadImages: Boolean,
    val canReadVideos: Boolean,
    val canReadSelectedVisualMedia: Boolean,
) {
    val isGranted: Boolean get() = canReadImages || canReadVideos || canReadSelectedVisualMedia
    val isFullVisualAccess: Boolean get() = canReadImages && canReadVideos
    val isPartialVisualAccess: Boolean get() = !isFullVisualAccess && canReadSelectedVisualMedia
    val key: String get() = "images=$canReadImages;videos=$canReadVideos;selected=$canReadSelectedVisualMedia"
    val label: String get() = when {
        isFullVisualAccess -> "完整相册访问"
        isPartialVisualAccess -> "部分照片访问"
        canReadImages -> "仅照片访问"
        canReadVideos -> "仅视频访问"
        else -> "未授权"
    }
}

@Composable
fun MediaPermissionGate(
    onPermissionReady: (accessKey: String, accessLabel: String) -> Unit,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val permissions = remember { requiredMediaPermissions() }
    var access by remember { mutableStateOf(currentMediaAccessState(context)) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        access = currentMediaAccessState(context)
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        access = currentMediaAccessState(context)
    }

    LaunchedEffect(access.key) {
        if (access.isGranted) {
            onPermissionReady(access.key, access.label)
        }
    }

    if (access.isGranted || permissions.isEmpty()) {
        Box(Modifier.fillMaxSize()) {
            content()
            if (access.isPartialVisualAccess) {
                PartialAccessBanner(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 12.dp, start = 16.dp, end = 16.dp),
                    onRequestFullAccess = { launcher.launch(permissions) },
                )
            }
        }
    } else {
        Box(Modifier.fillMaxSize().padding(18.dp), contentAlignment = Alignment.Center) {
            GlassSurface {
                Column(
                    Modifier.padding(26.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalAlignment = Alignment.Start,
                ) {
                    Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("需要访问相册", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "回留的核心功能是全相册整理，所以需要读取照片和视频。所有扫描都在本机完成，不上传媒体文件。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "Android 14 及以上如果选择“选择照片和视频”，只能整理你选中的部分内容；要做全相册整理，请在系统弹窗里选择允许访问所有照片和视频。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = { launcher.launch(permissions) }) { Text("授权相册访问") }
                        AssistChip(onClick = { access = currentMediaAccessState(context) }, label = { Text("刷新状态") })
                    }
                }
            }
        }
    }
}

@Composable
private fun PartialAccessBanner(
    modifier: Modifier = Modifier,
    onRequestFullAccess: () -> Unit,
) {
    GlassSurface(modifier = modifier.fillMaxWidth(), tonalAlpha = 0.82f) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text("当前是部分照片访问", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))
            Text(
                "时间轴和整理流只会显示系统允许回留访问的照片/视频。需要全相册整理时，请重新授权并选择允许访问所有照片和视频。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            FilledTonalButton(onClick = onRequestFullAccess) { Text("重新选择授权范围") }
        }
    }
}

fun currentMediaAccessState(context: Context): MediaAccessState {
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

private fun requiredMediaPermissions(): Array<String> = when {
    Build.VERSION.SDK_INT >= 34 -> arrayOf(
        Manifest.permission.READ_MEDIA_IMAGES,
        Manifest.permission.READ_MEDIA_VIDEO,
        Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
    )
    Build.VERSION.SDK_INT >= 33 -> arrayOf(
        Manifest.permission.READ_MEDIA_IMAGES,
        Manifest.permission.READ_MEDIA_VIDEO,
    )
    else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
}
