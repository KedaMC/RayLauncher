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

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.media.ExifInterface
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import com.movtery.zalithlauncher.setting.enums.GlassTier
import com.movtery.zalithlauncher.utils.image.isGifFile
import com.movtery.zalithlauncher.viewmodel.LocalBackgroundViewModel
import com.styropyr0.prismal.sources.PrismalGlassLayer
import com.styropyr0.prismal.sources.prismalGlassLayer
import com.styropyr0.prismal.sources.rememberPrismalGlassLayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Shared pre-blurred wallpaper for Liquid Glass.
 *
 * Every glass surface normally records the wallpaper into its own layer and runs a full resolution
 * blur on it each time it draws, so the cost grows with the number of cards on screen. For a still
 * wallpaper that blur never changes, so it is done once here, at low resolution, and recorded into a
 * single [PrismalGlassLayer]. Surfaces on opted-in pages (see [SharedGlassBlurPage]) sample that layer
 * with their own blur turned off; edge refraction, tint and press response stay per surface, so the
 * look is unchanged. Video and GIF wallpapers, other blur radii and anything inside a dialog or popup
 * window keep the live blur.
 */
@Stable
class SharedGlassBlur internal constructor(
    internal val slots: List<SharedBlurSlot>
) {
    /** The shared layer holding an image blurred with [radiusDp], otherwise null (use the live blur). */
    fun layerFor(radiusDp: Float): PrismalGlassLayer? =
        slots.firstOrNull { it.image != null && abs(radiusDp - it.radiusDp) < 0.01f }?.layer
}

/** One pre-blurred copy of the wallpaper: its layer, image and the blur radius (dp) it was made with. */
@Stable
internal class SharedBlurSlot(internal val layer: PrismalGlassLayer, internal val imageState: MutableState<ImageBitmap?>) {
    var radiusDp by mutableFloatStateOf(0f)
        internal set
    val image: ImageBitmap? get() = imageState.value
}

/** Set by the theme in the launcher when Liquid Glass is on. Null in game and when glass is off. */
val LocalLiquidGlassSharedBlur = staticCompositionLocalOf<SharedGlassBlur?> { null }

/** True inside a page that opted in with [SharedGlassBlurPage]. */
private val LocalSharedGlassBlurPage = staticCompositionLocalOf { false }

/**
 * Blur radii (dp) served by shared images: the default radius of [LiquidGlassSurface] (8), the home
 * title and version card (10) and the page header and home toolbar (12).
 */
private val SHARED_BLUR_RADII_DP = floatArrayOf(8f, 10f, 12f)

/** The shared image is a quarter of the page size; the blur is far too soft to show the difference. */
private const val SHARED_BLUR_DOWNSCALE = 4

@Composable
internal fun rememberSharedGlassBlur(): SharedGlassBlur {
    val slots = SHARED_BLUR_RADII_DP.map { _ ->
        val imageState = remember { mutableStateOf<ImageBitmap?>(null) }
        val onDraw = remember<ContentDrawScope.() -> Unit> {
            {
                val image = imageState.value
                if (image != null) {
                    drawImage(
                        image = image,
                        dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
                        filterQuality = FilterQuality.Low
                    )
                }
            }
        }
        SharedBlurSlot(rememberPrismalGlassLayer(onDraw = onDraw), imageState)
    }
    return remember(slots.map { it.layer }) { SharedGlassBlur(slots) }
}

/**
 * Opts the glass surfaces inside [content] into the shared pre-blurred wallpaper (when it is available).
 */
@Composable
fun SharedGlassBlurPage(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalSharedGlassBlurPage provides true, content = content)
}

/**
 * The shared layer to sample instead of [backdrop], or null to keep the live blur. Only the launcher
 * wallpaper layer qualifies: dialog and popup samplers keep their own window-corrected backdrop.
 */
@Composable
internal fun sharedGlassLayerFor(backdrop: Any, blurRadiusDp: Float): PrismalGlassLayer? {
    if (!LocalSharedGlassBlurPage.current) return null
    val shared = LocalLiquidGlassSharedBlur.current ?: return null
    if (backdrop !== LocalLiquidGlassBackdrop.current) return null
    return shared.layerFor(blurRadiusDp)
}

