package com.futureape.kanleme.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FilterAlt
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Swipe
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import com.futureape.kanleme.data.local.PhotoEntity
import com.futureape.kanleme.data.repository.SwipeAction
import com.futureape.kanleme.data.settings.AppVisualStyle
import com.futureape.kanleme.R
import com.futureape.kanleme.ui.components.AdaptiveWidthInfo
import com.futureape.kanleme.ui.components.EmptyState
import com.futureape.kanleme.ui.util.rememberHapticKit
import com.futureape.kanleme.ui.util.formatDate
import com.futureape.kanleme.ui.util.formatSize
import com.futureape.kanleme.ui.util.photoMediaKindLabel
import com.futureape.kanleme.ui.util.sharePhoto
import com.futureape.kanleme.ui.viewmodel.KanlemeViewModel
import com.futureape.kanleme.ui.i18n.Text
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlinx.coroutines.launch

@Composable
fun PhotoCleanScreen(
    viewModel: KanlemeViewModel,
    onBack: () -> Unit,
    onOpenPhoto: (PhotoEntity) -> Unit,
    onBatchFinished: () -> Unit,
) {
    val deck by viewModel.photoDeck.collectAsStateWithLifecycle()
    val timelinePhotos by viewModel.timelinePhotos.collectAsStateWithLifecycle()
    val deckPreparing by viewModel.photoDeckPreparing.collectAsStateWithLifecycle()
    val dashboard by viewModel.dashboard.collectAsStateWithLifecycle()
    val typeStats by viewModel.photoTypeStats.collectAsStateWithLifecycle()
    val scope by viewModel.photoScope.collectAsStateWithLifecycle()
    val folders by viewModel.photoFolders.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val sessionActionCount by viewModel.photoSessionActionCount.collectAsStateWithLifecycle()
    val lastPhotoAction by viewModel.lastPhotoAction.collectAsStateWithLifecycle()
    val undoAnimation by viewModel.photoUndoAnimation.collectAsStateWithLifecycle()
    val haptics = rememberHapticKit(settings)
    val context = LocalContext.current
    val chromeVisible = !settings.photoFocusMode
    var showGuide by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showFolderPicker by remember { mutableStateOf(false) }
    var showDayMemory by rememberSaveable { mutableStateOf(false) }
    var photoSwipeFeedback by remember { mutableStateOf(PhotoSwipeFeedback()) }
    var guideTargets by remember { mutableStateOf<Map<PhotoGuideTarget, Rect>>(emptyMap()) }
    var batchFinishing by remember { mutableStateOf(false) }
    var batchKeepCount by rememberSaveable { mutableIntStateOf(0) }
    var batchFavoriteCount by rememberSaveable { mutableIntStateOf(0) }
    var batchDeleteCount by rememberSaveable { mutableIntStateOf(0) }
    var batchDeleteBytes by rememberSaveable { mutableStateOf(0L) }
    var lastBatchAction by remember { mutableStateOf<SwipeAction?>(null) }
    var lastBatchDeleteBytes by rememberSaveable { mutableStateOf(0L) }
    var batchSummary by remember { mutableStateOf<PhotoBatchSummary?>(null) }
    var dayMemoryPinch by remember { mutableStateOf(DayMemoryPinchState()) }

    fun clearBatchCounters() {
        batchKeepCount = 0
        batchFavoriteCount = 0
        batchDeleteCount = 0
        batchDeleteBytes = 0L
        lastBatchAction = null
        lastBatchDeleteBytes = 0L
    }

    fun closeDayMemory() {
        showDayMemory = false
        dayMemoryPinch = DayMemoryPinchState()
    }

    fun previewDayMemory(progress: Float, scale: Float) {
        dayMemoryPinch = DayMemoryPinchState(progress = progress, scale = scale, active = true)
    }

    fun commitDayMemory() {
        if (dayMemoryPinch.progress > 0.04f) {
            showDayMemory = true
            dayMemoryPinch = dayMemoryPinch.copy(progress = 1f, active = false)
            haptics.threshold()
        } else {
            dayMemoryPinch = DayMemoryPinchState()
        }
    }

    fun leaveCleaning() {
        viewModel.resetPhotoSessionDateMode()
        viewModel.finishPhotoCleaningSession()
        clearBatchCounters()
        onBack()
    }

    BackHandler(onBack = ::leaveCleaning)

    LaunchedEffect(batchFinishing) {
        if (!batchFinishing) return@LaunchedEffect
        kotlinx.coroutines.delay(650)
        batchSummary = PhotoBatchSummary(
            total = batchKeepCount + batchFavoriteCount + batchDeleteCount,
            kept = batchKeepCount,
            favorited = batchFavoriteCount,
            deleted = batchDeleteCount,
            deletedBytes = batchDeleteBytes,
            lastAction = lastBatchAction,
            lastDeletedBytes = lastBatchDeleteBytes,
        )
        batchFinishing = false
    }

    fun undoLastBatchAction(summary: PhotoBatchSummary) {
        viewModel.undoPhotoCleaningAction()
        when (summary.lastAction) {
            SwipeAction.Keep -> batchKeepCount = (batchKeepCount - 1).coerceAtLeast(0)
            SwipeAction.Favorite -> batchFavoriteCount = (batchFavoriteCount - 1).coerceAtLeast(0)
            SwipeAction.Delete -> {
                batchDeleteCount = (batchDeleteCount - 1).coerceAtLeast(0)
                batchDeleteBytes = (batchDeleteBytes - summary.lastDeletedBytes).coerceAtLeast(0L)
            }
            null -> Unit
        }
        lastBatchAction = null
        lastBatchDeleteBytes = 0L
        batchSummary = null
    }

    fun updateGuideTarget(target: PhotoGuideTarget, rect: Rect) {
        guideTargets = guideTargets + (target to rect)
    }

    LaunchedEffect(Unit) {
        // Do not reload a non-empty deck when returning from the viewer.
        // In random mode, reloading would rebuild the deck and make the user lose
        // the first random sequence they were already browsing.
        if (deck.isEmpty() && !deckPreparing) viewModel.loadPhotoDeck(scope)
    }
    LaunchedEffect(deck.isEmpty(), deckPreparing, dashboard.photoCount, dashboard.processedPhotoCount, scope) {
        if (deck.isEmpty() && !deckPreparing && dashboard.processedPhotoCount < dashboard.photoCount) viewModel.loadPhotoDeck(scope)
    }
    LaunchedEffect(deck, settings.photoGuideShown) {
        showGuide = false
        deck.take(3).forEach { photo ->
            context.imageLoader.enqueue(
                ImageRequest.Builder(context)
                    .data(Uri.parse(photo.uri))
                    .memoryCacheKey(photo.uri)
                    .diskCacheKey(photo.uri)
                    .build()
            )
        }
    }

    if (deck.isEmpty() && (batchSummary != null || batchFinishing)) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            batchSummary?.let { summary ->
                PhotoBatchSummaryOverlay(
                    summary = summary,
                    onContinue = {
                        viewModel.finishPhotoCleaningSession()
                        batchSummary = null
                        clearBatchCounters()
                    },
                    onBackHome = {
                        viewModel.finishPhotoCleaningSession()
                        batchSummary = null
                        viewModel.resetPhotoSessionDateMode()
                        clearBatchCounters()
                        onBack()
                    },
                    onOpenTrash = {
                        viewModel.finishPhotoCleaningSession()
                        batchSummary = null
                        viewModel.resetPhotoSessionDateMode()
                        clearBatchCounters()
                        onBatchFinished()
                    },
                    onUndoLast = { undoLastBatchAction(summary) },
                )
            }
        }
        return
    }

    if (deck.isEmpty() && deckPreparing) {
        Box(
            Modifier
                .fillMaxSize()
                .background(if (settings.appVisualStyle == AppVisualStyle.IMMERSIVE_PHOTO) Color.Black else MaterialTheme.colorScheme.background)
                .statusBarsPadding()
                .padding(18.dp),
        ) {
            if (settings.appVisualStyle == AppVisualStyle.IMMERSIVE_PHOTO) {
                KeepixRoundButton(
                    icon = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                    contentDescription = stringResource(R.string.a11y_back),
                    onClick = ::leaveCleaning,
                    modifier = Modifier.align(Alignment.TopStart),
                )
            }
        }
        return
    }

    if (deck.isEmpty()) {
        // New Keepix empty state uses the immersive controls from ImmersivePhotoCleanScreen.kt.
        if (settings.appVisualStyle == AppVisualStyle.IMMERSIVE_PHOTO) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .statusBarsPadding()
                    .padding(18.dp),
            ) {
                KeepixRoundButton(
                    icon = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                    contentDescription = stringResource(R.string.a11y_back),
                    onClick = ::leaveCleaning,
                    modifier = Modifier.align(Alignment.TopStart),
                )
                Column(
                    modifier = Modifier.align(Alignment.Center).padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        "当前没有待整理照片",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "可以回到首页调整整理范围",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.58f),
                        textAlign = TextAlign.Center,
                    )
                }
            }
            return
        }
        Column(Modifier.fillMaxSize().padding(top = 36.dp)) {
            ScreenHeader("照片整理", "按每组数量完成一轮整理", ::leaveCleaning)
            Box(Modifier.fillMaxSize().padding(18.dp), contentAlignment = Alignment.Center) {
                EmptyState(
                    title = "当前筛选下暂无待整理照片",
                    message = "这里为空通常表示当前筛选、排除文件夹或相册权限下已经没有可整理内容。",
                    actionText = "重新读取队列",
                    onAction = { haptics.success(); if (dashboard.photoCount == 0) viewModel.refreshLibrary() else viewModel.loadPhotoDeck(scope) },
                )
            }
        }
        return
    }

    val currentPhoto = deck.first()
    val memoryPhotos = timelinePhotos.ifEmpty { deck }
    val remainingPhotos = (dashboard.photoCount - dashboard.processedPhotoCount).coerceAtLeast(deck.size)
    val remainingInBatch = (settings.photoBatchSize.coerceIn(1, 100) - sessionActionCount).coerceAtLeast(1)
    val visibleDeck = deck.take(remainingInBatch.coerceAtMost(3))
    val dayMemoryVisible = showDayMemory || dayMemoryPinch.progress > 0.001f
    val dimAlpha by animateFloatAsState(targetValue = if (showGuide) 0.46f else 0f, label = "guide_dim")

    fun perform(photo: PhotoEntity, action: SwipeAction, exitTargetX: Float, exitTargetY: Float) {
        if (batchFinishing) return
        when (action) {
            SwipeAction.Keep -> haptics.keep()
            SwipeAction.Delete -> haptics.delete()
            SwipeAction.Favorite -> haptics.favorite()
        }
        when (action) {
            SwipeAction.Keep -> batchKeepCount += 1
            SwipeAction.Favorite -> batchFavoriteCount += 1
            SwipeAction.Delete -> {
                batchDeleteCount += 1
                batchDeleteBytes += photo.size
            }
        }
        lastBatchAction = action
        lastBatchDeleteBytes = if (action == SwipeAction.Delete) photo.size else 0L
        viewModel.onPhotoAction(photo, action, exitTargetX, exitTargetY)
        val nextActionCount = sessionActionCount + 1
        if (nextActionCount >= settings.photoBatchSize.coerceIn(1, 100)) {
            batchFinishing = true
        }
    }

    val progressPercent = if (dashboard.photoCount > 0) {
        ((dashboard.processedPhotoCount * 100f) / dashboard.photoCount).toInt().coerceIn(0, 100)
    } else {
        0
    }

    // New Keepix visual path lives in ImmersivePhotoCleanScreen.kt.
    if (settings.appVisualStyle == AppVisualStyle.IMMERSIVE_PHOTO) {
        Box(
            Modifier
                .fillMaxSize()
                .detectScreenPinchToMemory(
                    key = currentPhoto.id,
                    disabled = showDayMemory,
                    onProgress = ::previewDayMemory,
                    onCommit = ::commitDayMemory,
                    onCancel = { dayMemoryPinch = DayMemoryPinchState() },
                ),
        ) {
            ImmersivePhotoCleanContent(
                photos = visibleDeck,
                currentPhoto = currentPhoto,
                settings = settings,
                haptics = haptics,
                progressPercent = progressPercent,
                remaining = remainingPhotos,
                sessionActionCount = sessionActionCount,
                undoAnimation = undoAnimation,
                onBack = ::leaveCleaning,
                onOpenPhoto = { photo -> haptics.tick(); onOpenPhoto(photo) },
                onSwipeFeedbackChanged = { photoSwipeFeedback = it },
                photoSwipeFeedback = photoSwipeFeedback,
                onOpenDayMemory = {
                    dayMemoryPinch = DayMemoryPinchState(progress = 1f, scale = 1f, active = false)
                    showDayMemory = true
                },
                onTopCardPositioned = { rect -> updateGuideTarget(PhotoGuideTarget.PhotoCard, rect) },
                onUndoAnimationConsumed = { sequence -> viewModel.clearPhotoUndoAnimation(sequence) },
                onAction = { photo, action, _, _, targetX, targetY -> perform(photo, action, targetX, targetY) },
                onShare = { haptics.tick(); sharePhoto(context, currentPhoto) },
                onUndo = { haptics.undo(); viewModel.undoPhotoCleaningAction() },
                onToggleFocus = { haptics.tick(); viewModel.setPhotoFocusMode(!settings.photoFocusMode) },
                onDateClick = { haptics.tick(); showDatePicker = true },
                onFolderClick = { haptics.tick(); showFolderPicker = true },
            )
            if (dimAlpha > 0.01f) {
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = dimAlpha)))
            }
            androidx.compose.animation.AnimatedVisibility(visible = showGuide, modifier = Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    PhotoGuideDialog(onDismiss = { haptics.tick(); showGuide = false; viewModel.markPhotoGuideShown() })
                }
            }
            OrganizerDatePickerAnimatedOverlay(
                visible = showDatePicker,
                title = "照片时间筛选",
                currentMode = scope.dateMode,
                onApply = { mode -> haptics.tick(); viewModel.setPhotoSessionDateMode(mode) },
                onDismiss = { showDatePicker = false },
            )
            OrganizerFolderPickerOverlay(
                visible = showFolderPicker,
                title = "归档当前照片",
                folders = folders.ifEmpty { listOf("DCIM/Camera/", "Pictures/", "Download/") },
                onArchive = { path -> haptics.keep(); viewModel.archivePhotoToFolder(currentPhoto, path) },
                onDismiss = { showFolderPicker = false },
            )
            KeepixDayMemoryOverlay(
                visible = dayMemoryVisible,
                currentPhoto = currentPhoto,
                photos = memoryPhotos,
                entryProgress = if (showDayMemory) 1f else dayMemoryPinch.progress,
                entryScale = dayMemoryPinch.scale,
                entryActive = dayMemoryPinch.active,
                onDismiss = ::closeDayMemory,
                onOpen = { photo -> haptics.tick(); onOpenPhoto(photo) },
                onDelete = { photo -> haptics.delete(); viewModel.markPhotoForTrashOutsideCleaning(photo) },
                onUndo = { haptics.undo(); viewModel.undoPhotoCleaningAction() },
                onApply = { mode ->
                    haptics.threshold()
                    viewModel.setPhotoSessionDateMode(mode)
                    closeDayMemory()
                },
            )
            batchSummary?.let { summary ->
                PhotoBatchSummaryOverlay(
                    summary = summary,
                    onContinue = {
                        viewModel.finishPhotoCleaningSession()
                        batchSummary = null
                        clearBatchCounters()
                    },
                    onBackHome = {
                        viewModel.finishPhotoCleaningSession()
                        batchSummary = null
                        viewModel.resetPhotoSessionDateMode()
                        clearBatchCounters()
                        onBack()
                    },
                    onOpenTrash = {
                        viewModel.finishPhotoCleaningSession()
                        batchSummary = null
                        viewModel.resetPhotoSessionDateMode()
                        clearBatchCounters()
                        onBatchFinished()
                    },
                    onUndoLast = { undoLastBatchAction(summary) },
                )
            }
        }
        return
    }

    // Legacy Liquid Glass photo cleaning path. Keep old visual code here only.
    Box(
        Modifier
            .fillMaxSize()
            .detectScreenPinchToMemory(
                key = currentPhoto.id,
                disabled = showDayMemory,
                onProgress = ::previewDayMemory,
                onCommit = ::commitDayMemory,
                onCancel = { dayMemoryPinch = DayMemoryPinchState() },
            ),
    ) {
        Crossfade(
            targetState = currentPhoto.uri,
            animationSpec = tween(durationMillis = 560),
            label = "photo_background_crossfade",
        ) { photoUri ->
            Box(Modifier.fillMaxSize().background(Color.Black)) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(Uri.parse(photoUri))
                        .memoryCacheKey(photoUri)
                        .diskCacheKey(photoUri)
                        .placeholderMemoryCacheKey(photoUri)
                        .crossfade(false)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().blur(18.dp),
                    contentScale = ContentScale.Crop,
                )
            }
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = if (chromeVisible) 0.24f else 0.10f),
                            Color.Black.copy(alpha = if (chromeVisible) 0.10f else 0.04f),
                            Color.Black.copy(alpha = if (chromeVisible) 0.30f else 0.12f),
                        )
                    )
                )
        )
        AdaptiveWidthInfo { _, isExpanded ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(horizontal = if (isExpanded) 34.dp else 18.dp, vertical = 10.dp),
            ) {
                PhotoDeckStage(
                    photos = visibleDeck,
                    settings = settings,
                    haptics = haptics,
                    modifier = (if (isExpanded) {
                        Modifier
                            .align(Alignment.Center)
                            .widthIn(max = 720.dp)
                            .fillMaxWidth()
                            .zIndex(1f)
                    } else {
                        Modifier
                            .align(Alignment.Center)
                            .fillMaxWidth()
                            .zIndex(1f)
                    }),
                    onOpen = { photo -> haptics.tick(); onOpenPhoto(photo) },
                    onTopCardPositioned = { rect -> updateGuideTarget(PhotoGuideTarget.PhotoCard, rect) },
                    onSwipeFeedbackChanged = { photoSwipeFeedback = it },
                    undoAnimation = undoAnimation,
                    onUndoAnimationConsumed = { sequence -> viewModel.clearPhotoUndoAnimation(sequence) },
                    onAction = { photo, action, _, _, targetX, targetY -> perform(photo, action, targetX, targetY) },
                )

                if (chromeVisible && (settings.photoShowInfoBar || settings.photoShowFolderChips)) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(bottom = 4.dp)
                            .zIndex(8f)
                            .onGloballyPositioned { coordinates ->
                                updateGuideTarget(PhotoGuideTarget.BottomInfo, coordinates.boundsInRoot())
                            },
                    ) {
                        CompactPhotoInfoBar(
                            remaining = remainingPhotos,
                            photo = currentPhoto,
                            selectedAlbum = "指定归档",
                            sessionActionCount = sessionActionCount,
                            lastAction = lastPhotoAction,
                            showInfo = settings.photoShowInfoBar,
                            showAlbum = settings.photoShowFolderChips,
                            onOpen = { haptics.tick(); onOpenPhoto(currentPhoto) },
                            onUndo = { haptics.undo(); viewModel.undoPhotoCleaningAction() },
                            onAlbumClick = { haptics.tick(); showFolderPicker = true },
                            onAlbumPositioned = { rect -> updateGuideTarget(PhotoGuideTarget.AlbumButton, rect) },
                        )
                    }
                }
            }
        }
        if (chromeVisible && (settings.photoShowTopBar || settings.photoShowShuffleButton || settings.photoShowFilterChips)) {
            PhotoOrganizerTopControls(
                remainingCount = remainingPhotos,
                progressPercent = progressPercent,
                deletedSizeBytes = dashboard.pendingDeleteBytes,
                mediaCounts = listOf(
                    "全部" to typeStats.all.toString(),
                    "普通照片" to typeStats.normal.toString(),
                    "截图" to typeStats.screenshot.toString(),
                    "自拍" to typeStats.selfie.toString(),
                    "实况" to typeStats.motion.toString(),
                    "GIF" to typeStats.gif.toString(),
                    "长图" to typeStats.longImage.toString(),
                ),
                selectedMediaIndex = listOf("all", "normal", "screenshot", "selfie", "motion", "gif", "long").indexOf(scope.mediaType).coerceAtLeast(0),
                dateMode = scope.dateMode,
                randomEnabled = scope.sortOrder == "random",
                showShuffle = settings.photoShowShuffleButton,
                showFilters = settings.photoShowFilterChips,
                showRemaining = settings.photoShowTopBar,
                onShuffle = { haptics.threshold(); viewModel.reshufflePhotoCleaningSession() },
                onMediaSelected = { index ->
                    val type = listOf("all", "normal", "screenshot", "selfie", "motion", "gif", "long").getOrElse(index) { "all" }
                    haptics.tick()
                    viewModel.setPhotoTypeFilter(type)
                },
                onDateClick = { haptics.tick(); showDatePicker = true },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 10.dp)
                    .zIndex(9f)
                    .onGloballyPositioned { coordinates ->
                        updateGuideTarget(PhotoGuideTarget.TopBar, coordinates.boundsInRoot())
                    },
            )
        }
        PhotoChromeControl(
            focusMode = settings.photoFocusMode,
            actionCount = sessionActionCount,
            onToggle = { haptics.tick(); viewModel.setPhotoFocusMode(!settings.photoFocusMode) },
            onUndo = { haptics.undo(); viewModel.undoPhotoCleaningAction() },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 64.dp, end = 18.dp)
                .zIndex(10f),
        )
        if (chromeVisible && settings.photoShowGestureHint) {
            PhotoEdgeGlow(feedback = photoSwipeFeedback)
        }

        if (dimAlpha > 0.01f) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = dimAlpha)))
        }
        androidx.compose.animation.AnimatedVisibility(visible = showGuide, modifier = Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                PhotoGuideDialog(onDismiss = { haptics.tick(); showGuide = false; viewModel.markPhotoGuideShown() })
            }
        }
        OrganizerDatePickerAnimatedOverlay(
            visible = showDatePicker,
            title = "照片时间筛选",
            currentMode = scope.dateMode,
            onApply = { mode -> haptics.tick(); viewModel.setPhotoSessionDateMode(mode) },
            onDismiss = { showDatePicker = false },
        )
        OrganizerFolderPickerOverlay(
            visible = showFolderPicker,
            title = "归档当前照片",
            folders = folders.ifEmpty { listOf("DCIM/Camera/", "Pictures/", "Download/") },
            onArchive = { path -> haptics.keep(); viewModel.archivePhotoToFolder(currentPhoto, path) },
            onDismiss = { showFolderPicker = false },
        )
        KeepixDayMemoryOverlay(
            visible = dayMemoryVisible,
            currentPhoto = currentPhoto,
            photos = memoryPhotos,
            entryProgress = if (showDayMemory) 1f else dayMemoryPinch.progress,
            entryScale = dayMemoryPinch.scale,
            entryActive = dayMemoryPinch.active,
            onDismiss = ::closeDayMemory,
            onOpen = { photo -> haptics.tick(); onOpenPhoto(photo) },
            onDelete = { photo -> haptics.delete(); viewModel.markPhotoForTrashOutsideCleaning(photo) },
            onUndo = { haptics.undo(); viewModel.undoPhotoCleaningAction() },
            onApply = { mode ->
                haptics.threshold()
                viewModel.setPhotoSessionDateMode(mode)
                closeDayMemory()
            },
        )
        batchSummary?.let { summary ->
            PhotoBatchSummaryOverlay(
                summary = summary,
                onContinue = {
                    viewModel.finishPhotoCleaningSession()
                    batchSummary = null
                    clearBatchCounters()
                },
                onBackHome = {
                    viewModel.finishPhotoCleaningSession()
                    batchSummary = null
                    viewModel.resetPhotoSessionDateMode()
                    clearBatchCounters()
                    onBack()
                },
                onOpenTrash = {
                    viewModel.finishPhotoCleaningSession()
                    batchSummary = null
                    viewModel.resetPhotoSessionDateMode()
                    clearBatchCounters()
                    onBatchFinished()
                },
                onUndoLast = { undoLastBatchAction(summary) },
            )
        }
    }
}


