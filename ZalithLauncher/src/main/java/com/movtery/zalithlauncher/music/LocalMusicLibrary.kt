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

package com.movtery.zalithlauncher.music

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.movtery.zalithlauncher.path.PathManager
import com.movtery.zalithlauncher.utils.logging.Logger
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import org.apache.commons.io.FileUtils
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

private const val TAG = "LocalMusicLibrary"

/**
 * One imported audio file.
 * @param uri Storage Access Framework uri, read permission is persisted at import time
 * @param artFile file name of the extracted album artwork inside [LocalMusicLibrary.artDir], empty when none
 */
data class LocalTrack(
    @SerializedName("id")
    val id: String = "",
    @SerializedName("uri")
    val uri: String = "",
    @SerializedName("title")
    val title: String = "",
    @SerializedName("artist")
    val artist: String = "",
    @SerializedName("album")
    val album: String = "",
    @SerializedName("durationMs")
    val durationMs: Long = 0L,
    @SerializedName("artFile")
    val artFile: String = ""
)

/**
 * The music files imported for the local source of the Music Card.
 * Files are never copied: only the Storage Access Framework uri and the metadata are kept.
 */
object LocalMusicLibrary {
    private const val KEY_TRACKS = "tracks"
    private const val ART_MAX_SIZE = 512

    private val mmkv: MMKV by lazy { MMKV.mmkvWithID("music_library") }
    private val gson = Gson()

    private val _tracks = MutableStateFlow(loadTracks())

    /** Every imported track, in import order */
    val tracks: StateFlow<List<LocalTrack>> = _tracks.asStateFlow()

    /** Folder of the extracted album artwork */
    val artDir: File get() = File(PathManager.DIR_BACKGROUND, "music_art")

    fun artFileOf(track: LocalTrack): File? =
        track.artFile.takeIf { it.isNotBlank() }?.let { File(artDir, it) }?.takeIf { it.exists() }

    /**
     * Reads the metadata of [uris] and adds them to the library. Files already imported are skipped,
     * files that cannot be read as audio are skipped too.
     * @return how many tracks were added
     */
    suspend fun importUris(context: Context, uris: List<Uri>): Int = withContext(Dispatchers.IO) {
        val known = _tracks.value.mapTo(HashSet()) { it.uri }
        val added = mutableListOf<LocalTrack>()
        for (uri in uris) {
            if (uri.toString() in known) continue
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val track = runCatching { readTrack(context, uri) }
                .onFailure { Logger.warning(TAG, "Failed to read audio metadata: $uri", it) }
                .getOrNull() ?: continue
            known.add(track.uri)
            added.add(track)
        }
        if (added.isNotEmpty()) {
            synchronized(this@LocalMusicLibrary) {
                _tracks.update { it + added }
                saveTracks()
            }
        }
        added.size
    }

    /** Removes every imported track, its artwork and the persisted read permissions */
    fun clear(context: Context) {
        val old = synchronized(this) {
            val tracks = _tracks.value
            _tracks.value = emptyList()
            saveTracks()
            tracks
        }
        old.forEach { track ->
            runCatching {
                context.contentResolver.releasePersistableUriPermission(
                    Uri.parse(track.uri),
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
        }
        FileUtils.deleteQuietly(artDir)
    }

    private fun readTrack(context: Context, uri: Uri): LocalTrack {
        val id = UUID.randomUUID().toString()
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            //A file without audio is not music
            if (retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO) == "no") {
                error("No audio track: $uri")
            }
            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                ?.trim()?.takeIf { it.isNotEmpty() }
                ?: displayName(context, uri)
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                ?: retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST)
            val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
            val artFile = retriever.embeddedPicture?.let { saveArtwork(id, it) } ?: ""
            return LocalTrack(
                id = id,
                uri = uri.toString(),
                title = title,
                artist = artist?.trim().orEmpty(),
                album = album?.trim().orEmpty(),
                durationMs = duration,
                artFile = artFile
            )
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun displayName(context: Context, uri: Uri): String {
        val name = runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        }.getOrNull() ?: uri.lastPathSegment ?: "Unknown"
        return name.substringBeforeLast('.', name)
    }

    /** Decodes the embedded picture down to [ART_MAX_SIZE] and stores it as JPEG, returns the file name */
    private fun saveArtwork(id: String, data: ByteArray): String? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(data, 0, data.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= ART_MAX_SIZE && bounds.outHeight / (sample * 2) >= ART_MAX_SIZE) {
            sample *= 2
        }
        val bitmap = BitmapFactory.decodeByteArray(
            data, 0, data.size,
            BitmapFactory.Options().apply { inSampleSize = sample }
        ) ?: return null
        val fileName = "$id.jpg"
        return try {
            artDir.mkdirs()
            FileOutputStream(File(artDir, fileName)).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            fileName
        } catch (e: Throwable) {
            Logger.warning(TAG, "Failed to save album artwork.", e)
            null
        } finally {
            bitmap.recycle()
        }
    }

    private fun loadTracks(): List<LocalTrack> {
        val json = mmkv.decodeString(KEY_TRACKS, "").orEmpty()
        if (json.isBlank()) return emptyList()
        return runCatching {
            gson.fromJson(json, Array<LocalTrack>::class.java)
                .orEmpty()
                .filter { it.id.isNotBlank() && it.uri.isNotBlank() }
        }.onFailure { e ->
            Logger.error(TAG, "Failed to parse the local music library.", e)
        }.getOrDefault(emptyList())
    }

    private fun saveTracks() {
        mmkv.encode(KEY_TRACKS, gson.toJson(_tracks.value))
    }
}