/**
 * Put next to the wallpaper in the launcher page container. Makes the shared pre-blurred image for the
 * current still wallpaper and records it into the shared layer at the same position and size as the
 * wallpaper. Draws nothing on screen.
 */
@Composable
fun SharedGlassBlurRecorder() {
    val shared = LocalLiquidGlassSharedBlur.current ?: return
    val background = LocalBackgroundViewModel.current ?: return
    val density = LocalDensity.current.density
    var size by remember { mutableStateOf(IntSize.Zero) }

    val lite = launcherGlassTier() == GlassTier.LITE
    val file = background.activeFile
    val candidate = background.isValid && background.isImage

    LaunchedEffect(file, background.refreshTrigger, candidate, size, lite, density) {
        val radii = SHARED_BLUR_RADII_DP.map { if (lite) it * LITE_BLUR_FACTOR else it }
        shared.slots.forEachIndexed { i, slot -> slot.publish(null, radii[i]) }
        if (!candidate || size.width <= 0 || size.height <= 0) return@LaunchedEffect
        val images = withContext(Dispatchers.IO) {
            runCatching { renderBlurredWallpapers(file, size, radii.map { it * density }) }.getOrNull()
        } ?: return@LaunchedEffect
        shared.slots.forEachIndexed { i, slot -> slot.publish(images[i].asImageBitmap(), radii[i]) }
    }

    Box(modifier = Modifier.fillMaxSize().onSizeChanged { size = it }) {
        shared.slots.forEach { slot ->
            Box(modifier = Modifier.fillMaxSize().prismalGlassLayer(slot.layer))
        }
    }
}

private fun SharedBlurSlot.publish(image: ImageBitmap?, radiusDp: Float) {
    imageState.value = image
    this.radiusDp = radiusDp
}

/**
 * Decodes [file] the way the wallpaper is shown (centre crop, EXIF orientation) at a quarter of
 * [pageSize] once, then blurs a copy per entry of [sigmasPx] with a Gaussian (measured at full size).
 * Null for GIFs, which animate and so cannot share a still blur.
 */
private fun renderBlurredWallpapers(file: File, pageSize: IntSize, sigmasPx: List<Float>): List<Bitmap>? {
    if (!file.exists() || file.isGifFile()) return null
    val targetWidth = (pageSize.width / SHARED_BLUR_DOWNSCALE).coerceAtLeast(1)
    val targetHeight = (pageSize.height / SHARED_BLUR_DOWNSCALE).coerceAtLeast(1)

    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.absolutePath, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    val orientation = readExifOrientation(file)
    val swapped = orientation == ExifInterface.ORIENTATION_TRANSPOSE ||
        orientation == ExifInterface.ORIENTATION_ROTATE_90 ||
        orientation == ExifInterface.ORIENTATION_TRANSVERSE ||
        orientation == ExifInterface.ORIENTATION_ROTATE_270
    val shownWidth = if (swapped) bounds.outHeight else bounds.outWidth
    val shownHeight = if (swapped) bounds.outWidth else bounds.outHeight

    //Largest power of two that still leaves the picture at least as big as the target
    var sample = 1
    while (shownWidth / (sample * 2) >= targetWidth && shownHeight / (sample * 2) >= targetHeight) sample *= 2

    val decoded = BitmapFactory.decodeFile(
        file.absolutePath,
        BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
    ) ?: return null

    val matrix = exifMatrix(orientation)
    val shown = RectF(0f, 0f, decoded.width.toFloat(), decoded.height.toFloat())
    matrix.mapRect(shown)
    matrix.postTranslate(-shown.left, -shown.top)
    val scale = max(targetWidth / shown.width(), targetHeight / shown.height())
    matrix.postScale(scale, scale)
    matrix.postTranslate((targetWidth - shown.width() * scale) / 2f, (targetHeight - shown.height() * scale) / 2f)

    val scaled = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
    Canvas(scaled).drawBitmap(decoded, matrix, Paint(Paint.FILTER_BITMAP_FLAG))
    decoded.recycle()

    val blurred = sigmasPx.map { gaussianBlur(scaled.copy(Bitmap.Config.ARGB_8888, false), it / SHARED_BLUR_DOWNSCALE) }
    scaled.recycle()
    return blurred
}

