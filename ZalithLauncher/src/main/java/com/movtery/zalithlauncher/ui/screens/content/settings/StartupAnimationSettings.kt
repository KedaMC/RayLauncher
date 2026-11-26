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

package com.movtery.zalithlauncher.ui.screens.content.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.contract.MediaPickerContract
import com.movtery.zalithlauncher.coroutine.Task
import com.movtery.zalithlauncher.coroutine.TaskSystem
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.setting.unit.IntSettingUnit
import com.movtery.zalithlauncher.setting.unit.floatRange
import com.movtery.zalithlauncher.startup.StartupAnimation
import com.movtery.zalithlauncher.startup.StartupVideoInvalidException
import com.movtery.zalithlauncher.startup.StartupVideoTooLongException
import com.movtery.zalithlauncher.ui.androidText
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.CardPosition
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.IntSliderSettingsCard
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.SettingsCard
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.SettingsCardColumn
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.SwitchSettingsCard
import com.movtery.zalithlauncher.utils.string.getMessageOrToString
import com.movtery.zalithlauncher.viewmodel.ErrorViewModel
import kotlinx.coroutines.Dispatchers

/** Startup animation video and the fade durations of the startup transition */
@Composable
internal fun StartupAnimationSettings(
    submitError: (ErrorViewModel.ThrowableMessage) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val hasVideo = StartupAnimation.video != null

    val filePicker = rememberLauncherForActivityResult(
        contract = MediaPickerContract(
            allowImages = false,
            allowVideos = true,
            allowMultiple = false
        )
    ) { result ->
        if (result != null) {
            TaskSystem.submitTask(
                Task.runTask(
                    dispatcher = Dispatchers.IO,
                    task = { task ->
                        task.updateMessage(androidText(R.string.settings_ray_startup_importing))
                        StartupAnimation.import(context, result[0])
                    },
                    onError = { th ->
                        submitError(
                            ErrorViewModel.ThrowableMessage(
                                title = androidText(R.string.settings_ray_startup_import_failed),
                                message = when (th) {
                                    is StartupVideoTooLongException -> androidText(R.string.settings_ray_startup_too_long)
                                    is StartupVideoInvalidException -> androidText(R.string.settings_ray_startup_invalid)
                                    else -> androidText(th.getMessageOrToString())
                                }
                            )
                        )
                    }
                )
            )
        }
    }

    SettingsCardColumn(modifier = modifier) {
        SwitchSettingsCard(
            modifier = Modifier.fillMaxWidth(),
            position = CardPosition.Top,
            unit = AllSettings.startupAnimationEnabled,
            title = stringResource(R.string.settings_ray_startup_enable),
            summary = stringResource(R.string.settings_ray_startup_enable_summary)
        )

        FadeDurationCard(
            unit = AllSettings.startupFadeInDuration,
            title = stringResource(R.string.settings_ray_startup_fade_in)
        )

        FadeDurationCard(
            unit = AllSettings.startupFadeOutDuration,
            title = stringResource(R.string.settings_ray_startup_fade_out)
        )

        SettingsCard(
            modifier = Modifier.fillMaxWidth(),
            position = CardPosition.Middle,
            title = stringResource(R.string.settings_ray_startup_import),
            summary = stringResource(
                if (hasVideo) R.string.settings_ray_startup_imported
                else R.string.settings_ray_startup_import_summary
            ),
            onClick = { filePicker.launch(Unit) }
        )

        SettingsCard(
            modifier = Modifier.fillMaxWidth(),
            position = CardPosition.Bottom,
            title = stringResource(R.string.settings_ray_startup_remove),
            enabled = hasVideo,
            onClick = { StartupAnimation.remove() }
        )
    }
}

@Composable
private fun FadeDurationCard(
    unit: IntSettingUnit,
    title: String
) {
    IntSliderSettingsCard(
        modifier = Modifier.fillMaxWidth(),
        position = CardPosition.Middle,
        unit = unit,
        title = title,
        valueRange = unit.floatRange,
        valueText = { value -> "${value.toInt() / 10f} s" }
    )
}
