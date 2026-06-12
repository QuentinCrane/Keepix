package com.futureape.kanleme.ui.screens

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.futureape.kanleme.R
import com.futureape.kanleme.ui.components.EmptyState
import com.futureape.kanleme.ui.i18n.Text

@Composable
fun PhotoCompareScreen(uris: List<Uri>, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(top = 36.dp, start = 18.dp, end = 18.dp)) {
        ScreenHeader("照片对比", "从系统分享菜单进入，适合 2-4 张图快速比较", onBack)
        if (uris.isEmpty()) {
            EmptyState("没有收到图片", stringResource(R.string.photo_compare_empty_message), "返回", onBack)
        } else {
            Text("已接收 ${uris.size} 张", style = MaterialTheme.typography.titleMedium)
            LazyVerticalGrid(
                columns = GridCells.Fixed(if (uris.size <= 2) 1 else 2),
                modifier = Modifier.fillMaxSize().padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(uris) { uri ->
                    AsyncImage(
                        model = uri,
                        contentDescription = null,
                        modifier = Modifier.clip(RoundedCornerShape(24.dp)),
                        contentScale = ContentScale.Crop,
                    )
                }
            }
        }
    }
}
