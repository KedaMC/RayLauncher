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

import android.graphics.Bitmap
import android.os.SystemClock
import com.movtery.zalithlauncher.setting.enums.MusicSourceType

/** Album artwork of the current track */
sealed interface MusicArtwork {
    /** Decoded bitmap (media session metadata) */
    data class Image(val bitmap: Bitmap) : MusicArtwork

    /** Artwork saved next to the imported local track */
    data class LocalFile(val path: String) : MusicArtwork

    /** Artwork referenced by a content or http(s) uri (media session metadata) */
    data class Remote(val uri: String) : MusicArtwork
}

/** What the Music Card can show right now */
enum class MusicStatus {
    /** A track is available */
    READY,

    /** Local source selected, but no music file was imported yet */
    LIBRARY_EMPTY,

    /** Background source selected, but notification access was not granted */
    ACCESS_REQUIRED,

    /** Background source selected, but no other app has a media session */
    NO_SESSION
}

enum class MusicRepeat { OFF, ALL, ONE }

/**
 * The single music state shared by the local player and by the music of other apps.
 *
 * The playback position is not polled: [positionMs] was valid at [positionStampMs]
 * ([SystemClock.elapsedRealtime]) and [currentPositionMs] extrapolates it with [playbackSpeed].
 */
data class MusicState(
    val source: MusicSourceType = MusicSourceType.LOCAL,
    val status: MusicStatus = MusicStatus.LIBRARY_EMPTY,
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val artwork: MusicArtwork? = null,
    /** Package of the app that plays the music (the launcher itself for local music) */
    val sourcePackage: String? = null,
    val sourceLabel: String? = null,
    val sourceIcon: Bitmap? = null,
    /** Play/pause button shows "pause" */
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val positionStampMs: Long = 0L,
    /** 0 while the position is not moving */
    val playbackSpeed: Float = 0f,
    val durationMs: Long = 0L,
    val repeat: MusicRepeat = MusicRepeat.OFF,
    val canPlayPause: Boolean = false,
    val canPrevious: Boolean = false,
    val canNext: Boolean = false,
    val canSeek: Boolean = false,
    val canRepeat: Boolean = false
) {
    val repeatActive: Boolean get() = repeat != MusicRepeat.OFF

    fun currentPositionMs(now: Long = SystemClock.elapsedRealtime()): Long {
        val elapsed = if (playbackSpeed > 0f) ((now - positionStampMs) * playbackSpeed).toLong() else 0L
        val position = (positionMs + elapsed).coerceAtLeast(0L)
        return if (durationMs > 0L) position.coerceAtMost(durationMs) else position
    }
}
