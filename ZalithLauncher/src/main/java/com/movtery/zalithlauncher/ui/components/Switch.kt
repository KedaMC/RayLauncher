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

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import com.movtery.zalithlauncher.ui.theme.itemColor
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.setting.AllSettings

@Composable
fun DefaultSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: SwitchColors = SwitchDefaults.colors(),
    interactionSource: MutableInteractionSource? = null
) {
    val glassEnabled = liquidGlassEnabled()
    val backdrop = liquidGlassBackdrop()
    val glassColors = if (glassEnabled) {
        SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = Color.White.copy(alpha = 0.24f),
            checkedBorderColor = Color.White.copy(alpha = 0.56f),
            checkedIconColor = Color(0xFF202124),
            uncheckedThumbColor = Color.White.copy(alpha = 0.86f),
            uncheckedTrackColor = Color.White.copy(alpha = 0.10f),
            uncheckedBorderColor = Color.White.copy(alpha = 0.38f),
            uncheckedIconColor = Color.White.copy(alpha = 0.82f),
            disabledCheckedThumbColor = Color.White.copy(alpha = 0.56f),
            disabledCheckedTrackColor = Color.White.copy(alpha = 0.12f),
            disabledCheckedBorderColor = Color.White.copy(alpha = 0.25f),
            disabledCheckedIconColor = Color.DarkGray.copy(alpha = 0.7f),
            disabledUncheckedThumbColor = Color.White.copy(alpha = 0.5f),
            disabledUncheckedTrackColor = Color.White.copy(alpha = 0.06f),
            disabledUncheckedBorderColor = Color.White.copy(alpha = 0.18f),
            disabledUncheckedIconColor = Color.White.copy(alpha = 0.4f)
        )
    } else colors

    val switchContent: @Composable () -> Unit = {
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.padding(horizontal = 2.dp, vertical = 1.dp),
            enabled = enabled,
            thumbContent = {
                val rotation by animateFloatAsState(if (checked) 0.0f else -(180.0f))
                Crossfade(modifier = Modifier.rotate(rotation), targetState = checked) {
                    Icon(
                        modifier = Modifier.size(16.dp),
                        painter = painterResource(if (it) R.drawable.ic_check else R.drawable.ic_close),
                        contentDescription = null,
                        tint = glassColors.trackColor(enabled, checked)
                    )
                }
            },
            colors = glassColors,
            interactionSource = interactionSource
        )
    }
    if (glassEnabled && backdrop != null) {
        Box(
            modifier = modifier.then(
                if (enabled) Modifier.liquidGlassPressAnimation() else Modifier
            )
        ) {
            // Draw the glass on the actual 52x32dp track, not the larger
            // minimum-touch-target bounds of Material3 Switch.
            LiquidGlassSurface(
                backdrop = backdrop,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .align(Alignment.Center)
                    .width(52.dp)
                    .height(32.dp),
                containerColor = itemColor(),
                contentColor = Color.White,
                blurRadius = 8.dp
            ) {}
            switchContent()
        }
    } else if (glassEnabled) {
        Box(modifier = modifier) { switchContent() }
    } else {
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = modifier,
            enabled = enabled,
            thumbContent = {
                val rotation by animateFloatAsState(if (checked) 0.0f else -(180.0f))
                Crossfade(modifier = Modifier.rotate(rotation), targetState = checked) {
                    Icon(
                        modifier = Modifier.size(16.dp),
                        painter = painterResource(if (it) R.drawable.ic_check else R.drawable.ic_close),
                        contentDescription = null,
                        tint = colors.trackColor(enabled, checked)
                    )
                }
            },
            colors = colors,
            interactionSource = interactionSource
        )
    }
}

/**
 * Represents the color used for the switch's track, depending on [enabled] and [checked].
 *
 * @param enabled whether the [Switch] is enabled or not
 * @param checked whether the [Switch] is checked or not
 */
@Stable
private fun SwitchColors.trackColor(enabled: Boolean, checked: Boolean): Color =
    if (enabled) {
        if (checked) checkedTrackColor else uncheckedTrackColor
    } else {
        if (checked) disabledCheckedTrackColor else disabledUncheckedTrackColor
    }