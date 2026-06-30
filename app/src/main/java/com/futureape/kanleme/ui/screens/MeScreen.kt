package com.futureape.kanleme.ui.screens

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
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import com.futureape.kanleme.data.repository.DashboardStats
import com.futureape.kanleme.ui.components.AdaptiveCenter
import com.futureape.kanleme.ui.util.formatSize
import com.futureape.kanleme.ui.viewmodel.KanlemeViewModel
import com.futureape.kanleme.ui.i18n.Text

@Composable
fun MeScreen(
    viewModel: KanlemeViewModel,
    contentPadding: PaddingValues,
    onFavorites: () -> Unit,
    onTrash: () -> Unit,
    onSettings: () -> Unit,
    onSimilar: () -> Unit,
    onAchievements: () -> Unit,
    onAnnualReport: () -> Unit,
    onHelp: () -> Unit,
    onPrivacy: () -> Unit,
    onChangelog: () -> Unit,
    onDiagnosis: () -> Unit,
) {
    val dashboard by viewModel.dashboard.collectAsStateWithLifecycle()
    val photoTypeStats by viewModel.photoTypeStats.collectAsStateWithLifecycle()
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
                            "我的",
                            style = MaterialTheme.typography.headlineLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                        )
                        RoundIconSurface(icon = Icons.Rounded.Settings, onClick = onSettings)
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
                            title = "照片",
                            viewed = dashboard.processedPhotoCount,
                            deleted = dashboard.photoPendingDeleteCount,
                            cleaned = dashboard.photoPendingDeleteBytes,
                            accent = Color(0xFF86A7FF),
                        )
                        MediaStatCard(
                            icon = Icons.Rounded.Movie,
                            title = "视频",
                            viewed = dashboard.processedVideoCount,
                            deleted = dashboard.videoPendingDeleteCount,
                            cleaned = dashboard.videoPendingDeleteBytes,
                            accent = Color(0xFFB884FF),
                        )
                        MediaStatCard(
                            icon = Icons.Rounded.Image,
                            title = "截屏",
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
                            title = "收藏",
                            subtitle = dashboard.favoriteCount.toString() + " 个项目",
                            accent = Color(0xFFFF6F7F),
                            onClick = onFavorites,
                        )
                        ProfileRow(
                            icon = Icons.Rounded.Delete,
                            title = "回收站",
                            subtitle = dashboard.trashCount.toString() + " 个待确认项目",
                            accent = Color(0xFFE46A62),
                            onClick = onTrash,
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
    val freed = dashboard.pendingDeleteBytes
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
                    Text("使用统计", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Black)
                    Text(
                        formatSize(freed),
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                    )
                    Text("预计可释放", style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.58f), fontWeight = FontWeight.Bold)
                }
                RoundIconSurface(icon = Icons.Rounded.Refresh, onClick = onRefresh)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HeroMetric("已浏览", dashboard.processedCount.toString(), Modifier.weight(1f))
                HeroMetric("待确认", dashboard.pendingDeleteCount.toString(), Modifier.weight(1f))
                HeroMetric("回收站", dashboard.trashCount.toString(), Modifier.weight(1f))
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
    val progress = when {
        viewed <= 0 && deleted <= 0 -> 0f
        viewed <= 0 -> 1f
        else -> (deleted.toFloat() / viewed.toFloat()).coerceIn(0f, 1f)
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFF121417).copy(alpha = 0.90f),
        contentColor = Color.White,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.09f)),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
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
                StatColumn("浏览", viewed.toString(), Color(0xFF86A7FF), Modifier.weight(1f))
                StatColumn("待确认", deleted.toString(), Color(0xFFFF7B70), Modifier.weight(1f))
                StatColumn("预计释放", if (cleaned > 0L) formatSize(cleaned) else "0 字节", Color(0xFFC8EF67), Modifier.weight(1.15f))
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(5.dp),
                color = accent.copy(alpha = 0.88f),
                trackColor = Color.White.copy(alpha = 0.09f),
            )
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
                Text("预计释放", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            }
            Text(formatSize(total), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black)
            LinearProgressIndicator(
                progress = { photoWeight.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = Color(0xFF86A7FF),
                trackColor = Color.White.copy(alpha = 0.10f),
            )
            Text("照片待确认 " + (photoWeight * 100).toInt() + "%", style = MaterialTheme.typography.titleSmall, color = Color.White.copy(alpha = 0.58f))
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
        modifier = Modifier.fillMaxWidth(),
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
private fun RoundIconSurface(icon: ImageVector, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(52.dp),
        shape = CircleShape,
        color = Color.White.copy(alpha = 0.13f),
        contentColor = Color.White,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
        }
    }
}
