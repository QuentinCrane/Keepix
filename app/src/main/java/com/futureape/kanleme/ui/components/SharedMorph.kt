package com.futureape.kanleme.ui.components

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.ui.focus.focusProperties
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.ui.Alignment
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.collect
import kotlin.math.atan2
import kotlin.math.roundToInt
import kotlin.math.abs

/** One reversible clock drives geometry, material and content; text is never scaled. */
object MorphMotion {
    const val DurationMillis = 420
    val Easing = CubicBezierEasing(0.22f, 0f, 0.08f, 1f)
    fun duration(from: Float, to: Float) = if (from == to) 0 else
        (DurationMillis * abs(to - from)).roundToInt().coerceIn(80, DurationMillis)
    fun contentAlpha(progress: Float) = ((progress - 0.38f) / 0.50f).coerceIn(0f, 1f)
    fun sourceAlpha(progress: Float) = (1f - progress / 0.42f).coerceIn(0f, 1f)
    fun bounds(source: Rect, target: Rect, progress: Float): Rect {
        val p = progress.coerceIn(0f, 1f)
        fun between(a: Float, b: Float) = a + (b - a) * p
        return Rect(between(source.left, target.left), between(source.top, target.top),
            between(source.right, target.right), between(source.bottom, target.bottom))
    }
}

data class MorphOrigin(
    val id: String,
    val owner: String,
    val bounds: Rect,
    val cornerRadius: Dp = 28.dp,
    val color: Color = Color(0xFF171717),
    val image: Any? = null,
    val icon: ImageVector? = null,
    val tint: Color = Color.White,
    val rotation: Float = 0f,
    val imageAspectRatio: Float? = null,
)

@Stable
class MorphRegistry {
    val anchors = mutableStateMapOf<String, MorphOrigin>()
    var pending: MorphOrigin? = null
    var hiddenId by mutableStateOf<String?>(null)
    var modalDepth by mutableStateOf(0)
    private val entryOrigins = mutableMapOf<String, MorphOrigin?>()
    private val stack = mutableListOf<String>()
    var movingEntryId: String? = null
        private set
    private var currentEntryId: String? = null

    // Called once per navigation change, before composing either destination.
    fun navigateTo(entryId: String): MorphOrigin? {
        if (entryId != currentEntryId) {
            val existingIndex = stack.indexOf(entryId)
            if (existingIndex >= 0) {
                movingEntryId = currentEntryId
                while (stack.size > existingIndex + 1) stack.removeAt(stack.lastIndex)
            } else {
                entryOrigins[entryId] = pending?.takeIf { it.owner == currentEntryId } ?: entryOrigins[entryId]
                stack += entryId
                movingEntryId = entryId
            }
            currentEntryId = entryId
            pending = null
        }
        return movingEntryId?.let(entryOrigins::get)
    }

    fun originFor(entryId: String) = entryOrigins[entryId]
    fun releaseDisposed(entryId: String) {
        if (entryId !in stack) entryOrigins.remove(entryId)
    }
}

val LocalMorphRegistry = staticCompositionLocalOf<MorphRegistry?> { null }
val LocalMorphOwner = staticCompositionLocalOf { "root" }
val LocalMorphAnimationsEnabled = staticCompositionLocalOf { true }

