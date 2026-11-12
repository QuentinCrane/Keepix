package com.futureape.kanleme.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.futureape.kanleme.R
import com.futureape.kanleme.data.media.*
import com.futureape.kanleme.ui.components.LongImageViewer
import com.futureape.kanleme.ui.components.isLongImageCandidate
import com.futureape.kanleme.ui.util.advancedPhotoFolderFromTree
import com.futureape.kanleme.ui.util.advancedPhotoInitialFolder
import com.futureape.kanleme.ui.util.formatSize
import com.futureape.kanleme.ui.util.openAdvancedPhotoExternally
import com.futureape.kanleme.ui.viewmodel.KanlemeViewModel
import kotlin.math.abs

@Composable
fun AdvancedPhotoScreen(viewModel: KanlemeViewModel, onBack: () -> Unit) {
    AdvancedProfileTheme { AdvancedPhotoContent(viewModel, onBack) }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AdvancedPhotoContent(viewModel: KanlemeViewModel, onBack: () -> Unit) {
    val library = viewModel.advancedPhotos
    val state by library.state.collectAsStateWithLifecycle()
    val filter by library.filter.collectAsStateWithLifecycle()
    val results by library.results.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var access by remember { mutableStateOf(MediaAccessSnapshot.current(context)) }
    var folderSheet by remember { mutableStateOf(false) }
    var filterSheet by remember { mutableStateOf(false) }
    var viewingUri by rememberSaveable { mutableStateOf<String?>(null) }
    var deletingUri by rememberSaveable { mutableStateOf<String?>(null) }
    var gridView by rememberSaveable { mutableStateOf(true) }
    var firstResume by remember { mutableStateOf(true) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        access = MediaAccessSnapshot.current(context)
        library.invalidateAccess()
    }
    val requestAccess = {
        val permissions = if (Build.VERSION.SDK_INT >= 34) arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
        ) else if (Build.VERSION.SDK_INT >= 33) arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        else arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        permissionLauncher.launch(permissions)
    }
    val moveFolderLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        val target = library.consumeFolderPickerPhoto()
        if (uri != null && target != null) {
            val folder = advancedPhotoFolderFromTree(uri)
            when {
                folder == null -> viewModel.showMessage(R.string.advanced_local_folder_only)
                folder.volume != target.folder.volume -> viewModel.showMessage(R.string.advanced_same_volume)
                else -> viewModel.moveAdvancedPhoto(target, folder.relativePath)
            }
        }
    }
    val chooseMoveFolder: (AdvancedPhoto) -> Unit = { photo ->
        viewingUri = null
        library.prepareFolderPicker(photo)
        moveFolderLauncher.launch(advancedPhotoInitialFolder(photo.folder))
    }
    val scanFolderLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let {
            val folder = advancedPhotoFolderFromTree(it)
            if (folder == null) viewModel.showMessage(R.string.advanced_local_folder_only)
            else library.selectSystemFolder(folder)
        }
    }
    DisposableEffect(library, context) {
        library.loadFolders()
        onDispose {
            library.leave(clearResults = !context.isAdvancedConfigurationChange())
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        val latest = MediaAccessSnapshot.current(context)
        if (!firstResume && (latest.key != access.key || (!latest.canReadImages && latest.canReadSelectedVisualMedia))) {
            viewingUri = null
            library.invalidateAccess()
        }
        access = latest
        firstResume = false
    }

    AdvancedPage(stringResource(R.string.advanced_photo_title), onBack, stringResource(R.string.advanced_photo_subtitle)) { padding ->
        BoxWithConstraints(Modifier.fillMaxSize().padding(padding)) {
        val columns = ((maxWidth.value - 40f + 12f) / 182f).toInt().coerceIn(2, 6)
        val gridRows = remember(results.photos, columns, gridView) { if (gridView) results.photos.chunked(columns) else emptyList() }
        LazyColumn(
            Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                ProfilePanel(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(stringResource(R.string.advanced_scope), style = MaterialTheme.typography.titleMedium)
                        Text(if (state.selectedFolders == null) stringResource(R.string.advanced_photo_scope_all)
                            else pluralStringResource(R.plurals.advanced_scope_folders, state.selectedFolders!!.size, state.selectedFolders!!.size))
                        if (!state.hasScanned) Text(stringResource(R.string.advanced_photo_scope_note), style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(onClick = { folderSheet = true }, enabled = state.busyUris.isEmpty()) {
                                Text(stringResource(R.string.advanced_select_folders))
                            }
                            Button(onClick = { if (state.scanning) library.cancelScan() else library.scan() },
                                enabled = (access.canReadImages || access.canReadSelectedVisualMedia) && state.busyUris.isEmpty()) {
                                Text(stringResource(if (state.scanning) R.string.advanced_cancel
                                    else if (state.hasScanned) R.string.advanced_rescan else R.string.advanced_photo_scan))
                            }
                        }
                        if (!access.canReadImages) {
                            Text(stringResource(if (access.canReadSelectedVisualMedia) R.string.advanced_permission_limited
                                else R.string.advanced_photo_permission), style = MaterialTheme.typography.bodySmall)
                            TextButton(onClick = requestAccess) { Text(stringResource(R.string.advanced_photo_permissions)) }
                        }
                        if (state.scanning) {
                            LinearProgressIndicator(Modifier.fillMaxWidth())
                            Text(stringResource(R.string.advanced_photo_progress, state.scannedCount))
                        }
                        state.errorRes?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
                    }
                }
            }
            item {
                OutlinedTextField(filter.query, { library.updateFilter(filter.copy(query = it)) },
                    Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.advanced_photo_search)) }, singleLine = true,
                    trailingIcon = {
                        if (filter.query.isNotEmpty()) IconButton(onClick = { library.updateFilter(filter.copy(query = "")) }) {
                            Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.advanced_search_clear))
                        }
                    },
                    shape = RoundedCornerShape(22.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    val activeFilters = listOf(filter.resolution != PhotoResolution.ALL, filter.size != PhotoSizeRange(),
                        filter.orientation != PhotoOrientation.ALL, filter.format != PhotoFormat.ALL,
                        filter.kind != PhotoKind.ALL, filter.date != PhotoDate.ALL).count { it }
                    OutlinedButton(onClick = { filterSheet = true }) {
                        Text(if (activeFilters == 0) stringResource(R.string.advanced_filter)
                            else stringResource(R.string.advanced_filter_count, activeFilters))
                    }
                    TextButton(onClick = { library.updateFilter(AdvancedPhotoFilter()) }, enabled = filter != AdvancedPhotoFilter()) {
                        Text(stringResource(R.string.advanced_reset))
                    }
                }
            }
            if (state.hasScanned && !state.scanning && results.filter == filter) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.advanced_photo_result_count, results.photos.size, results.totalCount,
                            formatSize(results.totalBytes)), style = MaterialTheme.typography.titleMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = !gridView, onClick = { gridView = false },
                                label = { Text(stringResource(R.string.advanced_view_list)) },
                                leadingIcon = { Icon(Icons.AutoMirrored.Rounded.ViewList, contentDescription = null) })
                            FilterChip(selected = gridView, onClick = { gridView = true },
                                label = { Text(stringResource(R.string.advanced_view_grid)) },
                                leadingIcon = { Icon(Icons.Rounded.GridView, contentDescription = null) })
                        }
                    }
                }
                if (results.photos.isEmpty()) item {
                    Text(stringResource(R.string.advanced_photo_no_match), style = MaterialTheme.typography.titleLarge)
                    Text(stringResource(R.string.advanced_no_match_hint), Modifier.padding(top = 8.dp))
                }
                if (gridView) {
                    items(gridRows, key = { row -> "grid:" + row.first().uri }) { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                            row.forEach { photo ->
                                AdvancedPhotoGridCard(
                                    photo = photo,
                                    busy = photo.uri in state.busyUris,
                                    modifier = Modifier.weight(1f),
                                    onView = { viewingUri = photo.uri },
                                    onExternal = { openAdvancedPhotoExternally(context, photo) },
                                    onDelete = { deletingUri = photo.uri },
                                    onMove = { chooseMoveFolder(photo) },
                                )
                            }
                            repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                } else {
                    items(results.photos, key = { it.uri }) { photo ->
                        PhotoMetadataRow(photo, busy = photo.uri in state.busyUris,
                            onView = { viewingUri = photo.uri }, onExternal = { openAdvancedPhotoExternally(context, photo) },
                            onDelete = { deletingUri = photo.uri }, onMove = { chooseMoveFolder(photo) })
                    }
                }
            } else if (!state.scanning && !state.hasScanned) item {
                Text(stringResource(R.string.advanced_start_hint), color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else if (!state.scanning) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        }
        }
    }
    if (folderSheet) PhotoFolderSheet(state.folders, state.selectedFolders, state.includeSubfolders, state.loadingFolders,
        onRefresh = library::loadFolders, onDismiss = { folderSheet = false }, onApply = { selected, include ->
            library.selectFolders(selected, include); folderSheet = false
        }, onSystemPicker = { include ->
            library.prepareScopeFolderPicker(include)
            folderSheet = false
            scanFolderLauncher.launch(advancedPhotoInitialFolder())
        })
    if (filterSheet) PhotoFilterSheet(filter, onDismiss = { filterSheet = false }, onApply = {
        library.updateFilter(it); filterSheet = false
    })
    val viewing = remember(state.photos, viewingUri) { viewingUri?.let { uri -> state.photos.firstOrNull { it.uri == uri } } }
    viewing?.let { photo ->
        val index = remember(results.photos, photo.uri) { results.photos.indexOfFirst { it.uri == photo.uri } }
        val adjacent = results.filter == filter && index >= 0
        AdvancedPhotoPreviewDialog(photo, photo.uri in state.busyUris,
            onDismiss = { viewingUri = null }, onExternal = { openAdvancedPhotoExternally(context, photo) },
            onDelete = { viewingUri = null; deletingUri = photo.uri }, onMove = { chooseMoveFolder(photo) },
            index = if (adjacent) index else -1, count = if (adjacent) results.photos.size else 0,
            onPrevious = results.photos.getOrNull(index - 1)?.takeIf { adjacent }?.let { previous -> { viewingUri = previous.uri } },
            onNext = results.photos.getOrNull(index + 1)?.takeIf { adjacent }?.let { next -> { viewingUri = next.uri } })
    }
    val deleting = remember(state.photos, deletingUri) { deletingUri?.let { uri -> state.photos.firstOrNull { it.uri == uri } } }
    deleting?.let { photo ->
        AlertDialog(onDismissRequest = { deletingUri = null }, containerColor = ProfileScreenSurface,
            title = { Text(stringResource(R.string.advanced_delete_title)) },
            text = { Text(stringResource(R.string.advanced_photo_delete_body, photo.name)) },
            confirmButton = {
                TextButton(onClick = { deletingUri = null; viewModel.deleteAdvancedPhoto(photo) },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                    Text(stringResource(R.string.advanced_delete_confirm))
                }
            }, dismissButton = { TextButton(onClick = { deletingUri = null }) { Text(stringResource(R.string.advanced_cancel)) } })
    }
}

