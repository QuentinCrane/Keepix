package com.futureape.kanleme.ui.screens

import com.futureape.kanleme.ui.components.morphAnchor
import com.futureape.kanleme.ui.components.MorphPanel
import com.futureape.kanleme.ui.util.photoImageRequest

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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.futureape.kanleme.data.local.PhotoEntity
import com.futureape.kanleme.data.local.VideoEntity
import com.futureape.kanleme.data.repository.CleaningScope
import com.futureape.kanleme.data.repository.DashboardStats
import com.futureape.kanleme.data.settings.AppSettings
import com.futureape.kanleme.R
import com.futureape.kanleme.ui.components.AdaptiveCenter
import com.futureape.kanleme.ui.components.AdaptiveWidthInfo
import com.futureape.kanleme.ui.components.GlassSurface
import com.futureape.kanleme.ui.util.formatSize
import com.futureape.kanleme.ui.util.photoDisplayAspectRatio
import com.futureape.kanleme.ui.util.rememberHapticKit
import com.futureape.kanleme.ui.util.shareDailyReportImage
import com.futureape.kanleme.ui.viewmodel.KanlemeViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Keepix immersive home visual path. Do not put legacy Liquid Glass UI in this file.
@Composable
internal fun ImmersiveCleanHomeScreen(
    contentPadding: PaddingValues,
    animateDeckEntry: Boolean,
    dashboard: DashboardStats,
    settings: AppSettings,
    photoScope: CleaningScope,
    videoScope: CleaningScope,
    photos: List<PhotoEntity>,
    videos: List<VideoEntity>,
    photoPreparing: Boolean,
    videoPreparing: Boolean,
    selectedIsPhoto: Boolean,
    onPhotoTab: () -> Unit,
    onVideoTab: () -> Unit,
    onPhoto: () -> Unit,
    onVideo: () -> Unit,
    onTimeline: () -> Unit,
    onTrash: () -> Unit,
    onFavorites: () -> Unit,
    onToday: () -> Unit,
    onSettings: () -> Unit,
    onPhotoType: (String) -> Unit,
    onPhotoDate: (String) -> Unit,
    onPhotoSort: (String) -> Unit,
    onPhotoBatch: (Int) -> Unit,
    onVideoDate: (String) -> Unit,
    onVideoSort: (String) -> Unit,
    onVideoBatch: (Int) -> Unit,
    onDailyReport: () -> Unit,
) {
    val context = LocalContext.current
    var menuExpanded by remember { mutableStateOf(false) }
    var menuPanel by remember { mutableStateOf(KeepixMenuPanel.ROOT) }
    LaunchedEffect(menuExpanded) {
        if (!menuExpanded) menuPanel = KeepixMenuPanel.ROOT
    }
    val previewContext = LocalContext.current
    val previews = if (selectedIsPhoto) {
        photos.take(3).map { KeepixPreviewItem(it.uri, it.displayName, false, photoDisplayAspectRatio(it, minRatio = 0.42f, maxRatio = 2.20f),
            photoImageRequest(previewContext, it, "photo_fit", 1080, 1440)) }
    } else {
        videos.take(3).map { KeepixPreviewItem(it.uri, it.displayName, true, keepixPhotoAspectRatio(it.width, it.height)) }
    }
    val canOpenDeck = if (selectedIsPhoto) {
        previews.isNotEmpty() || photoPreparing || dashboard.processedPhotoCount < dashboard.photoCount
    } else {
        previews.isNotEmpty() || videoPreparing || dashboard.processedVideoCount < dashboard.videoCount
    }
    val heroUri = previews.firstOrNull()?.uri
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (heroUri != null) {
            AsyncImage(
                model = previews.firstOrNull()?.imageModel ?: ImageRequest.Builder(context)
                    .data(Uri.parse(heroUri))
                    .memoryCacheKey(heroUri + "#home_blur")
                    .diskCacheKey(heroUri)
                    .placeholderMemoryCacheKey(heroUri + "#home_deck")
                    .size(1080, 1080)
                    .crossfade(false)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(34.dp)
                    .graphicsLayer {
                        scaleX = 1.12f
                        scaleY = 1.12f
                        alpha = 0.58f
                    },
                contentScale = ContentScale.Crop,
            )
        }
        Box(
            Modifier.fillMaxSize().background(
                androidx.compose.ui.graphics.Brush.verticalGradient(
                    listOf(
                        Color.Black.copy(alpha = 0.54f),
                        Color.Black.copy(alpha = 0.18f),
                        Color.Black.copy(alpha = 0.40f),
                        Color.Black.copy(alpha = 0.88f),
                    )
                )
            )
        )
        AdaptiveCenter(maxWidth = 760.dp) {
            Box(Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 22.dp,
                        end = 22.dp,
                        top = 58.dp,
                        bottom = contentPadding.calculateBottomPadding(),
                    ),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().zIndex(2f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start,
                        ) {
                            Box(Modifier.height(50.dp), contentAlignment = Alignment.CenterStart) {
                                if (!menuExpanded) {
                                    KeepixQueueSelector(
                                        selectedIsPhoto = selectedIsPhoto,
                                        mediaType = photoScope.mediaType,
                                        onClick = { menuExpanded = true },
                                    )
                                }
                            }
                        }
                    }
                    item {
                        KeepixHomeDeck(
                            items = previews,
                            preparing = if (selectedIsPhoto) photoPreparing else videoPreparing,
                            selectedIsPhoto = selectedIsPhoto,
                            canOpen = canOpenDeck,
                            animateEntry = animateDeckEntry,
                            onClick = if (selectedIsPhoto) onPhoto else onVideo,
                        )
                    }
                }
                MorphPanel(
                    visible = menuExpanded,
                    background = Color(0xFF202020),
                    alignment = Alignment.TopStart,
                    onDismiss = { menuExpanded = false },
                    modifier = Modifier
                        .padding(start = 22.dp, top = 58.dp)
                        .fillMaxWidth(0.70f),
                ) {
                    KeepixQueueMenu(
                        modifier = Modifier.fillMaxWidth(),
                        selectedIsPhoto = selectedIsPhoto,
                        mediaType = photoScope.mediaType,
                        photoScope = photoScope,
                        videoScope = videoScope,
                        settings = settings,
                        panel = menuPanel,
                        onToggle = { menuExpanded = false },
                        onPanel = { menuPanel = it },
                        onPhotoTab = {
                            onPhotoTab()
                            menuPanel = KeepixMenuPanel.ROOT
                        },
                        onVideoTab = {
                            onVideoTab()
                            menuExpanded = false
                            menuPanel = KeepixMenuPanel.ROOT
                        },
                        onPhotoType = {
                            onPhotoType(it)
                            menuExpanded = false
                        },
                        onPhotoDate = {
                            onPhotoDate(it)
                            menuExpanded = false
                        },
                        onPhotoSort = {
                            onPhotoSort(it)
                            menuExpanded = false
                        },
                        onPhotoBatch = {
                            onPhotoBatch(it)
                            menuExpanded = false
                        },
                        onVideoDate = {
                            onVideoDate(it)
                            menuExpanded = false
                        },
                        onVideoSort = {
                            onVideoSort(it)
                            menuExpanded = false
                        },
                        onVideoBatch = {
                            onVideoBatch(it)
                            menuExpanded = false
                        },
                    )
                }
            }
        }
    }
}

