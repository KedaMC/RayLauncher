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

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.annotation.StringRes
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.movtery.zalithlauncher.bridge.CursorShape
import com.movtery.zalithlauncher.context.copyLocalFile
import com.movtery.zalithlauncher.contract.MediaPickerContract
import com.movtery.zalithlauncher.coroutine.Task
import com.movtery.zalithlauncher.coroutine.TaskSystem
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.setting.enums.GestureActionType
import com.movtery.zalithlauncher.setting.enums.MouseControlMode
import com.movtery.zalithlauncher.setting.unit.ParcelableSettingUnit
import com.movtery.zalithlauncher.setting.unit.floatRange
import com.movtery.zalithlauncher.ui.androidText
import com.movtery.zalithlauncher.ui.base.BaseScreen
import com.movtery.zalithlauncher.ui.components.AnimatedLazyColumn
import com.movtery.zalithlauncher.ui.components.IconTextButton
import com.movtery.zalithlauncher.ui.components.LittleTextLabel
import com.movtery.zalithlauncher.ui.components.MarqueeText
import com.movtery.zalithlauncher.ui.components.SimpleAlertDialog
import com.movtery.zalithlauncher.ui.components.TitleAndSummary
import com.movtery.zalithlauncher.ui.components.TooltipIconButton
import com.movtery.zalithlauncher.ui.components.infiniteShimmer
import com.movtery.zalithlauncher.ui.components.lazyScrollWithBar
import com.movtery.zalithlauncher.ui.control.mouse.CursorHotspot
import com.movtery.zalithlauncher.ui.control.mouse.MouseHotspotEditorDialog
import com.movtery.zalithlauncher.ui.control.mouse.MousePointer
import com.movtery.zalithlauncher.ui.control.mouse.arrowPointerFile
import com.movtery.zalithlauncher.ui.control.mouse.crossHairPointerFile
import com.movtery.zalithlauncher.ui.control.mouse.iBeamPointerFile
import com.movtery.zalithlauncher.ui.control.mouse.linkPointerFile
import com.movtery.zalithlauncher.ui.control.mouse.notAllowedPointerFile
import com.movtery.zalithlauncher.ui.control.mouse.resizeAllPointerFile
import com.movtery.zalithlauncher.ui.control.mouse.resizeEWPointerFile
import com.movtery.zalithlauncher.ui.control.mouse.resizeNSPointerFile
import com.movtery.zalithlauncher.ui.screens.NestedNavKey
import com.movtery.zalithlauncher.ui.screens.NormalNavKey
import com.movtery.zalithlauncher.ui.screens.TitledNavKey
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.CardPosition
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.IntSliderSettingsCard
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.ListSettingsCard
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.SettingsCard
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.SwitchSettingsCard
import com.movtery.zalithlauncher.utils.formatKeyCode
import com.movtery.zalithlauncher.utils.image.isImageFile
import com.movtery.zalithlauncher.utils.string.getMessageOrToString
import com.movtery.zalithlauncher.viewmodel.ErrorViewModel
import com.movtery.zalithlauncher.viewmodel.EventViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.withContext
import org.apache.commons.io.FileUtils
import java.io.File

/** One cursor image card: which pointer file, hotspot and strings it uses. */
private class PointerCardSpec(
    val key: String,
    @StringRes val titleRes: Int,
    @StringRes val summaryRes: Int,
    val file: File,
    val shape: CursorShape,
    val hotspot: ParcelableSettingUnit<CursorHotspot>
)

