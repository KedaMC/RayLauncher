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

package com.movtery.zalithlauncher.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.movtery.zalithlauncher.game.version.installed.Version
import com.movtery.zalithlauncher.setting.enums.GlassTier
import java.io.File

/** File name of a per-profile wallpaper inside the version's launcher folder. */
const val PROFILE_WALLPAPER_NAME = "theme_background.file"

/** Where the wallpaper of [version]'s theme is stored (it may not exist). */
fun profileWallpaperFile(version: Version): File = File(version.getZalithVersionPath(), PROFILE_WALLPAPER_NAME)

/**
 * Per-profile (per-version) theme of the launcher: accent color, glass tier and wallpaper.
 * Values come from the selected version's config; null means "follow the launcher setting".
 *
 * Everything here reads Compose snapshot state, so UI that calls these getters recomposes when
 * the selected version changes or [notifyChanged] is called after an edit.
 */
object ProfileTheme {
    private var version by mutableStateOf<Version?>(null)
    private var revision by mutableIntStateOf(0)

    /** Called with the currently selected version (null when there is none). */
    fun setVersion(newVersion: Version?) {
        version = newVersion
    }

    /** Call after the theme fields of the selected version were edited and saved. */
    fun notifyChanged() {
        revision++
    }

    /** Changes whenever the selected version or its theme changes. */
    val stamp: Pair<Version?, Int> get() = version to revision

    /** Accent color (ARGB) of the selected version, or null. */
    val accent: Int?
        get() {
            if (revision < 0) return null
            return version?.getVersionConfig()?.themeAccent
        }

    /** Glass tier override of the selected version, or null. */
    val glassTier: GlassTier?
        get() {
            if (revision < 0) return null
            return version?.getVersionConfig()?.themeGlassTier
        }

    /** Wallpaper of the selected version, or null when it has none. */
    val wallpaperFile: File?
        get() {
            if (revision < 0) return null
            return version?.let { profileWallpaperFile(it) }?.takeIf { it.isFile }
        }
}
