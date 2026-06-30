package com.futureape.kanleme.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.FilterAlt
import androidx.compose.material.icons.rounded.Folder
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
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
    onFavorite: () -> Unit,
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
                    onPinchToMemory = onOpenDayMemory,
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
                            icon = Icons.Rounded.FavoriteBorder,
                            contentDescription = null,
                            onClick = onFavorite,
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun KeepixDayMemoryOverlay(
    visible: Boolean,
    currentPhoto: PhotoEntity,
    photos: List<PhotoEntity>,
    onDismiss: () -> Unit,
    onOpen: (PhotoEntity) -> Unit,
    onDelete: (PhotoEntity) -> Unit,
    onUndo: () -> Unit,
    onApply: (String) -> Unit,
) {
    val context = LocalContext.current
    val memoryScope = rememberCoroutineScope()
    val currentDayKey = remember(currentPhoto.id, currentPhoto.dateTaken, currentPhoto.dateModified, currentPhoto.dateAdded) {
        memoryDayKey(currentPhoto)
    }
    val dayGroups = remember(currentPhoto.id, photos) { buildMemoryDays(currentPhoto, photos) }
    val initialPage = remember(currentDayKey, dayGroups) {
        dayGroups.indexOfFirst { it.key == currentDayKey }.coerceAtLeast(0)
    }
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { dayGroups.size.coerceAtLeast(1) })
    LaunchedEffect(currentDayKey, dayGroups.size) {
        val target = dayGroups.indexOfFirst { it.key == currentDayKey }
        if (target >= 0 && pagerState.currentPage != target) pagerState.scrollToPage(target)
    }
    val selectedPage = pagerState.settledPage.coerceIn(0, dayGroups.lastIndex.coerceAtLeast(0))
    val selectedDay = dayGroups.getOrNull(selectedPage) ?: MemoryDay(currentDayKey, currentDayKey.replace("-", "/"), listOf(currentPhoto))
    val pageProgress = if (dayGroups.size > 1) selectedPage.toFloat() / dayGroups.lastIndex.toFloat() else 0f
    var memoryScale by remember(currentPhoto.id) { mutableFloatStateOf(1f) }
    val rowCount = if (memoryScale < 0.86f) 3 else 2
    val returnProgress = ((memoryScale - 1.28f) / 0.30f).coerceIn(0f, 1f)
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(180)) + scaleIn(tween(240), initialScale = 0.86f),
        exit = fadeOut(tween(160)) + scaleOut(tween(200), targetScale = 0.86f),
        modifier = Modifier.fillMaxSize().zIndex(20f),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = (1f - returnProgress).coerceIn(0.08f, 1f) }
                .background(Color.Black.copy(alpha = 0.90f))
                .statusBarsPadding()
                .padding(horizontal = 18.dp, vertical = 12.dp)
                .pointerInput(currentPhoto.id, dayGroups.size) {
                    awaitEachGesture {
                        while (true) {
                            val event = awaitPointerEvent()
                            val pressed = event.changes.filter { it.pressed }
                            if (pressed.size < 2) break
                            val previousA = pressed[0].previousPosition
                            val previousB = pressed[1].previousPosition
                            val currentA = pressed[0].position
                            val currentB = pressed[1].position
                            val previousDistance = sqrt(
                                (previousA.x - previousB.x) * (previousA.x - previousB.x) +
                                    (previousA.y - previousB.y) * (previousA.y - previousB.y)
                            ).coerceAtLeast(1f)
                            val currentDistance = sqrt(
                                (currentA.x - currentB.x) * (currentA.x - currentB.x) +
                                    (currentA.y - currentB.y) * (currentA.y - currentB.y)
                            ).coerceAtLeast(1f)
                            val zoom = currentDistance / previousDistance
                            memoryScale = (memoryScale * zoom).coerceIn(0.66f, 1.62f)
                            pressed.forEach { it.consume() }
                            if (memoryScale >= 1.58f) {
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
                    Text(selectedDay.title, style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Black)
                }
                KeepixRoundButton(
                    icon = Icons.Rounded.Check,
                    contentDescription = null,
                    onClick = { onApply("d:" + selectedDay.key) },
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
                val rowHeight = if (rowCount == 3) {
                    ((maxHeight - gap * 2) / 3) * 0.88f
                } else {
                    ((maxHeight - gap) / 2) * (0.72f * memoryScale.coerceIn(0.86f, 1.28f))
                }
                val rowHeightPx = with(density) { rowHeight.roundToPx().coerceAtLeast(96) }
                val gapPx = with(density) { gap.roundToPx() }
                HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                    val day = dayGroups.getOrNull(page) ?: return@HorizontalPager
                    val dayScrollState = rememberScrollState()
                    KeepixMemoryPhotoTape(
                        context = context,
                        photos = day.photos,
                        rowHeightPx = rowHeightPx,
                        gapPx = gapPx,
                        rowCount = rowCount,
                        modifier = Modifier.horizontalScroll(dayScrollState),
                        onOpen = onOpen,
                        onDelete = onDelete,
                    )
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
                                memoryScope.launch { pagerState.animateScrollToPage(initialPage) }
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
                                total = dayGroups.size,
                                progress = pageProgress,
                                onSelect = { fraction ->
                                    memoryScope.launch {
                                        val target = (dayGroups.lastIndex * fraction).roundToInt().coerceIn(0, dayGroups.lastIndex.coerceAtLeast(0))
                                        pagerState.animateScrollToPage(target)
                                    }
                                },
                            )
                        }
                        KeepixRoundButton(
                            icon = Icons.AutoMirrored.Rounded.Undo,
                            contentDescription = null,
                            onClick = onUndo,
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
    modifier: Modifier,
    onOpen: (PhotoEntity) -> Unit,
    onDelete: (PhotoEntity) -> Unit,
) {
    var dragY by remember(photo.id) { mutableStateOf(0f) }
    var dismissed by remember(photo.id) { mutableStateOf(false) }
    val animatedY by animateFloatAsState(
        targetValue = if (dismissed) -280f else dragY,
        animationSpec = tween(170),
        label = "day_memory_photo_y",
    )
    val animatedAlpha by animateFloatAsState(
        targetValue = if (dismissed) 0f else 1f,
        animationSpec = tween(170),
        label = "day_memory_photo_alpha",
    )
    val scale = 1f - abs(animatedY / 900f).coerceIn(0f, 0.055f)
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = modifier
            .graphicsLayer {
                translationY = animatedY
                alpha = animatedAlpha
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .pointerInput(photo.id) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var totalX = 0f
                    var totalY = 0f
                    var deleting = false
                    val slop = viewConfiguration.touchSlop
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (change.changedToUpIgnoreConsumed()) break
                        val delta = change.positionChange()
                        totalX += delta.x
                        totalY += delta.y
                        if (!deleting) {
                            if (abs(totalX) > slop && abs(totalX) > abs(totalY)) break
                            deleting = totalY < -slop && abs(totalY) > abs(totalX) * 1.45f
                        }
                        if (deleting) {
                            change.consume()
                            dragY = (dragY + delta.y).coerceIn(-210f, 0f)
                        }
                    }
                    if (deleting && dragY < -150f) {
                        dismissed = true
                        onDelete(photo)
                    } else {
                        dragY = 0f
                    }
                }
            }
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(Uri.parse(photo.uri))
                .memoryCacheKey(photo.uri)
                .diskCacheKey(photo.uri)
                .placeholderMemoryCacheKey(photo.uri)
                .crossfade(false)
                .build(),
            contentDescription = photo.displayName,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
    }
}

