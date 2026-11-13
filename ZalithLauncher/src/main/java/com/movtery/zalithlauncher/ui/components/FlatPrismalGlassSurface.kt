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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceAtMost
import androidx.compose.ui.util.lerp
import com.styropyr0.prismal.PrismalBackdrop
import com.styropyr0.prismal.PrismalDefaultTintAlpha
import com.styropyr0.prismal.drawPrismalGlass
import com.styropyr0.prismal.drawPrismalGlassTint
import com.styropyr0.prismal.effects.applyPrismalGlassEffects
import com.styropyr0.prismal.interactive.PrismalPressRipple
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tanh

/**
 * Same as PrismalGlassSurface (PrismalAGSL v1.0.6), but without the library's built-in depth shadow.
 *
 * PrismalGlassSurface calls drawPrismalGlass() and never passes depthShadow, so the library default
 * (black 10%, 24dp blur, 4dp offset) is drawn around every glass panel and cannot be turned off from
 * the outside. Here depthShadow is null; everything else (lens refraction, blur, tint, edge highlight,
 * press spring) is kept identical to the library implementation.
 */
@Composable
fun FlatPrismalGlassSurface(
    backdrop: PrismalBackdrop,
    modifier: Modifier = Modifier,
    shape: () -> Shape,
    onClick: (() -> Unit)? = null,
    blurRadius: Dp = 8.dp,
    surfaceColor: Color = Color.Unspecified,
    content: @Composable BoxScope.() -> Unit = {}
) {
    val density = LocalDensity.current
    val pressLiftPx = with(density) { 4.dp.toPx() }
    val animationScope = rememberCoroutineScope()
    val interactivePrismalSpecular = remember(onClick, animationScope) {
        if (onClick != null) PrismalPressRipple(animationScope = animationScope) else null
    }

    Box(
        modifier = modifier
            .drawPrismalGlass(
                backdrop = backdrop,
                shape = shape,
                effects = {
                    applyPrismalGlassEffects(
                        density = density,
                        adaptiveLuminance = false,
                        luminance = 0.5f,
                        blurRadiusPx = with(density) { blurRadius.toPx() },
                        refractionHeightPx = 16f,
                        refractionAmountPx = 32f,
                        brightness = 0f,
                        saturation = 1.5f,
                        depthEffect = false,
                        chromaticAberration = 0f,
                        useVibrancy = true,
                    )
                },
                depthShadow = null,
                layerBlock = if (interactivePrismalSpecular != null) {
                    {
                        val width = size.width
                        val height = size.height
                        val progress = interactivePrismalSpecular.pressProgress
                        val scale = lerp(1f, 1f + pressLiftPx / size.height, progress)

                        val maxOffset = size.minDimension
                        val initialDerivative = 0.05f
                        val offset = interactivePrismalSpecular.offset
                        translationX = maxOffset * tanh(initialDerivative * offset.x / maxOffset)
                        translationY = maxOffset * tanh(initialDerivative * offset.y / maxOffset)

                        val maxDragScale = pressLiftPx / size.height
                        val offsetAngle = atan2(offset.y, offset.x)
                        scaleX = scale +
                            maxDragScale * abs(cos(offsetAngle) * offset.x / size.maxDimension) *
                            (width / height).fastCoerceAtMost(1f)
                        scaleY = scale +
                            maxDragScale * abs(sin(offsetAngle) * offset.y / size.maxDimension) *
                            (height / width).fastCoerceAtMost(1f)
                    }
                } else {
                    null
                },
                onDrawSurface = {
                    drawPrismalGlassTint(Color.Unspecified, PrismalDefaultTintAlpha)
                    if (surfaceColor.isSpecified) {
                        drawRect(surfaceColor)
                    }
                }
            )
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = null,
                        indication = null,
                        role = Role.Button,
                        onClick = onClick
                    )
                } else {
                    Modifier
                }
            )
            .then(
                if (interactivePrismalSpecular != null) {
                    Modifier
                        .then(interactivePrismalSpecular.modifier)
                        .then(interactivePrismalSpecular.gestureModifier)
                } else {
                    Modifier
                }
            ),
        content = content
    )
}
