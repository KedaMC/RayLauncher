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

package com.movtery.zalithlauncher.music

import android.content.Context
import com.movtery.zalithlauncher.context.GlobalContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * The one music state of the launcher. It serves the local player,
 * the Music Card only talks to this object.
 *
 * Nothing runs while no Music Card is on the screen: the card calls [acquire] / [release] around its
 * composition and [onStart] / [onStop] around the app lifecycle. Every update is callback driven.
 * All functions must be called on the main thread.
 */
object MusicManager {
    private val _state = MutableStateFlow(MusicState())
    val state: StateFlow<MusicState> = _state.asStateFlow()

    private var users = 0
    private var started = false
    private var scope: CoroutineScope? = null

    private var local: LocalMusicSource? = null
    private val appContext: Context get() = GlobalContext.applicationContext

    /** A Music Card entered the composition */
    fun acquire() {
        users++
        if (users != 1) return
        val newScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        scope = newScope
        local = LocalMusicSource(appContext, ::publish)
        newScope.launch {
            LocalMusicLibrary.tracks.collect { tracks ->
                local?.setTracks(tracks)
                publish()
            }
        }
    }

    /** A Music Card left the composition */
    fun release() {
        if (users == 0) return
        users--
        if (users != 0) return
        started = false
        scope?.cancel()
        scope = null
        local?.release()
        local = null
        AudioSpectrumBus.clear()
        _state.value = MusicState()
    }

    /** The app is on the screen: start listening */
    fun onStart() {
        if (users == 0 || started) return
        started = true
        local?.setAnalysisEnabled(true)
        publish()
    }

    /** The app left the screen: stop everything that costs power, local playback is paused */
    fun onStop() {
        if (!started) return
        started = false
        local?.pause()
        local?.setAnalysisEnabled(false)
        AudioSpectrumBus.clear()
        publish()
    }

    private fun publish() {
        if (users == 0) return
        _state.value = local?.snapshot() ?: return
    }

    fun playPause() {
        local?.playPause()
    }

    fun next() {
        local?.next()
    }

    fun previous() {
        local?.previous()
    }

    fun seekTo(positionMs: Long) {
        local?.seekTo(positionMs)
    }

    fun toggleRepeat() {
        local?.toggleRepeat()
    }
}