@Composable
private fun KeepixMemoryPhotoTape(
    context: android.content.Context,
    photos: List<PhotoEntity>,
    rowHeightPx: Int,
    gapPx: Int,
    rowCount: Int,
    modifier: Modifier,
    onOpen: (PhotoEntity) -> Unit,
    onDelete: (PhotoEntity) -> Unit,
) {
    Layout(
        modifier = modifier,
        content = {
            photos.forEach { photo ->
                KeepixMemoryGridPhoto(
                    context = context,
                    photo = photo,
                    modifier = Modifier,
                    onOpen = onOpen,
                    onDelete = onDelete,
                )
            }
        },
    ) { measurables, constraints ->
        val widths = photos.map { photo ->
            (rowHeightPx * memoryPhotoAspectRatio(photo)).roundToInt().coerceAtLeast(rowHeightPx / 2)
        }
        val safeRowCount = rowCount.coerceIn(1, 3)
        val rowX = IntArray(safeRowCount)
        val xOffsets = IntArray(measurables.size)
        val placeables = measurables.mapIndexed { index, measurable ->
            val row = index % safeRowCount
            val width = widths[index]
            xOffsets[index] = rowX[row]
            rowX[row] += width + gapPx
            measurable.measure(Constraints.fixed(width, rowHeightPx))
        }
        val contentWidth = (rowX.maxOrNull() ?: 0).let { if (it > 0) it - gapPx else 0 }
        val layoutWidth = contentWidth.coerceAtLeast(constraints.minWidth)
        val layoutHeight = rowHeightPx * safeRowCount + gapPx * (safeRowCount - 1)
        layout(layoutWidth, layoutHeight) {
            placeables.forEachIndexed { index, placeable ->
                val y = (index % safeRowCount) * (rowHeightPx + gapPx)
                placeable.placeRelative(xOffsets[index], y)
            }
        }
    }
}

