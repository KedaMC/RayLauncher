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

package com.movtery.zalithlauncher.startup

import androidx.annotation.OptIn
import androidx.annotation.StringRes
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.setting.AllSettings

/** How the startup video is sized against the screen */
@OptIn(UnstableApi::class)
enum class StartupVideoScale(
    @StringRes val textRes: Int,
    val resizeMode: Int
) {
    /** The whole picture, with black bars when the ratios differ */
    FIT(R.string.startup_video_fit, AspectRatioFrameLayout.RESIZE_MODE_FIT),
    /** Covers the screen, cutting off what does not fit */
    FILL(R.string.startup_video_fill, AspectRatioFrameLayout.RESIZE_MODE_ZOOM),
    /** Covers the screen by distorting the picture */
    STRETCH(R.string.startup_video_stretch, AspectRatioFrameLayout.RESIZE_MODE_FILL)
}

/**
 * How the user placed the startup video.
 * @param zoom percent, applied on top of [scale]
 * @param offsetX horizontal shift in thousandths of the screen width
 * @param offsetY vertical shift in thousandths of the screen height
 */
data class StartupVideoLayout(
    val scale: StartupVideoScale = StartupVideoScale.FIT,
    val zoom: Int = 100,
    val offsetX: Int = 0,
    val offsetY: Int = 0
) {
    fun save() {
        AllSettings.startupVideoScale.save(scale)
        AllSettings.startupVideoZoom.save(zoom)
        AllSettings.startupVideoOffsetX.save(offsetX)
        AllSettings.startupVideoOffsetY.save(offsetY)
    }

    companion object {
        fun load() = StartupVideoLayout(
            scale = AllSettings.startupVideoScale.getValue(),
            zoom = AllSettings.startupVideoZoom.getValue(),
            offsetX = AllSettings.startupVideoOffsetX.getValue(),
            offsetY = AllSettings.startupVideoOffsetY.getValue()
        )
    }
}