private data class PhotoBatchSummary(
    val total: Int,
    val kept: Int,
    val favorited: Int,
    val deleted: Int,
    val deletedBytes: Long,
    val lastAction: SwipeAction?,
    val lastDeletedBytes: Long,
)

private data class DayMemoryPinchState(
    val progress: Float = 0f,
    val scale: Float = 1f,
    val active: Boolean = false,
)

@Composable
private fun PhotoBatchSummaryOverlay(
    summary: PhotoBatchSummary,
    onContinue: () -> Unit,
    onBackHome: () -> Unit,
    onOpenTrash: () -> Unit,
    onUndoLast: () -> Unit,
) {
    androidx.compose.animation.AnimatedVisibility(
        visible = true,
        enter = fadeIn(tween(180)) + scaleIn(tween(220), initialScale = 0.94f),
        exit = fadeOut(tween(150)) + scaleOut(tween(170), targetScale = 0.94f),
        modifier = Modifier.fillMaxSize().zIndex(60f),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.72f))
                .padding(22.dp),
            contentAlignment = Alignment.Center,
        ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(30.dp),
            color = Color(0xFF111317).copy(alpha = 0.96f),
            contentColor = Color.White,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text("本轮整理完毕", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                Text(
                    "本轮处理 " + summary.total + " 张 · 保留 " + summary.kept + " · 收藏 " + summary.favorited + " · 待删 " + summary.deleted,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.72f),
                )
                Text(
                    if (summary.deleted > 0) "预计释放 " + formatSize(summary.deletedBytes) else "本轮没有加入待删区的照片",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.62f),
                )
                Button(onClick = onUndoLast, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(999.dp)) {
                    Text("撤回上一张")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(onClick = onContinue, modifier = Modifier.weight(1f), shape = RoundedCornerShape(999.dp)) {
                        Text("继续")
                    }
                    Button(onClick = onBackHome, modifier = Modifier.weight(1f), shape = RoundedCornerShape(999.dp)) {
                        Text("回首页")
                    }
                }
                if (summary.deleted > 0) {
                    Button(onClick = onOpenTrash, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(999.dp)) {
                        Text("去待删区")
                    }
                }
            }
        }
        }
    }
}

