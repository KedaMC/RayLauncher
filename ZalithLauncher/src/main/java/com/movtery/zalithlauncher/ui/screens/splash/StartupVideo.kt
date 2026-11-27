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

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import com.movtery.zalithlauncher.startup.StartupVideoLayout
import com.movtery.zalithlauncher.ui.components.VideoPlayer
import java.io.File

/** The startup video placed as [layout] says. Shared by the startup page and its editor. */
@Composable
fun StartupVideo(
    video: File,
    layout: StartupVideoLayout,
    modifier: Modifier = Modifier,
    loop: Boolean = false,
    muted: Boolean = false,
    onFirstFrame: () -> Unit = {},
    onFinished: () -> Unit = {},
    volume: Float = if (muted) 0f else 1f,
    nearEndMillis: Long = 0L,
    onNearEnd: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
    ) {
        VideoPlayer(
            videoUri = Uri.fromFile(video),
            modifier = Modifier.graphicsLayer {
                scaleX = layout.zoom / 100f
                scaleY = layout.zoom / 100f
                translationX = size.width * layout.offsetX / 1000f
                translationY = size.height * layout.offsetY / 1000f
            },
            loop = loop,
            muted = muted,
            resizeMode = layout.scale.resizeMode,
            onFirstFrame = onFirstFrame,
            onFinished = onFinished,
            volume = volume,
            nearEndMillis = nearEndMillis,
            onNearEnd = onNearEnd
        )
    }
}
