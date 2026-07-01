package com.futureape.kanleme.ui.screens

import android.net.Uri
import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.staggeredgrid.LazyHorizontalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Swipe
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.positionInRoot
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
import com.futureape.kanleme.ui.viewmodel.KanlemeViewModel
import com.futureape.kanleme.ui.i18n.Text
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

// Keepix immersive photo cleaning visual path. Do not put legacy Liquid Glass UI in this file.
@Composable
internal fun ImmersivePhotoCleanContent(
    photos: List<PhotoEntity>,
    currentPhoto: PhotoEntity,
    settings: com.futureape.kanleme.data.settings.AppSettings,
    haptics: com.futureape.kanleme.ui.util.HapticKit,
    progressPercent: Int,
    remaining: Int,
    sessionActionCount: Int,
    undoAnimation: com.futureape.kanleme.ui.viewmodel.PhotoUndoAnimation?,
    onBack: () -> Unit,
    onOpenPhoto: (PhotoEntity) -> Unit,
    onSwipeFeedbackChanged: (PhotoSwipeFeedback) -> Unit,
    photoSwipeFeedback: PhotoSwipeFeedback,
    onOpenDayMemory: () -> Unit,
    onTopCardPositioned: (Rect) -> Unit,
    onUndoAnimationConsumed: (Long) -> Unit,
    onAction: (PhotoEntity, SwipeAction, Float, Float, Float, Float) -> Unit,
    onShare: () -> Unit,
    onUndo: () -> Unit,
    onToggleFocus: () -> Unit,
    onDateClick: () -> Unit,
    onFolderClick: () -> Unit,
) {
    val context = LocalContext.current
    val focusMode = settings.photoFocusMode
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val enterScale by animateFloatAsState(
        targetValue = if (entered) 1f else 0.96f,
        animationSpec = tween(360, easing = FastOutSlowInEasing),
        label = "photo_clean_enter_scale",
    )
    val enterOffsetY by animateFloatAsState(
        targetValue = if (entered) 0f else 30f,
        animationSpec = tween(360, easing = FastOutSlowInEasing),
        label = "photo_clean_enter_y",
    )
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        Crossfade(
            targetState = currentPhoto.uri,
            animationSpec = tween(durationMillis = 520),
            label = "keepix_photo_background",
        ) { photoUri ->
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(Uri.parse(photoUri))
                    .memoryCacheKey(photoUri)
                    .diskCacheKey(photoUri)
                    .placeholderMemoryCacheKey(photoUri)
                    .crossfade(false)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(30.dp)
                    .graphicsLayer {
                        scaleX = 1.10f
                        scaleY = 1.10f
                        alpha = 0.70f
                    },
                contentScale = ContentScale.Crop,
            )
        }
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(
                        Color.Black.copy(alpha = 0.62f),
                        Color.Black.copy(alpha = 0.24f),
                        Color.Black.copy(alpha = 0.26f),
                        Color.Black.copy(alpha = 0.82f),
                    )
                )
            )
        )
        AdaptiveWidthInfo { _, isExpanded ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(horizontal = if (isExpanded) 36.dp else 18.dp, vertical = 12.dp),
            ) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .zIndex(5f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    KeepixRoundButton(
                        icon = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                        contentDescription = stringResource(R.string.a11y_back),
                        onClick = onBack,
                    )
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        KeepixProgressBar(progress = progressPercent / 100f)
                        AnimatedVisibility(
                            visible = !focusMode,
                            enter = fadeIn(tween(180)) + slideInVertically(tween(220)) { -it / 3 },
                            exit = fadeOut(tween(140)) + slideOutVertically(tween(180)) { -it / 4 },
                        ) {
                            KeepixTopPill(
                                label = photoMediaKindLabel(currentPhoto),
                                trailing = remaining.toString(),
                                onClick = onDateClick,
                            )
                        }
                    }
                    KeepixRoundButton(
                        icon = if (focusMode) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                        contentDescription = null,
                        onClick = onToggleFocus,
                    )
                }

                PhotoDeckStage(
                    photos = photos,
                    settings = settings,
                    haptics = haptics,
                    modifier = (if (isExpanded) {
                        Modifier
                            .align(Alignment.Center)
                            .widthIn(max = 700.dp)
                            .fillMaxWidth()
                            .fillMaxHeight(if (focusMode) 0.78f else 0.68f)
                            .padding(top = 56.dp, bottom = if (focusMode) 44.dp else 140.dp)
                    } else {
                        Modifier
                            .align(Alignment.Center)
                            .fillMaxWidth()
                            .fillMaxHeight(if (focusMode) 0.78f else 0.68f)
                            .padding(top = 62.dp, bottom = if (focusMode) 46.dp else 146.dp)
                    })
                        .graphicsLayer {
                            scaleX = enterScale
                            scaleY = enterScale
                            translationY = enterOffsetY
                        }
                        .zIndex(1f),
                    onOpen = onOpenPhoto,
                    onTopCardPositioned = onTopCardPositioned,
                    onSwipeFeedbackChanged = onSwipeFeedbackChanged,
                    onPinchToMemory = null,
                    undoAnimation = undoAnimation,
                    onUndoAnimationConsumed = onUndoAnimationConsumed,
                    onAction = onAction,
                )

                AnimatedVisibility(
                    visible = !focusMode,
                    enter = fadeIn(tween(180)) + slideInVertically(tween(220)) { it / 5 },
                    exit = fadeOut(tween(140)) + slideOutVertically(tween(180)) { it / 5 },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                        .zIndex(6f),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        KeepixRoundButton(
                            icon = Icons.Rounded.Share,
                            contentDescription = null,
                            onClick = onShare,
                        )
                        KeepixBottomInfoBar(
                            photo = currentPhoto,
                            modifier = Modifier.weight(1f),
                            onClick = onFolderClick,
                        )
                        KeepixRoundButton(
                            icon = Icons.AutoMirrored.Rounded.Undo,
                            contentDescription = null,
                            enabled = sessionActionCount > 0,
                            onClick = onUndo,
                        )
                    }
                }
                AnimatedVisibility(
                    visible = focusMode,
                    enter = fadeIn(tween(180)) + slideInVertically(tween(220)) { it / 5 },
                    exit = fadeOut(tween(140)) + slideOutVertically(tween(180)) { it / 5 },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 6.dp)
                        .zIndex(6f),
                ) {
                    KeepixRoundButton(
                        icon = Icons.AutoMirrored.Rounded.Undo,
                        contentDescription = null,
                        enabled = sessionActionCount > 0,
                        onClick = onUndo,
                    )
                }
            }
        }
        if (settings.photoShowGestureHint) {
            PhotoEdgeGlow(feedback = photoSwipeFeedback)
        }
    }
}

