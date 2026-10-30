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

import androidx.compose.foundation.background
import com.styropyr0.prismal.sources.prismalGlassLayer
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.layout.Layout
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.TransformOrigin
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.PixelCopy
import android.view.Window
import android.view.WindowManager
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Density
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.styropyr0.prismal.PrismalBackdrop
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.setting.enums.GlassTier
import com.movtery.zalithlauncher.viewmodel.LocalBackgroundViewModel
import com.styropyr0.prismal.PrismalGlassSurface
import com.styropyr0.prismal.sources.PrismalGlassLayer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * Liquid Glass (PrismalAGSL) 采样的背景图层。
 * 仅当设置开启且背景可被采样（图片背景）时由 MainActivity 提供，否则为 null
 */
val LocalLiquidGlassBackdrop = staticCompositionLocalOf<PrismalGlassLayer?> { null }

/** True for UI hosted in the game activity (menu panels), false for the launcher itself. */
val LocalLiquidGlassInGame = staticCompositionLocalOf { false }

/**
 * Whether Liquid Glass styling is on for the UI being composed. In the launcher this is the launcher
 * setting. In game it additionally needs the in-game switch, so turning glass off from the game menu
 * leaves the launcher's own glass untouched.
 */
@Composable
@ReadOnlyComposable
fun liquidGlassEnabled(): Boolean =
    if (LocalLiquidGlassInGame.current) {
        AllSettings.launcherLiquidGlass.state && AllSettings.gameLiquidGlass.state
    } else {
        launcherGlassOn()
    }

/**
 * Quality of the glass being composed: the in-game tier in game, otherwise the launcher tier
 * (selected version's theme first). Never [GlassTier.OFF]; use [liquidGlassEnabled] for on/off.
 */
@Composable
@ReadOnlyComposable
fun liquidGlassTier(): GlassTier {
    val tier = if (LocalLiquidGlassInGame.current) AllSettings.gameGlassTier.state else launcherGlassTier()
    return if (tier == GlassTier.OFF) GlassTier.FULL else tier
}

/**
 * What glass surfaces actually sample. Normally this is [LocalLiquidGlassBackdrop] itself; inside a
 * dialog window [GlassDialog] swaps in a window-corrected sampler (see [DialogWindowBackdrop]).
 */
val LocalLiquidGlassSampler = staticCompositionLocalOf<PrismalBackdrop?> { null }

/**
 * Master switch for recording the whole page behind open dialogs. Disabled: with it on, opening a
 * dialog a second time (e.g. "Add account") crashed the launcher. While off, dialogs sample the
 * wallpaper layer instead (still corrected for the dialog window's position).
 */
private const val RECORD_PAGE_BEHIND_DIALOGS = false

/**
 * Records the whole launcher page (wallpaper + screen UI) so dialogs can blur what is really behind
 * them, not only the wallpaper. Recording is only switched on while a dialog is open
 * ([openDialogs] > 0), because it draws the page content a second time.
 */
@Stable
class GlassPageCapture(val layer: PrismalGlassLayer) {
    /** Number of [GlassDialog]s currently shown. */
    var openDialogs by mutableIntStateOf(0)

    /** True while the page is actually being recorded into [layer]. */
    val recording: Boolean get() = RECORD_PAGE_BEHIND_DIALOGS && openDialogs > 0

    /** Screen position of the captured page's top-left corner. */
    var originOnScreen by mutableStateOf(Offset.Zero)
}

/** Provided by the theme in activities that show a launcher page. Null elsewhere. */
val LocalLiquidGlassPage = staticCompositionLocalOf<GlassPageCapture?> { null }

/**
 * Put on the container that holds the wallpaper and the screen content. Remembers where the page is
 * on screen and, while a dialog is open, records the page into [GlassPageCapture.layer].
 */
@Composable
fun Modifier.glassPageCapture(): Modifier {
    val page = LocalLiquidGlassPage.current ?: return this
    val recording = page.recording
    return this
        .onGloballyPositioned { coordinates ->
            if (coordinates.isAttached) page.originOnScreen = coordinates.localToScreen(Offset.Zero)
        }
        .then(if (recording) Modifier.prismalGlassLayer(page.layer) else Modifier)
}