private fun pointerCardSpecs(): List<PointerCardSpec> = listOf(
    PointerCardSpec("arrow", R.string.settings_control_mouse_pointer_arrow_title, R.string.settings_control_mouse_pointer_arrow_summary, arrowPointerFile, CursorShape.Arrow, AllSettings.arrowMouseHotspot),
    PointerCardSpec("link", R.string.settings_control_mouse_pointer_link_title, R.string.settings_control_mouse_pointer_link_summary, linkPointerFile, CursorShape.Hand, AllSettings.linkMouseHotspot),
    PointerCardSpec("ibeam", R.string.settings_control_mouse_pointer_ibeam_title, R.string.settings_control_mouse_pointer_ibeam_summary, iBeamPointerFile, CursorShape.IBeam, AllSettings.iBeamMouseHotspot),
    PointerCardSpec("crosshair", R.string.settings_control_mouse_pointer_crosshair_title, R.string.settings_control_mouse_pointer_common_summary, crossHairPointerFile, CursorShape.CrossHair, AllSettings.crossHairMouseHotspot),
    PointerCardSpec("resize_ns", R.string.settings_control_mouse_pointer_resize_ns_title, R.string.settings_control_mouse_pointer_resize_ns_summary, resizeNSPointerFile, CursorShape.ResizeNS, AllSettings.resizeNSMouseHotspot),
    PointerCardSpec("resize_ew", R.string.settings_control_mouse_pointer_resize_ew_title, R.string.settings_control_mouse_pointer_resize_ew_summary, resizeEWPointerFile, CursorShape.ResizeEW, AllSettings.resizeEWMouseHotspot),
    PointerCardSpec("resize_all", R.string.settings_control_mouse_pointer_resize_all_title, R.string.settings_control_mouse_pointer_common_summary, resizeAllPointerFile, CursorShape.ResizeAll, AllSettings.resizeAllMouseHotspot),
    PointerCardSpec("not_allowed", R.string.settings_control_mouse_pointer_not_allowed_title, R.string.settings_control_mouse_pointer_not_allowed_summary, notAllowedPointerFile, CursorShape.NotAllowed, AllSettings.notAllowedMouseHotspot)
)

/** Gap between two card groups, on top of the 2dp the list already puts between any two cards. */
private val GroupGap = 8.dp