@Composable
internal fun KeepixDayMemoryOverlay(
    visible: Boolean,
    currentPhoto: PhotoEntity,
    photos: List<PhotoEntity>,
    entryProgress: Float = 1f,
    entryScale: Float = 1f,
    entryActive: Boolean = false,
    onDismiss: () -> Unit,
    onOpen: (PhotoEntity) -> Unit,
    onDelete: (PhotoEntity) -> Unit,
    onUndo: () -> Unit,
    onApply: (String) -> Unit,
) {
    val context = LocalContext.current
    val memoryScope = rememberCoroutineScope()
    val memoryGridState = rememberLazyStaggeredGridState()
    var memoryPhotos by remember(currentPhoto.id) { mutableStateOf(buildMemoryPhotoWindow(currentPhoto, photos)) }
    var lastDeletedMemory by remember(currentPhoto.id) { mutableStateOf<DeletedMemoryPhoto?>(null) }
    var restoringPhotoId by remember(currentPhoto.id) { mutableStateOf<Long?>(null) }
    var memoryPlacementSequence by remember(currentPhoto.id) { mutableStateOf(0L) }
    val suppressMemoryOpenUntil = remember(currentPhoto.id) { mutableLongStateOf(0L) }
    LaunchedEffect(visible, currentPhoto.id) {
        if (visible) {
            memoryPhotos = buildMemoryPhotoWindow(currentPhoto, photos)
            lastDeletedMemory = null
            restoringPhotoId = null
        }
    }
    LaunchedEffect(restoringPhotoId) {
        if (restoringPhotoId != null) {
            kotlinx.coroutines.delay(360)
            restoringPhotoId = null
        }
    }
    val initialPhotoIndex = remember(currentPhoto.id, memoryPhotos) {
        memoryPhotos.indexOfFirst { it.id == currentPhoto.id }.coerceAtLeast(0)
    }
    var memoryScale by remember(currentPhoto.id) { mutableFloatStateOf(1f) }
    LaunchedEffect(visible, entryScale, entryActive) {
        if (!visible) {
            memoryScale = 1f
        } else if (entryActive) {
            memoryScale = entryScale.coerceIn(0.66f, 1f)
        } else {
            memoryScale = 1f
        }
    }
    val rowCount = if (!entryActive && memoryScale < 0.86f) 3 else 2
    val selectedPhotoIndex by remember(memoryGridState, memoryPhotos) {
        derivedStateOf {
            val visibleItems = memoryGridState.layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty()) {
                initialPhotoIndex
            } else {
                val viewportCenter = (memoryGridState.layoutInfo.viewportStartOffset + memoryGridState.layoutInfo.viewportEndOffset) / 2
                visibleItems
                    .minByOrNull { item -> abs(item.offset.x + item.size.width / 2 - viewportCenter) }
                    ?.index
                    ?: initialPhotoIndex
            }.coerceIn(0, memoryPhotos.lastIndex.coerceAtLeast(0))
        }
    }
    val selectedPhoto = memoryPhotos.getOrNull(selectedPhotoIndex) ?: currentPhoto
    val memoryDayKeyById = remember(memoryPhotos) { memoryPhotos.associate { it.id to memoryDayKey(it) } }
    val selectedDayKey = memoryDayKeyById[selectedPhoto.id] ?: memoryDayKey(selectedPhoto)
    val memoryDayKeys = remember(memoryPhotos, memoryDayKeyById) {
        memoryPhotos.mapNotNull { memoryDayKeyById[it.id] }.distinct()
    }
    val memoryFirstIndexByDayKey = remember(memoryPhotos, memoryDayKeyById) {
        buildMap {
            memoryPhotos.forEachIndexed { index, photo ->
                val key = memoryDayKeyById[photo.id] ?: return@forEachIndexed
                if (!containsKey(key)) put(key, index)
            }
        }
    }
    val selectedDayIndex = memoryDayKeys.indexOf(selectedDayKey).coerceAtLeast(0)
    var lastDensityScrollIndex by remember(currentPhoto.id, memoryPhotos.size, rowCount) { mutableStateOf(-1) }
    val densityScrollJob = remember(currentPhoto.id) { arrayOf<Job?>(null) }
    val rowModeProgress = remember { Animatable(1f) }
    LaunchedEffect(rowCount) {
        rowModeProgress.snapTo(0f)
        rowModeProgress.animateTo(1f, animationSpec = tween(220, easing = FastOutSlowInEasing))
    }
    LaunchedEffect(memoryGridState) {
        snapshotFlow { memoryGridState.isScrollInProgress }.collect { scrolling ->
            if (scrolling) {
                suppressMemoryOpenUntil.longValue = SystemClock.uptimeMillis() + 520L
            }
        }
    }
    val returnProgress = ((memoryScale - 1.10f) / 0.50f).coerceIn(0f, 1f)
    val entryAlpha = entryProgress.coerceIn(0f, 1f)
    val entryZoom = 0.88f + entryAlpha * 0.12f
    val scrollProgress = if (memoryDayKeys.size > 1) {
        selectedDayIndex.toFloat() / memoryDayKeys.lastIndex.toFloat()
    } else {
        0.5f
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(180)) + scaleIn(tween(240), initialScale = 0.86f),
        exit = fadeOut(tween(160)) + scaleOut(tween(200), targetScale = 0.86f),
        modifier = Modifier.fillMaxSize().zIndex(20f),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = (entryAlpha * (1f - returnProgress)).coerceIn(if (entryActive) 0f else 0.08f, 1f)
                    scaleX = entryZoom
                    scaleY = entryZoom
                }
                .background(Color.Black.copy(alpha = 0.90f))
                .statusBarsPadding()
                .padding(horizontal = 18.dp, vertical = 12.dp)
                .pointerInput(currentPhoto.id, memoryPhotos.size) {
                    awaitEachGesture {
                        var initialDistance = 0f
                        while (true) {
                            val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                            val pressed = event.changes.filter { it.pressed }
                            if (pressed.size < 2) {
                                if (pressed.isEmpty()) break
                                continue
                            }
                            suppressMemoryOpenUntil.longValue = SystemClock.uptimeMillis() + 650L
                            val currentA = pressed[0].position
                            val currentB = pressed[1].position
                            val currentDistance = sqrt(
                                (currentA.x - currentB.x) * (currentA.x - currentB.x) +
                                    (currentA.y - currentB.y) * (currentA.y - currentB.y)
                            ).coerceAtLeast(1f)
                            if (initialDistance <= 0f) {
                                initialDistance = currentDistance
                                continue
                            }
                            val zoom = currentDistance / initialDistance.coerceAtLeast(1f)
                            suppressMemoryOpenUntil.longValue = SystemClock.uptimeMillis() + 650L
                            memoryScale = zoom.coerceIn(0.66f, 1.42f)
                            pressed.forEach { it.consume() }
                            if (zoom >= 1.18f) {
                                onDismiss()
                                break
                            }
                            if (pressed.any { it.changedToUpIgnoreConsumed() }) break
                        }
                    }
                },
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().zIndex(2f),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                KeepixRoundButton(
                    icon = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                    contentDescription = stringResource(R.string.a11y_back),
                    onClick = onDismiss,
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("回到那天", style = MaterialTheme.typography.titleMedium, color = Color.White.copy(alpha = 0.72f), fontWeight = FontWeight.Bold)
                    Text(selectedDayKey.replace("-", "/"), style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Black)
                }
                KeepixRoundButton(
                    icon = Icons.Rounded.Check,
                    contentDescription = null,
                    onClick = { onApply("d:" + selectedDayKey) },
                )
            }
            BoxWithConstraints(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .padding(top = 84.dp, bottom = 142.dp),
            ) {
                val density = LocalDensity.current
                val gap = 10.dp
                val targetRowHeight = if (rowCount == 3) {
                    (maxHeight - gap * 2) / 3
                } else {
                    (maxHeight - gap) / 2
                }
                val rowHeight by animateDpAsState(
                    targetValue = targetRowHeight,
                    animationSpec = tween(220, easing = FastOutSlowInEasing),
                    label = "day_memory_row_height",
                )
                val verticalGap by animateDpAsState(
                    targetValue = if (rowCount == 2) 18.dp else 12.dp,
                    animationSpec = tween(220, easing = FastOutSlowInEasing),
                    label = "day_memory_vertical_gap",
                )
                LaunchedEffect(visible, currentPhoto.id, initialPhotoIndex) {
                    if (!visible || memoryPhotos.isEmpty()) return@LaunchedEffect
                    memoryGridState.scrollToItem(initialPhotoIndex)
                    repeat(8) {
                        withFrameNanos { }
                        val layoutInfo = memoryGridState.layoutInfo
                        val itemInfo = layoutInfo.visibleItemsInfo.firstOrNull { it.index == initialPhotoIndex }
                        if (itemInfo != null) {
                            val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2f
                            val itemCenter = itemInfo.offset.x + itemInfo.size.width / 2f
                            val delta = itemCenter - viewportCenter
                            if (abs(delta) > with(density) { 2.dp.toPx() }) {
                                memoryGridState.scrollBy(delta)
                            }
                            return@LaunchedEffect
                        }
                    }
                }
                LazyHorizontalStaggeredGrid(
                    rows = StaggeredGridCells.Fixed(rowCount),
                    state = memoryGridState,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            val progress = rowModeProgress.value
                            alpha = 0.84f + progress * 0.16f
                            val modeScale = 0.982f + progress * 0.018f
                            scaleX = modeScale
                            scaleY = modeScale
                        },
                    contentPadding = PaddingValues(horizontal = maxWidth * 0.34f),
                    horizontalItemSpacing = gap,
                    verticalArrangement = Arrangement.spacedBy(verticalGap),
                ) {
                    itemsIndexed(memoryPhotos, key = { _, photo -> photo.id }) { _, photo ->
                        val photoHeight = rowHeight
                        val photoWidth = photoHeight * memoryPhotoAspectRatio(photo)
                        KeepixMemoryGridPhoto(
                            context = context,
                            photo = photo,
                            restoring = restoringPhotoId == photo.id,
                            placementAnimationKey = memoryPlacementSequence,
                            openSuppressedUntilMillis = suppressMemoryOpenUntil.longValue,
                            modifier = Modifier
                                .width(photoWidth)
                                .fillMaxHeight(),
                            onOpen = onOpen,
                            onDelete = { _ ->
                                val index = memoryPhotos.indexOfFirst { it.id == photo.id }
                                if (index >= 0) {
                                    lastDeletedMemory = DeletedMemoryPhoto(photo = photo, index = index)
                                    memoryPlacementSequence += 1L
                                    memoryPhotos = memoryPhotos.filterNot { it.id == photo.id }
                                }
                                onDelete(photo)
                            },
                        )
                    }
                }
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 14.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(999.dp),
                    color = Color.Transparent,
                    contentColor = Color.White,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        KeepixRoundButton(
                            icon = Icons.Rounded.Swipe,
                            contentDescription = null,
                            onClick = {
                                memoryScope.launch {
                                    memoryGridState.animateScrollToItem((initialPhotoIndex - rowCount * 2).coerceAtLeast(0))
                                }
                            },
                        )
                        Surface(
                            modifier = Modifier.weight(1f).height(54.dp),
                            shape = RoundedCornerShape(999.dp),
                            color = Color.White.copy(alpha = 0.10f),
                            contentColor = Color.White,
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
                        ) {
                            KeepixMemoryDensityBar(
                                total = memoryDayKeys.size,
                                progress = scrollProgress,
                                onDragStateChange = { dragging ->
                                    if (dragging) {
                                        suppressMemoryOpenUntil.longValue = SystemClock.uptimeMillis() + 720L
                                    }
                                },
                                onSelect = { fraction ->
                                    suppressMemoryOpenUntil.longValue = SystemClock.uptimeMillis() + 720L
                                    val targetDayIndex = ((memoryDayKeys.lastIndex.coerceAtLeast(0)) * fraction)
                                        .roundToInt()
                                        .coerceIn(0, memoryDayKeys.lastIndex.coerceAtLeast(0))
                                    val targetDayKey = memoryDayKeys.getOrNull(targetDayIndex)
                                    val targetIndex = targetDayKey?.let(memoryFirstIndexByDayKey::get)
                                        ?: initialPhotoIndex
                                    val scrollIndex = (targetIndex - rowCount * 2).coerceAtLeast(0)
                                    if (scrollIndex != lastDensityScrollIndex) {
                                        lastDensityScrollIndex = scrollIndex
                                        densityScrollJob[0]?.cancel()
                                        densityScrollJob[0] = memoryScope.launch {
                                            memoryGridState.scrollToItem(scrollIndex)
                                        }
                                    }
                                },
                            )
                        }
                        KeepixRoundButton(
                            icon = Icons.AutoMirrored.Rounded.Undo,
                            contentDescription = null,
                            onClick = {
                                val deleted = lastDeletedMemory
                                if (deleted != null && memoryPhotos.none { it.id == deleted.photo.id }) {
                                    val insertAt = deleted.index.coerceIn(0, memoryPhotos.size)
                                    memoryPlacementSequence += 1L
                                    memoryPhotos = memoryPhotos.toMutableList().apply { add(insertAt, deleted.photo) }
                                    restoringPhotoId = deleted.photo.id
                                    lastDeletedMemory = null
                                }
                                onUndo()
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KeepixMemoryGridPhoto(
    context: android.content.Context,
    photo: PhotoEntity,
    restoring: Boolean,
    placementAnimationKey: Long,
    openSuppressedUntilMillis: Long,
    modifier: Modifier,
    onOpen: (PhotoEntity) -> Unit,
    onDelete: (PhotoEntity) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var dragY by remember(photo.id) { mutableStateOf(0f) }
    var dismissed by remember(photo.id) { mutableStateOf(false) }
    var deleteDispatched by remember(photo.id) { mutableStateOf(false) }
    var lastPosition by remember(photo.id) { mutableStateOf<Offset?>(null) }
    var lastPlacementAnimationKey by remember(photo.id) { mutableStateOf(placementAnimationKey) }
    val placementX = remember(photo.id) { Animatable(0f) }
    val placementY = remember(photo.id) { Animatable(0f) }
    val restoreY = remember(photo.id) { Animatable(0f) }
    val restoreAlpha = remember(photo.id) { Animatable(1f) }
    LaunchedEffect(restoring, photo.id) {
        if (restoring) {
            restoreY.snapTo(-640f)
            restoreAlpha.snapTo(0f)
            launch { restoreY.animateTo(0f, animationSpec = tween(280, easing = FastOutSlowInEasing)) }
            launch { restoreAlpha.animateTo(1f, animationSpec = tween(220, easing = FastOutSlowInEasing)) }
        }
    }
    val animatedY by animateFloatAsState(
        targetValue = if (dismissed) -920f else dragY,
        animationSpec = tween(240, easing = FastOutSlowInEasing),
        label = "day_memory_photo_y",
    )
    val animatedAlpha by animateFloatAsState(
        targetValue = if (dismissed) 0f else 1f,
        animationSpec = tween(210, easing = FastOutSlowInEasing),
        label = "day_memory_photo_alpha",
    )
    val scale = 1f - abs(animatedY / 900f).coerceIn(0f, 0.055f)
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = modifier
            .onGloballyPositioned { coordinates ->
                val next = coordinates.positionInRoot()
                val previous = lastPosition
                if (previous != null && !dismissed && lastPlacementAnimationKey != placementAnimationKey) {
                    val dx = previous.x - next.x
                    val dy = previous.y - next.y
                    if (abs(dx) > 0.5f || abs(dy) > 0.5f) {
                        scope.launch {
                            placementX.snapTo(dx)
                            placementX.animateTo(0f, animationSpec = tween(260, easing = FastOutSlowInEasing))
                        }
                        scope.launch {
                            placementY.snapTo(dy)
                            placementY.animateTo(0f, animationSpec = tween(260, easing = FastOutSlowInEasing))
                        }
                    }
                    lastPlacementAnimationKey = placementAnimationKey
                }
                lastPosition = next
            }
            .graphicsLayer {
                translationX = placementX.value
                translationY = animatedY + placementY.value + restoreY.value
                alpha = animatedAlpha * restoreAlpha.value
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .pointerInput(photo.id, openSuppressedUntilMillis) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val gestureStartedSuppressed = SystemClock.uptimeMillis() < openSuppressedUntilMillis
                    var totalX = 0f
                    var totalY = 0f
                    var deleting = false
                    var cancelledTap = gestureStartedSuppressed
                    var consumedByParent = false
                    val slop = viewConfiguration.touchSlop
                    val openSlop = (slop * 0.22f).coerceAtMost(4f)
                    val deleteThreshold = maxOf(260f, size.height * 0.36f)
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.changes.count { it.pressed } > 1) {
                            cancelledTap = true
                            break
                        }
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (change.changedToUpIgnoreConsumed()) break
                        if (change.isConsumed) {
                            consumedByParent = true
                            cancelledTap = true
                        }
                        val delta = change.positionChange()
                        totalX += delta.x
                        totalY += delta.y
                        if (!deleting) {
                            if (maxOf(abs(totalX), abs(totalY)) > openSlop) {
                                cancelledTap = true
                            }
                            if (abs(totalX) > slop * 0.82f && abs(totalX) > abs(totalY) * 1.05f) {
                                cancelledTap = true
                                break
                            }
                            deleting = totalY < -slop * 1.10f && abs(totalY) > abs(totalX) * 1.45f
                        }
                        if (deleting) {
                            change.consume()
                            dragY = (dragY + delta.y).coerceAtMost(0f)
                        }
                    }
                    if (deleting && dragY < -deleteThreshold && !deleteDispatched) {
                        deleteDispatched = true
                        dismissed = true
                        scope.launch {
                            kotlinx.coroutines.delay(230)
                            onDelete(photo)
                        }
                    } else if (
                        !cancelledTap &&
                        !consumedByParent &&
                        maxOf(abs(totalX), abs(totalY)) <= openSlop &&
                        SystemClock.uptimeMillis() >= openSuppressedUntilMillis
                    ) {
                        onOpen(photo)
                    } else {
                        dragY = 0f
                    }
                }
            }
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(Uri.parse(photo.uri))
                .memoryCacheKey(photo.uri + "#day_memory_grid")
                .diskCacheKey(photo.uri)
                .placeholderMemoryCacheKey(photo.uri + "#day_memory_grid")
                .size(560, 560)
                .crossfade(false)
                .build(),
            contentDescription = photo.displayName,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
    }
}

