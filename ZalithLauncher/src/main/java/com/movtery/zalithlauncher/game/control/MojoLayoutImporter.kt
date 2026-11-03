/* SPDX-License-Identifier: GPL-3.0-or-later */

package com.movtery.zalithlauncher.game.control

import android.content.Context
import androidx.compose.ui.graphics.Color
import com.movtery.inputmap.keycodes.ControlEventKeycode
import com.movtery.layer_controller.data.*
import com.movtery.layer_controller.data.lang.createTranslatable
import com.movtery.layer_controller.event.ClickEvent
import com.movtery.layer_controller.layout.ControlLayer
import com.movtery.layer_controller.layout.ControlLayout
import com.movtery.zalithlauncher.ui.control.event.LAUNCHER_EVENT_SWITCH_IME
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.UUID
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Converts MojoLauncher v3 control JSON files to Ray's native layout model. */
internal object MojoLayoutImporter {
    private const val MAX_JSON_BYTES = 16 * 1024 * 1024
    private const val MAX_MJ_VERSION = 9
    private const val PREFERRED_SCALE = 100f
    private const val MJ_SPECIAL_KEYBOARD = -1

    data class Payload(val json: String)
    private data class Point(val x: Float, val y: Float)
    private class State(
        val screenW: Float,
        val screenH: Float,
        val density: Float,
        /** MJ `scaledAt`: button scale the layout was saved with. */
        val layoutScale: Float
    ) {
        val styles = mutableListOf<ButtonStyle>()
        val joystickStyles = mutableListOf<JoystickStyle>()
        var buttons = 0
        var joysticks = 0
        var drawers = 0
        var skippedActions = 0
        var bitmapControls = 0
        /** MJ margin is dpToPx(2). */
        val margin get() = 2f * density

        /** MJ rescales saved sizes: size / scaledAt * buttonscale (default buttonscale is 100). */
        fun scaled(value: Float) = value / layoutScale * PREFERRED_SCALE
        fun strokeScaled(value: Float) = value * (layoutScale / 100f)
    }

    fun readPayload(input: InputStream): Payload {
        val bytes = input.readLimited(MAX_JSON_BYTES)
        return Payload(bytes.toString(Charsets.UTF_8))
    }

    fun isMojo(json: JSONObject) =
        json.has("mControlDataList") || json.has("mJoystickDataList") || json.has("mDrawerDataList")

