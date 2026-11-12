/*
 * Copyright (C) 2016 The Android Open Source Project
 * Copyright 2026 QuentinCrane
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Adapted from androidx/media 1.11.0 demo PlayerActivity: player creation,
 * audio focus, saved position and release lifecycle. Rewritten for Compose,
 * local content URIs and PlayerView controls; no demo network/DRM/ads code.
 */
package com.futureape.kanleme.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Handler
import android.os.Looper
import android.text.format.DateUtils
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.AudioAttributes
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerControlView
import androidx.media3.ui.PlayerView
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.ScreenRotation
import com.futureape.kanleme.R
import com.futureape.kanleme.data.media.AdvancedVideo
import com.futureape.kanleme.data.media.FolderPlaybackVideo
import com.futureape.kanleme.ui.util.openFolderVideoExternally
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun LightweightVideoViewer(
    video: AdvancedVideo,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)? = null,
    onMove: (() -> Unit)? = null,
    showAdjacent: Boolean = false,
    onPrevious: (() -> Unit)? = null,
    onNext: (() -> Unit)? = null,
) {
    val videos = remember(video.uri, video.name, video.mimeType) {
        listOf(FolderPlaybackVideo(video.uri, video.name, video.mimeType))
    }
    VideoPlaylistViewer(videos, randomMode = false, onDismiss = onDismiss, onDelete = onDelete, onMove = onMove,
        showAdjacent = showAdjacent, onPrevious = onPrevious, onNext = onNext)
}

