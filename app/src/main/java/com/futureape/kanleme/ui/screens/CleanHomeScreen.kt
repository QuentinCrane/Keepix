package com.futureape.kanleme.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.futureape.kanleme.ui.components.AdaptiveCenter
import com.futureape.kanleme.ui.components.AdaptiveWidthInfo
import com.futureape.kanleme.ui.components.GlassSurface
import com.futureape.kanleme.ui.util.rememberHapticKit
import com.futureape.kanleme.ui.viewmodel.KanlemeViewModel

@Composable
fun CleanHomeScreen(
    viewModel: KanlemeViewModel,
    contentPadding: PaddingValues,
    onPhoto: () -> Unit,
    onVideo: () -> Unit,
    onTimeline: () -> Unit,
    onSimilar: () -> Unit,
    onTrash: () -> Unit,
    onFavorites: () -> Unit,
    onToday: () -> Unit,
) {
    val dashboard by viewModel.dashboard.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val haptics = rememberHapticKit(settings)
    val selectedIsPhoto = settings.homeMediaTab == "photo"
    val selectedCount = if (selectedIsPhoto) dashboard.photoCount else dashboard.videoCount
    val selectedProcessed = if (selectedIsPhoto) dashboard.processedPhotoCount else dashboard.processedVideoCount
    val selectedProgress = if (selectedCount == 0) 0f else selectedProcessed.toFloat() / selectedCount.toFloat()
    val selectedTitle = if (selectedIsPhoto) "照片整理" else "视频整理"
    val selectedSubtitle = if (selectedIsPhoto) "卡片滑动 · 点击放大 · 可归档" else "上下刷视频 · 横滑进度 · 长按倍速"
    val selectedIcon = if (selectedIsPhoto) Icons.Rounded.PhotoLibrary else Icons.Rounded.PlayCircle
    val startSelected = if (selectedIsPhoto) onPhoto else onVideo

    AdaptiveWidthInfo { _, isExpanded ->
        AdaptiveCenter(maxWidth = if (isExpanded) 1180.dp else 720.dp) {
            LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 18.dp,
            end = 18.dp,
            top = 54.dp,
            bottom = contentPadding.calculateBottomPadding(),
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("整理", style = MaterialTheme.typography.displaySmall, modifier = Modifier.weight(1f))
                Surface(
                    modifier = Modifier.clickable(onClick = onToday),
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.58f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)),
                ) {
                    Row(
                        Modifier.padding(horizontal = 15.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(Icons.Rounded.CalendarToday, contentDescription = null, modifier = Modifier.size(19.dp))
                        Text("当年今日", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }

        item {
            GlassSurface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(30.dp), tonalAlpha = 0.68f) {
                Row(Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SegmentTab(
                        label = "照片",
                        value = dashboard.photoCount,
                        selected = selectedIsPhoto,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.setHomeMediaTab("photo") },
                    )
                    SegmentTab(
                        label = "视频",
                        value = dashboard.videoCount,
                        selected = !selectedIsPhoto,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.setHomeMediaTab("video") },
                    )
                }
            }
        }

        if (isExpanded) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    MainOrganizeCard(
                        title = selectedTitle,
                        subtitle = selectedSubtitle,
                        count = selectedCount,
                        processed = selectedProcessed,
                        progress = selectedProgress,
                        icon = selectedIcon,
                        actionText = if (selectedIsPhoto) "开始整理照片" else "开始整理视频",
                        modifier = Modifier.weight(1.55f),
                        onNumberPulse = { haptics.threshold() },
                        onClick = startSelected,
                    )
                    TodayStatsCard(
                        todayPhotos = dashboard.todayPhotoCount,
                        todayVideos = dashboard.todayVideoCount,
                        todayActions = dashboard.todayActionCount,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        } else {
            item {
                TodayStatsCard(
                    todayPhotos = dashboard.todayPhotoCount,
                    todayVideos = dashboard.todayVideoCount,
                    todayActions = dashboard.todayActionCount,
                )
            }

            item {
                MainOrganizeCard(
                    title = selectedTitle,
                    subtitle = selectedSubtitle,
                    count = selectedCount,
                    processed = selectedProcessed,
                    progress = selectedProgress,
                    icon = selectedIcon,
                    actionText = if (selectedIsPhoto) "开始整理照片" else "开始整理视频",
                    modifier = Modifier.fillMaxWidth(),
                    onNumberPulse = { haptics.threshold() },
                    onClick = startSelected,
                )
            }
        }

        item {
            GlassSurface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(30.dp), tonalAlpha = 0.64f) {
                Row(
                    Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    HomePill("随机排序", modifier = Modifier.weight(1f))
                    HomePill("全部时间", modifier = Modifier.weight(1f))
                    HomePill("排除文件夹", modifier = Modifier.weight(1f))
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SmallFeatureCard("相似照片", "智能检测", "检测连拍、截图和相似照片", Icons.Rounded.AutoAwesome, modifier = Modifier.weight(1f), onClick = onSimilar)
                SmallFeatureCard("最近新增照片", "最近 7 天", "快速查看新进入相册的内容", Icons.Rounded.AccessTime, modifier = Modifier.weight(1f), onClick = onTimeline)
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlineHomeButton("回收站", dashboard.trashCount, Icons.Rounded.Delete, modifier = Modifier.weight(1f), onClick = onTrash)
                OutlineHomeButton("我的收藏", dashboard.favoriteCount, Icons.Rounded.FavoriteBorder, modifier = Modifier.weight(1f), onClick = onFavorites)
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickEntry("全相册时间轴", "按日期浏览、打开大图、移动文件夹", Icons.Rounded.Image, modifier = Modifier.weight(1f), onClick = onTimeline)
                QuickEntry("当年今日", "回看往年今天的照片", Icons.Rounded.CalendarToday, modifier = Modifier.weight(1f), onClick = onToday)
            }
        }
            }
        }
    }
}

