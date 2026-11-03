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

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.movtery.zalithlauncher.ui.theme.showThemed
import java.lang.ref.WeakReference

/**
 * Message dialogs that are requested from non-Compose code (view models, utils, coroutines).
 *
 * The launcher activity hosts [GlobalDialogHost], so these dialogs are drawn with the same Liquid
 * Glass [AlertDialog] as every other dialog. Activities without a host (in-game, control editor,
 * crash screens) keep the native Material dialog, since they have no launcher backdrop to blur.
 */
object GlobalDialogs {
    class Action(val text: String, val onClick: () -> Unit = {})

    class Request(
        val title: String,
        val message: String,
        val positive: Action?,
        val negative: Action?,
        val neutral: Action?,
        val cancelable: Boolean
    )

    private val queue = mutableStateListOf<Request>()
    private var host: WeakReference<Activity>? = null

    internal val current: Request? get() = queue.firstOrNull()

    internal fun dismissCurrent() {
        if (queue.isNotEmpty()) queue.removeAt(0)
    }

    internal fun attach(activity: Activity) {
        host = WeakReference(activity)
    }

    internal fun detach(activity: Activity) {
        if (host?.get() === activity) {
            host = null
            queue.clear()
        }
    }

    private fun isHosted(context: Context): Boolean {
        val activity = host?.get() ?: return false
        return context === activity || (context as? android.content.ContextWrapper)?.baseContext === activity
    }

    /**
     * Show a message dialog: glass when [context] is the hosting launcher activity, native otherwise.
     * Must be called on the main thread.
     */
    fun show(
        context: Context,
        title: String,
        message: String,
        positive: Action? = null,
        negative: Action? = null,
        neutral: Action? = null,
        cancelable: Boolean = true
    ) {
        if (isHosted(context)) {
            queue.add(Request(title, message, positive, negative, neutral, cancelable))
            return
        }
        MaterialAlertDialogBuilder(context)
            .setTitle(title)
            .setMessage(message)
            .setCancelable(cancelable)
            .apply {
                positive?.let { a -> setPositiveButton(a.text) { d, _ -> d.dismiss(); a.onClick() } }
                negative?.let { a -> setNegativeButton(a.text) { d, _ -> d.dismiss(); a.onClick() } }
                neutral?.let { a -> setNeutralButton(a.text) { d, _ -> d.dismiss(); a.onClick() } }
            }
            .showThemed()
    }
}

/** Renders the pending [GlobalDialogs] requests, one at a time. Place it once in the launcher activity. */
@Composable
fun GlobalDialogHost() {
    val activity = LocalContext.current.let { ctx ->
        var c = ctx
        while (c is android.content.ContextWrapper && c !is Activity) c = c.baseContext
        c as? Activity
    }
    DisposableEffect(activity) {
        activity?.let { GlobalDialogs.attach(it) }
        onDispose { activity?.let { GlobalDialogs.detach(it) } }
    }

    val request = GlobalDialogs.current ?: return
    fun run(action: GlobalDialogs.Action) {
        GlobalDialogs.dismissCurrent()
        action.onClick()
    }

    AlertDialog(
        modifier = if (liquidGlassBackdrop() != null) Modifier.glassPopIn() else Modifier,
        onDismissRequest = {
            if (request.cancelable) GlobalDialogs.dismissCurrent()
        },
        properties = DialogProperties(
            dismissOnBackPress = request.cancelable,
            dismissOnClickOutside = request.cancelable
        ),
        title = {
            Text(text = request.title, style = MaterialTheme.typography.titleLarge)
        },
        text = {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fadeEdge(state = scrollState)
                    .verticalScrollWithBar(state = scrollState)
            ) {
                Text(text = request.message)
            }
        },
        confirmButton = {
            request.positive?.let { action ->
                Button(onClick = { run(action) }) { MarqueeText(text = action.text) }
            }
        },
        dismissButton = {
            if (request.neutral != null || request.negative != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    request.neutral?.let { action ->
                        FilledTonalButton(onClick = { run(action) }) { MarqueeText(text = action.text) }
                    }
                    request.negative?.let { action ->
                        FilledTonalButton(onClick = { run(action) }) { MarqueeText(text = action.text) }
                    }
                }
            }
        }
    )
}
