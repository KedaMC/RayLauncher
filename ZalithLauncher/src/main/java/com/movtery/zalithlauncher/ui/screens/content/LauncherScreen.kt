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

package com.movtery.zalithlauncher.ui.screens.content

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import com.movtery.zalithlauncher.ui.components.DropdownMenu
import com.movtery.zalithlauncher.ui.components.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import com.movtery.zalithlauncher.ui.components.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.movtery.cardgrid.state.CardGridState
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.account.Account
import com.movtery.zalithlauncher.game.account.AccountsManager
import com.movtery.zalithlauncher.game.account.getAccountTypeName
import com.movtery.zalithlauncher.game.version.installed.Version
import com.movtery.zalithlauncher.game.version.installed.VersionsManager
import com.movtery.zalithlauncher.setting.enums.ActionMenuSide
import com.movtery.zalithlauncher.ui.base.BaseScreen
import com.movtery.zalithlauncher.ui.components.BackgroundCard
import com.movtery.zalithlauncher.ui.components.LiquidGlassSurface
import com.movtery.zalithlauncher.ui.components.flatGlass
import com.movtery.zalithlauncher.ui.components.liquidGlassActive
import com.movtery.zalithlauncher.ui.components.liquidGlassBackdrop
import com.movtery.zalithlauncher.ui.components.MarqueeText
import com.movtery.zalithlauncher.ui.components.ScalingActionButton
import com.movtery.zalithlauncher.ui.components.SkinPreview3D
import com.movtery.zalithlauncher.ui.components.animated
import com.movtery.zalithlauncher.ui.components.rememberAnimatedContext
import com.movtery.zalithlauncher.ui.screens.NestedNavKey
import com.movtery.zalithlauncher.ui.screens.NormalNavKey
import com.movtery.zalithlauncher.ui.screens.content.elements.CommonVersionInfoLayout
import com.movtery.zalithlauncher.ui.screens.content.elements.PlayerFace
import com.movtery.zalithlauncher.ui.screens.content.elements.VersionIconImage
import com.movtery.cardgrid.model.CardState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.derivedStateOf
import com.movtery.zalithlauncher.ui.screens.content.home.HomeGrid
import com.movtery.zalithlauncher.ui.screens.content.home.LocalHomeGridState
import com.movtery.zalithlauncher.ui.screens.content.home.server.LocalServerCardQuickPlay
import com.movtery.zalithlauncher.ui.screens.content.home.version.LocalHomeCardLauncher
import com.movtery.zalithlauncher.ui.screens.content.home.version.LocalHomeCardVersionSettings
import com.movtery.zalithlauncher.ui.screens.content.home.workspace.LocalWorkspaceCamera
import com.movtery.zalithlauncher.ui.theme.itemColor
import com.movtery.zalithlauncher.viewmodel.ScreenBackStackViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Rows the launch card needs before it switches to the tall layout with the skin preview */
private const val LaunchCardTallRows = 28

/** Actions the launch card needs from the launcher screen */
class LaunchCardActions(
    val onLaunchGame: (Version?) -> Unit,
    val toAccountManageScreen: () -> Unit,
    val toVersionManageScreen: () -> Unit,
    val toVersionSettingsScreen: () -> Unit
)

val LocalLaunchCardActions = staticCompositionLocalOf<LaunchCardActions?> { null }

@Composable
fun LauncherScreen(
    backStackViewModel: ScreenBackStackViewModel,
    gridState: CardGridState,
    navigateToVersions: (Version) -> Unit,
    onLaunchGame: (Version?) -> Unit,
    onQuickPlayServer: (Version?, String) -> Unit,
    onOpenLink: (String) -> Unit,
) {
    //Home draws edge to edge: the grid also fills the area under the top bar
    BaseScreen(
        screenKey = NormalNavKey.LauncherMain,
        currentKey = backStackViewModel.mainScreen.currentKey,
        edgeToEdge = true
    ) { isVisible ->
        val launchActions = remember(backStackViewModel, onLaunchGame, navigateToVersions) {
            LaunchCardActions(
                onLaunchGame = onLaunchGame,
                toAccountManageScreen = {
                    backStackViewModel.mainScreen.navigateTo(
                        screenKey = NormalNavKey.AccountManager(FirstLoginMenu.NONE)
                    )
                },
                toVersionManageScreen = {
                    backStackViewModel.mainScreen.removeAndNavigateTo(
                        remove = NestedNavKey.VersionSettings::class,
                        screenKey = NormalNavKey.VersionsManager
                    )
                },
                toVersionSettingsScreen = {
                    VersionsManager.currentVersion.value?.let { version ->
                        navigateToVersions(version)
                    }
                }
            )
        }

        CompositionLocalProvider(
            LocalUriHandler provides object : UriHandler {
                override fun openUri(uri: String) {
                    onOpenLink(uri)
                }
            },
            LocalLaunchCardActions provides launchActions
        ) {
            ContentMenu(
                modifier = Modifier.fillMaxSize(),
                isVisible = isVisible,
                gridState = gridState,
                onLaunchGame = { version ->
                    onLaunchGame(version)
                },
                onQuickPlayServer = onQuickPlayServer,
                onOpenVersionSettings = navigateToVersions
            )
        }
    }
}

