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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import com.movtery.zalithlauncher.ui.components.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.multirt.RuntimesManager
import com.movtery.zalithlauncher.game.plugin.natives.NativePlugin
import com.movtery.zalithlauncher.game.plugin.natives.NativePluginManager
import com.movtery.zalithlauncher.path.URL_CLOUD_NATIVE_LIB_PLUGINS
import com.movtery.zalithlauncher.path.URL_GITHUB_NATIVE_LIB_PLUGINS
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.setting.unit.floatRange
import com.movtery.zalithlauncher.setting.unit.min
import com.movtery.zalithlauncher.ui.base.BaseScreen
import com.movtery.zalithlauncher.ui.components.GlassLazyColumn
import com.movtery.zalithlauncher.ui.components.animated
import com.movtery.zalithlauncher.ui.components.lazyScrollWithBar
import com.movtery.zalithlauncher.ui.screens.NestedNavKey
import com.movtery.zalithlauncher.ui.screens.NormalNavKey
import com.movtery.zalithlauncher.ui.screens.TitledNavKey
import com.movtery.zalithlauncher.ui.screens.content.elements.MemoryPreview
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.CardPosition
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.IntSliderSettingsCard
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.ListSettingsCard
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.SettingsCard
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.StringListSettingsCard
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.SwitchSettingsCard
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.TextInputSettingsCard
import com.movtery.zalithlauncher.utils.platform.getMaxMemoryForSettings
import com.movtery.zalithlauncher.viewmodel.EventViewModel
import com.movtery.zalithlauncher.viewmodel.sendDLPlugin

/** Gap between two card groups, on top of the 2dp the list already puts between any two cards. */
private val GroupGap = 8.dp