private fun Modifier.detectScreenPinchToMemory(
    key: Any,
    disabled: Boolean,
    onProgress: (Float, Float) -> Unit,
    onCommit: () -> Unit,
    onCancel: () -> Unit,
): Modifier = if (disabled) {
    this
} else {
    pointerInput(key) {
        awaitEachGesture {
            var initialDistance = 0f
            var previousDistance = 0f
            var cumulativeScale = 1f
            var currentProgress = 0f
            val slop = viewConfiguration.touchSlop
            while (true) {
                val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                val pressed = event.changes.filter { it.pressed }
                if (pressed.size < 2) {
                    if (pressed.isEmpty()) break
                    continue
                }
                val a = pressed[0].position
                val b = pressed[1].position
                val dx = a.x - b.x
                val dy = a.y - b.y
                val distance = sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
                if (initialDistance <= 0f && distance > slop) {
                    initialDistance = distance
                    previousDistance = distance
                } else if (previousDistance > 0f) {
                    cumulativeScale *= distance / previousDistance.coerceAtLeast(1f)
                    previousDistance = distance
                }
                if (initialDistance > 0f) {
                    val ratio = (distance / initialDistance).coerceIn(0.62f, 1.18f)
                    val shrink = maxOf(1f - ratio, 1f - cumulativeScale)
                    currentProgress = (shrink / 0.26f).coerceIn(0f, 1f)
                    if (currentProgress > 0f) {
                        pressed.forEach { it.consume() }
                        onProgress(currentProgress, ratio.coerceIn(0.66f, 1f))
                    }
                }
            }
            if (currentProgress > 0.04f) {
                onCommit()
            } else {
                onCancel()
            }
        }
    }
}




