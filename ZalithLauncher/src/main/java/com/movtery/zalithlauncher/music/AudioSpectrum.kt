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

import android.media.audiofx.Visualizer
import com.movtery.zalithlauncher.utils.logging.Logger
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

private const val TAG = "AudioSpectrum"

/**
 * Latest frequency band levels (0..1) measured from the music that is playing.
 * Producers (the local player's audio pipeline, or the system visualizer for other apps) publish here,
 * the Music Card reads it once per frame while it is on screen. Nothing is polled when nobody reads.
 */
object AudioSpectrumBus {
    /** Number of frequency bands, low to high */
    const val BANDS = 11

    /** Levels older than this are treated as "no live data" */
    private const val FRESH_NANOS = 400_000_000L

    @Volatile
    private var levels = FloatArray(BANDS)

    @Volatile
    private var lastPublishNanos = 0L

    fun publish(newLevels: FloatArray) {
        levels = newLevels
        lastPublishNanos = System.nanoTime()
    }

    /**
     * Copies the latest levels into [out].
     * @return false when there is no fresh data (nothing playing, or no data source available)
     */
    fun read(out: FloatArray): Boolean {
        val fresh = System.nanoTime() - lastPublishNanos < FRESH_NANOS
        if (!fresh) return false
        val src = levels
        System.arraycopy(src, 0, out, 0, min(out.size, src.size))
        return true
    }

    fun clear() {
        levels = FloatArray(BANDS)
        lastPublishNanos = 0L
    }
}

/** Frequency bands shared by every producer: log spaced between 60 Hz and 12 kHz */
internal object SpectrumBands {
    private const val LOW_HZ = 60f
    private const val HIGH_HZ = 12000f

    /** Band edges in Hz, size [AudioSpectrumBus.BANDS] + 1 */
    val edgesHz: FloatArray = FloatArray(AudioSpectrumBus.BANDS + 1) { i ->
        LOW_HZ * (HIGH_HZ / LOW_HZ).pow(i.toFloat() / AudioSpectrumBus.BANDS)
    }

    /** Amplitude (0..1, full scale = 1) to a 0..1 display level, with a small tilt for the highs */
    fun level(amplitude: Float, band: Int): Float {
        val db = 20f * log10(amplitude + 1e-6f)
        return ((db + 62f + band * 2.2f) / 50f).coerceIn(0f, 1f)
    }
}

/**
 * Small radix-2 FFT spectrum analyzer over mono samples in -1..1.
 */
internal class SpectrumAnalyzer(private val size: Int = 1024) {
    private val window = FloatArray(size) { i -> (0.5 - 0.5 * cos(2.0 * PI * i / (size - 1))).toFloat() }
    private val re = FloatArray(size)
    private val im = FloatArray(size)
    private val cosTable = FloatArray(size / 2) { i -> cos(2.0 * PI * i / size).toFloat() }
    private val sinTable = FloatArray(size / 2) { i -> -sin(2.0 * PI * i / size).toFloat() }
    private val bitReverse = IntArray(size).also { table ->
        val bits = Integer.numberOfTrailingZeros(size)
        for (i in 0 until size) table[i] = Integer.reverse(i) ushr (32 - bits)
    }

    /**
     * @param samples [size] mono samples, oldest first
     * @param out band levels, size [AudioSpectrumBus.BANDS]
     */
    fun analyze(samples: FloatArray, sampleRate: Int, out: FloatArray) {
        for (i in 0 until size) {
            re[bitReverse[i]] = samples[i] * window[i]
        }
        java.util.Arrays.fill(im, 0f)

        var half = 1
        while (half < size) {
            val step = size / (half * 2)
            var start = 0
            while (start < size) {
                var k = 0
                for (j in start until start + half) {
                    val wr = cosTable[k]
                    val wi = sinTable[k]
                    val tr = wr * re[j + half] - wi * im[j + half]
                    val ti = wr * im[j + half] + wi * re[j + half]
                    re[j + half] = re[j] - tr
                    im[j + half] = im[j] - ti
                    re[j] += tr
                    im[j] += ti
                    k += step
                }
                start += half * 2
            }
            half *= 2
        }

        val binHz = sampleRate.toFloat() / size
        val maxBin = size / 2 - 1
        for (band in 0 until AudioSpectrumBus.BANDS) {
            val from = max(1, (SpectrumBands.edgesHz[band] / binHz).toInt())
            val to = min(maxBin, max(from, (SpectrumBands.edgesHz[band + 1] / binHz).toInt()))
            var peak = 0f
            for (bin in from..to) {
                val magnitude = hypot(re[bin], im[bin])
                if (magnitude > peak) peak = magnitude
            }
            //Hann window: a full scale sine gives size / 4
            out[band] = SpectrumBands.level(4f * peak / size, band)
        }
    }
}

/**
 * Reads the frequency data of the audio that other apps play through the system output mix,
 * with the system [Visualizer]. Needs the RECORD_AUDIO permission; without it (or when the device
 * refuses) [start] fails and the card keeps its static pattern.
 */
internal class ExternalSpectrumCapture {
    private var visualizer: Visualizer? = null
    private val levels = FloatArray(AudioSpectrumBus.BANDS)

    val isRunning: Boolean get() = visualizer != null

    fun start(): Boolean {
        if (visualizer != null) return true
        return try {
            val created = Visualizer(0)
            val range = Visualizer.getCaptureSizeRange()
            val captureSize = 512.coerceIn(range[0], range[1])
            created.setEnabled(false)
            created.setCaptureSize(captureSize)
            created.setDataCaptureListener(
                object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(visualizer: Visualizer?, waveform: ByteArray?, samplingRate: Int) {}

                    override fun onFftDataCapture(visualizer: Visualizer?, fft: ByteArray?, samplingRate: Int) {
                        if (fft != null) handleFft(fft, samplingRate)
                    }
                },
                Visualizer.getMaxCaptureRate() / 2,
                false,
                true
            )
            created.setEnabled(true)
            visualizer = created
            true
        } catch (e: Throwable) {
            Logger.warning(TAG, "The system visualizer is not available.", e)
            stop()
            false
        }
    }

    fun stop() {
        val current = visualizer ?: return
        visualizer = null
        runCatching { current.setEnabled(false) }
        runCatching { current.setDataCaptureListener(null, 0, false, false) }
        runCatching { current.release() }
    }

    private fun handleFft(fft: ByteArray, samplingRateMilliHz: Int) {
        val n = fft.size
        if (n < 16) return
        val binHz = (samplingRateMilliHz / 1000f) / n
        if (binHz <= 0f) return
        val maxBin = n / 2 - 1
        for (band in 0 until AudioSpectrumBus.BANDS) {
            val from = max(1, (SpectrumBands.edgesHz[band] / binHz).toInt())
            val to = min(maxBin, max(from, (SpectrumBands.edgesHz[band + 1] / binHz).toInt()))
            var peak = 0f
            for (bin in from..to) {
                val magnitude = hypot(fft[2 * bin].toFloat(), fft[2 * bin + 1].toFloat())
                if (magnitude > peak) peak = magnitude
            }
            //The system FFT is 8 bit: roughly 90 is a very loud bin
            val db = 20f * log10(peak / 90f + 1e-4f)
            levels[band] = ((db + 36f + band * 1.5f) / 36f).coerceIn(0f, 1f)
        }
        AudioSpectrumBus.publish(levels.copyOf())
    }
}
