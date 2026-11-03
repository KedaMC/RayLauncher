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

package com.movtery.zalithlauncher.game.version.installed.theme

import android.content.Context
import android.net.Uri
import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import com.movtery.zalithlauncher.context.copyLocalFile
import com.movtery.zalithlauncher.game.version.installed.Version
import com.movtery.zalithlauncher.setting.enums.GlassTier
import com.movtery.zalithlauncher.ui.theme.profileWallpaperFile
import com.movtery.zalithlauncher.utils.GSON
import com.movtery.zalithlauncher.utils.image.isImageFile
import com.movtery.zalithlauncher.utils.video.isVideoFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.io.FileUtils
import java.io.File
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** Extension of exported theme files. */
const val PROFILE_THEME_EXTENSION = "raytheme"

private const val ENTRY_THEME = "theme.json"
private const val ENTRY_WALLPAPER = "wallpaper.file"
private const val THEME_FORMAT = 1

/** Content of `theme.json` inside a theme file. Null fields mean "follow the launcher". */
@Keep
private class ThemeJson(
    @SerializedName("format")
    val format: Int = THEME_FORMAT,
    @SerializedName("accent")
    val accent: Int? = null,
    @SerializedName("glassTier")
    val glassTier: GlassTier? = null,
    @SerializedName("hasWallpaper")
    val hasWallpaper: Boolean = false
)

/**
 * Reading and writing the theme of one version: accent color, glass tier and wallpaper.
 * A theme file is a zip with `theme.json` and, when the version has one, `wallpaper.file`.
 */
object ProfileThemeIO {
    /**
     * Stores [uri] as the wallpaper of [version]. A file that is neither an image nor a video is
     * removed again and an error is thrown.
     */
    suspend fun importWallpaper(context: Context, version: Version, uri: Uri) = withContext<Unit>(Dispatchers.IO) {
        val target = profileWallpaperFile(version)
        FileUtils.deleteQuietly(target)
        context.copyLocalFile(uri, target)
        if (!target.isImageFile() && !target.isVideoFile()) {
            FileUtils.deleteQuietly(target)
            error("The selected file is not an image or a video!")
        }
    }

    suspend fun removeWallpaper(version: Version) = withContext<Unit>(Dispatchers.IO) {
        FileUtils.deleteQuietly(profileWallpaperFile(version))
    }

    /** Clears accent, glass tier and wallpaper of [version] (the config is not saved here). */
    suspend fun reset(version: Version) {
        val config = version.getVersionConfig()
        config.themeAccent = null
        config.themeGlassTier = null
        removeWallpaper(version)
    }

    /** Writes the theme of [version] as a zip to [outputUri]. */
    suspend fun export(context: Context, version: Version, outputUri: Uri) = withContext<Unit>(Dispatchers.IO) {
        val config = version.getVersionConfig()
        val wallpaper = profileWallpaperFile(version).takeIf { it.isFile }
        val json = ThemeJson(
            accent = config.themeAccent,
            glassTier = config.themeGlassTier,
            hasWallpaper = wallpaper != null
        )

        val output = context.contentResolver.openOutputStream(outputUri)
            ?: throw IOException("Unable to open the output file")
        ZipOutputStream(output.buffered()).use { zip ->
            zip.putNextEntry(ZipEntry(ENTRY_THEME))
            zip.write(GSON.toJson(json).toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            if (wallpaper != null) {
                zip.putNextEntry(ZipEntry(ENTRY_WALLPAPER))
                wallpaper.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
    }

    /**
     * Applies the theme file at [inputUri] to [version]. Accent and glass tier are set on the
     * version's config (the caller saves it); the wallpaper is replaced or removed.
     */
    suspend fun import(context: Context, version: Version, inputUri: Uri) = withContext<Unit>(Dispatchers.IO) {
        var themeText: String? = null
        val wallpaperTemp = File(version.getZalithVersionPath(), "$ENTRY_WALLPAPER.tmp")
        FileUtils.deleteQuietly(wallpaperTemp)
        wallpaperTemp.parentFile?.mkdirs()

        try {
            val input = context.contentResolver.openInputStream(inputUri)
                ?: throw IOException("Unable to open the theme file")
            ZipInputStream(input.buffered()).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    when (entry.name) {
                        ENTRY_THEME -> themeText = zip.readBytes().toString(Charsets.UTF_8)
                        ENTRY_WALLPAPER -> wallpaperTemp.outputStream().use { zip.copyTo(it) }
                    }
                    entry = zip.nextEntry
                }
            }

            val json = themeText?.let { runCatching { GSON.fromJson(it, ThemeJson::class.java) }.getOrNull() }
                ?: error("This is not a valid theme file")
            if (json.format > THEME_FORMAT) error("This theme file needs a newer version of the launcher")

            //Check the wallpaper before touching anything of the current theme
            val wallpaperValid = json.hasWallpaper && wallpaperTemp.isFile &&
                    (wallpaperTemp.isImageFile() || wallpaperTemp.isVideoFile())

            val config = version.getVersionConfig()
            config.themeAccent = json.accent
            config.themeGlassTier = json.glassTier

            val target = profileWallpaperFile(version)
            FileUtils.deleteQuietly(target)
            if (wallpaperValid) FileUtils.moveFile(wallpaperTemp, target)
        } finally {
            FileUtils.deleteQuietly(wallpaperTemp)
        }
    }
}