/**
 * The launch card (account, version, launch button). It is a regular home grid card:
 * it can be placed anywhere, resized and pushed around like the others, but never deleted.
 */
@Composable
fun CardState.LaunchCardContent(cardId: String) {
    val actions = LocalLaunchCardActions.current ?: return
    val gridState = LocalHomeGridState.current
    val account by AccountsManager.currentAccountFlow.collectAsStateWithLifecycle()

    //The skin preview turns towards the middle of the screen
    val camera = LocalWorkspaceCamera.current
    val onRight by remember(gridState, camera, cardId) {
        derivedStateOf {
            val layout = gridState?.cards?.firstOrNull { it.id == cardId }?.layout
            if (gridState == null || camera == null || layout == null) true
            else camera.toViewport(gridState.rectFor(layout)).center.x > camera.viewport.width / 2f
        }
    }

    if (spanHeight >= LaunchCardTallRows && spanWidth >= 10) {
        ActionMenuTallerContent(
            modifier = Modifier.fillMaxSize(),
            account = account,
            dockedSide = if (onRight) ActionMenuSide.END else ActionMenuSide.START,
            onLaunchGame = actions.onLaunchGame,
            toAccountManageScreen = actions.toAccountManageScreen,
            toVersionManageScreen = actions.toVersionManageScreen,
            toVersionSettingsScreen = actions.toVersionSettingsScreen,
        )
    } else {
        ActionMenuCardContent(
            modifier = Modifier.fillMaxSize(),
            account = account,
            onLaunchGame = actions.onLaunchGame,
            toAccountManageScreen = actions.toAccountManageScreen,
            toVersionManageScreen = actions.toVersionManageScreen,
            toVersionSettingsScreen = actions.toVersionSettingsScreen,
        )
    }
}

@Composable
private fun ContentMenu(
    isVisible: Boolean,
    gridState: CardGridState,
    onLaunchGame: (Version) -> Unit,
    onQuickPlayServer: (Version?, String) -> Unit,
    onOpenVersionSettings: (Version) -> Unit,
    modifier: Modifier = Modifier,
) {
    val ctx = rememberAnimatedContext(isVisible = isVisible)

    CompositionLocalProvider(
        LocalHomeCardLauncher provides onLaunchGame,
        LocalHomeCardVersionSettings provides onOpenVersionSettings,
        LocalServerCardQuickPlay provides onQuickPlayServer
    ) {
        HomeGrid(
            state = gridState,
            isVisible = isVisible,
            modifier = modifier
                .fillMaxSize()
                .animated(ctx)
        )
    }
}

private val PanelPadding = 8.dp
private val PanelSpacing = 6.dp
private val PanelInnerShape
    @Composable get() = MaterialTheme.shapes.large

/**
 * Rounded inner card used by the account and instance rows of the launcher panel.
 * Uses a cheap flat glass tint while Liquid Glass is active (the panel itself already draws the full effect).
 */
@Composable
private fun PanelInnerCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val shape = PanelInnerShape
    val background = if (liquidGlassActive()) {
        Modifier.flatGlass(shape, MaterialTheme.colorScheme.surfaceVariant)
    } else {
        Modifier
            .clip(shape)
            .background(itemColor())
    }
    Box(
        modifier = modifier
            .then(background)
            .then(
                if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier
            )
    ) {
        content()
    }
}