/**
 * True while a [GlassDialog] is open and the launcher page is being recorded into a glass layer.
 *
 * Native views (WebView, etc.) must not be drawn into that recording: their draw is not a plain
 * display list and crashes when it is replayed a second time or from the dialog's window.
 * Composables hosting such a view should hide it while this is true.
 */
@Composable
fun isGlassPageRecording(): Boolean = LocalLiquidGlassPage.current?.recording == true

/**
 * A dialog is its own Android window with its own coordinate space, so Prismal's
 * `layerCoordinates.localPositionOf(glass)` cannot relate the glass to the recorded layer and falls
 * back to a window-relative offset of about (0, 0): the glass then shows the top-left of the layer.
 * This sampler measures the glass on screen instead and subtracts the layer's screen origin.
 *
 * Two more corrections are made here:
 *  - Video wallpaper: the recorded layer is shared between windows and does not follow the video
 *    decoder, so the glass in a dialog showed a frozen frame. While a video plays, [GlassDialog]
 *    copies the launcher window (video + panels, without the dialog) into a small bitmap a few
 *    times per second ([publishSnapshot]) and that bitmap is drawn instead of the layer.
 *  - Scrim: the dialog's dim scrim darkens everything around the dialog but not the dialog
 *    itself, which made the glass look too bright. [dimAlpha] darkens the sampled backdrop to match.
 */
@Stable
private class DialogWindowBackdrop(
    private val source: PrismalGlassLayer,
    private val page: GlassPageCapture?
) : PrismalBackdrop {

    override val isCoordinatesDependent: Boolean = true

    /** Alpha of the dialog window's dim scrim, drawn over the sampled backdrop. */
    var dimAlpha: Float = 0f

    private var snapshot: ImageBitmap? = null
    private var snapshotLeft = 0
    private var snapshotTop = 0
    private var snapshotWidth = 0
    private var snapshotHeight = 0

    /** Read in [readSamplingState] so glass nodes redraw whenever a new snapshot arrives. */
    private var snapshotVersion by mutableIntStateOf(0)

    /**
     * Reads the snapshot counter. [LiquidGlassSurface] calls this from a draw modifier inside the
     * glass, so the glass re-records every time a new snapshot arrives, exactly like a glass that
     * contains an animation does. Relying only on [readSamplingState] left most panels frozen.
     */
    fun readFrame(): Int = snapshotVersion

    /** Latest copy of the launcher window; its top-left is at ([left], [top]) in screen pixels. */
    fun publishSnapshot(image: ImageBitmap, left: Int, top: Int, width: Int, height: Int) {
        snapshot = image
        snapshotLeft = left
        snapshotTop = top
        snapshotWidth = width
        snapshotHeight = height
        snapshotVersion++
    }

    override fun readSamplingState() {
        source.readSamplingState()
        page?.originOnScreen
        snapshotVersion
    }

    override fun DrawScope.drawPrismalGlass(
        density: Density,
        coordinates: LayoutCoordinates?,
        layerBlock: (GraphicsLayerScope.() -> Unit)?
    ) {
        val glass = coordinates ?: return
        if (!glass.isAttached) return
        val glassOnScreen = glass.localToScreen(Offset.Zero)
        val image = snapshot
        if (image != null) {
            withTransform({
                translate(snapshotLeft - glassOnScreen.x, snapshotTop - glassOnScreen.y)
            }) {
                drawImage(
                    image = image,
                    dstSize = IntSize(snapshotWidth, snapshotHeight),
                    filterQuality = FilterQuality.Low
                )
            }
        } else {
            val offset = glassOnScreen - (page?.originOnScreen ?: Offset.Zero)
            withTransform({ translate(-offset.x, -offset.y) }) {
                drawLayer(source.graphicsLayer)
            }
        }
        if (dimAlpha > 0f) drawRect(Color.Black.copy(alpha = dimAlpha))
    }
}

