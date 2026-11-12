package com.futureape.kanleme.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
        Box(
            Modifier
                .fillMaxSize()
                .background(onboardingBackgroundBrush())
                .systemBarsPadding()
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            OnboardingGlow(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = (-80).dp, y = (-60).dp)
                    .size(300.dp),
                color = OnboardingAccentBlue,
                alpha = 0.30f,
            )
            OnboardingGlow(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 100.dp, y = 80.dp)
                    .size(340.dp),
                color = OnboardingAccentBlue,
                alpha = 0.20f,
            )
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp),
                shape = RoundedCornerShape(36.dp),
                color = Color.White.copy(alpha = 0.08f),
                contentColor = OnboardingText,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
            ) {
                Column(
                    Modifier.padding(horizontal = 24.dp, vertical = 30.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        OnboardingGlow(
                            modifier = Modifier.size(110.dp),
                            color = OnboardingAccentBlue,
                            alpha = 0.22f,
                        )
                        Surface(
                            modifier = Modifier.size(72.dp),
                            shape = RoundedCornerShape(24.dp),
                            color = OnboardingAccentBlue.copy(alpha = 0.16f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Rounded.PhotoLibrary,
                                    contentDescription = null,
                                    modifier = Modifier.size(34.dp),
                                    tint = OnboardingAccentBlue,
                                )
                            }
                        }
                    }
                    Text(
                        stringResource(R.string.permission_title),
                        style = MaterialTheme.typography.headlineMedium,
                        color = OnboardingText,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        stringResource(R.string.permission_intro),
                        style = MaterialTheme.typography.bodyLarge,
                        color = OnboardingMuted,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        stringResource(R.string.permission_platform_notice),
                        style = MaterialTheme.typography.bodySmall,
                        color = OnboardingMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(20.dp))
                            .border(BorderStroke(0.5.dp, Color.White.copy(alpha = 0.10f)), RoundedCornerShape(20.dp))
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = { access = MediaAccessSnapshot.current(context) },
                            modifier = Modifier.weight(1f).height(54.dp),
                            shape = RoundedCornerShape(999.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.32f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = OnboardingText),
                        ) {
                            Text(stringResource(R.string.permission_action_refresh))
                        }
                        Button(
                            onClick = { launcher.launch(permissions) },
                            modifier = Modifier.weight(1f).height(54.dp),
                            shape = RoundedCornerShape(999.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = OnboardingAccentBlue,
                                contentColor = Color(0xFF08111C),
                            ),
                        ) {
                            Text(stringResource(R.string.permission_action_authorize))
                        }
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
