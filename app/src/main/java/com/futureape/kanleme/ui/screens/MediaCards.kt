package com.futureape.kanleme.ui.screens

import android.net.Uri
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Swipe
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.futureape.kanleme.data.local.PhotoEntity
import com.futureape.kanleme.data.local.VideoEntity
import com.futureape.kanleme.data.repository.SwipeAction
import com.futureape.kanleme.data.settings.AppSettings
import com.futureape.kanleme.data.settings.GestureDirection
import com.futureape.kanleme.ui.util.HapticKit
import com.futureape.kanleme.ui.util.formatDate
import com.futureape.kanleme.ui.util.formatDuration
import com.futureape.kanleme.ui.util.formatSize
import com.futureape.kanleme.ui.components.GlassSurface
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max

@Composable
fun ScreenHeader(title: String, subtitle: String? = null, onBack: (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "返回") }
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineMedium)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun OrganizerTopBar(
    current: Int,
    total: Int,
    deletedCount: Int,
    modifier: Modifier = Modifier,
    lightContent: Boolean = false,
) {
    val contentColor = if (lightContent) Color.White else MaterialTheme.colorScheme.onSurface
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        GlassPill(text = "全部时间", icon = Icons.Rounded.CalendarToday, contentColor = contentColor)
        GlassCircleIcon(Icons.Rounded.Folder, contentColor = contentColor)
        GlassPill(text = "随机", icon = Icons.Rounded.Swipe, contentColor = contentColor)
        GlassPill(text = deletedCount.toString(), icon = Icons.Rounded.Delete, contentColor = contentColor)
        GlassPill(text = "$current/$total", minWidth = 118.dp, contentColor = contentColor, emphasized = true)
    }
}

