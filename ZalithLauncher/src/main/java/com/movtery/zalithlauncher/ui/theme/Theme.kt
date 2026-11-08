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

package com.movtery.zalithlauncher.ui.theme

import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamicColorScheme
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.setting.enums.isLauncherInDarkTheme
import com.movtery.zalithlauncher.ui.theme.components.activeMaskView
import com.movtery.zalithlauncher.utils.festival.Festival
import com.movtery.zalithlauncher.utils.festival.LocalFestivals
import com.movtery.zalithlauncher.game.version.installed.VersionsManager
import com.movtery.zalithlauncher.ui.components.GlassPageCapture
import com.movtery.zalithlauncher.ui.components.launcherGlassOn
import com.movtery.zalithlauncher.ui.components.LocalLiquidGlassBackdrop
import com.movtery.zalithlauncher.ui.components.LocalLiquidGlassInGame
import com.movtery.zalithlauncher.ui.components.LocalLiquidGlassPage
import com.movtery.zalithlauncher.ui.components.LocalLiquidGlassSharedBlur
import com.movtery.zalithlauncher.ui.components.rememberSharedGlassBlur
import com.movtery.zalithlauncher.viewmodel.BackgroundViewModel
import com.movtery.zalithlauncher.viewmodel.LocalBackgroundViewModel
import com.styropyr0.prismal.sources.rememberPrismalGlassLayer

private val embermireLight = lightColorScheme(
    primary = primaryLight,
    onPrimary = onPrimaryLight,
    primaryContainer = primaryContainerLight,
    onPrimaryContainer = onPrimaryContainerLight,
    secondary = secondaryLight,
    onSecondary = onSecondaryLight,
    secondaryContainer = secondaryContainerLight,
    onSecondaryContainer = onSecondaryContainerLight,
    tertiary = tertiaryLight,
    onTertiary = onTertiaryLight,
    tertiaryContainer = tertiaryContainerLight,
    onTertiaryContainer = onTertiaryContainerLight,
    error = errorLight,
    onError = onErrorLight,
    errorContainer = errorContainerLight,
    onErrorContainer = onErrorContainerLight,
    background = backgroundLight,
    onBackground = onBackgroundLight,
    surface = surfaceLight,
    onSurface = onSurfaceLight,
    surfaceVariant = surfaceVariantLight,
    onSurfaceVariant = onSurfaceVariantLight,
    outline = outlineLight,
    outlineVariant = outlineVariantLight,
    scrim = scrimLight,
    inverseSurface = inverseSurfaceLight,
    inverseOnSurface = inverseOnSurfaceLight,
    inversePrimary = inversePrimaryLight,
    surfaceDim = surfaceDimLight,
    surfaceBright = surfaceBrightLight,
    surfaceContainerLowest = surfaceContainerLowestLight,
    surfaceContainerLow = surfaceContainerLowLight,
    surfaceContainer = surfaceContainerLight,
    surfaceContainerHigh = surfaceContainerHighLight,
    surfaceContainerHighest = surfaceContainerHighestLight,
)

private val embermireDark = darkColorScheme(
    primary = primaryDark,
    onPrimary = onPrimaryDark,
    primaryContainer = primaryContainerDark,
    onPrimaryContainer = onPrimaryContainerDark,
    secondary = secondaryDark,
    onSecondary = onSecondaryDark,
    secondaryContainer = secondaryContainerDark,
    onSecondaryContainer = onSecondaryContainerDark,
    tertiary = tertiaryDark,
    onTertiary = onTertiaryDark,
    tertiaryContainer = tertiaryContainerDark,
    onTertiaryContainer = onTertiaryContainerDark,
    error = errorDark,
    onError = onErrorDark,
    errorContainer = errorContainerDark,
    onErrorContainer = onErrorContainerDark,
    background = backgroundDark,
    onBackground = onBackgroundDark,
    surface = surfaceDark,
    onSurface = onSurfaceDark,
    surfaceVariant = surfaceVariantDark,
    onSurfaceVariant = onSurfaceVariantDark,
    outline = outlineDark,
    outlineVariant = outlineVariantDark,
    scrim = scrimDark,
    inverseSurface = inverseSurfaceDark,
    inverseOnSurface = inverseOnSurfaceDark,
    inversePrimary = inversePrimaryDark,
    surfaceDim = surfaceDimDark,
    surfaceBright = surfaceBrightDark,
    surfaceContainerLowest = surfaceContainerLowestDark,
    surfaceContainerLow = surfaceContainerLowDark,
    surfaceContainer = surfaceContainerDark,
    surfaceContainerHigh = surfaceContainerHighDark,
    surfaceContainerHighest = surfaceContainerHighestDark,
)

