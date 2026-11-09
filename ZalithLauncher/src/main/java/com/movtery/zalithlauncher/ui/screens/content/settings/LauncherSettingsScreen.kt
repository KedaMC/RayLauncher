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

package com.movtery.zalithlauncher.ui.screens.content.settings

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import com.movtery.zalithlauncher.ui.screens.content.home.overlay.OverlayCardManager
import com.movtery.zalithlauncher.setting.enums.GlassTier
import com.movtery.zalithlauncher.ui.components.launcherGlassOn
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import com.movtery.zalithlauncher.ui.components.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.contract.MediaPickerContract
import com.movtery.zalithlauncher.coroutine.Task
import com.movtery.zalithlauncher.coroutine.TaskSystem
import com.movtery.zalithlauncher.path.PathManager
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.setting.enums.AppLanguage
import com.movtery.zalithlauncher.setting.enums.BackgroundBlur
import com.movtery.zalithlauncher.setting.enums.DarkMode
import com.movtery.zalithlauncher.setting.enums.MirrorSourceType
import com.movtery.zalithlauncher.setting.enums.applyLanguage
import com.movtery.zalithlauncher.setting.unit.floatRange
import com.movtery.zalithlauncher.ui.androidText
import com.movtery.zalithlauncher.ui.base.BaseScreen
import com.movtery.zalithlauncher.ui.components.GlassLazyColumn
import com.movtery.zalithlauncher.ui.components.animated
import com.movtery.zalithlauncher.ui.components.lazyScrollWithBar
import com.movtery.zalithlauncher.ui.components.IconTextButton
import com.movtery.zalithlauncher.ui.components.SimpleAlertDialog
import com.movtery.zalithlauncher.ui.components.TitleAndSummary
import com.movtery.zalithlauncher.ui.screens.NestedNavKey
import com.movtery.zalithlauncher.ui.systemLocaleStringResource
import com.movtery.zalithlauncher.ui.screens.NormalNavKey
import com.movtery.zalithlauncher.ui.screens.TitledNavKey
import com.movtery.zalithlauncher.ui.screens.content.elements.DisabledAlpha
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.CardPosition
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.IntSliderSettingsCard
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.ListSettingsCard
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.SettingsCard
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.SettingsCardColumn
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.SwitchSettingsCard
import com.movtery.zalithlauncher.utils.file.shareFile
import com.movtery.zalithlauncher.utils.isChinaMainland
import com.movtery.zalithlauncher.utils.logging.Logger
import com.movtery.zalithlauncher.utils.string.getMessageOrToString
import com.movtery.zalithlauncher.viewmodel.BackgroundViewModel
import com.movtery.zalithlauncher.viewmodel.ErrorViewModel
import com.movtery.zalithlauncher.viewmodel.LocalBackgroundViewModel
import kotlinx.coroutines.Dispatchers
import java.io.File

private const val TAG = "LauncherSettingsScreen"