@Composable
fun RandomFolderVideoViewer(videos: List<FolderPlaybackVideo>, onDismiss: () -> Unit) {
    if (videos.isNotEmpty()) VideoPlaylistViewer(videos, randomMode = true, onDismiss = onDismiss)
}

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VideoPlaylistViewer(
    videos: List<FolderPlaybackVideo>,
    randomMode: Boolean,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)? = null,
    onMove: (() -> Unit)? = null,
    showAdjacent: Boolean = false,
    onPrevious: (() -> Unit)? = null,
    onNext: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val activity = remember(context) { context.videoViewerActivity() }
    val configuration = LocalConfiguration.current
    val landscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val originalOrientation = rememberSaveable { activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
    var manualOrientation by rememberSaveable { mutableStateOf(false) }
    var pendingOrientation by rememberSaveable { mutableIntStateOf(0) }
    DisposableEffect(activity) {
        onDispose {
            if (manualOrientation && activity != null && !activity.isChangingConfigurations) {
                activity.requestedOrientation = originalOrientation
            }
        }
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val queueKey = remember(videos) { videos.map { it.uri }.hashCode() }
    var position by rememberSaveable(queueKey) { mutableLongStateOf(0L) }
    var currentIndex by rememberSaveable(queueKey) { mutableIntStateOf(0) }
    var resumePlaying by rememberSaveable(queueKey) { mutableStateOf(true) }
    var volume by rememberSaveable { mutableFloatStateOf(1f) }
    var lastAudibleVolume by rememberSaveable { mutableFloatStateOf(1f) }
    var speed by rememberSaveable { mutableFloatStateOf(1f) }
    var playbackFailed by remember(queueKey) { mutableStateOf(false) }
    var consecutiveFailures by remember(queueKey) { mutableIntStateOf(0) }
    var playing by remember(queueKey) { mutableStateOf(false) }
    var player by remember(queueKey) { mutableStateOf<ExoPlayer?>(null) }
    var playerView by remember(queueKey) { mutableStateOf<GesturePlayerView?>(null) }
    var seekPreview by remember(queueKey) { mutableStateOf<Pair<Long, Long>?>(null) }
    var gestureHint by remember(queueKey) { mutableStateOf<String?>(null) }
    var gestureHintRevision by remember(queueKey) { mutableIntStateOf(0) }
    var speedMenuExpanded by remember(queueKey) { mutableStateOf(false) }
    var moreMenuExpanded by remember { mutableStateOf(false) }
    var showGestureHelp by remember { mutableStateOf(false) }
    val video = videos[currentIndex.coerceIn(videos.indices)]

    LaunchedEffect(gestureHintRevision) {
        if (gestureHint != null) {
            val revision = gestureHintRevision
            kotlinx.coroutines.delay(1_000)
            if (gestureHintRevision == revision) gestureHint = null
        }
    }
    LaunchedEffect(pendingOrientation, landscape) {
        if (pendingOrientation != 0) {
            val targetReached = (pendingOrientation == 1) == landscape
            if (!targetReached) {
                kotlinx.coroutines.delay(2_500)
                Toast.makeText(context, R.string.advanced_rotation_unavailable, Toast.LENGTH_LONG).show()
            }
            pendingOrientation = 0
        }
    }

    DisposableEffect(queueKey, lifecycle, context) {
        fun createPlayer() {
            if (player != null) return
            // Local quick viewing: bounded sample-buffer target, no back buffer or preloading.
            val loadControl = DefaultLoadControl.Builder()
                .setBufferDurationsMs(1_500, 8_000, 250, 500)
                .setTargetBufferBytes(8 * 1024 * 1024)
                .setPrioritizeTimeOverSizeThresholds(false)
                .setBackBuffer(0, false)
                .build()
            player = ExoPlayer.Builder(context).setLoadControl(loadControl).build().also { instance ->
                instance.setAudioAttributes(AudioAttributes.DEFAULT, true)
                instance.setHandleAudioBecomingNoisy(true)
                instance.volume = volume
                instance.setPlaybackSpeed(speed)
                instance.addListener(object : Player.Listener {
                    override fun onPlayerError(error: PlaybackException) {
                        consecutiveFailures++
                        if (randomMode && consecutiveFailures < videos.size) {
                            val failedIndex = instance.currentMediaItemIndex
                            Handler(Looper.getMainLooper()).post {
                                if (player === instance && instance.currentMediaItemIndex == failedIndex && instance.playerError != null) {
                                    instance.seekToNextMediaItem()
                                    instance.prepare()
                                    instance.playWhenReady = true
                                }
                            }
                        } else {
                            playbackFailed = true
                        }
                    }
                    override fun onIsPlayingChanged(isPlaying: Boolean) { playing = isPlaying }
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_READY) {
                            playbackFailed = false
                        }
                    }
                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                        // A ready decoder can still fail. Only a completed item resets the
                        // consecutive-error count, preventing endless retries of a bad queue.
                        if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO || reason == Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT) {
                            consecutiveFailures = 0
                        }
                        val nextIndex = instance.currentMediaItemIndex
                        if (nextIndex in videos.indices) {
                            if (currentIndex != nextIndex) position = 0L
                            currentIndex = nextIndex
                        }
                    }
                })
                val startIndex = currentIndex.coerceIn(videos.indices)
                val startPosition = position
                instance.setMediaItems(videos.map { MediaItem.fromUri(it.uri) }, startIndex, startPosition)
                instance.repeatMode = if (randomMode) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF
                instance.prepare()
            }
            playerView?.player = player
        }
        fun releasePlayer() {
            playerView?.cancelActiveGesture()
            player?.let { instance ->
                position = instance.currentPosition.coerceAtLeast(0)
                currentIndex = instance.currentMediaItemIndex.coerceIn(videos.indices)
                playerView?.player = null
                instance.release()
            }
            player = null
        }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> createPlayer()
                Lifecycle.Event.ON_RESUME -> player?.playWhenReady = resumePlaying
                Lifecycle.Event.ON_PAUSE -> player?.let {
                    playerView?.cancelActiveGesture()
                    resumePlaying = it.playWhenReady
                    position = it.currentPosition.coerceAtLeast(0)
                    currentIndex = it.currentMediaItemIndex.coerceIn(videos.indices)
                    it.pause()
                }
                Lifecycle.Event.ON_STOP -> releasePlayer()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) createPlayer()
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) player?.playWhenReady = resumePlaying
        onDispose {
            lifecycle.removeObserver(observer)
            releasePlayer()
        }
    }

    Dialog(onDismissRequest = {
        if (manualOrientation && activity != null) {
            manualOrientation = false
            pendingOrientation = 0
            activity.requestedOrientation = originalOrientation
        } else onDismiss()
    }, properties = DialogProperties(usePlatformDefaultWidth = false,
        decorFitsSystemWindows = false)) {
        val dialogView = LocalView.current
        val dialogWindow = remember(dialogView) {
            (dialogView.parent as? DialogWindowProvider)?.window
        }
        val originalWindowBrightness = remember(dialogWindow) {
            dialogWindow?.attributes?.screenBrightness ?: -1f
        }
        var localBrightness by rememberSaveable {
            mutableFloatStateOf(originalWindowBrightness.takeIf { it >= 0f } ?: 0.5f)
        }
        var brightnessOverridden by rememberSaveable { mutableStateOf(false) }
        DisposableEffect(dialogWindow, originalWindowBrightness) {
            if (brightnessOverridden) dialogWindow?.let { window ->
                window.attributes = window.attributes.apply { screenBrightness = localBrightness }
            }
            onDispose {
                dialogWindow?.let { window ->
                    window.attributes = window.attributes.apply {
                        screenBrightness = originalWindowBrightness
                    }
                }
            }
        }
        val showGestureHint: (String) -> Unit = { hint ->
            gestureHint = hint
            gestureHintRevision += 1
        }
        val onTemporaryFastPlaybackChange: (Boolean) -> Unit = { active ->
            player?.setPlaybackSpeed(if (active) 2f else speed)
            if (active) showGestureHint(context.getString(R.string.advanced_gesture_hold_speed))
        }
        val onSeekPreviewChange: (Long?, Long) -> Unit = { previewPosition, duration ->
            seekPreview = previewPosition?.let { it to duration }
        }
        Column(Modifier.fillMaxSize().background(Color.Black).systemBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.advanced_back)) }
                Column(Modifier.weight(1f)) {
                    Text(video.name, color = Color.White, style = MaterialTheme.typography.titleMedium,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (randomMode) Text(stringResource(R.string.random_video_position, currentIndex + 1, videos.size),
                        color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.bodySmall)
                }
                if (activity != null) {
                    IconButton(onClick = {
                        manualOrientation = true
                        pendingOrientation = if (landscape) 2 else 1
                        activity.requestedOrientation = if (landscape) ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                            else ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    }) {
                        Icon(Icons.Rounded.ScreenRotation,
                            contentDescription = stringResource(if (landscape) R.string.advanced_rotate_portrait else R.string.advanced_rotate_landscape),
                            tint = Color.White)
                    }
                }
                Box {
                    IconButton(onClick = { moreMenuExpanded = true }) {
                        Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.advanced_more_actions), tint = Color.White)
                    }
                    DropdownMenu(expanded = moreMenuExpanded, onDismissRequest = { moreMenuExpanded = false }) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.advanced_external)) }, onClick = {
                            moreMenuExpanded = false
                            player?.pause()
                            resumePlaying = false
                            openFolderVideoExternally(context, video)
                        })
                        onMove?.let { action -> DropdownMenuItem(text = { Text(stringResource(R.string.advanced_move)) },
                            onClick = { moreMenuExpanded = false; action() }) }
                        onDelete?.let { action -> DropdownMenuItem(text = { Text(stringResource(R.string.advanced_delete)) },
                            onClick = { moreMenuExpanded = false; action() }) }
                        DropdownMenuItem(text = { Text(stringResource(R.string.advanced_gesture_help)) },
                            onClick = { moreMenuExpanded = false; showGestureHelp = true })
                    }
                }
            }
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                val activePlayer = player
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { viewContext ->
                        GesturePlayerView(viewContext).apply {
                            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                            useController = true
                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                            setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                            setKeepContentOnPlayerReset(true)
                            findViewById<PlayerControlView>(androidx.media3.ui.R.id.exo_controller)
                                ?.setTimeBarScrubbingEnabled(true)
                            findViewById<View>(androidx.media3.ui.R.id.exo_controls_background)
                                ?.setBackgroundColor(android.graphics.Color.TRANSPARENT)
                            setShowNextButton(randomMode)
                            setShowPreviousButton(randomMode)
                            setShowRewindButton(true)
                            setShowFastForwardButton(true)
                            keepScreenOn = playing
                            onGestureHint = showGestureHint
                            onBrightnessGesture = { value ->
                                localBrightness = value
                                brightnessOverridden = true
                                dialogWindow?.let { window ->
                                    window.attributes = window.attributes.apply { screenBrightness = value }
                                }
                                showGestureHint(viewContext.getString(R.string.advanced_gesture_brightness, (value * 100).roundToInt()))
                            }
                            onVolumeGesture = { value ->
                                volume = value
                                if (value > 0.01f) lastAudibleVolume = value
                                player?.volume = value
                                showGestureHint(viewContext.getString(R.string.advanced_gesture_volume, (value * 100).roundToInt()))
                            }
                            this.onTemporaryFastPlaybackChange = onTemporaryFastPlaybackChange
                            this.onSeekPreviewChange = onSeekPreviewChange
                            brightness = localBrightness
                            playerView = this
                            this.player = activePlayer
                        }
                    },
                    update = { view ->
                        if (playerView !== view) playerView = view
                        view.player = player
                        view.keepScreenOn = playing
                        view.onGestureHint = showGestureHint
                        view.onTemporaryFastPlaybackChange = onTemporaryFastPlaybackChange
                        view.onSeekPreviewChange = onSeekPreviewChange
                        view.brightness = localBrightness
                        view.onBrightnessGesture = { value ->
                            localBrightness = value
                            brightnessOverridden = true
                            dialogWindow?.let { window ->
                                window.attributes = window.attributes.apply { screenBrightness = value }
                            }
                            showGestureHint(context.getString(R.string.advanced_gesture_brightness, (value * 100).roundToInt()))
                        }
                        view.onVolumeGesture = { value ->
                            volume = value
                            if (value > 0.01f) lastAudibleVolume = value
                            player?.volume = value
                            showGestureHint(context.getString(R.string.advanced_gesture_volume, (value * 100).roundToInt()))
                        }
                    },
                    onRelease = {
                        it.cancelActiveGesture()
                        it.player = null
                        if (playerView === it) playerView = null
                    },
                )
                gestureHint?.let { hint ->
                    androidx.compose.material3.Surface(
                        modifier = Modifier.align(Alignment.Center),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(999.dp),
                        color = Color.Black.copy(alpha = 0.78f),
                        contentColor = Color.White,
                    ) {
                        Text(hint, Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                            style = MaterialTheme.typography.titleMedium)
                    }
                }
                seekPreview?.let { (previewPosition, duration) ->
                    androidx.compose.material3.Surface(
                        modifier = Modifier.align(Alignment.Center),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(999.dp),
                        color = Color.Black.copy(alpha = 0.72f),
                        contentColor = Color.White,
                    ) {
                        Text(
                            stringResource(
                                R.string.advanced_gesture_seek_preview,
                                DateUtils.formatElapsedTime(previewPosition / 1_000L),
                                DateUtils.formatElapsedTime(duration / 1_000L),
                            ),
                            Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
                if (playbackFailed) {
                    Column(Modifier.background(Color.Black.copy(alpha = 0.9f)).padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(stringResource(if (randomMode) R.string.random_video_playback_error else R.string.advanced_playback_error), color = Color.White)
                        Button(onClick = {
                            playbackFailed = false
                            consecutiveFailures = 0
                            player?.prepare()
                            player?.play()
                        }) { Text(stringResource(R.string.advanced_retry)) }
                    }
                }
            }
            FlowRow(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (randomMode || showAdjacent) {
                    TextButton(enabled = randomMode || onPrevious != null, onClick = {
                        if (randomMode) {
                            val failed = playbackFailed
                            playbackFailed = false
                            consecutiveFailures = 0
                            player?.let { it.seekToPreviousMediaItem(); if (failed) { it.prepare(); it.play() } }
                        } else onPrevious?.invoke()
                    }) { Text(stringResource(R.string.random_video_previous)) }
                    TextButton(enabled = randomMode || onNext != null, onClick = {
                        if (randomMode) {
                            val failed = playbackFailed
                            playbackFailed = false
                            consecutiveFailures = 0
                            player?.let { it.seekToNextMediaItem(); if (failed) { it.prepare(); it.play() } }
                        } else onNext?.invoke()
                    }) { Text(stringResource(R.string.random_video_next)) }
                }
                TextButton(onClick = {
                    volume = if (volume <= 0.01f) lastAudibleVolume.coerceAtLeast(0.02f) else {
                        lastAudibleVolume = volume
                        0f
                    }
                    player?.volume = volume
                }) {
                    Text(stringResource(if (volume <= 0.01f) R.string.advanced_muted else R.string.advanced_mute))
                }
                Box {
                    TextButton(onClick = { speedMenuExpanded = true }) {
                        Text(stringResource(R.string.advanced_speed) + " · " + speed.toString() + "×")
                    }
                    DropdownMenu(expanded = speedMenuExpanded, onDismissRequest = { speedMenuExpanded = false }) {
                        listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f).forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.toString() + "×") },
                                onClick = {
                                    speed = option
                                    player?.setPlaybackSpeed(option)
                                    speedMenuExpanded = false
                                },
                            )
                        }
                    }
                }
            }
        }
    }
    if (showGestureHelp) {
        AlertDialog(onDismissRequest = { showGestureHelp = false },
            title = { Text(stringResource(R.string.advanced_gesture_help)) },
            text = { Text(stringResource(R.string.advanced_gesture_hint)) },
            confirmButton = { TextButton(onClick = { showGestureHelp = false }) { Text(stringResource(R.string.advanced_apply)) } })
    }
}

