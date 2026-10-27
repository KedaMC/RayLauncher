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

package com.movtery.zalithlauncher.ui.screens.game.elements

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.sdl.SdlBridge
import com.movtery.zalithlauncher.recorder.RecorderController
import com.movtery.zalithlauncher.recorder.RecorderService
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.setting.enums.FpsDisplayMode
import com.movtery.zalithlauncher.setting.enums.GestureActionType
import com.movtery.zalithlauncher.setting.enums.MouseControlMode
import com.movtery.zalithlauncher.setting.enums.ResolutionRule
import com.movtery.zalithlauncher.setting.unit.floatRange
import com.movtery.zalithlauncher.ui.AndroidStringText
import com.movtery.zalithlauncher.ui.androidText
import com.movtery.zalithlauncher.ui.components.BackgroundCard
import com.movtery.zalithlauncher.ui.components.DualMenuSubscreen
import com.movtery.zalithlauncher.ui.components.IntInputField
import com.movtery.zalithlauncher.ui.components.MenuListLayout
import com.movtery.zalithlauncher.ui.components.MenuSliderLayout
import com.movtery.zalithlauncher.ui.components.MenuState
import com.movtery.zalithlauncher.ui.components.MenuSwitchButton
import com.movtery.zalithlauncher.ui.components.MenuTextButton
import com.movtery.zalithlauncher.ui.components.lazyScrollWithBar
import com.movtery.zalithlauncher.ui.control.HotbarRule
import com.movtery.zalithlauncher.ui.theme.cardColor
import com.movtery.zalithlauncher.ui.theme.cardTitleColor
import com.movtery.zalithlauncher.ui.theme.onCardColor
import com.movtery.zalithlauncher.utils.animation.getAnimateTween
import com.movtery.zalithlauncher.utils.customResolutionRange
import com.movtery.zalithlauncher.utils.ensureCustomResolutionInitialized
import com.movtery.zalithlauncher.utils.getRealScreenSize

private data class IconTab(val iconRes: Int, val iconSize: Dp = 18.dp)

private val controlTabs = listOf(
    //概览
    IconTab(R.drawable.ic_dashboard_filled),
    //虚拟鼠标设置
    IconTab(R.drawable.ic_mouse_filled, iconSize = 16.dp),
    //手势控制设置
    IconTab(R.drawable.ic_touch_app_filled),
    //录制（Lite 新增）
    IconTab(R.drawable.ic_movie_filled),
)

@Composable
fun GameMenuSubscreen(
    state: MenuState,
    controlMenuTabIndex: Int,
    onControlMenuTabChange: (Int) -> Unit,
    closeScreen: () -> Unit,
    onForceClose: () -> Unit,
    onSwitchLog: () -> Unit,
    onRefreshWindowSize: () -> Unit,
    onInputMethod: () -> Unit,
    onSendKeycode: () -> Unit,
    onReplacementControl: () -> Unit,
    onEditLayout: () -> Unit,
    onShowToast: (AndroidStringText, Int) -> Unit
) {
    DualMenuSubscreen(
        state = state,
        closeScreen = closeScreen,
        leftMenuContent = {
            val pagerState = rememberPagerState(pageCount = { controlTabs.size })

            LaunchedEffect(controlMenuTabIndex) {
                pagerState.animateScrollToPage(controlMenuTabIndex)
            }

            Column {
                //顶贴标签栏
                SecondaryScrollableTabRow(
                    selectedTabIndex = controlMenuTabIndex,
                    edgePadding = 0.dp,
                    minTabWidth = 58.dp,
                    containerColor = cardTitleColor(),
                ) {
                    controlTabs.forEachIndexed { index, iconTab ->
                        Tab(
                            selected = index == controlMenuTabIndex,
                            onClick = {
                                onControlMenuTabChange(index)
                            },
                            icon = {
                                Icon(
                                    modifier = Modifier.size(iconTab.iconSize),
                                    painter = painterResource(iconTab.iconRes),
                                    contentDescription = null
                                )
                            }
                        )
                    }
                }

                HorizontalPager(
                    state = pagerState,
                    userScrollEnabled = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                ) { page ->
                    when (page) {
                        0 -> {
                            ControlOverview(
                                modifier = Modifier.fillMaxSize(),
                                closeScreen = closeScreen,
                                onInputMethod = onInputMethod,
                                onSendKeycode = onSendKeycode,
                                onReplacementControl = onReplacementControl,
                                onEditLayout = onEditLayout
                            )
                        }
                        1 -> ControlMouse(modifier = Modifier.fillMaxSize())
                        2 -> ControlGesture(modifier = Modifier.fillMaxSize())
                        3 -> ControlRecorder(
                            modifier = Modifier.fillMaxSize(),
                            closeScreen = closeScreen,
                            onShowToast = onShowToast
                        )
                    }
                }
            }
        },
        rightMenuTitle = {
            Text(
                modifier = Modifier.padding(all = 8.dp),
                text = stringResource(R.string.game_menu_title),
                style = MaterialTheme.typography.titleMedium
            )
        },
        rightMenuContent = {
            GameActionContent(
                modifier = Modifier.weight(1f),
                onForceClose = onForceClose,
                onSwitchLog = onSwitchLog,
                onRefreshWindowSize = onRefreshWindowSize,
                onShowToast = onShowToast
            )
        }
    )
}

