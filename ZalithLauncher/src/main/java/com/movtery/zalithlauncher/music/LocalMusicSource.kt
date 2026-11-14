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
import android.net.Uri
import android.os.SystemClock
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import com.movtery.zalithlauncher.setting.enums.MusicSourceType
import java.nio.ByteBuffer
import java.nio.ByteOrder

private const val FFT_SIZE = 1024
private const val ANALYSIS_RATE = 30

/**
 * Pass-through audio processor that measures the frequency bands of the audio that is about to be
 * played, so the visualizer follows the real audio without any extra permission.
 * It runs inside the player's audio pipeline: no thread, no polling.
 */
@OptIn(UnstableApi::class)
internal class SpectrumAudioProcessor : BaseAudioProcessor() {
    private val analyzer = SpectrumAnalyzer(FFT_SIZE)
    private val ring = FloatArray(FFT_SIZE)
    private val ordered = FloatArray(FFT_SIZE)
    private var ringPos = 0
    private var filled = 0
    private var sinceAnalysis = 0
    private var hop = 1024
    private var sampleRate = 44100
    private var channels = 2
    private var floatInput = false

    /** Analysis is skipped while nobody looks at the visualizer */
    @Volatile
    var analysisEnabled = true

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        return when (inputAudioFormat.encoding) {
            C.ENCODING_PCM_16BIT, C.ENCODING_PCM_FLOAT -> {
                sampleRate = inputAudioFormat.sampleRate
                channels = inputAudioFormat.channelCount.coerceAtLeast(1)
                floatInput = inputAudioFormat.encoding == C.ENCODING_PCM_FLOAT
                hop = (sampleRate / ANALYSIS_RATE).coerceAtLeast(256)
                inputAudioFormat
            }
            //Other encodings are passed through untouched
            else -> AudioProcessor.AudioFormat.NOT_SET
        }
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val size = inputBuffer.remaining()
        if (size == 0) return
        if (analysisEnabled) analyze(inputBuffer.duplicate().order(ByteOrder.nativeOrder()))
        val output = replaceOutputBuffer(size)
        output.put(inputBuffer)
        output.flip()
    }

    private fun analyze(buffer: ByteBuffer) {
        val bytesPerFrame = (if (floatInput) 4 else 2) * channels
        val frames = buffer.remaining() / bytesPerFrame
        repeat(frames) {
            var sum = 0f
            for (channel in 0 until channels) {
                sum += if (floatInput) buffer.getFloat() else buffer.getShort() / 32768f
            }
            push(sum / channels)
        }
    }

    private fun push(sample: Float) {
        ring[ringPos] = sample
        ringPos = (ringPos + 1) % FFT_SIZE
        if (filled < FFT_SIZE) filled++
        sinceAnalysis++
        if (sinceAnalysis >= hop && filled == FFT_SIZE) {
            sinceAnalysis = 0
            var index = ringPos
            for (i in 0 until FFT_SIZE) {
                ordered[i] = ring[index]
                index = (index + 1) % FFT_SIZE
            }
            val levels = FloatArray(AudioSpectrumBus.BANDS)
            analyzer.analyze(ordered, sampleRate, levels)
            AudioSpectrumBus.publish(levels)
        }
    }
}

/**
 * Local music: the imported tracks are played by a Media3 ExoPlayer as one playlist.
 * Everything here runs on the main thread. The player is created on the first playback request
 * and released with [release].
 */
