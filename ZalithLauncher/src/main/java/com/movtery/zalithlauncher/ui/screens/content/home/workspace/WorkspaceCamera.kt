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

package com.movtery.zalithlauncher.ui.screens.content.home.workspace

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import com.tencent.mmkv.MMKV

const val MIN_WORKSPACE_ZOOM = 0.5f
const val MAX_WORKSPACE_ZOOM = 1f

/** The camera of the home workspace, for cards that depend on where they are on the screen */
val LocalWorkspaceCamera = staticCompositionLocalOf<WorkspaceCamera?> { null }

/**
 * The view onto the workspace: a canvas point p is shown at p * [scale] + [offset].
 * The position is persisted in dp, so it survives restarts.
 * @param density px per dp
 */
@Stable
class WorkspaceCamera(private val density: Float) {
    private val mmkv: MMKV by lazy { MMKV.mmkvWithID("home_workspace_camera") }

    var scale by mutableFloatStateOf(
        mmkv.decodeFloat(KEY_SCALE, MAX_WORKSPACE_ZOOM).coerceIn(MIN_WORKSPACE_ZOOM, MAX_WORKSPACE_ZOOM)
    )
        private set

    var offset by mutableStateOf(
        Offset(mmkv.decodeFloat(KEY_X, 0f) * density, mmkv.decodeFloat(KEY_Y, 0f) * density)
    )
        private set

    var viewport by mutableStateOf(Size.Zero)
        private set

    private var content = Size.Zero

    /** True while the camera is at its starting view: top left corner, 100% */
    val isHome: Boolean get() = scale == MAX_WORKSPACE_ZOOM && offset == Offset.Zero

    fun updateBounds(viewport: Size, content: Size) {
        this.viewport = viewport
        this.content = content
        offset = clamp(offset, scale)
    }

    /** @return the distance that was actually moved */
    fun panBy(delta: Offset): Offset {
        val before = offset
        offset = clamp(before + delta, scale)
        return offset - before
    }

    /** Pinch: zooms around [centroid] and moves by [pan] */
    fun transform(centroid: Offset, pan: Offset, zoom: Float) {
        val newScale = (scale * zoom).coerceIn(MIN_WORKSPACE_ZOOM, MAX_WORKSPACE_ZOOM)
        val target = centroid - (centroid - offset) * (newScale / scale) + pan
        scale = newScale
        offset = clamp(target, newScale)
    }

    fun set(scale: Float, offset: Offset) {
        this.scale = scale.coerceIn(MIN_WORKSPACE_ZOOM, MAX_WORKSPACE_ZOOM)
        this.offset = clamp(offset, this.scale)
    }

    /** Where a rect of the canvas currently is inside the viewport */
    fun toViewport(rect: Rect): Rect = Rect(
        offset = rect.topLeft * scale + offset,
        size = rect.size * scale
    )

    fun save() {
        mmkv.encode(KEY_SCALE, scale)
        mmkv.encode(KEY_X, offset.x / density)
        mmkv.encode(KEY_Y, offset.y / density)
    }

    /**
     * The canvas can be moved until its edge meets the viewport edge. A canvas smaller than the
     * viewport can be moved around inside of it.
     */
    private fun clamp(target: Offset, scale: Float): Offset {
        if (viewport == Size.Zero || content == Size.Zero) return target
        fun axis(value: Float, view: Float, size: Float): Float {
            val free = view - size * scale
            return value.coerceIn(minOf(0f, free), maxOf(0f, free))
        }
        return Offset(
            axis(target.x, viewport.width, content.width),
            axis(target.y, viewport.height, content.height)
        )
    }

    private companion object {
        const val KEY_SCALE = "scale"
        const val KEY_X = "x"
        const val KEY_Y = "y"
    }
}