@Composable
fun ShuffleSessionButton(
    label: String = "重新随机",
    modifier: Modifier = Modifier,
    lightContent: Boolean = false,
    onClick: () -> Unit,
) {
    val contentColor = if (lightContent) Color.White else MaterialTheme.colorScheme.primary
    Surface(
        modifier = modifier.height(44.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(999.dp),
        color = if (lightContent) Color.White.copy(alpha = 0.16f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
        contentColor = contentColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (lightContent) Color.White.copy(alpha = 0.24f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Icon(Icons.Rounded.Swipe, contentDescription = null, modifier = Modifier.size(18.dp), tint = contentColor)
            Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
    }
}

@Composable
fun FilterChipRail(
    modifier: Modifier = Modifier,
    selectedIndex: Int = 0,
    chips: List<Pair<String, String>> = listOf("全部" to "20807", "普通照片" to "17246", "截图" to "3230", "长图" to "401"),
    onSelected: (Int) -> Unit = {},
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        chips.forEachIndexed { index, chip ->
            Surface(
                modifier = Modifier.height(44.dp).clickable { onSelected(index) },
                shape = RoundedCornerShape(999.dp),
                color = if (index == selectedIndex) MaterialTheme.colorScheme.primary else adaptiveWhiteSurface(0.34f),
                contentColor = if (index == selectedIndex) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, adaptiveWhiteBorder(0.45f)),
            ) {
                Row(
                    Modifier.padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(chip.first, style = MaterialTheme.typography.titleMedium)
                    Text(chip.second, style = MaterialTheme.typography.titleMedium, color = if (index == selectedIndex) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.78f) else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun GlassPill(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    trailing: String? = null,
    minWidth: androidx.compose.ui.unit.Dp = 0.dp,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    emphasized: Boolean = false,
) {
    Surface(
        modifier = modifier.height(46.dp).then(if (minWidth > 0.dp) Modifier.width(minWidth) else Modifier),
        shape = RoundedCornerShape(999.dp),
        color = adaptiveWhiteSurface(if (emphasized) 0.18f else 0.13f),
        contentColor = contentColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, adaptiveWhiteBorder(0.22f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = contentColor.copy(alpha = 0.78f))
                Spacer(Modifier.width(7.dp))
            }
            Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1)
            if (trailing != null) {
                Spacer(Modifier.width(8.dp))
                Text(trailing, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun GlassCircleIcon(icon: ImageVector, contentColor: Color = MaterialTheme.colorScheme.onSurface, onClick: (() -> Unit)? = null) {
    Surface(
        modifier = Modifier.size(46.dp).clip(CircleShape).then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = CircleShape,
        color = adaptiveWhiteSurface(0.13f),
        contentColor = contentColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, adaptiveWhiteBorder(0.22f)),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = contentColor.copy(alpha = 0.78f))
        }
    }
}

@Composable
fun PhotoDeckStage(
    photos: List<PhotoEntity>,
    settings: AppSettings,
    haptics: HapticKit,
    modifier: Modifier = Modifier,
    onOpen: (PhotoEntity) -> Unit = {},
    onAction: (PhotoEntity, SwipeAction) -> Unit,
) {
    val top = photos.firstOrNull() ?: return
    val context = LocalContext.current
    Box(modifier, contentAlignment = Alignment.Center) {
        photos.drop(1).take(3).reversed().forEachIndexed { index, photo ->
            val depth = 3 - index
            val animatedLift by animateFloatAsState(
                targetValue = depth * 20f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                label = "deck_lift_$depth",
            )
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(Uri.parse(photo.uri))
                    .memoryCacheKey(photo.uri)
                    .diskCacheKey(photo.uri)
                    .placeholderMemoryCacheKey(photo.uri)
                    .crossfade(false)
                    .build(),
                contentDescription = photo.displayName,
                modifier = Modifier
                    .fillMaxWidth((0.90f - depth * 0.018f).coerceAtLeast(0.80f))
                    .aspectRatio(photoDisplayAspectRatio(photo))
                    .graphicsLayer {
                        translationY = animatedLift.dp.toPx()
                        rotationZ = when (depth) { 1 -> -2.8f; 2 -> 2.2f; else -> -1.2f }
                        scaleX = 1f - depth * 0.045f
                        scaleY = 1f - depth * 0.045f
                        alpha = 0.90f - depth * 0.13f
                    }
                    .shadow(28.dp, RoundedCornerShape(30.dp), ambientColor = Color.Black.copy(alpha = 0.18f), spotColor = Color.Black.copy(alpha = 0.20f))
                    .clip(RoundedCornerShape(30.dp)),
                contentScale = ContentScale.Fit,
            )
        }
        SwipePhotoCard(
            photo = top,
            settings = settings,
            haptics = haptics,
            onOpen = { onOpen(top) },
            onAction = { onAction(top, it) },
            modifier = Modifier.fillMaxWidth(0.96f),
        )
    }
}

@Composable
fun SwipePhotoCard(
    photo: PhotoEntity,
    settings: AppSettings,
    haptics: HapticKit,
    onOpen: () -> Unit = {},
    onAction: (SwipeAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val photoRatio = remember(photo.id, photo.width, photo.height, photo.exifWidth, photo.exifHeight) { photoDisplayAspectRatio(photo) }
    val offsetX = remember(photo.id) { Animatable(0f) }
    val offsetY = remember(photo.id) { Animatable(0f) }
    // Do not fade the incoming photo from transparent; that exposes the backdrop and reads as a flash.
    // The new card now rises from the already-rendered deck layer with a small scale/position motion.
    val entryAlpha = remember(photo.id) { Animatable(1f) }
    val entryScale = remember(photo.id) { Animatable(0.955f) }
    val entryOffsetY = remember(photo.id) { Animatable(38f) }
    var lastZone by remember(photo.id) { mutableStateOf("") }
    var activeHint by remember(photo.id) { mutableStateOf<SwipeAction?>(null) }
    val feedbackAlpha by animateFloatAsState(
        targetValue = (abs(offsetX.value) / 260f + abs(offsetY.value) / 260f).coerceIn(0f, 1f),
        animationSpec = tween(120),
        label = "swipe_feedback",
    )
    val rotation = (offsetX.value / 34f).coerceIn(-16f, 16f)
    // 手势判定改为“防误删优先”：
    // 1) 进入拖拽需要更明确的主方向位移；
    // 2) 待删属于破坏性动作，需要更高纵向阈值；
    // 3) 纵向/横向必须有明显主轴优势，避免斜向滑动误判。
    val safeVerticalScale = settings.photoThreshold.coerceAtLeast(1f)
    val deleteVerticalThreshold = 280f * safeVerticalScale
    val favoriteVerticalThreshold = 220f * settings.photoThreshold
    val horizontalThreshold = 230f * settings.photoThreshold
    val verticalDominanceRatio = 1.38f
    val horizontalDominanceRatio = 1.22f

    LaunchedEffect(photo.id) {
        launch { entryScale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)) }
        entryOffsetY.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
    }

    fun verticalAction(y: Float): SwipeAction {
        val defaultAction = if (y < 0f) SwipeAction.Delete else SwipeAction.Favorite
        return if (settings.gestureDirection == GestureDirection.DEFAULT) defaultAction else {
            if (defaultAction == SwipeAction.Delete) SwipeAction.Favorite else SwipeAction.Delete
        }
    }

    fun classify(x: Float, y: Float): SwipeAction? {
        val absX = abs(x)
        val absY = abs(y)
        val verticalDominant = absY >= absX * verticalDominanceRatio
        val horizontalDominant = absX >= absY * horizontalDominanceRatio
        if (verticalDominant) {
            val action = verticalAction(y)
            val needed = if (action == SwipeAction.Delete) deleteVerticalThreshold else favoriteVerticalThreshold
            if (absY > needed) return action
        }
        if (horizontalDominant && absX > horizontalThreshold) return SwipeAction.Keep
        return null
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                translationX = offsetX.value
                translationY = offsetY.value + entryOffsetY.value
                rotationZ = rotation
                val distance = (abs(offsetX.value) + abs(offsetY.value)).coerceAtMost(520f) / 520f
                scaleX = entryScale.value * (1f - distance * 0.035f)
                scaleY = entryScale.value * (1f - distance * 0.035f)
                alpha = entryAlpha.value
            }
            .pointerInput(photo.id, settings) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var totalX = 0f
                    var totalY = 0f
                    var dragging = false
                    val slop = viewConfiguration.touchSlop
                    val dragStartSlop = slop * 1.35f
                    val tapSlop = slop * 1.10f
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (change.changedToUpIgnoreConsumed()) break
                        val delta = change.positionChange()
                        if (delta.x != 0f || delta.y != 0f) {
                            totalX += delta.x
                            totalY += delta.y
                            if (!dragging && max(abs(totalX), abs(totalY)) > dragStartSlop) {
                                dragging = true
                                haptics.tick()
                            }
                            if (dragging) {
                                change.consume()
                                val nextX = offsetX.value + delta.x
                                val nextY = offsetY.value + delta.y
                                val zoneAction = classify(nextX, nextY)
                                activeHint = zoneAction
                                val zone = zoneAction?.name.orEmpty()
                                if (zone.isNotEmpty() && zone != lastZone) {
                                    lastZone = zone
                                    haptics.threshold()
                                } else if (zone.isEmpty()) {
                                    lastZone = ""
                                }
                                scope.launch {
                                    offsetX.snapTo(nextX)
                                    offsetY.snapTo(nextY)
                                }
                            }
                        }
                    }

                    if (!dragging && max(abs(totalX), abs(totalY)) <= tapSlop) {
                        haptics.tick()
                        onOpen()
                    } else {
                        val action = if (dragging) classify(offsetX.value, offsetY.value) else null
                        if (action == null) {
                            activeHint = null
                            scope.launch { offsetX.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)) }
                            scope.launch { offsetY.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)) }
                        } else {
                            val targetX = when (action) {
                                SwipeAction.Keep -> if (offsetX.value < 0) -980f else 980f
                                else -> offsetX.value * 1.75f
                            }
                            val targetY = when (action) {
                                SwipeAction.Delete -> if (settings.gestureDirection == GestureDirection.DEFAULT) -1260f else 1260f
                                SwipeAction.Favorite -> if (settings.gestureDirection == GestureDirection.DEFAULT) 1260f else -1260f
                                SwipeAction.Keep -> offsetY.value
                            }
                            scope.launch {
                                offsetX.animateTo(targetX, tween(260, easing = FastOutSlowInEasing))
                            }
                            scope.launch {
                                offsetY.animateTo(targetY, tween(260, easing = FastOutSlowInEasing))
                                onAction(action)
                                // Keep the old card off-screen until the deck state swaps.
                                // Snapping it back to 0 here caused a one-frame jump/flash before the next photo appeared.
                                activeHint = null
                                lastZone = ""
                            }
                        }
                    }
                }
            }
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(photoRatio)
                .clip(RoundedCornerShape(32.dp))
                .background(Color.Black.copy(alpha = 0.96f))
                .border(1.dp, adaptiveWhiteBorder(0.38f), RoundedCornerShape(32.dp))
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
                contentScale = ContentScale.Fit,
            )
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.10f), Color.Black.copy(alpha = 0.28f))
                    )
                )
            )
            val actionHint = activeHint
            if (actionHint != null && feedbackAlpha > 0.18f) {
                val hintAlignment = when (actionHint) {
                    SwipeAction.Delete -> Alignment.TopCenter
                    SwipeAction.Favorite -> Alignment.BottomCenter
                    SwipeAction.Keep -> if (offsetX.value < 0f) Alignment.CenterStart else Alignment.CenterEnd
                }
                Box(
                    modifier = Modifier
                        .align(hintAlignment)
                        .padding(18.dp)
                        .graphicsLayer { alpha = feedbackAlpha },
                ) {
                    EdgeSwipeHint(actionHint)
                }
            }
        }
    }
}