/** The launcher window is copied at 1/4 size: plenty for glass that is blurred anyway. */
private const val SNAPSHOT_DOWNSCALE = 4

/** At most ~30 snapshots per second. */
private const val SNAPSHOT_MIN_INTERVAL_NANOS = 30_000_000L

/** Lite tier: 1/8 size and at most ~15 snapshots per second. */
private const val LITE_SNAPSHOT_DOWNSCALE = 8
private const val LITE_SNAPSHOT_MIN_INTERVAL_NANOS = 66_000_000L

/** Lite tier uses half of the requested blur radius. */
private const val LITE_BLUR_FACTOR = 0.5f

private tailrec fun Context.findHostWindow(): Window? = when (this) {
    is Activity -> window
    is ContextWrapper -> baseContext.findHostWindow()
    else -> null
}

private const val TAG = "GlassDialog"

/** A copy that takes longer than this is abandoned so the capture loop can never stall. */
private const val SNAPSHOT_TIMEOUT_MS = 500L

private const val COPY_EXCEPTION = -1
private const val COPY_TIMEOUT = -2

/**
 * Copies [window] (scaled to the size of [target]) into [target].
 * Returns a [PixelCopy] result code, [COPY_EXCEPTION] or [COPY_TIMEOUT].
 */
private suspend fun copyWindowInto(window: Window, target: Bitmap, handler: Handler): Int =
    withTimeoutOrNull(SNAPSHOT_TIMEOUT_MS) {
        suspendCancellableCoroutine<Int> { continuation ->
            try {
                PixelCopy.request(
                    window,
                    target,
                    PixelCopy.OnPixelCopyFinishedListener { result ->
                        if (continuation.isActive) continuation.resume(result)
                    },
                    handler
                )
            } catch (_: Exception) {
                if (continuation.isActive) continuation.resume(COPY_EXCEPTION)
            }
        }
    } ?: COPY_TIMEOUT

/**
 * Keeps [sampler] supplied with fresh copies of the launcher [window] until cancelled.
 * Two bitmaps alternate so the one being drawn is never the one being written.
 */
private suspend fun captureWindowSnapshots(
    window: Window,
    sampler: DialogWindowBackdrop,
    downscale: Int,
    minIntervalNanos: Long
) {
    val handler = Handler(Looper.getMainLooper())
    val location = IntArray(2)
    var shown: Bitmap? = null
    var spare: Bitmap? = null
    var lastStartNanos = 0L
    var lastFailure = PixelCopy.SUCCESS
    while (true) {
        val now = withFrameNanos { it }
        if (now - lastStartNanos < minIntervalNanos) continue
        val decor = window.peekDecorView() ?: continue
        val width = decor.width
        val height = decor.height
        if (width <= 0 || height <= 0 || !decor.isAttachedToWindow) continue

        val bitmapWidth = (width / downscale).coerceAtLeast(1)
        val bitmapHeight = (height / downscale).coerceAtLeast(1)
        val target = spare?.takeIf { it.width == bitmapWidth && it.height == bitmapHeight }
            ?: Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)

        lastStartNanos = now
        val result = copyWindowInto(window, target, handler)
        if (result != PixelCopy.SUCCESS) {
            //Log only when the failure code changes, so a persistent failure does not flood logcat
            if (result != lastFailure) Log.w(TAG, "Window snapshot failed, code=$result")
            lastFailure = result
            continue
        }
        lastFailure = PixelCopy.SUCCESS

        decor.getLocationOnScreen(location)
        sampler.publishSnapshot(target.asImageBitmap(), location[0], location[1], width, height)
        spare = shown
        shown = target
    }
}

/**
 * Window-corrected glass sampler for an overlay window (dialog or popup), or null when Liquid Glass
 * is off. While it is in the composition the launcher page counts as having an open overlay, and
 * with a video wallpaper live copies of the launcher window feed the sampler.
 */
