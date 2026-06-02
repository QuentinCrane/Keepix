package com.futureape.kanleme.ui.screens

import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.futureape.kanleme.data.local.VideoEntity
import com.futureape.kanleme.data.repository.SwipeAction
import com.futureape.kanleme.data.settings.AppSettings
import com.futureape.kanleme.data.settings.VideoDisplayMode
import com.futureape.kanleme.ui.components.AdaptiveWidthInfo
import com.futureape.kanleme.ui.components.EmptyState
import com.futureape.kanleme.ui.util.HapticKit
import com.futureape.kanleme.ui.util.rememberHapticKit
import com.futureape.kanleme.ui.util.shareVideo
import com.futureape.kanleme.ui.util.formatDate
import com.futureape.kanleme.ui.util.formatDuration
import com.futureape.kanleme.ui.util.formatSize
import com.futureape.kanleme.ui.viewmodel.KanlemeViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VideoCleanScreen(viewModel: KanlemeViewModel, onBack: () -> Unit) {
    val rawVideos by viewModel.videoDeck.collectAsStateWithLifecycle()
    val deckPreparing by viewModel.videoDeckPreparing.collectAsStateWithLifecycle()
    val videos = remember(rawVideos) { rawVideos.distinctBy { it.mediaStoreId } }
    val dashboard by viewModel.dashboard.collectAsStateWithLifecycle()
    val scope by viewModel.videoScope.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val haptics = rememberHapticKit(settings)
    val uiScope = rememberCoroutineScope()
    var showGuide by remember { mutableStateOf(false) }
    val pagerState = rememberPagerState(pageCount = { videos.size.coerceAtLeast(1) })
    var lastAppliedShuffleSeed by remember { mutableStateOf(scope.randomSeed) }
    val dimAlpha by animateFloatAsState(targetValue = if (showGuide && videos.isNotEmpty()) 0.55f else 0f, label = "video_guide_dim")

    LaunchedEffect(scope.folderPaths, scope.sortOrder, settings.excludedFolderPaths) { viewModel.loadVideoDeck(scope) }
    LaunchedEffect(videos.isEmpty(), deckPreparing, dashboard.videoCount, dashboard.processedVideoCount, scope) {
        if (videos.isEmpty() && !deckPreparing && dashboard.processedVideoCount < dashboard.videoCount) viewModel.loadVideoDeck(scope)
    }
    LaunchedEffect(scope.randomSeed, videos.firstOrNull()?.mediaStoreId) {
        if (scope.sortOrder == "random" && scope.randomSeed != lastAppliedShuffleSeed && videos.isNotEmpty()) {
            pagerState.scrollToPage(0)
            lastAppliedShuffleSeed = scope.randomSeed
        }
    }
    LaunchedEffect(videos, settings.videoGuideShown) {
        if (videos.isNotEmpty() && !settings.videoGuideShown) showGuide = true
    }
    LaunchedEffect(videos.size) {
        if (videos.isNotEmpty() && pagerState.currentPage >= videos.size) {
            pagerState.scrollToPage((videos.size - 1).coerceAtLeast(0))
        }
    }
    LaunchedEffect(pagerState.currentPage) { haptics.tick() }

    fun performVideoAction(video: VideoEntity, action: SwipeAction) {
        haptics.success()
        uiScope.launch {
            if (videos.size > 1 && pagerState.currentPage >= videos.lastIndex) {
                pagerState.scrollToPage((videos.lastIndex - 1).coerceAtLeast(0))
            }
            viewModel.onVideoAction(video, action)
        }
    }

    if (videos.isEmpty()) {
        Column(Modifier.fillMaxSize().padding(top = 36.dp)) {
            ScreenHeader("视频整理", "单击播放/暂停，左右滑切换沉浸", onBack)
            Box(Modifier.fillMaxSize().padding(18.dp), contentAlignment = Alignment.Center) {
                if (deckPreparing) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        CircularProgressIndicator()
                        Text("正在准备整理队列", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                        Text("会直接进入当前随机 / 最新顺序，不再先闪出暂无队列", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    EmptyState(
                        title = "当前筛选下暂无待整理视频",
                        message = "连续整理不会按轮次停止；这里为空通常表示当前筛选、排除文件夹或相册权限下已经没有可整理内容。",
                        actionText = "重新读取队列",
                        onAction = { haptics.success(); if (dashboard.videoCount == 0) viewModel.refreshLibrary() else viewModel.loadVideoDeck(scope) },
                    )
                }
            }
        }
        return
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AdaptiveWidthInfo { isMediumOrExpanded, isExpanded ->
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                val pagerModifier = when {
                    isExpanded -> Modifier.widthIn(min = 480.dp, max = if (settings.videoDisplayMode == VideoDisplayMode.FIT_SCREEN) 860.dp else 720.dp).fillMaxHeight()
                    isMediumOrExpanded -> Modifier.widthIn(min = 420.dp, max = 680.dp).fillMaxHeight()
                    else -> Modifier.fillMaxSize()
                }
                VerticalPager(
                    state = pagerState,
                    modifier = pagerModifier,
                    key = { page -> videos.getOrNull(page)?.mediaStoreId ?: page },
                ) { page ->
                    val video = videos.getOrNull(page) ?: return@VerticalPager
                    VideoReelPage(
                        video = video,
                        settings = settings,
                        haptics = haptics,
                        isCurrent = page == pagerState.currentPage,
                        pageIndex = (dashboard.processedVideoCount + page + 1).coerceAtLeast(1),
                        total = dashboard.videoCount.coerceAtLeast(videos.size),
                        deletedCount = dashboard.pendingDeleteCount,
                        onKeep = { performVideoAction(video, SwipeAction.Keep) },
                        onFavorite = { performVideoAction(video, SwipeAction.Favorite) },
                        onDelete = { performVideoAction(video, SwipeAction.Delete) },
                    )
                }
            }
        }

        if (settings.videoShowTopBar || settings.videoShowShuffleButton) {
            VideoOrganizerTopRow(
                current = (dashboard.processedVideoCount + pagerState.currentPage + 1).coerceAtMost(dashboard.videoCount.coerceAtLeast(videos.size)),
                total = dashboard.videoCount.coerceAtLeast(videos.size),
                deletedCount = dashboard.pendingDeleteCount,
                deletedSizeBytes = dashboard.pendingDeleteBytes,
                showShuffle = settings.videoShowShuffleButton,
                onShuffle = { haptics.threshold(); viewModel.reshuffleVideoCleaningSession() },
                modifier = Modifier.align(Alignment.TopCenter),
            )
        }

        if (dimAlpha > 0.01f) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = dimAlpha)))
        androidx.compose.animation.AnimatedVisibility(visible = showGuide, modifier = Modifier.fillMaxSize()) {
            VideoPositionGuideOverlay(onDismiss = { haptics.tick(); showGuide = false; viewModel.markVideoGuideShown() })
        }
    }
}


