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
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
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
import com.movtery.zalithlauncher.game.renderer.ConfigurableRenderer
import com.movtery.zalithlauncher.game.renderer.Renderers
import com.movtery.zalithlauncher.game.support.touch_controller.VibrationHandler
import com.movtery.zalithlauncher.game.version.installed.GraphicsApi
import com.movtery.zalithlauncher.game.version.installed.Version
import com.movtery.zalithlauncher.game.version.installed.VersionConfig
import com.movtery.zalithlauncher.ui.screens.content.settings.RendererConfigDialog
import com.movtery.zalithlauncher.ui.screens.content.versions.elements.StatefulVersionConfig
import com.movtery.zalithlauncher.utils.string.isNotEmptyOrBlank
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import androidx.compose.runtime.DisposableEffect
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.setting.unit.floatRange
import com.movtery.zalithlauncher.setting.unit.getOrMin
import com.movtery.zalithlauncher.ui.AndroidStringText
import com.movtery.zalithlauncher.ui.androidText
import com.movtery.zalithlauncher.ui.base.BaseScreen
import com.movtery.zalithlauncher.ui.components.GlassLazyColumn
import com.movtery.zalithlauncher.ui.components.GlassLazyScope
import com.movtery.zalithlauncher.ui.components.animated
import com.movtery.zalithlauncher.ui.components.IDItem
import com.movtery.zalithlauncher.ui.components.lazyScrollWithBar
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
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.SimpleIDListCard
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.TextInputSettingsCard
import com.movtery.zalithlauncher.ui.screens.content.versions.layouts.StatefulDropdownMenuFollowGlobal
import com.movtery.zalithlauncher.ui.screens.content.versions.layouts.ToggleableIntSliderSettingsCard
import com.movtery.zalithlauncher.utils.logging.Logger
import com.movtery.zalithlauncher.utils.platform.getMaxMemoryForSettings
import com.movtery.zalithlauncher.utils.string.getMessageOrToString
import com.movtery.zalithlauncher.viewmodel.ErrorViewModel

private const val TAG = "VersionConfigScreen"

/** Scope that outlives the page, so the config is still saved after leaving it */
private val configSaveScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

/** Gap between two card groups, on top of the 2dp the list already puts between any two cards. */
private val GroupGap = 8.dp

/**
 * Registers the cards of one group as separate lazy items, so only the cards near the viewport are
 * composed (and blurred) while scrolling. Every card of a group shares the group's entrance delay.
 */
private class ConfigItems(
    private val scope: GlassLazyScope
) {
    fun card(itemKey: String, group: Int, content: @Composable () -> Unit) {
        scope.animatedItem(key = itemKey, delay = group * 50) { ctx ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .animated(ctx)
            ) {
                content()
            }
        }
    }

    fun groupTitle(itemKey: String, group: Int, @StringRes titleRes: Int) {
        card(itemKey, group) {
            Text(
                modifier = Modifier.padding(all = 8.dp),
                text = stringResource(titleRes),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }

    fun gap(itemKey: String) {
        scope.item(key = itemKey, contentType = "gap") { Spacer(modifier = Modifier.height(GroupGap)) }
    }
}

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
        val listState = rememberLazyListState()
        val stateful = remember(version) { StatefulVersionConfig(config) }

        DisposableEffect(version) {
            onDispose {
                if (stateful.isModified()) {
                    //Save everything once when the page is left
                    stateful.packInto(config)
                    saveConfigAsync(config, submitError)
                }
            }
        }

        //State, dialogs and launchers of every group live out here; the lazy items below only draw cards
        val versionItems = rememberVersionConfigItems(stateful)
        val themeItems = rememberProfileThemeItems(version, config, showToast, submitError)
        val gameItems = rememberGameConfigItems(stateful)
        val supportItems = rememberSupportConfigItems(
            stateful = stateful,
            onCheckVulkan = { onCheckVulkan(version) },
            showToast = showToast
        )

        GlassLazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .lazyScrollWithBar(listState),
            state = listState,
            isVisible = isVisible,
            verticalArrangement = Arrangement.spacedBy(2.dp),
            contentPadding = PaddingValues(all = 12.dp)
        ) {
            val items = ConfigItems(this)
            items.versionItems()
            items.gap("gap_0")
            items.themeItems()
            items.gap("gap_1")
            items.gameItems()
            items.gap("gap_2")
            items.supportItems()
        }
    }
}