@Composable
private fun GameActionContent(
    onForceClose: () -> Unit,
    onSwitchLog: () -> Unit,
    onRefreshWindowSize: () -> Unit,
    onShowToast: (AndroidStringText, Int) -> Unit,
    modifier: Modifier = Modifier,
    color: Color = cardColor(false),
    contentColor: Color = onCardColor(),
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    LazyColumn(
        modifier = modifier.lazyScrollWithBar(listState),
        state = listState,
        contentPadding = PaddingValues(all = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        //强制关闭
        item {
            MenuTextButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.game_button_force_close),
                onClick = onForceClose,
                color = color,
                contentColor = contentColor,
            )
        }
        //日志输出
        item {
            MenuTextButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.game_menu_option_switch_log),
                onClick = onSwitchLog,
                color = color,
                contentColor = contentColor,
            )
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
        }

        //开启菜单悬浮窗
        item {
            MenuSwitchButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.game_menu_option_show_menu),
                switch = AllSettings.showMenuBall.state,
                onSwitch = { value ->
                    AllSettings.showMenuBall.save(value)
                    if (!value) {
                        onShowToast(
                            androidText(R.string.game_menu_option_show_menu_hided),
                            Toast.LENGTH_LONG
                        )
                    }
                },
                color = color,
                contentColor = contentColor,
            )
        }
        //菜单悬浮窗不透明度
        item {
            MenuSliderLayout(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(R.string.game_menu_option_menu_ball_opacity),
                value = AllSettings.menuBallOpacity.state,
                valueRange = AllSettings.menuBallOpacity.floatRange,
                onValueChange = { value ->
                    AllSettings.menuBallOpacity.updateState(value)
                },
                onValueChangeFinished = { value ->
                    AllSettings.menuBallOpacity.save(value)
                },
                suffix = "%",
                color = color,
                contentColor = contentColor,
                enabled = AllSettings.showMenuBall.state
            )
        }
        //帧率显示
        item {
            MenuSwitchButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.game_menu_option_switch_fps),
                switch = AllSettings.showFPS.state,
                onSwitch = { AllSettings.showFPS.save(it) },
                color = color,
                contentColor = contentColor,
                enabled = AllSettings.showMenuBall.state
            )
        }
        //帧率显示模式
        item {
            MenuListLayout(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(R.string.game_menu_option_fps_display_mode),
                items = FpsDisplayMode.entries,
                currentItem = AllSettings.fpsDisplayMode.state,
                onItemChange = { mode ->
                    AllSettings.fpsDisplayMode.save(mode)
                },
                getItemText = { stringResource(it.nameRes) },
                color = color,
                contentColor = contentColor,
                enabled = AllSettings.showMenuBall.state && AllSettings.showFPS.state
            )
        }
        //内存显示
        item {
            MenuSwitchButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.game_menu_option_switch_memory),
                switch = AllSettings.showMemory.state,
                onSwitch = { AllSettings.showMemory.save(it) },
                color = color,
                contentColor = contentColor,
                enabled = AllSettings.showMenuBall.state
            )
        }
        //Liquid Glass 开关
        item {
            MenuSwitchButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.game_menu_option_liquid_glass),
                switch = AllSettings.gameLiquidGlass.state,
                onSwitch = { AllSettings.gameLiquidGlass.save(it) },
                color = color,
                contentColor = contentColor
            )
        }
        //请求最高刷新率
        item {
            MenuSwitchButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.settings_renderer_max_refresh_rate_title),
                switch = AllSettings.requestMaxRefreshRate.state,
                onSwitch = { AllSettings.requestMaxRefreshRate.save(it) },
                color = color,
                contentColor = contentColor,
            )
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
        }

        // 分辨率规则与游戏窗口分辨率
        item {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // 分辨率规则
                MenuListLayout(
                    modifier = Modifier.fillMaxWidth(),
                    title = stringResource(R.string.settings_renderer_resolution_rule_title),
                    items = ResolutionRule.entries,
                    currentItem = AllSettings.resolutionRule.state,
                    onItemChange = { rule ->
                        AllSettings.resolutionRule.save(rule)
                        // 自定义分辨率尚未初始化时，以屏幕真实宽高填充
                        if (rule == ResolutionRule.CUSTOM) {
                            ensureCustomResolutionInitialized(context)
                        }
                        onRefreshWindowSize()
                    },
                    getItemText = { stringResource(it.nameRes) },
                    color = color,
                    contentColor = contentColor,
                )

                // 百分比分辨率
                AnimatedVisibility(
                    visible = AllSettings.resolutionRule.state == ResolutionRule.PERCENTAGE,
                    enter = fadeIn(animationSpec = getAnimateTween()) +
                            expandVertically(animationSpec = getAnimateTween()),
                    exit = fadeOut(animationSpec = getAnimateTween()) +
                            shrinkVertically(animationSpec = getAnimateTween())
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        MenuSliderLayout(
                            modifier = Modifier.fillMaxWidth(),
                            title = stringResource(R.string.settings_renderer_resolution_scale_title),
                            value = AllSettings.resolutionRatio.state,
                            valueRange = AllSettings.resolutionRatio.floatRange,
                            onValueChange = { value ->
                                AllSettings.resolutionRatio.updateState(value)
                            },
                            onValueChangeFinished = { value ->
                                AllSettings.resolutionRatio.save(value)
                                onRefreshWindowSize()
                            },
                            suffix = "%",
                            color = color,
                            contentColor = contentColor,
                        )
                    }
                }

                // 自定义分辨率
                AnimatedVisibility(
                    visible = AllSettings.resolutionRule.state == ResolutionRule.CUSTOM,
                    enter = fadeIn(animationSpec = getAnimateTween()) +
                            expandVertically(animationSpec = getAnimateTween()),
                    exit = fadeOut(animationSpec = getAnimateTween()) +
                            shrinkVertically(animationSpec = getAnimateTween())
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        CustomResolutionContent(onValueCommitted = onRefreshWindowSize)
                    }
                }
            }
        }
    }
}