@Composable
fun GameSettingsScreen(
    key: NestedNavKey.Settings,
    settingsScreenKey: TitledNavKey?,
    mainScreenKey: TitledNavKey?,
    eventViewModel: EventViewModel
) {
    BaseScreen(
        Triple(key, mainScreenKey, false),
        Triple(NormalNavKey.Settings.Game, settingsScreenKey, false)
    ) { isVisible ->
        //Every card is a glass surface: each card is its own lazy item, so only the cards near the
        //viewport are composed and blurred while scrolling.
        val listState = rememberLazyListState()
        //Read once, like before: they decide which cards exist and where they sit in their group
        val runtimes = remember { RuntimesManager.getRuntimes() }
        val nativePlugins = remember { NativePluginManager.getPlugins() }

        GlassLazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .lazyScrollWithBar(listState),
            state = listState,
            isVisible = isVisible,
            verticalArrangement = Arrangement.spacedBy(2.dp),
            contentPadding = PaddingValues(all = 12.dp)
        ) {
            /** A card of a group. The whole group shares [group]'s entrance delay, like the old per-group stagger. */
            fun card(itemKey: String, group: Int, content: @Composable () -> Unit) {
                animatedItem(key = itemKey, delay = group * 50) { ctx ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animated(ctx)
                    ) {
                        content()
                    }
                }
            }

            fun groupGap(itemKey: String) {
                item(key = itemKey, contentType = "gap") { Spacer(modifier = Modifier.height(GroupGap)) }
            }

            //Group 0: version
            card("version_isolation", 0) {
                SwitchSettingsCard(
                    modifier = Modifier.fillMaxWidth(),
                    position = CardPosition.Top,
                    unit = AllSettings.versionIsolation,
                    title = stringResource(R.string.settings_game_version_isolation_title),
                    summary = stringResource(R.string.settings_game_version_isolation_summary)
                )
            }
            card("skip_integrity_check", 0) {
                SwitchSettingsCard(
                    modifier = Modifier.fillMaxWidth(),
                    position = CardPosition.Middle,
                    unit = AllSettings.skipGameIntegrityCheck,
                    title = stringResource(R.string.settings_game_skip_game_integrity_check_title),
                    summary = stringResource(R.string.settings_game_skip_game_integrity_check_summary)
                )
            }
            card("version_custom_info", 0) {
                TextInputSettingsCard(
                    modifier = Modifier.fillMaxWidth(),
                    position = CardPosition.Bottom,
                    unit = AllSettings.versionCustomInfo,
                    title = stringResource(R.string.settings_game_version_custom_info_title),
                    summary = stringResource(R.string.settings_game_version_custom_info_summary)
                )
            }
            groupGap("gap_0")

            //Group 1: Java, memory and JVM
            if (runtimes.isNotEmpty()) {
                card("java_runtime", 1) {
                    ListSettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = CardPosition.Top,
                        unit = AllSettings.javaRuntime,
                        items = RuntimesManager.getRuntimes().filter { it.isCompatible() },
                        title = stringResource(R.string.settings_game_java_runtime_title),
                        summary = stringResource(R.string.settings_game_java_runtime_summary),
                        getItemText = { it.name },
                        getItemId = { it.name }
                    )
                }
            }

            card("auto_pick_java_runtime", 1) {
                SwitchSettingsCard(
                    modifier = Modifier.fillMaxWidth(),
                    position = if (runtimes.isNotEmpty()) CardPosition.Middle else CardPosition.Top,
                    unit = AllSettings.autoPickJavaRuntime,
                    title = stringResource(R.string.settings_game_auto_pick_java_runtime_title),
                    summary = stringResource(R.string.settings_game_auto_pick_java_runtime_summary)
                )
            }

            card("native_lib_plugins", 1) {
                if (nativePlugins.isNotEmpty()) {
                    StringListSettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = CardPosition.Middle,
                        unit = AllSettings.disableNativeLibPlugins,
                        items = nativePlugins,
                        onItemsChange = { value, item ->
                            if (value) {
                                this - item.packageName
                            } else {
                                this + item.packageName
                            }
                        },
                        title = stringResource(R.string.settings_game_native_lib_plugin_title),
                        summary = stringResource(R.string.settings_game_native_lib_plugin_summary),
                        getItemID = { it.packageName },
                        getItemText = { it.displayName },
                        getItemSummary = { plugin ->
                            NativePluginSummaryLayout(plugin)
                        },
                        getItemCheck = { contains -> !contains },
                        trailingIcon = {
                            DLNativeLibsButton(eventViewModel)
                        },
                    )
                } else {
                    SettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = CardPosition.Middle,
                        title = stringResource(R.string.settings_game_native_lib_plugin_title),
                        summary = stringResource(R.string.settings_game_native_lib_plugin_summary),
                        onClick = {},
                        trailingIcon = {
                            DLNativeLibsButton(eventViewModel)
                        }
                    )
                }
            }

            card("java_memory", 1) {
                IntSliderSettingsCard(
                    modifier = Modifier.fillMaxWidth(),
                    position = CardPosition.Middle,
                    unit = AllSettings.ramAllocation,
                    title = stringResource(R.string.settings_game_java_memory_title),
                    summary = stringResource(R.string.settings_game_java_memory_summary),
                    valueRange = AllSettings.ramAllocation.floatRange.start..getMaxMemoryForSettings(LocalContext.current).toFloat(),
                    suffix = "MB",
                    fineTuningControl = true,
                    previewContent = {
                        MemoryPreview(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 2.dp),
                            preview = (AllSettings.ramAllocation.state ?: AllSettings.ramAllocation.min).toDouble(),
                            usedText = { usedMemory, totalMemory ->
                                stringResource(R.string.settings_game_java_memory_used_text, usedMemory.toInt(), totalMemory.toInt())
                            },
                            previewText = { preview ->
                                stringResource(R.string.settings_game_java_memory_allocation_text, preview.toInt())
                            }
                        )
                    }
                )
            }

            card("jvm_args", 1) {
                TextInputSettingsCard(
                    modifier = Modifier.fillMaxWidth(),
                    position = CardPosition.Bottom,
                    unit = AllSettings.jvmArgs,
                    title = stringResource(R.string.settings_game_jvm_args_title),
                    summary = stringResource(R.string.settings_game_jvm_args_summary)
                )
            }
            groupGap("gap_1")

            //Group 2: log
            card("show_log_automatic", 2) {
                SwitchSettingsCard(
                    modifier = Modifier.fillMaxWidth(),
                    position = CardPosition.Top,
                    unit = AllSettings.showLogAutomatic,
                    title = stringResource(R.string.settings_game_show_log_automatic_title),
                    summary = stringResource(R.string.settings_game_show_log_automatic_summary)
                )
            }
            card("log_text_size", 2) {
                IntSliderSettingsCard(
                    modifier = Modifier.fillMaxWidth(),
                    position = CardPosition.Middle,
                    unit = AllSettings.logTextSize,
                    title = stringResource(R.string.settings_game_log_text_size_title),
                    summary = stringResource(R.string.settings_game_log_text_size_summary),
                    valueRange = AllSettings.logTextSize.floatRange,
                    suffix = "Sp",
                    fineTuningControl = true
                )
            }
            card("log_buffer_flush_interval", 2) {
                IntSliderSettingsCard(
                    modifier = Modifier.fillMaxWidth(),
                    position = CardPosition.Bottom,
                    unit = AllSettings.logBufferFlushInterval,
                    title = stringResource(R.string.settings_game_log_buffer_flush_interval_title),
                    summary = stringResource(R.string.settings_game_log_buffer_flush_interval_summary),
                    valueRange = AllSettings.logBufferFlushInterval.floatRange,
                    suffix = "ms",
                    fineTuningControl = true
                )
            }
        }
    }
}

/** Opens the native lib plugin download dialog. */
@Composable
private fun DLNativeLibsButton(eventViewModel: EventViewModel) {
    IconButton(
        onClick = {
            eventViewModel.sendDLPlugin(
                githubLink = URL_GITHUB_NATIVE_LIB_PLUGINS,
                cloudDrives = listOf(
                    EventViewModel.Event.DownloadPlugins.CloudDrive(
                        language = "zh",
                        link = URL_CLOUD_NATIVE_LIB_PLUGINS
                    )
                )
            )
        }
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_download_2_filled),
            contentDescription = stringResource(R.string.generic_download)
        )
    }
}

@Composable
private fun NativePluginSummaryLayout(plugin: NativePlugin) {
    FlowRow(
        modifier = Modifier.alpha(0.7f),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.settings_renderer_from_plugins,plugin.appName),
            style = MaterialTheme.typography.labelSmall
        )

        val minVer = plugin.minMCVer
        val maxVer = plugin.maxMCVer

        if (minVer != null || maxVer != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(text = stringResource(R.string.renderer_version_support), style = MaterialTheme.typography.labelSmall)

                minVer?.let {
                    Text(text = ">= $it", style = MaterialTheme.typography.labelSmall)
                }

                maxVer?.let {
                    Text(text = "<= $it", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}