@Composable
private fun rememberGlassWindowSampler(): DialogWindowBackdrop? {
    val record = LocalLiquidGlassBackdrop.current
    if (record == null || !liquidGlassEnabled()) return null

    val page = LocalLiquidGlassPage.current
    if (page != null) {
        DisposableEffect(page) {
            page.openDialogs++
            onDispose { page.openDialogs-- }
        }
    }
    val sampler = remember(record, page) {
        DialogWindowBackdrop(if (RECORD_PAGE_BEHIND_DIALOGS) page?.layer ?: record else record, page)
    }

    //Video wallpaper: sample live copies of the launcher window instead of the shared layer
    val background = LocalBackgroundViewModel.current
    val videoBehindDialog = background != null && background.isValid && background.isVideo
    val hostWindow = LocalContext.current.findHostWindow()
    val lite = liquidGlassTier() == GlassTier.LITE
    if (videoBehindDialog && hostWindow != null) {
        LaunchedEffect(sampler, hostWindow, lite) {
            captureWindowSnapshots(
                window = hostWindow,
                sampler = sampler,
                downscale = if (lite) LITE_SNAPSHOT_DOWNSCALE else SNAPSHOT_DOWNSCALE,
                minIntervalNanos = if (lite) LITE_SNAPSHOT_MIN_INTERVAL_NANOS else SNAPSHOT_MIN_INTERVAL_NANOS
            )
        }
    }
    return sampler
}

/**
 * Drop-in for [Dialog]. With Liquid Glass on, glass inside the dialog samples the launcher page
 * behind the dialog at the correct position instead of the top-left of the wallpaper.
 */
@Composable
fun GlassDialog(
    onDismissRequest: () -> Unit,
    properties: DialogProperties = DialogProperties(),
    content: @Composable () -> Unit
) {
    val sampler = rememberGlassWindowSampler()
    if (sampler == null) {
        Dialog(onDismissRequest = onDismissRequest, properties = properties, content = content)
        return
    }

    Dialog(onDismissRequest = onDismissRequest, properties = properties) {
        //Match the scrim the system draws behind this dialog (the glass itself is not dimmed by it)
        val dialogWindow = (LocalView.current.parent as? DialogWindowProvider)?.window
        val dim = remember(dialogWindow) {
            dialogWindow?.attributes?.let { attributes ->
                if (attributes.flags and WindowManager.LayoutParams.FLAG_DIM_BEHIND != 0) {
                    attributes.dimAmount
                } else {
                    0f
                }
            } ?: 0f
        }
        SideEffect { sampler.dimAlpha = dim }

        CompositionLocalProvider(LocalLiquidGlassSampler provides sampler) {
            content()
        }
    }
}

/** 容器色叠加在玻璃上的不透明度，保证文字可读 */
private const val GLASS_SURFACE_ALPHA = 0.3f

/**
 * 当前可用的 Liquid Glass 背景图层；为 null 表示应使用普通样式
 */
@Composable
fun liquidGlassBackdrop(): PrismalBackdrop? {
    if (!liquidGlassEnabled()) return null
    return LocalLiquidGlassSampler.current ?: LocalLiquidGlassBackdrop.current
}

/**
 * 统一的 Liquid Glass 表面，所有使用该效果的组件都应通过它绘制
 * @param containerColor 叠加在玻璃上的容器色（会被降低不透明度）
 * @param contentColor 内容颜色
 * @param onClick 非空时启用按压动画与点击
 */
