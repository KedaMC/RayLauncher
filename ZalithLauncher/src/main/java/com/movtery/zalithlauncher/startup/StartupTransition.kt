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

package com.movtery.zalithlauncher.startup

import com.movtery.zalithlauncher.setting.AllSettings

/** Fade durations of the startup transition through black, stored in tenths of a second */
object StartupTransition {
    /** Intent extra: the main page has to be revealed from black without entrance animations */
    const val EXTRA_REVEAL = "EXTRA_STARTUP_REVEAL"

    /** Intent extra: the main page plays the startup video while it is revealed */
    const val EXTRA_PLAY_VIDEO = "EXTRA_STARTUP_PLAY_VIDEO"

    val fadeInMillis: Int
        get() = AllSettings.startupFadeInDuration.getValue() * 100

    val fadeOutMillis: Int
        get() = AllSettings.startupFadeOutDuration.getValue() * 100
}