    fun convert(context: Context, root: JSONObject): ControlLayout {
        val version = root.optInt("version", 9)
        require(version in 3..MAX_MJ_VERSION) {
            "Unsupported MJ layout version $version. Export the layout again with a current MojoLauncher version."
        }
        val metrics = context.resources.displayMetrics
        val state = State(
            max(metrics.widthPixels, metrics.heightPixels).toFloat().coerceAtLeast(1f),
            min(metrics.widthPixels, metrics.heightPixels).toFloat().coerceAtLeast(1f),
            metrics.density.coerceAtLeast(0.1f),
            root.optDouble("scaledAt", 100.0).toFloat().takeIf { it.isFinite() && it > 0f } ?: 100f
        )
        val mainButtons = mutableListOf<NormalData>()
        val mainJoysticks = mutableListOf<JoystickData>()
        root.optJSONArray("mControlDataList").each { mainButtons += state.button(it) }
        root.optJSONArray("mJoystickDataList").each { mainJoysticks += state.joystick(it) }

        val layers = mutableListOf<ControlLayer>()
        root.optJSONArray("mDrawerDataList").eachIndexed { index, drawer ->
            val properties = drawer.optJSONObject("properties") ?: return@eachIndexed
            val children = drawer.optJSONArray("buttonProperties") ?: JSONArray()
            val layerId = id()
            val w = state.scaled(properties.optDouble("width", 50.0).toFloat()).coerceAtLeast(5f)
            val h = state.scaled(properties.optDouble("height", 50.0).toFloat()).coerceAtLeast(5f)
            val wp = w * state.density
            val hp = h * state.density
            val anchor = state.position(properties, wp, hp)
            // MJ recomputes child positions and sizes for every non-FREE drawer at runtime:
            // child = drawer position + (drawer size + margin) * (i + 1), child size = drawer size.
            // Stored child coordinates are stale there, so they are only used for FREE drawers.
            val orientation = drawer.orientation()
            val childButtons = mutableListOf<NormalData>()
            for (i in 0 until children.length()) {
                val child = children.optJSONObject(i) ?: continue
                val step = i + 1
                val aligned = when (orientation) {
                    "RIGHT" -> Point(anchor.x + (wp + state.margin) * step, anchor.y)
                    "LEFT" -> Point(anchor.x - (wp + state.margin) * step, anchor.y)
                    "UP" -> Point(anchor.x, anchor.y - (hp + state.margin) * step)
                    "DOWN" -> Point(anchor.x, anchor.y + (hp + state.margin) * step)
                    else -> null
                }
                childButtons += if (aligned != null) {
                    state.button(child, aligned, sizeDp = w to h)
                } else {
                    state.button(child)
                }
            }
            layers += ControlLayer(
                name = "MJ ${properties.optString("name", "Drawer")} ${index + 1}",
                uuid = layerId,
                hide = true,
                visibilityType = VisibilityType.ALWAYS,
                normalButtons = childButtons
            )
            mainButtons += state.button(
                properties,
                extra = listOf(ClickEvent(ClickEvent.Type.SwitchLayer, layerId))
            )
            state.drawers++
        }

        layers += ControlLayer(
                name = "MojoLauncher",
                uuid = id(),
                hide = false,
                visibilityType = VisibilityType.ALWAYS,
                normalButtons = mainButtons,
                joystickButtons = mainJoysticks
            )

        val warnings = buildList {
            if (state.skippedActions > 0) add("${state.skippedActions} unsupported MJ special actions skipped")
            if (state.bitmapControls > 0) add("MJ bitmap assets are not imported")
        }
        val description = buildString {
            append("Imported from MojoLauncher: ${state.buttons} buttons, ${state.joysticks} joysticks, ${state.drawers} drawers.")
            if (warnings.isNotEmpty()) append(" Limitations: ${warnings.joinToString("; ")}.")
        }
        val sourceInfo = root.optJSONObject("mControlInfoDataList")
        return ControlLayout(
            info = ControlLayout.Info(
                name = createTranslatable(sourceInfo?.optString("name", "MojoLauncher Layout") ?: "MojoLauncher Layout"),
                author = createTranslatable(sourceInfo?.optString("author", "MojoLauncher") ?: "MojoLauncher"),
                description = createTranslatable(
                    sourceInfo?.optString("desc", description)?.takeUnless { it == "null" || it.isBlank() } ?: description
                ),
                versionCode = version,
                versionName = sourceInfo?.optString("version", "MJ v$version") ?: "MJ v$version"
            ),
            layers = layers,
            styles = state.styles,
            joystickStyles = state.joystickStyles,
            editorVersion = 12
        )
    }

    private fun State.button(
        src: JSONObject,
        overridePosition: Point? = null,
        extra: List<ClickEvent> = emptyList(),
        sizeDp: Pair<Float, Float>? = null
    ): NormalData {
        val name = src.optString("name", "Button").ifBlank { "Button" }
        val w = sizeDp?.first ?: scaled(src.optDouble("width", 50.0).toFloat()).coerceAtLeast(5f)
        val h = sizeDp?.second ?: scaled(src.optDouble("height", 50.0).toFloat()).coerceAtLeast(5f)
        val wp = w * density
        val hp = h * density
        val p = toPosition(overridePosition ?: position(src, wp, hp), wp, hp)
        val events = mutableListOf<ClickEvent>()
        src.optJSONArray("keycodes").eachInt { code ->
            if (code == 0) return@eachInt // unused MJ key slot
            if (code == MJ_SPECIAL_KEYBOARD) {
                // MJ on-screen keyboard button -> launcher "switch input method" event
                val ime = ClickEvent(ClickEvent.Type.LauncherEvent, LAUNCHER_EVENT_SWITCH_IME)
                if (ime !in events) events += ime
                return@eachInt
            }
            val key = key(code)
            if (key == null) skippedActions++ else events += ClickEvent(ClickEvent.Type.Key, key)
        }
        events += extra
        val style = buttonStyle(
            name, w, h,
            src.optDouble("opacity", 1.0).toFloat(),
            src.optInt("bgColor", 0x4D000000),
            src.optInt("strokeColor", -1),
            strokeScaled(src.optDouble("strokeWidth", 0.0).toFloat()),
            src.optDouble("cornerRadius", 0.0).toFloat()
        )
        styles += style
        buttons++
        if (src.optString("bitmapTag", "").let { it.isNotBlank() && it != "null" }) bitmapControls++
        return NormalData(
            text = createTranslatable(name),
            uuid = id(),
            position = p,
            buttonSize = ButtonSize(
                type = ButtonSize.Type.Dp,
                widthDp = w,
                heightDp = h,
                widthPercentage = 100,
                heightPercentage = 100,
                widthReference = ButtonSize.Reference.ScreenWidth,
                heightReference = ButtonSize.Reference.ScreenHeight
            ),
            buttonStyle = style.uuid,
            visibilityType = visibility(src),
            _clickEvents = events,
            isSwipple = src.optBoolean("isSwipeable", false),
            isPenetrable = src.optBoolean("passThruEnabled", false),
            isToggleable = src.optBoolean("isToggle", false)
        )
    }