private enum class KeepixMenuPanel {
    ROOT,
    RANGE,
    SORT,
    BATCH,
}

private data class KeepixPreviewItem(
    val uri: String,
    val name: String,
    val video: Boolean,
    val aspectRatio: Float,
    val imageModel: ImageRequest? = null,
)

private fun keepixPhotoAspectRatio(width: Int, height: Int): Float =
    ((width.takeIf { it > 0 } ?: 1).toFloat() / (height.takeIf { it > 0 } ?: 1).toFloat()).coerceIn(0.42f, 2.20f)

@Composable
private fun KeepixQueueSelector(
    selectedIsPhoto: Boolean,
    mediaType: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.height(50.dp).morphAnchor("queue-selector", 22.dp, color = Color(0xFF2D2D2D),
            icon = if (selectedIsPhoto) Icons.Rounded.PhotoLibrary else Icons.Rounded.Movie),
        shape = RoundedCornerShape(22.dp),
        color = Color(0xFF2D2D2D).copy(alpha = 0.72f),
        contentColor = Color.White,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(19.dp), tint = Color.White.copy(alpha = 0.84f))
            Icon(if (selectedIsPhoto) Icons.Rounded.PhotoLibrary else Icons.Rounded.Movie, contentDescription = null, modifier = Modifier.size(22.dp), tint = Color.White)
            Text(
                if (selectedIsPhoto) photoTypeMenuLabel(mediaType) else stringResource(R.string.immersive_video_label),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
            )
        }
    }
}