private data class VideoPositionGuideStep(
    val title: String,
    val body: String,
    val targetAlignment: Alignment,
    val cardAlignment: Alignment,
    val targetWidth: androidx.compose.ui.unit.Dp,
    val targetHeight: androidx.compose.ui.unit.Dp,
    val targetPadding: androidx.compose.foundation.layout.PaddingValues = androidx.compose.foundation.layout.PaddingValues(0.dp),
)

@Composable
private fun VideoPositionGuideOverlay(onDismiss: () -> Unit) {
    var stepIndex by remember { mutableIntStateOf(0) }
    val steps = listOf(
        VideoPositionGuideStep(
            title = "顶部视频状态栏",
            body = "当前进度、待删数量和重新随机都在第一行，竖屏平板也不会被左侧 Dock 挤开。",
            targetAlignment = Alignment.TopCenter,
            cardAlignment = Alignment.BottomCenter,
            targetWidth = 340.dp,
            targetHeight = 56.dp,
            targetPadding = androidx.compose.foundation.layout.PaddingValues(top = 42.dp),
        ),
        VideoPositionGuideStep(
            title = "中间视频区域",
            body = "单击视频区域可播放 / 暂停，左右滑切换沉浸观看和显示按钮；当前页才创建播放器，降低闪退和卡顿概率。",
            targetAlignment = Alignment.Center,
            cardAlignment = Alignment.BottomCenter,
            targetWidth = 320.dp,
            targetHeight = 500.dp,
        ),
        VideoPositionGuideStep(
            title = "侧边操作栏",
            body = "播放 / 暂停只保留在侧边栏，保留、收藏、待删和分享集中在同一列，避免底部按钮与信息区重叠。",
            targetAlignment = Alignment.CenterEnd,
            cardAlignment = Alignment.CenterStart,
            targetWidth = 92.dp,
            targetHeight = 424.dp,
            targetPadding = androidx.compose.foundation.layout.PaddingValues(end = 12.dp),
        ),
    )
    val step = steps[stepIndex.coerceIn(0, steps.lastIndex)]
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.66f)))
        Box(
            modifier = Modifier
                .align(step.targetAlignment)
                .padding(step.targetPadding)
                .size(width = step.targetWidth, height = step.targetHeight)
                .border(2.dp, Color.White.copy(alpha = 0.86f), RoundedCornerShape(28.dp))
                .background(Color.White.copy(alpha = 0.10f), RoundedCornerShape(28.dp)),
        )
        Surface(
            modifier = Modifier.align(step.cardAlignment).padding(horizontal = 22.dp, vertical = 42.dp).fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = Color.White.copy(alpha = 0.94f),
            contentColor = Color.Black,
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("步骤 " + (stepIndex + 1) + "/" + steps.size, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Text(step.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(step.body, style = MaterialTheme.typography.bodyMedium, color = Color.Black.copy(alpha = 0.70f))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    if (stepIndex > 0) Button(onClick = { stepIndex -= 1 }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(999.dp)) { Text("上一步") }
                    Button(onClick = { if (stepIndex < steps.lastIndex) stepIndex += 1 else onDismiss() }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(999.dp)) { Text(if (stepIndex < steps.lastIndex) "下一步" else "知道了") }
                }
            }
        }
    }
}

