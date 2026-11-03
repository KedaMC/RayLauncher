/*
 * Zalith Launcher Lite - Recorder controller
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.movtery.zalithlauncher.recorder

import android.content.Context
import android.media.projection.MediaProjection
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * UI-facing wrapper around [GameRecorder].
 *
 * [GameRecorder] keeps its own state in plain volatile fields, which Compose cannot
 * observe. This object mirrors the part the menu needs (recording / paused) as Compose
 * state, so the recorder panel in the game menu always shows what is really going on.
 * Every call goes through here, so the mirrored state cannot drift from the real one.
 */
object RecorderController {
    var isActive by mutableStateOf(false)
        private set
    var isPaused by mutableStateOf(false)
        private set

    /**
     * @param projection when given, game audio is recorded as well (see [RecorderService]).
     * @return true if recording really started
     */
    fun start(context: Context, projection: MediaProjection? = null): Boolean {
        if (isActive) return false
        val started = GameRecorder.start(context, RecorderConfig(), projection)
        isActive = started
        isPaused = false
        return started
    }

    fun pause() {
        if (!isActive || isPaused) return
        GameRecorder.pause()
        isPaused = true
    }

    fun resume() {
        if (!isActive || !isPaused) return
        GameRecorder.resume()
        isPaused = false
    }

    fun stop(context: Context) {
        // Safe to call when nothing is recording; GameRecorder ignores it in that case.
        GameRecorder.stop(context)
        isActive = false
        isPaused = false
        RecorderService.stop(context.applicationContext)
    }
}