@Composable
private fun AdvancedPhotoGridCard(
    photo: AdvancedPhoto,
    busy: Boolean,
    modifier: Modifier = Modifier,
    onView: () -> Unit,
    onExternal: () -> Unit,
    onDelete: () -> Unit,
    onMove: () -> Unit,
) {
    var actionsExpanded by rememberSaveable(photo.uri) { mutableStateOf(false) }
    val unknown = stringResource(R.string.advanced_unknown)
    ProfilePanel(modifier) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            AsyncImage(model = photo.uri, contentDescription = stringResource(R.string.advanced_photo_cover, photo.name),
                modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.06f)).clickable(onClick = onView), contentScale = ContentScale.Crop)
            Text(photo.name, Modifier.fillMaxWidth().heightIn(min = 40.dp).clickable(onClick = onView),
                style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(photoMetadata(photo, unknown), style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(photo.folder.relativePath, Modifier.weight(1f), style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                PhotoActionsMenu(actionsExpanded, { actionsExpanded = it }, busy, onExternal, onMove, onDelete)
            }
            if (busy) Text(stringResource(R.string.advanced_busy), color = ProfileScreenAccent, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun PhotoMetadataRow(
    photo: AdvancedPhoto, busy: Boolean, onView: () -> Unit, onExternal: () -> Unit, onDelete: () -> Unit, onMove: () -> Unit,
) {
    val unknown = stringResource(R.string.advanced_unknown)
    ProfilePanel(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                AsyncImage(model = photo.uri, contentDescription = stringResource(R.string.advanced_photo_cover, photo.name),
                    modifier = Modifier.width(96.dp).height(96.dp).clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.06f)).clickable(onClick = onView), contentScale = ContentScale.Crop)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(photo.name, Modifier.fillMaxWidth().heightIn(min = 44.dp).clickable(onClick = onView),
                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(photoMetadata(photo, unknown), style = MaterialTheme.typography.bodySmall)
                }
            }
            Text(photo.folder.relativePath, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onView) { Text(stringResource(R.string.advanced_internal)) }
                TextButton(onClick = onExternal) { Text(stringResource(R.string.advanced_external)) }
                TextButton(onClick = onMove, enabled = !busy) { Text(stringResource(R.string.advanced_move)) }
                TextButton(onClick = onDelete, enabled = !busy,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                    Text(stringResource(R.string.advanced_delete))
                }
            }
            if (busy) Text(stringResource(R.string.advanced_busy), color = ProfileScreenAccent)
        }
    }
}