@Composable
private fun rememberVersionConfigItems(
    stateful: StatefulVersionConfig
): ConfigItems.() -> Unit {
        val renderers = Renderers.getRenderers()
        val renderersIdList = getIDList(renderers) { IDItem(it.getUniqueIdentifier(), it.getRendererName()) }
        val configurableRenderer = remember(stateful.renderer) {
            if (stateful.renderer.isNotEmpty()) {
                renderers.filterIsInstance<ConfigurableRenderer>()
                    .find { it.getUniqueIdentifier() == stateful.renderer }
                    ?.takeIf { it.configSpec.entries.isNotEmpty() }
            } else null
        }
        var showConfigDialog by remember { mutableStateOf(false) }

        if (showConfigDialog && configurableRenderer != null) {
            RendererConfigDialog(
                renderer = configurableRenderer,
                versionFolder = stateful.versionPath,
                onDismissRequest = { showConfigDialog = false }
            )
        }

        val drivers = DriverPluginManager.getDriverList()
        val driversIdList = getIDList(drivers) { IDItem(it.id, it.name) }
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
        val unspecifiedName = stringResource(R.string.generic_unspecified)
        val controls by ControlManager.dataList.collectAsStateWithLifecycle()
        val controlsIdList = getIDList(controls.filter { it.isSupport }) {
            IDItem(
                it.file.name,
                it.controlLayout.info.name.translate()
                    .takeIf { name -> name.isNotEmptyOrBlank() } ?: unspecifiedName
            )
        }

    return {
        groupTitle("version_title", 0, R.string.versions_config_version_settings)
        card("isolation", 0) {
            StatefulDropdownMenuFollowGlobal(
                modifier = Modifier.fillMaxWidth(),
                position = CardPosition.Top,
                currentValue = stateful.isolationType,
                onValueChange = { stateful.isolationType = it },
                title = stringResource(R.string.versions_config_isolation_title),
                summary = stringResource(R.string.versions_config_isolation_summary)
            )
        }

        card("skip_integrity_check", 0) {
            StatefulDropdownMenuFollowGlobal(
                modifier = Modifier.fillMaxWidth(),
                position = CardPosition.Middle,
                currentValue = stateful.skipGameIntegrityCheck,
                onValueChange = { stateful.skipGameIntegrityCheck = it },
                title = stringResource(R.string.settings_game_skip_game_integrity_check_title),
                summary = stringResource(R.string.settings_game_skip_game_integrity_check_summary)
            )
        }

        card("renderer", 0) {
            ListSettingsCard(
                modifier = Modifier.fillMaxWidth(),
                position = CardPosition.Middle,
                items = renderersIdList,
                currentId = stateful.renderer,
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
                    //Follow-global can't be configured per version
                    if (configurableRenderer != null) {
                        IconButton(onClick = { showConfigDialog = true }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_settings_filled),
                                contentDescription = stringResource(R.string.settings_renderer_config_title)
                            )
                        }
                    }
                },
                onValueChange = { stateful.renderer = it.id }
            )
        }

        card("vulkan_driver", 0) {
            ListSettingsCard(
                modifier = Modifier.fillMaxWidth(),
                position = CardPosition.Middle,
                items = driversIdList,
                currentId = stateful.driver,
                defaultId = "",
                title = stringResource(R.string.versions_config_vulkan_driver),
                getItemText = { it.title },
                getItemId = { it.id },
                getItemSummary = { item ->
                    drivers.find { it.id == item.id }?.let { driver ->
                        DriverSummaryLayout(driver)
                    }
                },
                onValueChange = { stateful.driver = it.id }
            )
        }

        card("graphics_api", 0) {
            ListSettingsCard(
                modifier = Modifier.fillMaxWidth(),
                position = CardPosition.Middle,
                items = graphicsApisList,
                currentId = stateful.graphicsApi?.name ?: "",
                defaultId = "",
                title = stringResource(R.string.settings_game_graphics_api_title),
                summary = stringResource(R.string.settings_game_graphics_api_summary),
                getItemText = { it.title },
                getItemId = { it.id },
                onValueChange = { item ->
                    stateful.graphicsApi = GraphicsApi.entries.find { it.name == item.id }
                }
            )
        }

        card("control", 0) {
            ListSettingsCard(
                modifier = Modifier.fillMaxWidth(),
                position = CardPosition.Bottom,
                items = controlsIdList,
                currentId = stateful.control,
                defaultId = "",
                title = stringResource(R.string.versions_config_control),
                getItemText = { it.title },
                getItemId = { it.id },
                onValueChange = { stateful.control = it.id }
            )
        }
    }
}

