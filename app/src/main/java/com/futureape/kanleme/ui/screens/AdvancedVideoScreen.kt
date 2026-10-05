package com.futureape.kanleme.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.futureape.kanleme.R
import com.futureape.kanleme.data.media.*
import com.futureape.kanleme.ui.util.formatDuration
import com.futureape.kanleme.ui.util.formatSize
import com.futureape.kanleme.ui.util.openAdvancedVideoExternally
import com.futureape.kanleme.ui.util.AdvancedVideoThumbnails
import com.futureape.kanleme.ui.util.advancedVideoFolderFromTree
import com.futureape.kanleme.ui.util.advancedVideoInitialFolder
import com.futureape.kanleme.ui.viewmodel.KanlemeViewModel

@Composable
fun AdvancedVideoScreen(viewModel: KanlemeViewModel, onBack: () -> Unit) {
    AdvancedProfileTheme { AdvancedVideoContent(viewModel, onBack) }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun AdvancedVideoContent(viewModel: KanlemeViewModel, onBack: () -> Unit) {
    val library = viewModel.advancedVideos
    val state by library.state.collectAsStateWithLifecycle()
    val filter by library.filter.collectAsStateWithLifecycle()
    val results by library.results.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var access by remember { mutableStateOf(MediaAccessSnapshot.current(context)) }
    var folderSheet by remember { mutableStateOf(false) }
    var filterSheet by remember { mutableStateOf(false) }
    var viewingUri by rememberSaveable { mutableStateOf<String?>(null) }
    var deletingUri by rememberSaveable { mutableStateOf<String?>(null) }
    var firstResume by remember { mutableStateOf(true) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        access = MediaAccessSnapshot.current(context)
        AdvancedVideoThumbnails.clear()
        library.invalidateAccess()
    }
    val requestAccess = {
        val permissions = if (Build.VERSION.SDK_INT >= 34) arrayOf(
            Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
        ) else if (Build.VERSION.SDK_INT >= 33) arrayOf(Manifest.permission.READ_MEDIA_VIDEO)
        else arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        permissionLauncher.launch(permissions)
    }
    val moveFolderLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        val target = library.consumeFolderPickerVideo()
        if (uri != null && target != null) {
            val folder = advancedVideoFolderFromTree(uri)
            when {
                folder == null -> viewModel.showMessage(R.string.advanced_local_folder_only)
                folder.volume != target.folder.volume -> viewModel.showMessage(R.string.advanced_same_volume)
                else -> viewModel.moveAdvancedVideo(target, folder.relativePath)
            }
        }
    }
    val chooseMoveFolder: (AdvancedVideo) -> Unit = { video ->
        viewingUri = null
        library.prepareFolderPicker(video)
        moveFolderLauncher.launch(advancedVideoInitialFolder(video.folder))
    }
    val scanFolderLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let {
            val folder = advancedVideoFolderFromTree(it)
            if (folder == null) viewModel.showMessage(R.string.advanced_local_folder_only)
            else library.selectSystemFolder(folder)
        }
    }
    LaunchedEffect(library) { library.loadFolders() }
    DisposableEffect(library, context) {
        onDispose {
            val clear = !context.isAdvancedConfigurationChange()
            library.leave(clearResults = clear)
            if (clear) AdvancedVideoThumbnails.clear()
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        val latest = MediaAccessSnapshot.current(context)
        if (!firstResume && (latest.key != access.key || (!latest.canReadVideos && latest.canReadSelectedVisualMedia))) {
            viewingUri = null
            AdvancedVideoThumbnails.clear()
            library.invalidateAccess()
        }
        access = latest
        firstResume = false
    }

    AdvancedPage(stringResource(R.string.advanced_video_title), onBack, stringResource(R.string.advanced_video_subtitle)) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                ProfilePanel(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(stringResource(R.string.advanced_scope), style = MaterialTheme.typography.titleMedium)
                        Text(if (state.selectedFolders == null) stringResource(R.string.advanced_scope_all)
                            else pluralStringResource(R.plurals.advanced_scope_folders, state.selectedFolders!!.size, state.selectedFolders!!.size))
                        if (!state.hasScanned) Text(stringResource(R.string.advanced_scope_note), style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(onClick = { folderSheet = true }, enabled = state.busyUris.isEmpty()) { Text(stringResource(R.string.advanced_select_folders)) }
                            Button(onClick = { if (state.scanning) library.cancelScan() else library.scan() },
                                enabled = (access.canReadVideos || access.canReadSelectedVisualMedia) && state.busyUris.isEmpty()) {
                                Text(stringResource(if (state.scanning) R.string.advanced_cancel
                                    else if (state.hasScanned) R.string.advanced_rescan else R.string.advanced_scan))
                            }
                        }
                        if (!access.canReadVideos) {
                            Text(stringResource(if (access.canReadSelectedVisualMedia) R.string.advanced_permission_limited
                                else R.string.advanced_video_permission), style = MaterialTheme.typography.bodySmall)
                            TextButton(onClick = requestAccess) { Text(stringResource(R.string.advanced_permissions)) }
                        }
                        if (state.scanning) {
                            LinearProgressIndicator(Modifier.fillMaxWidth())
                            Text(pluralStringResource(R.plurals.advanced_progress, state.scannedCount, state.scannedCount))
                        }
                        state.errorRes?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
                    }
                }
            }
            item {
                OutlinedTextField(filter.query, { library.updateFilter(filter.copy(query = it)) },
                    Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.advanced_search)) }, singleLine = true,
                    shape = RoundedCornerShape(22.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = { filterSheet = true }) { Text(stringResource(R.string.advanced_filter)) }
                    TextButton(onClick = { library.updateFilter(AdvancedVideoFilter()) }) { Text(stringResource(R.string.advanced_reset)) }
                }
            }
            if (state.hasScanned && !state.scanning && results.filter == filter) {
                item {
                    Text(pluralStringResource(R.plurals.advanced_result_count, results.videos.size, results.videos.size, results.totalCount,
                        formatSize(results.totalBytes)), style = MaterialTheme.typography.titleMedium)
                }
                if (results.videos.isEmpty()) item {
                    Text(stringResource(R.string.advanced_no_match), style = MaterialTheme.typography.titleLarge)
                    Text(stringResource(R.string.advanced_no_match_hint), Modifier.padding(top = 8.dp))
                }
                items(results.videos, key = { it.uri }) { video ->
                    VideoMetadataRow(video, busy = video.uri in state.busyUris,
                        onView = { viewingUri = video.uri }, onExternal = { openAdvancedVideoExternally(context, video) },
                        onDelete = { deletingUri = video.uri }, onMove = { chooseMoveFolder(video) })
                }
            } else if (!state.scanning && !state.hasScanned) item {
                Text(stringResource(R.string.advanced_start_hint), color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else if (!state.scanning) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        }
    }
    if (folderSheet) VideoFolderSheet(state.folders, state.selectedFolders, state.includeSubfolders, state.loadingFolders,
        onRefresh = library::loadFolders, onDismiss = { folderSheet = false }, onApply = { selected, include ->
            library.selectFolders(selected, include); folderSheet = false
        }, onSystemPicker = { include ->
            library.prepareScopeFolderPicker(include)
            folderSheet = false
            scanFolderLauncher.launch(advancedVideoInitialFolder())
        })
    if (filterSheet) VideoFilterSheet(filter, onDismiss = { filterSheet = false }, onApply = {
        library.updateFilter(it); filterSheet = false
    })
    val viewing = remember(state.videos, viewingUri) {
        viewingUri?.let { uri -> state.videos.firstOrNull { it.uri == uri } }
    }
    viewing?.let { video ->
        LightweightVideoViewer(video, onDismiss = { viewingUri = null },
            onDelete = { viewingUri = null; deletingUri = video.uri }, onMove = { chooseMoveFolder(video) })
    }
    val deleting = remember(state.videos, deletingUri) { deletingUri?.let { uri -> state.videos.firstOrNull { it.uri == uri } } }
    deleting?.let { video ->
        AlertDialog(onDismissRequest = { deletingUri = null }, containerColor = ProfileScreenSurface,
            title = { Text(stringResource(R.string.advanced_delete_title)) },
            text = { Text(stringResource(R.string.advanced_delete_body, video.name)) },
            confirmButton = {
                TextButton(onClick = { deletingUri = null; viewModel.deleteAdvancedVideo(video) },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                    Text(stringResource(R.string.advanced_delete_confirm))
                }
            }, dismissButton = { TextButton(onClick = { deletingUri = null }) { Text(stringResource(R.string.advanced_cancel)) } })
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun VideoMetadataRow(video: AdvancedVideo, busy: Boolean, onView: () -> Unit, onExternal: () -> Unit,
    onDelete: () -> Unit, onMove: () -> Unit) {
    val unknown = stringResource(R.string.advanced_unknown)
    ProfilePanel(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                AdvancedVideoCover(video, onView, Modifier.width(112.dp).height(72.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(video.name, Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(onClick = onView),
                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(stringResource(R.string.advanced_metadata,
                        if (video.shortEdge > 0) video.width.toString() + " × " + video.height else unknown,
                        if (video.sizeBytes > 0) formatSize(video.sizeBytes) else unknown,
                        if (video.durationMs > 0) formatDuration(video.durationMs) else unknown),
                        style = MaterialTheme.typography.bodySmall)
                }
            }
            Text(video.folder.relativePath,
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
private fun AdvancedVideoCover(video: AdvancedVideo, onView: () -> Unit, modifier: Modifier) {
    val context = LocalContext.current.applicationContext
    val thumbnail by produceState<Bitmap?>(null, AdvancedVideoThumbnails.key(video)) {
        value = AdvancedVideoThumbnails.load(context, video)
    }
    Box(modifier.clip(RoundedCornerShape(14.dp))
        .background(Color.White.copy(alpha = 0.06f)).clickable(onClick = onView), contentAlignment = Alignment.Center) {
        val bitmap = thumbnail
        if (bitmap != null) Image(bitmap.asImageBitmap(), stringResource(R.string.advanced_cover, video.name),
            Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
        else Icon(Icons.Rounded.Movie, stringResource(R.string.advanced_cover_missing) + ": " + video.name,
            Modifier.size(28.dp), tint = ProfileScreenAccent)
        Text(if (video.durationMs > 0) formatDuration(video.durationMs) else stringResource(R.string.advanced_unknown),
            Modifier.align(Alignment.BottomEnd).padding(4.dp).background(Color.Black.copy(alpha = 0.72f), RoundedCornerShape(6.dp)).padding(3.dp),
            color = Color.White, style = MaterialTheme.typography.labelLarge)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VideoFolderSheet(
    folders: List<VideoFolder>, selected: Set<VideoFolderKey>?, includeSubfolders: Boolean, loading: Boolean,
    onRefresh: () -> Unit, onDismiss: () -> Unit, onApply: (Set<VideoFolderKey>?, Boolean) -> Unit,
    onSystemPicker: (Boolean) -> Unit,
) {
    var all by remember { mutableStateOf(selected == null) }
    var draft by remember { mutableStateOf(selected.orEmpty()) }
    var query by remember { mutableStateOf("") }
    var include by remember { mutableStateOf(includeSubfolders) }
    val visible = remember(folders, query) { folders.filter {
        (it.key.volume + "/" + it.key.relativePath).contains(query, ignoreCase = true)
    } }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = ProfileScreenSurface, contentColor = androidx.compose.ui.graphics.Color.White,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)) {
        Column(Modifier.fillMaxWidth().heightIn(max = 640.dp).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.advanced_select_folders), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.advanced_folder_direct), style = MaterialTheme.typography.bodySmall)
            OutlinedButton(onClick = { onSystemPicker(include) }) { Text(stringResource(R.string.advanced_system_folder)) }
            Row(Modifier.fillMaxWidth().clickable { include = !include }, verticalAlignment = Alignment.CenterVertically) {
                Checkbox(include, { include = it })
                Text(stringResource(R.string.advanced_include_subfolders))
            }
            FilterChip(selected = all, onClick = { all = !all }, label = { Text(stringResource(R.string.advanced_scope_all)) }, shape = RoundedCornerShape(999.dp))
            OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), singleLine = true,
                label = { Text(stringResource(R.string.advanced_folder_search)) }, shape = RoundedCornerShape(22.dp))
            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (visible.isEmpty() && !loading) item { Text(stringResource(R.string.advanced_folder_empty)) }
                items(visible, key = { it.key.volume + ":" + it.key.relativePath }) { folder ->
                    val toggle = {
                        all = false
                        draft = if (folder.key in draft) draft - folder.key else draft + folder.key
                    }
                    Row(Modifier.fillMaxWidth().clickable(onClick = toggle).padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(!all && folder.key in draft, { toggle() })
                        Column(Modifier.weight(1f)) {
                            Text(folder.key.relativePath.ifEmpty { stringResource(R.string.advanced_root_folder) })
                            Text(folder.key.volume + " · " + pluralStringResource(R.plurals.advanced_folder_count, folder.count, folder.count),
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
private fun VideoFilterSheet(initial: AdvancedVideoFilter, onDismiss: () -> Unit, onApply: (AdvancedVideoFilter) -> Unit) {
    var draft by remember { mutableStateOf(initial) }
    fun mib(value: Long?): String = value?.let { java.math.BigDecimal(it).divide(java.math.BigDecimal(1_048_576)).stripTrailingZeros().toPlainString() }.orEmpty()
    var minSize by remember { mutableStateOf(mib(initial.size.minBytes)) }
    var maxSize by remember { mutableStateOf(mib(initial.size.maxBytes)) }
    val range = remember(minSize, maxSize) { VideoSizeRange.parse(minSize, maxSize) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = ProfileScreenSurface, contentColor = androidx.compose.ui.graphics.Color.White,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)) {
        Column(Modifier.fillMaxWidth().heightIn(max = 720.dp).padding(horizontal = 20.dp)) {
            Text(stringResource(R.string.advanced_filter), style = MaterialTheme.typography.titleLarge)
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)) {
                item {
                    VideoChoices(R.string.advanced_resolution, VideoResolution.entries, draft.resolution) { draft = draft.copy(resolution = it) }
                    Text(stringResource(R.string.advanced_resolution_note), style = MaterialTheme.typography.bodySmall)
                }
                item {
                    Text(stringResource(R.string.advanced_size), style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(minSize, { minSize = it }, Modifier.weight(1f), singleLine = true,
                            isError = range.isFailure, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            label = { Text(stringResource(R.string.advanced_size_min)) }, shape = RoundedCornerShape(18.dp))
                        OutlinedTextField(maxSize, { maxSize = it }, Modifier.weight(1f), singleLine = true,
                            isError = range.isFailure, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            label = { Text(stringResource(R.string.advanced_size_max)) }, shape = RoundedCornerShape(18.dp))
                    }
                    Text(stringResource(if (range.isFailure) R.string.advanced_size_error else R.string.advanced_size_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (range.isFailure) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                    SizePresets(minSize, maxSize) { min, max -> minSize = min; maxSize = max }
                }
                item { VideoChoices(R.string.advanced_duration, VideoDuration.entries, draft.duration) { draft = draft.copy(duration = it) } }
                item { VideoChoices(R.string.advanced_orientation, VideoOrientation.entries, draft.orientation) { draft = draft.copy(orientation = it) } }
                item { VideoChoices(R.string.advanced_format, VideoFormat.entries, draft.format) { draft = draft.copy(format = it) } }
                item { VideoChoices(R.string.advanced_date, VideoDate.entries, draft.date) { draft = draft.copy(date = it) } }
                item { VideoChoices(R.string.advanced_sort, VideoSort.entries, draft.sort) { draft = draft.copy(sort = it) } }
            }
            Row(Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = { draft = AdvancedVideoFilter(query = initial.query); minSize = ""; maxSize = "" }) { Text(stringResource(R.string.advanced_reset)) }
                Button(onClick = { range.getOrNull()?.let { onApply(draft.copy(size = it)) } }, enabled = range.isSuccess) { Text(stringResource(R.string.advanced_apply)) }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SizePresets(min: String, max: String, onSelect: (String, String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(Triple("", "", R.string.advanced_all), Triple("", "100", R.string.advanced_size_small),
            Triple("500", "", R.string.advanced_size_large), Triple("1024", "", R.string.advanced_size_huge)).forEach {
            FilterChip(min == it.first && max == it.second, { onSelect(it.first, it.second) }, label = { Text(stringResource(it.third)) }, shape = RoundedCornerShape(999.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T : Enum<T>> VideoChoices(titleRes: Int, values: List<T>, selected: T, onSelect: (T) -> Unit) {
    Text(stringResource(titleRes), style = MaterialTheme.typography.titleMedium)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        values.forEach { value ->
            val label = videoFilterLabel(value)
            FilterChip(value == selected, { onSelect(value) }, label = { Text(label) }, shape = RoundedCornerShape(999.dp))
        }
    }
}

@Composable
private fun videoFilterLabel(value: Enum<*>): String {
    val res = when (value) {
        VideoResolution.BELOW_HD -> R.string.advanced_res_low
        VideoResolution.HD -> R.string.advanced_res_hd
        VideoResolution.FULL_HD -> R.string.advanced_res_fhd
        VideoResolution.UHD -> R.string.advanced_res_uhd
        VideoDuration.SHORT -> R.string.advanced_dur_short
        VideoDuration.MEDIUM -> R.string.advanced_dur_medium
        VideoDuration.LONG -> R.string.advanced_dur_long
        VideoOrientation.LANDSCAPE -> R.string.advanced_landscape
        VideoOrientation.PORTRAIT -> R.string.advanced_portrait
        VideoOrientation.SQUARE -> R.string.advanced_square
        VideoFormat.OTHER -> R.string.advanced_other
        VideoDate.WEEK -> R.string.advanced_week
        VideoDate.MONTH -> R.string.advanced_month
        VideoDate.YEAR -> R.string.advanced_year
        VideoSort.NEWEST -> R.string.advanced_newest
        VideoSort.OLDEST -> R.string.advanced_oldest
        VideoSort.LARGEST -> R.string.advanced_largest
        VideoSort.SMALLEST -> R.string.advanced_smallest
        VideoSort.RESOLUTION -> R.string.advanced_res_sort
        VideoSort.DURATION -> R.string.advanced_dur_sort
        VideoSort.NAME -> R.string.advanced_name_sort
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
