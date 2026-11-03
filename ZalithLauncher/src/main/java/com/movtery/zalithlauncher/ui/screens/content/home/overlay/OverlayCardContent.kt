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

package com.movtery.zalithlauncher.ui.screens.content.home.overlay

import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.crossfade
import com.movtery.cardgrid.model.CardState
import com.movtery.zalithlauncher.ui.components.VideoPlayer

/**
 * Content of a decoration overlay card: the imported image, or the imported video played muted in
 * a loop. The media fills the card (cropped), so resizing the card resizes the decoration.
 */
@OptIn(UnstableApi::class)
@Composable
fun CardState.OverlayCardContent(cardId: String) {
    val records by OverlayCardManager.cards.collectAsStateWithLifecycle()
    val record = remember(records, cardId) {
        records.firstOrNull { it.cardId == cardId }
    } ?: return
    val file = remember(record) { OverlayCardManager.fileOf(record) }
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize()) {
        if (record.video) {
            VideoPlayer(
                videoUri = Uri.fromFile(file),
                modifier = Modifier.fillMaxSize(),
                muted = true,
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            )
        } else {
            val request = remember(file) {
                ImageRequest.Builder(context)
                    .data(file)
                    .allowHardware(false)
                    .crossfade(false)
                    .build()
            }
            AsyncImage(
                modifier = Modifier.fillMaxSize(),
                model = request,
                contentDescription = null,
                contentScale = ContentScale.Crop
            )
        }
    }
}
