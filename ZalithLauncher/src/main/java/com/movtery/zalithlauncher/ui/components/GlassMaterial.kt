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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/gpl-3.0.txt>.
 */

package com.movtery.zalithlauncher.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.ButtonColors
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Animatable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.window.PopupProperties
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.ExposedDropdownMenu as M3ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuBoxScope
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CheckboxColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.ui.theme.itemColor

/*
 * Drop-in replacements for the Material3 Button / Surface / Card / IconButton family.
 *
 * Files import these instead of the androidx.compose.material3 versions (except the file
 * manager and the in-game touch controls, which are excluded from Liquid Glass). With Liquid
 * Glass off they just delegate to Material3, so behaviour and visuals are unchanged. With it on:
 *  - Buttons / Surfaces / Cards render as a Liquid Glass box with white content and the
 *    springy press animation;
 *  - IconButtons and text buttons keep their shape but get white content and the springy press.
 * Tint is white rather than the theme's purple; destructive (error) buttons keep their color.
 */

private const val DISABLED_ALPHA = 0.38f

/**
 * Glass tint for a button, given its Material colors. Every button uses the item tint, the same
 * one list items use (see the About page), so Cancel/Close in dialogs match the rows around them
 * instead of looking brighter (white) or darker (plain surface). Error buttons keep their color.
 * [tonal] is kept for callers but no longer changes the tint.
 */
@Composable
@Suppress("UNUSED_PARAMETER")
internal fun glassButtonTint(colors: ButtonColors, enabled: Boolean, tonal: Boolean): Color {
    val container = if (enabled) colors.containerColor else colors.disabledContainerColor
    val error = MaterialTheme.colorScheme.error
    return when {
        container == error || container == MaterialTheme.colorScheme.errorContainer -> container
        else -> itemColor()
    }
}

@Composable
private fun GlassButtonBody(
    backdrop: com.styropyr0.prismal.PrismalBackdrop,
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    shape: Shape,
    tint: Color,
    contentPadding: PaddingValues,
    content: @Composable RowScope.() -> Unit
) {
    LiquidGlassSurface(
        backdrop = backdrop,
        shape = shape,
        modifier = modifier.liquidGlassPressAnimation(enabled),
        containerColor = tint,
        //A disabled button keeps its full glass (blur, tint, edge); only its content is dimmed.
        contentColor = if (enabled) Color.White else Color.White.copy(alpha = 0.5f),
        onClick = if (enabled) onClick else null
    ) {
        ProvideTextStyle(MaterialTheme.typography.labelLarge) {
            Row(
                modifier = Modifier
                    .defaultMinSize(
                        minWidth = ButtonDefaults.MinWidth,
                        minHeight = ButtonDefaults.MinHeight
                    )
                    .padding(contentPadding),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                content = content
            )
        }
    }
}

@Composable
fun Button(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.shape,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    elevation: ButtonElevation? = null,
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit
) {
    val backdrop = liquidGlassBackdrop()
    if (backdrop != null) {
        GlassButtonBody(
            backdrop, onClick, modifier, enabled, shape,
            glassButtonTint(colors, enabled, tonal = false), contentPadding, content
        )
    } else {
        androidx.compose.material3.Button(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            shape = shape,
            colors = colors,
            elevation = elevation,
            border = border,
            contentPadding = contentPadding,
            interactionSource = interactionSource,
            content = content
        )
    }
}

@Composable
fun FilledTonalButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.filledTonalShape,
    colors: ButtonColors = ButtonDefaults.filledTonalButtonColors(),
    elevation: ButtonElevation? = null,
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit
) {
    val backdrop = liquidGlassBackdrop()
    if (backdrop != null) {
        GlassButtonBody(
            backdrop, onClick, modifier, enabled, shape,
            glassButtonTint(colors, enabled, tonal = true), contentPadding, content
        )
    } else {
        androidx.compose.material3.FilledTonalButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            shape = shape,
            colors = colors,
            elevation = elevation,
            border = border,
            contentPadding = contentPadding,
            interactionSource = interactionSource,
            content = content
        )
    }
}

@Composable
fun OutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.outlinedShape,
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(),
    elevation: ButtonElevation? = null,
    border: BorderStroke? = ButtonDefaults.outlinedButtonBorder(enabled),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit
) {
    val backdrop = liquidGlassBackdrop()
    if (backdrop != null) {
        GlassButtonBody(
            backdrop, onClick, modifier, enabled, shape,
            glassButtonTint(colors, enabled, tonal = true), contentPadding, content
        )
    } else {
        androidx.compose.material3.OutlinedButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            shape = shape,
            colors = colors,
            elevation = elevation,
            border = border,
            contentPadding = contentPadding,
            interactionSource = interactionSource,
            content = content
        )
    }
}

