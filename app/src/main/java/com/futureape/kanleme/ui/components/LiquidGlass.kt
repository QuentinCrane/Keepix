package com.futureape.kanleme.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
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
        val baseBrush = Brush.linearGradient(
            colors = listOf(
                scheme.primary.copy(alpha = 0.16f),
                scheme.surface,
                scheme.surfaceVariant.copy(alpha = 0.72f),
                scheme.background,
            )
        )
        Box(modifier = modifier.background(baseBrush)) {
            Box(
                Modifier.fillMaxSize().background(
                    Brush.radialGradient(
                        colors = listOf(scheme.primary.copy(alpha = 0.26f), Color.Transparent),
                        center = Offset(120f, 160f),
                        radius = 760f,
                    )
                )
            )
            Box(
                Modifier.fillMaxSize().background(
                    Brush.radialGradient(
                        colors = listOf(scheme.tertiary.copy(alpha = 0.18f), Color.Transparent),
                        center = Offset(920f, 320f),
                        radius = 820f,
                    )
                )
            )
            Box(
                Modifier.fillMaxSize().background(
                    Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = 0.22f), Color.Transparent),
                        center = Offset(520f, 980f),
                        radius = 900f,
                    )
                )
            )
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
    val glassColors = if (oledDark) {
        listOf(
            Color(0xFF050505).copy(alpha = 0.96f),
            Color(0xFF080808).copy(alpha = 0.96f),
            Color(0xFF000000).copy(alpha = 0.96f),
        )
    } else {
        listOf(
            Color.White.copy(alpha = 0.42f),
            scheme.surface.copy(alpha = tonalAlpha),
            scheme.surfaceVariant.copy(alpha = (tonalAlpha * 0.72f).coerceIn(0f, 1f)),
        )
    }
    Surface(
        modifier = modifier
            .shadow(if (oledDark) 0.dp else 24.dp, shape, clip = false, ambientColor = scheme.primary.copy(alpha = if (oledDark) 0.00f else 0.18f), spotColor = scheme.primary.copy(alpha = if (oledDark) 0.00f else 0.16f))
            .clip(shape)
            .background(Brush.linearGradient(colors = glassColors), shape)
            .border(BorderStroke(1.dp, if (oledDark) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.52f)), shape),
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
    GlassSurface(modifier = modifier, shape = RoundedCornerShape(999.dp), tonalAlpha = 0.70f) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp),
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
    GlassSurface(modifier = modifier.width(72.dp).fillMaxHeight(), shape = RoundedCornerShape(36.dp), tonalAlpha = 0.70f) {
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
    Surface(
        modifier = Modifier
            .width(56.dp)
            .height(if (selected) 82.dp else 56.dp)
            .clip(RoundedCornerShape(28.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(28.dp),
        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else Color.Transparent,
        contentColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
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
    val width by animateDpAsState(if (selected) 120.dp else 62.dp, label = "nav_width")
    Surface(
        modifier = Modifier
            .height(48.dp)
            .width(width)
            .clip(RoundedCornerShape(999.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(999.dp),
        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else Color.Transparent,
        contentColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(22.dp))
            androidx.compose.animation.AnimatedVisibility(selected) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.width(8.dp))
                    Text(label, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}
