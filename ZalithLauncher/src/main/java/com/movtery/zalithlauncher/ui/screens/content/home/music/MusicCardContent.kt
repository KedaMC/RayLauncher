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

package com.movtery.zalithlauncher.ui.screens.content.home.music

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.movtery.cardgrid.model.CardState
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.music.AudioSpectrumBus
import com.movtery.zalithlauncher.music.MusicArtwork
import com.movtery.zalithlauncher.music.MusicManager
import com.movtery.zalithlauncher.music.MusicRepeat
import com.movtery.zalithlauncher.music.MusicState
import com.movtery.zalithlauncher.music.MusicStatus
import com.movtery.zalithlauncher.setting.enums.MusicSourceType
import com.movtery.zalithlauncher.ui.components.IconButton
import com.movtery.zalithlauncher.ui.components.MarqueeText
import kotlinx.coroutines.delay

private const val VISUALIZER_ROWS = 17
private const val VISUALIZER_FRAME_NANOS = 32_000_000L

/** Level every row settles on while there is no live audio data (paused): the static pattern */
private const val STATIC_LEVEL = 0.5f

/**
 * The Music Card: album artwork, title / artist / source app, a vertical audio visualizer, the
 * progress bar and the playback controls. It only shows the unified [MusicManager] state, so local
 * music and the music of other apps look and behave the same.
 */
@Composable
fun CardState.MusicCardContent() {
    val lifecycleOwner = LocalLifecycleOwner.current

    //The manager only runs while a Music Card is composed and the app is on the screen
    DisposableEffect(lifecycleOwner) {
        MusicManager.acquire()
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> MusicManager.onStart()
                Lifecycle.Event.ON_STOP -> MusicManager.onStop()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            MusicManager.release()
        }
    }

    val music by MusicManager.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = stringResource(R.string.music_card_now_playing),
            style = MaterialTheme.typography.labelMedium,
            letterSpacing = 1.2.sp,
            color = MaterialTheme.colorScheme.primary
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            MusicArtworkView(music = music)
            TrackInfo(
                modifier = Modifier.weight(1f),
                music = music
            )
            MusicVisualizer(
                modifier = Modifier
                    .width(54.dp)
                    .height(88.dp),
                playing = music.isPlaying,
                color = MaterialTheme.colorScheme.primary
            )
        }

        MusicProgress(music = music)
        MusicControls(music = music)
    }
}