/**
 * 自定义分辨率的宽高输入卡片
 */
@Composable
private fun CustomResolutionContent(
    onValueCommitted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val screenSize = remember(context) { getRealScreenSize(context) }

    BackgroundCard(
        modifier = modifier.fillMaxWidth(),
        influencedByBackground = false,
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_renderer_resolution_scale_title),
                style = MaterialTheme.typography.titleSmall
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IntInputField(
                    modifier = Modifier.weight(1f),
                    value = AllSettings.customResolutionWidth.state,
                    permitted = customResolutionRange(screenSize.width),
                    label = stringResource(R.string.settings_renderer_resolution_custom_width),
                    onValueChange = { value ->
                        AllSettings.customResolutionWidth.save(value)
                        onValueCommitted()
                    }
                )
                IntInputField(
                    modifier = Modifier.weight(1f),
                    value = AllSettings.customResolutionHeight.state,
                    permitted = customResolutionRange(screenSize.height),
                    label = stringResource(R.string.settings_renderer_resolution_custom_height),
                    onValueChange = { value ->
                        AllSettings.customResolutionHeight.save(value)
                        onValueCommitted()
                    }
                )
            }
        }
    }
}

@Composable
private fun ControlOverview(
    modifier: Modifier = Modifier,
    color: Color = cardColor(false),
    contentColor: Color = onCardColor(),
    closeScreen: () -> Unit,
    onInputMethod: () -> Unit,
    onSendKeycode: () -> Unit,
    onReplacementControl: () -> Unit,
    onEditLayout: () -> Unit
) {
    val listState = rememberLazyListState()
    val sdlEnabled by SdlBridge.enabled.collectAsState()

    LazyColumn(
        modifier = modifier.lazyScrollWithBar(listState),
        state = listState,
        contentPadding = PaddingValues(all = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        //切换输入法
        item {
            MenuTextButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.game_menu_option_input_method),
                onClick = {
                    onInputMethod()
                    closeScreen()
                },
                color = color,
                contentColor = contentColor,
            )
        }
        //自动唤起输入法（SDL）
        item {
            MenuSwitchButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.game_menu_option_auto_show_ime),
                switch = AllSettings.sdlAutoShowIme.state,
                onSwitch = { AllSettings.sdlAutoShowIme.save(it) },
                enabled = sdlEnabled,
                color = color,
                contentColor = contentColor,
            )
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
        }

        //发送键值
        item {
            MenuTextButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.game_menu_option_send_keycode),
                onClick = {
                    onSendKeycode()
                    closeScreen()
                },
                color = color,
                contentColor = contentColor,
            )
        }
        //更换控制布局
        item {
            MenuTextButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.game_menu_option_replacement_control),
                onClick = {
                    onReplacementControl()
                    closeScreen()
                },
                color = color,
                contentColor = contentColor,
            )
        }
        //编辑布局
        item {
            MenuTextButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.control_manage_info_edit),
                onClick = {
                    onEditLayout()
                    closeScreen()
                },
                color = color,
                contentColor = contentColor,
            )
        }
        //控制布局不透明度
        item {
            MenuSliderLayout(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(R.string.game_menu_option_controls_opacity),
                value = AllSettings.controlsOpacity.state,
                valueRange = AllSettings.controlsOpacity.floatRange,
                onValueChange = { AllSettings.controlsOpacity.updateState(it) },
                onValueChangeFinished = { AllSettings.controlsOpacity.save(it) },
                suffix = "%",
                color = color,
                contentColor = contentColor,
            )
        }
    }
}