private enum class PhotoGuideTarget { TopBar, PhotoCard, BottomInfo, AlbumButton }

private data class PositionGuideStep(
    val target: PhotoGuideTarget,
    val title: String,
    val body: String,
)

@Composable
private fun PhotoPositionGuideOverlay(
    targets: Map<PhotoGuideTarget, Rect>,
    onDismiss: () -> Unit,
) {
    var stepIndex by remember { mutableIntStateOf(0) }
    val steps = listOf(
        PositionGuideStep(
            target = PhotoGuideTarget.TopBar,
            title = "顶部操作栏",
            body = "这里是一行核心动作：保留、重新随机、图片筛选、时间筛选和剩余数量。筛选点开才展开。",
        ),
        PositionGuideStep(
            target = PhotoGuideTarget.PhotoCard,
            title = "中间照片卡片",
            body = "点按图片进入大图；明显左右滑是保留，明显上下滑才会收藏或待删。斜向拖动不会轻易触发破坏性操作。",
        ),
        PositionGuideStep(
            target = PhotoGuideTarget.BottomInfo,
            title = "底部图片信息",
            body = "这里集中显示拍摄时间、文件夹、类型、大小和剩余数量；点按信息条也能进入大图查看细节。",
        ),
        PositionGuideStep(
            target = PhotoGuideTarget.AlbumButton,
            title = "相册按钮",
            body = "点击相册可以展开目标文件夹，选择后保留/收藏会按设置自动归档。",
        ),
    )
    val step = steps[stepIndex.coerceIn(0, steps.lastIndex)]
    val density = LocalDensity.current
    Box(Modifier.fillMaxSize()) {
        androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize()) {
            val fallbackRect = with(density) {
                val w = maxWidth.toPx()
                val h = maxHeight.toPx()
                when (step.target) {
                    PhotoGuideTarget.TopBar -> Rect(18.dp.toPx(), 44.dp.toPx(), w - 18.dp.toPx(), 106.dp.toPx())
                    PhotoGuideTarget.PhotoCard -> Rect(28.dp.toPx(), h * 0.22f, w - 28.dp.toPx(), h * 0.73f)
                    PhotoGuideTarget.BottomInfo -> Rect(18.dp.toPx(), h - 104.dp.toPx(), w - 18.dp.toPx(), h - 22.dp.toPx())
                    PhotoGuideTarget.AlbumButton -> Rect(w - 92.dp.toPx(), h - 94.dp.toPx(), w - 20.dp.toPx(), h - 22.dp.toPx())
                }
            }
            val rawRect = targets[step.target] ?: fallbackRect
            val highlightRect = Rect(
                left = (rawRect.left - 8f).coerceAtLeast(0f),
                top = (rawRect.top - 8f).coerceAtLeast(0f),
                right = (rawRect.right + 8f).coerceAtMost(with(density) { maxWidth.toPx() }),
                bottom = (rawRect.bottom + 8f).coerceAtMost(with(density) { maxHeight.toPx() }),
            )
            val screenWidthPx = with(density) { maxWidth.toPx() }
            val screenHeightPx = with(density) { maxHeight.toPx() }
            Canvas(Modifier.fillMaxSize()) {
                val scrim = Color.Black.copy(alpha = 0.18f)
                drawRect(scrim, topLeft = Offset.Zero, size = Size(screenWidthPx, highlightRect.top))
                drawRect(scrim, topLeft = Offset(0f, highlightRect.bottom), size = Size(screenWidthPx, screenHeightPx - highlightRect.bottom))
                drawRect(scrim, topLeft = Offset(0f, highlightRect.top), size = Size(highlightRect.left, highlightRect.height))
                drawRect(scrim, topLeft = Offset(highlightRect.right, highlightRect.top), size = Size(screenWidthPx - highlightRect.right, highlightRect.height))
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.92f),
                    topLeft = Offset(highlightRect.left, highlightRect.top),
                    size = Size(highlightRect.width, highlightRect.height),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(28f, 28f),
                    style = Stroke(width = 3f),
                )
                drawRoundRect(
                    color = Color(0xFF7CC6F2).copy(alpha = 0.82f),
                    topLeft = Offset(highlightRect.left + 2f, highlightRect.top + 2f),
                    size = Size((highlightRect.width - 4f).coerceAtLeast(0f), (highlightRect.height - 4f).coerceAtLeast(0f)),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(26f, 26f),
                    style = Stroke(width = 4f),
                )
            }
            val cardOnTop = highlightRect.center.y > screenHeightPx * 0.56f
            Surface(
                modifier = Modifier
                    .align(if (cardOnTop) Alignment.TopCenter else Alignment.BottomCenter)
                    .padding(horizontal = 22.dp, vertical = 32.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                contentColor = MaterialTheme.colorScheme.onSurface,
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("步骤 " + (stepIndex + 1) + "/" + steps.size, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Text(step.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(step.body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        if (stepIndex > 0) {
                            Button(onClick = { stepIndex -= 1 }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(999.dp)) { Text("上一步") }
                        }
                        Button(
                            onClick = { if (stepIndex < steps.lastIndex) stepIndex += 1 else onDismiss() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(999.dp),
                        ) { Text(if (stepIndex < steps.lastIndex) "下一步" else "知道了") }
                    }
                }
            }
        }
    }
}

