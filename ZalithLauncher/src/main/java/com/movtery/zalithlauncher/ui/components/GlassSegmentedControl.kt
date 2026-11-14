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

package com.movtery.zalithlauncher.ui.components

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.lerp
import com.styropyr0.prismal.PrismalBackdrop
import com.styropyr0.prismal.PrismalGlass
import com.styropyr0.prismal.PrismalGlassPipeline
import com.styropyr0.prismal.depth.PrismalDepthInset
import com.styropyr0.prismal.depth.PrismalDepthShadow
import com.styropyr0.prismal.drawPrismalGlass
import com.styropyr0.prismal.effects.prismalLens
import com.styropyr0.prismal.interactive.LocalPrismalHapticFeedback
import com.styropyr0.prismal.interactive.PrismalSpringMotion
import com.styropyr0.prismal.shapes.PrismalCapsule
import com.styropyr0.prismal.shapes.PrismalRoundedCornerStyle
import com.styropyr0.prismal.sources.prismalGlassLayer
import com.styropyr0.prismal.sources.rememberPrismalGlassLayer
import com.styropyr0.prismal.sources.rememberPrismalMergedSource
import com.styropyr0.prismal.specular.PrismalSpecular
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sqrt

//Based on PrismalSegmentedControl from PrismalAGSL v1.0.7 (MIT License, Copyright (c) 2026 Saurav Sajeev).
//Changes: segments take composable content (icons) instead of text, and that content is recorded into the
//layer the droplet samples, so the lens refracts it. No track and no resting thumb are drawn, and the
//lens always refracts the page at the rim, and reaches the segment content only while the droplet is
//touched, dragged or moving to another segment.

private val SegmentedHorizontalInset = 2.dp
private const val SegmentedDropletPressedScale = 1.14f
private const val SegmentedDropletMaxAspect = 2.2f
private const val SegmentedRefractionBand = 0.58f
private const val SegmentedRestingRefractionBand = 0.22f

/** Droplet stretch (a smoothed speed) at which a moving droplet has the full refraction band. */
private const val SegmentedMotionLensStretch = 0.08f
private const val SegmentedRefractionAmount = 0.8f
private const val SegmentedStretchReferenceSpeedDp = 1600f
private const val SegmentedStretchWidth = 0.3f
private const val SegmentedStretchHeight = 0.16f
private const val SegmentedStretchDampingRatio = 0.4f
private const val SegmentedStretchStiffness = 380f
private val SegmentedOverdragMax = 5.dp
private const val SegmentedOverdragRange = 0f
private const val SegmentedOverdragStretchWidth = 0.12f
private const val SegmentedOverdragStretchHeight = 0.06f
private val SegmentedOverdragReleaseSpring = spring<Float>(dampingRatio = 0.45f, stiffness = 500f)
private val SegmentedDropletShape = PrismalCapsule(PrismalRoundedCornerStyle.Circular)

/**
 * Segmented picker with a clear liquid-glass droplet that is always visible over the selected segment.
 * Tap a segment to move the droplet, or drag it across segments; it snaps to the nearest one on release.
 * The droplet stretches with its speed, wobbles when it stops and rubber-bands past the first and last
 * segments.
 *
 * @param labels Accessibility name of each segment (not drawn).
 * @param selectedIndex Index of the selected segment.
 * @param onSelected Called with the new index after a tap or a drag ends on another segment.
 * @param backdrop Source sampled through the lens.
 * @param height Overall control height; the droplet is as tall as the control.
 * @param segment Content of segment `index`, centered in its cell. It is drawn on screen and also recorded
 *   for the lens, so the droplet refracts it.
 */
