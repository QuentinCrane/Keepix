package com.futureape.kanleme.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun AdaptiveCenter(
    modifier: Modifier = Modifier,
    maxWidth: Dp = 1180.dp,
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Box(Modifier.fillMaxWidth().widthIn(max = maxWidth)) {
            content()
        }
    }
}

@Composable
fun AdaptiveWidthInfo(content: @Composable (isMediumOrExpanded: Boolean, isExpanded: Boolean) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        content(maxWidth >= 600.dp, maxWidth >= 840.dp)
    }
}