@Composable
fun TextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.textShape,
    colors: ButtonColors = ButtonDefaults.textButtonColors(),
    elevation: ButtonElevation? = null,
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.TextButtonContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit
) {
    val glass = liquidGlassBackdrop() != null
    androidx.compose.material3.TextButton(
        onClick = onClick,
        modifier = if (glass) modifier.liquidGlassPressAnimation(enabled) else modifier,
        enabled = enabled,
        shape = shape,
        colors = if (glass) {
            ButtonDefaults.textButtonColors(
                contentColor = Color.White,
                disabledContentColor = Color.White.copy(alpha = DISABLED_ALPHA)
            )
        } else {
            colors
        },
        elevation = elevation,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = content
    )
}

@Composable
fun IconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: IconButtonColors = IconButtonDefaults.iconButtonColors(),
    interactionSource: MutableInteractionSource? = null,
    shape: Shape = CircleShape,
    content: @Composable () -> Unit
) {
    val glass = liquidGlassBackdrop() != null
    androidx.compose.material3.IconButton(
        onClick = onClick,
        modifier = if (glass) modifier.liquidGlassPressAnimation(enabled) else modifier,
        enabled = enabled,
        colors = if (glass) {
            IconButtonDefaults.iconButtonColors(
                containerColor = colors.containerColor,
                contentColor = Color.White,
                disabledContentColor = Color.White.copy(alpha = DISABLED_ALPHA)
            )
        } else {
            colors
        },
        interactionSource = interactionSource,
        shape = shape,
        content = content
    )
}

/**
 * Drop-in for Material3 FloatingActionButton. With Liquid Glass on it is a glass box with the
 * shared item tint (same as every other button) and white content; otherwise it delegates.
 */
@Composable
fun FloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = androidx.compose.material3.FloatingActionButtonDefaults.shape,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = contentColorFor(containerColor),
    content: @Composable () -> Unit
) {
    val backdrop = liquidGlassBackdrop()
    if (backdrop != null) {
        LiquidGlassSurface(
            backdrop = backdrop,
            shape = shape,
            modifier = modifier
                .size(56.dp)
                .liquidGlassPressAnimation(),
            containerColor = itemColor(),
            contentColor = Color.White,
            onClick = onClick
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                content()
            }
        }
    } else {
        androidx.compose.material3.FloatingActionButton(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            containerColor = containerColor,
            contentColor = contentColor,
            content = content
        )
    }
}

/** True when a Surface/Card should become glass: a real, visible, shaped container. */
private fun isGlassCandidate(shape: Shape, color: Color): Boolean =
    shape != RectangleShape && color != Color.Transparent

@Composable
fun Surface(
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    color: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = contentColorFor(color),
    tonalElevation: Dp = 0.dp,
    shadowElevation: Dp = 0.dp,
    border: BorderStroke? = null,
    content: @Composable () -> Unit
) {
    val backdrop = liquidGlassBackdrop()
    if (backdrop != null && isGlassCandidate(shape, color)) {
        LiquidGlassSurface(
            backdrop = backdrop,
            shape = shape,
            modifier = modifier,
            containerColor = color,
            contentColor = Color.White
        ) {
            content()
        }
    } else {
        androidx.compose.material3.Surface(
            modifier = modifier,
            shape = shape,
            color = color,
            contentColor = if (backdrop != null) Color.White else contentColor,
            tonalElevation = tonalElevation,
            shadowElevation = shadowElevation,
            border = border,
            content = content
        )
    }
}

@Composable
fun Surface(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RectangleShape,
    color: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = contentColorFor(color),
    tonalElevation: Dp = 0.dp,
    shadowElevation: Dp = 0.dp,
    border: BorderStroke? = null,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit
) {
    val backdrop = liquidGlassBackdrop()
    if (backdrop != null && isGlassCandidate(shape, color)) {
        LiquidGlassSurface(
            backdrop = backdrop,
            shape = shape,
            modifier = modifier,
            containerColor = color,
            contentColor = if (enabled) Color.White else Color.White.copy(alpha = 0.6f),
            onClick = if (enabled) onClick else null
        ) {
            content()
        }
    } else {
        androidx.compose.material3.Surface(
            onClick = onClick,
            modifier = if (backdrop != null) modifier.liquidGlassPressAnimation(enabled) else modifier,
            enabled = enabled,
            shape = shape,
            color = color,
            contentColor = if (backdrop != null) Color.White else contentColor,
            tonalElevation = tonalElevation,
            shadowElevation = shadowElevation,
            border = border,
            interactionSource = interactionSource,
            content = content
        )
    }
}