@Composable
private fun EdgeSwipeHint(action: SwipeAction?) {
    val (label, tint) = when (action) {
        SwipeAction.Delete -> "松手待删" to Color(0xFFE66A6A)
        SwipeAction.Favorite -> "松手收藏" to Color(0xFFE6A63E)
        SwipeAction.Keep -> "松手保留" to Color(0xFF68A7D8)
        null -> "" to MaterialTheme.colorScheme.primary
    }
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = tint.copy(alpha = 0.96f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)),
        shadowElevation = 0.dp,
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 13.dp),
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
fun FolderChipRail(
    folders: List<String>,
    selected: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.size(width = 58.dp, height = 44.dp),
            shape = RoundedCornerShape(999.dp),
            color = adaptiveWhiteSurface(0.26f),
            border = androidx.compose.foundation.BorderStroke(1.dp, adaptiveWhiteBorder(0.32f)),
        ) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Add, contentDescription = "添加文件夹") } }
        folders.forEach { folder ->
            val isSelected = folder == selected
            Surface(
                modifier = Modifier.height(44.dp).clickable { onSelected(folder) },
                shape = RoundedCornerShape(999.dp),
                color = if (isSelected) adaptiveWhiteSurface(0.42f) else adaptiveWhiteSurface(0.20f),
                border = androidx.compose.foundation.BorderStroke(1.dp, adaptiveWhiteBorder(0.34f)),
            ) {
                Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (isSelected) {
                        Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(folder.take(18), maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
fun BottomPhotoInfoBar(
    index: Int,
    photo: PhotoEntity,
    onShare: () -> Unit,
    onOpen: () -> Unit = {},
    modifier: Modifier = Modifier,
    shareLeft: Boolean = false,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (shareLeft) ShareCircle(onShare)
        Surface(
            modifier = Modifier.size(64.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.34f),
            border = androidx.compose.foundation.BorderStroke(1.dp, adaptiveWhiteBorder(0.38f)),
        ) { Box(contentAlignment = Alignment.Center) { Text(index.toString(), style = MaterialTheme.typography.headlineSmall, color = Color.White) } }
        Surface(
            modifier = Modifier.weight(1f).height(70.dp).clip(RoundedCornerShape(28.dp)).clickable(onClick = onOpen),
            shape = RoundedCornerShape(28.dp),
            color = adaptiveWhiteSurface(0.26f),
            border = androidx.compose.foundation.BorderStroke(1.dp, adaptiveWhiteBorder(0.34f)),
        ) {
            Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.Center) {
                Text(formatDate(photo.dateTaken), style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(photo.folderName + " · " + formatSize(photo.size) + " · 点按查看大图", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (!shareLeft) ShareCircle(onShare)
    }
}

@Composable
private fun ShareCircle(onShare: () -> Unit) {
    Surface(
        modifier = Modifier.size(64.dp).clickable(onClick = onShare),
        shape = CircleShape,
        color = adaptiveWhiteSurface(0.26f),
        border = androidx.compose.foundation.BorderStroke(1.dp, adaptiveWhiteBorder(0.34f)),
    ) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Share, contentDescription = "分享") } }
}


@Composable
fun PhotoActionMiniRail(
    onKeep: () -> Unit,
    onFavorite: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.width(76.dp),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.58f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)),
    ) {
        Column(
            modifier = Modifier.fillMaxHeight().padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly,
        ) {
            RailIconCompact(Icons.Rounded.Close, "保留", Color(0xFF68A7D8), onKeep)
            RailIconCompact(Icons.Rounded.FavoriteBorder, "收藏", Color(0xFFE6A63E), onFavorite)
            RailIconCompact(Icons.Rounded.Delete, "待删", Color(0xFFE66A6A), onDelete)
        }
    }
}

