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

package com.movtery.zalithlauncher.ui.screens.content.versions

import android.os.Build
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import com.movtery.zalithlauncher.ui.theme.profileWallpaperFile
import com.movtery.zalithlauncher.ui.theme.ProfileTheme
import com.movtery.zalithlauncher.ui.components.ColorPickerDialog
import com.movtery.zalithlauncher.setting.enums.GlassTier
import com.movtery.zalithlauncher.game.version.installed.theme.ProfileThemeIO
import com.movtery.zalithlauncher.game.version.installed.theme.PROFILE_THEME_EXTENSION
import com.movtery.zalithlauncher.contract.MediaPickerContract
import com.movtery.colorpicker.rememberColorPickerController
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import android.os.Vibrator
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Icon
import com.movtery.zalithlauncher.ui.components.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.content.getSystemService
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.control.ControlManager
import com.movtery.zalithlauncher.game.multirt.RuntimesManager
import com.movtery.zalithlauncher.game.plugin.driver.DriverPluginManager
import com.movtery.zalithlauncher.game.plugin.renderer_v2.RendererV2Data
import com.movtery.zalithlauncher.game.plugin.renderer_v2.data.EnvSettingUnit
import com.movtery.zalithlauncher.game.plugin.renderer_v2.data.VersionEnvStorage
import com.movtery.zalithlauncher.game.renderer.Renderers
import com.movtery.zalithlauncher.game.support.touch_controller.VibrationHandler
import com.movtery.zalithlauncher.game.version.installed.GraphicsApi
import com.movtery.zalithlauncher.game.version.installed.Version
import com.movtery.zalithlauncher.game.version.installed.VersionConfig
import com.movtery.zalithlauncher.ui.screens.content.settings.RendererV2ConfigDialog
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.setting.unit.floatRange
import com.movtery.zalithlauncher.setting.unit.getOrMin
import com.movtery.zalithlauncher.ui.AndroidStringText
import com.movtery.zalithlauncher.ui.androidText
import com.movtery.zalithlauncher.ui.base.BaseScreen
import com.movtery.zalithlauncher.ui.components.AnimatedColumn
import com.movtery.zalithlauncher.ui.components.IDItem
import com.movtery.zalithlauncher.ui.components.SimpleAlertDialog
import com.movtery.zalithlauncher.ui.components.verticalScrollWithBar
import com.movtery.zalithlauncher.ui.screens.NestedNavKey
import com.movtery.zalithlauncher.ui.screens.NormalNavKey
import com.movtery.zalithlauncher.ui.screens.TitledNavKey
import com.movtery.zalithlauncher.ui.screens.content.elements.MemoryPreview
import com.movtery.zalithlauncher.ui.screens.content.elements.MicrophoneCheckOperation
import com.movtery.zalithlauncher.ui.screens.content.elements.MicrophoneCheckState
import com.movtery.zalithlauncher.ui.screens.content.settings.DriverSummaryLayout
import com.movtery.zalithlauncher.ui.screens.content.settings.RendererSummaryLayout
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.CardPosition
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.EnumSettingsCard
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.IntSliderSettingsCard
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.ListSettingsCard
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.SettingsCard
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.SettingsCardColumn
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.SimpleIDListCard
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.TextInputSettingsCard
import com.movtery.zalithlauncher.ui.screens.content.versions.layouts.StatefulDropdownMenuFollowGlobal
import com.movtery.zalithlauncher.ui.screens.content.versions.layouts.ToggleableIntSliderSettingsCard
import com.movtery.zalithlauncher.utils.logging.Logger
import com.movtery.zalithlauncher.utils.platform.getMaxMemoryForSettings
import com.movtery.zalithlauncher.utils.string.getMessageOrToString
import com.movtery.zalithlauncher.viewmodel.ErrorViewModel

private const val TAG = "VersionConfigScreen"

