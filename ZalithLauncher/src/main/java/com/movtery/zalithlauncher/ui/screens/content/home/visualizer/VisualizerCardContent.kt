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

package com.movtery.zalithlauncher.ui.screens.content.home.visualizer

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.music.AudioSpectrumBus
import com.movtery.zalithlauncher.music.MusicManager
import com.movtery.zalithlauncher.music.hasRecordAudioPermission
import com.movtery.zalithlauncher.ui.components.LiquidGlassSurface
import com.movtery.zalithlauncher.ui.components.liquidGlassBackdrop
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.roundToInt

private const val FRAME_NANOS = 32_000_000L

/** Preferred width of one bar plus its gap */
private val BarPitch = 12.dp
private const val MIN_BARS = 5
private const val MAX_BARS = 64

/** Share of the pitch that is filled by the bar itself */
private const val BAR_FILL = 0.72f

/**
 * Spectrum visualizer of whatever audio is playing: rounded bars standing on a common baseline,
 * low frequencies on the left. It has no playback controls and works without a Music card.
 * White while Liquid Glass is active, the theme color otherwise.
 */
@Composable
fun VisualizerCardContent() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var granted by remember { mutableStateOf(hasRecordAudioPermission(context)) }

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            granted = hasRecordAudioPermission(context)
            MusicManager.startSpectrum()
            try {
                awaitCancellation()
            } finally {
                MusicManager.stopSpectrum()
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { result ->
        granted = result
        MusicManager.refresh()
    }

    val glassBackdrop = liquidGlassBackdrop()
    val color = if (glassBackdrop != null) Color.White else MaterialTheme.colorScheme.primary

    Box(modifier = Modifier.fillMaxSize()) {
        if (glassBackdrop != null) {
            LiquidGlassSurface(
                backdrop = glassBackdrop,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxSize(),
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                blurRadius = 10.dp
            ) {}
        }

        SpectrumBars(
            color = color,
            modifier = Modifier
                .fillMaxSize()
                .padding(all = 12.dp)
        )

        //Without the permission the system gives no audio data
        if (!granted) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }
                    .padding(all = 8.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Text(
                    text = stringResource(R.string.visualizer_card_permission),
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Without live data every bar settles into a short pill, and no more frames are produced.
 */
@Composable
private fun SpectrumBars(color: Color, modifier: Modifier = Modifier) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val bands = remember { FloatArray(AudioSpectrumBus.BANDS) }
    val shown = remember { FloatArray(AudioSpectrumBus.BANDS) }
    val tick = remember { mutableLongStateOf(0L) }

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            var lastFrame = 0L
            while (true) {
                val live = AudioSpectrumBus.read(bands)
                if (!live && shown.all { it == 0f }) {
                    delay(100)
                    continue
                }
                val frame = withFrameNanos { it }
                if (frame - lastFrame < FRAME_NANOS) continue
                lastFrame = frame
                for (band in shown.indices) {
                    val target = if (live) bands[band] else 0f
                    val current = shown[band]
                    //Quick attack, slower decay
                    val factor = if (target > current) 0.6f else 0.18f
                    val next = current + (target - current) * factor
                    shown[band] = if (!live && abs(next) < 0.005f) 0f else next
                }
                tick.longValue = frame
            }
        }
    }

    Canvas(modifier = modifier) {
        tick.longValue //Redraw only, no recomposition
        val count = (size.width / BarPitch.toPx()).roundToInt().coerceIn(MIN_BARS, MAX_BARS)
        //The gaps sit between the bars only, so the first and last bar touch the edges
        val barWidth = size.width / (count + (count - 1) * (1f - BAR_FILL) / BAR_FILL)
        val pitch = barWidth / BAR_FILL
        val minHeight = barWidth.coerceAtMost(size.height)
        for (bar in 0 until count) {
            //Interpolates between the measured bands to fill any number of bars
            val position = bar * (shown.size - 1f) / (count - 1)
            val low = position.toInt()
            val high = (low + 1).coerceAtMost(shown.lastIndex)
            val level = shown[low] + (shown[high] - shown[low]) * (position - low)
            val barHeight = minHeight + (size.height - minHeight) * level.coerceIn(0f, 1f)
            drawRoundRect(
                color = color,
                topLeft = Offset(bar * pitch, size.height - barHeight),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f)
            )
        }
    }
}