@Composable
private fun PhotoActionsMenu(
    expanded: Boolean, onExpanded: (Boolean) -> Unit, busy: Boolean,
    onExternal: () -> Unit, onMove: () -> Unit, onDelete: () -> Unit,
) {
    Box {
        IconButton(onClick = { onExpanded(true) }) {
            Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.advanced_more_actions))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { onExpanded(false) }) {
            DropdownMenuItem(text = { Text(stringResource(R.string.advanced_external)) }, onClick = { onExpanded(false); onExternal() })
            DropdownMenuItem(text = { Text(stringResource(R.string.advanced_move)) }, onClick = { onExpanded(false); onMove() }, enabled = !busy)
            DropdownMenuItem(text = { Text(stringResource(R.string.advanced_delete)) }, onClick = { onExpanded(false); onDelete() }, enabled = !busy)
        }
    }
}

@Composable
internal fun AdvancedPhotoPreviewDialog(
    photo: AdvancedPhoto, busy: Boolean, onDismiss: () -> Unit, onExternal: () -> Unit,
    onDelete: () -> Unit, onMove: () -> Unit,
    index: Int, count: Int, onPrevious: (() -> Unit)?, onNext: (() -> Unit)?,
) {
    val unknown = stringResource(R.string.advanced_unknown)
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(Modifier.fillMaxSize().background(Color.Black).padding(16.dp)) {
            Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.advanced_back)) }
                Text(photo.name, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium, color = Color.White)
            }
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                AdvancedPhotoPreviewImage(photo, onPrevious, onNext)
            }
            Column(Modifier.fillMaxWidth().padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (index >= 0 && count > 1) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(onClick = { onPrevious?.invoke() }, enabled = onPrevious != null) {
                            Text(stringResource(R.string.advanced_photo_previous))
                        }
                        Text(stringResource(R.string.advanced_photo_position, index + 1, count), color = Color.White)
                        TextButton(onClick = { onNext?.invoke() }, enabled = onNext != null) {
                            Text(stringResource(R.string.advanced_photo_next))
                        }
                    }
                }
                Text(photoMetadata(photo, unknown), color = Color.White.copy(alpha = 0.72f), style = MaterialTheme.typography.bodySmall)
                Text(photo.folder.relativePath, color = Color.White.copy(alpha = 0.50f), style = MaterialTheme.typography.bodySmall,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onExternal) { Text(stringResource(R.string.advanced_external)) }
                    OutlinedButton(onClick = onMove, enabled = !busy) { Text(stringResource(R.string.advanced_move)) }
                    Button(onClick = onDelete, enabled = !busy,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                        Text(stringResource(R.string.advanced_delete))
                    }
                }
            }
        }
    }
}