    private fun State.joystick(src: JSONObject): JoystickData {
        val w = scaled(src.optDouble("width", 100.0).toFloat()).coerceAtLeast(20f)
        val h = scaled(src.optDouble("height", src.optDouble("width", 100.0)).toFloat()).coerceAtLeast(20f)
        val size = max(w, h)
        val sizePx = size * density
        val styleId = id()
        val alpha = src.optDouble("opacity", 1.0).toFloat().coerceIn(0f, 1f)
        val stroke = (strokeScaled(src.optDouble("strokeWidth", 0.0).toFloat()) / size * 100f)
            .roundToInt().coerceIn(0, 50)
        val config = JoystickStyle.StyleConfig(
            alpha = alpha,
            backgroundColor = Color(src.optInt("bgColor", 0x4D000000)),
            joystickColor = Color.White,
            joystickCanLockColor = Color.Yellow,
            joystickLockedColor = Color.Green,
            lockMarkColor = Color.White,
            borderWidthRatio = stroke,
            borderColor = Color(src.optInt("strokeColor", -1)),
            backgroundShape = src.optDouble("cornerRadius", 50.0).toInt().coerceIn(0, 50),
            joystickShape = 50,
            joystickSize = 0.5f
        )
        joystickStyles += JoystickStyle("MJ Joystick", styleId, true, config, config)
        joysticks++
        return JoystickData(
            uuid = id(),
            position = toPosition(position(src, sizePx, sizePx), sizePx, sizePx),
            sizeType = ButtonSize.Type.Dp,
            sizeDp = size,
            sizePercentage = 2500,
            visibilityType = visibility(src),
            joystickStyleId = styleId,
            deadZoneRatio = 0.35f,
            canLock = src.optBoolean("forwardLock", false),
            triggerMode = if (src.optBoolean("absolute", false)) JoystickTriggerMode.TOUCH else JoystickTriggerMode.DRAG,
            directionEvents = DefaultDirectionEvents,
            lockEvents = DefaultLockEvents
        )
    }

    private fun State.buttonStyle(
        name: String, w: Float, h: Float, opacity: Float,
        bg: Int, border: Int, borderWidth: Float, radiusPercent: Float
    ): ButtonStyle {
        val alpha = opacity.coerceIn(0f, 1f)
        val radius = (min(w, h) * radiusPercent.coerceIn(0f, 100f) / 100f).coerceIn(0f, 100f)
        val background = Color(bg)
        val borderColor = Color(border)
        val width = borderWidth.coerceIn(0f, 50f).roundToInt()
        val shape = ButtonShape(radius)
        val config = ButtonStyle.StyleConfig(
            alpha, alpha, background, background, Color.White, Color.White,
            null, null, width, width, borderColor, borderColor, shape, shape
        )
        return ButtonStyle("MJ: $name", id(), false, true, config, config)
    }

    /** MJ serializes the drawer orientation as an enum name; older exports may use its int index. */
    private fun JSONObject.orientation(): String = when (val raw = opt("orientation")) {
        is Number -> listOf("DOWN", "LEFT", "UP", "RIGHT", "FREE").getOrElse(raw.toInt()) { "FREE" }
        is String -> raw.uppercase()
        else -> "FREE"
    }

