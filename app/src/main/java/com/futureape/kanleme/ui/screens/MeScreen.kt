package com.futureape.kanleme.ui.screens

import com.futureape.kanleme.ui.components.morphAnchor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.stringResource
import com.futureape.kanleme.data.repository.DashboardStats
import com.futureape.kanleme.R
import com.futureape.kanleme.ui.components.AdaptiveCenter
import com.futureape.kanleme.ui.util.formatSize
import com.futureape.kanleme.ui.viewmodel.KanlemeViewModel

@Composable
fun MeScreen(
    viewModel: KanlemeViewModel,
    contentPadding: PaddingValues,
    onFavorites: () -> Unit,
    onTrash: () -> Unit,
    onHistory: () -> Unit,
    onSettings: () -> Unit,
    onSimilar: () -> Unit,
    onAchievements: () -> Unit,
    onAnnualReport: () -> Unit,
    onHelp: () -> Unit,
    onPrivacy: () -> Unit,
    onChangelog: () -> Unit,
    onDiagnosis: () -> Unit,
    onAdvanced: () -> Unit = {},
) {
    val screenState by viewModel.meScreenUiState.collectAsStateWithLifecycle()
    val dashboard = screenState.dashboard
    val photoTypeStats = screenState.photoTypeStats
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF050607),
                        Color(0xFF090B0E),
                        Color.Black,
                    )
                )
            )
    ) {
        AdaptiveCenter(maxWidth = 760.dp) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 22.dp,
                    end = 22.dp,
                    top = 46.dp,
                    bottom = contentPadding.calculateBottomPadding(),
                ),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            stringResource(R.string.me_title),
                            style = MaterialTheme.typography.headlineLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                        )
                        RoundIconSurface(
                            icon = Icons.Rounded.Settings,
                            contentDescription = stringResource(R.string.a11y_settings),
                            onClick = onSettings,
                        )
                    }
                }

                item {
                    UsageHeroCard(
                        dashboard = dashboard,
                        onRefresh = { viewModel.refreshLibrary() },
                    )
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        MediaStatCard(
                            icon = Icons.Rounded.Image,
                            title = stringResource(R.string.me_photo),
                            viewed = dashboard.processedPhotoCount,
                            deleted = dashboard.photoPendingDeleteCount,
                            cleaned = dashboard.photoPendingDeleteBytes,
                            accent = Color(0xFF86A7FF),
                        )
                        MediaStatCard(
                            icon = Icons.Rounded.Movie,
                            title = stringResource(R.string.me_video),
                            viewed = dashboard.processedVideoCount,
                            deleted = dashboard.videoPendingDeleteCount,
                            cleaned = dashboard.videoPendingDeleteBytes,
                            accent = Color(0xFFB884FF),
                        )
                        MediaStatCard(
                            icon = Icons.Rounded.Image,
                            title = stringResource(R.string.me_screenshot),
                            viewed = photoTypeStats.screenshot,
                            deleted = 0,
                            cleaned = 0L,
                            accent = Color(0xFF5DD9CB),
                        )
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        ProfileRow(
                            icon = Icons.Rounded.FavoriteBorder,
                            title = stringResource(R.string.me_favorites),
                            subtitle = stringResource(R.string.me_item_count, dashboard.favoriteCount),
                            accent = Color(0xFFFF6F7F),
                            onClick = onFavorites,
                        )
                        ProfileRow(
                            icon = Icons.Rounded.Delete,
                            title = stringResource(R.string.me_trash),
                            subtitle = stringResource(R.string.me_pending_item_count, dashboard.trashCount),
                            accent = Color(0xFFE46A62),
                            onClick = onTrash,
                        )
                        ProfileRow(
                            icon = Icons.Rounded.History,
                            title = stringResource(R.string.me_history),
                            subtitle = stringResource(R.string.me_recent_photo_history),
                            accent = Color(0xFF86A7FF),
                            onClick = onHistory,
                        )
                        ProfileRow(
                            icon = Icons.Rounded.Tune,
                            title = stringResource(R.string.advanced_title),
                            subtitle = stringResource(R.string.advanced_subtitle),
                            accent = Color(0xFF86A7FF),
                            onClick = onAdvanced,
                        )
                    }
                }
            }
        }
    }

    // Keep callback parameters wired for navigation compatibility while the visible entries are intentionally hidden.
    listOf(onSimilar, onAchievements, onAnnualReport, onHelp, onPrivacy, onChangelog, onDiagnosis)
}

