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

package com.movtery.zalithlauncher.ui.screens.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.startup.StartupVideoLayout
import com.movtery.zalithlauncher.startup.StartupVideoScale
import java.io.File
import kotlin.math.roundToInt

private const val MIN_ZOOM = 50f
private const val MAX_ZOOM = 400f
private const val MAX_OFFSET = 2000f

/**
 * Full screen preview of the startup video: pinch to zoom, drag to move, and pick how it is scaled.
 * Nothing changes until it is saved.
 */
@Composable
fun StartupVideoEditor(
    video: File,
    initial: StartupVideoLayout,
    onSave: (StartupVideoLayout) -> Unit,
    onCancel: () -> Unit
) {
    var scale by rememberSaveable { mutableStateOf(initial.scale) }
    //Kept as floats while editing, so that small gestures add up
    var zoom by remember { mutableStateOf(initial.zoom.toFloat()) }
    var offset by remember { mutableStateOf(Offset(initial.offsetX.toFloat(), initial.offsetY.toFloat())) }
    val layout = StartupVideoLayout(
        scale = scale,
        zoom = zoom.roundToInt(),
        offsetX = offset.x.roundToInt(),
        offsetY = offset.y.roundToInt()
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        StartupVideo(
            video = video,
            layout = layout,
            loop = true,
            muted = true
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, gestureZoom, _ ->
                        zoom = (zoom * gestureZoom).coerceIn(MIN_ZOOM, MAX_ZOOM)
                        offset = Offset(
                            (offset.x + pan.x * 1000f / size.width).coerceIn(-MAX_OFFSET, MAX_OFFSET),
                            (offset.y + pan.y * 1000f / size.height).coerceIn(-MAX_OFFSET, MAX_OFFSET)
                        )
                    }
                }
        )

        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(all = 12.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.9f)
        ) {
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StartupVideoScale.entries.forEach { entry ->
                    FilterChip(
                        selected = scale == entry,
                        onClick = { scale = entry },
                        label = { Text(text = stringResource(entry.textRes)) }
                    )
                }
                Text(
                    text = "${layout.zoom}%",
                    style = MaterialTheme.typography.labelLarge
                )
                TextButton(
                    onClick = {
                        scale = StartupVideoScale.FIT
                        zoom = 100f
                        offset = Offset.Zero
                    }
                ) {
                    Text(text = stringResource(R.string.generic_reset))
                }
                TextButton(onClick = onCancel) {
                    Text(text = stringResource(R.string.generic_cancel))
                }
                Button(onClick = { onSave(layout) }) {
                    Text(text = stringResource(R.string.generic_save))
                }
            }
        }
    }
}