@Composable
private fun RailIconCompact(icon: ImageVector, description: String, color: Color, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Surface(
            modifier = Modifier.size(42.dp).clickable(onClick = onClick),
            shape = CircleShape,
            color = color.copy(alpha = 0.18f),
            border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.32f)),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = description, tint = color, modifier = Modifier.size(22.dp))
            }
        }
        Text(description, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun MediaActionRail(
    onKeep: () -> Unit,
    onFavorite: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    deletedCount: Int = 0,
) {
    Surface(
        modifier = modifier.width(72.dp),
        shape = RoundedCornerShape(34.dp),
        color = Color.Black.copy(alpha = 0.10f),
        border = androidx.compose.foundation.BorderStroke(1.dp, adaptiveWhiteBorder(0.18f)),
    ) {
        Column(
            modifier = Modifier.fillMaxHeight().padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly,
        ) {
            ProgressBubble(deletedCount)
            RailIcon(Icons.Rounded.FavoriteBorder, "收藏", onFavorite)
            RailIcon(Icons.Rounded.Delete, "待删", onDelete)
            RailIcon(Icons.Rounded.Share, "保留", onKeep)
        }
    }
}

@Composable
private fun ProgressBubble(value: Int) {
    Box(
        modifier = Modifier.size(54.dp).drawBehind {
            drawCircle(color = Color.White.copy(alpha = 0.12f))
            drawArc(
                color = Color.White.copy(alpha = 0.72f),
                startAngle = -90f,
                sweepAngle = (value.coerceAtLeast(1) % 10) * 36f,
                useCenter = false,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4.dp.toPx()),
            )
        },
        contentAlignment = Alignment.Center,
    ) {
        Text(value.toString(), style = MaterialTheme.typography.titleMedium, color = Color.White)
    }
}