@Composable
fun VersionConfigScreen(
    mainScreenKey: TitledNavKey?,
    versionsScreenKey: TitledNavKey?,
    version: Version,
    backToMainScreen: () -> Unit,
    onCheckVulkan: (Version) -> Unit,
    showToast: (AndroidStringText) -> Unit,
    submitError: (ErrorViewModel.ThrowableMessage) -> Unit
) {
    if (!version.isValid()) {
        backToMainScreen()
        return
    }

    BaseScreen(
        levels1 = listOf(
            Pair(NestedNavKey.VersionSettings::class.java, mainScreenKey)
        ),
        Triple(NormalNavKey.Versions.Config, versionsScreenKey, false)
    ) { isVisible ->
        val config = version.getVersionConfig()

        AnimatedColumn(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScrollWithBar(state = rememberScrollState())
                .padding(all = 12.dp),
            isVisible = isVisible
        ) { scope ->
            AnimatedItem(scope) { yOffset ->
                VersionConfigs(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset { IntOffset(x = 0, y = yOffset.roundToPx()) },
                    config = config,
                    submitError = submitError
                )
            }

            AnimatedItem(scope) { yOffset ->
                ProfileThemeConfigs(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset { IntOffset(x = 0, y = yOffset.roundToPx()) },
                    version = version,
                    config = config,
                    showToast = showToast,
                    submitError = submitError
                )
            }

            AnimatedItem(scope) { yOffset ->
                GameConfigs(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset { IntOffset(x = 0, y = yOffset.roundToPx()) },
                    config = config,
                    submitError = submitError
                )
            }

            AnimatedItem(scope) { yOffset ->
                SupportConfigs(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset { IntOffset(x = 0, y = yOffset.roundToPx()) },
                    config = config,
                    onCheckVulkan = {
                        onCheckVulkan(version)
                    },
                    showToast = showToast,
                    submitError = submitError
                )
            }
        }
    }
}

