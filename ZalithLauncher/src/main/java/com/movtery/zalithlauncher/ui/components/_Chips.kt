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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableChipColors
import androidx.compose.material3.SelectableChipElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.R

/**
 * A composable that displays a checkable chip, visually similar to a [FilterChip].
 * It allows for a selection state to be toggled.
 */
@Composable
fun CheckChip(
    selected: Boolean,
    label: @Composable (() -> Unit),
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = FilterChipDefaults.shape,
    colors: SelectableChipColors = FilterChipDefaults.filterChipColors(),
    elevation: SelectableChipElevation? = FilterChipDefaults.filterChipElevation(),
    border: BorderStroke? = FilterChipDefaults.filterChipBorder(enabled, selected),
    interactionSource: MutableInteractionSource? = null
) {
    val glassBackdrop = liquidGlassBackdrop()
    if (glassBackdrop != null) {
        // Liquid Glass chip: its own glass capsule, tinted when selected
        LiquidGlassSurface(
            backdrop = glassBackdrop,
            shape = MaterialTheme.shapes.large,
            modifier = modifier.alpha(if (enabled) 1f else 0.5f),
            containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
            contentColor = Color.White,
            onClick = if (enabled) onClick else null
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AnimatedVisibility(
                    visible = selected,
                    enter = expandIn(
                        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
                        expandFrom = Alignment.CenterStart
                    ) + fadeIn(),
                    exit = shrinkOut(
                        shrinkTowards = Alignment.CenterStart
                    ) + fadeOut()
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_check),
                        contentDescription = null,
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .size(FilterChipDefaults.IconSize)
                    )
                }
                ProvideTextStyle(MaterialTheme.typography.labelLarge) {
                    label()
                }
            }
        }
        return
    }

    FilterChip(
        modifier = modifier,
        selected = selected,
        onClick = onClick,
        leadingIcon = {
            AnimatedVisibility(
                visible = selected,
                enter = expandIn(
                    animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
                    expandFrom = Alignment.CenterStart
                ) + fadeIn(),
                exit = shrinkOut(
                    shrinkTowards = Alignment.CenterStart
                ) + fadeOut()
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_check),
                    contentDescription = null,
                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                )
            }
        },
        label = label,
        enabled = enabled,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border,
        interactionSource = interactionSource
    )
}