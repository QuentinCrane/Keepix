package com.futureape.kanleme.ui.screens

import android.Manifest
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.futureape.kanleme.R
import com.futureape.kanleme.data.media.MediaAccessState
import com.futureape.kanleme.data.media.MediaAccessSnapshot
import com.futureape.kanleme.ui.components.GlassSurface

/**
 * Android 13+ uses granular media permissions. Android 14+ can return either full
 * access or partial Selected Photos Access. This gate never persists permission
 * state; it reads the real system permission every time the composable enters.
 */
@Composable
fun MediaPermissionGate(
    onPermissionReady: (access: MediaAccessState, accessLabel: String) -> Unit,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val permissions = remember { requiredMediaPermissions() }
    var access by remember { mutableStateOf(MediaAccessSnapshot.current(context)) }
    var firstResumeObserved by remember { mutableStateOf(false) }

    val accessLabel = when {
        access.isFullVisualAccess -> stringResource(R.string.permission_access_full)
        access.isPartialVisualAccess -> stringResource(R.string.permission_access_partial)
        access.canReadImages -> stringResource(R.string.permission_access_images)
        access.canReadVideos -> stringResource(R.string.permission_access_videos)
        else -> stringResource(R.string.permission_access_none)
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        val latest = MediaAccessSnapshot.current(context)
        val changed = latest.key != access.key
        access = latest
        if (!changed && latest.isPartialVisualAccess) {
            // The selected-media URI set may change without changing permission booleans.
            onPermissionReady(latest, accessLabel)
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        val latest = MediaAccessSnapshot.current(context)
        val changed = latest.key != access.key
        access = latest
        if (firstResumeObserved && !changed && latest.isPartialVisualAccess) {
            // Android 14 can keep READ_MEDIA_VISUAL_USER_SELECTED granted while the
            // user replaces the selected photos/videos in Settings.
            onPermissionReady(latest, accessLabel)
        }
        firstResumeObserved = true
    }

    LaunchedEffect(access.key, accessLabel) {
        if (access.isGranted) {
            onPermissionReady(access, accessLabel)
        }
    }

    if (access.isGranted || permissions.isEmpty()) {
        Box(Modifier.fillMaxSize()) {
            content()
            if (access.isGranted && !access.isFullVisualAccess) {
                LimitedAccessBanner(
                    accessLabel = accessLabel,
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
                    Text(stringResource(R.string.permission_title), style = MaterialTheme.typography.headlineSmall)
                    Text(
                        stringResource(R.string.permission_intro),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        stringResource(R.string.permission_platform_notice),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = { launcher.launch(permissions) }) { Text(stringResource(R.string.permission_action_authorize)) }
                        AssistChip(onClick = { access = MediaAccessSnapshot.current(context) }, label = { Text(stringResource(R.string.permission_action_refresh)) })
                    }
                }
            }
        }
    }
}

@Composable
private fun LimitedAccessBanner(
    accessLabel: String,
    modifier: Modifier = Modifier,
    onRequestFullAccess: () -> Unit,
) {
    GlassSurface(modifier = modifier.fillMaxWidth(), tonalAlpha = 0.82f) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(stringResource(R.string.permission_limited_title, accessLabel), style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.permission_scope_notice),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            FilledTonalButton(onClick = onRequestFullAccess) { Text(stringResource(R.string.permission_action_reselect)) }
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