@Composable
private fun VersionConfigs(
    config: VersionConfig,
    submitError: (ErrorViewModel.ThrowableMessage) -> Unit,
    modifier: Modifier = Modifier
) {
    SettingsCardColumn(
        modifier = modifier
    ) {
        Text(
            modifier = Modifier.padding(all = 8.dp),
            text = stringResource(R.string.versions_config_version_settings),
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelLarge
        )

        StatefulDropdownMenuFollowGlobal(
            modifier = Modifier.fillMaxWidth(),
            position = CardPosition.Top,
            currentValue = config.isolationType,
            onValueChange = { type ->
                if (config.isolationType != type) {
                    config.isolationType = type
                    config.saveOrShowError(submitError)
                }
            },
            title = stringResource(R.string.versions_config_isolation_title),
            summary = stringResource(R.string.versions_config_isolation_summary)
        )

        StatefulDropdownMenuFollowGlobal(
            modifier = Modifier.fillMaxWidth(),
            position = CardPosition.Middle,
            currentValue = config.skipGameIntegrityCheck,
            onValueChange = { type ->
                if (config.skipGameIntegrityCheck != type) {
                    config.skipGameIntegrityCheck = type
                    config.saveOrShowError(submitError)
                }
            },
            title = stringResource(R.string.settings_game_skip_game_integrity_check_title),
            summary = stringResource(R.string.settings_game_skip_game_integrity_check_summary)
        )

        val renderers = Renderers.getRenderers()
        val renderersIdList = getIDList(renderers) { IDItem(it.getUniqueIdentifier(), it.getRendererName()) }
        var currentRendererId by remember { mutableStateOf(config.renderer) }
        val v2Plugin = remember(currentRendererId) {
            if (currentRendererId.isNotEmpty()) {
                renderers.filterIsInstance<RendererV2Data>()
                    .find { it.getUniqueIdentifier() == currentRendererId }
                    ?.takeIf { it.env.getConfigurableUnits().isNotEmpty() }
            } else null
        }
        var showV2ConfigDialog by remember { mutableStateOf(false) }
        var showResetGlobalDialog by remember { mutableStateOf(false) }
        var v2PluginEnvUnits by remember { mutableStateOf<List<EnvSettingUnit>?>(null) }

        ListSettingsCard(
            modifier = Modifier.fillMaxWidth(),
            position = CardPosition.Middle,
            items = renderersIdList,
            currentId = currentRendererId,
            defaultId = "",
            title = stringResource(R.string.versions_config_renderer),
            getItemText = { it.title },
            getItemId = { it.id },
            getItemSummary = { item ->
                renderers.find { it.getUniqueIdentifier() == item.id }?.let { renderer ->
                    RendererSummaryLayout(renderer)
                }
            },
            trailingIcon = {
                // 仅当独立指定了具有可配置项的新一代渲染器时提供配置入口；设置跟随全局时不能独立修改
                if (v2Plugin != null) {
                    IconButton(
                        onClick = {
                            val storage = VersionEnvStorage(config.getVersionPath())
                            v2PluginEnvUnits = v2Plugin.env.createConfigurableUnits(storage)
                            showV2ConfigDialog = true
                        }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_settings_filled),
                            contentDescription = stringResource(R.string.settings_renderer_config_title)
                        )
                    }
                }
            },
            onValueChange = { item ->
                if (config.renderer != item.id) {
                    config.renderer = item.id
                    currentRendererId = item.id
                    config.saveOrShowError(submitError)
                }
            }
        )

        // 新一代渲染器插件在当前版本中的独立环境变量配置对话框
        if (showV2ConfigDialog && v2PluginEnvUnits != null) {
            RendererV2ConfigDialog(
                units = v2PluginEnvUnits!!,
                onDismissRequest = {
                    showV2ConfigDialog = false
                    v2PluginEnvUnits = null
                },
                onResetToGlobal = { showResetGlobalDialog = true }
            )
        }

        if (showResetGlobalDialog) {
            val rendererName = v2Plugin?.getRendererName().orEmpty()
            SimpleAlertDialog(
                title = stringResource(R.string.versions_config_renderer_reset_global),
                text = stringResource(R.string.versions_config_renderer_reset_global_message, rendererName),
                onConfirm = {
                    showResetGlobalDialog = false
                    // 删除整个独立配置文件，放弃本版本的全部独立环境变量配置
                    VersionEnvStorage(config.getVersionPath()).delete()
                    showV2ConfigDialog = false
                    v2PluginEnvUnits = null
                },
                onDismiss = { showResetGlobalDialog = false }
            )
        }

        val drivers = DriverPluginManager.getDriverList()
        val driversIdList = getIDList(drivers) { IDItem(it.id, it.name) }
        ListSettingsCard(
            modifier = Modifier.fillMaxWidth(),
            position = CardPosition.Middle,
            items = driversIdList,
            currentId = config.driver,
            defaultId = "",
            title = stringResource(R.string.versions_config_vulkan_driver),
            getItemText = { it.title },
            getItemId = { it.id },
            getItemSummary = { item ->
                drivers.find { it.id == item.id }?.let { driver ->
                    DriverSummaryLayout(driver)
                }
            },
            onValueChange = { item ->
                if (config.driver != item.id) {
                    config.driver = item.id
                    config.saveOrShowError(submitError)
                }
            }
        )

        val graphicsApis = GraphicsApi.entries
        val defaultGraphicsTitle = stringResource(R.string.settings_game_graphics_api_default)
        val defaultOpenGLTitle = stringResource(R.string.settings_game_graphics_api_default_opengl)
        val graphicsApisList = getIDList(graphicsApis) {
            val title = when (it) {
                GraphicsApi.DEFAULT -> defaultGraphicsTitle
                GraphicsApi.DEFAULT_OPENGL -> defaultOpenGLTitle
                else -> it.displayName
            }
            IDItem(it.name, title)
        }
        ListSettingsCard(
            modifier = Modifier.fillMaxWidth(),
            position = CardPosition.Middle,
            items = graphicsApisList,
            currentId = config.graphicsApi?.name ?: "",
            defaultId = "",
            title = stringResource(R.string.settings_game_graphics_api_title),
            summary = stringResource(R.string.settings_game_graphics_api_summary),
            getItemText = { it.title },
            getItemId = { it.id },
            onValueChange = { item ->
                if (config.graphicsApi?.name != item.id) {
                    config.graphicsApi = GraphicsApi.entries.find { it.name == item.id }
                    config.saveOrShowError(submitError)
                }
            }
        )

        val controls by ControlManager.dataList.collectAsStateWithLifecycle()
        val controlsIdList = getIDList(controls.filter { it.isSupport }) {
            IDItem(it.file.name, it.controlLayout.info.name.translate())
        }
        ListSettingsCard(
            modifier = Modifier.fillMaxWidth(),
            position = CardPosition.Bottom,
            items = controlsIdList,
            currentId = config.control,
            defaultId = "",
            title = stringResource(R.string.versions_config_control),
            getItemText = { it.title },
            getItemId = { it.id },
            onValueChange = {
                if (config.control != it.id) {
                    config.control = it.id
                    config.saveOrShowError(submitError)
                }
            }
        )
    }
}