@Composable
fun ControlSettingsScreen(
    key: NestedNavKey.Settings,
    settingsScreenKey: TitledNavKey?,
    mainScreenKey: TitledNavKey?,
    eventViewModel: EventViewModel,
    submitError: (ErrorViewModel.ThrowableMessage) -> Unit
) {
    BaseScreen(
        Triple(key, mainScreenKey, false),
        Triple(NormalNavKey.Settings.Control, settingsScreenKey, false)
    ) { isVisible ->
        //Every card is a glass surface, and this page has dozens of them: each card is its own lazy item,
        //so only the cards near the viewport are composed and blurred while scrolling.
        val listState = rememberLazyListState()

        AnimatedLazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .lazyScrollWithBar(listState),
            state = listState,
            isVisible = isVisible,
            verticalArrangement = Arrangement.spacedBy(2.dp),
            contentPadding = PaddingValues(all = 12.dp)
        ) { scope ->
            /** A card of a group. The whole group shares [group]'s entrance delay, like the old per-group stagger. */
            fun card(itemKey: String, group: Int, content: @Composable () -> Unit) {
                animatedItem(scope, key = itemKey, delay = group * 50) { yOffset ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset { IntOffset(x = 0, y = yOffset.roundToPx()) }
                    ) {
                        content()
                    }
                }
            }

            fun groupGap(itemKey: String) {
                scope.item(key = itemKey, contentType = "gap") { Spacer(modifier = Modifier.height(GroupGap)) }
            }

            //Group 0: physical mouse / key
            card("physical_mouse_mode", 0) {
                SwitchSettingsCard(
                    modifier = Modifier.fillMaxWidth(),
                    position = CardPosition.Top,
                    unit = AllSettings.physicalMouseMode,
                    title = stringResource(R.string.settings_control_mouse_physical_mouse_mode_title),
                    summary = stringResource(R.string.settings_control_mouse_physical_mouse_mode_summary),
                    trailingIcon = {
                        TooltipIconButton(
                            modifier = Modifier
                                .align(Alignment.CenterVertically)
                                .padding(horizontal = 8.dp),
                            tooltipTitle = stringResource(R.string.generic_warning),
                            tooltipMessage = stringResource(R.string.settings_control_mouse_physical_mouse_warning)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_warning_filled),
                                contentDescription = stringResource(R.string.generic_warning),
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                            )
                        }
                    }
                )
            }
            card("physical_key_ime", 0) {
                var operation by remember { mutableStateOf<PhysicalKeyOperation>(PhysicalKeyOperation.None) }
                SettingsCard(
                    modifier = Modifier.fillMaxWidth(),
                    position = CardPosition.Bottom,
                    onClick = { operation = PhysicalKeyOperation.Bind }
                ) {
                    PhysicalKeyImeTrigger(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(all = 16.dp),
                        operation = operation,
                        changeOperation = { operation = it },
                        eventViewModel = eventViewModel
                    )
                }
            }
            groupGap("gap_0")

            //Group 1: cursor images
            val pointerSpecs = pointerCardSpecs()
            pointerSpecs.forEachIndexed { index, spec ->
                card("pointer_${spec.key}", 1) {
                    var operation by remember { mutableStateOf<MousePointerOperation>(MousePointerOperation.None) }
                    MousePointerCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = if (index == 0) CardPosition.Top else CardPosition.Middle,
                        title = stringResource(spec.titleRes),
                        summary = stringResource(spec.summaryRes),
                        mouseSize = AllSettings.mouseSize.state,
                        mousePointerFile = spec.file,
                        cursorShape = spec.shape,
                        hotspot = spec.hotspot,
                        mouseOperation = operation,
                        changeOperation = { operation = it },
                        submitError = submitError
                    )
                }
            }
            card("pointer_size", 1) {
                IntSliderSettingsCard(
                    modifier = Modifier.fillMaxWidth(),
                    position = CardPosition.Bottom,
                    unit = AllSettings.mouseSize,
                    title = stringResource(R.string.settings_control_mouse_size_title),
                    valueRange = AllSettings.mouseSize.floatRange,
                    suffix = "Dp",
                    fineTuningControl = true
                )
            }
            groupGap("gap_1")

            //Group 2: mouse control
            card("mouse_hide", 2) {
                SwitchSettingsCard(
                    modifier = Modifier.fillMaxWidth(),
                    position = CardPosition.Top,
                    unit = AllSettings.hideMouse,
                    title = stringResource(R.string.settings_control_mouse_hide_title),
                    summary = stringResource(R.string.settings_control_mouse_hide_summary),
                    enabled = AllSettings.mouseControlMode.state == MouseControlMode.CLICK //仅点击模式下可更改设置
                )
            }
            card("mouse_enable_click", 2) {
                SwitchSettingsCard(
                    modifier = Modifier.fillMaxWidth(),
                    unit = AllSettings.enableMouseClick,
                    position = CardPosition.Middle,
                    title = stringResource(R.string.settings_control_mouse_enable_click_title),
                    summary = stringResource(R.string.settings_control_mouse_enable_click_summary),
                    enabled = AllSettings.mouseControlMode.state == MouseControlMode.SLIDE //仅滑动模式下可更改设置
                )
            }
            card("mouse_control_mode", 2) {
                ListSettingsCard(
                    modifier = Modifier.fillMaxWidth(),
                    position = CardPosition.Middle,
                    unit = AllSettings.mouseControlMode,
                    items = MouseControlMode.entries,
                    title = stringResource(R.string.settings_control_mouse_control_mode_title),
                    summary = stringResource(R.string.settings_control_mouse_control_mode_summary),
                    getItemText = { stringResource(it.nameRes) }
                )
            }
            card("mouse_sensitivity", 2) {
                IntSliderSettingsCard(
                    modifier = Modifier.fillMaxWidth(),
                    position = CardPosition.Middle,
                    unit = AllSettings.cursorSensitivity,
                    title = stringResource(R.string.settings_control_mouse_sensitivity_title),
                    summary = stringResource(R.string.settings_control_mouse_sensitivity_summary),
                    valueRange = AllSettings.cursorSensitivity.floatRange,
                    suffix = "%",
                    fineTuningControl = true
                )
            }
            card("mouse_capture_sensitivity", 2) {
                IntSliderSettingsCard(
                    modifier = Modifier.fillMaxWidth(),
                    position = CardPosition.Middle,
                    unit = AllSettings.mouseCaptureSensitivity,
                    title = stringResource(R.string.settings_control_mouse_capture_sensitivity_title),
                    summary = stringResource(R.string.settings_control_mouse_capture_sensitivity_summary),
                    valueRange = AllSettings.mouseCaptureSensitivity.floatRange,
                    suffix = "%",
                    fineTuningControl = true
                )
            }
            card("mouse_long_press_delay", 2) {
                IntSliderSettingsCard(
                    modifier = Modifier.fillMaxWidth(),
                    position = CardPosition.Bottom,
                    unit = AllSettings.mouseLongPressDelay,
                    title = stringResource(R.string.settings_control_mouse_long_press_delay_title),
                    summary = stringResource(R.string.settings_control_mouse_long_press_delay_summary),
                    valueRange = AllSettings.mouseLongPressDelay.floatRange,
                    suffix = "ms",
                    fineTuningControl = true
                )
            }
            groupGap("gap_2")

            //Group 3: gestures
            card("gesture_control", 3) {
                SwitchSettingsCard(
                    modifier = Modifier.fillMaxWidth(),
                    position = CardPosition.Top,
                    unit = AllSettings.gestureControl,
                    title = stringResource(R.string.settings_control_gesture_control_title),
                    summary = stringResource(R.string.settings_control_gesture_control_summary)
                )
            }
            card("gesture_tap_action", 3) {
                ListSettingsCard(
                    modifier = Modifier.fillMaxWidth(),
                    position = CardPosition.Middle,
                    unit = AllSettings.gestureTapMouseAction,
                    items = GestureActionType.entries,
                    title = stringResource(R.string.settings_control_gesture_tap_action_title),
                    summary = stringResource(R.string.settings_control_gesture_tap_action_summary),
                    getItemText = { stringResource(it.nameRes) },
                    enabled = AllSettings.gestureControl.state
                )
            }
            card("gesture_long_press_action", 3) {
                ListSettingsCard(
                    modifier = Modifier.fillMaxWidth(),
                    position = CardPosition.Middle,
                    unit = AllSettings.gestureLongPressMouseAction,
                    items = GestureActionType.entries,
                    title = stringResource(R.string.settings_control_gesture_long_press_action_title),
                    summary = stringResource(R.string.settings_control_gesture_long_press_action_summary),
                    getItemText = { stringResource(it.nameRes) },
                    enabled = AllSettings.gestureControl.state
                )
            }
            card("gesture_long_press_delay", 3) {
                IntSliderSettingsCard(
                    modifier = Modifier.fillMaxWidth(),
                    position = CardPosition.Bottom,
                    unit = AllSettings.gestureLongPressDelay,
                    title = stringResource(R.string.settings_control_gesture_long_press_delay_title),
                    summary = stringResource(R.string.settings_control_mouse_long_press_delay_summary),
                    valueRange = AllSettings.gestureLongPressDelay.floatRange,
                    suffix = "ms",
                    enabled = AllSettings.gestureControl.state,
                    fineTuningControl = true
                )
            }
        }
    }
}

