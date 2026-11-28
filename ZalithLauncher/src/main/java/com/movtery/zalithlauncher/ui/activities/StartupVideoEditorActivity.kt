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

package com.movtery.zalithlauncher.ui.activities

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import com.movtery.zalithlauncher.startup.StartupAnimation
import com.movtery.zalithlauncher.startup.StartupVideoLayout
import com.movtery.zalithlauncher.ui.base.BaseAppCompatActivity
import com.movtery.zalithlauncher.ui.screens.splash.StartupVideoEditor
import dagger.hilt.android.AndroidEntryPoint

/**
 * Places the startup video. It is its own full screen window, like the startup page, so the preview
 * matches what is shown at startup.
 */
@AndroidEntryPoint
class StartupVideoEditorActivity : BaseAppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val video = StartupAnimation.video ?: run {
            finish()
            return
        }

        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                StartupVideoEditor(
                    video = video,
                    initial = StartupVideoLayout.load(),
                    onSave = { layout ->
                        layout.save()
                        finish()
                    },
                    onCancel = ::finish
                )
            }
        }
    }
}