@Composable
private fun ControlMouse(
    modifier: Modifier = Modifier,
    color: Color = cardColor(false),
    contentColor: Color = onCardColor(),
) {
    val listState = rememberLazyListState()
    LazyColumn(
        modifier = modifier.lazyScrollWithBar(listState),
        state = listState,
        contentPadding = PaddingValues(all = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        //隐藏虚拟鼠标
        item {
            MenuSwitchButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.settings_control_mouse_hide_title),
                switch = AllSettings.hideMouse.state,
                onSwitch = { AllSettings.hideMouse.save(it) },
                color = color,
                contentColor = contentColor,
                enabled = AllSettings.mouseControlMode.state == MouseControlMode.CLICK
            )
        }
        //触控板式操作
        item {
            MenuSwitchButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.settings_control_mouse_enable_click_title),
                switch = AllSettings.enableMouseClick.state,
                onSwitch = { AllSettings.enableMouseClick.save(it) },
                color = color,
                contentColor = contentColor,
                enabled = AllSettings.mouseControlMode.state == MouseControlMode.SLIDE
            )
        }
        //鼠标控制模式
        item {
            MenuListLayout(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(R.string.settings_control_mouse_control_mode_title),
                items = MouseControlMode.entries,
                currentItem = AllSettings.mouseControlMode.state,
                onItemChange = { AllSettings.mouseControlMode.save(it) },
                getItemText = { stringResource(it.nameRes) },
                color = color,
                contentColor = contentColor,
            )
        }
        //虚拟鼠标大小
        item {
            MenuSliderLayout(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(R.string.settings_control_mouse_size_title),
                value = AllSettings.mouseSize.state,
                valueRange = AllSettings.mouseSize.floatRange,
                onValueChange = { AllSettings.mouseSize.updateState(it) },
                onValueChangeFinished = { AllSettings.mouseSize.save(it) },
                suffix = "Dp",
                color = color,
                contentColor = contentColor,
            )
        }
        //虚拟鼠标灵敏度
        item {
            MenuSliderLayout(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(R.string.settings_control_mouse_sensitivity_title),
                value = AllSettings.cursorSensitivity.state,
                valueRange = AllSettings.cursorSensitivity.floatRange,
                onValueChange = { AllSettings.cursorSensitivity.updateState(it) },
                onValueChangeFinished = { AllSettings.cursorSensitivity.save(it) },
                suffix = "%",
                color = color,
                contentColor = contentColor,
            )
        }
        //抓获鼠标滑动灵敏度
        item {
            MenuSliderLayout(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(R.string.settings_control_mouse_capture_sensitivity_title),
                value = AllSettings.mouseCaptureSensitivity.state,
                valueRange = AllSettings.mouseCaptureSensitivity.floatRange,
                onValueChange = { AllSettings.mouseCaptureSensitivity.updateState(it) },
                onValueChangeFinished = { AllSettings.mouseCaptureSensitivity.save(it) },
                suffix = "%",
                color = color,
                contentColor = contentColor,
            )
        }
        //虚拟鼠标长按触发的延迟
        item {
            MenuSliderLayout(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(R.string.settings_control_mouse_long_press_delay_title),
                value = AllSettings.mouseLongPressDelay.state,
                valueRange = AllSettings.mouseLongPressDelay.floatRange,
                onValueChange = { AllSettings.mouseLongPressDelay.updateState(it) },
                onValueChangeFinished = { AllSettings.mouseLongPressDelay.save(it) },
                suffix = "ms",
                color = color,
                contentColor = contentColor,
            )
        }
    }
}