@Composable
private fun GameConfigs(
    config: VersionConfig,
    submitError: (ErrorViewModel.ThrowableMessage) -> Unit,
    modifier: Modifier = Modifier
) {
    SettingsCardColumn(
        modifier = modifier
    ) {
        Text(
            modifier = Modifier.padding(all = 8.dp),
            text = stringResource(R.string.versions_config_game_settings),
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelLarge
        )

        SimpleIDListCard(
            modifier = Modifier.fillMaxWidth(),
            position = CardPosition.Top,
            items = getIDList(RuntimesManager.getRuntimes().filter { it.isCompatible() }) { IDItem(it.name, it.name) },
            currentId = config.javaRuntime,
            defaultId = "",
            title = stringResource(R.string.settings_game_java_runtime_title),
            summary = stringResource(R.string.versions_config_java_runtime_summary),
            onValueChange = { item ->
                if (config.javaRuntime != item.id) {
                    config.javaRuntime = item.id
                    config.saveOrShowError(submitError)
                }
            }
        )

        /**
         * 临时已分配内存，用于UI状态更新
         */
        var ramAllocation by remember { mutableIntStateOf(config.ramAllocation) }
        ToggleableIntSliderSettingsCard(
            modifier = Modifier.fillMaxWidth(),
            position = CardPosition.Middle,
            currentValue = config.ramAllocation,
            valueRange = AllSettings.ramAllocation.floatRange.start..getMaxMemoryForSettings(LocalContext.current).toFloat(),
            defaultValue = AllSettings.ramAllocation.getOrMin(),
            title = stringResource(R.string.settings_game_java_memory_title),
            summary = stringResource(R.string.settings_game_java_memory_summary),
            suffix = "MB",
            onValueChange = {
                config.ramAllocation = it
                ramAllocation = it
            },
            onValueChangeFinished = { config.saveOrShowError(submitError) },
            previewContent = {
                AnimatedVisibility(
                    modifier = Modifier.fillMaxWidth(),
                    visible = ramAllocation >= 256
                ) {
                    MemoryPreview(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 2.dp, end = 8.dp),
                        preview = ramAllocation.takeIf { it >= 256 }?.toDouble(),
                        usedText = { usedMemory, totalMemory ->
                            stringResource(R.string.settings_game_java_memory_used_text, usedMemory.toInt(), totalMemory.toInt())
                        },
                        previewText = { preview ->
                            stringResource(R.string.settings_game_java_memory_allocation_text, preview.toInt())
                        }
                    )
                }
            }
        )

        var customInfo by remember { mutableStateOf(config.customInfo) }
        TextInputSettingsCard(
            modifier = Modifier.fillMaxWidth(),
            position = CardPosition.Middle,
            value = customInfo,
            onValueChange = { value ->
                customInfo = value
                if (config.customInfo != value) {
                    config.customInfo = value
                    config.saveOrShowError(submitError)
                }
            },
            title = stringResource(R.string.settings_game_version_custom_info_title),
            summary = stringResource(R.string.settings_game_version_custom_info_summary),
            label = {
                Text(text = stringResource(R.string.versions_config_follow_global_if_blank))
            }
        )

        var gameArgs by remember { mutableStateOf(config.gameArgs) }
        TextInputSettingsCard(
            modifier = Modifier.fillMaxWidth(),
            position = CardPosition.Middle,
            value = gameArgs,
            title = stringResource(R.string.versions_config_game_args_title),
            summary = stringResource(R.string.versions_config_game_args_summary),
            onValueChange = { value ->
                gameArgs = value
                if (config.gameArgs != value) {
                    config.gameArgs = value
                    config.saveOrShowError(submitError)
                }
            },
            label = {
                Text(text = stringResource(R.string.versions_config_disable_if_blank))
            }
        )

        var jvmArgs by remember { mutableStateOf(config.jvmArgs) }
        TextInputSettingsCard(
            modifier = Modifier.fillMaxWidth(),
            position = CardPosition.Middle,
            value = jvmArgs,
            title = stringResource(R.string.settings_game_jvm_args_title),
            summary = stringResource(R.string.settings_game_jvm_args_summary),
            onValueChange = { value ->
                jvmArgs = value
                if (config.jvmArgs != value) {
                    config.jvmArgs = value
                    config.saveOrShowError(submitError)
                }
            },
            label = {
                Text(text = stringResource(R.string.versions_config_follow_global_if_blank))
            }
        )

        var serverIp by remember { mutableStateOf(config.serverIp) }
        TextInputSettingsCard(
            modifier = Modifier.fillMaxWidth(),
            position = CardPosition.Bottom,
            value = serverIp,
            title = stringResource(R.string.versions_config_auto_join_server_ip_title),
            summary = stringResource(R.string.versions_config_auto_join_server_ip_summary),
            onValueChange = { value ->
                serverIp = value
                if (config.serverIp != value) {
                    config.serverIp = value
                    config.saveOrShowError(submitError)
                }
            },
            label = {
                Text(text = stringResource(R.string.versions_config_disable_if_blank))
            }
        )
    }
}