@Composable
private fun VideoOrganizerTopRow(
    current: Int,
    total: Int,
    deletedCount: Int,
    deletedSizeBytes: Long,
    showShuffle: Boolean,
    onShuffle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        VideoTopPill(label = current.toString() + "/" + total.toString(), emphasized = true)
        VideoTopPill(label = if (deletedSizeBytes > 0L) "可释放 " + formatSize(deletedSizeBytes) else "待删 " + deletedCount.toString())
        if (showShuffle) {
            ShuffleSessionButton(
                label = "重新随机",
                lightContent = true,
                onClick = onShuffle,
            )
        }
    }
}

@Composable
private fun VideoTopPill(
    label: String,
    emphasized: Boolean = false,
) {
    Surface(
        modifier = Modifier.height(44.dp),
        shape = RoundedCornerShape(999.dp),
        color = Color.White.copy(alpha = if (emphasized) 0.20f else 0.14f),
        contentColor = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun VideoReelPage(
    video: VideoEntity,
    settings: AppSettings,
    haptics: HapticKit,
    isCurrent: Boolean,
    pageIndex: Int,
    total: Int,
    deletedCount: Int,
    onKeep: () -> Unit,
    onFavorite: () -> Unit,
    onDelete: () -> Unit,
) {
    val context = LocalContext.current
    val player = remember(video.uri, isCurrent) {
        if (!isCurrent) {
            null
        } else {
            runCatching {
                ExoPlayer.Builder(context).build().apply {
                    setMediaItem(MediaItem.fromUri(Uri.parse(video.uri)))
                    repeatMode = Player.REPEAT_MODE_ONE
                    prepare()
                }
            }.getOrNull()
        }
    }
    var muted by remember(video.id, settings.videoDefaultMuted) { mutableStateOf(settings.videoDefaultMuted) }
    var controlsVisible by remember(video.id) { mutableStateOf(true) }
    var shouldPlay by remember(video.id) { mutableStateOf(true) }

    LaunchedEffect(player, muted, isCurrent, shouldPlay) {
        player?.playWhenReady = isCurrent && shouldPlay
        player?.volume = if (muted) 0f else 1f
    }
    DisposableEffect(player) {
        onDispose {
            player?.playWhenReady = false
            player?.clearMediaItems()
            player?.release()
        }
    }

    val position by produceState(initialValue = 0L, key1 = player, key2 = isCurrent) {
        while (player != null && isCurrent) {
            value = player.currentPosition.coerceAtLeast(0L)
            delay(260)
        }
    }
    val playerDuration = player?.duration?.takeIf { it > 0 }
    val duration = video.duration.takeIf { it > 0 } ?: playerDuration ?: 1L
    val progress = (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
    var draggingProgress by remember(video.id) { mutableStateOf<Float?>(null) }
    val shownProgress = draggingProgress ?: progress

    Box(Modifier.fillMaxSize()) {
        if (player != null) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = {
                    PlayerView(it).apply {
                        useController = false
                        resizeMode = when (settings.videoDisplayMode) {
                            VideoDisplayMode.IMMERSIVE_CROP -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                            VideoDisplayMode.FIT_SCREEN -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                            VideoDisplayMode.FILL_WIDTH -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
                        }
                        isClickable = false
                        isFocusable = false
                        this.player = player
                    }
                },
                update = { view ->
                    view.player = player
                    view.resizeMode = when (settings.videoDisplayMode) {
                        VideoDisplayMode.IMMERSIVE_CROP -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        VideoDisplayMode.FIT_SCREEN -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                        VideoDisplayMode.FILL_WIDTH -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
                    }
                },
            )
        } else {
            AsyncImage(
                model = Uri.parse(video.uri),
                contentDescription = video.displayName,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }

        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(video.id, settings.videoSeekThresholdPx, player) {
                    detectReelSurfaceGestures(
                        haptics = haptics,
                        seekThreshold = settings.videoSeekThresholdPx,
                        onTogglePlay = { haptics.tick(); shouldPlay = !shouldPlay },
                        onToggleControls = { controlsVisible = !controlsVisible },
                        onSpeedStart = { player?.setPlaybackSpeed(2f) },
                        onSpeedEnd = { player?.setPlaybackSpeed(1f) },
                    )
                }
        )

        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.06f)))
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.42f),
                            Color.Transparent,
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.76f),
                        )
                    )
                )
        )

        androidx.compose.animation.AnimatedVisibility(visible = controlsVisible && settings.videoShowActionRail, modifier = Modifier.align(Alignment.CenterEnd).padding(end = 12.dp)) {
            ReelActionRail(
                muted = muted,
                playing = shouldPlay,
                deletedCount = deletedCount,
                onTogglePlay = { haptics.tick(); shouldPlay = !shouldPlay },
                onMute = { haptics.tick(); muted = !muted },
                onKeep = onKeep,
                onFavorite = onFavorite,
                onDelete = onDelete,
                onShare = { haptics.tick(); shareVideo(context, video) },
                modifier = Modifier.height(424.dp),
            )
        }

        androidx.compose.animation.AnimatedVisibility(
            visible = controlsVisible && (settings.videoShowInfoPanel || settings.videoShowFolderChips || settings.videoShowProgressBar),
            modifier = Modifier.align(Alignment.BottomStart),
        ) {
            Column(Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (settings.videoShowInfoPanel) {
                    Text("@" + video.folderName, style = MaterialTheme.typography.headlineSmall, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        settings.videoDisplayMode.label + " · " + formatDate(video.dateTaken) + " · " + formatSize(video.size) + " · " + formatDuration(duration),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.78f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (settings.videoShowFolderChips) {
                    FolderChipRail(
                        folders = listOf("指定归档", video.folderName, "Camera (DCIM)", "Downloads"),
                        selected = video.folderName,
                        onSelected = { haptics.tick() },
                        modifier = Modifier.fillMaxWidth().padding(end = if (settings.videoShowActionRail) 74.dp else 0.dp),
                    )
                }
                if (settings.videoShowProgressBar) {
                    Slider(
                        value = shownProgress,
                        onValueChange = { value ->
                            draggingProgress = value
                            player?.seekTo((duration.toFloat() * value).toLong().coerceIn(0L, duration))
                        },
                        onValueChangeFinished = {
                            haptics.tick()
                            draggingProgress = null
                        },
                        modifier = Modifier.fillMaxWidth().height(28.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color.White.copy(alpha = 0.92f),
                            inactiveTrackColor = Color.White.copy(alpha = 0.20f),
                        ),
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(formatDuration((duration.toFloat() * shownProgress).toLong()), color = Color.White.copy(alpha = 0.78f), style = MaterialTheme.typography.bodyMedium)
                        Text(pageIndex.toString() + "/" + total.toString(), color = Color.White.copy(alpha = 0.78f), style = MaterialTheme.typography.bodyMedium)
                        Text(formatDuration(duration), color = Color.White.copy(alpha = 0.78f), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReelActionRail(
    muted: Boolean,
    playing: Boolean,
    deletedCount: Int,
    onTogglePlay: () -> Unit,
    onMute: () -> Unit,
    onKeep: () -> Unit,
    onFavorite: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.width(68.dp),
        shape = RoundedCornerShape(34.dp),
        color = Color.Black.copy(alpha = 0.16f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly,
        ) {
            ReelRailButton(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, if (playing) "暂停" else "播放", onTogglePlay)
            ReelRailButton(if (muted) Icons.AutoMirrored.Rounded.VolumeOff else Icons.AutoMirrored.Rounded.VolumeUp, if (muted) "静音" else "声音", onMute)
            ReelRailButton(Icons.Rounded.FavoriteBorder, "收藏", onFavorite)
            ReelRailButton(Icons.Rounded.Delete, "待删", onDelete, badge = deletedCount.takeIf { it > 0 }?.toString())
            ReelRailButton(Icons.Rounded.Share, "分享", onShare)
            ReelRailButton(Icons.Rounded.CheckCircle, "保留", onKeep)
        }
    }
}

@Composable
private fun ReelRailButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    badge: String? = null,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = RoundedCornerShape(999.dp),
                color = Color.White.copy(alpha = 0.14f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.20f)),
                onClick = onClick,
            ) {
                Box(contentAlignment = Alignment.Center) { Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(21.dp)) }
            }
            if (badge != null) {
                Surface(
                    modifier = Modifier.align(Alignment.TopEnd),
                    shape = RoundedCornerShape(999.dp),
                    color = Color(0xFFE66A6A),
                ) { Text(badge, modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp), color = Color.White, style = MaterialTheme.typography.labelSmall) }
            }
        }
        Text(label, color = Color.White.copy(alpha = 0.76f), style = MaterialTheme.typography.labelSmall)
    }
}