private const val MEMORY_PHOTO_WINDOW_DAY_COUNT = 7

private data class DeletedMemoryPhoto(
    val photo: PhotoEntity,
    val index: Int,
)

private fun buildMemoryPhotoWindow(currentPhoto: PhotoEntity, photos: List<PhotoEntity>): List<PhotoEntity> {
    val sorted = (photos + currentPhoto)
        .distinctBy { it.id }
        .sortedBy { memoryItemMillis(it) }
    if (sorted.isEmpty()) return listOf(currentPhoto)
    val dayKeys = sorted.map(::memoryDayKey).distinct()
    val currentDayKey = memoryDayKey(currentPhoto)
    val currentDayIndex = dayKeys.indexOf(currentDayKey)
        .takeIf { it >= 0 }
        ?: dayKeys.indexOfFirst { it >= currentDayKey }.takeIf { it >= 0 }
        ?: 0
    val halfWindow = MEMORY_PHOTO_WINDOW_DAY_COUNT / 2
    val firstStart = (currentDayIndex - halfWindow).coerceAtLeast(0)
    val firstEnd = (firstStart + MEMORY_PHOTO_WINDOW_DAY_COUNT).coerceAtMost(dayKeys.size)
    val start = (firstEnd - MEMORY_PHOTO_WINDOW_DAY_COUNT).coerceAtLeast(0)
    val selectedDayKeys = dayKeys.subList(start, firstEnd).toSet()
    return sorted.filter { memoryDayKey(it) in selectedDayKeys }.ifEmpty { listOf(currentPhoto) }
}

