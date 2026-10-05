package com.futureape.kanleme.ui.util

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.max

/** A dead zone, latched ownership and release decision shared by opening and closing. */
internal class PinchMotion(private val initialSpan: Float, touchSlop: Float, private val direction: Float) {
    private val deadZone = max(touchSlop * 0.75f, initialSpan * 0.035f)
    private val travel = max(initialSpan * 0.24f, touchSlop * 6f)
    var owned = false
        private set
    var progress = 0f
        private set
    private var velocity = 0f
    private var lastTime: Long? = null

    fun update(span: Float, timeMillis: Long): Float {
        val distance = (span - initialSpan) * direction
        if (distance > deadZone) owned = true
        val next = ((distance - deadZone) / travel).coerceIn(0f, 1f)
        lastTime?.let { time ->
            val elapsed = (timeMillis - time).coerceAtLeast(1)
            velocity = if (elapsed > 100) 0f else velocity * 0.65f + (next - progress) * 1000f / elapsed * 0.35f
        }
        lastTime = timeMillis
        progress = next
        return progress
    }

    fun shouldCommit() = owned && (progress >= 0.40f || (progress >= 0.12f && velocity > 1.25f))
}

internal fun Modifier.pinchMorph(
    key: Any,
    enabled: Boolean = true,
    opening: Boolean,
    onProgress: (Float, Float) -> Unit,
    onFinish: (Boolean) -> Unit,
    onAlternativeScale: ((Float) -> Unit)? = null,
): Modifier = if (!enabled) this else pointerInput(key) {
    awaitEachGesture {
        var motion: PinchMotion? = null
        var pair: List<androidx.compose.ui.input.pointer.PointerId>? = null
        var initialSpan = 1f
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val pressed = event.changes.filter { it.pressed }
            val lockedPair = pair
            if (lockedPair != null && lockedPair.any { id -> pressed.none { it.id == id } }) break
            if (pressed.size < 2) {
                if (pressed.isEmpty()) break
                continue
            }
            val trackedPair = pair ?: pressed.take(2).map { it.id }.also { pair = it }
            val fingers = trackedPair.mapNotNull { id -> pressed.firstOrNull { it.id == id } }
            if (fingers.size != 2) break
            val span = (fingers[0].position - fingers[1].position).getDistance().coerceAtLeast(1f)
            if (motion == null) {
                initialSpan = span
                motion = PinchMotion(span, viewConfiguration.touchSlop, if (opening) -1f else 1f)
            }
            val progress = motion.update(span, event.changes.maxOf { it.uptimeMillis })
            if (!opening && span < initialSpan - viewConfiguration.touchSlop && onAlternativeScale != null) {
                event.changes.forEach { it.consume() }
                onAlternativeScale(span / initialSpan)
            }
            if (motion.owned) {
                event.changes.forEach { it.consume() }
                onProgress(progress, span / initialSpan)
            }
        }
        motion?.takeIf { it.owned }?.let { onFinish(it.shouldCommit()) }
    }
}