private tailrec fun Context.videoViewerActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.videoViewerActivity()
    else -> null
}

/** Keeps normal PlayerView controls intact while adding local player gestures. */
private class GesturePlayerView(context: Context) : PlayerView(context) {
    var onGestureHint: (String) -> Unit = {}
    var onBrightnessGesture: (Float) -> Unit = {}
    var onVolumeGesture: (Float) -> Unit = {}
    var onTemporaryFastPlaybackChange: (Boolean) -> Unit = {}
    var onSeekPreviewChange: (Long?, Long) -> Unit = { _, _ -> }
    var brightness: Float = 0.5f

    private enum class GestureSide { NONE, SEEK, BRIGHTNESS, VOLUME }
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var side = GestureSide.NONE
    private var gestureStarted = false
    private var startY = 0f
    private var startX = 0f
    private var startValue = 0f
    private var seekStartPosition = 0L
    private var seekDuration = 0L
    private var seekTargetPosition = 0L
    private var temporaryFastPlayback = false

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(event: MotionEvent): Boolean = true

        override fun onDoubleTap(event: MotionEvent): Boolean {
            if (event.y >= height * 0.78f) return false
            val activePlayer = player ?: return false
            when {
                event.x < width / 3f -> {
                    activePlayer.seekTo((activePlayer.currentPosition - 10_000L).coerceAtLeast(0L))
                    onGestureHint(context.getString(R.string.advanced_gesture_seek_back, 10))
                }
                event.x > width * 2f / 3f -> {
                    val duration = activePlayer.duration
                    val target = activePlayer.currentPosition + 10_000L
                    activePlayer.seekTo(if (duration > 0L) target.coerceAtMost(duration) else target)
                    onGestureHint(context.getString(R.string.advanced_gesture_seek_forward, 10))
                }
                activePlayer.isPlaying -> {
                    activePlayer.pause()
                    onGestureHint(context.getString(R.string.advanced_gesture_paused))
                }
                else -> {
                    activePlayer.play()
                    onGestureHint(context.getString(R.string.advanced_gesture_playing))
                }
            }
            return true
        }

