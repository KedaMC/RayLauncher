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

package com.movtery.zalithlauncher.ui.screens.content.home

import com.movtery.zalithlauncher.ui.components.effectiveBackgroundBlur
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalDensity
import com.movtery.zalithlauncher.setting.enums.ActionMenuSide
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.delay
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import kotlinx.coroutines.launch
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import com.movtery.zalithlauncher.ui.components.IconButton
import androidx.compose.material3.MaterialTheme
import com.movtery.zalithlauncher.ui.components.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.movtery.cardgrid.model.CardRect
import com.movtery.cardgrid.state.CardGridState
import com.movtery.cardgrid.state.CardSeed
import com.movtery.cardgrid.ui.CardGrid
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.ui.screens.content.elements.backgroundGlass
import com.movtery.zalithlauncher.ui.screens.content.home.music.MusicCardManager
import com.movtery.zalithlauncher.ui.screens.content.home.overlay.OverlayCardManager
import com.movtery.zalithlauncher.ui.screens.content.home.server.ServerCardManager
import com.movtery.zalithlauncher.ui.screens.content.home.server.ServerCardSettingsHost
import com.movtery.zalithlauncher.ui.screens.content.home.version.VersionCardManager
import com.movtery.zalithlauncher.ui.screens.content.home.visualizer.VisualizerCardManager
import com.movtery.zalithlauncher.ui.screens.content.home.workspace.HomeWorkspace
import com.movtery.zalithlauncher.ui.screens.content.home.workspace.LocalWorkspaceCamera
import com.movtery.zalithlauncher.ui.screens.content.home.workspace.MAX_WORKSPACE_ZOOM
import com.movtery.zalithlauncher.ui.screens.content.home.workspace.WorkspaceCamera
import com.movtery.zalithlauncher.ui.screens.content.home.workspace.WorkspaceCanvas
import com.movtery.zalithlauncher.ui.theme.cardColor
import com.movtery.zalithlauncher.ui.theme.onCardColor
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull

/** The home grid state, for cards that need to know where they sit on the grid */
val LocalHomeGridState = staticCompositionLocalOf<CardGridState?> { null }

/** 主页网格的卡片操作 */
private sealed interface HomeCardOperation {
    data object None : HomeCardOperation
    /** 打开服务器卡片的设置对话框 */
    data class ServerCardSettings(val cardId: String) : HomeCardOperation
}

/** How long the viewport must stay unchanged before a missing launch card is placed */
private const val LAUNCH_CARD_PLACE_DELAY_MS = 500L

/**
 * 主页网格：自由画布上的卡片网格，
 * 负责布局的播种、持久化、卡片记录同步与服务器卡片的可见性上报。
 */