/** Register the actual object, without consuming its click or changing its layout. */
fun Modifier.morphAnchor(
    id: String,
    cornerRadius: Dp = 28.dp,
    color: Color = Color(0xFF171717),
    image: Any? = null,
    icon: ImageVector? = null,
    tint: Color = Color.White,
    imageAspectRatio: Float? = null,
): Modifier = composed {
    val registry = LocalMorphRegistry.current ?: return@composed this
    val owner = LocalMorphOwner.current
    val key = "$owner/$id"
    val latestVisual by rememberUpdatedState(MorphOrigin(key, owner, Rect.Zero, cornerRadius, color, image, icon, tint,
        imageAspectRatio = imageAspectRatio))
    DisposableEffect(key) {
        onDispose { registry.anchors.remove(key) }
    }
    this
        .onGloballyPositioned { coordinates ->
            val topLeft = coordinates.localToRoot(Offset.Zero)
            val topRight = coordinates.localToRoot(Offset(coordinates.size.width.toFloat(), 0f))
            val bottomLeft = coordinates.localToRoot(Offset(0f, coordinates.size.height.toFloat()))
            val center = coordinates.localToRoot(Offset(coordinates.size.width / 2f, coordinates.size.height / 2f))
            val halfSize = Offset((topRight - topLeft).getDistance() / 2f, (bottomLeft - topLeft).getDistance() / 2f)
            val angle = atan2(topRight.y - topLeft.y, topRight.x - topLeft.x) * 180f / Math.PI.toFloat()
            val origin = latestVisual.copy(bounds = Rect(center - halfSize, center + halfSize), rotation = angle)
            if (registry.anchors[key] != origin) registry.anchors[key] = origin
        }
        .pointerInput(key) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    if (event.changes.any { it.pressed && !it.previousPressed }) {
                        registry.pending = registry.anchors[key]
                    }
                }
            }
        }
        .graphicsLayer { alpha = if (registry.hiddenId == key) 0f else 1f }
}

/** Navigation keeps its own back stack and saved scroll state; only its surface morphs. */
@Composable
fun MorphDestination(
    entryId: String,
    scope: AnimatedVisibilityScope,
    background: Color,
    gestureProgress: Float? = null,
    imageDestination: Boolean = false,
    content: @Composable () -> Unit,
) {
    val registry = LocalMorphRegistry.current
    val animationsEnabled = LocalMorphAnimationsEnabled.current
    val moving = registry?.movingEntryId == entryId
    val origin = registry?.originFor(entryId)
    DisposableEffect(entryId) { onDispose { registry?.releaseDisposed(entryId) } }
    val progress = scope.transition.animateFloat(
        transitionSpec = { tween(if (animationsEnabled) MorphMotion.DurationMillis else 0, easing = MorphMotion.Easing) },
        label = "destination_morph",
    ) { state -> if (!moving || state == EnterExitState.Visible) 1f else 0f }
    val gesture = remember { Animatable(1f) }
    var gestureDriven by remember { mutableStateOf(false) }
    LaunchedEffect(gestureProgress, scope.transition.targetState) {
        if (gestureProgress != null) {
            gestureDriven = true
            gesture.snapTo(gestureProgress.coerceIn(0f, 1f))
        } else if (gestureDriven) {
            gesture.animateTo(if (scope.transition.targetState == EnterExitState.Visible) 1f else 0f,
                tween(MorphMotion.DurationMillis, easing = MorphMotion.Easing))
            gestureDriven = false
        }
    }
    val animating by remember(moving, scope) { derivedStateOf { moving && scope.transition.currentState != scope.transition.targetState } }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(animating, moving, origin?.id) {
        if (moving) registry?.hiddenId = if (animating) origin?.id else null
        if (moving && animating) { keyboard?.hide(); focusManager.clearFocus() }
    }
    CompositionLocalProvider(LocalMorphOwner provides entryId) {
        MorphFrame(
            origin = if (scope.transition.targetState == EnterExitState.PostExit) origin?.let { registry?.anchors?.get(it.id) } else origin,
            progress = { if (gestureDriven) gesture.value else if (moving) progress.value else 1f },
            background = background,
            imageDestination = imageDestination,
            content = content,
        )
    }
}

