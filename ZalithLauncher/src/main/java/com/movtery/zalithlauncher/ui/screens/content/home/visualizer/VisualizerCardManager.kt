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

package com.movtery.zalithlauncher.ui.screens.content.home.visualizer

import com.tencent.mmkv.MMKV
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * Keeps the spectrum visualizer cards of the home workspace. Any number of them can be added.
 */
object VisualizerCardManager {
    private const val KEY_CARD_IDS = "visualizerCardIds"
    private const val SEPARATOR = ","

    private val mmkv: MMKV by lazy { MMKV.mmkvWithID("home_visualizer_cards") }

    private val _cardIds = MutableStateFlow(
        mmkv.decodeString(KEY_CARD_IDS, "").orEmpty().split(SEPARATOR).filter { it.isNotBlank() }
    )

    val cardIds: StateFlow<List<String>> = _cardIds.asStateFlow()

    fun addCard() = update { it + UUID.randomUUID().toString() }

    fun removeCard(cardId: String) = update { it - cardId }

    fun removeAll() = update { emptyList() }

    private fun update(change: (List<String>) -> List<String>) {
        synchronized(this) {
            val ids = change(_cardIds.value)
            mmkv.encode(KEY_CARD_IDS, ids.joinToString(SEPARATOR))
            _cardIds.value = ids
        }
    }
}
