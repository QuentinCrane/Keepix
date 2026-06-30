package com.futureape.kanleme.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.futureape.kanleme.ui.i18n.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp

@Composable
fun LiquidBackground(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val oledDark = scheme.background.luminance() < 0.03f
    CompositionLocalProvider(LocalContentColor provides scheme.onBackground) {
        if (oledDark) {
            Box(modifier = modifier.background(Color.Black)) {
                content()
            }
            return@CompositionLocalProvider
        }
        Box(modifier = modifier.background(scheme.background)) {
            content()
        }
    }
}

@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(28.dp),
    tonalAlpha: Float = 0.70f,
    content: @Composable () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val oledDark = scheme.background.luminance() < 0.03f
    val containerColor = if (oledDark) {
        Color(0xFF090909).copy(alpha = 0.94f)
    } else {
        scheme.surface.copy(alpha = tonalAlpha.coerceIn(0.58f, 0.96f))
    }
    val borderColor = if (oledDark) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.82f)
    val outlineColor = if (oledDark) Color.White.copy(alpha = 0.08f) else scheme.outline.copy(alpha = 0.20f)
    Surface(
        modifier = modifier
            .shadow(if (oledDark) 0.dp else 14.dp, shape, clip = false, ambientColor = Color.Black.copy(alpha = if (oledDark) 0.00f else 0.045f), spotColor = Color.Black.copy(alpha = if (oledDark) 0.00f else 0.035f))
            .clip(shape)
            .background(containerColor, shape)
            .border(BorderStroke(1.dp, borderColor), shape)
            .border(BorderStroke(0.5.dp, outlineColor), shape),
        color = Color.Transparent,
        contentColor = scheme.onSurface,
        shape = shape,
    ) { content() }
}

@Composable
fun MetricPill(label: String, value: String, modifier: Modifier = Modifier) {
    GlassSurface(modifier = modifier, shape = RoundedCornerShape(22.dp), tonalAlpha = 0.62f) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(value, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun FloatingGlassNav(
    selectedIndex: Int,
    items: List<Pair<String, ImageVector>>,
    onSelected: (Int) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .shadow(24.dp, RoundedCornerShape(999.dp), ambientColor = Color.Black.copy(alpha = 0.24f), spotColor = Color.Black.copy(alpha = 0.30f))
            .clip(RoundedCornerShape(999.dp))
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.11f)), RoundedCornerShape(999.dp)),
        shape = RoundedCornerShape(999.dp),
        color = Color(0xFF171717).copy(alpha = 0.78f),
        contentColor = Color.White,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items.forEachIndexed { index, item ->
                NavBubble(
                    selected = index == selectedIndex,
                    label = item.first,
                    icon = item.second,
                    onClick = { onSelected(index) },
                )
            }
        }
    }
}


@Composable
fun FloatingGlassRail(
    selectedIndex: Int,
    items: List<Pair<String, ImageVector>>,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .width(72.dp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(36.dp))
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)), RoundedCornerShape(36.dp)),
        shape = RoundedCornerShape(36.dp),
        color = Color(0xFF141414).copy(alpha = 0.82f),
        contentColor = Color.White,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            items.forEachIndexed { index, item ->
                RailBubble(
                    selected = index == selectedIndex,
                    label = item.first,
                    icon = item.second,
                    onClick = { onSelected(index) },
                )
                if (index != items.lastIndex) Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun RailBubble(selected: Boolean, label: String, icon: ImageVector, onClick: () -> Unit) {
    val selectedColor by animateColorAsState(
        if (selected) Color.White.copy(alpha = 0.14f) else Color.Transparent,
        label = "rail_selected_color",
    )
    val content = if (selected) Color(0xFFAFC5FF) else Color.White.copy(alpha = 0.72f)
    Surface(
        modifier = Modifier
            .width(56.dp)
            .height(if (selected) 82.dp else 56.dp)
            .clip(RoundedCornerShape(28.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        shape = RoundedCornerShape(28.dp),
        color = selectedColor,
        contentColor = content,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(23.dp))
            androidx.compose.animation.AnimatedVisibility(selected) {
                Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
            }
        }
    }
}

@Composable
private fun RowScope.NavBubble(selected: Boolean, label: String, icon: ImageVector, onClick: () -> Unit) {
    val width by animateDpAsState(if (selected) 118.dp else 58.dp, label = "nav_width")
    val selectedColor by animateColorAsState(
        if (selected) Color.White.copy(alpha = 0.18f) else Color.Transparent,
        label = "nav_selected_color",
    )
    val content = if (selected) Color(0xFF8FA8FF) else Color.White.copy(alpha = 0.68f)
    Surface(
        modifier = Modifier
            .height(50.dp)
            .width(width)
            .clip(RoundedCornerShape(999.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        shape = RoundedCornerShape(999.dp),
        color = selectedColor,
        contentColor = content,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(if (selected) 23.dp else 22.dp))
            androidx.compose.animation.AnimatedVisibility(selected) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.width(8.dp))
                    Text(label, style = MaterialTheme.typography.labelLarge, color = content)
                }
            }
        }
    }
}