private fun readExifOrientation(file: File): Int = runCatching {
    ExifInterface(file.absolutePath).getAttributeInt(
        ExifInterface.TAG_ORIENTATION,
        ExifInterface.ORIENTATION_NORMAL
    )
}.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

private fun exifMatrix(orientation: Int): Matrix = Matrix().apply {
    when (orientation) {
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> postScale(-1f, 1f)
        ExifInterface.ORIENTATION_ROTATE_180 -> postRotate(180f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> postScale(1f, -1f)
        ExifInterface.ORIENTATION_TRANSPOSE -> {
            postRotate(90f)
            postScale(-1f, 1f)
        }
        ExifInterface.ORIENTATION_ROTATE_90 -> postRotate(90f)
        ExifInterface.ORIENTATION_TRANSVERSE -> {
            postRotate(270f)
            postScale(-1f, 1f)
        }
        ExifInterface.ORIENTATION_ROTATE_270 -> postRotate(270f)
    }
}

/**
 * Three box blur passes per axis approximate a Gaussian of [sigma] (the same radius semantics as
 * RenderEffect blur). Edges are clamped, like the live blur.
 */
private fun gaussianBlur(source: Bitmap, sigma: Float): Bitmap {
    if (sigma < 0.5f) return source
    val width = source.width
    val height = source.height
    val pixels = IntArray(width * height)
    source.getPixels(pixels, 0, width, 0, 0, width, height)
    source.recycle()

    val temp = IntArray(pixels.size)
    for (boxSize in boxSizesForGaussian(sigma)) {
        val radius = (boxSize - 1) / 2
        if (radius < 1) continue
        boxBlurPass(pixels, temp, width, height, radius, horizontal = true)
        boxBlurPass(temp, pixels, width, height, radius, horizontal = false)
    }

    val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    result.setPixels(pixels, 0, width, 0, 0, width, height)
    return result
}

/** Widths (odd) of three successive box blurs whose combined variance matches a Gaussian of [sigma]. */
private fun boxSizesForGaussian(sigma: Float): IntArray {
    val passes = 3
    val ideal = sqrt(12f * sigma * sigma / passes + 1f)
    var lower = floor(ideal).toInt()
    if (lower % 2 == 0) lower--
    lower = lower.coerceAtLeast(1)
    val upper = lower + 2
    val lowerCount = ((12f * sigma * sigma - passes * lower * lower - 4f * passes * lower - 3f * passes) /
        (-4f * lower - 4f)).roundToInt()
    return IntArray(passes) { if (it < lowerCount) lower else upper }
}

private fun boxBlurPass(
    src: IntArray,
    dst: IntArray,
    width: Int,
    height: Int,
    radius: Int,
    horizontal: Boolean
) {
    val window = radius * 2 + 1
    val half = window / 2
    val lines = if (horizontal) height else width
    val length = if (horizontal) width else height
    for (line in 0 until lines) {
        fun at(i: Int): Int {
            val clamped = i.coerceIn(0, length - 1)
            return if (horizontal) src[line * width + clamped] else src[clamped * width + line]
        }

        var sumA = 0
        var sumR = 0
        var sumG = 0
        var sumB = 0
        for (i in -radius..radius) {
            val p = at(i)
            sumA += p ushr 24 and 0xFF
            sumR += p ushr 16 and 0xFF
            sumG += p ushr 8 and 0xFF
            sumB += p and 0xFF
        }
        for (i in 0 until length) {
            val out = (((sumA + half) / window) shl 24) or
                (((sumR + half) / window) shl 16) or
                (((sumG + half) / window) shl 8) or
                ((sumB + half) / window)
            if (horizontal) dst[line * width + i] = out else dst[i * width + line] = out

            val outP = at(i - radius)
            val inP = at(i + radius + 1)
            sumA += (inP ushr 24 and 0xFF) - (outP ushr 24 and 0xFF)
            sumR += (inP ushr 16 and 0xFF) - (outP ushr 16 and 0xFF)
            sumG += (inP ushr 8 and 0xFF) - (outP ushr 8 and 0xFF)
            sumB += (inP and 0xFF) - (outP and 0xFF)
        }
    }
}