@Composable
private fun KeepixQueueMenu(
    modifier: Modifier,
    selectedIsPhoto: Boolean,
    mediaType: String,
    photoScope: CleaningScope,
    videoScope: CleaningScope,
    settings: AppSettings,
    panel: KeepixMenuPanel,
    onToggle: () -> Unit,
    onPanel: (KeepixMenuPanel) -> Unit,
    onPhotoTab: () -> Unit,
    onVideoTab: () -> Unit,
    onPhotoType: (String) -> Unit,
    onPhotoDate: (String) -> Unit,
    onPhotoSort: (String) -> Unit,
    onPhotoBatch: (Int) -> Unit,
    onVideoDate: (String) -> Unit,
    onVideoSort: (String) -> Unit,
    onVideoBatch: (Int) -> Unit,
) {
    val activeBatch = if (selectedIsPhoto) settings.photoBatchSize else settings.videoBatchSize
    val activeDateMode = if (selectedIsPhoto) photoScope.dateMode else videoScope.dateMode
    val activeSortOrder = if (selectedIsPhoto) photoScope.sortOrder else videoScope.sortOrder
    val togglePanel: (KeepixMenuPanel) -> Unit = { target ->
        onPanel(if (panel == target) KeepixMenuPanel.ROOT else target)
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(32.dp),
        color = Color(0xFF363636).copy(alpha = 0.76f),
        contentColor = Color.White,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
        shadowElevation = 12.dp,
    ) {
        Column(Modifier.padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                modifier = Modifier
                    .height(50.dp)
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(19.dp), tint = Color.White.copy(alpha = 0.84f))
                Icon(if (selectedIsPhoto) Icons.Rounded.PhotoLibrary else Icons.Rounded.Movie, contentDescription = null, modifier = Modifier.size(22.dp), tint = Color.White)
                Text(
                    if (selectedIsPhoto) photoTypeMenuLabel(mediaType) else stringResource(R.string.immersive_video_label),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                )
            }
            Box(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp).height(1.dp).background(Color.White.copy(alpha = 0.10f)))
            KeepixMenuRow(stringResource(R.string.immersive_photo_label), selectedIsPhoto && photoScope.mediaType == "all", Icons.Rounded.PhotoLibrary) { onPhotoType("all") }
            KeepixMenuRow(stringResource(R.string.immersive_photo_screenshot), selectedIsPhoto && photoScope.mediaType == "screenshot", Icons.Rounded.Image) { onPhotoType("screenshot") }
            KeepixMenuRow(stringResource(R.string.immersive_photo_selfie), selectedIsPhoto && photoScope.mediaType == "selfie", Icons.Rounded.Image) { onPhotoType("selfie") }
            KeepixMenuRow(stringResource(R.string.immersive_photo_motion), selectedIsPhoto && photoScope.mediaType == "motion", Icons.Rounded.PlayCircle) { onPhotoType("motion") }
            KeepixMenuRow(stringResource(R.string.immersive_photo_gif), selectedIsPhoto && photoScope.mediaType == "gif", Icons.Rounded.Image) { onPhotoType("gif") }
            KeepixMenuRow(stringResource(R.string.immersive_video_label), !selectedIsPhoto, Icons.Rounded.Movie, onClick = onVideoTab)
            Box(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp).height(1.dp).background(Color.White.copy(alpha = 0.12f)))
            KeepixMenuRow(
                title = stringResource(R.string.immersive_scope_title),
                selected = false,
                icon = Icons.Rounded.CalendarToday,
                value = dateModeMenuLabel(activeDateMode),
                showChevron = true,
                expanded = panel == KeepixMenuPanel.RANGE,
                onClick = { togglePanel(KeepixMenuPanel.RANGE) },
            )
            AnimatedVisibility(visible = panel == KeepixMenuPanel.RANGE) {
                KeepixChoiceList(
                    options = listOf("all", "year", "month", "seven_days", "today_history").map { it to dateModeMenuLabel(it) },
                    selected = activeDateMode,
                    onSelect = { if (selectedIsPhoto) onPhotoDate(it) else onVideoDate(it) },
                )
            }
            KeepixMenuRow(
                title = stringResource(R.string.immersive_sort_title),
                selected = false,
                icon = Icons.Rounded.Share,
                value = if (activeSortOrder == "newest") {
                    stringResource(R.string.immersive_sort_newest)
                } else {
                    stringResource(R.string.immersive_sort_random)
                },
                showChevron = true,
                expanded = panel == KeepixMenuPanel.SORT,
                onClick = { togglePanel(KeepixMenuPanel.SORT) },
            )
            AnimatedVisibility(visible = panel == KeepixMenuPanel.SORT) {
                KeepixChoiceList(
                    options = listOf(
                        "random" to stringResource(R.string.immersive_sort_random),
                        "newest" to stringResource(R.string.immersive_sort_newest),
                    ),
                    selected = activeSortOrder,
                    onSelect = { if (selectedIsPhoto) onPhotoSort(it) else onVideoSort(it) },
                )
            }
            KeepixMenuRow(
                title = stringResource(R.string.immersive_batch_title),
                selected = false,
                icon = Icons.Rounded.Tune,
                value = activeBatch.toString(),
                showChevron = true,
                expanded = panel == KeepixMenuPanel.BATCH,
                onClick = { togglePanel(KeepixMenuPanel.BATCH) },
            )
            AnimatedVisibility(visible = panel == KeepixMenuPanel.BATCH) {
                KeepixBatchSlider(
                    value = activeBatch,
                    onSelect = { size -> if (selectedIsPhoto) onPhotoBatch(size) else onVideoBatch(size) },
                )
            }
        }
    }
}