@Composable
private fun TodayStatsCard(todayPhotos: Int, todayVideos: Int, todayActions: Int, modifier: Modifier = Modifier) {
    GlassSurface(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), tonalAlpha = 0.66f) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("今日整理", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Text("你今天处理了 " + (todayPhotos + todayVideos) + " 个媒体文件", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Surface(shape = RoundedCornerShape(999.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.13f)) {
                    Text("动作 " + todayActions, modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TodayMetric("照片", todayPhotos, Modifier.weight(1f))
                TodayMetric("视频", todayVideos, Modifier.weight(1f))
                TodayMetric("总计", todayPhotos + todayVideos, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun TodayMetric(label: String, value: Int, modifier: Modifier) {
    Surface(
        modifier = modifier.height(64.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
    ) {
        Column(Modifier.padding(horizontal = 12.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            RollingNumberText(
                value = value,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun MainOrganizeCard(
    title: String,
    subtitle: String,
    count: Int,
    processed: Int,
    progress: Float,
    icon: ImageVector,
    actionText: String,
    modifier: Modifier,
    onNumberPulse: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    val oledDark = MaterialTheme.colorScheme.background.luminance() < 0.03f
    val cardBrush = if (oledDark) {
        Brush.linearGradient(
            listOf(
                Color(0xFF050505),
                Color(0xFF0A0A0A),
                Color(0xFF000000),
            )
        )
    } else {
        Brush.linearGradient(
            listOf(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.20f),
                MaterialTheme.colorScheme.surface.copy(alpha = 0.66f),
                MaterialTheme.colorScheme.secondary.copy(alpha = 0.10f),
            )
        )
    }
    Box(
        modifier = modifier
            .height(268.dp)
            .clip(RoundedCornerShape(34.dp))
            .background(cardBrush)
            .clickable(onClick = onClick)
            .padding(20.dp),
    ) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.13f)) {
                    Icon(icon, contentDescription = null, modifier = Modifier.padding(10.dp).size(24.dp), tint = MaterialTheme.colorScheme.primary)
                }
                Text(if (title.contains("视频")) "VIDEO CLEAN" else "PHOTO CLEAN", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                    RollingNumberText(
                        value = count,
                        compact = true,
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        onPulse = onNumberPulse,
                    )
                    Spacer(Modifier.width(10.dp))
                    Text("待整理", modifier = Modifier.padding(bottom = 8.dp), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.weight(1f))
                    Text(processed.toString() + "/" + count, modifier = Modifier.padding(bottom = 8.dp), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                }
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(7.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.78f),
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                )
                Surface(
                    modifier = Modifier.fillMaxWidth().height(68.dp),
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.primary,
                ) {
                    Box(contentAlignment = Alignment.Center) { Text(actionText, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

@Composable
private fun SegmentTab(label: String, value: Int, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier.height(74.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else Color.Transparent,
        border = if (selected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)) else null,
    ) {
        Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.titleLarge, color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            RollingNumberText(
                value = value,
                compact = true,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun HomePill(text: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.48f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.16f)),
    ) {
        Box(contentAlignment = Alignment.Center) { Text(text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary) }
    }
}

@Composable
private fun SmallFeatureCard(title: String, headline: String, subtitle: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    GlassSurface(modifier = modifier.height(176.dp).clickable(onClick = onClick), shape = RoundedCornerShape(28.dp), tonalAlpha = 0.62f) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(title, style = MaterialTheme.typography.titleMedium)
            }
            Column {
                Text(headline, style = MaterialTheme.typography.headlineLarge)
                Spacer(Modifier.height(6.dp))
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun OutlineHomeButton(title: String, count: Int, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier.height(68.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.42f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
    ) {
        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = if (title.contains("回收")) Color(0xFFE25C5C) else MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(10.dp))
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.width(8.dp))
            RollingNumberText(
                value = count,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun QuickEntry(title: String, subtitle: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    GlassSurface(modifier = modifier.clickable(onClick = onClick), shape = RoundedCornerShape(26.dp), tonalAlpha = 0.58f) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(modifier = Modifier.size(46.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)) {
                Box(contentAlignment = Alignment.Center) { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun RollingNumberText(
    value: Int,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    style: TextStyle = MaterialTheme.typography.titleLarge,
    color: Color = MaterialTheme.colorScheme.primary,
    fontWeight: FontWeight? = null,
    onPulse: (() -> Unit)? = null,
) {
    val target = value.coerceAtLeast(0)
    val animated = remember(target) { Animatable(0f) }
    LaunchedEffect(target) {
        animated.snapTo(0f)
        if (target > 0) {
            onPulse?.invoke()
            animated.animateTo(target.toFloat(), animationSpec = tween(durationMillis = 820))
        }
    }
    val shown = animated.value.toInt().coerceIn(0, target)
    Text(
        text = if (compact) shown.toDisplayCount() else shown.toString(),
        modifier = modifier,
        style = style,
        color = color,
        fontWeight = fontWeight,
    )
}

private fun Int.toDisplayCount(): String = if (this >= 1000) "%.1fK".format(this / 1000.0).replace(".0", "") else toString()
