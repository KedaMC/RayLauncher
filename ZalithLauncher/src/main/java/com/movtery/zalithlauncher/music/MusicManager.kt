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

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.runtime.snapshotFlow
import androidx.core.content.ContextCompat
import com.movtery.zalithlauncher.context.GlobalContext
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.setting.enums.MusicSourceType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * The one music state of the launcher. It serves the local player and the music of other apps,
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
    private var external: ExternalMusicSource? = null
    private var capture: ExternalSpectrumCapture? = null

    private var source = MusicSourceType.LOCAL

    private val appContext: Context get() = GlobalContext.applicationContext

    /** A Music Card entered the composition */
    fun acquire() {
        users++
        if (users != 1) return
        val newScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        scope = newScope
        local = LocalMusicSource(appContext, ::publish)
        external = ExternalMusicSource(appContext, ::publish)
        capture = ExternalSpectrumCapture()
        source = AllSettings.musicCardSource.getValue()

        newScope.launch {
            snapshotFlow { AllSettings.musicCardSource.state }
                .distinctUntilChanged()
                .collect { changeSource(it) }
        }
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
        external?.stop()
        capture?.stop()
        local = null
        external = null
        capture = null
        AudioSpectrumBus.clear()
        _state.value = MusicState()
    }

    /** The app is on the screen: start listening */
    fun onStart() {
        if (users == 0 || started) return
        started = true
        local?.setAnalysisEnabled(true)
        if (source == MusicSourceType.BACKGROUND) external?.start()
        publish()
    }

    /** The app left the screen: stop everything that costs power, local playback is paused */
    fun onStop() {
        if (!started) return
        started = false
        local?.pause()
        local?.setAnalysisEnabled(false)
        external?.stop()
        capture?.stop()
        AudioSpectrumBus.clear()
        publish()
    }

    /** Re-reads the permissions, used when the user comes back from the system settings */
    fun refresh() {
        if (users == 0) return
        if (started && source == MusicSourceType.BACKGROUND) external?.refresh()
        publish()
    }

    private fun changeSource(newSource: MusicSourceType) {
        if (newSource == source && _state.value.source == newSource) return
        source = newSource
        AudioSpectrumBus.clear()
        when (newSource) {
            MusicSourceType.LOCAL -> {
                external?.stop()
                capture?.stop()
            }
            MusicSourceType.BACKGROUND -> {
                local?.pause()
                if (started) external?.start()
            }
        }
        publish()
    }

    private fun publish() {
        if (users == 0) return
        val snapshot = when (source) {
            MusicSourceType.LOCAL -> local?.snapshot()
            MusicSourceType.BACKGROUND -> external?.snapshot()
        } ?: return
        _state.value = snapshot
        updateCapture(snapshot)
    }

    /**
     * The system visualizer is only needed for the music of other apps, only while it plays,
     * and only when the RECORD_AUDIO permission was granted.
     */
    private fun updateCapture(snapshot: MusicState) {
        val capture = capture ?: return
        val wanted = started &&
                source == MusicSourceType.BACKGROUND &&
                snapshot.status == MusicStatus.READY &&
                snapshot.isPlaying &&
                hasRecordAudioPermission(appContext)
        if (wanted) {
            capture.start()
        } else if (capture.isRunning) {
            capture.stop()
            AudioSpectrumBus.clear()
        }
    }

    fun playPause() {
        when (source) {
            MusicSourceType.LOCAL -> local?.playPause()
            MusicSourceType.BACKGROUND -> external?.playPause()
        }
    }

    fun next() {
        when (source) {
            MusicSourceType.LOCAL -> local?.next()
            MusicSourceType.BACKGROUND -> external?.next()
        }
    }

    fun previous() {
        when (source) {
            MusicSourceType.LOCAL -> local?.previous()
            MusicSourceType.BACKGROUND -> external?.previous()
        }
    }

    fun seekTo(positionMs: Long) {
        when (source) {
            MusicSourceType.LOCAL -> local?.seekTo(positionMs)
            MusicSourceType.BACKGROUND -> external?.seekTo(positionMs)
        }
    }

    fun toggleRepeat() {
        when (source) {
            MusicSourceType.LOCAL -> local?.toggleRepeat()
            MusicSourceType.BACKGROUND -> external?.toggleRepeat()
        }
    }

    /** Opens the app that plays the music of the background source */
    fun openSourceApp() {
        external?.openSourceApp()
    }
}

fun hasRecordAudioPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