@Composable
private fun RailIcon(icon: ImageVector, description: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(54.dp)) {
        Icon(icon, contentDescription = description, tint = Color.White, modifier = Modifier.size(28.dp))
    }
}

@Composable
fun PhotoGuideDialog(onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(0.86f),
        shape = RoundedCornerShape(34.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 28.dp,
    ) {
        Column(Modifier.padding(26.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("操作提示", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(22.dp))
            GuideRow(Icons.Rounded.Visibility, "单击图片", "进入大图查看模式")
            GuideRow(Icons.Rounded.Visibility, "双击屏幕", "切换导航栏显示或隐藏")
            GuideRow(Icons.Rounded.MoreHoriz, "左下角进度圈", "点击可撤回查看上一张")
            GuideRow(Icons.Rounded.Folder, "指定归档", "选择目标文件夹后，保留和收藏会自动归档")
            Spacer(Modifier.height(16.dp))
            Surface(
                modifier = Modifier.fillMaxWidth().height(54.dp).clip(RoundedCornerShape(16.dp)).clickable(onClick = onDismiss),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.48f),
            ) { Box(contentAlignment = Alignment.Center) { Text("知道了（4）", color = Color.White, style = MaterialTheme.typography.titleMedium) } }
        }
    }
}

@Composable
fun VideoGuideDialog(onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(0.86f),
        shape = RoundedCornerShape(34.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 28.dp,
    ) {
        Column(Modifier.padding(26.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("操作提示", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(22.dp))
            GuideRow(Icons.Rounded.Swipe, "上下滑动", "切换到上一个或下一个视频")
            GuideRow(Icons.Rounded.Swipe, "左右滑动", "快进或快退调节播放进度")
            GuideRow(Icons.Rounded.MoreHoriz, "长按屏幕", "2 倍速播放，松开恢复正常")
            GuideRow(Icons.Rounded.Visibility, "双击屏幕", "切换导航栏显示或隐藏")
            Spacer(Modifier.height(16.dp))
            Surface(
                modifier = Modifier.fillMaxWidth().height(54.dp).clip(RoundedCornerShape(16.dp)).clickable(onClick = onDismiss),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.48f),
            ) { Box(contentAlignment = Alignment.Center) { Text("知道了（4）", color = Color.White, style = MaterialTheme.typography.titleMedium) } }
        }
    }
}