@Composable
fun LauncherSettingsScreen(
    key: NestedNavKey.Settings,
    settingsScreenKey: TitledNavKey?,
    mainScreenKey: TitledNavKey?,
    submitError: (ErrorViewModel.ThrowableMessage) -> Unit,
) {
    val context = LocalContext.current

    BaseScreen(
        Triple(key, mainScreenKey, false),
        Triple(NormalNavKey.Settings.Launcher, settingsScreenKey, false)
    ) { isVisible ->
        //Lazy list: every card is a glass surface, so only the groups near the viewport may be composed.
        //Composable reads (CompositionLocal) must happen here, the list content below is not composable.
        val listState = rememberLazyListState()
        val backgroundViewModel = LocalBackgroundViewModel.current

        GlassLazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .lazyScrollWithBar(listState),
            state = listState,
            isVisible = isVisible,
            contentPadding = PaddingValues(all = 12.dp)
        ) {
            animatedItem { ctx ->
                SettingsCardColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animated(ctx)
                ) {
                    ListSettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = CardPosition.Top,
                        unit = AllSettings.launcherDarkMode,
                        items = DarkMode.entries,
                        title = stringResource(R.string.settings_launcher_dark_mode_title),
                        getItemText = { stringResource(it.textRes) }
                    )

                    ListSettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = CardPosition.Middle,
                        unit = AllSettings.launcherLanguage,
                        items = AppLanguage.entries,
                        title = systemLocaleStringResource(R.string.settings_launcher_language),
                        getItemText = {
                            if (it == AppLanguage.FOLLOW_SYSTEM) systemLocaleStringResource(it.textRes)
                            else stringResource(it.textRes)
                        },
                        onValueChange = {
                            applyLanguage(it)
                        }
                    )

                    SwitchSettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = CardPosition.Middle,
                        unit = AllSettings.launcherFestivalEffects,
                        title = stringResource(R.string.settings_launcher_festivals_effects_title),
                        summary = stringResource(R.string.settings_launcher_festivals_effects_summary)
                    )

                    SwitchSettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = CardPosition.Middle,
                        unit = AllSettings.launcherFullScreen,
                        title = stringResource(R.string.settings_launcher_full_screen_title),
                        summary = stringResource(R.string.settings_launcher_full_screen_summary)
                    )

                    SwitchSettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = CardPosition.Middle,
                        unit = AllSettings.launcherLiquidGlass,
                        title = stringResource(R.string.settings_launcher_liquid_glass_title),
                        summary = stringResource(R.string.settings_launcher_liquid_glass_summary)
                    )

                    ListSettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = CardPosition.Middle,
                        unit = AllSettings.launcherGlassTier,
                        items = glassTierChoices,
                        title = stringResource(R.string.settings_launcher_glass_tier_title),
                        summary = stringResource(R.string.settings_launcher_glass_tier_summary),
                        getItemText = { stringResource(it.textRes) },
                        enabled = AllSettings.launcherLiquidGlass.state
                    )

                    ListSettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = CardPosition.Bottom,
                        unit = AllSettings.gameGlassTier,
                        items = glassTierChoices,
                        title = stringResource(R.string.settings_game_glass_tier_title),
                        summary = stringResource(R.string.settings_game_glass_tier_summary),
                        getItemText = { stringResource(it.textRes) }
                    )
                }
            }

            //启动器背景设置板块
            if (backgroundViewModel != null) {
                animatedItem { ctx ->
                    //Liquid Glass manages background opacity/blur itself, see effectiveBackgroundOpacity()
                    val liquidGlassOn = launcherGlassOn()
                    SettingsCardColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animated(ctx)
                    ) {
                        SettingsCard(
                            modifier = Modifier.fillMaxWidth(),
                            position = CardPosition.Top
                        ) {
                            CustomBackground(
                                modifier = Modifier.fillMaxWidth(),
                                backgroundViewModel = backgroundViewModel,
                                submitError = submitError
                            )
                        }

                        IntSliderSettingsCard(
                            modifier = Modifier.fillMaxWidth(),
                            position = CardPosition.Middle,
                            unit = AllSettings.launcherBackgroundOpacity,
                            title = stringResource(R.string.settings_launcher_background_opacity_title),
                            summary = if (liquidGlassOn) stringResource(R.string.settings_launcher_background_liquid_glass_managed)
                            else stringResource(R.string.settings_launcher_background_opacity_summary),
                            valueRange = AllSettings.launcherBackgroundOpacity.floatRange,
                            suffix = "%",
                            enabled = backgroundViewModel.isValid && !liquidGlassOn,
                            fineTuningControl = true
                        )

                        IntSliderSettingsCard(
                            modifier = Modifier.fillMaxWidth(),
                            position = CardPosition.Middle,
                            unit = AllSettings.videoBackgroundVolume,
                            title = stringResource(R.string.settings_launcher_background_video_volume_title),
                            summary = stringResource(R.string.settings_launcher_background_video_volume_summary),
                            valueRange = AllSettings.videoBackgroundVolume.floatRange,
                            suffix = "%",
                            enabled = backgroundViewModel.isValid && backgroundViewModel.isVideo,
                            fineTuningControl = true
                        )

                        IntSliderSettingsCard(
                            modifier = Modifier.fillMaxWidth(),
                            position = CardPosition.Bottom,
                            unit = AllSettings.backgroundBlur,
                            title = stringResource(R.string.settings_title_blur),
                            summary = if (liquidGlassOn) stringResource(R.string.settings_launcher_background_liquid_glass_managed)
                            else stringResource(R.string.settings_launcher_background_blur_summary),
                            valueRange = AllSettings.backgroundBlur.floatRange,
                            suffix = "Dp",
                            enabled = backgroundViewModel.isValid && !liquidGlassOn,
                            fineTuningControl = true,
                            appendContent = {
                                val unit = AllSettings.backgroundBlurType
                                val state = unit.state
                                IconButton(
                                    modifier = Modifier
                                        .padding(start = 12.dp)
                                        .size(32.dp),
                                    colors = IconButtonDefaults.iconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary,
                                        disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = DisabledAlpha),
                                        disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = DisabledAlpha),
                                    ),
                                    onClick = {
                                        unit.save(state.switch())
                                    },
                                    enabled = backgroundViewModel.isValid && !liquidGlassOn,
                                ) {
                                    Crossfade(
                                        targetState = state
                                    ) { target ->
                                        val painter = when (target) {
                                            BackgroundBlur.Background -> painterResource(R.drawable.ic_blur_circular_outlined)
                                            BackgroundBlur.Foreground -> painterResource(R.drawable.ic_blur_circular_filled)
                                        }
                                        Icon(
                                            painter = painter,
                                            contentDescription = null
                                        )
                                    }
                                }
                            }
                        )
                    }
                }
            }

            //Decoration overlay (image/video from the gallery above the UI)
            animatedItem { ctx ->
                OverlaySettings(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animated(ctx),
                    submitError = submitError
                )
            }

            //动画设置板块
            animatedItem { ctx ->
                SettingsCardColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animated(ctx)
                ) {
                    SwitchSettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = CardPosition.Top,
                        unit = AllSettings.launcherSwapAnimEnabled,
                        title = stringResource(R.string.settings_launcher_swap_anim_enabled_title),
                        summary = stringResource(R.string.settings_launcher_swap_anim_enabled_summary)
                    )

                    IntSliderSettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = CardPosition.Bottom,
                        unit = AllSettings.launcherAnimateSpeedLevel,
                        title = stringResource(R.string.settings_launcher_animate_speed_title),
                        summary = stringResource(R.string.settings_launcher_animate_speed_summary),
                        valueRange = AllSettings.launcherAnimateSpeedLevel.floatRange,
                        valueText = { value -> "×" + (value / 10f) },
                        enabled = AllSettings.launcherSwapAnimEnabled.state,
                    )
                }
            }

            animatedItem { ctx ->
                SettingsCardColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animated(ctx)
                ) {
                    //这些镜像源都是为了改善中国大陆内陆的网络环境而存在的
                    //境外不需要这些镜像源，反而可能拖慢境外的下载速度
                    //所以不应该向中国境外开放这些选项
                    val isChinaMainland = remember { isChinaMainland() }
                    if (isChinaMainland) {
                        ListSettingsCard(
                            modifier = Modifier.fillMaxWidth(),
                            position = CardPosition.Top,
                            unit = AllSettings.gameDownloadSource,
                            items = MirrorSourceType.entries,
                            title = stringResource(R.string.settings_launcher_mirror_game_source_title),
                            getItemText = { stringResource(it.textRes) }
                        )

                        ListSettingsCard(
                            modifier = Modifier.fillMaxWidth(),
                            position = CardPosition.Middle,
                            unit = AllSettings.addonPlatformSource,
                            items = MirrorSourceType.entries,
                            title = stringResource(R.string.settings_launcher_mirror_addon_platform_source_title),
                            getItemText = { stringResource(it.textRes) }
                        )
                    }

                    IntSliderSettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = if (isChinaMainland) {
                            CardPosition.Middle
                        } else {
                            CardPosition.Top
                        },
                        unit = AllSettings.launcherLogRetentionDays,
                        title = stringResource(R.string.settings_launcher_log_retention_days_title),
                        summary = stringResource(R.string.settings_launcher_log_retention_days_summary),
                        valueRange = AllSettings.launcherLogRetentionDays.floatRange,
                        suffix = stringResource(R.string.unit_day)
                    )

                    SettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = CardPosition.Bottom,
                        title = stringResource(R.string.settings_launcher_log_share_title),
                        summary = stringResource(R.string.settings_launcher_log_share_summary),
                        onClick = {
                            TaskSystem.submitTask(
                                Task.runTask(
                                    id = "ZIP_LOGS",
                                    task = { task ->
                                        task.updateProgress(-1f)
                                        task.updateMessage(androidText(R.string.settings_launcher_log_share_packing))
                                        val logsFile = File(PathManager.DIR_CACHE, "logs.zip")
                                        Logger.pack(logsFile)
                                        task.updateProgress(1f)
                                        task.updateMessage(null)
                                        //分享压缩包
                                        shareFile(
                                            context = context,
                                            file = logsFile
                                        )
                                    },
                                    onError = { e ->
                                        Logger.error(TAG, "Failed to package log files.", e)
                                    }
                                )
                            )
                        }
                    )
                }
            }
        }
    }
}