private fun accentLight(color: Color): ColorScheme {
    return dynamicColorScheme(
        primary = color,
        isDark = false,
        style = PaletteStyle.TonalSpot,
    )
}

private fun accentDark(color: Color): ColorScheme {
    return dynamicColorScheme(
        primary = color,
        isDark = true,
        style = PaletteStyle.TonalSpot,
    )
}

@Composable
fun ZalithLauncherTheme(
    darkTheme: Boolean = isLauncherInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    backgroundViewModel: BackgroundViewModel? = null,
    festivals: List<Festival> = emptyList(),
    //In game there is no launcher background; provide a glass layer anyway (see GameGlassBackdrop)
    forceGlassLayer: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    //Per-profile theme: the selected version may bring its own accent color (launcher only, not in game)
    val profileAccent = if (forceGlassLayer) null else ProfileTheme.accent

    //Liquid Glass：仅在设置开启且存在有效背景媒体时提供可采样的背景图层
    val glassLayer = rememberPrismalGlassLayer()
    //forceGlassLayer is only set by the game activity: there the in-game switch applies as well
    val glassEnabled = if (forceGlassLayer) {
        AllSettings.launcherLiquidGlass.state && AllSettings.gameLiquidGlass.state
    } else {
        launcherGlassOn()
    }
    val activeGlassLayer = glassLayer.takeIf {
        glassEnabled && (backgroundViewModel?.isValid == true || forceGlassLayer)
    }

    //Glass is tinted dark and carries white content in both themes. While it is on, a light theme uses
    //the dark palette so text, tints and surfaces match dark mode instead of turning black or white.
    val paletteDark = darkTheme || activeGlassLayer != null

    val targetColorScheme = remember(paletteDark, dynamicColor, profileAccent) {
        when {
            profileAccent != null -> {
                val accent = Color(profileAccent)
                if (paletteDark) accentDark(accent) else accentLight(accent)
            }

            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                if (paletteDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }

            paletteDark -> embermireDark
            else -> embermireLight
        }
    }

    var currentDarkTheme by remember { mutableStateOf(paletteDark) }
    var currentDisplayScheme by remember { mutableStateOf(targetColorScheme) }

    LaunchedEffect(paletteDark, targetColorScheme) {
        if (paletteDark != currentDarkTheme) {
            context.activeMaskView(
                maskComplete = {
                    currentDarkTheme = paletteDark
                    currentDisplayScheme = targetColorScheme
                },
                maskAnimFinish = {

                }
            )
        } else {
            currentDarkTheme = paletteDark
            currentDisplayScheme = targetColorScheme
        }
    }

    if (!forceGlassLayer) {
        //Follow the selected version so its theme (accent, glass tier, wallpaper) is applied
        LaunchedEffect(Unit) {
            VersionsManager.currentVersion.collect { ProfileTheme.selectVersion(it) }
        }
        val profileWallpaper = remember(ProfileTheme.stamp) { ProfileTheme.wallpaperFile }
        LaunchedEffect(backgroundViewModel, profileWallpaper) {
            backgroundViewModel?.setProfileWallpaper(profileWallpaper)
        }
    }


    //Second layer that records the whole page (wallpaper + UI) while a dialog is open
    val pageLayer = rememberPrismalGlassLayer()
    val pageCapture = remember(pageLayer) { GlassPageCapture(pageLayer) }
    val activePageCapture = pageCapture.takeIf { activeGlassLayer != null }

    //Launcher only: the wallpaper blurred once at low resolution, sampled by glass instead of a live blur per surface
    val sharedBlur = rememberSharedGlassBlur()
    val activeSharedBlur = sharedBlur.takeIf { activeGlassLayer != null && !forceGlassLayer }

    CompositionLocalProvider(
        LocalBackgroundViewModel provides backgroundViewModel,
        LocalFestivals provides festivals,
        LocalLiquidGlassBackdrop provides activeGlassLayer,
        LocalLiquidGlassInGame provides forceGlassLayer,
        LocalLiquidGlassPage provides activePageCapture,
        LocalLiquidGlassSharedBlur provides activeSharedBlur
    ) {
        MaterialExpressiveTheme(
            colorScheme = currentDisplayScheme,
            motionScheme = MotionScheme.expressive(),
            typography = AppTypography,
            content = content
        )
    }
}