@Composable
fun Card(
    modifier: Modifier = Modifier,
    shape: Shape = CardDefaults.shape,
    colors: CardColors = CardDefaults.cardColors(),
    elevation: CardElevation = CardDefaults.cardElevation(),
    border: BorderStroke? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val backdrop = liquidGlassBackdrop()
    if (backdrop != null && isGlassCandidate(shape, colors.containerColor)) {
        LiquidGlassSurface(
            backdrop = backdrop,
            shape = shape,
            modifier = modifier,
            containerColor = colors.containerColor,
            contentColor = Color.White
        ) {
            Column(content = content)
        }
    } else {
        androidx.compose.material3.Card(
            modifier = modifier,
            shape = shape,
            colors = colors,
            elevation = elevation,
            border = border,
            content = content
        )
    }
}

@Composable
fun Card(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = CardDefaults.shape,
    colors: CardColors = CardDefaults.cardColors(),
    elevation: CardElevation = CardDefaults.cardElevation(),
    border: BorderStroke? = null,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val backdrop = liquidGlassBackdrop()
    if (backdrop != null && isGlassCandidate(shape, colors.containerColor)) {
        LiquidGlassSurface(
            backdrop = backdrop,
            shape = shape,
            modifier = modifier,
            containerColor = colors.containerColor,
            contentColor = if (enabled) Color.White else Color.White.copy(alpha = 0.6f),
            onClick = if (enabled) onClick else null
        ) {
            Column(content = content)
        }
    } else {
        androidx.compose.material3.Card(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            shape = shape,
            colors = colors,
            elevation = elevation,
            border = border,
            interactionSource = interactionSource,
            content = content
        )
    }
}

/**
 * Drop-in for Material3 AlertDialog. With Liquid Glass on, the dialog card is a glass box
 * (white text); otherwise it delegates to Material3 unchanged.
 */
@Composable
fun AlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: @Composable (() -> Unit)? = null,
    icon: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    shape: Shape = AlertDialogDefaults.shape,
    containerColor: Color = AlertDialogDefaults.containerColor,
    iconContentColor: Color = AlertDialogDefaults.iconContentColor,
    titleContentColor: Color = AlertDialogDefaults.titleContentColor,
    textContentColor: Color = AlertDialogDefaults.textContentColor,
    tonalElevation: Dp = AlertDialogDefaults.TonalElevation,
    properties: DialogProperties = DialogProperties()
) {
    val backdrop = liquidGlassBackdrop()
    if (backdrop != null) {
        GlassDialog(onDismissRequest = onDismissRequest, properties = properties) {
            //Re-read inside the dialog window: GlassDialog provides a window-corrected sampler here
            LiquidGlassSurface(
                backdrop = liquidGlassBackdrop() ?: backdrop,
                shape = shape,
                modifier = modifier,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = Color.White
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    icon?.let {
                        Box(modifier = Modifier.align(Alignment.CenterHorizontally)) { it() }
                    }
                    title?.let {
                        ProvideTextStyle(MaterialTheme.typography.headlineSmall) { it() }
                    }
                    text?.let {
                        ProvideTextStyle(MaterialTheme.typography.bodyMedium) {
                            Box(modifier = Modifier.weight(1f, fill = false)) { it() }
                        }
                    }
                    Row(
                        modifier = Modifier.align(Alignment.End),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        dismissButton?.invoke()
                        confirmButton()
                    }
                }
            }
        }
    } else {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = onDismissRequest,
            confirmButton = confirmButton,
            modifier = modifier,
            dismissButton = dismissButton,
            icon = icon,
            title = title,
            text = text,
            shape = shape,
            containerColor = containerColor,
            iconContentColor = iconContentColor,
            titleContentColor = titleContentColor,
            textContentColor = textContentColor,
            tonalElevation = tonalElevation,
            properties = properties
        )
    }
}

/**
 * Icon + label body of a glass navigation-rail entry. It has no click handling or ripple of its
 * own: the enclosing [LiquidGlassSurface] owns the tap and its springy press, so there is no
 * stray white indicator drawn over the icon.
 */
