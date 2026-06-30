package com.futureape.kanleme.ui.screens

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.media.AudioManager
import android.os.SystemClock
import android.provider.MediaStore
import androidx.activity.compose.BackHandler
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.futureape.kanleme.data.local.VideoEntity
import com.futureape.kanleme.data.repository.SwipeAction
import com.futureape.kanleme.data.settings.AppSettings
import com.futureape.kanleme.data.settings.VideoDisplayMode
import com.futureape.kanleme.R
import com.futureape.kanleme.ui.components.AdaptiveWidthInfo
import com.futureape.kanleme.ui.components.EmptyState
import com.futureape.kanleme.ui.util.HapticKit
import com.futureape.kanleme.ui.util.rememberHapticKit
import com.futureape.kanleme.ui.util.shareVideo
import com.futureape.kanleme.ui.viewmodel.KanlemeViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import com.futureape.kanleme.ui.i18n.Text

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VideoCleanScreen(viewModel: KanlemeViewModel, onBack: () -> Unit) {
    val rawVideos by viewModel.videoDeck.collectAsStateWithLifecycle()
    val deckPreparing by viewModel.videoDeckPreparing.collectAsStateWithLifecycle()
    val videos = remember(rawVideos) { rawVideos.distinctBy { it.mediaStoreId } }
    val dashboard by viewModel.dashboard.collectAsStateWithLifecycle()
    val scope by viewModel.videoScope.collectAsStateWithLifecycle()
    val folders by viewModel.videoFolders.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val settingsLoaded by viewModel.settingsLoaded.collectAsStateWithLifecycle()
    val videoSessionActionCount by viewModel.videoSessionActionCount.collectAsStateWithLifecycle()
    val lastVideoAction by viewModel.lastVideoAction.collectAsStateWithLifecycle()
    val pendingVideoKeeps by viewModel.pendingVideoKeeps.collectAsStateWithLifecycle()
    val haptics = rememberHapticKit(settings)
    val uiScope = rememberCoroutineScope()
    var showGuide by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showFolderPicker by remember { mutableStateOf(false) }
    val videoChromeVisible = settingsLoaded && settings.videoChromeVisible
    val context = LocalContext.current
    val audioManager = remember(context) { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    var sessionMuted by remember { mutableStateOf<Boolean?>(null) }
    val videoMuted = sessionMuted ?: settings.videoDefaultMuted
    val pagerState = rememberPagerState(pageCount = { videos.size.coerceAtLeast(1) })
    var lastAppliedShuffleSeed by remember { mutableStateOf(scope.randomSeed) }
    var lastSettledPage by remember { mutableIntStateOf(0) }
    val dimAlpha by animateFloatAsState(targetValue = if (showGuide && videos.isNotEmpty()) 0.55f else 0f, label = "video_guide_dim")

    fun leaveCleaning() {
        viewModel.finishVideoCleaningSession(videos.getOrNull(pagerState.currentPage))
        viewModel.resetVideoSessionDateMode()
        onBack()
    }

    BackHandler(onBack = ::leaveCleaning)

    LaunchedEffect(settingsLoaded) {
        if (settingsLoaded && sessionMuted == null) sessionMuted = settings.videoDefaultMuted
    }
    LaunchedEffect(settingsLoaded, videoMuted) {
        if (!settingsLoaded) return@LaunchedEffect
        audioManager.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            if (videoMuted) AudioManager.ADJUST_MUTE else AudioManager.ADJUST_UNMUTE,
            0,
        )
    }
    LaunchedEffect(scope.folderPaths, scope.dateMode, scope.sortOrder, settings.excludedFolderPaths) { viewModel.loadVideoDeck(scope) }
    LaunchedEffect(videos.isEmpty(), deckPreparing, dashboard.videoCount, dashboard.processedVideoCount, scope) {
        if (videos.isEmpty() && !deckPreparing && dashboard.processedVideoCount < dashboard.videoCount) viewModel.loadVideoDeck(scope)
    }
    LaunchedEffect(scope.randomSeed, videos.firstOrNull()?.mediaStoreId) {
        if (scope.sortOrder == "random" && scope.randomSeed != lastAppliedShuffleSeed && videos.isNotEmpty()) {
            pagerState.scrollToPage(0)
            lastAppliedShuffleSeed = scope.randomSeed
        }
        lastSettledPage = pagerState.currentPage
    }
    LaunchedEffect(videos, settings.videoGuideShown) {
        showGuide = false
    }
    LaunchedEffect(videos.size) {
        if (videos.isNotEmpty() && pagerState.currentPage >= videos.size) {
            pagerState.scrollToPage((videos.size - 1).coerceAtLeast(0))
        }
    }
    LaunchedEffect(pagerState.settledPage, videos) {
        val currentPage = pagerState.settledPage
        if (videos.isNotEmpty() && currentPage > lastSettledPage) {
            (lastSettledPage until currentPage).forEach { index ->
                videos.getOrNull(index)?.let { viewModel.markVideoPendingKeep(it) }
            }
        }
        lastSettledPage = currentPage
        haptics.tick()
    }

    fun performVideoAction(video: VideoEntity, action: SwipeAction) {
        when (action) {
            SwipeAction.Keep -> haptics.keep()
            SwipeAction.Delete -> haptics.delete()
            SwipeAction.Favorite -> haptics.favorite()
        }
        uiScope.launch {
            if (videos.size > 1 && pagerState.currentPage >= videos.lastIndex) {
                pagerState.scrollToPage((videos.lastIndex - 1).coerceAtLeast(0))
            }
            viewModel.onVideoAction(video, action)
        }
    }

    if (videos.isEmpty()) {
        Column(Modifier.fillMaxSize().padding(top = 36.dp)) {
            ScreenHeader("视频整理", "单击播放/暂停，左右滑切换沉浸", ::leaveCleaning)
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

    val currentVideo = videos.getOrNull(pagerState.currentPage)
    val activeVideoPage = pagerState.targetPage.coerceIn(0, videos.lastIndex)

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
                    beyondViewportPageCount = 1,
                    key = { page -> videos.getOrNull(page)?.mediaStoreId ?: page },
                ) { page ->
                    val video = videos.getOrNull(page) ?: return@VerticalPager
                    VideoReelPage(
                        video = video,
                        settings = settings,
                        haptics = haptics,
                        shouldPrepare = abs(page - activeVideoPage) <= 1,
                        isCurrent = page == activeVideoPage,
                        pageIndex = (dashboard.processedVideoCount + page + 1).coerceAtLeast(1),
                        total = dashboard.videoCount.coerceAtLeast(videos.size),
                        sessionActionCount = videoSessionActionCount,
                        lastAction = lastVideoAction,
                        chromeVisible = videoChromeVisible,
                        muted = videoMuted,
                        onFavorite = { performVideoAction(video, SwipeAction.Favorite) },
                        onDelete = { performVideoAction(video, SwipeAction.Delete) },
                        onChromeVisibilityChange = { visible -> viewModel.setVideoChromeVisible(visible) },
                        onMuteChange = { muted -> sessionMuted = muted },
                        onUndo = {
                            haptics.undo()
                            val mediaStoreId = viewModel.undoVideoCleaningAction()
                            if (mediaStoreId != null) {
                                val index = videos.indexOfFirst { it.mediaStoreId == mediaStoreId }
                                if (index >= 0) uiScope.launch { pagerState.animateScrollToPage(index) }
                            }
                        },
                    )
                }
            }
        }

        androidx.compose.animation.AnimatedVisibility(
            visible = videoChromeVisible && (settings.videoShowTopBar || settings.videoShowShuffleButton),
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            VideoOrganizerTopRow(
                    progressPercent = if (dashboard.videoCount > 0) (((dashboard.processedVideoCount + pendingVideoKeeps.size) * 100f) / dashboard.videoCount).toInt().coerceIn(0, 100) else 0,
                    remainingCount = (dashboard.videoCount - dashboard.processedVideoCount - pendingVideoKeeps.size).coerceAtLeast(0),
                    deletedSizeBytes = dashboard.pendingDeleteBytes,
                    displayMode = settings.videoDisplayMode,
                    dateMode = scope.dateMode,
                    showShuffle = settings.videoShowShuffleButton,
                    onToggleDisplayMode = { haptics.tick(); viewModel.toggleVideoDisplayModeQuick() },
                    onDateClick = { haptics.tick(); showDatePicker = true },
                    onFolderClick = { haptics.tick(); showFolderPicker = true },
                    onShuffle = { haptics.threshold(); viewModel.reshuffleVideoCleaningSession() },
                )
        }

        if (dimAlpha > 0.01f) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = dimAlpha)))
        androidx.compose.animation.AnimatedVisibility(visible = showGuide, modifier = Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                VideoGuideDialog(onDismiss = { haptics.tick(); showGuide = false; viewModel.markVideoGuideShown() })
            }
        }
        OrganizerDatePickerAnimatedOverlay(
            visible = showDatePicker,
            title = "视频时间筛选",
            currentMode = scope.dateMode,
            onApply = { mode -> haptics.tick(); viewModel.setVideoSessionDateMode(mode) },
            onDismiss = { showDatePicker = false },
        )
        if (currentVideo != null) {
            OrganizerFolderPickerOverlay(
                visible = showFolderPicker,
                title = "归档当前视频",
                folders = folders.ifEmpty { listOf("DCIM/Camera/", "Movies/", "Download/") },
                onArchive = { path -> haptics.keep(); viewModel.archiveVideoToFolder(currentVideo, path) },
                onDismiss = { showFolderPicker = false },
            )
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
    progressPercent: Int,
    remainingCount: Int,
    deletedSizeBytes: Long,
    displayMode: VideoDisplayMode,
    dateMode: String,
    showShuffle: Boolean,
    onToggleDisplayMode: () -> Unit,
    onDateClick: () -> Unit,
    onFolderClick: () -> Unit,
    onShuffle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .statusBarsPadding()
            .padding(vertical = 12.dp)
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        VideoTopPill(
            label = if (displayMode == VideoDisplayMode.FIT_SCREEN) "原比例" else "全屏",
            onClick = onToggleDisplayMode,
        )
        VideoTopPill(
            label = organizerDateModeLabel(dateMode),
            icon = Icons.Rounded.CalendarToday,
            emphasized = dateMode != "all",
            onClick = onDateClick,
        )
        VideoTopPill(
            label = "指定归档",
            icon = Icons.Rounded.Folder,
            onClick = onFolderClick,
        )
        OrganizerProgressPill(
            progressPercent = progressPercent,
            remainingCount = remainingCount,
            remainingLabel = "个",
            releasableBytes = deletedSizeBytes,
            modifier = Modifier.width(94.dp),
            compact = true,
        )
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
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    emphasized: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    Surface(
        modifier = Modifier
            .height(44.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
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
            if (icon != null) Icon(icon, contentDescription = null, modifier = Modifier.size(17.dp))
            Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun VideoReelPage(
    video: VideoEntity,
    settings: AppSettings,
    haptics: HapticKit,
    shouldPrepare: Boolean,
    isCurrent: Boolean,
    pageIndex: Int,
    total: Int,
    sessionActionCount: Int,
    lastAction: SwipeAction?,
    chromeVisible: Boolean,
    muted: Boolean,
    onFavorite: () -> Unit,
    onDelete: () -> Unit,
    onChromeVisibilityChange: (Boolean) -> Unit,
    onMuteChange: (Boolean) -> Unit,
    onUndo: () -> Unit,
) {
    val context = LocalContext.current
    val playbackUris = remember(video.uri, video.mediaStoreId) {
        listOfNotNull(
            video.uri.takeIf { it.isNotBlank() }?.let { Uri.parse(it) },
            video.mediaStoreId.takeIf { it > 0L }?.let { ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, it) },
        ).distinctBy { it.toString() }
    }
    var playbackUriIndex by remember(video.id, playbackUris) { mutableIntStateOf(0) }
    val playbackUri = playbackUris.getOrNull(playbackUriIndex.coerceIn(0, playbackUris.lastIndex.coerceAtLeast(0))) ?: Uri.EMPTY
    val player = remember(playbackUris, shouldPrepare) {
        if (!shouldPrepare || playbackUris.isEmpty()) {
            null
        } else {
            runCatching {
                val renderersFactory = DefaultRenderersFactory(context).setEnableDecoderFallback(true)
                ExoPlayer.Builder(context, renderersFactory).build().apply {
                    repeatMode = Player.REPEAT_MODE_ONE
                }
            }.getOrNull()
        }
    }
    var shouldPlay by remember(video.id) { mutableStateOf(true) }
    var playbackFailed by remember(video.id) { mutableStateOf(false) }
    var lastTapMillis by remember(video.id) { mutableStateOf(0L) }
    var actionOffsetY by remember(video.id) { mutableStateOf(0f) }

    LaunchedEffect(player, playbackUri) {
        if (player != null && playbackUri != Uri.EMPTY) {
            playbackFailed = false
            player.setMediaItem(MediaItem.fromUri(playbackUri))
            player.prepare()
        }
    }
    LaunchedEffect(player, muted, isCurrent, shouldPlay) {
        player?.playWhenReady = isCurrent && shouldPlay
        player?.volume = 1f
    }
    DisposableEffect(player, playbackUris, playbackUriIndex) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                if (playbackUriIndex < playbackUris.lastIndex) {
                    playbackUriIndex += 1
                } else {
                    playbackFailed = true
                    player?.playWhenReady = false
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) playbackFailed = false
            }
        }
        player?.addListener(listener)
        onDispose {
            player?.removeListener(listener)
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
        AsyncImage(
            model = playbackUri,
            contentDescription = video.displayName,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        if (player != null && !playbackFailed) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = {
                    PlayerView(it).apply {
                        useController = false
                        setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                        setKeepContentOnPlayerReset(true)
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
                    view.setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                    view.setKeepContentOnPlayerReset(true)
                    view.resizeMode = when (settings.videoDisplayMode) {
                        VideoDisplayMode.IMMERSIVE_CROP -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        VideoDisplayMode.FIT_SCREEN -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                        VideoDisplayMode.FILL_WIDTH -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
                    }
                },
            )
        } else {
            AsyncImage(
                model = playbackUri,
                contentDescription = video.displayName,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }

        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(video.id, settings.videoSeekThresholdPx, player, chromeVisible) {
                    detectReelSurfaceGestures(
                        haptics = haptics,
                        seekThreshold = settings.videoSeekThresholdPx,
                        onSurfaceTap = onSurfaceTap@{
                            if (playbackFailed) {
                                playbackFailed = false
                                playbackUriIndex = 0
                                shouldPlay = true
                                player?.prepare()
                                haptics.tick()
                                return@onSurfaceTap
                            }
                            val now = SystemClock.uptimeMillis()
                            if (now - lastTapMillis < 280L) {
                                onChromeVisibilityChange(!chromeVisible)
                                lastTapMillis = 0L
                            } else {
                                lastTapMillis = now
                                shouldPlay = !shouldPlay
                            }
                            haptics.tick()
                        },
                        onSeekBy = { deltaMillis ->
                            val target = ((player?.currentPosition ?: position) + deltaMillis).coerceIn(0L, duration)
                            player?.seekTo(target)
                        },
                        onSpeedStart = { player?.setPlaybackSpeed(2f) },
                        onSpeedEnd = { player?.setPlaybackSpeed(1f) },
                    )
                }
        )

        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.06f)))
        androidx.compose.animation.AnimatedVisibility(
            visible = playbackFailed && isCurrent,
            enter = fadeIn(tween(180, easing = LinearOutSlowInEasing)),
            exit = fadeOut(tween(140, easing = FastOutSlowInEasing)),
            modifier = Modifier.align(Alignment.Center),
        ) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = Color.Black.copy(alpha = 0.46f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.20f)),
            ) {
                Text(
                    "无法播放，点按重试",
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        androidx.compose.animation.AnimatedVisibility(
            visible = !shouldPlay && isCurrent,
            enter = fadeIn(tween(220, easing = LinearOutSlowInEasing)) + scaleIn(tween(260, easing = FastOutSlowInEasing), initialScale = 0.86f),
            exit = fadeOut(tween(180, easing = FastOutSlowInEasing)) + scaleOut(tween(180, easing = FastOutSlowInEasing), targetScale = 0.92f),
            modifier = Modifier.align(Alignment.Center),
        ) {
            Surface(
                modifier = Modifier.size(78.dp),
                shape = RoundedCornerShape(999.dp),
                color = Color.Black.copy(alpha = 0.24f),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(43.dp), tint = Color.White.copy(alpha = 0.92f))
                }
            }
        }
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

        androidx.compose.animation.AnimatedVisibility(
            visible = settings.videoShowActionRail,
            enter = fadeIn() + slideInHorizontally { it / 3 },
            exit = fadeOut() + slideOutHorizontally { it / 3 },
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp)
                .offset { IntOffset(0, actionOffsetY.roundToInt()) }
                .pointerInput(video.id) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        actionOffsetY = (actionOffsetY + dragAmount.y).coerceIn(-230f, 230f)
                    }
                },
        ) {
            ReelActionRail(
                muted = muted,
                chromeVisible = chromeVisible,
                sessionActionCount = sessionActionCount,
                lastAction = lastAction,
                onToggleChrome = {
                    haptics.tick()
                    onChromeVisibilityChange(!chromeVisible)
                },
                onMute = {
                    haptics.tick()
                    onMuteChange(!muted)
                },
                onFavorite = onFavorite,
                onDelete = onDelete,
                onUndo = onUndo,
                onShare = { haptics.tick(); shareVideo(context, video) },
                modifier = Modifier.width(58.dp),
            )
        }

        androidx.compose.animation.AnimatedVisibility(
            visible = chromeVisible && (settings.videoShowInfoPanel || settings.videoShowProgressBar),
            enter = fadeIn() + slideInVertically { it / 5 },
            exit = fadeOut() + slideOutVertically { it / 5 },
            modifier = Modifier.align(Alignment.BottomStart),
        ) {
            Column(Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (settings.videoShowInfoPanel) {
                    Column {
                        Text(simpleAgeLabel(video.dateTaken), style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Black, maxLines = 1)
                        Text(video.folderName.ifBlank { "本地相册" }, style = MaterialTheme.typography.titleMedium, color = Color.White.copy(alpha = 0.72f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                if (settings.videoShowProgressBar) {
                    ThinVideoScrubber(
                        progress = shownProgress,
                        onValueChange = { value ->
                            draggingProgress = value
                            player?.seekTo((duration.toFloat() * value).toLong().coerceIn(0L, duration))
                        },
                        onValueChangeFinished = {
                            haptics.tick()
                            draggingProgress = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun ThinVideoScrubber(
    progress: Float,
    modifier: Modifier = Modifier,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
) {
    var dragging by remember { mutableStateOf(false) }
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        label = "thin_video_scrubber",
    )
    Canvas(
        modifier = modifier
            .height(22.dp)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    dragging = true
                    fun updateFromX(x: Float) {
                        val width = size.width.toFloat().coerceAtLeast(1f)
                        onValueChange((x / width).coerceIn(0f, 1f))
                    }
                    updateFromX(down.position.x)
                    down.consume()
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (change.changedToUpIgnoreConsumed()) {
                            change.consume()
                            break
                        }
                        val delta = change.positionChange()
                        if (delta.x != 0f || delta.y != 0f) {
                            updateFromX(change.position.x)
                            change.consume()
                        }
                    }
                    dragging = false
                    onValueChangeFinished()
                }
            },
    ) {
        val trackHeight = 2.2.dp.toPx()
        val activeHeight = if (dragging) 3.dp.toPx() else trackHeight
        val glowHeight = if (dragging) 8.dp.toPx() else 6.dp.toPx()
        val centerY = size.height / 2f
        val radius = trackHeight / 2f
        val activeWidth = size.width * animatedProgress.coerceIn(0f, 1f)
        drawRoundRect(
            color = Color.White.copy(alpha = 0.18f),
            topLeft = Offset(0f, centerY - trackHeight / 2f),
            size = Size(size.width, trackHeight),
            cornerRadius = CornerRadius(radius, radius),
        )
        drawRoundRect(
            color = Color(0xFF86A6FF).copy(alpha = 0.30f),
            topLeft = Offset(0f, centerY - glowHeight / 2f),
            size = Size(activeWidth, glowHeight),
            cornerRadius = CornerRadius(glowHeight / 2f, glowHeight / 2f),
        )
        drawRoundRect(
            color = Color.White.copy(alpha = 0.88f),
            topLeft = Offset(0f, centerY - activeHeight / 2f),
            size = Size(activeWidth, activeHeight),
            cornerRadius = CornerRadius(activeHeight / 2f, activeHeight / 2f),
        )
    }
}

private fun simpleAgeLabel(timeMillis: Long): String {
    val normalized = if (timeMillis in 1L..10_000_000_000L) timeMillis * 1000L else timeMillis
    if (normalized <= 0L) return "本地视频"
    val days = ((System.currentTimeMillis() - normalized).coerceAtLeast(0L) / (24L * 60L * 60L * 1000L)).toInt()
    return when {
        days <= 0 -> "今天"
        days < 30 -> days.toString() + " 天前"
        days < 365 -> (days / 30).coerceAtLeast(1).toString() + " 个月前"
        else -> (days / 365).coerceAtLeast(1).toString() + " 年前"
    }
}

@Composable
private fun ReelActionRail(
    muted: Boolean,
    chromeVisible: Boolean,
    sessionActionCount: Int,
    lastAction: SwipeAction?,
    onToggleChrome: () -> Unit,
    onMute: () -> Unit,
    onFavorite: () -> Unit,
    onDelete: () -> Unit,
    onUndo: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = Color.Black.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.20f)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ReelUndoButton(
                count = sessionActionCount,
                onClick = onUndo,
            )
            ReelRailButton(if (chromeVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility, if (chromeVisible) "隐藏" else "显示", onToggleChrome)
            ReelRailButton(if (muted) Icons.AutoMirrored.Rounded.VolumeOff else Icons.AutoMirrored.Rounded.VolumeUp, if (muted) "静音" else "声音", onMute)
            ReelRailButton(Icons.Rounded.FavoriteBorder, "收藏", onFavorite)
            ReelRailButton(Icons.Rounded.Delete, "待删", onDelete)
            ReelRailButton(Icons.Rounded.Share, "分享", onShare)
        }
    }
}

@Composable
private fun ReelUndoButton(
    count: Int,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.size(44.dp),
        shape = RoundedCornerShape(999.dp),
        color = Color.White.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
        onClick = onClick,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(
                count.toString(),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Black,
                maxLines = 1,
            )
            Icon(
                Icons.AutoMirrored.Rounded.Undo,
                contentDescription = stringResource(R.string.a11y_back_to_previous_video),
                modifier = Modifier.size(13.dp),
                tint = Color.White.copy(alpha = 0.90f),
            )
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
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = RoundedCornerShape(999.dp),
                color = Color.White.copy(alpha = 0.12f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
                onClick = onClick,
            ) {
                Box(contentAlignment = Alignment.Center) { Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(22.dp)) }
            }
            if (badge != null) {
                Surface(
                    modifier = Modifier.align(Alignment.TopEnd),
                    shape = RoundedCornerShape(999.dp),
                    color = Color(0xFFE66A6A),
                ) { Text(badge, modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp), color = Color.White, style = MaterialTheme.typography.labelSmall) }
            }
        }
    }
}

private suspend fun PointerInputScope.detectReelSurfaceGestures(
    haptics: HapticKit,
    seekThreshold: Float,
    onSurfaceTap: () -> Unit,
    onSeekBy: (Long) -> Unit,
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
                onSeekBy((totalX * 80f).toLong())
                haptics.success()
            }
        } else if (!longPressed && abs(totalX) < slop && abs(totalY) < slop) {
            onSurfaceTap()
        }
    }
}