private enum class PhotoQuickPanel { MEDIA, DATE }

@Composable
private fun PhotoOrganizerTopControls(
    remainingCount: Int,
    progressPercent: Int,
    deletedSizeBytes: Long,
    mediaCounts: List<Pair<String, String>>,
    selectedMediaIndex: Int,
    dateMode: String,
    randomEnabled: Boolean,
    showShuffle: Boolean = true,
    showFilters: Boolean = true,
    showRemaining: Boolean = true,
    onShuffle: () -> Unit,
    onMediaSelected: (Int) -> Unit,
    onDateClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var openPanel by remember { mutableStateOf<PhotoQuickPanel?>(null) }
    val dateLabel = organizerDateModeLabel(dateMode)
    val mediaLabel = mediaCounts.getOrNull(selectedMediaIndex)?.first ?: "全部"

    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showShuffle) {
                CompactControlPill(
                    title = "重新随机",
                    subtitle = if (randomEnabled) "已随机" else "随机",
                    icon = Icons.Rounded.Swipe,
                    selected = randomEnabled,
                    onClick = onShuffle,
                )
            }
            if (showFilters) {
                CompactControlPill(
                    title = "图片筛选",
                    subtitle = mediaLabel,
                    icon = Icons.Rounded.FilterAlt,
                    selected = openPanel == PhotoQuickPanel.MEDIA,
                    onClick = { openPanel = if (openPanel == PhotoQuickPanel.MEDIA) null else PhotoQuickPanel.MEDIA },
                )
                CompactControlPill(
                    title = "时间筛选",
                    subtitle = dateLabel,
                    icon = Icons.Rounded.CalendarToday,
                    selected = dateMode != "all",
                    onClick = onDateClick,
                )
            }
            if (showRemaining) {
                OrganizerProgressPill(
                    progressPercent = progressPercent,
                    remainingCount = remainingCount,
                    remainingLabel = "张",
                    releasableBytes = deletedSizeBytes,
                    modifier = Modifier.width(94.dp),
                    compact = true,
                )
            }
        }

        androidx.compose.animation.AnimatedVisibility(visible = showFilters && openPanel == PhotoQuickPanel.MEDIA) {
            FilterChipRail(
                chips = mediaCounts,
                selectedIndex = selectedMediaIndex,
                onSelected = { index -> onMediaSelected(index); openPanel = null },
            )
        }
    }
}

