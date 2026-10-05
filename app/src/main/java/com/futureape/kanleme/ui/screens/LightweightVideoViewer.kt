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
 * one local content URI and PlayerView controls; no demo network/DRM/ads code.
 */
package com.futureape.kanleme.ui.screens

import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
import androidx.media3.ui.PlayerView
import com.futureape.kanleme.R
import com.futureape.kanleme.data.media.AdvancedVideo
import com.futureape.kanleme.ui.util.openAdvancedVideoExternally

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LightweightVideoViewer(video: AdvancedVideo, onDismiss: () -> Unit, onDelete: (() -> Unit)? = null, onMove: (() -> Unit)? = null) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var position by rememberSaveable(video.uri) { mutableLongStateOf(0L) }
    var resumePlaying by rememberSaveable(video.uri) { mutableStateOf(true) }
    var muted by rememberSaveable(video.uri) { mutableStateOf(false) }
    var speed by rememberSaveable(video.uri) { mutableFloatStateOf(1f) }
    var playbackFailed by remember(video.uri) { mutableStateOf(false) }
    var playing by remember(video.uri) { mutableStateOf(false) }
    var player by remember(video.uri) { mutableStateOf<ExoPlayer?>(null) }
    var playerView by remember(video.uri) { mutableStateOf<PlayerView?>(null) }

    DisposableEffect(video.uri, lifecycle, context) {
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
                instance.volume = if (muted) 0f else 1f
                instance.setPlaybackSpeed(speed)
                instance.addListener(object : Player.Listener {
                    override fun onPlayerError(error: PlaybackException) { playbackFailed = true }
                    override fun onIsPlayingChanged(isPlaying: Boolean) { playing = isPlaying }
                })
                instance.setMediaItem(MediaItem.fromUri(video.uri))
                instance.seekTo(position)
                instance.prepare()
            }
            playerView?.player = player
        }
        fun releasePlayer() {
            player?.let { instance ->
                position = instance.currentPosition.coerceAtLeast(0)
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
                    resumePlaying = it.playWhenReady
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

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false,
        decorFitsSystemWindows = false)) {
        Column(Modifier.fillMaxSize().background(Color.Black).systemBarsPadding()) {
            FlowRow(Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.advanced_back)) }
                TextButton(onClick = {
                    player?.pause()
                    resumePlaying = false
                    openAdvancedVideoExternally(context, video)
                }) { Text(stringResource(R.string.advanced_external)) }
                onMove?.let { TextButton(onClick = it) { Text(stringResource(R.string.advanced_move)) } }
                onDelete?.let { TextButton(onClick = it) { Text(stringResource(R.string.advanced_delete), color = MaterialTheme.colorScheme.error) } }
            }
            Text(video.name, Modifier.padding(horizontal = 20.dp), color = Color.White,
                style = MaterialTheme.typography.titleMedium, maxLines = 2)
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                val activePlayer = player
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { viewContext ->
                        PlayerView(viewContext).apply {
                            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                            useController = true
                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                            setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                            setShowNextButton(false)
                            setShowPreviousButton(false)
                            setShowRewindButton(true)
                            setShowFastForwardButton(true)
                            keepScreenOn = playing
                            playerView = this
                            this.player = activePlayer
                        }
                    },
                    update = { it.player = player; it.keepScreenOn = playing },
                    onRelease = { it.player = null; if (playerView === it) playerView = null },
                )
                if (playbackFailed) {
                    Column(Modifier.background(Color.Black.copy(alpha = 0.9f)).padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(stringResource(R.string.advanced_playback_error), color = Color.White)
                        Button(onClick = {
                            playbackFailed = false
                            player?.prepare()
                            player?.play()
                        }) { Text(stringResource(R.string.advanced_retry)) }
                    }
                }
            }
            FlowRow(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { muted = !muted; player?.volume = if (muted) 0f else 1f }) {
                    Text(stringResource(if (muted) R.string.advanced_muted else R.string.advanced_mute))
                }
                TextButton(onClick = {
                    speed = when (speed) { 1f -> 1.5f; 1.5f -> 2f; else -> 1f }
                    player?.setPlaybackSpeed(speed)
                }) { Text(stringResource(R.string.advanced_speed) + " · " + speed.toString() + "×") }
            }
        }
    }
}
