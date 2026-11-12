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

package com.movtery.zalithlauncher.ui.screens.main

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.ui.components.LiquidGlassSurface
import com.movtery.zalithlauncher.ui.components.SharedGlassBlurPage
import com.movtery.zalithlauncher.ui.components.liquidGlassBackdrop
import com.movtery.zalithlauncher.ui.screens.PAGE_SLIDE_MILLIS
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** The three main pages shown in the top navigation, in slide order */
enum class MainPage(
    val index: Int,
    @DrawableRes val icon: Int,
    @StringRes val label: Int
) {
    HOME(0, R.drawable.ic_home_filled, R.string.main_nav_home),
    DOWNLOAD(1, R.drawable.ic_download_2_filled, R.string.generic_download),
    SETTINGS(2, R.drawable.ic_settings_filled, R.string.generic_setting)
}

private val NavItemWidth = 88.dp
private val NavItemHeight = 32.dp
private val NavPadding = 4.dp

//The edge the blob moves toward leaves early, the other edge follows late: the pill stretches, then settles
private val LeadingEasing = CubicBezierEasing(0.25f, 0.0f, 0.2f, 1.0f)
private val TrailingEasing = CubicBezierEasing(0.6f, 0.0f, 0.75f, 0.9f)

/**
 * Horizontal navigation group for the three main pages, with one shared Liquid Glass indicator.
 * The indicator travels to the target button and stretches like a water drop while the page slides.
 * @param shown false hides the group (auto-hide on Home)
 */
@Composable
fun MainNavigationBar(
    selectedIndex: Int,
    shown: Boolean,
    onNavigate: (MainPage) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val itemWidthPx = with(density) { NavItemWidth.toPx() }
    val itemHeightPx = with(density) { NavItemHeight.toPx() }

    //Indicator edges in px, relative to the padded row. Edges are animated separately to get the stretch
    val left = remember { Animatable(selectedIndex * itemWidthPx) }
    val right = remember { Animatable((selectedIndex + 1) * itemWidthPx) }

    LaunchedEffect(selectedIndex) {
        val targetLeft = selectedIndex * itemWidthPx
        val targetRight = targetLeft + itemWidthPx
        if (!AllSettings.launcherSwapAnimEnabled.state) {
            left.snapTo(targetLeft)
            right.snapTo(targetRight)
            return@LaunchedEffect
        }
        val movingRight = targetLeft >= left.value
        coroutineScope {
            launch {
                left.animateTo(
                    targetLeft,
                    tween(PAGE_SLIDE_MILLIS, easing = if (movingRight) TrailingEasing else LeadingEasing)
                )
            }
            launch {
                right.animateTo(
                    targetRight,
                    tween(PAGE_SLIDE_MILLIS, easing = if (movingRight) LeadingEasing else TrailingEasing)
                )
            }
        }
    }

    val visibility by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = tween(durationMillis = 220),
        label = "NavBarVisibility"
    )
    val hiddenShiftPx = with(density) { 12.dp.toPx() }

    val backdrop = liquidGlassBackdrop()
    val glass = backdrop != null

    val body: @Composable () -> Unit = {
        Box(modifier = Modifier.padding(NavPadding)) {
            val indicatorModifier = Modifier
                .offset { IntOffset(left.value.roundToInt(), 0) }
                .layout { measurable, _ ->
                    val w = (right.value - left.value).roundToInt().coerceAtLeast(0)
                    val h = itemHeightPx.roundToInt()
                    val placeable = measurable.measure(Constraints.fixed(w, h))
                    layout(w, h) { placeable.place(0, 0) }
                }
                .graphicsLayer {
                    //Water drop: the longer the pill stretches, the flatter it gets
                    val stretch = ((right.value - left.value) / itemWidthPx - 1f).coerceIn(0f, 1f)
                    scaleY = 1f - 0.12f * stretch
                }
            if (backdrop != null) {
                LiquidGlassSurface(
                    backdrop = backdrop,
                    shape = MaterialTheme.shapes.large,
                    modifier = indicatorModifier,
                    containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.22f),
                    contentColor = Color.White,
                    blurRadius = 8.dp
                ) {}
            } else {
                Box(
                    modifier = indicatorModifier.background(
                        MaterialTheme.colorScheme.secondaryContainer,
                        MaterialTheme.shapes.large
                    )
                )
            }

            Row {
                MainPage.entries.forEach { page ->
                    NavItem(
                        page = page,
                        selected = page.index == selectedIndex,
                        glass = glass,
                        enabled = shown,
                        onClick = { onNavigate(page) }
                    )
                }
            }
        }
    }

    Box(
        modifier = modifier.graphicsLayer {
            alpha = visibility
            translationY = -(1f - visibility) * hiddenShiftPx
        }
    ) {
        if (backdrop != null) {
            SharedGlassBlurPage {
                LiquidGlassSurface(
                    backdrop = backdrop,
                    shape = MaterialTheme.shapes.large,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = LocalContentColor.current,
                    blurRadius = 12.dp
                ) {
                    body()
                }
            }
        } else {
            body()
        }
    }
}

@Composable
private fun NavItem(
    page: MainPage,
    selected: Boolean,
    glass: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val targetColor = when {
        glass -> Color.White
        selected -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val color by animateColorAsState(targetValue = targetColor, label = "NavItemColor")
    val label = stringResource(page.label)

    Row(
        modifier = Modifier
            .width(NavItemWidth)
            .height(NavItemHeight)
            .clip(MaterialTheme.shapes.large)
            .clickable(enabled = enabled, role = Role.Tab, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            modifier = Modifier.size(18.dp),
            painter = painterResource(page.icon),
            contentDescription = label,
            tint = color
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            color = color,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