@Composable
fun GlassSegmentedControl(
    labels: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    backdrop: PrismalBackdrop,
    modifier: Modifier = Modifier,
    height: Dp = 48.dp,
    chromaticAberration: Float = 0.8f,
    specular: (() -> PrismalSpecular?)? = { PrismalSpecular.Default },
    hapticFeedback: Boolean = LocalPrismalHapticFeedback.current,
    segment: @Composable BoxScope.(index: Int) -> Unit
) {
    val segmentCount = labels.size
    if (segmentCount == 0) return

    val density = LocalDensity.current
    val view = LocalView.current
    val hapticsOn by rememberUpdatedState(hapticFeedback)
    val labelsLayer = rememberPrismalGlassLayer()
    labelsLayer.readSamplingState()

    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(height),
        contentAlignment = Alignment.CenterStart
    ) {
        val insetPx = with(density) { SegmentedHorizontalInset.toPx() }
        val segmentWidthPx = (constraints.maxWidth - insetPx * 2f) / segmentCount
        val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
        val animationScope = rememberCoroutineScope()
        val onSelectedState = rememberUpdatedState<(Int) -> Unit> {
            if (hapticsOn) {
                runCatching {
                    view.performHapticFeedback(
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                            HapticFeedbackConstants.SEGMENT_TICK
                        } else {
                            HapticFeedbackConstants.CLOCK_TICK
                        }
                    )
                }
            }
            onSelected(it)
        }
        val currentSelectedIndex by rememberUpdatedState(selectedIndex)
        val overdragMaxPx = with(density) { SegmentedOverdragMax.toPx() }
        val overdrag = remember { Animatable(0f) }
        val dropletStretch = remember { mutableFloatStateOf(0f) }

        val lensMotion = remember(animationScope, segmentCount, segmentWidthPx, isLtr, overdragMaxPx) {
            val maxValue = (segmentCount - 1).toFloat()
            val direction = if (isLtr) 1f else -1f
            var rawValue = 0f
            PrismalSpringMotion(
                animationScope = animationScope,
                initialValue = selectedIndex.coerceIn(0, segmentCount - 1).toFloat(),
                valueRange = 0f..maxValue,
                visibilityThreshold = 0.001f,
                initialScale = 1f,
                pressedScale = SegmentedDropletPressedScale,
                onDragStarted = { rawValue = targetValue },
                onDragStopped = {
                    val targetIndex = targetValue.fastRoundToInt().coerceIn(0, segmentCount - 1)
                    animateToValue(targetIndex.toFloat())
                    animationScope.launch { overdrag.animateTo(0f, SegmentedOverdragReleaseSpring) }
                    if (targetIndex != currentSelectedIndex) {
                        onSelectedState.value(targetIndex)
                    }
                },
                onDrag = { _, dragAmount ->
                    if (segmentWidthPx <= 0f) return@PrismalSpringMotion
                    rawValue = (rawValue + dragAmount.x / segmentWidthPx * direction)
                        .fastCoerceIn(-SegmentedOverdragRange, maxValue + SegmentedOverdragRange)
                    val clamped = rawValue.fastCoerceIn(0f, maxValue)
                    updateValue(clamped)
                    val excessPx = (rawValue - clamped) * segmentWidthPx * direction
                    val resisted = overdragMaxPx * (1f - exp(-abs(excessPx) / overdragMaxPx))
                    animationScope.launch {
                        overdrag.snapTo(if (excessPx < 0f) -resisted else resisted)
                    }
                }
            )
        }

        //The lens samples the page and the segment content. Where the refracting lens is unavailable
        //(below API 33) it only shows the segment content, like Prismal's own segmented control
        val mergedLensBackdrop = rememberPrismalMergedSource(backdrop, labelsLayer)
        val lensBackdrop =
            if (PrismalGlass.pipeline == PrismalGlassPipeline.Legacy) labelsLayer else mergedLensBackdrop

        LaunchedEffect(lensMotion, selectedIndex) {
            val target = selectedIndex.coerceIn(0, segmentCount - 1)
            if (lensMotion.targetValue.fastRoundToInt() != target) {
                lensMotion.animateToValue(target.toFloat())
            }
        }

        LaunchedEffect(lensMotion) {
            val damping = 2f * SegmentedStretchDampingRatio * sqrt(SegmentedStretchStiffness)
            var lastValue = lensMotion.value
            var stretchVelocity = 0f
            while (true) {
                if (abs(dropletStretch.floatValue) < 0.001f && abs(stretchVelocity) < 0.01f) {
                    dropletStretch.floatValue = 0f
                    stretchVelocity = 0f
                    val start = lastValue
                    snapshotFlow { lensMotion.value }.first { it != start }
                }
                var lastFrameNanos = withFrameNanos { it }
                while (true) {
                    val frameNanos = withFrameNanos { it }
                    val seconds = ((frameNanos - lastFrameNanos) / 1_000_000_000f).fastCoerceIn(0f, 1f / 30f)
                    lastFrameNanos = frameNanos
                    if (seconds <= 0f) continue

                    val value = lensMotion.value
                    val speedDp = abs(value - lastValue) * segmentWidthPx / density.density / seconds
                    lastValue = value
                    val target = (speedDp / SegmentedStretchReferenceSpeedDp).fastCoerceIn(0f, 1f)

                    val stretch = dropletStretch.floatValue
                    stretchVelocity += (SegmentedStretchStiffness * (target - stretch) -
                        damping * stretchVelocity) * seconds
                    dropletStretch.floatValue = stretch + stretchVelocity * seconds

                    if (target == 0f && abs(dropletStretch.floatValue) < 0.001f &&
                        abs(stretchVelocity) < 0.01f
                    ) break
                }
            }
        }

        val dropletHeightPx = with(density) { height.toPx() }
        val dropletWidthPx = segmentWidthPx
            .coerceAtMost(dropletHeightPx * SegmentedDropletMaxAspect)
            .coerceAtLeast(dropletHeightPx.coerceAtMost(segmentWidthPx))
        val dropletCenteringPx = (segmentWidthPx - dropletWidthPx) / 2f

        //Segment content: drawn here and recorded into [labelsLayer] for the lens
        Row(
            Modifier
                .prismalGlassLayer(labelsLayer)
                .fillMaxSize()
                .padding(horizontal = SegmentedHorizontalInset),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (index in 0 until segmentCount) {
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .selectable(
                            selected = index == selectedIndex,
                            interactionSource = null,
                            indication = null,
                            role = Role.Tab,
                            onClick = {
                                lensMotion.animateToValue(index.toFloat())
                                if (index != selectedIndex) onSelectedState.value(index)
                            }
                        )
                        .semantics { contentDescription = labels[index] },
                    contentAlignment = Alignment.Center
                ) {
                    segment(index)
                }
            }
        }

        Box(
            Modifier
                .padding(horizontal = SegmentedHorizontalInset)
                .graphicsLayer {
                    translationX =
                        (lensMotion.value * segmentWidthPx + dropletCenteringPx) * if (isLtr) 1f else -1f
                    translationX += overdrag.value
                }
                .then(lensMotion.modifier)
                .drawPrismalGlass(
                    backdrop = lensBackdrop,
                    shape = { SegmentedDropletShape },
                    effects = {
                        val radius = size.minDimension / 2f
                        //At rest the refracting band stays on the rim, outside the icon, so the lens copy of
                        //the icon coincides with the real one (no bright edges above and below it). Touching
                        //or dragging the droplet, or the droplet moving to another page, widens the band over
                        //the icon; it narrows again as the droplet settles.
                        val activity = maxOf(
                            lensMotion.pressProgress,
                            (abs(dropletStretch.floatValue) / SegmentedMotionLensStretch).fastCoerceIn(0f, 1f)
                        )
                        val band = lerp(SegmentedRestingRefractionBand, SegmentedRefractionBand, activity)
                        prismalLens(
                            refractionHeight = radius * band,
                            refractionAmount = radius * SegmentedRefractionAmount,
                            depthEffect = true,
                            chromaticAberration = chromaticAberration
                        )
                    },
                    specular = specular,
                    depthShadow = {
                        PrismalDepthShadow(alpha = lerp(0.6f, 1f, lensMotion.pressProgress))
                    },
                    depthInset = {
                        val progress = lensMotion.pressProgress
                        PrismalDepthInset(
                            radius = 8.dp * lerp(0.75f, 1f, progress),
                            alpha = lerp(0.6f, 1f, progress)
                        )
                    },
                    layerBlock = {
                        val stretch = dropletStretch.floatValue
                        val overdragFraction = abs(overdrag.value) / overdragMaxPx
                        scaleX = lensMotion.scaleX *
                            (1f + stretch * SegmentedStretchWidth) *
                            (1f + overdragFraction * SegmentedOverdragStretchWidth)
                        scaleY = lensMotion.scaleY *
                            (1f - stretch * SegmentedStretchHeight) *
                            (1f - overdragFraction * SegmentedOverdragStretchHeight)
                    }
                )
                .width(with(density) { dropletWidthPx.toDp() })
                .height(height)
        )
    }
}