private fun memoryPhotoAspectRatio(photo: PhotoEntity): Float {
    val width = photo.width.takeIf { it > 0 } ?: photo.exifWidth?.takeIf { it > 0 } ?: 1
    val height = photo.height.takeIf { it > 0 } ?: photo.exifHeight?.takeIf { it > 0 } ?: 1
    return (width.toFloat() / height.toFloat()).coerceIn(0.36f, 3.20f)
}

private data class MemoryDay(
    val key: String,
    val title: String,
    val photos: List<PhotoEntity>,
)

private fun buildMemoryDays(currentPhoto: PhotoEntity, photos: List<PhotoEntity>): List<MemoryDay> {
    val unique = (listOf(currentPhoto) + photos).distinctBy { it.id }
    return unique
        .groupBy { memoryDayKey(it) }
        .toSortedMap()
        .map { (key, dayPhotos) ->
            MemoryDay(
                key = key,
                title = key.replace("-", "/"),
                photos = dayPhotos.sortedByDescending { memoryItemMillis(it) },
            )
        }
}

@Composable
private fun KeepixMemoryThumb(context: android.content.Context, photo: PhotoEntity, modifier: Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        color = Color.White.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(Uri.parse(photo.uri))
                .memoryCacheKey(photo.uri)
                .diskCacheKey(photo.uri)
                .placeholderMemoryCacheKey(photo.uri)
                .crossfade(false)
                .build(),
            contentDescription = photo.displayName,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
    }
}

@Composable
private fun KeepixMemoryInstructionCard(title: String, subtitle: String, modifier: Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        color = Color(0xFF2E2E2E).copy(alpha = 0.86f),
        contentColor = Color.White,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("↘", style = MaterialTheme.typography.displaySmall, color = Color.White.copy(alpha = 0.76f), fontWeight = FontWeight.Black)
            Spacer(Modifier.height(22.dp))
            Text(title, style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Black)
            Text(subtitle, style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun KeepixMemoryDensityBar(
    total: Int,
    progress: Float,
    onSelect: (Float) -> Unit,
) {
    val itemCount = total.coerceAtLeast(1)
    val selectedTick = (progress.coerceIn(0f, 1f) * 27f).roundToInt().coerceIn(0, 27)
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .pointerInput(itemCount) {
                fun selectFromX(x: Float) {
                    val width = size.width.toFloat().coerceAtLeast(1f)
                    onSelect((x / width).coerceIn(0f, 1f))
                }
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    selectFromX(down.position.x)
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break
                        if (change.changedToUpIgnoreConsumed()) break
                        selectFromX(change.position.x)
                        change.consume()
                    }
                }
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        repeat(28) { index ->
            Box(
                modifier = Modifier
                    .padding(horizontal = 2.dp)
                    .width(if (index == selectedTick) 4.dp else 2.dp)
                    .height(if (index == selectedTick) 28.dp else 22.dp)
                    .background(Color.White.copy(alpha = if (index == selectedTick) 0.92f else 0.28f), RoundedCornerShape(999.dp)),
            )
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



