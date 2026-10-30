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

package com.movtery.zalithlauncher.setting.enums

import com.google.gson.annotations.SerializedName
import com.movtery.zalithlauncher.R

/**
 * Quality level of the Liquid Glass effect. Chosen manually, never changed automatically.
 *
 * - [OFF]: no glass, normal Material surfaces
 * - [LITE]: lighter blur and cheaper dialog backdrop capture, for weaker GPUs
 * - [FULL]: the complete effect
 */
enum class GlassTier(val textRes: Int) {
    @SerializedName("OFF")
    OFF(R.string.generic_disable),
    @SerializedName("LITE")
    LITE(R.string.glass_tier_lite),
    @SerializedName("FULL")
    FULL(R.string.glass_tier_full)
}