@Composable
private fun SupportConfigs(
    config: VersionConfig,
    onCheckVulkan: () -> Unit,
    showToast: (AndroidStringText) -> Unit,
    submitError: (ErrorViewModel.ThrowableMessage) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    SettingsCardColumn(modifier = modifier) {
        Text(
            modifier = Modifier.padding(all = 8.dp),
            text = stringResource(R.string.versions_config_support_settings),
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelLarge
        )

        val vibrator = remember(context) { context.getSystemService<Vibrator>() }
        var touchVibrateKind by remember { mutableStateOf(config.touchVibrateKind) }
        val effectiveVibrateKind = touchVibrateKind ?: VibrationHandler.VibrateKind.default
        var touchVibrateDuration by remember { mutableIntStateOf(config.touchVibrateDuration) }
        EnumSettingsCard(
            modifier = Modifier.fillMaxWidth(),
            position = CardPosition.Top,
            value = effectiveVibrateKind,
            title = stringResource(R.string.versions_config_vibrate_kind_title),
            summary = stringResource(R.string.versions_config_vibrate_kind_summary),
            entries = VibrationHandler.VibrateKind.entries,
            getRadioEnable = { enum ->
                if (enum == VibrationHandler.VibrateKind.ONE_SHOT) {
                    true
                } else {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                }
            },
            getRadioText = { enum ->
                when (enum) {
                    VibrationHandler.VibrateKind.ONE_SHOT -> stringResource(R.string.vibrate_kind_one_shot)
                    VibrationHandler.VibrateKind.CLICK -> stringResource(R.string.vibrate_kind_click)
                    VibrationHandler.VibrateKind.DOUBLE_CLICK -> stringResource(R.string.vibrate_kind_double_click)
                    VibrationHandler.VibrateKind.HEAVY_CLICK -> stringResource(R.string.vibrate_kind_heavy_click)
                    VibrationHandler.VibrateKind.TICK -> stringResource(R.string.vibrate_kind_tick)
                }
            },
            maxItemsInEachRow = 4,
            onRadioClick = { enum ->
                touchVibrateKind = enum
                vibrator?.let { vibrator ->
                    VibrationHandler.vibrate(
                        vibrator = vibrator,
                        vibrateDuration = touchVibrateDuration,
                        vibrateKind = touchVibrateKind,
                    )
                }
                if (config.touchVibrateKind != enum) {
                    config.touchVibrateKind = enum
                    config.saveOrShowError(submitError)
                }
            }
        )

        AnimatedVisibility(effectiveVibrateKind == VibrationHandler.VibrateKind.ONE_SHOT) {
            IntSliderSettingsCard(
                modifier = Modifier.fillMaxWidth(),
                position = CardPosition.Middle,
                value = touchVibrateDuration,
                title = stringResource(R.string.versions_config_vibrate_duration_title),
                summary = stringResource(R.string.versions_config_vibrate_duration_summary),
                valueRange = 80f..500f,
                onValueChange = {
                    touchVibrateDuration = it
                    config.touchVibrateDuration = touchVibrateDuration
                },
                onValueChangeFinished = {
                    vibrator?.let { vibrator ->
                        VibrationHandler.vibrate(
                            vibrator = vibrator,
                            vibrateDuration = touchVibrateDuration,
                            vibrateKind = touchVibrateKind,
                        )
                    }
                    config.saveOrShowError(submitError)
                },
                suffix = "ms",
                fineTuningControl = true
            )
        }

        SettingsCard(
            modifier = Modifier.fillMaxWidth(),
            position = CardPosition.Middle,
            title = stringResource(R.string.game_vulkan_check_title),
            summary = stringResource(R.string.game_vulkan_check_text),
            onClick = onCheckVulkan
        )

        //检查麦克风
        var microphoneState by remember { mutableStateOf<MicrophoneCheckState>(MicrophoneCheckState.None) }
        MicrophoneCheckOperation(
            state = microphoneState,
            changeState = { microphoneState = it },
            onShowToast = showToast,
        )
        SettingsCard(
            modifier = Modifier.fillMaxWidth(),
            position = CardPosition.Bottom,
            title = stringResource(R.string.versions_config_microphone_check_title),
            summary = stringResource(R.string.versions_config_microphone_check_summary),
            onClick = {
                microphoneState = MicrophoneCheckState.Start
            }
        )
    }
}