@Composable
fun LiquidGlassSurface(
    backdrop: PrismalBackdrop,
    shape: Shape,
    modifier: Modifier = Modifier,
    containerColor: Color = Color.Unspecified,
    contentColor: Color = LocalContentColor.current,
    blurRadius: Dp = 8.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    // PrismalGlassSurface builds its press ripple with remember(onClick, scope), but the
    // ripple's pointer input is keyed on the scope only and keeps driving the instance
    // created first. If onClick changes identity (any lambda capturing toggled state),
    // drawing reads a new, never-animated ripple and the spring press disappears until
    // the screen is recreated. Hand Prismal a callback whose identity never changes and
    // forward to the latest onClick, so a single ripple lives as long as the surface.
    val latestOnClick = rememberUpdatedState(onClick)
    val hasClick = onClick != null
    val stableOnClick: (() -> Unit)? = remember(hasClick) {
        if (hasClick) {
            val callback: () -> Unit = { latestOnClick.value?.invoke() }
            callback
        } else {
            null
        }
    }

    PrismalGlassSurface(
        backdrop = backdrop,
        modifier = modifier,
        shape = { shape },
        onClick = stableOnClick,
        blurRadius = if (liquidGlassTier() == GlassTier.LITE) blurRadius * LITE_BLUR_FACTOR else blurRadius,
        surfaceColor = if (containerColor.isSpecified) {
            containerColor.copy(alpha = GLASS_SURFACE_ALPHA)
        } else {
            Color.Unspecified
        }
    ) {
        //Inside a dialog window the glass must redraw for every new backdrop snapshot. Reading the
        //counter in a draw modifier of a child invalidates this glass' layer, as an animated child does.
        val liveBackdrop = backdrop as? DialogWindowBackdrop
        if (liveBackdrop != null) {
            Box(modifier = Modifier.matchParentSize().drawBehind { liveBackdrop.readFrame() })
        }
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            content()
        }
    }
}

/** Holds the release animation that is scheduled but may not have started yet. */
private class PendingRelease {
    var job: Job? = null
}

private val GlassPressSpring = spring<Float>(dampingRatio = 0.48f, stiffness = 420f)

/** Springy entrance for floating glass panels (menus, message dialogs): low damping gives the overshoot. */
internal val GlassPopSpring = spring<Float>(dampingRatio = 0.55f, stiffness = 380f)

/** Applies the pop animation [progress] (0 hidden .. 1 shown, may overshoot) to a floating glass panel. */
internal fun Modifier.glassPopTransform(
    progress: () -> Float,
    transformOrigin: TransformOrigin = TransformOrigin.Center
): Modifier = graphicsLayer {
    val p = progress()
    val scale = 0.8f + 0.2f * p
    scaleX = scale
    scaleY = scale
    alpha = p.coerceIn(0f, 1f)
    this.transformOrigin = transformOrigin
}

/** Pops a glass panel in with a springy scale and fade when it enters the composition. */
@Composable
fun Modifier.glassPopIn(transformOrigin: TransformOrigin = TransformOrigin.Center): Modifier {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) { progress.animateTo(1f, GlassPopSpring) }
    return glassPopTransform({ progress.value }, transformOrigin)
}

/** Minimum time a press stays visible so very quick taps still show the spring. */
private const val MIN_PRESS_HOLD_MS = 90L

/**
 * Reuse PrismalAGSL's native spring press response on interactive content that is
 * not itself a PrismalGlassSurface (for example, rows inside a glass settings card).
 * The spring/ripple/offset all come from PrismalPressRipple, matching glass surfaces.
 */
@Composable
fun Modifier.liquidGlassPressAnimation(enabled: Boolean = true): Modifier {
    if (!enabled) return this
    val press = rememberGlassPress()
    return this.then(press.visual).then(press.gesture)
}

/**
 * Press animation split in two so the touch area and the animated visual can differ:
 * [gesture] drives the spring from pointer input, [visual] applies scale and highlight.
 */
class GlassPress internal constructor(
    val gesture: Modifier,
    val visual: Modifier
)