@Composable
private fun MusicArtworkView(music: MusicState) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = Modifier
            .size(88.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center
    ) {
        when (val artwork = music.artwork) {
            is MusicArtwork.Image -> {
                Image(
                    modifier = Modifier.fillMaxSize(),
                    bitmap = artwork.bitmap.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop
                )
            }
            is MusicArtwork.LocalFile -> ArtworkModel(model = java.io.File(artwork.path))
            is MusicArtwork.Remote -> ArtworkModel(model = artwork.uri)
            null -> {
                Icon(
                    modifier = Modifier.size(36.dp),
                    painter = painterResource(R.drawable.ic_music_note_filled),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun ArtworkModel(model: Any) {
    val context = LocalContext.current
    val request = remember(model) {
        ImageRequest.Builder(context)
            .data(model)
            .crossfade(true)
            .build()
    }
    AsyncImage(
        modifier = Modifier.fillMaxSize(),
        model = request,
        contentDescription = null,
        contentScale = ContentScale.Crop
    )
}

@Composable
private fun TrackInfo(
    music: MusicState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        when (music.status) {
            MusicStatus.READY -> {
                MarqueeText(
                    text = music.title.ifBlank { stringResource(R.string.music_card_unknown_title) },
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                if (music.artist.isNotBlank()) {
                    Text(
                        modifier = Modifier.alpha(0.7f),
                        text = music.artist,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                SourceBadge(music = music)
            }
            MusicStatus.LIBRARY_EMPTY -> {
                StatusText(
                    title = stringResource(R.string.music_card_empty_title),
                    summary = stringResource(R.string.music_card_empty_summary)
                )
            }
            MusicStatus.ACCESS_REQUIRED -> {
                StatusText(
                    title = stringResource(R.string.music_card_access_title),
                    summary = stringResource(R.string.music_card_access_summary)
                )
                TextButton(
                    onClick = {
                        runCatching {
                            context.startActivity(
                                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                    }
                ) {
                    Text(text = stringResource(R.string.music_card_access_grant))
                }
            }
            MusicStatus.NO_SESSION -> {
                StatusText(
                    title = stringResource(R.string.music_card_no_session_title),
                    summary = stringResource(R.string.music_card_no_session_summary)
                )
            }
        }
    }
}

@Composable
private fun StatusText(title: String, summary: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
    Text(
        modifier = Modifier.alpha(0.7f),
        text = summary,
        style = MaterialTheme.typography.bodySmall,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis
    )
}

/**
 * Small badge under the artist that shows which app plays the music. It never replaces the album
 * artwork: the icon of the source app is read through the PackageManager.
 */
@Composable
private fun SourceBadge(music: MusicState) {
    val accent = MaterialTheme.colorScheme.primary
    val icon = music.sourceIcon
    val label = music.sourceLabel ?: stringResource(R.string.music_source_local)
    val clickable = music.source == MusicSourceType.BACKGROUND && music.sourcePackage != null
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .then(if (clickable) Modifier.clickable { MusicManager.openSourceApp() } else Modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (icon != null) {
            Image(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape),
                bitmap = icon.asImageBitmap(),
                contentDescription = label
            )
        } else {
            Icon(
                modifier = Modifier.size(18.dp),
                painter = painterResource(R.drawable.ic_music_note_filled),
                contentDescription = null,
                tint = accent
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = accent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Vertical waveform: symmetric horizontal lines. The line lengths follow the frequency bands that
 * are measured from the audio that is playing (bass in the middle, treble towards the ends).
 * Without live audio data (paused, or no data source) the lines settle on a static diamond.
 * Frames are only produced while the card is resumed and something is moving.
 */
@Composable
private fun MusicVisualizer(
    playing: Boolean,
    color: Color,
    modifier: Modifier = Modifier
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val bands = remember { FloatArray(AudioSpectrumBus.BANDS) }
    val shown = remember { FloatArray(VISUALIZER_ROWS) { STATIC_LEVEL } }
    val tick = remember { mutableLongStateOf(0L) }

    LaunchedEffect(playing, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            var lastFrame = 0L
            while (true) {
                val live = playing && AudioSpectrumBus.read(bands)
                val settled = shown.all { kotlin.math.abs(it - STATIC_LEVEL) < 0.01f }
                if (!live && settled) {
                    //Nothing is moving: stop producing frames until the music state changes
                    if (!playing) break
                    //Playing without a data source yet: look again shortly, no per-frame work
                    delay(200)
                    continue
                }
                val frame = androidx.compose.runtime.withFrameNanos { it }
                if (frame - lastFrame < VISUALIZER_FRAME_NANOS) continue
                lastFrame = frame

                val center = VISUALIZER_ROWS / 2
                for (row in 0 until VISUALIZER_ROWS) {
                    val distance = kotlin.math.abs(row - center)
                    val band = (distance * (AudioSpectrumBus.BANDS - 1) / center)
                        .coerceIn(0, AudioSpectrumBus.BANDS - 1)
                    val target = if (live) bands[band] else STATIC_LEVEL
                    val current = shown[row]
                    //Quick attack, slower decay: smooth without hiding the beat
                    val factor = if (target > current) 0.6f else 0.16f
                    val next = current + (target - current) * factor
                    shown[row] = if (kotlin.math.abs(next - STATIC_LEVEL) < 0.004f && !live) STATIC_LEVEL else next
                }
                tick.longValue = frame
            }
        }
    }

    androidx.compose.foundation.Canvas(modifier = modifier) {
        tick.longValue //Redraw only, no recomposition
        val lineHeight = size.height / (VISUALIZER_ROWS + (VISUALIZER_ROWS - 1) * 0.75f)
        val gap = lineHeight * 0.75f
        val center = VISUALIZER_ROWS / 2
        for (row in 0 until VISUALIZER_ROWS) {
            val distance = kotlin.math.abs(row - center).toFloat()
            //Diamond envelope: widest in the middle, short at both ends
            val envelope = (1f - (distance / (center + 1.5f)).let { it * it }).coerceIn(0.08f, 1f)
            val width = (size.width * envelope * (0.2f + 0.8f * shown[row])).coerceIn(lineHeight, size.width)
            drawRoundRect(
                color = color,
                topLeft = Offset((size.width - width) / 2f, row * (lineHeight + gap)),
                size = Size(width, lineHeight),
                cornerRadius = CornerRadius(lineHeight / 2f)
            )
        }
    }
}

@Composable
private fun MusicProgress(music: MusicState) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var position by remember { mutableLongStateOf(music.currentPositionMs()) }
    var dragFraction by remember { mutableStateOf<Float?>(null) }

    //The position is extrapolated from the last reported state, a few times per second, only while playing
    LaunchedEffect(music, lifecycleOwner) {
        position = music.currentPositionMs()
        if (music.playbackSpeed <= 0f) return@LaunchedEffect
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                position = music.currentPositionMs()
                delay(250)
            }
        }
    }

    val duration = music.durationMs
    val fraction = dragFraction
        ?: if (duration > 0L) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f
    val shownPosition = dragFraction?.let { (it * duration).toLong() } ?: position
    val seekable = music.status == MusicStatus.READY && music.canSeek && duration > 0L

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SeekBar(
            fraction = fraction,
            enabled = seekable,
            onPreview = { dragFraction = it },
            onCommit = { value ->
                dragFraction = null
                val target = (value * duration).toLong()
                position = target
                MusicManager.seekTo(target)
            },
            onCancel = { dragFraction = null }
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .alpha(0.7f),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = formatTime(shownPosition), style = MaterialTheme.typography.labelMedium)
            Text(text = formatTime(duration), style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun SeekBar(
    fraction: Float,
    enabled: Boolean,
    onPreview: (Float) -> Unit,
    onCommit: (Float) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
    var latest by remember { mutableStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(20.dp)
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures { offset ->
                    onCommit((offset.x / size.width).coerceIn(0f, 1f))
                }
            }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        latest = (offset.x / size.width).coerceIn(0f, 1f)
                        onPreview(latest)
                    },
                    onDragEnd = { onCommit(latest) },
                    onDragCancel = onCancel,
                    onHorizontalDrag = { change, _ ->
                        latest = (change.position.x / size.width).coerceIn(0f, 1f)
                        onPreview(latest)
                    }
                )
            }
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val thickness = 4.dp.toPx()
            val top = (size.height - thickness) / 2f
            val radius = CornerRadius(thickness / 2f)
            drawRoundRect(color = track, topLeft = Offset(0f, top), size = Size(size.width, thickness), cornerRadius = radius)
            val progress = size.width * fraction.coerceIn(0f, 1f)
            if (progress > 0f) {
                drawRoundRect(color = accent, topLeft = Offset(0f, top), size = Size(progress, thickness), cornerRadius = radius)
            }
        }
    }
}

@Composable
private fun MusicControls(music: MusicState) {
    val accent = MaterialTheme.colorScheme.primary
    val ready = music.status == MusicStatus.READY
    //Local music always has a playlist to start; other apps decide what they allow
    val canPlay = music.canPlayPause

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        //Loop: highlighted while any repeat mode is on, the icon tells all from one
        IconButton(
            onClick = { MusicManager.toggleRepeat() },
            enabled = ready && music.canRepeat,
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = if (music.repeatActive) accent.copy(alpha = 0.22f) else Color.Transparent
            )
        ) {
            Icon(
                painter = painterResource(
                    if (music.repeat == MusicRepeat.ONE) R.drawable.ic_music_repeat_one else R.drawable.ic_music_repeat
                ),
                contentDescription = stringResource(R.string.music_card_repeat),
                tint = if (music.repeatActive) accent else LocalContentColorAlpha()
            )
        }

        IconButton(
            onClick = { MusicManager.previous() },
            enabled = ready && music.canPrevious
        ) {
            Icon(
                modifier = Modifier.size(28.dp),
                painter = painterResource(R.drawable.ic_music_skip_previous_filled),
                contentDescription = stringResource(R.string.music_card_previous)
            )
        }

        IconButton(
            modifier = Modifier.size(60.dp),
            onClick = { MusicManager.playPause() },
            enabled = ready && canPlay,
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = accent,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                disabledContainerColor = accent.copy(alpha = 0.38f)
            )
        ) {
            Icon(
                modifier = Modifier.size(30.dp),
                painter = painterResource(
                    if (music.isPlaying) R.drawable.ic_music_pause_filled else R.drawable.ic_play_arrow_filled
                ),
                contentDescription = stringResource(
                    if (music.isPlaying) R.string.music_card_pause else R.string.music_card_play
                )
            )
        }

        IconButton(
            onClick = { MusicManager.next() },
            enabled = ready && music.canNext
        ) {
            Icon(
                modifier = Modifier.size(28.dp),
                painter = painterResource(R.drawable.ic_music_skip_next_filled),
                contentDescription = stringResource(R.string.music_card_next)
            )
        }

        //Keeps the play button centered
        Spacer(modifier = Modifier.size(48.dp))
    }
}

@Composable
private fun LocalContentColorAlpha(): Color =
    androidx.compose.material3.LocalContentColor.current.copy(alpha = 0.75f)

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms.coerceAtLeast(0L) / 1000L).toInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