/** Retains content until collapse finishes, and reverses from its current geometry. */
@Composable
fun MorphOverlay(
    visible: Boolean,
    modifier: Modifier = Modifier,
    background: Color = Color.Black,
    source: MorphOrigin? = null,
    gestureProgress: Float? = null,
    targetBounds: Rect? = null,
    targetCornerRadius: Dp = 0.dp,
    scrim: Boolean = false,
    imageDestination: Boolean = false,
    onDismiss: () -> Unit,
    onClosed: () -> Unit = {},
    content: @Composable () -> Unit,
) {
    val registry = LocalMorphRegistry.current
    val animationsEnabled = LocalMorphAnimationsEnabled.current
    val progress = remember { Animatable(0f) }
    var origin by remember { mutableStateOf<MorphOrigin?>(null) }
    var retained by remember { mutableStateOf(false) }
    val latestClosed by rememberUpdatedState(onClosed)
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    var backProgress by remember { mutableStateOf<Float?>(null) }
    val effectiveGestureProgress = gestureProgress ?: backProgress?.let { 1f - it }
    LaunchedEffect(visible, effectiveGestureProgress, animationsEnabled) {
        if (visible && !retained) {
            origin = source ?: registry?.pending
            retained = true
            registry?.let { it.modalDepth += 1 }
            registry?.hiddenId = origin?.id
        }
        if (!visible) {
            keyboard?.hide()
            focusManager.clearFocus()
        }
        if (!animationsEnabled) {
            progress.snapTo(if (visible) 1f else 0f)
        } else if (effectiveGestureProgress != null && visible) {
            progress.snapTo(effectiveGestureProgress.coerceIn(0f, 1f))
        } else {
            val target = if (visible) 1f else 0f
            progress.animateTo(target,
                tween(MorphMotion.duration(progress.value, target), easing = MorphMotion.Easing))
        }
        if (!visible && progress.value == 0f && retained) {
            retained = false
            registry?.let { it.modalDepth = (it.modalDepth - 1).coerceAtLeast(0) }
            if (registry?.hiddenId == origin?.id) registry?.hiddenId = null
            latestClosed()
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            if (registry?.hiddenId == origin?.id) registry?.hiddenId = null
            if (retained) registry?.let { it.modalDepth = (it.modalDepth - 1).coerceAtLeast(0) }
        }
    }
    PredictiveBackHandler(enabled = retained || visible) { events ->
        keyboard?.hide()
        focusManager.clearFocus()
        try {
            events.collect { backProgress = it.progress }
            backProgress = null
            onDismiss()
        } catch (_: CancellationException) {
            backProgress = null
        }
    }
    if (retained || visible) {
        // A missing/offscreen source uses a small centered collapse instead of a stale position.
        val attached = origin?.let { registry?.anchors?.get(it.id) }
        MorphFrame(
            origin = if (!visible && registry != null) attached else origin,
            progress = { progress.value },
            background = background,
            modifier = modifier.zIndex(30f),
            targetBounds = targetBounds,
            targetCornerRadius = targetCornerRadius,
            scrim = scrim,
            imageDestination = imageDestination,
            content = content,
        )
    }
}

/** Inline modal: its measured panel is the destination, rather than a separate window. */
@Composable
fun MorphPanel(
    visible: Boolean,
    background: Color,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.Center,
    onClosed: () -> Unit = {},
    content: @Composable BoxScope.() -> Unit,
) {
    var target by remember { mutableStateOf<Rect?>(null) }
    MorphOverlay(visible = visible, background = background, onDismiss = onDismiss, onClosed = onClosed,
        targetBounds = target, targetCornerRadius = 28.dp, scrim = true) {
        Box(Modifier.fillMaxSize().clickable(onClick = onDismiss)) {
            Box(modifier.align(alignment)
                .onGloballyPositioned { val next = it.boundsInRoot(); if (target != next) target = next }
                .clickable(onClick = {}), content = content)
        }
    }
}