@Composable
fun HomeGrid(
    state: CardGridState,
    isVisible: Boolean,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val camera = remember(density.density) { WorkspaceCamera(density.density) }
    val scope = rememberCoroutineScope()
    //长按调整工具栏触发的卡片操作
    var operation by remember { mutableStateOf<HomeCardOperation>(HomeCardOperation.None) }

    // 网格状态跨导航存活：离开主页时退出调整态，避免回来时仍停留在调整态
    DisposableEffect(state) {
        onDispose { state.exitAdjusting() }
    }

    // 播种持久化的用户卡片布局（状态存活期间只播种一次，回到主页不重放）
    LaunchedEffect(Unit) {
        if (state.isSeeded) return@LaunchedEffect
        val snapshot = HomeGridStore.load()
        state.seed(
            types = HomeCards.userCardTypes,
            seeds = snapshot?.cards?.map { entry ->
                CardSeed(
                    id = entry.id,
                    typeId = entry.type,
                    layout = CardRect(
                        id = entry.id,
                        x = entry.x,
                        y = entry.y,
                        width = entry.width,
                        height = entry.height
                    )
                )
            } ?: emptyList(),
            storedColumns = snapshot?.columns ?: 0
        )
    }

    // 布局结算后持久化
    LaunchedEffect(Unit) {
        state.onLayoutCommitted = {
            HomeGridStore.save(
                cards = state.cards,
                columns = state.geometry.columns
            )
        }
    }

    // 卡片移除回调：同步各卡片管理器的记录
    LaunchedEffect(Unit) {
        state.onCardRemoved = { cardId ->
            VersionCardManager.removeCard(cardId)
            ServerCardManager.removeCard(cardId)
            OverlayCardManager.removeCard(cardId)
            MusicCardManager.removeCard(cardId)
            VisualizerCardManager.removeCard(cardId)
        }
    }

    // 与版本卡片记录保持同步：记录存在而网格缺卡时补齐
    LaunchedEffect(Unit) {
        VersionCardManager.cards.collect { states ->
            states.forEach { cardState ->
                if (state.cards.none { it.id == cardState.record.cardId }) {
                    state.addCard(HomeCards.versionCardType(), cardState.record.cardId)
                }
            }
        }
    }

    // 装饰叠加卡片：记录存在而网格缺卡时补齐，网格中的叠加卡片没有记录时移除。
    // 网格卡片数量变化（如播种完成）时也重新对齐一次
    LaunchedEffect(Unit) {
        snapshotFlow { state.cards.size }
            .combine(OverlayCardManager.cards) { _, records -> records }
            .collect { records ->
                records.forEach { record ->
                    if (state.cards.none { it.id == record.cardId }) {
                        state.addCard(HomeCards.overlayCardType(), record.cardId)
                    }
                }
                state.cards
                    .filter { it.type.typeId == HomeCards.OVERLAY_CARD_TYPE_ID }
                    .filter { card -> records.none { it.cardId == card.id } }
                    .forEach { state.removeCard(it.id) }
            }
    }

    // Music card: add it to the grid while its record exists, drop it from the grid when the record is gone
    LaunchedEffect(Unit) {
        snapshotFlow { state.cards.size }
            .combine(MusicCardManager.cardId) { _, id -> id }
            .collect { id ->
                if (id != null && state.cards.none { it.id == id }) {
                    state.addCard(HomeCards.musicCardType(), id)
                }
                state.cards
                    .filter { it.type.typeId == HomeCards.MUSIC_CARD_TYPE_ID && it.id != id }
                    .forEach { state.removeCard(it.id) }
            }
    }

    // Launch card: always on the grid. A missing one (first run) is placed at the bottom of the starting
    // view, on the side the launch panel was docked to (right by default). The window can still be resizing
    // right after the first start, so the card is only placed once a landscape viewport has settled
    LaunchedEffect(state, camera) {
        snapshotFlow { camera.viewport.takeIf { state.isLayoutReady && it.width > it.height } }
            .filterNotNull()
            .collectLatest {
                if (state.cards.any { it.type.typeId == HomeCards.LAUNCH_CARD_TYPE_ID }) return@collectLatest
                delay(LAUNCH_CARD_PLACE_DELAY_MS)
                val type = HomeCards.launchCardType()
                val (visibleColumns, visibleRows) = state.visibleCells.let { it.x to it.y }
                val width = type.defaultSpan.x
                val height = type.limits.minHeight
                val y = (visibleRows - height).coerceAtLeast(0)
                val x = if (AllSettings.launcherActionMenuSide.getValue() == ActionMenuSide.START) 0
                else (visibleColumns - width).coerceAtLeast(0)
                state.addCard(
                    type = type,
                    id = HomeCards.LAUNCH_CARD_ID,
                    preferred = CardRect(id = HomeCards.LAUNCH_CARD_ID, x = x, y = y, width = width, height = height)
                )
            }
    }

    // Spectrum visualizer cards follow their records, in both directions
    LaunchedEffect(Unit) {
        snapshotFlow { state.isLayoutReady to state.cards.size }
            .combine(VisualizerCardManager.cardIds) { (ready, _), ids -> ids.takeIf { ready } }
            .filterNotNull()
            .collect { ids ->
                ids.forEach { id ->
                    if (state.cards.none { it.id == id }) {
                        state.addCard(HomeCards.visualizerCardType(), id)
                    }
                }
                state.cards
                    .filter { it.type.typeId == HomeCards.VISUALIZER_CARD_TYPE_ID && it.id !in ids }
                    .forEach { state.removeCard(it.id) }
            }
    }

    //Home must never open onto an empty part of the canvas: when the screen is entered or resized and no
    //card is in view, the camera returns to the starting view
    LaunchedEffect(state, camera, isVisible) {
        if (!isVisible) return@LaunchedEffect
        snapshotFlow { camera.viewport.takeIf { it != Size.Zero && state.isLayoutReady && state.cards.isNotEmpty() } }
            .filterNotNull()
            .distinctUntilChanged()
            .collect { viewport ->
                val view = Rect(Offset.Zero, viewport)
                val anyInView = Snapshot.withoutReadObservation {
                    state.cards.any { camera.toViewport(state.rectFor(it.layout)).overlaps(view) }
                }
                if (!anyInView) camera.set(MAX_WORKSPACE_ZOOM, Offset.Zero)
            }
    }

    // 与服务器卡片记录保持同步：记录存在而网格缺卡时补齐
    LaunchedEffect(Unit) {
        ServerCardManager.cards.collect { states ->
            states.forEach { cardState ->
                if (state.cards.none { it.id == cardState.record.cardId }) {
                    state.addCard(HomeCards.serverCardType(), cardState.record.cardId)
                }
            }
        }
    }

    // 服务器卡片可见性：可见 = 屏幕可见 ∧ App 前台 ∧ 卡片矩形与滚动视口相交，
    // 新进入可见集合的卡片会触发一次静默刷新（15s 冷却由管理器约束）
    val isVisibleState = rememberUpdatedState(isVisible)
    val lifecycleOwner = LocalLifecycleOwner.current
    var resumed by remember { mutableStateOf(false) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            resumed = event == Lifecycle.Event.ON_RESUME
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(state) {
        snapshotFlow {
            val view = Rect(Offset.Zero, camera.viewport)
            if (!isVisibleState.value || !resumed || view.isEmpty) {
                emptySet()
            } else {
                state.cards.mapNotNullTo(mutableSetOf()) { card ->
                    if (card.type.typeId != HomeCards.SERVER_CARD_TYPE_ID) return@mapNotNullTo null
                    val rect = camera.toViewport(state.rectFor(card.layout))
                    if (rect.overlaps(view)) card.id else null
                }
            }
        }.distinctUntilChanged().collect { ids ->
            ServerCardManager.onVisibleCardsChanged(ids)
        }
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        CompositionLocalProvider(
            LocalHomeGridState provides state,
            LocalWorkspaceCamera provides camera
        ) {
            WorkspaceCanvas(
                state = state,
                camera = camera,
                //Edge to edge on all four sides: the system bars are hidden, but the immersive window can
                //still report their insets, which would keep the cards away from the screen edges
                modifier = Modifier.fillMaxSize()
            ) {
                CardGrid(
                    state = state,
                    containerColor = cardColor(),
                    contentColor = onCardColor(),
                    cardBackground = { base ->
                        base.backgroundGlass(
                            blur = effectiveBackgroundBlur(),
                            color = cardColor(),
                            enabled = true
                        )
                    },
                )
            }
        }

        val systemCards = HomeCards.systemCards()
        if (!systemCards.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(all = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                systemCards.forEach { systemCard ->
                    key(systemCard.id) { systemCard.content() }
                }
            }
        }

        //Back to the starting view
        AnimatedVisibility(
            visible = !camera.isHome && !state.isAdjusting,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .navigationBarsPadding()
                .padding(all = 9.dp)
        ) {
            ResetViewButton(
                onClick = {
                    val fromScale = camera.scale
                    val fromOffset = camera.offset
                    scope.launch {
                        animate(0f, 1f) { progress, _ ->
                            camera.set(
                                scale = fromScale + (MAX_WORKSPACE_ZOOM - fromScale) * progress,
                                offset = fromOffset * (1f - progress)
                            )
                        }
                    }
                }
            )
        }

        //调整态底部操作栏
        AnimatedVisibility(
            visible = state.isAdjusting,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 9.dp)
        ) {
            AdjustingBar(
                state = state,
                onShowSettings = {
                    operation = HomeCardOperation.ServerCardSettings(it)
                }
            )
        }
    }

    HomeCardOperation(
        operation = operation,
        onChange = { operation = it }
    )
}