@Composable
private fun GuideRow(icon: ImageVector, title: String, text: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Surface(modifier = Modifier.size(56.dp), shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)) {
            Box(contentAlignment = Alignment.Center) { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun MediaActionRow(onKeep: () -> Unit, onFavorite: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ActionBubble(Icons.Rounded.Close, "保留", onKeep)
        ActionBubble(Icons.Rounded.FavoriteBorder, "收藏", onFavorite, large = true)
        ActionBubble(Icons.Rounded.Delete, "待删", onDelete)
    }
}

@Composable
private fun ActionBubble(icon: ImageVector, label: String, onClick: () -> Unit, large: Boolean = false) {
    Surface(
        modifier = Modifier.size(if (large) 68.dp else 58.dp).clip(CircleShape).clickable(onClick = onClick),
        shape = CircleShape,
        color = adaptiveWhiteSurface(0.32f),
        border = androidx.compose.foundation.BorderStroke(1.dp, adaptiveWhiteBorder(0.38f)),
    ) {
        Box(contentAlignment = Alignment.Center) { Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary) }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PhotoGrid(photos: List<PhotoEntity>, modifier: Modifier = Modifier, onPhotoClick: ((PhotoEntity) -> Unit)? = null) {
    val context = LocalContext.current
    LazyVerticalGrid(
        columns = GridCells.Adaptive(112.dp),
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(photos, key = { it.id }) { photo ->
            val shape = RoundedCornerShape(22.dp)
            Surface(
                modifier = Modifier.aspectRatio(photoDisplayAspectRatio(photo).coerceIn(0.72f, 1.38f)),
                shape = shape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.42f),
                shadowElevation = 3.dp,
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
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(shape)
                        .then(if (onPhotoClick != null) Modifier.clickable { onPhotoClick?.invoke(photo) } else Modifier),
                    contentScale = ContentScale.Fit,
                )
            }
        }
    }
}

@Composable
fun VideoCard(video: VideoEntity, onAction: (SwipeAction) -> Unit) {
    GlassSurface(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(24.dp))) {
                AsyncImage(
                    model = Uri.parse(video.uri),
                    contentDescription = video.displayName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
                Text(
                    formatDuration(video.duration),
                    modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp).background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(999.dp)).padding(horizontal = 10.dp, vertical = 5.dp),
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(video.displayName, style = MaterialTheme.typography.titleMedium)
            Text("${video.folderName} · ${formatSize(video.size)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(10.dp))
            MediaActionRow(
                onKeep = { onAction(SwipeAction.Keep) },
                onFavorite = { onAction(SwipeAction.Favorite) },
                onDelete = { onAction(SwipeAction.Delete) },
            )
        }
    }
}

private fun photoDisplayAspectRatio(photo: PhotoEntity): Float {
    val width = photo.width.takeIf { it > 0 } ?: photo.exifWidth?.takeIf { it > 0 } ?: 1
    val height = photo.height.takeIf { it > 0 } ?: photo.exifHeight?.takeIf { it > 0 } ?: 1
    val raw = width.toFloat() / height.toFloat()
    // Keep the real photo orientation visible, while avoiding unusably tall/wide cards on phones.
    return raw.coerceIn(0.58f, 1.70f)
}



@Composable
private fun adaptiveWhiteSurface(lightAlpha: Float): Color {
    val oledDark = MaterialTheme.colorScheme.background.luminance() < 0.03f
    return if (oledDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.70f) else Color.White.copy(alpha = lightAlpha)
}

@Composable
private fun adaptiveWhiteBorder(lightAlpha: Float): Color {
    val oledDark = MaterialTheme.colorScheme.background.luminance() < 0.03f
    return if (oledDark) MaterialTheme.colorScheme.outline.copy(alpha = 0.58f) else Color.White.copy(alpha = lightAlpha)
}