@Composable
private fun PanelIconBox(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .size(36.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(LocalContentColor.current.copy(alpha = 0.1f)),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
private fun AccountCard(
    modifier: Modifier = Modifier,
    account: Account?,
    refreshKey: Any? = null,
    onClick: () -> Unit = {}
) {
    PanelInnerCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PanelIconBox {
                if (account != null) {
                    PlayerFace(
                        account = account,
                        avatarSize = 26.dp,
                        refreshKey = refreshKey
                    )
                } else {
                    Icon(
                        modifier = Modifier.size(24.dp),
                        painter = painterResource(R.drawable.ic_add),
                        contentDescription = null
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE),
                    text = account?.username ?: stringResource(R.string.account_add_new_account),
                    maxLines = 1,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (account != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(LocalContentColor.current.copy(alpha = 0.45f))
                        )
                        Text(
                            modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE),
                            text = getAccountTypeName(account),
                            maxLines = 1,
                            style = MaterialTheme.typography.labelMedium,
                            color = LocalContentColor.current.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            Icon(
                modifier = Modifier.size(20.dp),
                painter = painterResource(R.drawable.ic_keyboard_arrow_right),
                contentDescription = null,
                tint = LocalContentColor.current.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun InstanceCard(
    onLaunchGame: (Version?) -> Unit,
    toVersionManageScreen: () -> Unit,
    toVersionSettingsScreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val version by VersionsManager.currentVersion.collectAsStateWithLifecycle()
    val isRefreshing by VersionsManager.isRefreshing.collectAsStateWithLifecycle()

    var showList by remember { mutableStateOf(false) }
    var versionManagerRow by remember { mutableStateOf<LayoutCoordinates?>(null) }

    Box(modifier = modifier.fillMaxWidth()) {
        PanelInnerCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .onGloballyPositioned { coordinates ->
                            versionManagerRow = coordinates
                        }
                ) {
                    VersionManagerLayout(
                        isRefreshing = isRefreshing,
                        version = version,
                        modifier = Modifier.fillMaxWidth(),
                        swapToVersionManage = toVersionManageScreen,
                        openListMenu = { showList = true },
                    )
                }
                version?.takeIf { !isRefreshing && it.isValid() }?.let {
                    IconButton(
                        modifier = Modifier.size(36.dp),
                        onClick = toVersionSettingsScreen
                    ) {
                        Icon(
                            modifier = Modifier.size(22.dp),
                            painter = painterResource(R.drawable.ic_settings_filled),
                            contentDescription = stringResource(R.string.versions_manage_settings)
                        )
                    }
                }
            }
        }

        val menuAnchor = versionManagerRow
        val menuAnchorBounds = menuAnchor?.boundsInParent()
        val menuAnchorX = menuAnchorBounds?.left ?: 0f
        val menuAnchorHeight = menuAnchorBounds?.height ?: 0f

        DropdownMenu(
            expanded = showList && menuAnchor != null,
            onDismissRequest = { showList = false },
            modifier = Modifier.width(260.dp),
            offset = DpOffset(
                x = with(LocalDensity.current) { menuAnchorX.toDp() },
                y = with(LocalDensity.current) { (-menuAnchorHeight).toDp() } - 8.dp
            ),
            shape = MaterialTheme.shapes.extraLarge
        ) {
            val versions by VersionsManager.versions.collectAsStateWithLifecycle()
            versions.forEach { version0 ->
                DropdownMenuItem(
                    text = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CommonVersionInfoLayout(
                                modifier = Modifier.weight(1f),
                                version = version0,
                                iconSize = 28.dp
                            )
                            IconButton(
                                onClick = {
                                    onLaunchGame(version0)
                                    showList = false
                                }
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_play_arrow_filled),
                                    contentDescription = stringResource(R.string.main_launch_game),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    },
                    onClick = {
                        if (version == version0) return@DropdownMenuItem
                        VersionsManager.saveVersion(version0)
                        showList = false
                    }
                )
            }
        }
    }
}

@Composable
private fun LaunchButton(
    onLaunchGame: (Version?) -> Unit,
    modifier: Modifier = Modifier
) {
    val launchGlass = liquidGlassBackdrop() != null
    val textStyle = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
    ScalingActionButton(
        modifier = modifier.fillMaxWidth(),
        shape = PanelInnerShape,
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp, focusedElevation = 0.dp, hoveredElevation = 0.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        onClick = { onLaunchGame(null) },
        content = {
            if (launchGlass) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    MarqueeText(text = stringResource(R.string.main_launch_game), style = textStyle)
                }
            } else {
                MarqueeText(text = stringResource(R.string.main_launch_game), style = textStyle)
            }
        }
    )
}

/**
 * Compact launcher panel: account card, instance card and the launch button inside one rounded glass card.
 */
