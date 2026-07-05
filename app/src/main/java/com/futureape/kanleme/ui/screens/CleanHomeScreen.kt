package com.futureape.kanleme.ui.screens

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import com.futureape.kanleme.ui.i18n.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.futureape.kanleme.data.local.PhotoEntity
import com.futureape.kanleme.data.local.VideoEntity
import com.futureape.kanleme.data.repository.CleaningScope
import com.futureape.kanleme.data.repository.DashboardStats
import com.futureape.kanleme.data.settings.AppSettings
import com.futureape.kanleme.ui.components.AdaptiveCenter
import com.futureape.kanleme.ui.components.AdaptiveWidthInfo
import com.futureape.kanleme.ui.util.formatSize
import com.futureape.kanleme.ui.util.rememberHapticKit
import com.futureape.kanleme.ui.util.shareDailyReportImage
import com.futureape.kanleme.ui.viewmodel.KanlemeViewModel
import kotlinx.coroutines.launch

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
    onSettings: () -> Unit,
) {
    val dashboard by viewModel.dashboard.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val recentlyAddedPhotos by viewModel.recentlyAddedPhotos.collectAsStateWithLifecycle()
    val recentVideos by viewModel.recentVideos.collectAsStateWithLifecycle()
    val todayInHistoryPhotos by viewModel.todayInHistoryPhotos.collectAsStateWithLifecycle()
    val todayInHistoryVideos by viewModel.todayInHistoryVideos.collectAsStateWithLifecycle()
    val photoDeck by viewModel.photoDeck.collectAsStateWithLifecycle()
    val photoDeckMatchesCurrentScope by viewModel.photoDeckMatchesCurrentScope.collectAsStateWithLifecycle()
    val videoDeck by viewModel.videoDeck.collectAsStateWithLifecycle()
    val photoDeckPreview by viewModel.photoDeckPreview.collectAsStateWithLifecycle()
    val videoDeckPreview by viewModel.videoDeckPreview.collectAsStateWithLifecycle()
    val photoDeckPreparing by viewModel.photoDeckPreparing.collectAsStateWithLifecycle()
    val videoDeckPreparing by viewModel.videoDeckPreparing.collectAsStateWithLifecycle()
    val photoDeckPreviewPreparing by viewModel.photoDeckPreviewPreparing.collectAsStateWithLifecycle()
    val photoDeckPreviewReady by viewModel.photoDeckPreviewReady.collectAsStateWithLifecycle()
    val videoDeckPreviewPreparing by viewModel.videoDeckPreviewPreparing.collectAsStateWithLifecycle()
    val mediaLibraryRefreshing by viewModel.mediaLibraryRefreshing.collectAsStateWithLifecycle()
    val organizerScopesReady by viewModel.organizerScopesReady.collectAsStateWithLifecycle()
    val startupMediaBootstrapPending by viewModel.startupMediaBootstrapPending.collectAsStateWithLifecycle()
    val photoScope by viewModel.photoScope.collectAsStateWithLifecycle()
    val videoScope by viewModel.videoScope.collectAsStateWithLifecycle()
    val haptics = rememberHapticKit(settings)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showDailyReport by remember { mutableStateOf(false) }
    var exportingDailyReport by remember { mutableStateOf(false) }
    var dailyReportExportError by remember { mutableStateOf<String?>(null) }
    var lastEmptyPhotoPreviewReloadKey by remember { mutableStateOf<Any?>(null) }
    var lastPhotoQueuePrepareKey by remember { mutableStateOf<Any?>(null) }
    val selectedIsPhoto = true
    val hasTodayMemories = todayInHistoryPhotos.isNotEmpty() || todayInHistoryVideos.isNotEmpty()
    val openTodayIfAvailable = {
        if (hasTodayMemories) {
            haptics.tick()
            onToday()
        } else {
            viewModel.showMessage("今天暂无往年照片或视频")
        }
    }
    LaunchedEffect(settings.homeMediaTab) {
        if (settings.homeMediaTab != "photo") viewModel.setHomeMediaTab("photo")
    }
    LaunchedEffect(
        photoScope,
        settings.excludedFolderPaths,
        dashboard.photoCount,
        dashboard.processedPhotoCount,
        photoDeckMatchesCurrentScope,
        photoDeck.size,
        photoDeckPreparing,
        photoDeckPreviewReady,
        photoDeckPreview.size,
        organizerScopesReady,
    ) {
        if (!organizerScopesReady) return@LaunchedEffect
        val prepareKey = photoScope to (dashboard.photoCount to dashboard.processedPhotoCount)
        if (
            dashboard.processedPhotoCount < dashboard.photoCount &&
            (!photoDeckMatchesCurrentScope || photoDeck.isEmpty()) &&
            lastPhotoQueuePrepareKey != prepareKey
        ) {
            lastPhotoQueuePrepareKey = prepareKey
            viewModel.preparePhotoHomeQueue(photoScope)
        }
        if (photoDeck.isNotEmpty() && photoDeckMatchesCurrentScope) {
            lastPhotoQueuePrepareKey = null
        }
        val reloadKey = photoScope to (dashboard.photoCount to dashboard.processedPhotoCount)
        if (
            photoDeckPreviewReady &&
            photoDeckPreview.isEmpty() &&
            dashboard.processedPhotoCount < dashboard.photoCount &&
            lastEmptyPhotoPreviewReloadKey != reloadKey
        ) {
            lastEmptyPhotoPreviewReloadKey = reloadKey
            viewModel.reloadPhotoDeckPreview(photoScope)
        } else {
            if (photoDeckPreview.isNotEmpty()) lastEmptyPhotoPreviewReloadKey = null
            viewModel.loadPhotoDeckPreview(photoScope)
        }
    }
    val scopedPhotoDeck = if (organizerScopesReady && photoDeckMatchesCurrentScope) photoDeck else emptyList()
    ImmersiveCleanHomeScreen(
        contentPadding = contentPadding,
        dashboard = dashboard,
        settings = settings,
        photoScope = photoScope,
        videoScope = videoScope,
        photos = if (organizerScopesReady) scopedPhotoDeck.ifEmpty { photoDeckPreview } else emptyList(),
        videos = if (organizerScopesReady) videoDeck.ifEmpty { videoDeckPreview } else emptyList(),
        photoPreparing = !organizerScopesReady ||
            startupMediaBootstrapPending ||
            mediaLibraryRefreshing ||
            photoDeckPreparing ||
            (scopedPhotoDeck.isEmpty() && (photoDeckPreviewPreparing || (photoDeckPreview.isEmpty() && !photoDeckPreviewReady))),
        videoPreparing = !organizerScopesReady ||
            startupMediaBootstrapPending ||
            videoDeckPreparing ||
            (videoDeck.isEmpty() && videoDeckPreviewPreparing),
        selectedIsPhoto = selectedIsPhoto,
        onPhotoTab = {
            haptics.tick()
            viewModel.setHomeMediaTab("photo")
        },
        onVideoTab = {
            haptics.tick()
            onVideo()
        },
        onPhoto = onPhoto,
        onVideo = onVideo,
        onTimeline = onTimeline,
        onTrash = onTrash,
        onFavorites = onFavorites,
        onToday = openTodayIfAvailable,
        onSettings = onSettings,
        onPhotoType = { type ->
            haptics.tick()
            viewModel.setHomeMediaTab("photo")
            viewModel.setPhotoTypePreview(type)
        },
        onPhotoDate = { mode ->
            haptics.tick()
            if (mode == "today_history" && !hasTodayMemories) {
                viewModel.showMessage("今天暂无往年照片或视频")
            } else {
                viewModel.setPhotoDateModePreview(mode)
            }
        },
        onPhotoSort = { order ->
            haptics.tick()
            viewModel.setPhotoSortOrderPreview(order)
        },
        onPhotoBatch = { size ->
            haptics.tick()
            viewModel.setPhotoBatchSize(size)
        },
        onVideoDate = { mode ->
            haptics.tick()
            if (mode == "today_history" && !hasTodayMemories) {
                viewModel.showMessage("今天暂无往年照片或视频")
            } else {
                viewModel.setVideoDateModePreview(mode)
            }
        },
        onVideoSort = { order ->
            haptics.tick()
            viewModel.setVideoSortOrderPreview(order)
        },
        onVideoBatch = { size ->
            haptics.tick()
            viewModel.setVideoBatchSize(size)
        },
        onDailyReport = {
            haptics.tick()
            dailyReportExportError = null
            showDailyReport = true
        },
    )
    if (showDailyReport) {
        DailyReportDialog(
            todayPhotos = dashboard.todayPhotoCount,
            todayVideos = dashboard.todayVideoCount,
            todayActions = dashboard.todayActionCount,
            exporting = exportingDailyReport,
            exportError = dailyReportExportError,
            onDismiss = {
                showDailyReport = false
                dailyReportExportError = null
            },
            onExport = {
                exportingDailyReport = true
                dailyReportExportError = null
                scope.launch {
                    runCatching {
                        shareDailyReportImage(
                            context = context,
                            todayPhotos = dashboard.todayPhotoCount,
                            todayVideos = dashboard.todayVideoCount,
                            todayActions = dashboard.todayActionCount,
                        )
                    }.onSuccess {
                        haptics.success()
                    }.onFailure {
                        dailyReportExportError = "导出失败，请稍后再试"
                    }
                    exportingDailyReport = false
                }
            },
        )
    }
}

