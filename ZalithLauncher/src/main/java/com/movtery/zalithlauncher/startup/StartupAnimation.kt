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

package com.movtery.zalithlauncher.startup

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.movtery.zalithlauncher.context.copyLocalFile
import com.movtery.zalithlauncher.path.PathManager
import com.movtery.zalithlauncher.setting.AllSettings
import org.apache.commons.io.FileUtils
import java.io.File

/** The imported video is longer than [StartupAnimation.MAX_DURATION_MILLIS] */
class StartupVideoTooLongException : IllegalArgumentException()

/** The imported file is not a playable video */
class StartupVideoInvalidException : IllegalArgumentException()

/** The user imported startup animation video. The launcher ships without one. */
object StartupAnimation {
    const val MAX_DURATION_MILLIS = 10_000L

    private val videoFile: File
        get() = File(PathManager.DIR_FILES_PRIVATE, "startup_animation/video")

    private var changes by mutableStateOf(0)

    /** The imported video, null when there is none. Observable from Compose. */
    val video: File?
        get() {
            changes //read so that importing and removing recompose the readers
            return videoFile.takeIf { it.isFile }
        }

    /** The video to play at startup, null when the animation is disabled or unavailable */
    fun videoToPlay(): File? = video?.takeIf { AllSettings.startupAnimationEnabled.getValue() }

    /** Copies and validates [uri], the current video is only replaced when the new one is valid */
    fun import(context: Context, uri: Uri) {
        val target = videoFile
        val temp = File(target.parentFile, "video.tmp")
        try {
            context.copyLocalFile(uri, temp)
            validate(temp)
            FileUtils.deleteQuietly(target)
            FileUtils.moveFile(temp, target)
            //A new video starts without the placement of the old one
            StartupVideoLayout().save()
            changes++
        } finally {
            FileUtils.deleteQuietly(temp)
        }
    }

    fun remove() {
        FileUtils.deleteQuietly(videoFile)
        changes++
    }

    private fun validate(file: File) {
        val retriever = MediaMetadataRetriever()
        val duration = try {
            retriever.setDataSource(file.absolutePath)
            if (retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO) == null) null
            else retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
        } catch (_: RuntimeException) {
            null
        } finally {
            retriever.release()
        }
        if (duration == null) throw StartupVideoInvalidException()
        if (duration > MAX_DURATION_MILLIS) throw StartupVideoTooLongException()
    }
}