@Composable
private fun PendingDeletePill(count: Int, sizeBytes: Long) {
    Surface(
        modifier = Modifier.height(42.dp),
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
        contentColor = MaterialTheme.colorScheme.primary,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text("可释放", style = MaterialTheme.typography.labelLarge)
            Text(if (sizeBytes > 0L) formatSize(sizeBytes) else count.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ProgressPercentPill(percent: Int) {
    Surface(
        modifier = Modifier.height(42.dp),
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.11f),
        contentColor = MaterialTheme.colorScheme.primary,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(percent.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
            Text("%", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun CompactControlPill(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean = false,
    onClick: () -> Unit,
) {
    val background = if (selected) Color.White.copy(alpha = 0.26f) else Color.Black.copy(alpha = 0.24f)
    val content = Color.White
    Surface(
        modifier = Modifier.height(42.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(999.dp),
        color = background,
        contentColor = content,
        border = BorderStroke(1.dp, Color.White.copy(alpha = if (selected) 0.32f else 0.16f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(17.dp), tint = content)
            Column(verticalArrangement = Arrangement.Center) {
                Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = content.copy(alpha = 0.68f), maxLines = 1)
            }
        }
    }
}

@Composable
private fun RemainingCountPill(remainingCount: Int) {
    Surface(
        modifier = Modifier.height(42.dp),
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
        contentColor = MaterialTheme.colorScheme.primary,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text("剩余", style = MaterialTheme.typography.labelLarge)
            Text(remainingCount.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("张", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun PhotoChromeControl(
    focusMode: Boolean,
    actionCount: Int,
    onToggle: () -> Unit,
    onUndo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (focusMode && actionCount > 0) {
            Surface(
                modifier = Modifier.size(46.dp).clickable(onClick = onUndo),
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.28f),
                contentColor = Color.White,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Rounded.Undo, contentDescription = stringResource(R.string.a11y_back_to_previous_photo), modifier = Modifier.size(19.dp))
                }
            }
        }
        Surface(
            modifier = Modifier.height(46.dp).clickable(onClick = onToggle),
            shape = RoundedCornerShape(999.dp),
            color = Color.Black.copy(alpha = 0.28f),
            contentColor = Color.White,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Icon(
                    if (focusMode) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = Color.White.copy(alpha = 0.86f),
                )
                Text(if (focusMode) "显示控件" else "隐藏控件", style = MaterialTheme.typography.labelLarge, color = Color.White)
            }
        }
    }
}

@Composable
private fun CompactPhotoInfoBar(
    remaining: Int,
    photo: PhotoEntity,
    selectedAlbum: String,
    sessionActionCount: Int,
    lastAction: SwipeAction?,
    showInfo: Boolean = true,
    showAlbum: Boolean = true,
    onOpen: () -> Unit,
    onUndo: () -> Unit,
    onAlbumClick: () -> Unit,
    onAlbumPositioned: (Rect) -> Unit = {},
) {
    val actionColor = actionFeedbackColor(lastAction, MaterialTheme.colorScheme.primary)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Surface(
            modifier = Modifier.size(62.dp),
            shape = CircleShape,
            color = Color.Black.copy(alpha = 0.28f),
            contentColor = Color.White,
            border = BorderStroke(1.dp, actionColor.copy(alpha = 0.42f)),
            onClick = onUndo,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text(sessionActionCount.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, maxLines = 1)
                Icon(Icons.AutoMirrored.Rounded.Undo, contentDescription = stringResource(R.string.a11y_back_to_previous_photo), modifier = Modifier.size(16.dp), tint = Color.White.copy(alpha = 0.88f))
            }
        }
        if (showInfo) {
            Surface(
                modifier = Modifier.weight(1f).height(66.dp).clickable(onClick = onOpen),
                shape = RoundedCornerShape(26.dp),
                color = Color.Black.copy(alpha = 0.30f),
                contentColor = Color.White,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
            ) {
                Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.Center) {
                    Text(
                        "剩余 " + remaining + " 张 · " + formatDate(photo.dateTaken),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        photo.folderName + " · " + photoMediaKindLabel(photo) + " · " + formatSize(photo.size) + " · 点按查看大图",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.72f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        } else {
            Spacer(Modifier.weight(1f))
        }
        if (showAlbum) {
            Surface(
                modifier = Modifier
                    .size(62.dp)
                    .onGloballyPositioned { coordinates -> onAlbumPositioned(coordinates.boundsInRoot()) }
                    .clickable(onClick = onAlbumClick),
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.28f),
                contentColor = Color.White,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Icon(Icons.Rounded.Folder, contentDescription = stringResource(R.string.a11y_choose_album, selectedAlbum), modifier = Modifier.size(20.dp))
                    Text("相册", style = MaterialTheme.typography.labelSmall, maxLines = 1)
                }
            }
        }
    }
}

private fun photoGestureColor(action: SwipeAction): Color = when (action) {
    SwipeAction.Keep -> Color(0xFF74A7FF)
    SwipeAction.Favorite -> Color(0xFFE6A63E)
    SwipeAction.Delete -> Color(0xFFFF4E4E)
}

@Composable
internal fun PhotoEdgeGlow(feedback: PhotoSwipeFeedback) {
    val action = feedback.action ?: return
    val activeColor = photoGestureColor(action)
    val edgeAlpha by animateFloatAsState(
        targetValue = (0.24f + feedback.intensity * 0.76f).coerceIn(0f, 1f),
        animationSpec = tween(100),
        label = "photo_edge_feedback",
    )
    Box(Modifier.fillMaxSize()) {
        when (action) {
            SwipeAction.Keep -> {
                val draggingRight = feedback.offsetX >= 0f
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            if (draggingRight) {
                                Brush.horizontalGradient(
                                    0.0f to Color.Transparent,
                                    0.48f to Color.Transparent,
                                    0.70f to activeColor.copy(alpha = edgeAlpha * 0.10f),
                                    0.88f to activeColor.copy(alpha = edgeAlpha * 0.28f),
                                    1.0f to activeColor.copy(alpha = edgeAlpha * 0.62f),
                                )
                            } else {
                                Brush.horizontalGradient(
                                    0.0f to activeColor.copy(alpha = edgeAlpha * 0.62f),
                                    0.12f to activeColor.copy(alpha = edgeAlpha * 0.28f),
                                    0.30f to activeColor.copy(alpha = edgeAlpha * 0.10f),
                                    0.52f to Color.Transparent,
                                    1.0f to Color.Transparent,
                                )
                            }
                        ),
                )
            }
            SwipeAction.Delete -> Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.0f to activeColor.copy(alpha = edgeAlpha * 0.62f),
                            0.15f to activeColor.copy(alpha = edgeAlpha * 0.34f),
                            0.36f to activeColor.copy(alpha = edgeAlpha * 0.14f),
                            0.56f to Color.Transparent,
                            1.0f to Color.Transparent,
                        )
                    ),
            )
            SwipeAction.Favorite -> Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.0f to Color.Transparent,
                            0.44f to Color.Transparent,
                            0.62f to activeColor.copy(alpha = edgeAlpha * 0.14f),
                            0.82f to activeColor.copy(alpha = edgeAlpha * 0.34f),
                            1.0f to activeColor.copy(alpha = edgeAlpha * 0.62f),
                        )
                    ),
            )
        }
    }
}