/**
 * Panel perekam video game, satu baris tab dengan pengaturan mouse dan gestur.
 * Status (merekam / dijeda) dibaca dari [RecorderController], jadi selalu sama dengan
 * kondisi perekam yang sebenarnya.
 */
@Composable
private fun ControlRecorder(
    closeScreen: () -> Unit,
    onShowToast: (AndroidStringText, Int) -> Unit,
    modifier: Modifier = Modifier,
    color: Color = cardColor(false),
    contentColor: Color = onCardColor(),
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val isActive = RecorderController.isActive
    val isPaused = RecorderController.isPaused

    // Video only, used when game audio cannot be captured.
    fun startVideoOnly() {
        if (RecorderController.start(context)) {
            onShowToast(androidText(R.string.game_menu_recorder_started), Toast.LENGTH_SHORT)
            closeScreen()
        } else {
            onShowToast(androidText(R.string.game_menu_recorder_start_failed), Toast.LENGTH_LONG)
        }
    }

    // Step 2: the system "start recording" prompt. Android only allows capturing the
    // game's audio with it; RecorderService then starts the recorder with the token.
    val requestScreenCapture = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        if (result.resultCode == Activity.RESULT_OK && data != null) {
            RecorderService.start(context, result.resultCode, data)
            closeScreen()
        } else {
            onShowToast(androidText(R.string.game_menu_recorder_consent_cancelled), Toast.LENGTH_SHORT)
        }
    }

    fun launchScreenCapturePrompt() {
        val manager = context.getSystemService(MediaProjectionManager::class.java)
        if (manager == null) {
            startVideoOnly()
        } else {
            requestScreenCapture.launch(manager.createScreenCaptureIntent())
        }
    }

    // Step 1: RECORD_AUDIO, which audio playback capture requires.
    val requestAudioPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            launchScreenCapturePrompt()
        } else {
            onShowToast(androidText(R.string.game_menu_recorder_audio_denied), Toast.LENGTH_LONG)
            startVideoOnly()
        }
    }

    fun startRecordingWithAudio() {
        when {
            Build.VERSION.SDK_INT < Build.VERSION_CODES.Q -> startVideoOnly()
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED -> launchScreenCapturePrompt()
            else -> requestAudioPermission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    LazyColumn(
        modifier = modifier.lazyScrollWithBar(listState),
        state = listState,
        contentPadding = PaddingValues(all = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        //状态
        item {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                text = stringResource(
                    when {
                        !isActive -> R.string.game_menu_recorder_status_idle
                        isPaused -> R.string.game_menu_recorder_status_paused
                        else -> R.string.game_menu_recorder_status_recording
                    }
                ),
                style = MaterialTheme.typography.titleSmall
            )
        }
        //开始录制
        item {
            MenuTextButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.game_menu_recorder_start),
                enabled = !isActive,
                onClick = { startRecordingWithAudio() },
                color = color,
                contentColor = contentColor,
            )
        }
        //暂停 / 继续
        item {
            MenuTextButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(
                    if (isPaused) R.string.game_menu_recorder_resume else R.string.game_menu_recorder_pause
                ),
                enabled = isActive,
                onClick = {
                    if (isPaused) {
                        RecorderController.resume()
                        closeScreen()
                    } else {
                        RecorderController.pause()
                    }
                },
                color = color,
                contentColor = contentColor,
            )
        }
        //停止并保存
        item {
            MenuTextButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.game_menu_recorder_stop),
                enabled = isActive,
                onClick = {
                    RecorderController.stop(context)
                    onShowToast(
                        androidText(R.string.game_menu_recorder_stopped),
                        Toast.LENGTH_SHORT
                    )
                },
                color = color,
                contentColor = contentColor,
            )
        }
        //说明
        item {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                text = stringResource(R.string.game_menu_recorder_hint),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun ControlGesture(
    modifier: Modifier = Modifier,
    color: Color = cardColor(false),
    contentColor: Color = onCardColor(),
) {
    val listState = rememberLazyListState()
    LazyColumn(
        modifier = modifier.lazyScrollWithBar(listState),
        state = listState,
        contentPadding = PaddingValues(all = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        //手势控制
        item {
            MenuSwitchButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.settings_control_gesture_control_title),
                switch = AllSettings.gestureControl.state,
                onSwitch = { AllSettings.gestureControl.save(it) },
                color = color,
                contentColor = contentColor,
            )
        }

        //点击触发的操作类型
        item {
            MenuListLayout(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(R.string.settings_control_gesture_tap_action_title),
                items = GestureActionType.entries,
                currentItem = AllSettings.gestureTapMouseAction.state,
                onItemChange = { AllSettings.gestureTapMouseAction.save(it) },
                getItemText = { stringResource(it.nameRes) },
                color = color,
                contentColor = contentColor,
                enabled = AllSettings.gestureControl.state
            )
        }

        //长按触发的操作类型
        item {
            MenuListLayout(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(R.string.settings_control_gesture_long_press_action_title),
                items = GestureActionType.entries,
                currentItem = AllSettings.gestureLongPressMouseAction.state,
                onItemChange = { AllSettings.gestureLongPressMouseAction.save(it) },
                getItemText = { stringResource(it.nameRes) },
                color = color,
                contentColor = contentColor,
                enabled = AllSettings.gestureControl.state
            )
        }

        //手势长按触发的延迟
        item {
            MenuSliderLayout(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(R.string.settings_control_gesture_long_press_delay_title),
                value = AllSettings.gestureLongPressDelay.state,
                valueRange = AllSettings.gestureLongPressDelay.floatRange,
                enabled = AllSettings.gestureControl.state,
                onValueChange = { AllSettings.gestureLongPressDelay.updateState(it) },
                onValueChangeFinished = { AllSettings.gestureLongPressDelay.save(it) },
                suffix = "ms",
                color = color,
                contentColor = contentColor,
            )
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
        }

        //快捷栏定位规则
        item {
            MenuListLayout(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(R.string.game_menu_option_hotbar_rule),
                items = HotbarRule.entries,
                currentItem = AllSettings.hotbarRule.state,
                onItemChange = { AllSettings.hotbarRule.save(it) },
                getItemText = { stringResource(it.nameRes) },
                color = color,
                contentColor = contentColor,
            )
        }

        //快捷栏宽度
        item {
            MenuSliderLayout(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(R.string.game_menu_option_hotbar_width),
                value = AllSettings.hotbarWidth.state / 10f,
                valueRange = 0f..100f,
                enabled = AllSettings.hotbarRule.state == HotbarRule.Custom,
                onValueChange = { value ->
                    AllSettings.hotbarWidth.updateState((value * 10f).toInt())
                },
                onValueChangeFinished = { value ->
                    AllSettings.hotbarWidth.save((value * 10f).toInt())
                },
                suffix = "%",
                color = color,
                contentColor = contentColor,
            )
        }

        //快捷栏高度
        item {
            MenuSliderLayout(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(R.string.game_menu_option_hotbar_height),
                value = AllSettings.hotbarHeight.state / 10f,
                valueRange = 0f..100f,
                enabled = AllSettings.hotbarRule.state == HotbarRule.Custom,
                onValueChange = { value ->
                    AllSettings.hotbarHeight.updateState((value * 10f).toInt())
                },
                onValueChangeFinished = { value ->
                    AllSettings.hotbarHeight.save((value * 10f).toInt())
                },
                suffix = "%",
                color = color,
                contentColor = contentColor,
            )
        }

        //快捷栏双击与副手交换物品
        item {
            MenuSwitchButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.game_menu_option_hotbar_double_click),
                switch = AllSettings.hotbarDoubleClick.state,
                onSwitch = { AllSettings.hotbarDoubleClick.save(it) },
                color = color,
                contentColor = contentColor,
            )
        }

        //快捷栏长按丢弃所选物品
        item {
            MenuSwitchButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.game_menu_option_hotbar_long_click),
                switch = AllSettings.hotbarLongClick.state,
                onSwitch = { AllSettings.hotbarLongClick.save(it) },
                color = color,
                contentColor = contentColor,
            )
        }

        //快捷栏长按快捷栏触发延迟
        item {
            MenuSliderLayout(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(R.string.game_menu_option_hotbar_long_click_delay),
                value = AllSettings.hotbarLongClickDelay.state,
                valueRange = AllSettings.hotbarLongClickDelay.floatRange,
                onValueChange = { value ->
                    AllSettings.hotbarLongClickDelay.updateState(value)
                },
                onValueChangeFinished = { value ->
                    AllSettings.hotbarLongClickDelay.save(value)
                },
                suffix = "ms",
                enabled = AllSettings.hotbarLongClick.state,
                color = color,
                contentColor = contentColor,
            )
        }
    }
}