/** Glass tiers the user can choose in settings; [GlassTier.OFF] is the Liquid Glass switch itself. */
private val glassTierChoices = listOf(GlassTier.LITE, GlassTier.FULL)

@Composable
private fun OverlaySettings(
    submitError: (ErrorViewModel.ThrowableMessage) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val cards by OverlayCardManager.cards.collectAsStateWithLifecycle()

    val importErrorText = stringResource(R.string.error_import_image)
    val gifUnsupportedText = stringResource(R.string.settings_launcher_overlay_gif_unsupported)
    val importingText = stringResource(R.string.settings_launcher_overlay_importing)
    val filePicker = rememberLauncherForActivityResult(
        contract = MediaPickerContract(
            allowImages = true,
            allowVideos = true,
            allowMultiple = false
        )
    ) { result ->
        if (result != null) {
            TaskSystem.submitTask(
                Task.runTask(
                    dispatcher = Dispatchers.IO,
                    task = { task ->
                        task.updateMessage(androidText(importingText))
                        OverlayCardManager.addFromUri(context, result[0], gifUnsupportedText)
                    },
                    onError = { th ->
                        submitError(
                            ErrorViewModel.ThrowableMessage(
                                title = androidText(importErrorText),
                                message = androidText(th.getMessageOrToString())
                            )
                        )
                    }
                )
            )
        }
    }

    SettingsCardColumn(modifier = modifier) {
        SettingsCard(
            modifier = Modifier.fillMaxWidth(),
            position = CardPosition.Top,
            title = stringResource(R.string.settings_launcher_overlay_import),
            summary = stringResource(R.string.settings_launcher_overlay_import_summary),
            onClick = { filePicker.launch(Unit) }
        )

        SettingsCard(
            modifier = Modifier.fillMaxWidth(),
            position = CardPosition.Bottom,
            title = stringResource(R.string.settings_launcher_overlay_remove_all),
            summary = if (cards.isEmpty()) stringResource(R.string.settings_launcher_overlay_none) else null,
            enabled = cards.isNotEmpty(),
            onClick = { OverlayCardManager.removeAll() }
        )
    }
}

