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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import com.movtery.zalithlauncher.setting.AllSettings

/**
 * Background opacity (element opacity) used while Liquid Glass is on.
 * Equals the minimum of the opacity slider, so the background stays fully visible
 * around and behind the glass panels without the user touching the sliders.
 */
const val LIQUID_GLASS_BACKGROUND_OPACITY = 20

/**
 * Whether the launcher-level Liquid Glass switch is on. For code that cannot call composables
 * (draw nodes, snapshotFlow, view models). Only used inside the launcher background pipeline.
 */
fun liquidGlassOverridesBackground(): Boolean = AllSettings.launcherLiquidGlass.state

/**
 * Effective background blur. Liquid Glass does its own blur/refraction, so the global
 * background blur is forced to 0 while it is on. The stored setting is never modified.
 */
@Composable
@ReadOnlyComposable
fun effectiveBackgroundBlur(): Int =
    if (liquidGlassEnabled()) 0 else AllSettings.backgroundBlur.state

/**
 * Effective background (element) opacity. While Liquid Glass is on it is capped to
 * [LIQUID_GLASS_BACKGROUND_OPACITY]. The stored setting is never modified.
 */
@Composable
@ReadOnlyComposable
fun effectiveBackgroundOpacity(): Int {
    val stored = AllSettings.launcherBackgroundOpacity.state
    return if (liquidGlassEnabled()) minOf(stored, LIQUID_GLASS_BACKGROUND_OPACITY) else stored
}

/** Non-composable variant of [effectiveBackgroundBlur]. */
fun effectiveBackgroundBlurNow(): Int =
    if (liquidGlassOverridesBackground()) 0 else AllSettings.backgroundBlur.state

/** Non-composable variant of [effectiveBackgroundOpacity]. */
fun effectiveBackgroundOpacityNow(): Int {
    val stored = AllSettings.launcherBackgroundOpacity.state
    return if (liquidGlassOverridesBackground()) minOf(stored, LIQUID_GLASS_BACKGROUND_OPACITY) else stored
}