@OptIn(UnstableApi::class)
internal class LocalMusicSource(
    private val context: Context,
    private val onChanged: () -> Unit
) {
    private var player: ExoPlayer? = null
    private val processor = SpectrumAudioProcessor()
    private var tracks: List<LocalTrack> = emptyList()
    private var tracksById: Map<String, LocalTrack> = emptyMap()

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            onChanged()
        }
    }

    fun setAnalysisEnabled(enabled: Boolean) {
        processor.analysisEnabled = enabled
    }

    fun setTracks(list: List<LocalTrack>) {
        tracks = list
        tracksById = list.associateBy { it.id }
        player?.let { applyPlaylist(it) }
    }

    private fun createPlayer(): ExoPlayer {
        val renderersFactory = object : DefaultRenderersFactory(context) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioOutputPlaybackParams: Boolean
            ): AudioSink? {
                return DefaultAudioSink.Builder(context)
                    .setEnableFloatOutput(enableFloatOutput)
                    .setEnableAudioOutputPlaybackParameters(enableAudioOutputPlaybackParams)
                    .setAudioProcessors(arrayOf<AudioProcessor>(processor))
                    .build()
            }
        }.setEnableDecoderFallback(true)

        return ExoPlayer.Builder(context, renderersFactory).build().also { created ->
            //Audio session: take audio focus, pause when it is lost or when headphones are unplugged
            created.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true
            )
            created.setHandleAudioBecomingNoisy(true)
            created.repeatMode = Player.REPEAT_MODE_OFF
            created.addListener(listener)
            applyPlaylist(created)
        }
    }

    private fun ensurePlayer(): ExoPlayer = player ?: createPlayer().also { player = it }

    /** Rebuilds the playlist when the library changed, keeping the current track when it still exists */
    private fun applyPlaylist(p: ExoPlayer) {
        val ids = tracks.map { it.id }
        val currentIds = List(p.mediaItemCount) { p.getMediaItemAt(it).mediaId }
        if (ids == currentIds) return

        if (tracks.isEmpty()) {
            p.stop()
            p.clearMediaItems()
            return
        }
        val currentId = p.currentMediaItem?.mediaId
        val index = ids.indexOf(currentId)
        val keep = index >= 0
        val items = tracks.map { track ->
            MediaItem.Builder()
                .setMediaId(track.id)
                .setUri(Uri.parse(track.uri))
                .build()
        }
        p.setMediaItems(items, if (keep) index else 0, if (keep) p.currentPosition else 0L)
        if (!keep) p.playWhenReady = false
        p.prepare()
    }

    fun snapshot(): MusicState {
        val p = player
        val track = p?.currentMediaItem?.mediaId?.let { tracksById[it] } ?: tracks.firstOrNull()
        val base = MusicState(
            source = MusicSourceType.LOCAL,
            status = if (track == null) MusicStatus.LIBRARY_EMPTY else MusicStatus.READY
        )
        if (track == null) return base

        val artwork = LocalMusicLibrary.artFileOf(track)?.let { MusicArtwork.LocalFile(it.absolutePath) }
        val withTrack = base.copy(
            title = track.title,
            artist = track.artist,
            album = track.album,
            artwork = artwork,
            durationMs = track.durationMs,
            canPlayPause = true,
            canPrevious = tracks.size > 1,
            canNext = tracks.size > 1,
            canSeek = true,
            canRepeat = true
        )
        if (p == null) return withTrack

        val duration = p.duration.takeIf { it != C.TIME_UNSET && it > 0 } ?: track.durationMs
        val playing = p.playWhenReady &&
                (p.playbackState == Player.STATE_READY || p.playbackState == Player.STATE_BUFFERING)
        return withTrack.copy(
            isPlaying = playing,
            positionMs = p.currentPosition.coerceAtLeast(0L),
            positionStampMs = SystemClock.elapsedRealtime(),
            playbackSpeed = if (p.isPlaying) 1f else 0f,
            durationMs = duration,
            repeat = when (p.repeatMode) {
                Player.REPEAT_MODE_ALL -> MusicRepeat.ALL
                Player.REPEAT_MODE_ONE -> MusicRepeat.ONE
                else -> MusicRepeat.OFF
            },
            canPrevious = tracks.size > 1 || p.currentPosition > 0L,
            canNext = p.hasNextMediaItem()
        )
    }

    fun playPause() {
        if (tracks.isEmpty()) return
        val p = ensurePlayer()
        if (p.playbackState == Player.STATE_ENDED) {
            p.seekToDefaultPosition(0)
            p.play()
            return
        }
        if (p.playWhenReady && p.playbackState != Player.STATE_IDLE) {
            p.pause()
        } else {
            if (p.playbackState == Player.STATE_IDLE) p.prepare()
            p.play()
        }
    }

    fun next() {
        val p = player ?: return
        if (p.hasNextMediaItem()) p.seekToNextMediaItem()
    }

    fun previous() {
        val p = player ?: return
        p.seekToPrevious()
    }

    fun seekTo(positionMs: Long) {
        player?.seekTo(positionMs.coerceAtLeast(0L))
    }

    /** off -> repeat the whole playlist -> repeat the current track -> off */
    fun toggleRepeat() {
        val p = ensurePlayer()
        p.repeatMode = when (p.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    fun pause() {
        player?.pause()
    }

    fun release() {
        val p = player ?: return
        player = null
        p.removeListener(listener)
        p.release()
    }
}
