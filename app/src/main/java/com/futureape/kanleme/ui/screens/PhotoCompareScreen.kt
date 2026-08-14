package com.futureape.kanleme.ui.screens

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.futureape.kanleme.R
import com.futureape.kanleme.ui.components.EmptyState

@Composable
fun PhotoCompareScreen(
    uris: List<Uri>,
    isValidating: Boolean = false,
    onBack: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(top = 12.dp, start = 18.dp, end = 18.dp),
    ) {
        ScreenHeader(
            stringResource(R.string.photo_compare_title),
            stringResource(R.string.photo_compare_subtitle),
            onBack,
        )
        if (isValidating) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CircularProgressIndicator()
                    Text(stringResource(R.string.photo_compare_checking), style = MaterialTheme.typography.bodyLarge)
                }
            }
        } else if (uris.isEmpty()) {
            EmptyState(
                stringResource(R.string.photo_compare_empty_title),
                stringResource(R.string.photo_compare_empty_message),
                stringResource(R.string.onboarding_previous),
                onBack,
            )
        } else {
            Text(stringResource(R.string.photo_compare_received, uris.size), style = MaterialTheme.typography.titleMedium)
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
