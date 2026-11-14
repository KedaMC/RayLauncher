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

package com.movtery.zalithlauncher.ui.screens.main

import com.movtery.cardgrid.state.rememberCardGridState
import com.movtery.zalithlauncher.ui.components.effectiveBackgroundOpacity
import com.movtery.zalithlauncher.ui.components.liquidGlassActive
import com.movtery.zalithlauncher.ui.components.SharedGlassBlurPage
import com.movtery.zalithlauncher.ui.components.glassTint
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import com.movtery.zalithlauncher.ui.components.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.RichTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import com.movtery.zalithlauncher.ui.components.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.entryProvider
import com.movtery.zalithlauncher.BuildKeys
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.coroutine.TaskSystem
import com.movtery.zalithlauncher.game.version.installed.Version
import com.movtery.zalithlauncher.game.version.installed.VersionsManager
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.ui.AndroidStringText
import com.movtery.zalithlauncher.ui.androidText
import com.movtery.zalithlauncher.ui.components.BackgroundCard
import com.movtery.zalithlauncher.ui.components.CardTitleLayout
import com.movtery.zalithlauncher.ui.components.LiquidGlassSurface
import com.movtery.zalithlauncher.ui.components.liquidGlassBackdrop
import com.movtery.zalithlauncher.ui.screens.BackStackNavKey
import com.movtery.zalithlauncher.ui.screens.NavStateHost
import com.movtery.zalithlauncher.ui.screens.NestedNavDisplay
import com.movtery.zalithlauncher.ui.screens.NestedNavKey
import com.movtery.zalithlauncher.ui.screens.NormalNavKey
import com.movtery.zalithlauncher.ui.screens.PAGE_SLIDE_MILLIS
import com.movtery.zalithlauncher.ui.screens.PageSlideTracker
import com.movtery.zalithlauncher.ui.screens.pageIndexOf
import com.movtery.zalithlauncher.ui.screens.rememberPageSlideSpec
import com.movtery.zalithlauncher.ui.screens.TitledNavKey
import com.movtery.zalithlauncher.ui.screens.content.AccountManageScreen
import com.movtery.zalithlauncher.ui.screens.content.DownloadScreen
import com.movtery.zalithlauncher.ui.screens.content.FileSelectorScreen
import com.movtery.zalithlauncher.ui.screens.content.LauncherScreen
import com.movtery.zalithlauncher.ui.screens.content.LicenseScreen
import com.movtery.zalithlauncher.ui.screens.content.LogViewScreen
import com.movtery.zalithlauncher.ui.screens.content.SettingsScreen
import com.movtery.zalithlauncher.ui.screens.content.VersionExportScreen
import com.movtery.zalithlauncher.ui.screens.content.VersionSettingsScreen
import com.movtery.zalithlauncher.ui.screens.content.VersionsManageScreen
import com.movtery.zalithlauncher.ui.screens.content.WebViewScreen
import com.movtery.zalithlauncher.ui.screens.content.addoninfo.AddonInfoScreen
import com.movtery.zalithlauncher.ui.screens.content.navigateToDownload
import com.movtery.zalithlauncher.ui.screens.navigateTo
import com.movtery.zalithlauncher.ui.screens.onBack
import com.movtery.zalithlauncher.ui.theme.backgroundColor
import com.movtery.zalithlauncher.ui.theme.cardColor
import com.movtery.zalithlauncher.ui.theme.festivals.FestivalTitleText
import com.movtery.zalithlauncher.ui.theme.onBackgroundColor
import com.movtery.zalithlauncher.ui.theme.onCardColor
import com.movtery.zalithlauncher.utils.animation.SwapTimeline
import com.movtery.zalithlauncher.utils.animation.getAnimateTween
import com.movtery.zalithlauncher.utils.festival.LocalFestivals
import com.movtery.zalithlauncher.utils.file.formatFileSize
import com.movtery.zalithlauncher.viewmodel.ErrorViewModel
import com.movtery.zalithlauncher.viewmodel.EventViewModel
import com.movtery.zalithlauncher.viewmodel.LocalBackgroundViewModel
import com.movtery.zalithlauncher.viewmodel.ModifyVersionViewModel
import com.movtery.zalithlauncher.viewmodel.ModpackImportViewModel
import com.movtery.zalithlauncher.viewmodel.ScreenBackStackViewModel
import com.movtery.zalithlauncher.viewmodel.sendKeepScreen
import com.movtery.zalithlauncher.viewmodel.sendToast
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

