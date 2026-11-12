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
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.ui.components.LiquidGlassSurface
import com.movtery.zalithlauncher.ui.components.SharedGlassBlurPage
import com.movtery.zalithlauncher.ui.components.liquidGlassBackdrop
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

private val NavItemWidth = 60.dp
private val NavItemHeight = 32.dp
private val NavPadding = 4.dp
private val BlobShape = RoundedCornerShape(percent = 50)

//Water drop physics: the edge moving toward the target is stiff and leaves first, the trailing edge
//is soft and lags behind, then overshoots and wobbles before the blob settles
private val LeadingSpring = spring<Float>(dampingRatio = 0.72f, stiffness = 560f)
private val TrailingSpring = spring<Float>(dampingRatio = 0.5f, stiffness = 240f)

//Thin bright rim, brightest at the top-left and bottom-right corners like light catching glass
private val GlassRimBrush = Brush.linearGradient(
    colors = listOf(
        Color.White.copy(alpha = 0.75f),
        Color.White.copy(alpha = 0.08f),
        Color.White.copy(alpha = 0.45f)
    )
)

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
            launch { left.animateTo(targetLeft, if (movingRight) TrailingSpring else LeadingSpring) }
            launch { right.animateTo(targetRight, if (movingRight) LeadingSpring else TrailingSpring) }
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
                    //Water drop: stretching thins the blob, the wobble after landing squeezes it taller
                    val stretch = ((right.value - left.value) / itemWidthPx - 1f).coerceIn(-0.5f, 1f)
                    scaleY = 1f - 0.2f * stretch
                }
            if (backdrop != null) {
                LiquidGlassSurface(
                    backdrop = backdrop,
                    shape = BlobShape,
                    modifier = indicatorModifier,
                    //No container tint: a tint is what made the indicator milky white. Clear glass
                    //with a light blur lets the lens refract what is behind it
                    containerColor = Color.Unspecified,
                    contentColor = Color.White,
                    blurRadius = 3.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .border(width = 1.dp, brush = GlassRimBrush, shape = BlobShape)
                    )
                }
            } else {
                Box(
                    modifier = indicatorModifier.background(
                        MaterialTheme.colorScheme.secondaryContainer,
                        BlobShape
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

    Box(
        modifier = Modifier
            .width(NavItemWidth)
            .height(NavItemHeight)
            .clip(BlobShape)
            .clickable(enabled = enabled, role = Role.Tab, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            modifier = Modifier.size(22.dp),
            painter = painterResource(page.icon),
            contentDescription = label,
            tint = color
        )
    }
}
