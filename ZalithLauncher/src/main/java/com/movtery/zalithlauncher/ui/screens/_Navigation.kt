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

package com.movtery.zalithlauncher.ui.screens

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.IntOffset
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.scene.Scene
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.utils.animation.SwapClock
import com.movtery.zalithlauncher.utils.animation.SwapTimeline
import kotlin.reflect.KClass

/**
 * 兼容嵌套NavDisplay的返回事件处理
 */
fun <E: TitledNavKey> onBack(currentBackStack: NavBackStack<E>) {
    when (val key = currentBackStack.lastOrNull()) {
        //普通的屏幕，直接退出当前堆栈的上层
        is NormalNavKey -> currentBackStack.removeLastOrNull()
        is BackStackNavKey<*> -> {
            if (key.backStack.size <= 1) {
                //嵌套屏幕的堆栈处于最后一个屏幕的状态
                //可以退出当前堆栈的上层了
                currentBackStack.removeLastOrNull()
            } else {
                //退出子堆栈的上层屏幕
                key.backStack.removeLastOrNull()
            }
        }
    }
}

fun <E: TitledNavKey> NavBackStack<E>.navigateOnce(key: E) {
    if (key == lastOrNull()) return //防止反复加载
    clearWith(key)
}

fun <E: TitledNavKey> NavBackStack<E>.navigateTo(screenKey: E, useClassEquality: Boolean = false) {
    val current = lastOrNull()
    if (useClassEquality) {
        if (current != null && screenKey::class == current::class) return //防止反复加载
    } else {
        if (screenKey == current) return //防止反复加载
    }
    add(screenKey)
}

fun <E: TitledNavKey> NavBackStack<E>.removeAndNavigateTo(remove: KClass<*>, screenKey: E, useClassEquality: Boolean = false) {
    removeIf { key ->
        key::class == remove
    }
    navigateTo(screenKey, useClassEquality)
}

fun <E: TitledNavKey> NavBackStack<E>.removeAndNavigateTo(removes: List<KClass<*>>, screenKey: E, useClassEquality: Boolean = false) {
    removeIf { key ->
        key::class in removes
    }
    navigateTo(screenKey, useClassEquality)
}

/**
 * 清除所有栈，并加入指定的key
 */
fun <E: TitledNavKey> NavBackStack<E>.clearWith(navKey: E) {
    val targetClass = navKey::class.java
    if (none { it::class.java == targetClass }) {
        //提前加入，避免让 Nav3 看到空帧
        add(navKey)
    }
    removeIf { it::class.java != targetClass }
}

/**
 * 清除指定的key
 */
fun <E: TitledNavKey> NavBackStack<E>.clearKeys(vararg navKeys: E) {
    val classes = navKeys.map { it::class.java }
    removeIf { it::class.java in classes }
}

fun <E: TitledNavKey> NavBackStack<E>.addIfEmpty(navKey: E) {
    if (isEmpty()) {
        add(navKey)
    }
}

@Composable
fun rememberSwapTween(): FiniteAnimationSpec<Float> {
    val level = AllSettings.launcherAnimateSpeedLevel.state
    return remember(level) {
        tween(durationMillis = SwapTimeline.time(SwapTimeline.CONTENT_BASE))
    }
}

/**
 * 串行页面切换编排
 */
@Composable
fun <T : Any> rememberTransitionSpec(): AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform {
    val level = AllSettings.launcherAnimateSpeedLevel.state
    return remember(level) {
        val transform: AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform = {
            val window = SwapClock.record(initialState, targetState)
            ContentTransform(
                fadeIn(animationSpec = snap(delayMillis = window.enterDelayMillis)),
                fadeOut(animationSpec = snap(delayMillis = window.exitDelayMillis)),
            )
        }
        transform
    }
}

/** Duration of the full-page horizontal slide between main pages (ms) */
const val PAGE_SLIDE_MILLIS = 300

/** Index of a main page in the top navigation (Home, Download, Settings), or -1 for any other screen */
fun pageIndexOf(key: TitledNavKey?): Int = when (key) {
    is NormalNavKey.LauncherMain -> 0
    is NestedNavKey.Download -> 1
    is NestedNavKey.Settings -> 2
    else -> -1
}

/**
 * Tracks which main page was left and which was entered, to pick the slide direction.
 * Direction comes from page order, so navigating back plays the reverse slide.
 */
class PageSlideTracker {
    private var current = -2
    private var previous = -2

    /** Call during composition with the index of the top screen */
    fun update(index: Int) {
        if (index == current) return
        previous = current
        current = index
        SwapTimeline.pageSlideActive = direction() != 0
    }

    /** 1: slide forward (content moves left), -1: backward, 0: not a main page switch */
    fun direction(): Int =
        if (previous >= 0 && current >= 0 && previous != current) (if (current > previous) 1 else -1) else 0
}

/**
 * Full-page horizontal slide between main pages, falling back to the default swap
 * transition for every other navigation.
 */
@Composable
fun <T : Any> rememberPageSlideSpec(tracker: PageSlideTracker): AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform {
    val fallback = rememberTransitionSpec<T>()
    return remember(fallback, tracker) {
        val spec: AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform = {
            val dir = tracker.direction()
            if (dir == 0 || !AllSettings.launcherSwapAnimEnabled.state) {
                fallback(this)
            } else {
                val slide = tween<IntOffset>(durationMillis = PAGE_SLIDE_MILLIS, easing = EaseInOut)
                ContentTransform(
                    slideInHorizontally(animationSpec = slide) { full -> dir * full },
                    slideOutHorizontally(animationSpec = slide) { full -> -dir * full }
                )
            }
        }
        spec
    }
}