@Composable
private fun HomeCardOperation(
    operation: HomeCardOperation,
    onChange: (HomeCardOperation) -> Unit
) {
    when (operation) {
        is HomeCardOperation.None -> {}
        is HomeCardOperation.ServerCardSettings -> {
            ServerCardSettingsHost(
                cardId = operation.cardId,
                onDismiss = { onChange(HomeCardOperation.None) }
            )
        }
    }
}

@Composable
private fun ResetViewButton(onClick: () -> Unit) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shadowElevation = 3.dp
    ) {
        IconButton(
            modifier = Modifier.size(40.dp),
            onClick = onClick
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_center_focus),
                contentDescription = stringResource(R.string.home_workspace_reset_view)
            )
        }
    }
}

/**
 * 调整态底部操作栏
 */
@Composable
private fun AdjustingBar(
    state: CardGridState,
    onShowSettings: (String) -> Unit
) {
    val adjustingId = state.adjustingCardId
    val isServerCard = remember(state.cards, adjustingId) {
        state.cards.firstOrNull { it.id == adjustingId }?.type?.typeId == HomeCards.SERVER_CARD_TYPE_ID
    }
    val isRemovable = remember(state.cards, adjustingId) {
        state.cards.firstOrNull { it.id == adjustingId }?.type?.removable != false
    }

    Surface(
        modifier = Modifier
            .padding(all = 3.dp)
            .barGuard(),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shadowElevation = 3.dp
    ) {
        Row(
            modifier = Modifier.padding(all = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnimatedVisibility(visible = isServerCard && adjustingId != null) {
                IconButton(
                    modifier = Modifier.size(34.dp),
                    onClick = { adjustingId?.let(onShowSettings) }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_settings_filled),
                        contentDescription = stringResource(R.string.generic_setting)
                    )
                }
                Spacer(Modifier.width(2.dp))
            }

            //Cards that can not be removed (the launch card) get no delete button at all
            if (isRemovable) {
                IconButton(
                    modifier = Modifier.size(34.dp),
                    enabled = adjustingId != null,
                    onClick = { adjustingId?.let(state::removeCard) }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_delete_filled),
                        tint = MaterialTheme.colorScheme.error,
                        contentDescription = stringResource(R.string.generic_delete)
                    )
                }

                Spacer(Modifier.width(2.dp))
            }

            IconButton(
                modifier = Modifier.size(34.dp),
                onClick = { state.exitAdjusting() }
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_check),
                    contentDescription = stringResource(R.string.generic_done)
                )
            }
        }
    }
}

/**
 * 消费落在自身范围内的全部指针事件
 */
private fun Modifier.barGuard(): Modifier = pointerInput(Unit) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false).consume()
        while (true) {
            val event = awaitPointerEvent()
            event.changes.forEach { it.consume() }
            if (event.changes.none { it.pressed }) return@awaitEachGesture
        }
    }
}