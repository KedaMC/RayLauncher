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

package com.movtery.zalithlauncher.ui.components

import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.PointerInputModifierNode
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.crossfade
import com.movtery.zalithlauncher.context.copyLocalFile
import com.movtery.zalithlauncher.path.PathManager
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.utils.image.isGifFile
import com.movtery.zalithlauncher.utils.image.isImageFile
import com.movtery.zalithlauncher.utils.video.isVideoFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.io.FileUtils
import java.io.File
import kotlin.math.roundToInt

/**
 * Keeps the decoration overlay file (an image or a video picked from the gallery) and what is known
 * about it. The overlay is drawn by [LauncherOverlay].
 */
object OverlayManager {
    enum class Kind { NONE, IMAGE, VIDEO }

    /** Imported overlay file, inside the launcher's private storage. */
    val file: File get() = File(PathManager.DIR_BACKGROUND, "overlay01.file")

    var kind by mutableStateOf(Kind.NONE)
        private set

    /** Width / height of the overlay media */
    var aspectRatio by mutableFloatStateOf(1f)
        private set

    /** Bumped whenever the file changes, so players and image loaders reload it */
    var generation by mutableIntStateOf(0)
        private set

    /** Reads the overlay file and updates [kind] and [aspectRatio]. */
    suspend fun refresh() {
        val result = withContext(Dispatchers.IO) { inspect(file) }
        kind = result.first
        aspectRatio = result.second
        generation++
    }

    /**
     * Copies [uri] into the overlay file. GIF is refused, and a file that is neither an image nor a
     * video is removed again. Throws with a readable message on failure.
     */
    suspend fun import(context: Context, uri: Uri, gifUnsupportedMessage: String) {
        withContext(Dispatchers.IO) {
            val target = file
            FileUtils.deleteQuietly(target)
            context.copyLocalFile(uri, target)
            if (target.isGifFile()) {
                FileUtils.deleteQuietly(target)
                error(gifUnsupportedMessage)
            }
            if (!target.isImageFile() && !target.isVideoFile()) {
                FileUtils.deleteQuietly(target)
                error("The selected file is not an image or a video!")
            }
        }
        refresh()
    }

    suspend fun remove() {
        withContext(Dispatchers.IO) { FileUtils.deleteQuietly(file) }
        refresh()
    }

    private fun inspect(target: File): Pair<Kind, Float> {
        if (!target.exists()) return Kind.NONE to 1f

        if (target.isImageFile()) {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(target.absolutePath, bounds)
            if (bounds.outWidth > 0 && bounds.outHeight > 0) {
                return Kind.IMAGE to bounds.outWidth.toFloat() / bounds.outHeight
            }
        }

        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(target.absolutePath)
            val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toFloatOrNull()
            val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toFloatOrNull()
            val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
            if (width != null && height != null && width > 0f && height > 0f) {
                val ratio = if (rotation == 90 || rotation == 270) height / width else width / height
                Kind.VIDEO to ratio
            } else {
                Kind.NONE to 1f
            }
        } catch (_: Exception) {
            Kind.NONE to 1f
        } finally {
            retriever.release()
        }
    }
}

/**
 * Decoration drawn above the launcher UI: an image or a muted, looping video from the gallery.
 * Position, size and opacity come from the launcher settings. It never takes touches: they pass
 * through to the UI underneath.
 */
@OptIn(UnstableApi::class)
@Composable
fun LauncherOverlay(modifier: Modifier = Modifier) {
    LaunchedEffect(Unit) { OverlayManager.refresh() }

    if (!AllSettings.launcherOverlayEnabled.state) return
    val kind = OverlayManager.kind
    if (kind == OverlayManager.Kind.NONE) return

    val context = LocalContext.current
    val density = LocalDensity.current
    val generation = OverlayManager.generation
    val aspectRatio = OverlayManager.aspectRatio.coerceIn(0.05f, 20f)
    val positionX = AllSettings.launcherOverlayX.state / 100f
    val positionY = AllSettings.launcherOverlayY.state / 100f
    val scale = AllSettings.launcherOverlayScale.state / 100f
    val opacity = AllSettings.launcherOverlayOpacity.state / 100f

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val containerWidth = constraints.maxWidth.toFloat()
        val containerHeight = constraints.maxHeight.toFloat()

        //Width follows the setting; height follows the media's aspect ratio, never taller than the screen
        var overlayWidth = containerWidth * scale
        var overlayHeight = overlayWidth / aspectRatio
        if (overlayHeight > containerHeight) {
            overlayHeight = containerHeight
            overlayWidth = overlayHeight * aspectRatio
        }
        val offsetX = ((containerWidth - overlayWidth) * positionX).roundToInt()
        val offsetY = ((containerHeight - overlayHeight) * positionY).roundToInt()

        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX, offsetY) }
                .size(
                    width = with(density) { overlayWidth.toDp() },
                    height = with(density) { overlayHeight.toDp() }
                )
                .graphicsLayer { alpha = opacity }
                .then(PassThroughPointerElement)
        ) {
            when (kind) {
                OverlayManager.Kind.IMAGE -> {
                    val request = remember(generation) {
                        ImageRequest.Builder(context)
                            .data(OverlayManager.file)
                            .allowHardware(false)
                            .crossfade(false)
                            .build()
                    }
                    AsyncImage(
                        modifier = Modifier.fillMaxSize(),
                        model = request,
                        contentDescription = null,
                        contentScale = ContentScale.Fit
                    )
                }
                OverlayManager.Kind.VIDEO -> {
                    VideoPlayer(
                        videoUri = Uri.fromFile(OverlayManager.file),
                        modifier = Modifier.fillMaxSize(),
                        muted = true,
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT,
                        refreshTrigger = generation
                    )
                }
                OverlayManager.Kind.NONE -> {}
            }
        }
    }
}

/**
 * Takes part in pointer hit testing but consumes nothing and lets siblings below receive the same
 * events. Without it a video overlay (an Android view) would swallow the touches in its area.
 */
private object PassThroughPointerElement : ModifierNodeElement<PassThroughPointerNode>() {
    override fun create() = PassThroughPointerNode()

    override fun update(node: PassThroughPointerNode) {}

    override fun hashCode(): Int = 1

    override fun equals(other: Any?): Boolean = other === this
}

private class PassThroughPointerNode : Modifier.Node(), PointerInputModifierNode {
    override fun onPointerEvent(pointerEvent: PointerEvent, pass: PointerEventPass, bounds: IntSize) {}

    override fun onCancelPointerInput() {}

    override fun sharePointerInputWithSiblings(): Boolean = true
}