@Composable
private fun UsageHeroCard(
    dashboard: DashboardStats,
    onRefresh: () -> Unit,
) {
    val historicalFreed = dashboard.userStats?.totalStorageFreed ?: 0L
    val freed = historicalFreed.takeIf { it > 0L } ?: dashboard.pendingDeleteBytes
    val progress = dashboard.progress.coerceIn(0f, 1f)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        color = Color(0xFF121417).copy(alpha = 0.94f),
        contentColor = Color.White,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(15.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(stringResource(R.string.me_usage_stats), style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Black)
                    Text(
                        formatSize(freed),
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                    )
                    Text(stringResource(if (historicalFreed > 0L) R.string.me_historical_freed else R.string.me_estimated_freed), style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.58f), fontWeight = FontWeight.Bold)
                }
                RoundIconSurface(
                    icon = Icons.Rounded.Refresh,
                    contentDescription = stringResource(R.string.a11y_refresh),
                    onClick = onRefresh,
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HeroMetric(stringResource(R.string.me_browsed), dashboard.processedCount.toString(), Modifier.weight(1f))
                HeroMetric(stringResource(R.string.me_pending), dashboard.pendingDeleteCount.toString(), Modifier.weight(1f))
                HeroMetric(stringResource(R.string.me_trash), dashboard.trashCount.toString(), Modifier.weight(1f))
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(5.dp),
                color = Color(0xFF86A7FF),
                trackColor = Color.White.copy(alpha = 0.10f),
            )
        }
    }
}

@Composable
private fun HeroMetric(label: String, value: String, modifier: Modifier) {
    Surface(
        modifier = modifier.height(72.dp),
        shape = RoundedCornerShape(20.dp),
        color = Color.White.copy(alpha = 0.055f),
        contentColor = Color.White,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
    ) {
        Column(Modifier.padding(horizontal = 12.dp), verticalArrangement = Arrangement.Center) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, maxLines = 1)
            Text(label, style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.56f), maxLines = 1)
        }
    }
}

@Composable
private fun MediaStatCard(
    icon: ImageVector,
    title: String,
    viewed: Int,
    deleted: Int,
    cleaned: Long,
    accent: Color,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFF121417).copy(alpha = 0.90f),
        contentColor = Color.White,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.09f)),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = RoundedCornerShape(13.dp),
                color = accent.copy(alpha = 0.20f),
                contentColor = accent,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp))
                }
            }
            Text(title, modifier = Modifier.width(54.dp), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, maxLines = 1)
            StatColumn(stringResource(R.string.me_viewed), viewed.toString(), Color(0xFF86A7FF), Modifier.weight(1f))
            StatColumn(stringResource(R.string.me_pending), deleted.toString(), Color(0xFFFF7B70), Modifier.weight(1f))
            StatColumn(stringResource(R.string.me_releasable), if (cleaned > 0L) formatSize(cleaned) else stringResource(R.string.me_zero_bytes), Color(0xFFC8EF67), Modifier.weight(1.15f))
        }
    }
}

@Composable
private fun StatColumn(label: String, value: String, accent: Color, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = accent, fontWeight = FontWeight.Bold, maxLines = 1)
        Text(value, style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun SpaceFreedCard(dashboard: DashboardStats) {
    val total = dashboard.pendingDeleteBytes.coerceAtLeast(0L)
    val photoWeight = if (total == 0L) 0f else (dashboard.photoPendingDeleteBytes.toFloat() / total.toFloat()).coerceIn(0f, 1f)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        color = Color(0xFF121417).copy(alpha = 0.90f),
        contentColor = Color.White,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.09f)),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Rounded.Storage, contentDescription = null, tint = Color.White.copy(alpha = 0.42f))
                Text(stringResource(R.string.me_releasable), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            }
            Text(formatSize(total), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black)
            LinearProgressIndicator(
                progress = { photoWeight.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = Color(0xFF86A7FF),
                trackColor = Color.White.copy(alpha = 0.10f),
            )
            Text(stringResource(R.string.me_photo_pending, (photoWeight * 100).toInt()), style = MaterialTheme.typography.titleSmall, color = Color.White.copy(alpha = 0.58f))
        }
    }
}

@Composable
private fun ProfileRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accent: Color,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().morphAnchor("profile:" + title, 26.dp,
            color = Color(0xFF121417), icon = icon, tint = accent),
        shape = RoundedCornerShape(26.dp),
        color = Color(0xFF121417).copy(alpha = 0.90f),
        contentColor = Color.White,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
    ) {
        Row(
            Modifier.padding(horizontal = 18.dp, vertical = 17.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Surface(modifier = Modifier.size(46.dp), shape = CircleShape, color = accent.copy(alpha = 0.16f), contentColor = accent) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(23.dp))
                }
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.54f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, tint = Color.White.copy(alpha = 0.42f))
        }
    }
}

@Composable
private fun RoundIconSurface(icon: ImageVector, contentDescription: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(52.dp).morphAnchor("profile-icon:" + contentDescription, 26.dp, icon = icon),
        shape = CircleShape,
        color = Color.White.copy(alpha = 0.13f),
        contentColor = Color.White,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(24.dp))
        }
    }
}