private suspend fun PointerInputScope.detectReelSurfaceGestures(
    haptics: HapticKit,
    seekThreshold: Float,
    onTogglePlay: () -> Unit,
    onToggleControls: () -> Unit,
    onSpeedStart: () -> Unit,
    onSpeedEnd: () -> Unit,
) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        var horizontalMode = false
        var longPressed = false
        var totalX = 0f
        var totalY = 0f
        var thresholdFired = false
        var speedStarted = false
        val slop = viewConfiguration.touchSlop
        val longPressTimeout = viewConfiguration.longPressTimeoutMillis

        while (true) {
            val event = awaitPointerEvent()
            val change = event.changes.firstOrNull { it.id == down.id } ?: break
            if (change.changedToUpIgnoreConsumed()) break

            val delta = change.positionChange()
            if (delta.x != 0f || delta.y != 0f) {
                totalX += delta.x
                totalY += delta.y
            }

            val elapsed = change.uptimeMillis - down.uptimeMillis
            if (!horizontalMode && !longPressed && abs(totalX) < slop && abs(totalY) < slop && elapsed >= longPressTimeout) {
                longPressed = true
                speedStarted = true
                haptics.threshold()
                onSpeedStart()
            }

            if (delta.x != 0f || delta.y != 0f) {
                if (!horizontalMode && !longPressed) {
                    if (abs(totalX) > slop || abs(totalY) > slop) {
                        if (abs(totalX) > abs(totalY) * 1.35f) {
                            horizontalMode = true
                            haptics.tick()
                            change.consume()
                        } else {
                            break
                        }
                    }
                } else if (horizontalMode) {
                    if (!thresholdFired && abs(totalX) > seekThreshold) {
                        thresholdFired = true
                        haptics.threshold()
                    }
                    change.consume()
                }
            }
        }

        if (speedStarted) onSpeedEnd()

        if (horizontalMode) {
            if (abs(totalX) > seekThreshold) {
                onToggleControls()
                haptics.success()
            }
        } else if (!longPressed && abs(totalX) < slop && abs(totalY) < slop) {
            onTogglePlay()
        }
    }
}