        override fun onLongPress(event: MotionEvent) {
            if (side != GestureSide.SEEK || event.y >= height * 0.78f || player == null) return
            if (!temporaryFastPlayback) {
                temporaryFastPlayback = true
                onTemporaryFastPlaybackChange(true)
            }
        }
    })

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)
        if (gestureStarted) {
            if (event.actionMasked == MotionEvent.ACTION_MOVE) {
                if (side == GestureSide.SEEK) updateSeekGesture(event) else updateGesture(event)
            }
            if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) {
                if (side == GestureSide.SEEK) {
                    val commit = event.actionMasked == MotionEvent.ACTION_UP
                    if (commit) updateSeekGesture(event)
                    finishSeekGesture(commit)
                }
                cancelTemporaryFastPlayback()
                gestureStarted = false
                side = GestureSide.NONE
            }
            return true
        }

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                startX = event.x
                startY = event.y
                side = when {
                    event.y >= height * 0.78f -> GestureSide.NONE
                    event.x < width * 0.22f -> GestureSide.BRIGHTNESS
                    event.x > width * 0.78f -> GestureSide.VOLUME
                    else -> GestureSide.SEEK
                }
                startValue = when (side) {
                    GestureSide.BRIGHTNESS -> brightness
                    GestureSide.VOLUME -> player?.volume ?: 1f
                    GestureSide.NONE, GestureSide.SEEK -> 0f
                }
                seekStartPosition = player?.currentPosition?.coerceAtLeast(0L) ?: 0L
                seekDuration = player?.duration?.takeIf { it > 0L } ?: 0L
                seekTargetPosition = seekStartPosition
            }
            MotionEvent.ACTION_MOVE -> {
                if (temporaryFastPlayback &&
                    (abs(event.x - startX) > touchSlop || abs(event.y - startY) > touchSlop)) {
                    cancelTemporaryFastPlayback()
                }
                val deltaY = startY - event.y
                val deltaX = event.x - startX
                val startsVerticalGesture = (side == GestureSide.BRIGHTNESS || side == GestureSide.VOLUME) &&
                    abs(deltaY) > touchSlop && abs(deltaY) > abs(deltaX) * 1.15f
                val startsSeekGesture = side == GestureSide.SEEK && seekDuration > 0L &&
                    abs(deltaX) > touchSlop && abs(deltaX) > abs(deltaY) * 1.15f
                if (startsVerticalGesture || startsSeekGesture) {
                    gestureStarted = true
                    MotionEvent.obtain(event).also { cancel ->
                        cancel.action = MotionEvent.ACTION_CANCEL
                        super.dispatchTouchEvent(cancel)
                        cancel.recycle()
                    }
                    if (side == GestureSide.SEEK) updateSeekGesture(event) else updateGesture(event)
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                cancelTemporaryFastPlayback()
                side = GestureSide.NONE
            }
        }
        return super.dispatchTouchEvent(event)
    }

    private fun updateGesture(event: MotionEvent) {
        val amount = ((startY - event.y) / height.coerceAtLeast(1) * 1.6f)
        val value = (startValue + amount).coerceIn(0f, 1f)
        when (side) {
            GestureSide.BRIGHTNESS -> {
                brightness = value
                onBrightnessGesture(value)
            }
            GestureSide.VOLUME -> {
                player?.volume = value
                onVolumeGesture(value)
            }
            GestureSide.NONE, GestureSide.SEEK -> Unit
        }
    }

    private fun updateSeekGesture(event: MotionEvent) {
        if (seekDuration <= 0L) return
        val fraction = (event.x - startX) / width.coerceAtLeast(1).toFloat()
        val delta = (fraction * seekDuration).toLong()
        seekTargetPosition = (seekStartPosition + delta).coerceIn(0L, seekDuration)
        onSeekPreviewChange(seekTargetPosition, seekDuration)
    }

    private fun finishSeekGesture(commit: Boolean) {
        if (commit && seekDuration > 0L) {
            player?.seekTo(seekTargetPosition)
            onGestureHint(context.getString(
                R.string.advanced_gesture_seek_target,
                DateUtils.formatElapsedTime(seekTargetPosition / 1_000L),
            ))
        }
        onSeekPreviewChange(null, seekDuration)
        seekDuration = 0L
    }

    fun cancelTemporaryFastPlayback() {
        if (!temporaryFastPlayback) return
        temporaryFastPlayback = false
        onTemporaryFastPlaybackChange(false)
    }

    fun cancelActiveGesture() {
        if (gestureStarted && side == GestureSide.SEEK) finishSeekGesture(commit = false)
        gestureStarted = false
        side = GestureSide.NONE
        cancelTemporaryFastPlayback()
    }
}