private sealed interface PhysicalKeyOperation {
    data object None: PhysicalKeyOperation
    data object Bind: PhysicalKeyOperation
}

@Composable
private fun PhysicalKeyImeTrigger(
    modifier: Modifier = Modifier,
    operation: PhysicalKeyOperation,
    changeOperation: (PhysicalKeyOperation) -> Unit,
    eventViewModel: EventViewModel
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .animateContentSize()
        ) {
            TitleAndSummary(
                title = stringResource(R.string.settings_control_physical_key_bind_ime_title),
                summary = stringResource(R.string.settings_control_physical_key_bind_ime_summary)
            )
            when (operation) {
                PhysicalKeyOperation.None -> {}
                PhysicalKeyOperation.Bind -> {
                    LaunchedEffect(Unit) {
                        eventViewModel.sendEvent(EventViewModel.Event.Key.StartKeyCapture)
                        //接收Activity发送的按键事件
                        eventViewModel.events
                            .filterIsInstance<EventViewModel.Event.Key.OnKeyDown>()
                            .collect { event ->
                                changeOperation(PhysicalKeyOperation.None)
                                AllSettings.physicalKeyImeCode.save(event.key.keyCode)
                            }
                    }

                    DisposableEffect(Unit) {
                        onDispose {
                            eventViewModel.sendEvent(EventViewModel.Event.Key.StopKeyCapture)
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LittleTextLabel(text = stringResource(R.string.control_keyboard_bind_title))
                        MarqueeText(
                            modifier = Modifier
                                .weight(1f)
                                .infiniteShimmer(
                                    initialValue = 0.5f,
                                    targetValue = 1f
                                ),
                            text = stringResource(R.string.control_keyboard_bind_summary),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.padding(start = 8.dp)
        ) {
            val code = AllSettings.physicalKeyImeCode.state
            when {
                code == null -> {
                    Text(
                        modifier = Modifier.padding(end = 12.dp),
                        text = stringResource(R.string.settings_control_physical_key_bind_ime_un_bind),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                else -> {
                    IconTextButton(
                        onClick = { AllSettings.physicalKeyImeCode.save(null) },
                        painter = painterResource(R.drawable.ic_restart_alt),
                        contentDescription = stringResource(R.string.generic_reset),
                        text = stringResource(
                            R.string.settings_control_physical_key_bind_ime_bound,
                            formatKeyCode(code)
                        )
                    )
                }
            }
        }
    }
}

private sealed interface MousePointerOperation {
    data object None: MousePointerOperation
    /** 重置鼠标指针前的提醒 */
    data object PreReset: MousePointerOperation
    /** 重置鼠标指针 */
    data object Reset: MousePointerOperation
    /** 变更鼠标热点 */
    data object Hotspot: MousePointerOperation
}

@Composable
private fun MousePointerCard(
    modifier: Modifier = Modifier,
    position: CardPosition,
    title: String,
    summary: String,
    mouseSize: Int,
    mousePointerFile: File,
    cursorShape: CursorShape,
    hotspot: ParcelableSettingUnit<CursorHotspot>,
    mouseOperation: MousePointerOperation,
    changeOperation: (MousePointerOperation) -> Unit,
    submitError: (ErrorViewModel.ThrowableMessage) -> Unit
) {
    val context = LocalContext.current
    var triggerState by remember { mutableIntStateOf(0) }
    var fileExists by remember { mutableStateOf(false) }

    LaunchedEffect(triggerState) {
        fileExists = withContext(Dispatchers.IO) { mousePointerFile.exists() }
    }

    MousePointerOperation(
        operation = mouseOperation,
        changeOperation = changeOperation,
        mousePointerFile = mousePointerFile,
        hotspot = hotspot,
        cursorShape = cursorShape,
        onRefresh = {
            triggerState++
        }
    )

    val filePicker = rememberLauncherForActivityResult(
        contract = MediaPickerContract(
            allowImages = true,
            allowVideos = false,
            allowMultiple = false
        )
    ) { result ->
        if (result != null) {
            TaskSystem.submitTask(
                Task.runTask(
                    dispatcher = Dispatchers.IO,
                    task = {
                        context.copyLocalFile(result[0], mousePointerFile)
                        if (!mousePointerFile.isImageFile()) error("The selected file is not an image!")
                        triggerState++
                        changeOperation(MousePointerOperation.None)
                    },
                    onError = { th ->
                        FileUtils.deleteQuietly(mousePointerFile)
                        submitError(
                            ErrorViewModel.ThrowableMessage(
                                title = androidText(R.string.error_import_image),
                                message = androidText(th.getMessageOrToString())
                            )
                        )
                    }
                )
            )
        }
    }

    SettingsCard(
        modifier = modifier,
        position = position
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { filePicker.launch(Unit) }
                    .padding(all = 16.dp)
            ) {
                TitleAndSummary(
                    title = title,
                    summary = summary
                )
            }

            Row(
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MousePointer(
                    modifier = Modifier.padding(all = 8.dp),
                    mouseSize = mouseSize.dp,
                    cursorShape = cursorShape,
                    mouseFile = mousePointerFile,
                    centerIcon = true,
                    triggerRefresh = triggerState,
                    crossfade = true
                )

                IconTextButton(
                    onClick = {
                        if (mouseOperation == MousePointerOperation.None) {
                            changeOperation(MousePointerOperation.Hotspot)
                        }
                    },
                    painter = painterResource(R.drawable.ic_highlight_mouse_cursor),
                    contentDescription = stringResource(R.string.settings_control_mouse_pointer_hotspot),
                    text = stringResource(R.string.settings_control_mouse_pointer_hotspot)
                )

                AnimatedVisibility(
                    visible = fileExists
                ) {
                    IconTextButton(
                        onClick = {
                            if (mouseOperation == MousePointerOperation.None) {
                                changeOperation(MousePointerOperation.PreReset)
                            }
                        },
                        painter = painterResource(R.drawable.ic_restart_alt),
                        contentDescription = stringResource(R.string.generic_reset),
                        text = stringResource(R.string.generic_reset)
                    )
                }
            }
        }
    }
}

@Composable
private fun MousePointerOperation(
    operation: MousePointerOperation,
    changeOperation: (MousePointerOperation) -> Unit,
    mousePointerFile: File,
    hotspot: ParcelableSettingUnit<CursorHotspot>,
    cursorShape: CursorShape,
    onRefresh: () -> Unit
) {
    when (operation) {
        is MousePointerOperation.None -> {}
        is MousePointerOperation.PreReset -> {
            SimpleAlertDialog(
                title = stringResource(R.string.generic_reset),
                text = stringResource(R.string.settings_control_mouse_pointer_reset_message),
                onConfirm = {
                    //正式开始重置鼠标指针
                    changeOperation(MousePointerOperation.Reset)
                },
                onDismiss = {
                    changeOperation(MousePointerOperation.None)
                }
            )
        }
        is MousePointerOperation.Reset -> {
            LaunchedEffect(Unit) {
                FileUtils.deleteQuietly(mousePointerFile)
                onRefresh()
                changeOperation(MousePointerOperation.None)
            }
        }
        is MousePointerOperation.Hotspot -> {
            MouseHotspotEditorDialog(
                hotspot = hotspot,
                cursorShape = cursorShape,
                onClose = {
                    changeOperation(MousePointerOperation.None)
                }
            )
        }
    }
}