@Composable
private fun BoxScope.PhotoFloatingActionHint(feedback: PhotoSwipeFeedback) {
    val action = feedback.action ?: return
    val color = photoGestureColor(action)
    val intensity = feedback.intensity.coerceIn(0f, 1f)
    val entrance = 2.dp + 130.dp * intensity
    val alpha = (0.08f + intensity * 0.78f).coerceIn(0f, 1f)
    val modifier = when (action) {
        SwipeAction.Keep -> {
            if (feedback.offsetX >= 0f) {
                Modifier.align(Alignment.CenterEnd).padding(end = entrance)
            } else {
                Modifier.align(Alignment.CenterStart).padding(start = entrance)
            }
        }
        SwipeAction.Delete -> Modifier.align(Alignment.TopCenter).padding(top = 28.dp + 112.dp * intensity)
        SwipeAction.Favorite -> Modifier.align(Alignment.BottomCenter).padding(bottom = 40.dp + 110.dp * intensity)
    }
    val arrow = when (action) {
        SwipeAction.Keep -> if (feedback.offsetX >= 0f) "›" else "‹"
        SwipeAction.Delete -> "×"
        SwipeAction.Favorite -> "♥"
    }
    val label = when (action) {
        SwipeAction.Keep -> "保留"
        SwipeAction.Delete -> "待删"
        SwipeAction.Favorite -> "收藏"
    }
    Column(
        modifier = modifier.graphicsLayer {
            this.alpha = alpha
            val scale = 0.82f + 0.18f * intensity
            scaleX = scale
            scaleY = scale
        },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Surface(
            modifier = Modifier.size(58.dp),
            shape = CircleShape,
            color = color.copy(alpha = 0.88f),
            contentColor = Color.White,
            shadowElevation = 4.dp,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(arrow, style = MaterialTheme.typography.headlineLarge, color = Color.White, fontWeight = FontWeight.Black)
            }
        }
        Text(label, style = MaterialTheme.typography.titleMedium, color = color, fontWeight = FontWeight.Black)
    }
}

