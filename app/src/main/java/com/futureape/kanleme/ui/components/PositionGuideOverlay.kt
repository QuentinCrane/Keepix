package com.futureape.kanleme.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.max

@Immutable
data class GuideSpot(
    val title: String,
    val body: String,
    val targetAlignment: Alignment,
    val cardAlignment: Alignment,
    val targetWidth: Dp,
    val targetHeight: Dp,
    val targetPadding: PaddingValues = PaddingValues(0.dp),
)

@Composable
fun PositionGuideOverlay(
    steps: List<GuideSpot>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    scrimColor: Color = Color.Black.copy(alpha = 0.30f),
    highlightColor: Color = Color.Unspecified,
) {
    if (steps.isEmpty()) return
    val resolvedHighlightColor = if (highlightColor == Color.Unspecified) MaterialTheme.colorScheme.primary else highlightColor
    var stepIndex by remember { mutableIntStateOf(0) }
    val step = steps[stepIndex.coerceIn(0, steps.lastIndex)]
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current

    BoxWithConstraints(modifier.fillMaxSize()) {
        val targetSize = with(density) {
            IntSize(
                width = step.targetWidth.toPx().toInt().coerceAtLeast(1),
                height = step.targetHeight.toPx().toInt().coerceAtLeast(1),
            )
        }
        val containerSize = with(density) {
            IntSize(
                width = maxWidth.toPx().toInt().coerceAtLeast(targetSize.width),
                height = maxHeight.toPx().toInt().coerceAtLeast(targetSize.height),
            )
        }
        val base = step.targetAlignment.align(targetSize, containerSize, layoutDirection)
        val paddingOffset = with(density) {
            val start = step.targetPadding.calculateLeftPadding(layoutDirection).toPx()
            val end = step.targetPadding.calculateRightPadding(layoutDirection).toPx()
            val top = step.targetPadding.calculateTopPadding().toPx()
            val bottom = step.targetPadding.calculateBottomPadding().toPx()
            val dx = when (step.targetAlignment) {
                Alignment.TopStart, Alignment.CenterStart, Alignment.BottomStart -> start
                Alignment.TopEnd, Alignment.CenterEnd, Alignment.BottomEnd -> -end
                else -> (start - end) / 2f
            }
            val dy = when (step.targetAlignment) {
                Alignment.TopStart, Alignment.TopCenter, Alignment.TopEnd -> top
                Alignment.BottomStart, Alignment.BottomCenter, Alignment.BottomEnd -> -bottom
                else -> (top - bottom) / 2f
            }
            dx to dy
        }
        val leftPx = (base.x + paddingOffset.first).coerceIn(0f, (containerSize.width - targetSize.width).toFloat())
        val topPx = (base.y + paddingOffset.second).coerceIn(0f, (containerSize.height - targetSize.height).toFloat())
        val rightPx = leftPx + targetSize.width
        val bottomPx = topPx + targetSize.height

        Canvas(Modifier.fillMaxSize()) {
            // Draw four dim regions and leave the target window truly transparent.
            drawRect(scrimColor, topLeft = Offset.Zero, size = Size(size.width, topPx))
            drawRect(scrimColor, topLeft = Offset(0f, bottomPx), size = Size(size.width, max(0f, size.height - bottomPx)))
            drawRect(scrimColor, topLeft = Offset(0f, topPx), size = Size(leftPx, targetSize.height.toFloat()))
            drawRect(scrimColor, topLeft = Offset(rightPx, topPx), size = Size(max(0f, size.width - rightPx), targetSize.height.toFloat()))
        }

        Box(
            modifier = Modifier
                .align(step.targetAlignment)
                .padding(step.targetPadding)
                .size(width = step.targetWidth, height = step.targetHeight)
                .border(BorderStroke(2.dp, resolvedHighlightColor.copy(alpha = 0.95f)), RoundedCornerShape(28.dp))
                .background(resolvedHighlightColor.copy(alpha = 0.08f), RoundedCornerShape(28.dp)),
        )

        Surface(
            modifier = Modifier
                .align(step.cardAlignment)
                .padding(horizontal = 22.dp, vertical = 40.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
            contentColor = MaterialTheme.colorScheme.onSurface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "步骤 " + (stepIndex + 1) + "/" + steps.size,
                    style = MaterialTheme.typography.labelLarge,
                    color = resolvedHighlightColor,
                )
                Text(step.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(step.body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    if (stepIndex > 0) {
                        Button(
                            onClick = { stepIndex -= 1 },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(999.dp),
                        ) { Text("上一步") }
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