    private fun visibility(src: JSONObject): VisibilityType {
        val game = src.optBoolean("displayInGame", true)
        val menu = src.optBoolean("displayInMenu", true)
        return when {
            game && menu -> VisibilityType.ALWAYS
            game -> VisibilityType.IN_GAME
            menu -> VisibilityType.IN_MENU
            else -> VisibilityType.ALWAYS
        }
    }

    private fun State.position(src: JSONObject, w: Float, h: Float): Point {
        val vars = mapOf(
            "top" to 0f, "left" to 0f, "right" to screenW - w, "bottom" to screenH - h,
            "width" to w, "height" to h, "screen_width" to screenW, "screen_height" to screenH,
            "margin" to margin, "preferred_scale" to PREFERRED_SCALE
        )
        val x = eval(src.optString("dynamicX", ""), vars)
            ?: src.optDouble("x", ((screenW - w) / 2f).toDouble()).toFloat()
        val y = eval(src.optString("dynamicY", ""), vars)
            ?: src.optDouble("y", ((screenH - h) / 2f).toDouble()).toFloat()
        return Point(x.coerceIn(0f, max(0f, screenW - w)), y.coerceIn(0f, max(0f, screenH - h)))
    }

    private fun State.eval(expression: String, vars: Map<String, Float>): Float? {
        if (expression.isBlank()) return null

        // Expand MJ variable placeholders manually to avoid regex-engine incompatibilities.
        val expanded = StringBuilder(expression.length + 32)
        var index = 0
        while (index < expression.length) {
            if (expression[index].code == 36 && expression.getOrNull(index + 1) == '{') {
                val end = expression.indexOf('}', index + 2)
                if (end < 0) return null
                val name = expression.substring(index + 2, end)
                val variable = vars[name] ?: return null
                expanded.append(variable)
                index = end + 1
            } else {
                expanded.append(expression[index])
                index++
            }
        }

        return runCatching { MathExpr(expanded.toString(), density).parse().toFloat() }
            .getOrNull()
            ?.takeIf { it.isFinite() }
    }
    private fun State.toPosition(p: Point, w: Float, h: Float) = ButtonPosition(
        (p.x / max(1f, screenW - w) * 10000f).roundToInt().coerceIn(0, 10000),
        (p.y / max(1f, screenH - h) * 10000f).roundToInt().coerceIn(0, 10000)
    )

    private fun key(code: Int): String? {
        val name = when (code) {
            -3 -> "GLFW_MOUSE_BUTTON_LEFT"
            -4 -> "GLFW_MOUSE_BUTTON_RIGHT"
            -6 -> "GLFW_MOUSE_BUTTON_MIDDLE"
            in Int.MIN_VALUE..0 -> return null // MJ uses zero as an unused key slot.
            in 48..57, in 65..90 -> "GLFW_KEY_" + code.toChar()
            in 290..314 -> "GLFW_KEY_F" + (code - 289)
            else -> {
                val key = when (code) {
                    32 -> "SPACE"
                    39 -> "APOSTROPHE"
                    44 -> "COMMA"
                    45 -> "MINUS"
                    46 -> "PERIOD"
                    47 -> "SLASH"
                    59 -> "SEMICOLON"
                    61 -> "EQUAL"
                    91 -> "LEFT_BRACKET"
                    92 -> "BACKSLASH"
                    93 -> "RIGHT_BRACKET"
                    96 -> "GRAVE_ACCENT"
                    256 -> "ESCAPE"
                    257 -> "ENTER"
                    258 -> "TAB"
                    259 -> "BACKSPACE"
                    260 -> "INSERT"
                    261 -> "DELETE"
                    262 -> "RIGHT"
                    263 -> "LEFT"
                    264 -> "DOWN"
                    265 -> "UP"
                    266 -> "PAGE_UP"
                    267 -> "PAGE_DOWN"
                    268 -> "HOME"
                    269 -> "END"
                    280 -> "CAPS_LOCK"
                    281 -> "SCROLL_LOCK"
                    282 -> "NUM_LOCK"
                    283 -> "PRINT_SCREEN"
                    284 -> "PAUSE"
                    320 -> "KP_0"
                    321 -> "KP_1"
                    322 -> "KP_2"
                    323 -> "KP_3"
                    324 -> "KP_4"
                    325 -> "KP_5"
                    326 -> "KP_6"
                    327 -> "KP_7"
                    328 -> "KP_8"
                    329 -> "KP_9"
                    330 -> "KP_DECIMAL"
                    331 -> "KP_DIVIDE"
                    332 -> "KP_MULTIPLY"
                    333 -> "KP_SUBTRACT"
                    334 -> "KP_ADD"
                    335 -> "KP_ENTER"
                    336 -> "KP_EQUAL"
                    340 -> "LEFT_SHIFT"
                    341 -> "LEFT_CONTROL"
                    342 -> "LEFT_ALT"
                    343 -> "LEFT_SUPER"
                    344 -> "RIGHT_SHIFT"
                    345 -> "RIGHT_CONTROL"
                    346 -> "RIGHT_ALT"
                    347 -> "RIGHT_SUPER"
                    348 -> "MENU"
                    else -> return null
                }
                "GLFW_KEY_" + key
            }
        }
        return name.takeIf { ControlEventKeycode.getKeycodeFromEvent(it) != null }
    }