@Composable
fun rememberGlassPress(shape: Shape? = null): GlassPress {
    val animationScope = rememberCoroutineScope()
    val pressProgress = remember { Animatable(0f) }
    val pendingRelease = remember { PendingRelease() }

    // Observe pointer events in the Initial pass so Material clickables and switches
    // can consume their own events without cancelling the glass press animation.
    val pressGesture = Modifier.pointerInput(animationScope) {
        awaitEachGesture {
            val down = awaitFirstDown(
                requireUnconsumed = false,
                pass = PointerEventPass.Initial
            )

            // A new press owns the animation: drop any release still waiting from a previous
            // tap (otherwise it would fire later and snap this press back to 0), then spring
            // towards 1 from wherever the previous animation currently is.
            pendingRelease.job?.cancel()
            pendingRelease.job = null
            animationScope.launch {
                pressProgress.animateTo(1f, GlassPressSpring)
            }

            var lastEventUptime = down.uptimeMillis
            try {
                var pressed = true
                while (pressed) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val change = event.changes.firstOrNull { it.id == down.id }
                    if (change != null) lastEventUptime = change.uptimeMillis
                    pressed = change?.pressed == true
                }
            } finally {
                // Keep a very short tap pressed long enough to actually be seen, then spring
                // back. Never cancel the press animation here: Animatable interrupts it
                // itself and continues from the current visual value.
                val remainingHold = (MIN_PRESS_HOLD_MS - (lastEventUptime - down.uptimeMillis))
                    .coerceAtLeast(0L)
                pendingRelease.job = animationScope.launch {
                    if (remainingHold > 0L) delay(remainingHold)
                    pressProgress.animateTo(0f, GlassPressSpring)
                }
            }
        }
    }

    val visual = Modifier
        .graphicsLayer {
            val progress = pressProgress.value
            val pressLiftPx = 4.dp.toPx()
            val lift = if (size.height > 0f) pressLiftPx / size.height else 0f
            scaleX = 1f + lift * progress
            scaleY = 1f + lift * progress
        }
        // Keep the highlight inside rounded glass (otherwise it shows as a square)
        .then(if (shape != null) Modifier.clip(shape) else Modifier)
        .drawWithContent {
            val progress = pressProgress.value
            if (progress > 0f) {
                drawRect(Color.White.copy(alpha = 0.12f * progress))
            }
            drawContent()
        }
    return GlassPress(gesture = pressGesture, visual = visual)
}
/**
 * A clickable list item / button surface: its own Liquid Glass box when glass is active,
 * otherwise a regular Material [Surface] with the given colors.
 */
@Composable
fun GlassItemSurface(
    shape: Shape,
    color: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val backdrop = liquidGlassBackdrop()
    if (backdrop != null) {
        LiquidGlassSurface(
            backdrop = backdrop,
            shape = shape,
            modifier = modifier,
            containerColor = color,
            contentColor = Color.White,
            onClick = onClick
        ) {
            content()
        }
    } else if (onClick != null) {
        Surface(
            modifier = modifier,
            shape = shape,
            color = color,
            contentColor = contentColor,
            onClick = onClick,
            content = content
        )
    } else {
        Surface(
            modifier = modifier,
            shape = shape,
            color = color,
            contentColor = contentColor,
            content = content
        )
    }
}

/**
 * Backdrop for screens that have no launcher background (the in-game UI). Records a flat dark
 * surface into the shared glass layer, so glass boxes still render blur, edge highlights and the
 * springy press. It is drawn at (almost) zero alpha, so it never visibly covers the game.
 */
@Composable
fun GameGlassBackdrop(modifier: Modifier = Modifier) {
    val layer = LocalLiquidGlassBackdrop.current ?: return
    Box(
        modifier = modifier
            .graphicsLayer { alpha = 0.004f }
            .prismalGlassLayer(layer)
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(Color(0xFF3A3F47))
        )
    }
}

/**
 * 条形区域（如顶部栏）：开启 Liquid Glass 时绘制玻璃背景，否则只是普通容器
 */
@Composable
fun LiquidGlassBar(
    shape: Shape,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val backdrop = liquidGlassBackdrop()
    if (backdrop == null) {
        Box(modifier = modifier) { content() }
    } else {
        LiquidGlassSurface(
            backdrop = backdrop,
            shape = shape,
            modifier = modifier,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = LocalContentColor.current,
            blurRadius = 12.dp
        ) {
            content()
        }
    }
}


