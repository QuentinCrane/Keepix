package com.futureape.kanleme.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Share
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.futureape.kanleme.ui.components.AdaptiveCenter
import com.futureape.kanleme.ui.components.AdaptiveWidthInfo
import com.futureape.kanleme.ui.components.GlassSurface
import com.futureape.kanleme.ui.util.formatSize
import com.futureape.kanleme.ui.util.rememberHapticKit
import com.futureape.kanleme.ui.viewmodel.KanlemeViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CleanHomeScreen(
    viewModel: KanlemeViewModel,
    contentPadding: PaddingValues,
    onPhoto: () -> Unit,
    onVideo: () -> Unit,
    onTimeline: () -> Unit,
    onTrash: () -> Unit,
    onFavorites: () -> Unit,
    onToday: () -> Unit,
    onReport: () -> Unit,
) {
    val dashboard by viewModel.dashboard.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val recentlyAddedPhotos by viewModel.recentlyAddedPhotos.collectAsStateWithLifecycle()
    val recentVideos by viewModel.recentVideos.collectAsStateWithLifecycle()
    val haptics = rememberHapticKit(settings)
    val selectedIsPhoto = settings.homeMediaTab == "photo"
    val pagerState = rememberPagerState(
        initialPage = if (selectedIsPhoto) 0 else 1,
        pageCount = { 2 },
    )

    LaunchedEffect(selectedIsPhoto) {
        val targetPage = if (selectedIsPhoto) 0 else 1
        if (pagerState.currentPage != targetPage) pagerState.animateScrollToPage(targetPage)
    }
    LaunchedEffect(pagerState.currentPage) {
        val nextTab = if (pagerState.currentPage == 0) "photo" else "video"
        if (settings.homeMediaTab != nextTab) {
            haptics.tick()
            viewModel.setHomeMediaTab(nextTab)
        }
    }

    AdaptiveWidthInfo { _, isExpanded ->
        AdaptiveCenter(maxWidth = if (isExpanded) 1180.dp else 720.dp) {
            Box(Modifier.fillMaxSize()) {
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
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            HomeTopAction("当年今日", Icons.Rounded.CalendarToday, Modifier.weight(1f), onToday)
                            HomeTopAction("今日整理", Icons.Rounded.Share, Modifier.weight(1f), onReport)
                        }
                    }

                    item {
                        HomeMediaSegment(
                            selectedIsPhoto = selectedIsPhoto,
                            photoCount = dashboard.photoCount,
                            videoCount = dashboard.videoCount,
                            onPhoto = {
                                haptics.tick()
                                viewModel.setHomeMediaTab("photo")
                            },
                            onVideo = {
                                haptics.tick()
                                viewModel.setHomeMediaTab("video")
                            },
                        )
                    }

                    item {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxWidth().height(474.dp),
                            pageSpacing = 12.dp,
                        ) { page ->
                            HomeOrganizePage(
                                isPhoto = page == 0,
                                photoCount = dashboard.photoCount,
                                videoCount = dashboard.videoCount,
                                processedPhotoCount = dashboard.processedPhotoCount,
                                processedVideoCount = dashboard.processedVideoCount,
                                releasableBytes = dashboard.pendingDeleteBytes,
                                recentlyAddedPhotoCount = recentlyAddedPhotos.size,
                                recentlyAddedVideoCount = recentVideos.size,
                                onNumberPulse = { haptics.threshold() },
                                onStart = if (page == 0) onPhoto else onVideo,
                                onTimeline = onTimeline,
                            )
                        }
                    }

                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlineHomeButton("回收站", dashboard.trashCount, Icons.Rounded.Delete, modifier = Modifier.weight(1f), onClick = onTrash)
                            OutlineHomeButton("我的收藏", dashboard.favoriteCount, Icons.Rounded.FavoriteBorder, modifier = Modifier.weight(1f), onClick = onFavorites)
                        }
                    }

                    item {
                        QuickEntry(
                            if (selectedIsPhoto) "全相册时间轴" else "全视频时间轴",
                            if (selectedIsPhoto) "按日期浏览、打开大图、移动文件夹" else "按日期浏览视频、快速打开播放",
                            if (selectedIsPhoto) Icons.Rounded.Image else Icons.Rounded.Movie,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = onTimeline,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeOrganizePage(
    isPhoto: Boolean,
    photoCount: Int,
    videoCount: Int,
    processedPhotoCount: Int,
    processedVideoCount: Int,
    releasableBytes: Long,
    recentlyAddedPhotoCount: Int,
    recentlyAddedVideoCount: Int,
    onNumberPulse: () -> Unit,
    onStart: () -> Unit,
    onTimeline: () -> Unit,
) {
    val count = if (isPhoto) photoCount else videoCount
    val processed = if (isPhoto) processedPhotoCount else processedVideoCount
    val progress = if (count == 0) 0f else processed.toFloat() / count.toFloat()
    Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
        MainOrganizeCard(
            title = if (isPhoto) "照片整理" else "视频整理",
            subtitle = if (isPhoto) "卡片滑动 · 点击放大 · 可归档" else "上下刷视频 · 横滑进度 · 侧边按钮",
            count = count,
            processed = processed,
            progress = progress,
            icon = if (isPhoto) Icons.Rounded.PhotoLibrary else Icons.Rounded.PlayCircle,
            actionText = if (isPhoto) "开始整理照片" else "开始整理视频",
            releasableBytes = releasableBytes,
            modifier = Modifier.fillMaxWidth(),
            onNumberPulse = onNumberPulse,
            onClick = onStart,
        )
        SmallFeatureCard(
            title = if (isPhoto) "最近新增照片" else "最近新增视频",
            headline = (if (isPhoto) recentlyAddedPhotoCount else recentlyAddedVideoCount).toString(),
            subtitle = if (isPhoto) "最近 7 天进入相册的照片" else "最近进入相册的视频",
            icon = if (isPhoto) Icons.Rounded.Image else Icons.Rounded.Movie,
            modifier = Modifier.fillMaxWidth(),
            onClick = onTimeline,
        )
    }
}

@Composable
private fun HomeTopAction(text: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier.height(52.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.66f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.14f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(19.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Text(text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun HomeMediaSegment(
    selectedIsPhoto: Boolean,
    photoCount: Int,
    videoCount: Int,
    onPhoto: () -> Unit,
    onVideo: () -> Unit,
) {
    GlassSurface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(30.dp), tonalAlpha = 0.68f) {
        BoxWithConstraints(Modifier.fillMaxWidth().height(90.dp).padding(8.dp)) {
            val tabWidth = (maxWidth - 16.dp) / 2
            val indicatorOffset by animateDpAsState(
                targetValue = if (selectedIsPhoto) 0.dp else tabWidth + 8.dp,
                animationSpec = tween(durationMillis = 260),
                label = "home_segment_indicator",
            )
            Surface(
                modifier = Modifier
                    .width(tabWidth)
                    .height(74.dp)
                    .offset(x = indicatorOffset),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)),
            ) {}
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SegmentTab(
                    label = "照片",
                    value = photoCount,
                    selected = selectedIsPhoto,
                    icon = Icons.Rounded.PhotoLibrary,
                    modifier = Modifier.weight(1f),
                    onClick = onPhoto,
                )
                SegmentTab(
                    label = "视频",
                    value = videoCount,
                    selected = !selectedIsPhoto,
                    icon = Icons.Rounded.Movie,
                    modifier = Modifier.weight(1f),
                    onClick = onVideo,
                )
            }
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
    releasableBytes: Long,
    modifier: Modifier,
    onNumberPulse: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    val oledDark = MaterialTheme.colorScheme.background.luminance() < 0.03f
    val cardColor = if (oledDark) Color(0xFF050505) else MaterialTheme.colorScheme.surface
    val cardBorder = if (oledDark) Color.White.copy(alpha = 0.10f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.86f)
    val cardShape = RoundedCornerShape(34.dp)
    Box(
        modifier = modifier
            .height(274.dp)
            .clip(cardShape)
            .background(cardColor)
            .border(BorderStroke(1.dp, cardBorder), cardShape)
            .clickable(onClick = onClick)
            .padding(20.dp),
    ) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.11f)) {
                    Icon(icon, contentDescription = null, modifier = Modifier.padding(10.dp).size(24.dp), tint = MaterialTheme.colorScheme.primary)
                }
                Text(if (title.contains("视频")) "本地视频" else "本地照片", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
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
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(999.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = if (releasableBytes > 0L) 0.13f else 0.08f),
                        contentColor = MaterialTheme.colorScheme.primary,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                if (releasableBytes > 0L) "整理后可释放 " + formatSize(releasableBytes) else "暂无待释放空间",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                    Surface(
                        modifier = Modifier.weight(1.2f).height(58.dp),
                        shape = RoundedCornerShape(999.dp),
                        color = MaterialTheme.colorScheme.primary,
                    ) {
                        Box(contentAlignment = Alignment.Center) { Text(actionText, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SegmentTab(label: String, value: Int, selected: Boolean, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    val contentColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = modifier
            .height(74.dp)
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(20.dp))
            Text(label, style = MaterialTheme.typography.titleLarge, color = contentColor)
        }
        RollingNumberText(
            value = value,
            compact = true,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = contentColor,
        )
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
        modifier = modifier.graphicsLayer { alpha = if (target == 0) 0.72f else 1f },
        style = style,
        color = color,
        fontWeight = fontWeight,
    )
}

private fun Int.toDisplayCount(): String = if (this >= 1000) "%.1fK".format(this / 1000.0).replace(".0", "") else toString()
