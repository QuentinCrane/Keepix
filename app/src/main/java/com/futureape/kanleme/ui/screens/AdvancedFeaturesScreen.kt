package com.futureape.kanleme.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.futureape.kanleme.R
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.size
import androidx.compose.ui.text.font.FontWeight
import com.futureape.kanleme.ui.components.morphAnchor

@Composable
internal fun AdvancedProfileTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    DisposableEffect(context) {
        val window = context.advancedActivity()?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, it.decorView) }
        val oldStatus = controller?.isAppearanceLightStatusBars
        controller?.isAppearanceLightStatusBars = false
        onDispose { oldStatus?.let { controller.isAppearanceLightStatusBars = it } }
    }
    // The Me section uses ProfileDarkPage even when the gallery theme is light.
    // Reuse its geometry, background, header and palette for every advanced subpage.
    MaterialTheme(colorScheme = MaterialTheme.colorScheme.copy(
        background = Color.Black, surface = ProfileScreenSurface, surfaceContainer = ProfileScreenSurface,
        onBackground = Color.White, onSurface = Color.White, onSurfaceVariant = Color.White.copy(alpha = 0.58f),
        primary = ProfileScreenAccent, onPrimary = Color(0xFF091224), primaryContainer = Color(0xFF203354),
        secondary = ProfileScreenAccent, onSecondary = Color(0xFF091224),
        secondaryContainer = ProfileScreenAccent.copy(alpha = 0.18f), onSecondaryContainer = ProfileScreenAccent,
        onPrimaryContainer = Color.White, outline = ProfileScreenCardBorder, outlineVariant = ProfileScreenCardBorder,
    )) {
        CompositionLocalProvider(LocalContentColor provides Color.White, content = content)
    }
}

private tailrec fun Context.advancedActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.advancedActivity()
    else -> null
}

@Composable
internal fun AdvancedPage(
    title: String, onBack: () -> Unit,
    subtitle: String = "",
    content: @Composable (PaddingValues) -> Unit,
) {
    AdvancedProfileTheme {
        ProfileDarkPage {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(top = 20.dp)) {
                ProfilePageHeader(title, subtitle, onBack, Modifier.padding(horizontal = 22.dp))
                Spacer(Modifier.height(18.dp))
                content(PaddingValues())
            }
        }
    }
}

@Composable
fun AdvancedFeaturesScreen(onBack: () -> Unit, onPhotos: () -> Unit, onVideos: () -> Unit, onRandomVideos: () -> Unit, onLicenses: () -> Unit) {
    AdvancedPage(stringResource(R.string.advanced_title), onBack, stringResource(R.string.advanced_subtitle)) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Text(stringResource(R.string.advanced_videos), style = MaterialTheme.typography.titleLarge)
            }
            item {
                ProfilePanel(Modifier.morphAnchor("advanced-video", 26.dp, color = ProfileScreenSurface,
                    icon = Icons.Rounded.Movie, tint = ProfileScreenAccent).clickable(onClick = onVideos)) {
                    Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Icon(Icons.Rounded.Movie, contentDescription = null, modifier = Modifier.size(32.dp), tint = ProfileScreenAccent)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(stringResource(R.string.advanced_video_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(stringResource(R.string.advanced_video_subtitle), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            item {
                ProfilePanel(Modifier.morphAnchor("advanced-random-video", 26.dp, color = ProfileScreenSurface,
                    icon = Icons.Rounded.Shuffle, tint = ProfileScreenAccent).clickable(onClick = onRandomVideos)) {
                    Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Icon(Icons.Rounded.Shuffle, contentDescription = null, modifier = Modifier.size(32.dp), tint = ProfileScreenAccent)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(stringResource(R.string.random_video_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(stringResource(R.string.random_video_subtitle), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            item {
                Text(stringResource(R.string.advanced_photos), style = MaterialTheme.typography.titleLarge)
            }
            item {
                ProfilePanel(Modifier.morphAnchor("advanced-photo", 26.dp, color = ProfileScreenSurface,
                    icon = Icons.Rounded.Image, tint = ProfileScreenAccent).clickable(onClick = onPhotos)) {
                    Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Icon(Icons.Rounded.Image, contentDescription = null, modifier = Modifier.size(32.dp), tint = ProfileScreenAccent)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(stringResource(R.string.advanced_photo_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(stringResource(R.string.advanced_photo_subtitle), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            item {
                TextButton(onClick = onLicenses, modifier = Modifier.morphAnchor("advanced-licenses", 26.dp, color = ProfileScreenSurface)) {
                    Text(stringResource(R.string.advanced_licenses))
                }
            }
        }
    }
}
