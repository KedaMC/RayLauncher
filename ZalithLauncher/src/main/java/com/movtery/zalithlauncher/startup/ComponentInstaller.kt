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
import com.movtery.zalithlauncher.components.Components
import com.movtery.zalithlauncher.components.InstallableItem
import com.movtery.zalithlauncher.components.UnpackComponentsTask
import com.movtery.zalithlauncher.components.jre.UnpackJnaTask
import com.movtery.zalithlauncher.utils.logging.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "ComponentInstaller"

/** A component that could not be installed, with the details to show to the user */
data class InstallFailure(
    val name: String,
    val detail: String
)

/** Checks and extracts the components bundled with the launcher */
class ComponentInstaller(context: Context) {
    private val items: List<InstallableItem> = buildList {
        Components.entries.forEach { component ->
            add(InstallableItem(component.displayName, null, UnpackComponentsTask(context, component)))
        }
        add(InstallableItem("JNA", null, UnpackJnaTask(context)))
    }.sorted()

    private val isFinished: Boolean
        get() = items.all { it.state.value == InstallableItem.State.FINISHED }

    /** @return true when every component is already installed and up to date */
    suspend fun check(): Boolean = withContext(Dispatchers.IO) {
        items.forEach { item ->
            guard(item) { item.updateState(item.task.checkState()) }
        }
        isFinished
    }

    /**
     * Installs every component that is not finished yet, can be called again to retry
     * @return the components that failed, empty on success
     */
    suspend fun install(): List<InstallFailure> {
        coroutineScope {
            items
                .filter {
                    val state = it.state.value
                    state == InstallableItem.State.NOT_STARTED ||
                    state == InstallableItem.State.PENDING ||
                    state == InstallableItem.State.FAILED
                }
                .forEach { item ->
                    launch(Dispatchers.IO) {
                        guard(item) {
                            item.updateState(InstallableItem.State.RUNNING)
                            item.task.updateMessage(null)
                            item.task.run()
                            item.updateState(InstallableItem.State.FINISHED)
                        }
                    }
                }
        }
        return items
            .filter { it.state.value != InstallableItem.State.FINISHED }
            .map { InstallFailure(it.name, it.task.taskMessage.value ?: it.state.value.name) }
    }

    private suspend fun guard(item: InstallableItem, block: suspend () -> Unit) {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.error(TAG, "Failed to install component: ${item.name}", e)
            item.task.updateMessage(e.stackTraceToString())
            item.updateState(InstallableItem.State.FAILED)
        }
    }
}
