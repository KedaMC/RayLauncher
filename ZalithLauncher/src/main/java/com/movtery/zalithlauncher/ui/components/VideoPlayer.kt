/*
 * Zalith Launcher 2
 * Copyright (C) 2025-2026 MovTery <movtery228@qq.com> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/gpl-3.0.txt>.
 */

package com.movtery.zalithlauncher.ui.components

import android.net.Uri
import android.view.LayoutInflater
import androidx.annotation.FloatRange
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.movtery.zalithlauncher.R
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

private const val NEAR_END_POLL_MILLIS = 30

/**
 * 简单的沉浸式视频播放层
 * @param autoPlay 准备好后，是否立即播放视频
 * @param loop 是否循环播放视频
 * @param muted 是否开启静音
 * @param volume 音量
 * @param onFirstFrame called when the first frame is on screen
 * @param onFinished called when playback ended or failed
 * @param onNearEnd called once, [nearEndMillis] before the end (at most half of the video)
 */
@OptIn(UnstableApi::class)
@Composable
fun VideoPlayer(
    videoUri: Uri,
    modifier: Modifier = Modifier,
    autoPlay: Boolean = true,
    loop: Boolean = true,
    muted: Boolean = true,
    @FloatRange(from = 0.0, to = 1.0) volume: Float = if (muted) 0.0f else 1.0f,
    resizeMode: Int = AspectRatioFrameLayout.RESIZE_MODE_ZOOM,
    refreshTrigger: Any? = null,
    onFirstFrame: () -> Unit = {},
    onFinished: () -> Unit = {},
    nearEndMillis: Long = 0L,
    onNearEnd: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val player = remember {
        val renderersFactory = DefaultRenderersFactory(context)
            .setEnableDecoderFallback(true) //开启解码器 fallback

        ExoPlayer.Builder(context, renderersFactory).build().apply {
            val audioAttr = AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .build()
            setAudioAttributes(audioAttr, false)

            this.repeatMode = if (loop) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
            this.volume = volume
        }
    }

    val playerView = remember {
        val playerView = LayoutInflater.from(context)
            .inflate(R.layout.player_texture_view, null) as PlayerView

        playerView.apply {
            this.player = player
            this.resizeMode = resizeMode
        }
    }

    val currentOnFirstFrame by rememberUpdatedState(onFirstFrame)
    val currentOnFinished by rememberUpdatedState(onFinished)
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onRenderedFirstFrame() = currentOnFirstFrame()
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) currentOnFinished()
            }
            override fun onPlayerError(error: PlaybackException) = currentOnFinished()
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    LaunchedEffect(videoUri, refreshTrigger) {
        val mediaItem = MediaItem.fromUri(videoUri)
        player.setMediaItem(mediaItem)
        player.prepare()
    }

    val currentOnNearEnd by rememberUpdatedState(onNearEnd)
    val watchEnd = onNearEnd != null
    LaunchedEffect(player, videoUri, watchEnd) {
        if (!watchEnd) return@LaunchedEffect
        while (true) {
            delay(NEAR_END_POLL_MILLIS.milliseconds)
            val duration = player.duration
            if (duration == C.TIME_UNSET || !player.isPlaying) continue
            if (duration - player.currentPosition <= minOf(nearEndMillis, duration / 2)) break
        }
        currentOnNearEnd?.invoke()
    }

    LaunchedEffect(autoPlay) {
        player.playWhenReady = autoPlay
    }

    LaunchedEffect(loop) {
        player.repeatMode = if (loop) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
    }

    LaunchedEffect(muted, volume) {
        player.volume = volume
    }

    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    player.playWhenReady = autoPlay
                }
                Lifecycle.Event.ON_STOP -> {
                    player.playWhenReady = false
                }
                else -> {}
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            player.release()
        }
    }

    AndroidView(
        factory = { playerView },
        update = { it.resizeMode = resizeMode },
        modifier = modifier.fillMaxSize()
    )
}