@Composable
private fun KeepixBatchSlider(value: Int, onSelect: (Int) -> Unit) {
    var localValue by remember(value) { mutableStateOf(value.coerceIn(1, 100).toFloat()) }
    Column(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.immersive_batch_title), style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.62f))
            Text((localValue + 0.5f).toInt().coerceIn(1, 100).toString(), style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
        }
        Slider(
            value = localValue,
            onValueChange = { localValue = it },
            valueRange = 1f..100f,
            steps = 98,
            onValueChangeFinished = { onSelect((localValue + 0.5f).toInt().coerceIn(1, 100)) },
        )
    }
}

@Composable
private fun KeepixSubMenuHeader(title: String, onBack: () -> Unit) {
    KeepixMenuRow(
        title = title,
        selected = false,
        icon = Icons.Rounded.KeyboardArrowDown,
        value = stringResource(R.string.immersive_back),
        onClick = onBack,
    )
    Box(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp).height(1.dp).background(Color.White.copy(alpha = 0.12f)))
}

@Composable
private fun KeepixChoiceList(
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        options.forEach { (value, label) ->
            KeepixMenuRow(
                title = label,
                selected = value == selected,
                icon = Icons.Rounded.Tune,
                onClick = { onSelect(value) },
            )
        }
    }
}

