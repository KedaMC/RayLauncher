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
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.media3.ui.AspectRatioFrameLayout
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.startup.InstallFailure
import com.movtery.zalithlauncher.startup.StartupTransition
import com.movtery.zalithlauncher.ui.components.VideoPlayer
import com.movtery.zalithlauncher.viewmodel.StartupPhase
import kotlinx.coroutines.delay
import java.io.File
import kotlin.time.Duration.Companion.milliseconds

/** How long "Ready!" stays on screen before leaving */
private const val READY_HOLD_MILLIS = 800

/** Gives up on a video that does not render its first frame */
private const val VIDEO_TIMEOUT_MILLIS = 3000

/**
 * The startup page on a black background: the first install, component updates and the startup video.
 * Everything fades in from and out to black.
 * @param video the startup animation, null for a plain black screen
 * @param onFinished called once, after the content has faded out
 */
@Composable
fun StartupScreen(
    phase: StartupPhase,
    isFirstInstall: Boolean,
    video: File?,
    onInstall: () -> Unit,
    onFinished: () -> Unit
) {
    val playingVideo = video.takeIf { phase == StartupPhase.Ready && !isFirstInstall }
    var videoRendered by remember { mutableStateOf(false) }
    var leaving by remember { mutableStateOf(false) }

    //A normal launch stays black until there is something to show
    val showInstaller = if (isFirstInstall) {
        phase != StartupPhase.Checking
    } else {
        phase == StartupPhase.Installing || phase is StartupPhase.Failed
    }
    val contentReady = showInstaller || videoRendered

    LaunchedEffect(phase) {
        if (phase != StartupPhase.Ready) return@LaunchedEffect
        if (isFirstInstall) {
            delay(READY_HOLD_MILLIS.milliseconds)
            leaving = true
        } else if (video == null) {
            leaving = true
        }
    }

    if (playingVideo != null) {
        LaunchedEffect(videoRendered) {
            if (!videoRendered) {
                delay(VIDEO_TIMEOUT_MILLIS.milliseconds)
                leaving = true
            }
        }
    }

    val alpha = remember { Animatable(0f) }
    val currentOnFinished by rememberUpdatedState(onFinished)
    LaunchedEffect(contentReady, leaving) {
        if (leaving) {
            if (alpha.value > 0f) {
                alpha.animateTo(0f, tween(StartupTransition.fadeOutMillis, easing = LinearEasing))
            }
            currentOnFinished()
        } else if (contentReady) {
            alpha.animateTo(1f, tween(StartupTransition.fadeInMillis, easing = LinearEasing))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        if (playingVideo != null) {
            VideoPlayer(
                videoUri = Uri.fromFile(playingVideo),
                loop = false,
                muted = false,
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT,
                onFirstFrame = { videoRendered = true },
                onFinished = { leaving = true }
            )
            //The video fades through this black layer, tapping it skips the video
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { this.alpha = 1f - alpha.value }
                    .background(Color.Black)
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { leaving = true }
            )
        } else if (showInstaller || isFirstInstall) {
            InstallerContent(
                modifier = Modifier.graphicsLayer { this.alpha = alpha.value },
                phase = phase,
                isFirstInstall = isFirstInstall,
                onInstall = onInstall
            )
        } else if (phase == StartupPhase.Ready) {
            //Keeps the loading indicator while an update install fades out
            InstallerContent(
                modifier = Modifier.graphicsLayer { this.alpha = alpha.value },
                phase = StartupPhase.Installing,
                isFirstInstall = false,
                onInstall = {}
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun InstallerContent(
    phase: StartupPhase,
    isFirstInstall: Boolean,
    onInstall: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(all = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
    ) {
        if (isFirstInstall) {
            Image(
                modifier = Modifier.size(72.dp),
                painter = painterResource(R.drawable.img_launcher),
                contentDescription = null
            )
            Text(
                text = stringResource(R.string.launcher_unofficial_notice),
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                textAlign = TextAlign.Center
            )
        }

        when (phase) {
            StartupPhase.Checking -> {}
            StartupPhase.AwaitingStart -> {
                Button(onClick = onInstall) {
                    Text(text = stringResource(R.string.welcome_screen_start))
                }
            }
            StartupPhase.Installing -> LoadingIndicator()
            is StartupPhase.Failed -> {
                FailureDetails(
                    modifier = Modifier.weight(1f, fill = false),
                    failures = phase.failures
                )
                Button(onClick = onInstall) {
                    Text(text = stringResource(R.string.generic_retry))
                }
            }
            StartupPhase.Ready -> {
                Button(onClick = {}) {
                    Text(text = stringResource(R.string.startup_ready))
                }
            }
        }
    }
}

@Composable
private fun FailureDetails(
    failures: List<InstallFailure>,
    modifier: Modifier = Modifier
) {
    SelectionContainer(modifier = modifier) {
        Column(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            failures.forEach { failure ->
                Column {
                    Text(
                        text = failure.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = failure.detail,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White
                    )
                }
            }
        }
    }
}
