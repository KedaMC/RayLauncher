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

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.movtery.zalithlauncher.context.copyLocalFile
import com.movtery.zalithlauncher.path.PathManager
import com.movtery.zalithlauncher.utils.image.isGifFile
import com.movtery.zalithlauncher.utils.image.isImageFile
import com.movtery.zalithlauncher.utils.logging.Logger
import com.movtery.zalithlauncher.utils.video.isVideoFile
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import org.apache.commons.io.FileUtils
import java.io.File
import java.util.UUID

private const val TAG = "OverlayCardManager"

/**
 * Record of one decoration overlay card on the home grid.
 * @param fileName media file inside [OverlayCardManager.dir]
 * @param video true when the media is a video, false for an image
 */
data class OverlayCardRecord(
    @SerializedName("cardId")
    val cardId: String = "",
    @SerializedName("fileName")
    val fileName: String = "",
    @SerializedName("video")
    val video: Boolean = false
)

/**
 * Keeps the decoration overlay cards: an image or a video imported from the gallery that lives on the
 * home grid as a transparent card, so it can be moved and resized like the other home cards.
 */
object OverlayCardManager {
    private const val KEY_RECORDS = "overlayCards"

    private val mmkv: MMKV by lazy { MMKV.mmkvWithID("home_overlay_cards") }
    private val gson = Gson()

    private val _cards = MutableStateFlow(loadRecords())

    /** All overlay card records */
    val cards: StateFlow<List<OverlayCardRecord>> = _cards.asStateFlow()

    /** Folder of the imported overlay media files */
    val dir: File get() = File(PathManager.DIR_BACKGROUND, "overlay_cards")

    fun fileOf(record: OverlayCardRecord): File = File(dir, record.fileName)

    /**
     * Copies [uri] into the overlay folder and creates a card record for it. GIF is refused and a
     * file that is neither an image nor a video is removed again. Throws with a readable message.
     */
    suspend fun addFromUri(context: Context, uri: Uri, gifUnsupportedMessage: String): OverlayCardRecord =
        withContext(Dispatchers.IO) {
            val cardId = UUID.randomUUID().toString()
            val file = File(dir, "$cardId.file")
            try {
                context.copyLocalFile(uri, file)
                if (file.isGifFile()) error(gifUnsupportedMessage)
                val video = when {
                    file.isImageFile() -> false
                    file.isVideoFile() -> true
                    else -> error("The selected file is not an image or a video!")
                }
                val record = OverlayCardRecord(cardId = cardId, fileName = file.name, video = video)
                synchronized(this@OverlayCardManager) {
                    _cards.update { it + record }
                    saveRecords()
                }
                record
            } catch (e: Throwable) {
                FileUtils.deleteQuietly(file)
                throw e
            }
        }

    /** Removes the record of [cardId] and deletes its media file */
    fun removeCard(cardId: String) {
        synchronized(this) {
            val record = _cards.value.firstOrNull { it.cardId == cardId } ?: return
            _cards.update { list -> list.filterNot { it.cardId == cardId } }
            saveRecords()
            FileUtils.deleteQuietly(fileOf(record))
        }
    }

    /** Removes every overlay card record and media file */
    fun removeAll() {
        synchronized(this) {
            val records = _cards.value
            _cards.value = emptyList()
            saveRecords()
            records.forEach { FileUtils.deleteQuietly(fileOf(it)) }
        }
    }

    private fun loadRecords(): List<OverlayCardRecord> {
        val json = mmkv.decodeString(KEY_RECORDS, "").orEmpty()
        if (json.isBlank()) return emptyList()
        return runCatching {
            gson.fromJson(json, Array<OverlayCardRecord>::class.java)
                .orEmpty()
                .filter { it.cardId.isNotBlank() && it.fileName.isNotBlank() }
        }.onFailure { e ->
            Logger.error(TAG, "Failed to parse overlay card records.", e)
        }.getOrDefault(emptyList())
    }

    private fun saveRecords() {
        mmkv.encode(KEY_RECORDS, gson.toJson(_cards.value))
    }
}
