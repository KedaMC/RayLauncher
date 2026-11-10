/*
 * Zalith Launcher 2
 * Copyright (C) 2025 MovTery <movtery228@qq.com> and contributors
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

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.WavyProgressIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.movtery.zalithlauncher.setting.enums.GlassTier

/** Opacity of the progress track on glass, same as the glass sliders. */
private const val GLASS_TRACK_ALPHA = 0.28f

/**
 * Indeterminate linear progress for loading states.
 *
 * - Liquid Glass off: the normal Material wavy indicator.
 * - Glass on ([GlassTier.FULL]): wavy indicator in white, like the glass sliders.
 * - Glass on ([GlassTier.LITE]): flat white bar. The wave animation redraws every frame for as long
 *   as it is on screen, which is not worth the GPU time on weaker devices.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GlassLinearProgress(
    modifier: Modifier = Modifier,
    wavelength: Dp = WavyProgressIndicatorDefaults.LinearIndeterminateWavelength
) {
    if (liquidGlassBackdrop() == null) {
        LinearWavyProgressIndicator(modifier = modifier, wavelength = wavelength)
        return
    }
    val color = Color.White
    val trackColor = Color.White.copy(alpha = GLASS_TRACK_ALPHA)
    if (liquidGlassTier() == GlassTier.LITE) {
        LinearProgressIndicator(modifier = modifier, color = color, trackColor = trackColor)
    } else {
        LinearWavyProgressIndicator(
            modifier = modifier,
            color = color,
            trackColor = trackColor,
            wavelength = wavelength
        )
    }
}

/**
 * Determinate variant of [GlassLinearProgress], with the same glass and tier behavior.
 * [progress] is read lazily so only the indicator redraws when the value changes.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GlassLinearProgress(
    progress: () -> Float,
    modifier: Modifier = Modifier
) {
    if (liquidGlassBackdrop() == null) {
        LinearWavyProgressIndicator(progress = progress, modifier = modifier)
        return
    }
    val color = Color.White
    val trackColor = Color.White.copy(alpha = GLASS_TRACK_ALPHA)
    if (liquidGlassTier() == GlassTier.LITE) {
        LinearProgressIndicator(progress = progress, modifier = modifier, color = color, trackColor = trackColor)
    } else {
        LinearWavyProgressIndicator(
            progress = progress,
            modifier = modifier,
            color = color,
            trackColor = trackColor
        )
    }
}
