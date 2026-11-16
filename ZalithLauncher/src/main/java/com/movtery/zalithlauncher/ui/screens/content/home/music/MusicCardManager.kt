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

package com.movtery.zalithlauncher.ui.screens.content.home.music

import com.tencent.mmkv.MMKV
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * Keeps the Music Card of the home grid. There is at most one: it is added from
 * Settings -> Launcher and then lives on the grid like every other home card.
 */
object MusicCardManager {
    private const val KEY_CARD_ID = "musicCardId"

    private val mmkv: MMKV by lazy { MMKV.mmkvWithID("home_music_card") }

    private val _cardId = MutableStateFlow(mmkv.decodeString(KEY_CARD_ID, "")?.takeIf { it.isNotBlank() })

    /** Id of the Music Card on the grid, null when the user has not added one */
    val cardId: StateFlow<String?> = _cardId.asStateFlow()

    /** Adds the Music Card, does nothing when it already exists */
    fun addCard() {
        synchronized(this) {
            if (_cardId.value != null) return
            val id = UUID.randomUUID().toString()
            mmkv.encode(KEY_CARD_ID, id)
            _cardId.value = id
        }
    }

    /** Removes the record of [cardId] (the grid already dropped the card itself) */
    fun removeCard(cardId: String) {
        synchronized(this) {
            if (_cardId.value != cardId) return
            removeCard()
        }
    }

    /** Removes the Music Card */
    fun removeCard() {
        synchronized(this) {
            mmkv.removeValueForKey(KEY_CARD_ID)
            _cardId.value = null
        }
    }
}