@Composable
private fun rememberGameConfigItems(
    stateful: StatefulVersionConfig
): ConfigItems.() -> Unit {
    val javaRuntimes = getIDList(RuntimesManager.getRuntimes().filter { it.isCompatible() }) { IDItem(it.name, it.name) }
    return {
        groupTitle("game_title", 2, R.string.versions_config_game_settings)
        card("java_runtime", 2) {
            SimpleIDListCard(
                modifier = Modifier.fillMaxWidth(),
                position = CardPosition.Top,
                items = javaRuntimes,
                currentId = stateful.javaRuntime,
                defaultId = "",
                title = stringResource(R.string.settings_game_java_runtime_title),
                summary = stringResource(R.string.versions_config_java_runtime_summary),
                onValueChange = { stateful.javaRuntime = it.id }
            )
        }

        card("java_memory", 2) {
            ToggleableIntSliderSettingsCard(
                modifier = Modifier.fillMaxWidth(),
                position = CardPosition.Middle,
                currentValue = stateful.ramAllocation,
                valueRange = AllSettings.ramAllocation.floatRange.start..getMaxMemoryForSettings(LocalContext.current).toFloat(),
                defaultValue = AllSettings.ramAllocation.getOrMin(),
                title = stringResource(R.string.settings_game_java_memory_title),
                summary = stringResource(R.string.settings_game_java_memory_summary),
                suffix = "MB",
                onValueChange = { stateful.ramAllocation = it },
                previewContent = {
                    AnimatedVisibility(
                        modifier = Modifier.fillMaxWidth(),
                        visible = stateful.ramAllocation >= 256
                    ) {
                        MemoryPreview(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 2.dp, end = 8.dp),
                            preview = stateful.ramAllocation.takeIf { it >= 256 }?.toDouble(),
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
        }

        card("version_custom_info", 2) {
            TextInputSettingsCard(
                modifier = Modifier.fillMaxWidth(),
                position = CardPosition.Middle,
                value = stateful.customInfo,
                onValueChange = { stateful.customInfo = it },
                title = stringResource(R.string.settings_game_version_custom_info_title),
                summary = stringResource(R.string.settings_game_version_custom_info_summary),
                label = {
                    Text(text = stringResource(R.string.versions_config_follow_global_if_blank))
                }
            )
        }

        card("game_args", 2) {
            TextInputSettingsCard(
                modifier = Modifier.fillMaxWidth(),
                position = CardPosition.Middle,
                value = stateful.gameArgs,
                title = stringResource(R.string.versions_config_game_args_title),
                summary = stringResource(R.string.versions_config_game_args_summary),
                onValueChange = { stateful.gameArgs = it },
                label = {
                    Text(text = stringResource(R.string.versions_config_disable_if_blank))
                }
            )
        }

        card("jvm_args", 2) {
            TextInputSettingsCard(
                modifier = Modifier.fillMaxWidth(),
                position = CardPosition.Middle,
                value = stateful.jvmArgs,
                title = stringResource(R.string.settings_game_jvm_args_title),
                summary = stringResource(R.string.settings_game_jvm_args_summary),
                onValueChange = { stateful.jvmArgs = it },
                label = {
                    Text(text = stringResource(R.string.versions_config_follow_global_if_blank))
                }
            )
        }

        card("server_ip", 2) {
            TextInputSettingsCard(
                modifier = Modifier.fillMaxWidth(),
                position = CardPosition.Bottom,
                value = stateful.serverIp,
                title = stringResource(R.string.versions_config_auto_join_server_ip_title),
                summary = stringResource(R.string.versions_config_auto_join_server_ip_summary),
                onValueChange = { stateful.serverIp = it },
                label = {
                    Text(text = stringResource(R.string.versions_config_disable_if_blank))
                }
            )
        }
    }
}

@Composable
private fun rememberSupportConfigItems(
    stateful: StatefulVersionConfig,
    onCheckVulkan: () -> Unit,
    showToast: (AndroidStringText) -> Unit
): ConfigItems.() -> Unit {
    val context = LocalContext.current

        val vibrator = remember(context) { context.getSystemService<Vibrator>() }
        val effectiveVibrateKind = stateful.touchVibrateKind ?: VibrationHandler.VibrateKind.default
        var microphoneState by remember { mutableStateOf<MicrophoneCheckState>(MicrophoneCheckState.None) }
        MicrophoneCheckOperation(
            state = microphoneState,
            changeState = { microphoneState = it },
            onShowToast = showToast,
        )

    return {
        groupTitle("support_title", 3, R.string.versions_config_support_settings)
        card("vibrate_kind", 3) {
            EnumSettingsCard(
                modifier = Modifier.fillMaxWidth(),
                position = CardPosition.Top,
                value = effectiveVibrateKind,
                title = stringResource(R.string.versions_config_vibrate_kind_title),
                summary = stringResource(R.string.versions_config_vibrate_kind_summary),
                entries = VibrationHandler.VibrateKind.entries,
                getRadioEnable = { enum ->
                    enum == VibrationHandler.VibrateKind.ONE_SHOT || Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
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
                    stateful.touchVibrateKind = enum
                    vibrator?.let { vibrator ->
                        VibrationHandler.vibrate(
                            vibrator = vibrator,
                            vibrateDuration = stateful.touchVibrateDuration,
                            vibrateKind = stateful.touchVibrateKind,
                        )
                    }
                }
            )
        }

        card("vibrate_duration", 3) {
            AnimatedVisibility(effectiveVibrateKind == VibrationHandler.VibrateKind.ONE_SHOT) {
                IntSliderSettingsCard(
                    modifier = Modifier.fillMaxWidth(),
                    position = CardPosition.Middle,
                    value = stateful.touchVibrateDuration,
                    title = stringResource(R.string.versions_config_vibrate_duration_title),
                    summary = stringResource(R.string.versions_config_vibrate_duration_summary),
                    valueRange = 80f..500f,
                    onValueChange = { stateful.touchVibrateDuration = it },
                    onValueChangeFinished = {
                        vibrator?.let { vibrator ->
                            VibrationHandler.vibrate(
                                vibrator = vibrator,
                                vibrateDuration = stateful.touchVibrateDuration,
                                vibrateKind = stateful.touchVibrateKind,
                            )
                        }
                    },
                    suffix = "ms",
                    fineTuningControl = true
                )
            }
        }

        card("vulkan_check", 3) {
            SettingsCard(
                modifier = Modifier.fillMaxWidth(),
                position = CardPosition.Middle,
                title = stringResource(R.string.game_vulkan_check_title),
                summary = stringResource(R.string.game_vulkan_check_text),
                onClick = onCheckVulkan
            )
        }

        card("microphone_check", 3) {
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
private fun rememberProfileThemeItems(
    version: Version,
    config: VersionConfig,
    showToast: (AndroidStringText) -> Unit,
    submitError: (ErrorViewModel.ThrowableMessage) -> Unit
): ConfigItems.() -> Unit {
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

        val tierItems = listOf(
            IDItem("", stringResource(R.string.generic_follow_global)),
            IDItem(GlassTier.OFF.name, stringResource(GlassTier.OFF.textRes)),
            IDItem(GlassTier.LITE.name, stringResource(GlassTier.LITE.textRes)),
            IDItem(GlassTier.FULL.name, stringResource(GlassTier.FULL.textRes))
        )

    return {
        groupTitle("theme_title", 1, R.string.versions_config_theme_title)
        card("theme_accent", 1) {
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
        }

        card("theme_glass", 1) {
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
        }

        card("theme_wallpaper", 1) {
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
        }

        card("theme_export", 1) {
            SettingsCard(
                modifier = Modifier.fillMaxWidth(),
                position = CardPosition.Middle,
                title = stringResource(R.string.versions_config_theme_export),
                summary = stringResource(R.string.versions_config_theme_export_summary),
                onClick = { exportLauncher.launch("${version.getVersionName()}.$PROFILE_THEME_EXTENSION") }
            )
        }

        card("theme_import", 1) {
            SettingsCard(
                modifier = Modifier.fillMaxWidth(),
                position = CardPosition.Middle,
                title = stringResource(R.string.versions_config_theme_import),
                summary = stringResource(R.string.versions_config_theme_import_summary),
                onClick = { importLauncher.launch("*/*") }
            )
        }

        card("theme_reset", 1) {
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

private fun saveConfigAsync(
    config: VersionConfig,
    submitError: (ErrorViewModel.ThrowableMessage) -> Unit
) {
    configSaveScope.launch {
        runCatching {
            config.saveWithThrowable()
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
}