@Composable
private fun <E> getIDList(list: List<E>, toIDItem: (E) -> IDItem): List<IDItem> {
    return list.map {
        toIDItem(it)
    }.toMutableList().apply {
        add(0, IDItem("", stringResource(R.string.generic_follow_global)))
    }
}

/**
 * Per-profile theme: accent color, glass tier and wallpaper that apply to the launcher while this
 * version is selected, plus export/import of the whole theme as one file.
 */
@Composable
private fun ProfileThemeConfigs(
    version: Version,
    config: VersionConfig,
    showToast: (AndroidStringText) -> Unit,
    submitError: (ErrorViewModel.ThrowableMessage) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var accent by remember { mutableStateOf(config.themeAccent) }
    var tierId by remember { mutableStateOf(config.themeGlassTier?.name ?: "") }
    var hasWallpaper by remember { mutableStateOf(profileWallpaperFile(version).isFile) }
    var showColorDialog by remember { mutableStateOf(false) }

    fun saveTheme() {
        config.saveOrShowError(submitError)
        ProfileTheme.notifyChanged()
    }

    fun syncFromConfig() {
        accent = config.themeAccent
        tierId = config.themeGlassTier?.name ?: ""
        hasWallpaper = profileWallpaperFile(version).isFile
    }

    fun runThemeTask(successText: Int?, block: suspend () -> Unit) {
        coroutineScope.launch {
            try {
                block()
                syncFromConfig()
                saveTheme()
                successText?.let { showToast(androidText(it)) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.error(TAG, "Profile theme operation failed!", e)
                syncFromConfig()
                saveTheme()
                submitError(
                    ErrorViewModel.ThrowableMessage(
                        title = androidText(R.string.versions_config_theme_error),
                        message = androidText(e.getMessageOrToString())
                    )
                )
            }
        }
    }

    val wallpaperPicker = rememberLauncherForActivityResult(
        contract = MediaPickerContract(allowImages = true, allowVideos = true, allowMultiple = false)
    ) { result ->
        result?.firstOrNull()?.let { uri ->
            runThemeTask(null) { ProfileThemeIO.importWallpaper(context, version, uri) }
        }
    }
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        uri?.let {
            runThemeTask(R.string.versions_config_theme_exported) { ProfileThemeIO.export(context, version, it) }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            runThemeTask(R.string.versions_config_theme_imported) { ProfileThemeIO.import(context, version, it) }
        }
    }

    if (showColorDialog) {
        val initial = Color(accent ?: MaterialTheme.colorScheme.primary.toArgb())
        val colorController = rememberColorPickerController(initialColor = initial)
        ColorPickerDialog(
            colorController = colorController,
            showAlpha = false,
            onCancel = { showColorDialog = false },
            onConfirm = { picked ->
                config.themeAccent = picked.copy(alpha = 1f).toArgb()
                accent = config.themeAccent
                saveTheme()
                showColorDialog = false
            }
        )
    }

    SettingsCardColumn(modifier = modifier) {
        Text(
            modifier = Modifier.padding(all = 8.dp),
            text = stringResource(R.string.versions_config_theme_title),
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelLarge
        )

        SettingsCard(
            modifier = Modifier.fillMaxWidth(),
            position = CardPosition.Top,
            title = stringResource(R.string.versions_config_theme_accent_title),
            summary = stringResource(R.string.versions_config_theme_accent_summary),
            onClick = { showColorDialog = true },
            trailingIcon = {
                accent?.let { argb ->
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(Color(argb), CircleShape)
                    )
                }
            }
        )

        val tierItems = listOf(
            IDItem("", stringResource(R.string.generic_follow_global)),
            IDItem(GlassTier.OFF.name, stringResource(GlassTier.OFF.textRes)),
            IDItem(GlassTier.LITE.name, stringResource(GlassTier.LITE.textRes)),
            IDItem(GlassTier.FULL.name, stringResource(GlassTier.FULL.textRes))
        )
        ListSettingsCard(
            modifier = Modifier.fillMaxWidth(),
            position = CardPosition.Middle,
            items = tierItems,
            currentId = tierId,
            defaultId = "",
            title = stringResource(R.string.versions_config_theme_glass_title),
            summary = stringResource(R.string.versions_config_theme_glass_summary),
            getItemText = { it.title },
            getItemId = { it.id },
            onValueChange = { item ->
                tierId = item.id
                config.themeGlassTier = GlassTier.entries.firstOrNull { it.name == item.id }
                saveTheme()
            }
        )

        SettingsCard(
            modifier = Modifier.fillMaxWidth(),
            position = CardPosition.Middle,
            title = stringResource(R.string.versions_config_theme_wallpaper_title),
            summary = stringResource(R.string.versions_config_theme_wallpaper_set),
            onClick = { wallpaperPicker.launch(Unit) },
            trailingIcon = {
                if (hasWallpaper) {
                    IconButton(
                        onClick = {
                            runThemeTask(null) { ProfileThemeIO.removeWallpaper(version) }
                        }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_delete_outlined),
                            contentDescription = stringResource(R.string.versions_config_theme_wallpaper_clear)
                        )
                    }
                }
            }
        )

        SettingsCard(
            modifier = Modifier.fillMaxWidth(),
            position = CardPosition.Middle,
            title = stringResource(R.string.versions_config_theme_export),
            summary = stringResource(R.string.versions_config_theme_export_summary),
            onClick = { exportLauncher.launch("${version.getVersionName()}.$PROFILE_THEME_EXTENSION") }
        )

        SettingsCard(
            modifier = Modifier.fillMaxWidth(),
            position = CardPosition.Middle,
            title = stringResource(R.string.versions_config_theme_import),
            summary = stringResource(R.string.versions_config_theme_import_summary),
            onClick = { importLauncher.launch("*/*") }
        )

        SettingsCard(
            modifier = Modifier.fillMaxWidth(),
            position = CardPosition.Bottom,
            title = stringResource(R.string.versions_config_theme_reset),
            onClick = {
                runThemeTask(null) { ProfileThemeIO.reset(version) }
            }
        )
    }
}

private fun VersionConfig.saveOrShowError(
    submitError: (ErrorViewModel.ThrowableMessage) -> Unit
) {
    runCatching {
        saveWithThrowable()
    }.onFailure { e ->
        Logger.error(TAG, "Failed to save version config!", e)
        submitError(
            ErrorViewModel.ThrowableMessage(
                title = androidText(R.string.versions_config_failed_to_save),
                message = androidText(e.getMessageOrToString())
            )
        )
    }
}