@Composable
private fun KeepixMenuRow(
    title: String,
    selected: Boolean,
    icon: ImageVector,
    value: String? = null,
    showChevron: Boolean = false,
    expanded: Boolean = false,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        shape = RoundedCornerShape(18.dp),
        color = if (selected) Color.White.copy(alpha = 0.10f) else Color.Transparent,
        contentColor = Color.White,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(if (selected) "✓" else "", modifier = Modifier.width(16.dp), style = MaterialTheme.typography.titleMedium, color = Color.White)
            Icon(icon, contentDescription = null, modifier = Modifier.size(21.dp), tint = Color.White.copy(alpha = 0.88f))
            Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1)
            if (value != null) {
                Text(value, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.62f), maxLines = 1)
            }
            if (showChevron) {
                Text(if (expanded) "⌃" else "›", style = MaterialTheme.typography.titleLarge, color = Color.White.copy(alpha = 0.56f), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun photoTypeMenuLabel(type: String): String = when (type) {
    "screenshot" -> stringResource(R.string.immersive_photo_screenshot)
    "selfie" -> stringResource(R.string.immersive_photo_selfie)
    "motion" -> stringResource(R.string.immersive_photo_motion)
    "gif" -> stringResource(R.string.immersive_photo_gif)
    "raw" -> stringResource(R.string.immersive_photo_raw)
    "long" -> stringResource(R.string.immersive_photo_long)
    else -> stringResource(R.string.immersive_photo_label)
}

@Composable
private fun dateModeMenuLabel(mode: String): String = when (mode) {
    "seven_days" -> stringResource(R.string.organizer_date_recent_days, 7)
    "month" -> stringResource(R.string.immersive_date_recent_month)
    "year" -> stringResource(R.string.immersive_date_recent_year)
    "today_history" -> stringResource(R.string.organizer_date_today_history)
    else -> if (mode.startsWith("d:")) mode.removePrefix("d:").replace("-", "/") else stringResource(R.string.organizer_date_all)
}

private fun nextDateMode(mode: String): String = when (mode) {
    "all" -> "year"
    "year" -> "month"
    "month" -> "seven_days"
    else -> "all"
}

@Composable
private fun KeepixHomeCircle(icon: ImageVector, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(52.dp).morphAnchor("home-icon:" + icon.name, 26.dp, icon = icon),
        shape = CircleShape,
        color = Color.White.copy(alpha = 0.13f),
        contentColor = Color.White,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp), tint = Color.White.copy(alpha = 0.86f))
        }
    }
}