@Composable
private fun AdvancedPhotoPreviewImage(photo: AdvancedPhoto, onPrevious: (() -> Unit)?, onNext: (() -> Unit)?) {
    val description = stringResource(R.string.advanced_photo_cover, photo.name)
    var scale by remember(photo.uri) { mutableStateOf(1f) }
    var offset by remember(photo.uri) { mutableStateOf(Offset.Zero) }
    var size by remember(photo.uri) { mutableStateOf(IntSize.Zero) }
    val previous by rememberUpdatedState(onPrevious)
    val next by rememberUpdatedState(onNext)

    fun clampOffset(value: Offset, targetScale: Float): Offset {
        if (targetScale <= 1.01f) return Offset.Zero
        val maxX = size.width * (targetScale - 1f) / 2f
        val maxY = size.height * (targetScale - 1f) / 2f
        return Offset(value.x.coerceIn(-maxX, maxX), value.y.coerceIn(-maxY, maxY))
    }

    val fallback: @Composable () -> Unit = {
        Box(Modifier.fillMaxSize().clipToBounds().onSizeChanged { size = it }
            .pointerInput(photo.uri) {
                detectTapGestures(onDoubleTap = {
                    scale = if (scale > 1.01f) 1f else 2.5f
                    offset = Offset.Zero
                })
            }
            .pointerInput(photo.uri, size) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    var drag = Offset.Zero
                    var horizontalDrag = false
                    var transformed = scale > 1.01f
                    do {
                        val event = awaitPointerEvent()
                        val pointers = event.changes.count { it.pressed }
                        val pan = event.calculatePan()
                        if (pointers > 1 || scale > 1.01f) {
                            transformed = true
                            val newScale = (scale * event.calculateZoom()).coerceIn(1f, 5f)
                            offset = clampOffset(offset + pan, newScale)
                            scale = newScale
                            event.changes.forEach { it.consume() }
                        } else if (!transformed) {
                            drag += pan
                            if (abs(drag.x) > viewConfiguration.touchSlop && abs(drag.x) > abs(drag.y)) horizontalDrag = true
                            if (horizontalDrag) event.changes.forEach { it.consume() }
                        }
                    } while (event.changes.any { it.pressed })
                    if (!transformed && horizontalDrag && abs(drag.x) >= 64.dp.toPx()) {
                        if (drag.x > 0) previous?.invoke() else next?.invoke()
                    }
                }
            }, contentAlignment = Alignment.Center) {
            AsyncImage(model = photo.uri, contentDescription = description,
                modifier = Modifier.fillMaxSize().graphicsLayer {
                    scaleX = scale; scaleY = scale; translationX = offset.x; translationY = offset.y
                }, contentScale = ContentScale.Fit)
        }
    }

    if (isLongImageCandidate(photo.width, photo.height, photo.mimeType)) {
        LongImageViewer(uri = Uri.parse(photo.uri), description = description, modifier = Modifier.fillMaxSize(),
            mimeType = photo.mimeType, topInset = 0.dp, bottomInset = 0.dp, fallback = fallback)
    } else fallback()
}

