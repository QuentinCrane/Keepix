package com.futureape.kanleme.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/** Only route static, unusually tall images to the region viewer. */
internal fun isLongImageCandidate(width: Int, height: Int, mimeType: String): Boolean =
    width > 0 && height > 0 &&
        max(width, height).toLong() > min(width, height).toLong() * 3L &&
        !mimeType.equals("image/gif", ignoreCase = true)

/**
 * Displays long images at readable width without allocating a full-resolution bitmap for the
 * entire image. Unsupported or unreadable images retain the caller's existing preview.
 */
@Composable
internal fun LongImageViewer(
    uri: Uri,
    description: String,
    modifier: Modifier = Modifier,
    mimeType: String = "",
    topInset: Dp = 60.dp,
    bottomInset: Dp = 110.dp,
    onHorizontalPanChanged: (Boolean) -> Unit = {},
    onLongPress: () -> Unit = {},
    fallback: @Composable () -> Unit,
) {
    val context = LocalContext.current
    var source by remember(uri, mimeType) { mutableStateOf<LongImageSource?>(null) }
    var failed by remember(uri, mimeType) { mutableStateOf(false) }

    if (failed) {
        LaunchedEffect(uri) { onHorizontalPanChanged(false) }
        fallback()
        return
    }

    LaunchedEffect(uri, mimeType) {
        var opened: LongImageSource? = null
        try {
            // Keep ownership even if the composable leaves while the provider is opening.
            withContext(Dispatchers.IO + NonCancellable) {
                opened = LongImageSource.open(context, uri, mimeType)
            }
            val image = requireNotNull(opened)
            if (image.displayHeight.toLong() <= image.displayWidth.toLong() * 3L) {
                failed = true
            } else {
                source = image
                awaitCancellation()
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: AnimatedLongImageException) {
            failed = true
        } catch (error: Exception) {
            Log.w("LongImageViewer", "Unable to open image for region decoding", error)
            failed = true
        } finally {
            withContext(Dispatchers.IO + NonCancellable) { opened?.close() }
        }
    }

    BoxWithConstraints(
        modifier = modifier.statusBarsPadding().navigationBarsPadding(),
    ) {
        // Reserve room for the preview controls, but keep a usable viewport in short windows.
        val safeTop = minOf(topInset, maxHeight * 0.20f)
        val safeBottom = minOf(bottomInset, maxHeight * 0.35f)
        val image = source
        if (image == null) {
            CircularProgressIndicator(Modifier.align(Alignment.Center), color = Color.White)
        } else {
            LongImageContent(
                source = image,
                description = description,
                modifier = Modifier.fillMaxSize().padding(top = safeTop, bottom = safeBottom),
                onHorizontalPanChanged = onHorizontalPanChanged,
                onLongPress = onLongPress,
                onDecodeFailed = { failed = true },
            )
        }
    }
}

@Composable
private fun LongImageContent(
    source: LongImageSource,
    description: String,
    modifier: Modifier,
    onHorizontalPanChanged: (Boolean) -> Unit,
    onLongPress: () -> Unit,
    onDecodeFailed: () -> Unit,
) {
    var viewport by remember(source) { mutableStateOf(IntSize.Zero) }
    var scale by remember(source) { mutableStateOf(0f) }
    var position by remember(source) { mutableStateOf(Offset.Zero) }
    var tile by remember(source) { mutableStateOf<DecodedRegion?>(null) }
    val currentPanCallback by rememberUpdatedState(onHorizontalPanChanged)
    val currentLongPress by rememberUpdatedState(onLongPress)
    val currentFailureCallback by rememberUpdatedState(onDecodeFailed)
    val paint = remember { Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG) }

    fun clampPosition(candidate: Offset, targetScale: Float): Offset {
        fun clampAxis(value: Float, screen: Int, image: Int): Float {
            val drawnSize = image * targetScale
            return if (drawnSize <= screen) (screen - drawnSize) / 2f
            else value.coerceIn(screen - drawnSize, 0f)
        }
        return Offset(
            clampAxis(candidate.x, viewport.width, source.displayWidth),
            clampAxis(candidate.y, viewport.height, source.displayHeight),
        )
    }

    LaunchedEffect(viewport) {
        if (viewport.width > 0 && viewport.height > 0) {
            scale = viewport.width.toFloat() / source.displayWidth
            position = clampPosition(Offset.Zero, scale)
        }
    }

    LaunchedEffect(source) {
        snapshotFlow {
            if (viewport.width > 0 && viewport.height > 0 && scale > 0f) {
                source.request(viewport, scale, position)
            } else null
        }.distinctUntilChanged().collectLatest { request ->
            if (request == null) return@collectLatest
            try {
                val decoded = withContext(Dispatchers.IO) { source.decode(request) }
                if (decoded == null) currentFailureCallback() else tile = decoded
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Log.w("LongImageViewer", "Unable to decode image region", error)
                currentFailureCallback()
            }
        }
    }

    LaunchedEffect(source, viewport, scale) {
        currentPanCallback(viewport.width > 0 && source.displayWidth * scale > viewport.width + 1f)
    }

    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(
            Modifier.fillMaxSize()
                .onSizeChanged { viewport = it }
                .semantics { contentDescription = description }
                .pointerInput(source, viewport) {
                    detectTapGestures(
                        onLongPress = { currentLongPress() },
                        onDoubleTap = { tap ->
                            if (viewport.width > 0 && scale > 0f) {
                                val base = viewport.width.toFloat() / source.displayWidth
                                val next = if (scale > base * 1.01f) base else base * 2.5f
                                position = clampPosition(tap - (tap - position) * (next / scale), next)
                                scale = next
                            }
                        },
                    )
                }
                .pointerInput(source, viewport) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        var totalPan = Offset.Zero
                        var handling = false
                        do {
                            val event = awaitPointerEvent()
                            val pan = event.calculatePan()
                            totalPan += pan
                            val horizontalOverflow = source.displayWidth * scale > viewport.width + 1f
                            if (event.changes.count { it.pressed } > 1 ||
                                (abs(totalPan.y) > viewConfiguration.touchSlop && abs(totalPan.y) > abs(totalPan.x)) ||
                                (horizontalOverflow && abs(totalPan.x) > viewConfiguration.touchSlop && abs(totalPan.x) > abs(totalPan.y))
                            ) handling = true
                            if (handling && viewport.width > 0 && scale > 0f) {
                                val base = viewport.width.toFloat() / source.displayWidth
                                val next = (scale * event.calculateZoom()).coerceIn(base, base * 4f)
                                val centroid = event.calculateCentroid()
                                if (centroid.isSpecified) {
                                    val previousCentroid = centroid - pan
                                    position = clampPosition(centroid - (previousCentroid - position) * (next / scale), next)
                                    scale = next
                                }
                                event.changes.forEach { it.consume() }
                            }
                        } while (event.changes.any { it.pressed })
                    }
                },
        ) {
            val region = tile
            if (region != null && scale > 0f) {
                drawIntoCanvas { canvas ->
                    val native = canvas.nativeCanvas
                    val save = native.save()
                    native.clipRect(0f, 0f, size.width, size.height)
                    native.translate(position.x, position.y)
                    native.scale(scale, scale)
                    native.concat(source.orientationMatrix)
                    native.drawBitmap(region.bitmap, null, RectF(region.bounds), paint)
                    native.restoreToCount(save)
                }
            }
        }
        if (tile == null) CircularProgressIndicator(color = Color.White)
    }
}