    private fun JSONArray?.eachInt(block: (Int) -> Unit) {
        if (this == null) return
        for (i in 0 until length()) block(optInt(i, 0))
    }

    private fun JSONArray?.each(block: (JSONObject) -> Unit) {
        if (this == null) return
        for (i in 0 until length()) optJSONObject(i)?.let(block)
    }

    private fun JSONArray?.eachIndexed(block: (Int, JSONObject) -> Unit) {
        if (this == null) return
        for (i in 0 until length()) optJSONObject(i)?.let { block(i, it) }
    }

    private fun id() = UUID.randomUUID().toString().replace("-", "").take(18)

    private fun InputStream.readLimited(limit: Int): ByteArray {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        var total = 0
        while (true) {
            val count = read(buffer)
            if (count < 0) break
            total += count
            if (total > limit) throw IOException("MJ control layout exceeds the import size limit")
            out.write(buffer, 0, count)
        }
        return out.toByteArray()
    }

    private class MathExpr(private val s: String, private val density: Float) {
        private var i = 0

        fun parse(): Double {
            val result = expression()
            spaces()
            require(i == s.length && result.isFinite())
            return result
        }

        private fun expression(): Double {
            var value = term()
            while (true) {
                spaces()
                value = when (peek()) {
                    '+' -> { i++; value + term() }
                    '-' -> { i++; value - term() }
                    else -> return value
                }
            }
        }

        private fun term(): Double {
            var value = factor()
            while (true) {
                spaces()
                value = when (peek()) {
                    '*' -> { i++; value * factor() }
                    '/' -> { i++; val divisor = factor(); require(divisor != 0.0); value / divisor }
                    else -> return value
                }
            }
        }

        private fun factor(): Double {
            spaces()
            return when {
                peek() == '+' -> { i++; factor() }
                peek() == '-' -> { i++; -factor() }
                peek() == '(' -> {
                    i++
                    val value = expression()
                    spaces()
                    require(peek() == ')')
                    i++
                    value
                }
                peek()?.isLetter() == true -> function()
                else -> number()
            }
        }

        private fun function(): Double {
            val start = i
            while (peek()?.isLetter() == true) i++
            val name = s.substring(start, i)
            spaces()
            require(peek() == '(')
            i++
            val argument = expression()
            spaces()
            require(peek() == ')')
            i++
            return when (name) {
                "dp" -> argument / density
                "px" -> argument * density
                else -> throw IllegalArgumentException("Unsupported MJ position function: $name")
            }
        }

        private fun number(): Double {
            spaces()
            val start = i
            while (peek()?.isDigit() == true || peek() == '.') i++
            require(i > start)
            if (peek() == 'e' || peek() == 'E') {
                i++
                if (peek() == '+' || peek() == '-') i++
                val exponentStart = i
                while (peek()?.isDigit() == true) i++
                require(i > exponentStart)
            }
            return s.substring(start, i).toDouble()
        }

        private fun peek() = s.getOrNull(i)
        private fun spaces() { while (i < s.length && s[i].isWhitespace()) i++ }
    }
}