@Composable
private fun photoMetadata(photo: AdvancedPhoto, unknown: String): String {
    val dimensions = if (photo.shortEdge > 0) photo.width.toString() + " × " + photo.height else unknown
    val size = if (photo.sizeBytes > 0) formatSize(photo.sizeBytes) else unknown
    val format = photo.mimeType.substringAfter('/', "").takeIf { it.isNotBlank() }?.uppercase() ?: photo.extension.uppercase().ifBlank { unknown }
    return stringResource(R.string.advanced_photo_metadata, dimensions, size, format)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhotoFolderSheet(
    folders: List<PhotoFolder>, selected: Set<PhotoFolderKey>?, includeSubfolders: Boolean, loading: Boolean,
    onRefresh: () -> Unit, onDismiss: () -> Unit, onApply: (Set<PhotoFolderKey>?, Boolean) -> Unit,
    onSystemPicker: (Boolean) -> Unit,
) {
    var all by remember { mutableStateOf(selected == null) }
    var draft by remember { mutableStateOf(selected.orEmpty()) }
    var query by remember { mutableStateOf("") }
    var include by remember { mutableStateOf(includeSubfolders) }
    val visible = remember(folders, query) { folders.filter { (it.key.volume + "/" + it.key.relativePath).contains(query, ignoreCase = true) } }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        sheetMaxWidth = 900.dp, containerColor = ProfileScreenSurface, contentColor = Color.White,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(0.94f).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.advanced_select_folders), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.advanced_folder_direct), style = MaterialTheme.typography.bodySmall)
            OutlinedButton(onClick = { onSystemPicker(include) }) { Text(stringResource(R.string.advanced_system_folder)) }
            Row(Modifier.fillMaxWidth().clickable { include = !include }, verticalAlignment = Alignment.CenterVertically) {
                Checkbox(include, { include = it })
                Text(stringResource(R.string.advanced_include_subfolders))
            }
            FilterChip(selected = all, onClick = { all = !all }, label = { Text(stringResource(R.string.advanced_photo_scope_all)) },
                shape = RoundedCornerShape(999.dp))
            OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), singleLine = true,
                label = { Text(stringResource(R.string.advanced_folder_search)) }, shape = RoundedCornerShape(22.dp))
            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (visible.isEmpty() && !loading) item { Text(stringResource(R.string.advanced_photo_folder_empty)) }
                items(visible, key = { it.key.volume + ":" + it.key.relativePath }) { folder ->
                    val toggle = {
                        all = false
                        draft = if (folder.key in draft) draft - folder.key else draft + folder.key
                    }
                    Row(Modifier.fillMaxWidth().clickable(onClick = toggle).padding(vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(!all && folder.key in draft, { toggle() })
                        Column(Modifier.weight(1f)) {
                            Text(folder.key.relativePath.ifEmpty { stringResource(R.string.advanced_root_folder) })
                            Text(folder.key.volume + " · " + stringResource(R.string.advanced_photo_folder_count, folder.count),
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = onRefresh, enabled = !loading) { Text(stringResource(R.string.advanced_refresh_folders)) }
                Button(onClick = { onApply(if (all) null else draft, include) }, enabled = all || draft.isNotEmpty()) {
                    Text(stringResource(R.string.advanced_apply))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhotoFilterSheet(initial: AdvancedPhotoFilter, onDismiss: () -> Unit, onApply: (AdvancedPhotoFilter) -> Unit) {
    var draft by remember { mutableStateOf(initial) }
    fun mib(value: Long?): String = value?.let {
        java.math.BigDecimal(it).divide(java.math.BigDecimal(1_048_576)).stripTrailingZeros().toPlainString()
    }.orEmpty()
    var minSize by remember { mutableStateOf(mib(initial.size.minBytes)) }
    var maxSize by remember { mutableStateOf(mib(initial.size.maxBytes)) }
    val range = remember(minSize, maxSize) { PhotoSizeRange.parse(minSize, maxSize) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = ProfileScreenSurface, contentColor = Color.White,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)) {
        Column(Modifier.fillMaxWidth().heightIn(max = 680.dp).padding(horizontal = 16.dp)) {
            Text(stringResource(R.string.advanced_filter), style = MaterialTheme.typography.titleMedium)
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    PhotoChoices(R.string.advanced_resolution, PhotoResolution.entries, draft.resolution) { draft = draft.copy(resolution = it) }
                    Text(stringResource(R.string.advanced_photo_resolution_note), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                item {
                    Text(stringResource(R.string.advanced_size), style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(minSize, { minSize = it }, Modifier.weight(1f), singleLine = true,
                            isError = range.isFailure, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            label = { Text(stringResource(R.string.advanced_size_min)) }, shape = RoundedCornerShape(16.dp))
                        OutlinedTextField(maxSize, { maxSize = it }, Modifier.weight(1f), singleLine = true,
                            isError = range.isFailure, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            label = { Text(stringResource(R.string.advanced_size_max)) }, shape = RoundedCornerShape(16.dp))
                    }
                    Text(stringResource(if (range.isFailure) R.string.advanced_size_error else R.string.advanced_size_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (range.isFailure) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                    PhotoSizePresets(minSize, maxSize) { min, max -> minSize = min; maxSize = max }
                }
                item { PhotoChoices(R.string.advanced_orientation, PhotoOrientation.entries, draft.orientation) { draft = draft.copy(orientation = it) } }
                item { PhotoChoices(R.string.advanced_format, PhotoFormat.entries, draft.format) { draft = draft.copy(format = it) } }
                item { PhotoChoices(R.string.advanced_photo_kind, PhotoKind.entries, draft.kind) { draft = draft.copy(kind = it) } }
                item { PhotoChoices(R.string.advanced_date, PhotoDate.entries, draft.date) { draft = draft.copy(date = it) } }
                item { PhotoChoices(R.string.advanced_sort, PhotoSort.entries, draft.sort) { draft = draft.copy(sort = it) } }
            }
            Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = { draft = AdvancedPhotoFilter(query = initial.query); minSize = ""; maxSize = "" }) {
                    Text(stringResource(R.string.advanced_reset))
                }
                Button(onClick = { range.getOrNull()?.let { onApply(draft.copy(size = it)) } }, enabled = range.isSuccess) {
                    Text(stringResource(R.string.advanced_apply))
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PhotoSizePresets(min: String, max: String, onSelect: (String, String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        listOf(Triple("", "", R.string.advanced_all), Triple("", "100", R.string.advanced_size_small),
            Triple("500", "", R.string.advanced_size_large), Triple("1024", "", R.string.advanced_size_huge)).forEach {
            FilterChip(min == it.first && max == it.second, { onSelect(it.first, it.second) },
                label = { Text(stringResource(it.third)) }, shape = RoundedCornerShape(999.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T : Enum<T>> PhotoChoices(titleRes: Int, values: List<T>, selected: T, onSelect: (T) -> Unit) {
    Text(stringResource(titleRes), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(bottom = 2.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        values.forEach { value ->
            FilterChip(value == selected, { onSelect(value) }, label = { Text(photoFilterLabel(value)) }, shape = RoundedCornerShape(999.dp))
        }
    }
}

@Composable
private fun photoFilterLabel(value: Enum<*>): String {
    val res = when (value) {
        PhotoResolution.BELOW_HD -> R.string.advanced_res_low
        PhotoResolution.HD -> R.string.advanced_res_hd
        PhotoResolution.FULL_HD -> R.string.advanced_res_fhd
        PhotoResolution.UHD -> R.string.advanced_res_uhd
        PhotoOrientation.LANDSCAPE -> R.string.advanced_landscape
        PhotoOrientation.PORTRAIT -> R.string.advanced_portrait
        PhotoOrientation.SQUARE -> R.string.advanced_square
        PhotoFormat.JPEG -> R.string.advanced_photo_format_jpeg
        PhotoFormat.PNG -> R.string.advanced_photo_format_png
        PhotoFormat.WEBP -> R.string.advanced_photo_format_webp
        PhotoFormat.HEIF -> R.string.advanced_photo_format_heif
        PhotoFormat.GIF -> R.string.advanced_photo_kind_gif
        PhotoFormat.RAW -> R.string.advanced_photo_kind_raw
        PhotoFormat.OTHER -> R.string.advanced_other
        PhotoKind.NORMAL -> R.string.advanced_photo_kind_normal
        PhotoKind.SCREENSHOT -> R.string.advanced_photo_kind_screenshot
        PhotoKind.SELFIE -> R.string.advanced_photo_kind_selfie
        PhotoKind.MOTION -> R.string.advanced_photo_kind_motion
        PhotoKind.GIF -> R.string.advanced_photo_kind_gif
        PhotoKind.RAW -> R.string.advanced_photo_kind_raw
        PhotoKind.LONG_IMAGE -> R.string.advanced_photo_kind_long
        PhotoDate.WEEK -> R.string.advanced_week
        PhotoDate.MONTH -> R.string.advanced_month
        PhotoDate.YEAR -> R.string.advanced_year
        PhotoSort.NEWEST -> R.string.advanced_newest
        PhotoSort.OLDEST -> R.string.advanced_oldest
        PhotoSort.LARGEST -> R.string.advanced_largest
        PhotoSort.SMALLEST -> R.string.advanced_smallest
        PhotoSort.RESOLUTION -> R.string.advanced_res_sort
        PhotoSort.NAME -> R.string.advanced_name_sort
        else -> when (value.name) {
            "ALL" -> R.string.advanced_all
            "UNKNOWN" -> R.string.advanced_unknown
            else -> null
        }
    }
    return res?.let { stringResource(it) } ?: value.name
}

private tailrec fun Context.isAdvancedConfigurationChange(): Boolean = when (this) {
    is Activity -> isChangingConfigurations
    is ContextWrapper -> baseContext.isAdvancedConfigurationChange()
    else -> false
}
