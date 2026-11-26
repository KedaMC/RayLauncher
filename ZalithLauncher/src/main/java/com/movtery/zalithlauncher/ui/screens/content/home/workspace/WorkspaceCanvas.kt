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

import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.movtery.cardgrid.state.CardGridState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/** Width of the viewport edge band that moves the camera while a card is dragged */
private val AutoPanEdge = 56.dp

/** Highest speed of that movement (dp per second) */
private const val AUTO_PAN_MAX_SPEED = 900f

/** The camera is saved once it has been still for this long */
private const val SAVE_DELAY_MS = 300L

/**
 * Free canvas navigation: drag in any direction to move, pinch to zoom between 50% and 100%.
 * Movement is continuous, with momentum and without any snapping. [content] is the card grid.
 */
@Composable
fun WorkspaceCanvas(
    state: CardGridState,
    camera: WorkspaceCamera,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val workspace = HomeWorkspace
    val density = LocalDensity.current
    val width = (workspace.columns * workspace.cellSize).dp
    val height = (workspace.rows * workspace.cellSize).dp
    val contentSize = with(density) { Size(width.toPx(), height.toPx()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(camera) {
        snapshotFlow { camera.scale to camera.offset }.collectLatest {
            delay(SAVE_DELAY_MS)
            camera.save()
        }
    }

    CardDragAutoPan(state = state, camera = camera)

    Box(
        modifier = modifier
            .clipToBounds()
            .onSizeChanged { camera.updateBounds(Size(it.width.toFloat(), it.height.toFloat()), contentSize) }
            .pointerInput(camera, state) {
                var fling: Job? = null
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    fling?.cancel()
                    val velocity = VelocityTracker()
                    var travelled = Offset.Zero
                    var panning = false
                    var pinched = false
                    while (true) {
                        //The cards see every event first: what they consume does not move the canvas
                        val event = awaitPointerEvent()
                        val pressed = event.changes.filter { it.pressed }
                        if (pressed.isEmpty()) break
                        if (state.hasSession) {
                            panning = false
                            pinched = true
                            continue
                        }
                        if (pressed.size > 1) {
                            pinched = true
                            camera.transform(
                                centroid = event.calculateCentroid(useCurrent = false),
                                pan = event.calculatePan(),
                                zoom = event.calculateZoom()
                            )
                            event.changes.forEach { it.consume() }
                            continue
                        }
                        val change = pressed.first()
                        if (change.isConsumed) continue
                        val delta = change.positionChange()
                        if (!panning) {
                            travelled += delta
                            if (travelled.getDistance() <= viewConfiguration.touchSlop) continue
                            panning = true
                        }
                        velocity.addPosition(change.uptimeMillis, camera.offset + delta)
                        camera.panBy(delta)
                        change.consume()
                    }
                    if (panning && !pinched) {
                        val speed = velocity.calculateVelocity()
                        fling = scope.launch {
                            var last = Offset.Zero
                            AnimationState(
                                typeConverter = Offset.VectorConverter,
                                initialValue = Offset.Zero,
                                initialVelocityVector = Offset.VectorConverter.convertToVector(Offset(speed.x, speed.y))
                            ).animateDecay(exponentialDecay()) {
                                val applied = camera.panBy(value - last)
                                last = value
                                if (applied == Offset.Zero) cancelAnimation()
                            }
                        }
                    }
                }
            }
    ) {
        Box(
            modifier = Modifier
                .wrapContentSize(align = AbsoluteAlignment.TopLeft, unbounded = true)
                .graphicsLayer {
                    transformOrigin = TransformOrigin(0f, 0f)
                    scaleX = camera.scale
                    scaleY = camera.scale
                    translationX = camera.offset.x
                    translationY = camera.offset.y
                }
                .requiredSize(width = width, height = height)
        ) {
            content()
        }
    }
}

/** Moves the camera while a dragged or resized card is held near an edge of the viewport */
@Composable
private fun CardDragAutoPan(state: CardGridState, camera: WorkspaceCamera) {
    val density = LocalDensity.current
    LaunchedEffect(state.hasSession, camera) {
        if (!state.hasSession) return@LaunchedEffect
        val edge = with(density) { AutoPanEdge.toPx() }
        val maxSpeed = with(density) { AUTO_PAN_MAX_SPEED.dp.toPx() }
        var lastFrame = 0L
        while (true) {
            val frame = withFrameNanos { it }
            val seconds = if (lastFrame == 0L) 0f else (frame - lastFrame) / 1_000_000_000f
            lastFrame = frame
            val pointer = state.pointerPosition ?: continue
            val onScreen = pointer * camera.scale + camera.offset

            fun speed(position: Float, size: Float): Float = when {
                position < edge -> ((edge - position) / edge).coerceAtMost(1f)
                position > size - edge -> -((position - (size - edge)) / edge).coerceAtMost(1f)
                else -> 0f
            } * maxSpeed

            val wanted = Offset(
                speed(onScreen.x, camera.viewport.width),
                speed(onScreen.y, camera.viewport.height)
            ) * seconds
            if (wanted == Offset.Zero) continue
            val moved = camera.panBy(wanted)
            if (moved != Offset.Zero) state.onSessionPointerMoved(pointer - moved / camera.scale)
        }
    }
}