private data class RegionRequest(val bounds: Rect, val sampleSize: Int)
private data class DecodedRegion(val bounds: Rect, val bitmap: Bitmap)
private class AnimatedLongImageException : Exception()

/** Serializes decode and close: a cancelled UI request may still be decoding on an IO thread. */
private class LongImageSource private constructor(
    private val decoder: BitmapRegionDecoder,
    orientation: Int,
) {
    private val lock = Any()
    private var closed = false
    private val rawWidth = decoder.width
    private val rawHeight = decoder.height
    private val swapsDimensions = orientation in 5..8
    val displayWidth = if (swapsDimensions) rawHeight else rawWidth
    val displayHeight = if (swapsDimensions) rawWidth else rawHeight
    val orientationMatrix: Matrix = orientationMatrix(orientation, rawWidth, rawHeight)
    private val inverseOrientation = Matrix().also { check(orientationMatrix.invert(it)) }

    fun request(viewport: IntSize, scale: Float, position: Offset): RegionRequest {
        val visibleLeft = (-position.x / scale).coerceIn(0f, displayWidth.toFloat())
        val visibleTop = (-position.y / scale).coerceIn(0f, displayHeight.toFloat())
        val visibleRight = ((viewport.width - position.x) / scale).coerceIn(0f, displayWidth.toFloat())
        val visibleBottom = ((viewport.height - position.y) / scale).coerceIn(0f, displayHeight.toFloat())
        val stepX = max(1f, viewport.width / scale / 3f)
        val stepY = max(1f, viewport.height / scale / 3f)
        val orientedBounds = RectF(
            max(0f, (floor(visibleLeft / stepX) - 1f) * stepX),
            max(0f, (floor(visibleTop / stepY) - 1f) * stepY),
            min(displayWidth.toFloat(), (ceil(visibleRight / stepX) + 1f) * stepX),
            min(displayHeight.toFloat(), (ceil(visibleBottom / stepY) + 1f) * stepY),
        )
        inverseOrientation.mapRect(orientedBounds)
        val bounds = Rect(
            floor(orientedBounds.left).toInt().coerceIn(0, rawWidth - 1),
            floor(orientedBounds.top).toInt().coerceIn(0, rawHeight - 1),
            ceil(orientedBounds.right).toInt().coerceIn(1, rawWidth),
            ceil(orientedBounds.bottom).toInt().coerceIn(1, rawHeight),
        )
        var sample = 1
        while (sample <= Int.MAX_VALUE / 2 && sample * 2f <= 1f / scale) sample *= 2
        // Bound each bitmap even for very large screens and leave room for its replacement.
        while ((bounds.width().toLong() / sample + 1) * (bounds.height().toLong() / sample + 1) > 6_000_000L) {
            sample *= 2
        }
        return RegionRequest(bounds, sample)
    }

    fun decode(request: RegionRequest): DecodedRegion? = synchronized(lock) {
        if (closed) return@synchronized null
        val options = BitmapFactory.Options().apply { inSampleSize = request.sampleSize }
        decoder.decodeRegion(request.bounds, options)?.let { DecodedRegion(request.bounds, it) }
    }

    fun close() = synchronized(lock) {
        if (!closed) {
            closed = true
            decoder.recycle()
        }
    }

    companion object {
        fun open(context: Context, uri: Uri, mimeType: String): LongImageSource {
            val resolver = context.contentResolver
            if (mimeType.lowercase() in setOf("image/webp", "image/heic", "image/heif", "image/avif")) {
                // A region decoder displays only one frame. Keep the existing animated viewer
                // for animated containers, while allowing static WebP/HEIF screenshots through.
                var animated = false
                try {
                    ImageDecoder.decodeDrawable(ImageDecoder.createSource(resolver, uri)) { decoder, info, _ ->
                        animated = info.isAnimated
                        if (animated) throw AnimatedLongImageException()
                        decoder.setTargetSize(1, 1)
                    }
                } catch (_: Exception) {
                    // Region decoding below still decides whether an otherwise readable image works.
                }
                if (animated) throw AnimatedLongImageException()
            }
            val orientation = try {
                resolver.openInputStream(uri)?.use {
                    ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                } ?: ExifInterface.ORIENTATION_NORMAL
            } catch (_: Exception) {
                ExifInterface.ORIENTATION_NORMAL
            }
            val decoder = requireNotNull(resolver.openInputStream(uri)).use { input ->
                if (Build.VERSION.SDK_INT >= 31) BitmapRegionDecoder.newInstance(input)
                else @Suppress("DEPRECATION") BitmapRegionDecoder.newInstance(input, false)
            }
            return LongImageSource(requireNotNull(decoder), orientation)
        }
    }
}

private fun orientationMatrix(orientation: Int, width: Int, height: Int): Matrix {
    val w = width.toFloat()
    val h = height.toFloat()
    val values = when (orientation) {
        2 -> floatArrayOf(-1f, 0f, w, 0f, 1f, 0f, 0f, 0f, 1f)
        3 -> floatArrayOf(-1f, 0f, w, 0f, -1f, h, 0f, 0f, 1f)
        4 -> floatArrayOf(1f, 0f, 0f, 0f, -1f, h, 0f, 0f, 1f)
        5 -> floatArrayOf(0f, 1f, 0f, 1f, 0f, 0f, 0f, 0f, 1f)
        6 -> floatArrayOf(0f, -1f, h, 1f, 0f, 0f, 0f, 0f, 1f)
        7 -> floatArrayOf(0f, -1f, h, -1f, 0f, w, 0f, 0f, 1f)
        8 -> floatArrayOf(0f, 1f, 0f, -1f, 0f, w, 0f, 0f, 1f)
        else -> floatArrayOf(1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f)
    }
    return Matrix().apply { setValues(values) }
}
