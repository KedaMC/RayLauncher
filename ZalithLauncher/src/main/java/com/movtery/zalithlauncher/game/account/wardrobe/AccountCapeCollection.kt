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

import com.movtery.zalithlauncher.path.PathManager
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.apache.commons.io.FileUtils
import java.io.File
import java.util.UUID

/**
 * A cape stored in an account's local cape collection.
 * Textures are always stored as PNG, because the game can only decode PNG capes.
 *
 * @param id unique id, also the texture file name
 * @param name display name
 * @param favorite reserved for sorting in the UI
 * @param source where the cape came from, e.g. "imported"
 * @param labyCapeId reserved for the online gallery
 */
@Serializable
data class CapeEntry(
    val id: String,
    val name: String,
    val favorite: Boolean = false,
    val source: String = SOURCE_IMPORTED,
    val labyCapeId: String? = null
) {
    companion object {
        const val SOURCE_IMPORTED = "imported"
    }
}

@Serializable
data class CapeManifest(
    val activeCapeId: String? = null,
    val capes: List<CapeEntry> = emptyList()
)

/**
 * Per-account cape collection used by offline (local) accounts.
 * Layout: `account_capes/<accountUUID>/capes.json` plus `<capeId>.png` textures.
 *
 * Idea and structure ported from ZalithLauncher2Plus (Star1xr, GPL-3.0).
 */
object AccountCapeCollection {
    private const val MANIFEST_NAME = "capes.json"

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    fun getCollectionDir(accountUUID: String): File =
        File(PathManager.DIR_ACCOUNT_CAPE, accountUUID)

    private fun getManifestFile(accountUUID: String): File =
        File(getCollectionDir(accountUUID), MANIFEST_NAME)

    fun getCapeTextureFile(accountUUID: String, capeId: String): File =
        File(getCollectionDir(accountUUID), "$capeId.png")

    @Synchronized
    fun loadManifest(accountUUID: String): CapeManifest {
        val file = getManifestFile(accountUUID)
        if (!file.exists()) return CapeManifest()
        return runCatching {
            json.decodeFromString<CapeManifest>(file.readText())
        }.getOrDefault(CapeManifest())
    }

    private fun saveManifest(accountUUID: String, manifest: CapeManifest) {
        val dir = getCollectionDir(accountUUID).apply { mkdirs() }
        //write to a temp file first so a crash can't leave a half-written manifest
        val tmp = File(dir, "$MANIFEST_NAME.tmp")
        tmp.writeText(json.encodeToString(manifest))
        FileUtils.deleteQuietly(getManifestFile(accountUUID))
        FileUtils.moveFile(tmp, getManifestFile(accountUUID))
    }

    /**
     * Add a cape to the collection.
     * @param pngBytes texture, must already be a valid PNG
     * @param activate make this cape the active one
     */
    @Synchronized
    fun addCape(
        accountUUID: String,
        name: String,
        pngBytes: ByteArray,
        source: String = CapeEntry.SOURCE_IMPORTED,
        labyCapeId: String? = null,
        activate: Boolean = true
    ): CapeEntry {
        val manifest = loadManifest(accountUUID)
        val entry = CapeEntry(
            id = UUID.randomUUID().toString().lowercase(),
            name = name,
            source = source,
            labyCapeId = labyCapeId
        )
        getCollectionDir(accountUUID).mkdirs()
        getCapeTextureFile(accountUUID, entry.id).writeBytes(pngBytes)
        saveManifest(
            accountUUID,
            manifest.copy(
                capes = manifest.capes + entry,
                activeCapeId = if (activate || manifest.activeCapeId == null) entry.id else manifest.activeCapeId
            )
        )
        return entry
    }

    /**
     * Remove a cape. If it was the active one, the account ends up with no cape.
     */
    @Synchronized
    fun removeCape(accountUUID: String, capeId: String) {
        val manifest = loadManifest(accountUUID)
        saveManifest(
            accountUUID,
            manifest.copy(
                capes = manifest.capes.filter { it.id != capeId },
                activeCapeId = manifest.activeCapeId.takeIf { it != capeId }
            )
        )
        FileUtils.deleteQuietly(getCapeTextureFile(accountUUID, capeId))
    }

    @Synchronized
    fun setActiveCape(accountUUID: String, capeId: String) {
        val manifest = loadManifest(accountUUID)
        if (manifest.capes.any { it.id == capeId }) {
            saveManifest(accountUUID, manifest.copy(activeCapeId = capeId))
        }
    }

    /**
     * Stop using any cape, without deleting the collection.
     */
    @Synchronized
    fun clearActiveCape(accountUUID: String) {
        val manifest = loadManifest(accountUUID)
        if (manifest.activeCapeId != null) {
            saveManifest(accountUUID, manifest.copy(activeCapeId = null))
        }
    }

    @Synchronized
    fun renameCape(accountUUID: String, capeId: String, newName: String) {
        val manifest = loadManifest(accountUUID)
        saveManifest(
            accountUUID,
            manifest.copy(
                capes = manifest.capes.map { if (it.id == capeId) it.copy(name = newName) else it }
            )
        )
    }

    @Synchronized
    fun toggleFavorite(accountUUID: String, capeId: String) {
        val manifest = loadManifest(accountUUID)
        saveManifest(
            accountUUID,
            manifest.copy(
                capes = manifest.capes.map { if (it.id == capeId) it.copy(favorite = !it.favorite) else it }
            )
        )
    }

    /**
     * @return the texture of the active cape, or null if there is none
     */
    fun getActiveCapeFile(accountUUID: String): File? {
        val activeId = loadManifest(accountUUID).activeCapeId ?: return null
        return getCapeTextureFile(accountUUID, activeId).takeIf { it.exists() }
    }

    /**
     * Delete the whole collection, used when the account is removed.
     */
    @Synchronized
    fun deleteCollection(accountUUID: String) {
        FileUtils.deleteQuietly(getCollectionDir(accountUUID))
    }
}