private fun memoryPhotoAspectRatio(photo: PhotoEntity): Float {
    val width = photo.width.takeIf { it > 0 } ?: photo.exifWidth?.takeIf { it > 0 } ?: 1
    val height = photo.height.takeIf { it > 0 } ?: photo.exifHeight?.takeIf { it > 0 } ?: 1
    return (width.toFloat() / height.toFloat()).coerceIn(0.30f, 4.80f)
}

@Composable
private fun KeepixMemoryDensityBar(
    total: Int,
    progress: Float,
    onDragStateChange: (Boolean) -> Unit = {},
    onSelect: (Float) -> Unit,
) {
    val currentOnSelect by rememberUpdatedState(onSelect)
    val currentOnDragStateChange by rememberUpdatedState(onDragStateChange)
    var dragProgress by remember { mutableFloatStateOf(Float.NaN) }
    var lastDispatchedBucket by remember { mutableStateOf(-1) }
    val visualProgress = if (dragProgress.isNaN()) progress else dragProgress
    val itemCount = total.coerceAtLeast(1)
    val selectedTick = (visualProgress.coerceIn(0f, 1f) * 27f).roundToInt().coerceIn(0, 27)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .pointerInput(itemCount) {
                fun selectFromX(x: Float) {
                    val width = size.width.toFloat().coerceAtLeast(1f)
                    val next = (x / width).coerceIn(0f, 1f)
                    val bucket = (next * (itemCount - 1).coerceAtLeast(0)).roundToInt()
                    dragProgress = next
                    if (bucket != lastDispatchedBucket) {
                        lastDispatchedBucket = bucket
                        currentOnSelect(next)
                    }
                }
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    lastDispatchedBucket = -1
                    currentOnDragStateChange(true)
                    selectFromX(down.position.x)
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break
                        if (change.changedToUpIgnoreConsumed()) break
                        selectFromX(change.position.x)
                        change.consume()
                    }
                    currentOnDragStateChange(false)
                    lastDispatchedBucket = -1
                    dragProgress = Float.NaN
                }
            },
    ) {
        Row(
            modifier = Modifier.align(Alignment.Center),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            repeat(28) { index ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 2.dp)
                        .width(if (index == selectedTick) 4.dp else 2.dp)
                        .height(if (index == selectedTick) 28.dp else 22.dp)
                        .background(Color.White.copy(alpha = if (index == selectedTick) 0.78f else 0.22f), RoundedCornerShape(999.dp)),
                )
            }
        }
    }
}