@Composable
private fun LauncherPanel(
    modifier: Modifier = Modifier,
    account: Account?,
    onLaunchGame: (Version?) -> Unit,
    toAccountManageScreen: () -> Unit,
    toVersionManageScreen: () -> Unit,
    toVersionSettingsScreen: () -> Unit,
) {
    BackgroundCard(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        liquidGlass = true
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PanelPadding),
            verticalArrangement = Arrangement.spacedBy(PanelSpacing)
        ) {
            AccountCard(
                account = account,
                onClick = toAccountManageScreen
            )
            InstanceCard(
                onLaunchGame = onLaunchGame,
                toVersionManageScreen = toVersionManageScreen,
                toVersionSettingsScreen = toVersionSettingsScreen
            )
            LaunchButton(
                onLaunchGame = onLaunchGame
            )
        }
    }
}

@Composable
private fun ActionMenuCardContent(
    modifier: Modifier = Modifier,
    account: Account?,
    onLaunchGame: (Version?) -> Unit,
    toAccountManageScreen: () -> Unit,
    toVersionManageScreen: () -> Unit,
    toVersionSettingsScreen: () -> Unit,
) {
    //The panel only takes the height of its content and sits at the bottom of the card box
    Box(
        modifier = modifier,
        contentAlignment = Alignment.BottomCenter
    ) {
        LauncherPanel(
            modifier = Modifier.fillMaxWidth(),
            account = account,
            onLaunchGame = onLaunchGame,
            toAccountManageScreen = toAccountManageScreen,
            toVersionManageScreen = toVersionManageScreen,
            toVersionSettingsScreen = toVersionSettingsScreen
        )
    }
}

@Composable
private fun ActionMenuTallerContent(
    modifier: Modifier = Modifier,
    account: Account?,
    dockedSide: ActionMenuSide,
    onLaunchGame: (Version?) -> Unit,
    toAccountManageScreen: () -> Unit,
    toVersionManageScreen: () -> Unit,
    toVersionSettingsScreen: () -> Unit,
) {
    val refreshWardrobe by AccountsManager.refreshWardrobe.collectAsStateWithLifecycle()
    val skinFile = remember(account, refreshWardrobe) {
        account?.getSkinFile()?.takeIf { it.exists() }
    }
    val capeFile = remember(account, refreshWardrobe) {
        account?.getCapeFile()?.takeIf { it.exists() }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
        val azimuth = (if (dockedSide == ActionMenuSide.START) -35 else 35) * (if (isRtl) -1 else 1)

        SkinPreview3D(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            skinFile = skinFile,
            capeFile = capeFile,
            modelType = account?.skinModelType,
            interactionEnabled = false,
            azimuth = azimuth,
            refreshKey = listOf(account?.uniqueUUID, refreshWardrobe, skinFile?.lastModified() ?: 0L, capeFile?.lastModified() ?: 0L)
        )

        LauncherPanel(
            modifier = Modifier.fillMaxWidth(),
            account = account,
            onLaunchGame = onLaunchGame,
            toAccountManageScreen = toAccountManageScreen,
            toVersionManageScreen = toVersionManageScreen,
            toVersionSettingsScreen = toVersionSettingsScreen
        )
    }
}

/** Compact "1.21.11 · Fabric 0.20.1" style summary; custom user summaries are shown untouched. */
private fun Version.panelSummary(): String {
    val summary = getVersionSummary()
    return if (isSummaryValid()) summary else summary.replace(", ", " · ").replace(" - ", " ")
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun VersionManagerLayout(
    isRefreshing: Boolean,
    version: Version?,
    swapToVersionManage: () -> Unit,
    openListMenu: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(shape = MaterialTheme.shapes.large)
            .combinedClickable(
                role = Role.Button,
                onClick = swapToVersionManage,
                onLongClick = {
                    if (version != null) openListMenu()
                }
            ).padding(PaddingValues(all = 4.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isRefreshing) {
            Box(modifier = Modifier.fillMaxWidth().height(36.dp)) {
                LoadingIndicator(
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.Center)
                )
            }
        } else {
            PanelIconBox {
                VersionIconImage(
                    version = version,
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(7.dp))
                )
            }
            Spacer(modifier = Modifier.width(10.dp))

            if (version == null) {
                Text(
                    modifier = Modifier
                        .weight(1f)
                        .basicMarquee(iterations = Int.MAX_VALUE),
                    text = stringResource(R.string.versions_manage_no_versions),
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1
                )
            } else {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE),
                        text = version.getVersionName(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    if (version.isValid()) {
                        Text(
                            modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE),
                            text = version.panelSummary(),
                            style = MaterialTheme.typography.labelMedium,
                            color = LocalContentColor.current.copy(alpha = 0.7f),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
