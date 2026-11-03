/*
 * Ray Launcher
 * Copyright (C) 2025-2026 contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.movtery.zalithlauncher.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Lazy column for the glass settings pages. Every card is its own lazy item so only the cards near
 * the viewport are composed (and blurred). It keeps plain `item`/`items` available next to the
 * entrance-animated [GlassLazyScope.animatedItem], which the upstream animated lazy column lacks.
 *
 * @param baseDelay entrance delay of the first item (ms)
 * @param delayIncrement extra entrance delay per following item that has no explicit delay (ms)
 */
@Composable
fun GlassLazyColumn(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    isVisible: Boolean,
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(12.dp),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    baseDelay: Int = 0,
    delayIncrement: Int = 50,
    content: GlassLazyScope.() -> Unit
) {
    LazyColumn(
        modifier = modifier,
        state = state,
        verticalArrangement = verticalArrangement,
        contentPadding = contentPadding
    ) {
        GlassLazyScope(this, isVisible, baseDelay, delayIncrement).content()
    }
}

class GlassLazyScope internal constructor(
    private val lazy: LazyListScope,
    private val isVisible: Boolean,
    private val baseDelay: Int,
    private val delayIncrement: Int
) : LazyListScope by lazy {
    private var itemIndex = 0

    /**
     * Adds one lazy item whose content receives the entrance [AnimatedContext]; apply it with
     * `Modifier.animated(ctx)`, which only reads the animation in the graphics layer.
     * @param delay explicit entrance delay (ms), replaces the index based one
     */
    fun animatedItem(
        key: Any? = null,
        delay: Int? = null,
        content: @Composable LazyItemScope.(ctx: AnimatedContext) -> Unit
    ) {
        val currentIndex = itemIndex++
        val actualDelay = delay ?: (baseDelay + currentIndex * delayIncrement)
        lazy.item(key = key) {
            val ctx = rememberAnimatedContext(isVisible = this@GlassLazyScope.isVisible, delayMillis = actualDelay)
            content(ctx)
        }
    }
}
