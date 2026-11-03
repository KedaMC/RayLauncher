/*
 * Zalith Launcher Lite - Recorder
 * Copyright (C) 2026 KedaMC contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.movtery.zalithlauncher.recorder

/**
 * User-selected output size. Dimensions describe a 16:9 output canvas;
 * the capture implementation must preserve aspect ratio when the source
 * display is not 16:9.
 */
enum class RecorderResolution(
    val width: Int,
    val height: Int,
    val label: String
) {
    P480(854, 480, "480p"),
    P720(1280, 720, "720p"),
    P1080(1920, 1080, "1080p")
}

/**
 * Requested output frame rate. The recorder must validate this against
 * the active display refresh rate and the device's encoder capabilities
 * before starting a recording.
 */
enum class RecorderFrameRate(val fps: Int) {
    FPS_30(30),
    FPS_60(60),
    FPS_90(90),
    FPS_120(120);

    val label: String
        get() = "${fps} FPS"
}

/**
 * Audio capture modes offered in the first version.
 *
 * Internal playback capture is subject to Android's playback-capture
 * policy and may not be available for every audio source.
 */
enum class RecorderAudioMode {
    INTERNAL_AUDIO,
    INTERNAL_AUDIO_AND_MICROPHONE
}

/**
 * Preferences for the next recording. This is deliberately a small model;
 * encoder-specific bitrate and codec options are out of scope for v1.
 */
data class RecorderConfig(
    val resolution: RecorderResolution = RecorderResolution.P720,
    val frameRate: RecorderFrameRate = RecorderFrameRate.FPS_30,
    val audioMode: RecorderAudioMode = RecorderAudioMode.INTERNAL_AUDIO
)