@Composable
private fun KeepixHomeDeck(
    items: List<KeepixPreviewItem>,
    preparing: Boolean,
    selectedIsPhoto: Boolean,
    canOpen: Boolean,
    animateEntry: Boolean,
    onClick: () -> Unit,
) {
    val deckClickSource = remember { MutableInteractionSource() }
    val density = LocalDensity.current
    val visibleItems = items.take(3)
    val visibleKey = visibleItems.joinToString("|") { it.uri }
    // Keep the source card's settled position across navigation so photo-screen
    // shared transitions can collapse back to the exact place they expanded from.
    var lastAnimatedKey by rememberSaveable { mutableStateOf<String?>(null) }
    val deckEntryStartPx = with(density) { 420.dp.toPx() }
    val deckEntryOffsets = remember(visibleKey, deckEntryStartPx) {
        val initialOffset = if (lastAnimatedKey == visibleKey) 0f else deckEntryStartPx
        List(3) { Animatable(initialOffset) }
    }
    LaunchedEffect(visibleKey, animateEntry) {
        if (visibleItems.isNotEmpty() && lastAnimatedKey != visibleKey) {
            lastAnimatedKey = visibleKey
            if (animateEntry) {
                deckEntryOffsets.forEachIndexed { index, offset ->
                    launch {
                        kotlinx.coroutines.delay(index * 75L)
                        offset.animateTo(
                            targetValue = 0f,
                            animationSpec = tween(540, easing = FastOutSlowInEasing),
                        )
                    }
                }
            } else {
                deckEntryOffsets.forEach { it.snapTo(0f) }
            }
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(650.dp)
            .clickable(
                interactionSource = deckClickSource,
                indication = null,
                onClick = {
                    if (canOpen) onClick()
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (visibleItems.isEmpty()) {
            Box(
                Modifier
                    .fillMaxWidth(0.88f)
                    .height(500.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (preparing) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(0.26f),
                        color = Color.White.copy(alpha = 0.58f),
                        trackColor = Color.White.copy(alpha = 0.12f),
                    )
                } else if (!canOpen) {
                    Text(
                        if (selectedIsPhoto) {
                            stringResource(R.string.immersive_no_pending_photos)
                        } else {
                            stringResource(R.string.immersive_no_pending_videos)
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White.copy(alpha = 0.62f),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        } else {
            val rotations = listOf(6f, -7f, 9f)
            val offsetsX = listOf(18.dp, -78.dp, 86.dp)
            val offsetsY = listOf(-8.dp, 76.dp, 118.dp)
            val fills = listOf(0.88f, 0.84f, 0.78f)
            visibleItems.reversed().forEachIndexed { reversedIndex, item ->
                val originalIndex = visibleItems.lastIndex - reversedIndex
                val isTop = originalIndex == 0
                val shape = RoundedCornerShape(22.dp)
                val entryStart = deckEntryStartPx + originalIndex * with(density) { 42.dp.toPx() }
                val entryOffset = deckEntryOffsets.getOrNull(originalIndex)?.value ?: 0f
                val entryProgress = (1f - entryOffset / entryStart.coerceAtLeast(1f)).coerceIn(0f, 1f)
                val context = LocalContext.current
                val cardImage = item.imageModel ?: ImageRequest.Builder(context)
                    .data(Uri.parse(item.uri)).memoryCacheKey(item.uri + "#home_deck")
                    .diskCacheKey(item.uri).placeholderMemoryCacheKey(item.uri + "#home_deck")
                    .size(900, 1200).crossfade(false).build()
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth(fills.getOrElse(originalIndex) { if (isTop) 0.86f else 0.90f })
                        .aspectRatio(item.aspectRatio)
                        .graphicsLayer {
                            rotationZ = rotations.getOrElse(originalIndex) { 0f }
                            translationX = offsetsX.getOrElse(originalIndex) { 0.dp }.toPx()
                            translationY = offsetsY.getOrElse(originalIndex) { 0.dp }.toPx() + entryOffset
                            alpha = entryProgress
                            val baseScale = if (isTop) 1f else 0.96f
                            val entryScale = 0.92f + entryProgress * 0.08f
                            scaleX = baseScale * entryScale
                            scaleY = baseScale * entryScale
                        }
                        .then(if (isTop) Modifier.morphAnchor("home-deck", 22.dp, image = cardImage, imageAspectRatio = item.aspectRatio) else Modifier)
                        .clip(shape),
                ) {
                    AsyncImage(
                        model = cardImage,
                        contentDescription = item.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                    Box(
                        Modifier.fillMaxSize().background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Transparent, Color.Black.copy(alpha = 0.28f))
                            )
                        )
                    )
                    if (item.video) {
                        Surface(
                            modifier = Modifier.align(Alignment.TopEnd).padding(14.dp),
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.46f),
                            contentColor = Color.White,
                        ) {
                            Icon(Icons.Rounded.PlayCircle, contentDescription = null, modifier = Modifier.padding(8.dp).size(24.dp))
                        }
                    }
                }
            }
        }
}
}