@Composable
fun GlassRailItemBody(
    icon: @Composable () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.layout.Column(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minWidth = 80.dp, minHeight = 80.dp)
            .padding(top = 12.dp, bottom = 16.dp, start = 8.dp, end = 8.dp),
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp, androidx.compose.ui.Alignment.CenterVertically)
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides Color.White
        ) {
            Box(
                modifier = Modifier.size(width = 56.dp, height = 32.dp),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) { icon() }
            label()
        }
    }
}


/** Menu item colors that stay white on the glass menu panel; Material3's default is a dark onSurface. */
@Composable
private fun glassMenuItemColors(): androidx.compose.material3.MenuItemColors =
    MenuDefaults.itemColors(
        textColor = Color.White,
        leadingIconColor = Color.White,
        trailingIconColor = Color.White,
        disabledTextColor = Color.White.copy(alpha = DISABLED_ALPHA),
        disabledLeadingIconColor = Color.White.copy(alpha = DISABLED_ALPHA),
        disabledTrailingIconColor = Color.White.copy(alpha = DISABLED_ALPHA)
    )

/** Drop-in for Material3 DropdownMenuItem: white content on the glass [DropdownMenu]. */
@Composable
fun DropdownMenuItem(
    text: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    colors: androidx.compose.material3.MenuItemColors = MenuDefaults.itemColors(),
    contentPadding: PaddingValues = MenuDefaults.DropdownMenuItemContentPadding,
    interactionSource: MutableInteractionSource? = null
) {
    val glass = liquidGlassBackdrop() != null
    androidx.compose.material3.DropdownMenuItem(
        text = text,
        onClick = onClick,
        modifier = if (glass) modifier.liquidGlassPressAnimation(enabled) else modifier,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        enabled = enabled,
        colors = if (glass) glassMenuItemColors() else colors,
        contentPadding = contentPadding,
        interactionSource = interactionSource
    )
}

/**
 * Drop-in for Material3 ExposedDropdownMenu (e.g. search suggestions under a text field). With
 * Liquid Glass on it is the glass [DropdownMenu], as wide as the field and not focusable so the
 * field keeps the keyboard.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExposedDropdownMenuBoxScope.ExposedDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.extraSmall,
    content: @Composable ColumnScope.() -> Unit
) {
    if (liquidGlassBackdrop() != null) {
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismissRequest,
            modifier = modifier,
            shape = shape,
            properties = PopupProperties(focusable = false),
            matchAnchorWidth = true,
            content = content
        )
    } else {
        M3ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismissRequest,
            modifier = modifier,
            shape = shape,
            content = content
        )
    }
}

/** Checkbox colors: white while Liquid Glass is active (like [glassRadioColors]), Material defaults otherwise. */
@Composable
fun glassCheckboxColors(): CheckboxColors {
    if (liquidGlassBackdrop() == null) return CheckboxDefaults.colors()
    return CheckboxDefaults.colors(
        checkedColor = Color.White,
        uncheckedColor = Color.White,
        checkmarkColor = Color.Black.copy(alpha = 0.75f),
        disabledCheckedColor = Color.White.copy(alpha = DISABLED_ALPHA),
        disabledUncheckedColor = Color.White.copy(alpha = DISABLED_ALPHA),
        disabledIndeterminateColor = Color.White.copy(alpha = DISABLED_ALPHA)
    )
}

/**
 * Drop-in for Material3 Checkbox. With Liquid Glass on the box is white and squishes like a pinch
 * while pressed, then springs back with an overshoot; a change of state (also a programmatic one,
 * e.g. select all) pops it with the same spring.
 */
@Composable
fun Checkbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: CheckboxColors = CheckboxDefaults.colors(),
    interactionSource: MutableInteractionSource? = null
) {
    if (liquidGlassBackdrop() == null) {
        androidx.compose.material3.Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = modifier,
            enabled = enabled,
            colors = colors,
            interactionSource = interactionSource
        )
        return
    }

    val source = interactionSource ?: remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale = remember { Animatable(1f) }
    val squish = remember { spring<Float>(dampingRatio = 0.35f, stiffness = 520f) }
    LaunchedEffect(pressed) {
        scale.animateTo(if (pressed) 0.78f else 1f, squish)
    }
    var firstComposition by remember { mutableStateOf(true) }
    LaunchedEffect(checked) {
        if (firstComposition) {
            firstComposition = false
        } else {
            scale.snapTo(0.78f)
            scale.animateTo(1f, squish)
        }
    }

    androidx.compose.material3.Checkbox(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier.graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
        },
        enabled = enabled,
        colors = glassCheckboxColors(),
        interactionSource = source
    )
}