private fun memoryDayKey(photo: PhotoEntity): String {
    return SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(Date(memoryItemMillis(photo)))
}

private fun memoryItemMillis(photo: PhotoEntity): Long {
    return listOf(photo.dateTaken, photo.dateModified, photo.dateAdded)
        .firstOrNull { it > 0L }
        ?.let(::normalizeMediaMillis)
        ?: System.currentTimeMillis()
}

private fun normalizeMediaMillis(value: Long): Long {
    return if (value in 1L..10_000_000_000L) value * 1000L else value
}

@Composable
private fun KeepixTopPill(label: String, trailing: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.height(38.dp),
        shape = RoundedCornerShape(999.dp),
        color = Color.White.copy(alpha = 0.13f),
        contentColor = Color.White,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Text(label.ifBlank { "照片" }, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.22f), contentColor = Color.White) {
                Text(
                    trailing,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun KeepixProgressBar(progress: Float) {
    Box(
        modifier = Modifier
            .width(156.dp)
            .height(5.dp)
            .background(Color.White.copy(alpha = 0.18f), RoundedCornerShape(999.dp)),
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress.coerceIn(0.06f, 1f))
                .background(Color.White.copy(alpha = 0.92f), RoundedCornerShape(999.dp)),
        )
    }
}

@Composable
internal fun KeepixRoundButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val alpha = if (enabled) 1f else 0.42f
    Surface(
        onClick = onClick,
        modifier = modifier.size(60.dp),
        enabled = enabled,
        shape = CircleShape,
        color = Color.White.copy(alpha = 0.13f * alpha),
        contentColor = tint.copy(alpha = alpha),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f * alpha)),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(30.dp), tint = tint.copy(alpha = alpha))
        }
    }
}

@Composable
private fun KeepixBottomInfoBar(photo: PhotoEntity, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val place = photo.locationName?.takeIf { it.isNotBlank() } ?: photo.folderName.ifBlank { "本地相册" }
    Surface(
        onClick = onClick,
        modifier = modifier.height(66.dp),
        shape = RoundedCornerShape(999.dp),
        color = Color(0xFF151515).copy(alpha = 0.82f),
        contentColor = Color.White,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 22.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                keepixRelativeDateLabel(photo.dateTaken),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = Color.White,
                maxLines = 1,
            )
            Text(
                place,
                style = MaterialTheme.typography.titleSmall,
                color = Color.White.copy(alpha = 0.68f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun keepixRelativeDateLabel(timeMillis: Long): String {
    if (timeMillis <= 0L) return "未知时间"
    val days = ((System.currentTimeMillis() - timeMillis).coerceAtLeast(0L) / 86_400_000L).toInt()
    return when {
        days == 0 -> "今天"
        days == 1 -> "昨天"
        days < 30 -> days.toString() + " 天前"
        days < 365 -> (days / 30).coerceAtLeast(1).toString() + " 个月前"
        else -> (days / 365).coerceAtLeast(1).toString() + " 年前"
    }
}