/** Places a menu popup under its anchor, flipping above or clamping when it would leave the window. */
private class GlassMenuPositionProvider(private val offset: IntOffset) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        val maxX = (windowSize.width - popupContentSize.width).coerceAtLeast(0)
        val maxY = (windowSize.height - popupContentSize.height).coerceAtLeast(0)
        val x = if (layoutDirection == LayoutDirection.Ltr) {
            anchorBounds.left + offset.x
        } else {
            anchorBounds.right - popupContentSize.width - offset.x
        }
        val below = anchorBounds.bottom + offset.y
        val above = anchorBounds.top - popupContentSize.height - offset.y
        val y = when {
            below + popupContentSize.height <= windowSize.height -> below
            above >= 0 -> above
            else -> maxY
        }
        return IntOffset(x.coerceIn(0, maxX), y.coerceIn(0, maxY))
    }
}

/**
 * Drop-in for Material3 DropdownMenu. With Liquid Glass on, the menu is its own popup window drawn
 * as a glass panel (same tint and blur as dialogs) that samples the launcher page behind it with the
 * window-corrected sampler. Material3's menu is not reused here: its opaque container would sit
 * behind the glass and show up as a white or blurred patch. Items must come from [DropdownMenuItem]
 * so their text is white.
 */
@Composable
fun DropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset(0.dp, 0.dp),
    shape: Shape = MaterialTheme.shapes.extraSmall,
    shadowElevation: Dp = 3.dp,
    properties: PopupProperties = PopupProperties(focusable = true),
    matchAnchorWidth: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val glass = LocalLiquidGlassBackdrop.current != null && liquidGlassEnabled()
    if (!glass) {
        androidx.compose.material3.DropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismissRequest,
            modifier = modifier,
            offset = offset,
            shape = shape,
            shadowElevation = shadowElevation,
            properties = properties,
            content = content
        )
        return
    }
    //Keep the popup composed while it springs out, then remove it
    val progress = remember { Animatable(0f) }
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(expanded) {
        if (expanded) {
            shown = true
            progress.animateTo(1f, GlassPopSpring)
        } else if (shown) {
            progress.animateTo(0f, tween(durationMillis = 110))
            shown = false
        }
    }
    if (!shown) return
    GlassDropdownPopup(onDismissRequest, modifier, offset, shape, properties, matchAnchorWidth, { progress.value }, content)
}

@Composable
private fun GlassDropdownPopup(
    onDismissRequest: () -> Unit,
    modifier: Modifier,
    offset: DpOffset,
    shape: Shape,
    properties: PopupProperties,
    matchAnchorWidth: Boolean,
    progress: () -> Float,
    content: @Composable ColumnScope.() -> Unit
) {
    val sampler = rememberGlassWindowSampler() ?: return
    val density = LocalDensity.current

    //The popup is anchored to its parent layout; read that width for menus that match their field
    var anchorWidthPx by remember { mutableIntStateOf(0) }
    if (matchAnchorWidth) {
        Layout(
            content = {},
            modifier = Modifier.onGloballyPositioned { anchorWidthPx = it.parentLayoutCoordinates?.size?.width ?: 0 }
        ) { _, _ -> layout(0, 0) {} }
    }
    val widthModifier = if (matchAnchorWidth && anchorWidthPx > 0) {
        Modifier.width(with(density) { anchorWidthPx.toDp() })
    } else {
        Modifier.sizeIn(minWidth = 112.dp, maxWidth = 280.dp)
    }
    val layoutDirection = LocalLayoutDirection.current
    val positionProvider = remember(offset, density, layoutDirection) {
        with(density) {
            GlassMenuPositionProvider(IntOffset(offset.x.roundToPx(), offset.y.roundToPx()))
        }
    }

    Popup(
        popupPositionProvider = positionProvider,
        onDismissRequest = onDismissRequest,
        properties = properties
    ) {
        CompositionLocalProvider(LocalLiquidGlassSampler provides sampler) {
            LiquidGlassSurface(
                backdrop = sampler,
                shape = shape,
                modifier = Modifier
                    .glassPopTransform(progress, TransformOrigin(0f, 0f))
                    .then(modifier)
                    .then(widthModifier),
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = Color.White
            ) {
                Column(
                    modifier = (if (matchAnchorWidth) Modifier.fillMaxWidth() else Modifier.width(IntrinsicSize.Max))
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 8.dp),
                    content = content
                )
            }
        }
    }
}
