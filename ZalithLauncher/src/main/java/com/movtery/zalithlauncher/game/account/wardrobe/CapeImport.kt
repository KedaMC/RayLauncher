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

package com.movtery.zalithlauncher.game.account.wardrobe

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream

/** Largest accepted cape texture width, the height is half of it */
private const val MAX_CAPE_WIDTH = 2048

/** Largest accepted source file size */
const val MAX_CAPE_FILE_BYTES = 8 * 1024 * 1024

/**
 * Check that the image is a valid cape texture and convert it to PNG.
 *
 * A valid cape texture is 64x32 or a multiple of it (2:1 ratio, width up to [MAX_CAPE_WIDTH]).
 * Other formats such as JPEG or WebP are accepted as input, but always re-encoded to PNG,
 * because the game can only decode PNG textures.
 *
 * @return PNG bytes, or null if the data is not a valid cape texture
 */
fun convertToCapePng(data: ByteArray): ByteArray? {
    if (data.isEmpty() || data.size > MAX_CAPE_FILE_BYTES) return null

    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(data, 0, data.size, bounds)
    val w = bounds.outWidth
    val h = bounds.outHeight
    if (w <= 0 || h <= 0) return null
    if (w % 64 != 0 || h * 2 != w || w > MAX_CAPE_WIDTH) return null

    val options = BitmapFactory.Options().apply { inScaled = false }
    val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size, options) ?: return null
    return try {
        ByteArrayOutputStream().use { out ->
            if (bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) out.toByteArray() else null
        }
    } finally {
        bitmap.recycle()
    }
}