@Composable
private fun MorphFrame(
    origin: MorphOrigin?,
    progress: () -> Float,
    background: Color,
    modifier: Modifier = Modifier,
    targetBounds: Rect? = null,
    targetCornerRadius: Dp = 0.dp,
    scrim: Boolean = false,
    imageDestination: Boolean = false,
    content: @Composable () -> Unit,
) {
    var hostBounds by remember { mutableStateOf(Rect.Zero) }
    val ready by remember(progress) { derivedStateOf { progress() >= 0.995f } }
    fun sourceBounds(target: Rect): Rect {
        val source = origin?.bounds?.translate(-hostBounds.topLeft)
        return source?.takeIf { it.width > 0 && it.height > 0 && it.overlaps(target) }
            ?: Rect(target.center - Offset(target.width * 0.44f, target.height * 0.44f),
                target.center + Offset(target.width * 0.44f, target.height * 0.44f))
    }
    Box(
        modifier.fillMaxSize()
            .onGloballyPositioned { val next = it.boundsInRoot(); if (hostBounds != next) hostBounds = next }
            .drawWithContent {
                val p = progress().coerceIn(0f, 1f)
                val target = targetBounds?.translate(-hostBounds.topLeft) ?: Rect(Offset.Zero, size)
                val rect = MorphMotion.bounds(sourceBounds(target), target, p)
                val radius = (origin?.cornerRadius ?: 28.dp).toPx() * (1f - p) + targetCornerRadius.toPx() * p
                val path = Path().apply { addRoundRect(RoundRect(rect, CornerRadius(radius))) }
                val angle = (origin?.rotation ?: 0f) * (1f - p)
                if (angle != 0f) path.transform(Matrix().apply {
                    translate(rect.center.x, rect.center.y)
                    rotateZ(angle)
                    translate(-rect.center.x, -rect.center.y)
                })
                if (scrim) drawRect(Color.Black.copy(alpha = 0.34f * p))
                rotate(angle, rect.center) {
                    drawRoundRect(lerp(origin?.color ?: background, background, p), rect.topLeft,
                        rect.size, CornerRadius(radius))
                }
                clipPath(path) { this@drawWithContent.drawContent() }
            }
            .pointerInput(ready) {
                if (!ready) awaitPointerEventScope {
                    while (true) awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
                }
            },
    ) {
        // Always measure text/forms at their final constraints. Only the enclosing surface changes.
        Box(Modifier.fillMaxSize()
            .focusProperties { canFocus = ready }.focusGroup()
            .graphicsLayer { alpha = if (imageDestination && origin?.image != null)
                ((progress() - 0.84f) / 0.16f).coerceIn(0f, 1f) else MorphMotion.contentAlpha(progress()) }
            .then(if (!ready) Modifier.clearAndSetSemantics {} else Modifier)) { content() }
        if (origin?.image != null || origin?.icon != null) {
            Layout(
                content = {
                    if (origin.image != null) {
                        AsyncImage(model = origin.image, contentDescription = null, contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize())
                    } else if (origin.icon != null) {
                        Icon(origin.icon, contentDescription = null, tint = origin.tint, modifier = Modifier.size(30.dp))
                    }
                },
                modifier = Modifier.fillMaxSize().graphicsLayer { alpha = if (imageDestination)
                    (1f - (progress() - 0.84f) / 0.16f).coerceIn(0f, 1f) else MorphMotion.sourceAlpha(progress()) },
            ) { measurables, constraints ->
                val target = targetBounds?.translate(-hostBounds.topLeft)
                    ?: Rect(0f, 0f, constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat())
                val ratio = origin.imageAspectRatio ?: sourceBounds(target).let { it.width / it.height.coerceAtLeast(1f) }
                val fitWidth = minOf(target.width, target.height * ratio)
                val fitHeight = fitWidth / ratio.coerceAtLeast(0.01f)
                val imageTarget = if (imageDestination) Rect(target.center - Offset(fitWidth / 2f, fitHeight / 2f),
                    target.center + Offset(fitWidth / 2f, fitHeight / 2f)) else target
                val rect = MorphMotion.bounds(sourceBounds(target), imageTarget, progress())
                val image = origin.image != null
                val width = if (image) rect.width.roundToInt().coerceAtLeast(1) else 30.dp.roundToPx()
                val height = if (image) rect.height.roundToInt().coerceAtLeast(1) else 30.dp.roundToPx()
                val child = measurables.single().measure(Constraints.fixed(width, height))
                layout(constraints.maxWidth, constraints.maxHeight) {
                    child.placeWithLayer((rect.center.x - width / 2f).roundToInt(), (rect.center.y - height / 2f).roundToInt()) {
                        rotationZ = origin.rotation * (1f - progress())
                    }
                }
            }
        }
    }
}