private const val NAV_AUTO_HIDE_MS = 3000L

@Composable
fun MainScreen(
    screenBackStackModel: ScreenBackStackViewModel,
    eventViewModel: EventViewModel,
    modpackImportViewModel: ModpackImportViewModel,
    modifyVersionViewModel: ModifyVersionViewModel,
    submitError: (ErrorViewModel.ThrowableMessage) -> Unit
) {
    //监控当前是否有任务正在进行
    var taskRunning by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        TaskSystem.tasksFlow.collect { tasks ->
            val noTasks = tasks.isEmpty()
            eventViewModel.sendKeepScreen(!noTasks)
            taskRunning = noTasks
        }
    }

    val isTaskMenuExpanded = AllSettings.launcherTaskMenuExpanded.state

    fun changeTasksExpandedState() {
        AllSettings.launcherTaskMenuExpanded.save(!isTaskMenuExpanded)
    }

    /** 回到主页面通用函数 */
    val toMainScreen: () -> Unit = {
        screenBackStackModel.mainScreen.clearWith(NormalNavKey.LauncherMain)
    }

    val mainScreenKey = screenBackStackModel.mainScreen.currentKey
    val inLauncherScreen = mainScreenKey == null || mainScreenKey is NormalNavKey.LauncherMain

    //Which main page the top navigation highlights: the topmost main page in the stack
    val mainBackStack = screenBackStackModel.mainScreen.backStack
    val navIndex = mainBackStack.lastOrNull { pageIndexOf(it) >= 0 }?.let { pageIndexOf(it) } ?: 0
    val autoHideEnabled = mainBackStack.lastOrNull().let { it == null || pageIndexOf(it) == 0 }

    //Auto-hide of the navigation: Home only. Shown on open, hidden after 3s without touch,
    //shown again on touch, never hidden while a page slide runs
    val lastTouch = remember { mutableLongStateOf(0L) }
    var navVisible by remember { mutableStateOf(true) }
    var pageTransitioning by remember { mutableStateOf(false) }
    LaunchedEffect(navIndex) {
        pageTransitioning = true
        delay(PAGE_SLIDE_MILLIS + 120L)
        pageTransitioning = false
    }
    LaunchedEffect(autoHideEnabled, pageTransitioning) {
        navVisible = true
        if (!autoHideEnabled || pageTransitioning) return@LaunchedEffect
        snapshotFlow { lastTouch.longValue }.collectLatest {
            navVisible = true
            delay(NAV_AUTO_HIDE_MS)
            navVisible = false
        }
    }
    val navShown = !autoHideEnabled || navVisible

    val slideTracker = remember { PageSlideTracker() }

    val isBackgroundValid = LocalBackgroundViewModel.current?.isValid == true
    val launcherBackgroundOpacity = effectiveBackgroundOpacity().toFloat() / 100f

    //With Liquid Glass on, a light theme veil over the wallpaper would wash it out to white
    val backgroundColor = if (isBackgroundValid) {
        val base = if (liquidGlassActive()) glassTint(backgroundColor()) else backgroundColor()
        base.copy(alpha = launcherBackgroundOpacity)
    } else backgroundColor()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = backgroundColor,
        contentColor = onBackgroundColor()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    //Observe only: never consumes, so children keep their own touch handling
                    awaitPointerEventScope {
                        while (true) {
                            awaitPointerEvent(PointerEventPass.Initial)
                            lastTouch.longValue = System.nanoTime()
                        }
                    }
                }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
            ) {
                TopBar(
                    modifier = Modifier.fillMaxSize(),
                    mainScreenKey = mainScreenKey,
                    inLauncherScreen = inLauncherScreen,
                    taskRunning = taskRunning,
                    isTasksExpanded = isTaskMenuExpanded,
                    contentColor = onBackgroundColor(),
                    onScreenBack = {
                        screenBackStackModel.mainScreen.backStack.removeFirstOrNull()
                    },
                    navIndex = navIndex,
                    navShown = navShown,
                    onNavigate = { page ->
                        when (page) {
                            MainPage.HOME -> if (pageIndexOf(mainBackStack.lastOrNull()) != 0) {
                                //Back to the Home group page that was open last (Home Screen, Version List,
                                //Account list or Manage version): drop everything stacked above it
                                val lastHome = mainBackStack.indexOfLast { pageIndexOf(it) == 0 }
                                if (lastHome >= 0) {
                                    repeat(mainBackStack.size - lastHome - 1) { mainBackStack.removeLastOrNull() }
                                } else {
                                    toMainScreen()
                                }
                            }
                            MainPage.DOWNLOAD -> if (mainScreenKey !is NestedNavKey.Download) {
                                screenBackStackModel.navigateToDownload()
                            }
                            MainPage.SETTINGS -> if (mainScreenKey !is NestedNavKey.Settings) {
                                screenBackStackModel.mainScreen.removeAndNavigateTo(
                                    removes = screenBackStackModel.clearBeforeNavKeys,
                                    screenKey = screenBackStackModel.settingsScreen
                                )
                            }
                        }
                    },
                    changeExpandedState = {
                        changeTasksExpandedState()
                    },
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                NavStateHost(rootKey = screenBackStackModel.mainScreen) {
                    NavigationUI(
                        modifier = Modifier.fillMaxSize(),
                        screenBackStackModel = screenBackStackModel,
                        toMainScreen = toMainScreen,
                        eventViewModel = eventViewModel,
                        modpackImportViewModel = modpackImportViewModel,
                        modifyVersionViewModel = modifyVersionViewModel,
                        slideTracker = slideTracker,
                        submitError = submitError
                    )
                }

                TaskMenu(
                    isExpanded = isTaskMenuExpanded,
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(0.3f)
                        .align(Alignment.CenterStart)
                        .padding(all = 6.dp)
                ) {
                    changeTasksExpandedState()
                }
            }
        }
    }
}