@Composable
private fun DailyReportDialog(
    todayPhotos: Int,
    todayVideos: Int,
    todayActions: Int,
    exporting: Boolean,
    exportError: String?,
    onDismiss: () -> Unit,
    onExport: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 28.dp),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth().widthIn(max = 420.dp),
                shape = RoundedCornerShape(34.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                contentColor = MaterialTheme.colorScheme.onSurface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.14f)),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("今日整理贴纸", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Surface(
                            modifier = Modifier.size(42.dp).clickable(onClick = onDismiss),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                    DailyReportSticker(
                        todayPhotos = todayPhotos,
                        todayVideos = todayVideos,
                        todayActions = todayActions,
                    )
                    if (exportError != null) {
                        Text(
                            exportError,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFE25C5C),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                        )
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(999.dp),
                        ) {
                            Text("关闭", style = MaterialTheme.typography.titleMedium)
                        }
                        Button(
                            onClick = onExport,
                            enabled = !exporting,
                            modifier = Modifier.weight(1.25f).height(48.dp),
                            shape = RoundedCornerShape(999.dp),
                        ) {
                            Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(if (exporting) "导出中" else "导出图片", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyReportSticker(
    todayPhotos: Int,
    todayVideos: Int,
    todayActions: Int,
    modifier: Modifier = Modifier,
) {
    val total = todayPhotos + todayVideos
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "今日整理",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        if (total > 0) "你今天处理了 " + total + " 个媒体文件" else "今天还没有开始整理",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Surface(shape = RoundedCornerShape(999.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.13f)) {
                    Text(
                        "动作 " + todayActions,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                RollingNumberText(
                    value = total,
                    compact = true,
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
                Text("今日总计", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TodayMetric("照片", todayPhotos, Modifier.weight(1f))
                TodayMetric("视频", todayVideos, Modifier.weight(1f))
                TodayMetric("动作", todayActions, Modifier.weight(1f))
            }
            Text(
                if (total > 0) "小步整理，也是在腾出生活空间" else "开一小局，从几张照片开始",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
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

