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

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import com.movtery.zalithlauncher.startup.StartupTransition
import com.movtery.zalithlauncher.utils.animation.SwapTimeline
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

/** Time the page gets to settle before entrance animations are allowed again */
private const val SETTLE_MILLIS = 500

/**
 * Fades [content] in from black after startup. While [reveal] runs, [SwapTimeline.entranceSuppressed]
 * keeps the page from playing entrance animations.
 */
@Composable
fun StartupRevealHost(
    reveal: Boolean,
    content: @Composable () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        content()

        if (reveal) {
            var finished by remember { mutableStateOf(false) }
            val alpha = remember { Animatable(1f) }

            LaunchedEffect(Unit) {
                //Start fading only once the first frames of the page are done
                repeat(2) { withFrameNanos {} }
                alpha.animateTo(0f, tween(StartupTransition.fadeInMillis, easing = LinearEasing))
                finished = true
                delay(SETTLE_MILLIS.milliseconds)
                SwapTimeline.entranceSuppressed = false
            }
            DisposableEffect(Unit) {
                onDispose { SwapTimeline.entranceSuppressed = false }
            }

            if (!finished) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { this.alpha = alpha.value }
                        .background(Color.Black)
                )
            }
        }
    }
}
