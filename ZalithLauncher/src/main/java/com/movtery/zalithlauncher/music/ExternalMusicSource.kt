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

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.service.notification.NotificationListenerService
import androidx.core.app.NotificationManagerCompat
import com.movtery.zalithlauncher.setting.enums.MusicSourceType
import com.movtery.zalithlauncher.utils.logging.Logger

private const val TAG = "ExternalMusicSource"
private const val ICON_SIZE = 96

/**
 * Empty notification listener. Android only hands the media sessions of other apps to apps that
 * were granted notification access, and this service is the component that access is granted to.
 * The notifications themselves are never read.
 */
class MusicNotificationListener : NotificationListenerService()

/** Whether the user granted notification access to the launcher */
fun hasMusicNotificationAccess(context: Context): Boolean =
    NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

/**
 * Music played by other apps, read from their media sessions (no polling: everything is callback driven).
 * Everything here runs on the main thread.
 */
internal class ExternalMusicSource(
    private val context: Context,
    private val onChanged: () -> Unit
) {
    private val sessionManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
    private val listenerComponent = ComponentName(context, MusicNotificationListener::class.java)
    private val packageManager = context.packageManager

    private var started = false
    private var accessGranted = false
    private var controller: MediaController? = null
    private val iconCache = HashMap<String, Pair<String, Bitmap?>>()

    /** The repeat custom action of the source app has no readable state, so the toggle is tracked here */
    private var repeatToggled = false

    private val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { list ->
        selectController(list.orEmpty())
    }

    private val callback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) = onChanged()
        override fun onMetadataChanged(metadata: MediaMetadata?) = onChanged()
        override fun onSessionDestroyed() {
            refresh()
        }
    }

    fun start() {
        if (started) return
        started = true
        accessGranted = hasMusicNotificationAccess(context)
        if (!accessGranted) return
        try {
            sessionManager.addOnActiveSessionsChangedListener(sessionsListener, listenerComponent)
            selectController(sessionManager.getActiveSessions(listenerComponent))
        } catch (e: SecurityException) {
            Logger.warning(TAG, "Notification access was revoked.", e)
            accessGranted = false
        }
    }

    /** Re-reads the access state and the sessions, used when the card comes back to the screen */
    fun refresh() {
        val granted = hasMusicNotificationAccess(context)
        if (granted != accessGranted || (started && controller == null)) {
            stop()
            start()
        }
        onChanged()
    }

    fun stop() {
        if (!started) return
        started = false
        runCatching { sessionManager.removeOnActiveSessionsChangedListener(sessionsListener) }
        controller?.unregisterCallback(callback)
        controller = null
    }

    private fun selectController(list: List<MediaController>) {
        //The list is ordered by priority: a playing session wins, otherwise the most recent one
        val chosen = list.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
            ?: list.firstOrNull { it.metadata != null }
            ?: list.firstOrNull()
        if (chosen?.sessionToken != controller?.sessionToken) {
            controller?.unregisterCallback(callback)
            controller = chosen
            chosen?.registerCallback(callback)
            repeatToggled = false
        }
        onChanged()
    }

    fun snapshot(): MusicState {
        val base = MusicState(source = MusicSourceType.BACKGROUND)
        if (!accessGranted) return base.copy(status = MusicStatus.ACCESS_REQUIRED)
        val c = controller ?: return base.copy(status = MusicStatus.NO_SESSION)

        val metadata = c.metadata
        val playback = c.playbackState
        val packageName = c.packageName
        val (label, icon) = appIdentity(packageName)

        val actions = playback?.actions ?: 0L
        val state = playback?.state ?: PlaybackState.STATE_NONE
        val playing = state == PlaybackState.STATE_PLAYING ||
                state == PlaybackState.STATE_BUFFERING ||
                state == PlaybackState.STATE_FAST_FORWARDING ||
                state == PlaybackState.STATE_REWINDING

        val title = metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_TITLE).orEmpty()
        val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE).orEmpty()
        val album = metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM).orEmpty()

        val repeatAction = findRepeatAction(playback)
        val speed = playback?.playbackSpeed ?: 0f
        val moving = state == PlaybackState.STATE_PLAYING

        return base.copy(
            status = MusicStatus.READY,
            title = title,
            artist = artist,
            album = album,
            artwork = artworkOf(metadata),
            sourcePackage = packageName,
            sourceLabel = label,
            sourceIcon = icon,
            isPlaying = playing,
            positionMs = playback?.position?.coerceAtLeast(0L) ?: 0L,
            positionStampMs = playback?.lastPositionUpdateTime?.takeIf { it > 0L }
                ?: android.os.SystemClock.elapsedRealtime(),
            playbackSpeed = if (moving) (if (speed > 0f) speed else 1f) else 0f,
            durationMs = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION)?.coerceAtLeast(0L) ?: 0L,
            repeat = if (repeatToggled) MusicRepeat.ALL else MusicRepeat.OFF,
            canPlayPause = actions and (PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE or
                    PlaybackState.ACTION_PLAY_PAUSE) != 0L,
            canPrevious = actions and PlaybackState.ACTION_SKIP_TO_PREVIOUS != 0L,
            canNext = actions and PlaybackState.ACTION_SKIP_TO_NEXT != 0L,
            canSeek = actions and PlaybackState.ACTION_SEEK_TO != 0L,
            canRepeat = repeatAction != null
        )
    }

    private fun findRepeatAction(playback: PlaybackState?): PlaybackState.CustomAction? =
        playback?.customActions?.firstOrNull { action ->
            action.action.contains("repeat", ignoreCase = true) ||
                    action.action.contains("loop", ignoreCase = true)
        }

    private fun artworkOf(metadata: MediaMetadata?): MusicArtwork? {
        if (metadata == null) return null
        val bitmap = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: metadata.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
        if (bitmap != null) return MusicArtwork.Image(bitmap)
        val uri = metadata.getString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI)
            ?: metadata.getString(MediaMetadata.METADATA_KEY_ART_URI)
            ?: metadata.getString(MediaMetadata.METADATA_KEY_DISPLAY_ICON_URI)
        return uri?.takeIf { it.isNotBlank() }?.let { MusicArtwork.Remote(it) }
    }

    /** Label and icon of the app that owns the session, read from the PackageManager and cached */
    private fun appIdentity(packageName: String): Pair<String, Bitmap?> =
        iconCache.getOrPut(packageName) {
            runCatching {
                val info = packageManager.getApplicationInfo(packageName, 0)
                packageManager.getApplicationLabel(info).toString() to
                        packageManager.getApplicationIcon(info).toBitmap(ICON_SIZE)
            }.getOrElse {
                Logger.warning(TAG, "Failed to resolve the app icon of $packageName.", it)
                packageName to null
            }
        }

    fun playPause() {
        val c = controller ?: return
        val controls = c.transportControls
        val state = c.playbackState?.state
        if (state == PlaybackState.STATE_PLAYING || state == PlaybackState.STATE_BUFFERING) {
            controls.pause()
        } else {
            controls.play()
        }
    }

    fun next() {
        controller?.transportControls?.skipToNext()
    }

    fun previous() {
        controller?.transportControls?.skipToPrevious()
    }

    fun seekTo(positionMs: Long) {
        controller?.transportControls?.seekTo(positionMs)
    }

    fun toggleRepeat() {
        val c = controller ?: return
        val action = findRepeatAction(c.playbackState) ?: return
        c.transportControls.sendCustomAction(action, null)
        repeatToggled = !repeatToggled
        onChanged()
    }

    fun openSourceApp() {
        val packageName = controller?.packageName ?: return
        val intent = packageManager.getLaunchIntentForPackage(packageName) ?: return
        runCatching { context.startActivity(intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }
}

private fun Drawable.toBitmap(size: Int): Bitmap {
    if (this is BitmapDrawable && bitmap != null && bitmap.width <= size * 2) return bitmap
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    setBounds(0, 0, size, size)
    draw(canvas)
    return bitmap
}