@Composable
private fun <E: TitledNavKey> TopBar(
    mainScreenKey: E?,
    inLauncherScreen: Boolean,
    taskRunning: Boolean,
    isTasksExpanded: Boolean,
    navIndex: Int,
    navShown: Boolean,
    modifier: Modifier = Modifier,
    contentColor: Color,
    onScreenBack: () -> Unit,
    onNavigate: (MainPage) -> Unit,
    changeExpandedState: () -> Unit,
) {
    val festivals = LocalFestivals.current

    CompositionLocalProvider(
        LocalContentColor provides if (liquidGlassActive()) Color.White else contentColor
    ) {
        ConstraintLayout(modifier = modifier) {
            val (backCenter, title, navGroup, endButtons) = createRefs()
            //Title spacing below is glass-only; with glass off the title keeps the stock layout
            val titleGlass = liquidGlassBackdrop() != null

            val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

            Row(
                modifier = Modifier
                    .constrainAs(backCenter) {
                        start.linkTo(parent.start)
                        top.linkTo(parent.top)
                        bottom.linkTo(parent.bottom)
                    }
                    .fillMaxHeight()
            ) {
                AnimatedVisibility(
                    visible = !inLauncherScreen
                ) {
                    Row(modifier = Modifier.fillMaxHeight()) {
                        Spacer(Modifier.width(12.dp))

                        ToolbarTooltip(label = stringResource(R.string.generic_back)) {
                            IconButton(
                                modifier = Modifier.fillMaxHeight(),
                                onClick = {
                                    if (!inLauncherScreen) {
                                        //不在主屏幕时才允许返回
                                        backDispatcher?.onBackPressed() ?: run {
                                            onScreenBack()
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    modifier = Modifier.size(24.dp),
                                    painter = painterResource(R.drawable.ic_arrow_back),
                                    contentDescription = stringResource(R.string.generic_back)
                                )
                            }
                        }
                    }
                }
            }
            val parentRes = mainScreenKey?.title
            val childRes = (mainScreenKey as? BackStackNavKey<*>)?.currentKey?.title
            //The page title is plain text on the page background. White only works where the glass veil
            //over a wallpaper darkens it; without a wallpaper use the theme's content color so it stays readable
            val titleColor = if (liquidGlassActive() && LocalBackgroundViewModel.current?.isValid == true) {
                Color.White
            } else {
                contentColor
            }

            Crossfade(
                modifier = Modifier.constrainAs(title) {
                    centerVerticallyTo(parent)
                    start.linkTo(backCenter.end, margin = 16.dp)
                    // Left-aligned at a fixed start point, never running under the centered navigation.
                    // The title area always fills the space between the Back button and the navigation,
                    // so it can never collapse to zero width
                    end.linkTo(navGroup.start, margin = 8.dp)
                    width = Dimension.fillToConstraints
                },
                targetState = parentRes to childRes
            ) { (parent, child) ->
                val style = MaterialTheme.typography.titleMedium
                val softWarp = false
                val maxLines = 1
                //Only the Home festival greeting sits on a glass pill. Decide it from the content being drawn
                //(parent == null), not from the live page: while Crossfade fades the old page title out,
                //the live page is already Home and the fading title would be wrapped in glass.
                val titleBackdrop = if (parent == null) liquidGlassBackdrop() else null

                @Composable
                fun TitleContent() {
                    if (parent == null) {
                        //Home shows no title text, except the festival greeting when one is active
                        if (festivals.isNotEmpty()) {
                            FestivalTitleText(festivals = festivals, style = style, maxLines = maxLines)
                        }
                    } else {
                        val titleText = if (child != null) androidText(parent, androidText(" - "), child) else parent
                        CompositionLocalProvider(LocalContentColor provides titleColor) {
                            AndroidStringText(
                                modifier = if (titleGlass) Modifier.padding(horizontal = 10.dp, vertical = 5.dp) else Modifier,
                                text = titleText,
                                style = style,
                                overflow = TextOverflow.Ellipsis,
                                softWrap = softWarp,
                                maxLines = maxLines
                            )
                        }
                    }
                }

                if (parent == null && festivals.isEmpty()) {
                    //Nothing to show on Home: no empty glass pill either
                } else if (titleBackdrop != null) {
                    SharedGlassBlurPage {
                        LiquidGlassSurface(
                            backdrop = titleBackdrop,
                            shape = MaterialTheme.shapes.large,
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = Color.White,
                            blurRadius = 10.dp
                        ) {
                            TitleContent()
                        }
                    }
                } else {
                    TitleContent()
                }
            }

            MainNavigationBar(
                modifier = Modifier.constrainAs(navGroup) {
                    centerHorizontallyTo(parent)
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                },
                selectedIndex = navIndex,
                shown = navShown,
                onNavigate = onNavigate
            )

            Row(
                modifier = Modifier
                    .constrainAs(endButtons) {
                        top.linkTo(parent.top)
                        bottom.linkTo(parent.bottom)
                        end.linkTo(parent.end, margin = 12.dp)
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AnimatedVisibility(
                    visible = !(isTasksExpanded || taskRunning),
                    enter = slideInVertically(
                        initialOffsetY = { -50 }
                    ) + fadeIn(),
                    exit = slideOutVertically(
                        targetOffsetY = { -50 }
                    ) + fadeOut()
                ) {
                    ToolbarTooltip(label = stringResource(R.string.main_task_menu)) {
                        Row(
                            modifier = Modifier
                                .clip(shape = MaterialTheme.shapes.large)
                                .clickable { changeExpandedState() }
                                .padding(all = 8.dp)
                                .width(120.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LinearProgressIndicator(modifier = Modifier.weight(1f))
                            Icon(
                                modifier = Modifier.size(22.dp),
                                painter = painterResource(R.drawable.ic_assignment_filled),
                                contentDescription = stringResource(R.string.main_task_menu)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ToolbarTooltip(
    label: String,
    content: @Composable () -> Unit
) {
    //Tooltips are part of the glass toolbar only; stock toolbar has none
    if (liquidGlassBackdrop() == null) {
        content()
        return
    }
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
            positioning = TooltipAnchorPosition.Below
        ),
        tooltip = {
            RichTooltip(shadowElevation = 3.dp) {
                Text(text = label)
            }
        },
        state = rememberTooltipState(),
        content = content
    )
}

@Composable
private fun NavigationUI(
    modifier: Modifier = Modifier,
    screenBackStackModel: ScreenBackStackViewModel,
    toMainScreen: () -> Unit,
    eventViewModel: EventViewModel,
    modpackImportViewModel: ModpackImportViewModel,
    modifyVersionViewModel: ModifyVersionViewModel,
    slideTracker: PageSlideTracker,
    submitError: (ErrorViewModel.ThrowableMessage) -> Unit
) {
    val backStack = screenBackStackModel.mainScreen.backStack
    val currentKey = backStack.lastOrNull()

    //Direction of the full-page slide follows page order, so Back plays the reverse
    slideTracker.update(currentKey)
    LaunchedEffect(currentKey) {
        if (slideTracker.direction() != 0) {
            delay(PAGE_SLIDE_MILLIS + 80L)
            SwapTimeline.pageSlideActive = false
        }
    }
    //主页网格状态随主导航存活，离开主页再返回时不会重建、重播种与重放入场动画
    val homeGridState = rememberCardGridState()

    LaunchedEffect(currentKey) {
        screenBackStackModel.mainScreen.currentKey = currentKey
    }

    if (backStack.isNotEmpty()) {
        /** 导航至版本详细信息屏幕 */
        val navigateToVersions: (Version) -> Unit = { version ->
            screenBackStackModel.mainScreen.navigateTo(
                screenKey = NestedNavKey.VersionSettings(version),
                useClassEquality = true
            )
        }
        /** 导航至整合包导出屏幕 */
        val navigateToExport: (Version) -> Unit = { version ->
            screenBackStackModel.mainScreen.removeAndNavigateTo(
                remove = NestedNavKey.VersionSettings::class,
                screenKey = NestedNavKey.VersionExport(version),
                useClassEquality = true
            )
        }

        NestedNavDisplay(
            ownerKey = screenBackStackModel.mainScreen,
            backStack = backStack,
            modifier = modifier,
            onBack = {
                onBack(backStack)
            },
            transitionSpec = rememberPageSlideSpec(slideTracker),
            popTransitionSpec = rememberPageSlideSpec(slideTracker),
            entryProvider = entryProvider {
                entry<NormalNavKey.LauncherMain> {
                    SharedGlassBlurPage {
                        LauncherScreen(
                            backStackViewModel = screenBackStackModel,
                            gridState = homeGridState,
                            navigateToVersions = navigateToVersions,
                            onLaunchGame = { version ->
                                eventViewModel.sendEvent(
                                    EventViewModel.Event.Launch.Game(version)
                                )
                            },
                            onQuickPlayServer = { version, address ->
                                val target = (version ?: VersionsManager.currentVersion.value)?.takeIf { it.isValid() }
                                if (target != null) {
                                    eventViewModel.sendEvent(
                                        EventViewModel.Event.Launch.PlayServer(target, address)
                                    )
                                } else {
                                    eventViewModel.sendToast(androidText(R.string.game_launch_no_version))
                                    screenBackStackModel.mainScreen.navigateTo(NormalNavKey.VersionsManager)
                                }
                            },
                            onOpenLink = {
                                eventViewModel.sendEvent(EventViewModel.Event.OpenLink(it))
                            }
                        )
                    }
                }
                entry<NestedNavKey.Settings> { key ->
                    SettingsScreen(
                        key = key,
                        backStackViewModel = screenBackStackModel,
                        openLicenseScreen = { raw ->
                            backStack.navigateTo(NormalNavKey.License(raw))
                        },
                        eventViewModel = eventViewModel,
                        submitError = submitError
                    )
                }
                entry<NormalNavKey.License> { key ->
                    LicenseScreen(
                        key = key,
                        backStackViewModel = screenBackStackModel
                    )
                }
                entry<NormalNavKey.AccountManager> { key ->
                    SharedGlassBlurPage {
                        AccountManageScreen(
                            key = key,
                            backStackViewModel = screenBackStackModel,
                            backToMainScreen = toMainScreen,
                            openLink = { url ->
                                eventViewModel.sendEvent(EventViewModel.Event.OpenLink(url))
                            },
                            eventViewModel = eventViewModel,
                            submitError = submitError
                        )
                    }
                }
                entry<NormalNavKey.WebScreen> { key ->
                    WebViewScreen(
                        key = key,
                        backStackViewModel = screenBackStackModel,
                        eventViewModel = eventViewModel
                    )
                }
                entry<NormalNavKey.VersionsManager> {
                    SharedGlassBlurPage {
                        VersionsManageScreen(
                            backScreenViewModel = screenBackStackModel,
                            navigateToVersions = navigateToVersions,
                            navigateToExport = navigateToExport,
                            eventViewModel = eventViewModel,
                            submitError = submitError
                        )
                    }
                }
                entry<NormalNavKey.FileSelector> { key ->
                    FileSelectorScreen(
                        key = key,
                        backScreenViewModel = screenBackStackModel
                    ) {
                        backStack.removeLastOrNull()
                    }
                }
                entry<NestedNavKey.VersionSettings> { key ->
                    SharedGlassBlurPage {
                        VersionSettingsScreen(
                            key = key,
                            modifyViewModel = modifyVersionViewModel,
                            backScreenViewModel = screenBackStackModel,
                            backToMainScreen = toMainScreen,
                            onExportModpack = {
                                navigateToExport(key.version)
                            },
                            eventViewModel = eventViewModel,
                            submitError = submitError
                        )
                    }
                }
                entry<NestedNavKey.VersionExport> { key ->
                    SharedGlassBlurPage {
                        VersionExportScreen(
                            key = key,
                            backScreenViewModel = screenBackStackModel,
                            eventViewModel = eventViewModel,
                            backToMainScreen = toMainScreen
                        )
                    }
                }
                entry<NestedNavKey.Download> { key ->
                    SharedGlassBlurPage {
                        DownloadScreen(
                            key = key,
                            backScreenViewModel = screenBackStackModel,
                            eventViewModel = eventViewModel,
                            modpackImportViewModel = modpackImportViewModel,
                            submitError = submitError
                        )
                    }
                }
                entry<NestedNavKey.AddonInfo> { key ->
                    SharedGlassBlurPage {
                        AddonInfoScreen(
                            key = key,
                            mainScreenKey = screenBackStackModel.mainScreen.currentKey,
                            addonInfoScreenKey = key.currentKey,
                            eventViewModel = eventViewModel,
                            submitError = submitError,
                        )
                    }
                }
                entry<NormalNavKey.LogView> { key ->
                    LogViewScreen(
                        key = key,
                        backStackViewModel = screenBackStackModel,
                    )
                }
            }
        )
    } else {
        Box(modifier)
    }
}

@Composable
private fun TaskMenu(
    isExpanded: Boolean,
    modifier: Modifier = Modifier,
    changeExpandedState: () -> Unit = {}
) {
    val tasks by TaskSystem.tasksFlow.collectAsStateWithLifecycle()
    val show = isExpanded && tasks.isNotEmpty()

    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    AnimatedVisibility(
        modifier = modifier,
        enter = slideInHorizontally(
            initialOffsetX = { if (isRtl) it else -it },
            animationSpec = getAnimateTween()
        ) + fadeIn(),
        exit = slideOutHorizontally(
            targetOffsetX = { if (isRtl) it else -it },
            animationSpec = getAnimateTween()
        ) + fadeOut(),
        visible = show
    ) {
        BackgroundCard(
            modifier = Modifier
                .fillMaxSize()
                .padding(all = 6.dp),
            influencedByBackground = false,
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(
                containerColor = backgroundColor(),
                contentColor = onBackgroundColor()
            ),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp)
        ) {
            Column {
                CardTitleLayout(blur = 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .padding(top = 8.dp, bottom = 4.dp)
                    ) {
                        IconButton(
                            modifier = Modifier
                                .size(28.dp)
                                .align(Alignment.CenterStart),
                            onClick = changeExpandedState
                        ) {
                            Icon(
                                modifier = Modifier.size(28.dp),
                                painter = painterResource(R.drawable.ic_arrow_left_rounded),
                                contentDescription = stringResource(R.string.generic_collapse)
                            )
                        }

                        Text(
                            modifier = Modifier.align(Alignment.Center),
                            text = stringResource(R.string.main_task_menu)
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    items(tasks, key = { it.id }) { task ->
                        val taskProgress by task.progress.collectAsStateWithLifecycle()
                        val taskMessage by task.message.collectAsStateWithLifecycle()
                        val rateBytesPerSec by task.rateBytesPerSec.collectAsStateWithLifecycle()

                        TaskItem(
                            taskProgress = taskProgress,
                            taskMessage = taskMessage,
                            rateBytesPerSec = rateBytesPerSec,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                        ) {
                            //取消任务
                            TaskSystem.cancelTask(task.id)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskItem(
    taskProgress: Float,
    taskMessage: AndroidStringText?,
    rateBytesPerSec: Long?,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.large,
    color: Color = cardColor(false),
    contentColor: Color = onCardColor(),
    onCancelClick: () -> Unit = {}
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = color,
        contentColor = contentColor,
    ) {
        Row(
            modifier = Modifier.padding(all = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                modifier = Modifier
                    .size(24.dp)
                    .align(Alignment.CenterVertically),
                onClick = onCancelClick
            ) {
                Icon(
                    modifier = Modifier.size(20.dp),
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = stringResource(R.string.generic_cancel)
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .align(Alignment.CenterVertically)
            ) {
                taskMessage?.let { message ->
                    AndroidStringText(
                        text = message,
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                if (taskProgress < 0) { //负数则代表不确定
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    LinearProgressIndicator(
                        progress = { taskProgress },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    taskProgress.takeIf { it >= 0f }?.let { progress ->
                        Text(
                            text = "${(progress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    rateBytesPerSec?.let { bytes ->
                        val text = remember(bytes) { "${formatFileSize(bytes)}/s" }
                        Text(
                            text = text,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    }
}