private sealed interface BackgroundOperation {
    data object None : BackgroundOperation
    data object PreReset : BackgroundOperation
    data object Reset : BackgroundOperation
}

@Composable
private fun CustomBackground(
    backgroundViewModel: BackgroundViewModel,
    submitError: (ErrorViewModel.ThrowableMessage) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var operation by remember { mutableStateOf<BackgroundOperation>(BackgroundOperation.None) }

    BackgroundOperation(
        operation = operation,
        changeOperation = { operation = it },
        backgroundViewModel = backgroundViewModel
    )

    val importErrorText = stringResource(R.string.error_import_image)
    val filePicker = rememberLauncherForActivityResult(
        contract = MediaPickerContract(
            allowImages = true,
            allowVideos = true,
            allowMultiple = false
        )
    ) { result ->
        if (result != null) {
            TaskSystem.submitTask(
                Task.runTask(
                    dispatcher = Dispatchers.IO,
                    task = { task ->
                        task.updateMessage(androidText(R.string.settings_launcher_background_importing))
                        backgroundViewModel.import(context, result[0] /* 取决于上面的allowMultiple，此处一定会是单个元素的列表 */)
                    },
                    onError = { th ->
                        backgroundViewModel.delete()
                        submitError(
                            ErrorViewModel.ThrowableMessage(
                                title = androidText(importErrorText),
                                message = androidText(th.getMessageOrToString())
                            )
                        )
                    }
                )
            )
        }
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable { filePicker.launch(Unit) }
                .padding(all = 16.dp),
        ) {
            TitleAndSummary(
                title = stringResource(R.string.settings_launcher_background_title),
                summary = stringResource(R.string.settings_launcher_background_summary),
            )
        }

        AnimatedVisibility(
            modifier = Modifier.padding(horizontal = 16.dp),
            visible = backgroundViewModel.isValid
        ) {
            IconTextButton(
                painter = painterResource(R.drawable.ic_restart_alt),
                text = stringResource(R.string.generic_reset),
                onClick = {
                    if (operation == BackgroundOperation.None) {
                        operation = BackgroundOperation.PreReset
                    }
                }
            )
        }
    }
}

@Composable
private fun BackgroundOperation(
    operation: BackgroundOperation,
    changeOperation: (BackgroundOperation) -> Unit,
    backgroundViewModel: BackgroundViewModel
) {
    when (operation) {
        is BackgroundOperation.None -> {}
        is BackgroundOperation.PreReset -> {
            SimpleAlertDialog(
                title = stringResource(R.string.generic_reset),
                text = stringResource(R.string.settings_launcher_background_reset_message),
                onConfirm = {
                    changeOperation(BackgroundOperation.Reset)
                },
                onDismiss = {
                    changeOperation(BackgroundOperation.None)
                }
            )
        }
        is BackgroundOperation.Reset -> {
            LaunchedEffect(Unit) {
                backgroundViewModel.delete()
                changeOperation(BackgroundOperation.None)
            }
        }
    }
}