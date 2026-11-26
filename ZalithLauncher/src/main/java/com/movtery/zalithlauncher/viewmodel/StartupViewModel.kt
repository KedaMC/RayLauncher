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

package com.movtery.zalithlauncher.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.movtery.zalithlauncher.components.jre.Jre
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.setting.launcherMMKV
import com.movtery.zalithlauncher.startup.ComponentInstaller
import com.movtery.zalithlauncher.startup.InstallFailure
import kotlinx.coroutines.launch

//Same key as the former welcome page, so existing installs are not treated as new ones
private const val FIRST_INSTALL_KEY = "welcomeVersionKey"
private const val FIRST_INSTALL_DONE = 1

sealed interface StartupPhase {
    data object Checking : StartupPhase
    /** First launch: waiting for the Start button */
    data object AwaitingStart : StartupPhase
    data object Installing : StartupPhase
    data class Failed(val failures: List<InstallFailure>) : StartupPhase
    data object Ready : StartupPhase
}

class StartupViewModel(application: Application) : AndroidViewModel(application) {
    val isFirstInstall = launcherMMKV().getInt(FIRST_INSTALL_KEY, 0) < FIRST_INSTALL_DONE

    var phase by mutableStateOf<StartupPhase>(StartupPhase.Checking)
        private set

    private val installer = ComponentInstaller(application)

    init {
        viewModelScope.launch {
            val installed = installer.check()
            when {
                isFirstInstall -> phase = StartupPhase.AwaitingStart
                installed -> phase = StartupPhase.Ready
                else -> install()
            }
        }
    }

    /** Starts or retries the installation */
    fun install() {
        if (phase == StartupPhase.Installing) return
        phase = StartupPhase.Installing
        viewModelScope.launch {
            val failures = installer.install()
            phase = if (failures.isEmpty()) {
                onInstalled()
                StartupPhase.Ready
            } else {
                StartupPhase.Failed(failures)
            }
        }
    }

    private fun onInstalled() {
        AllSettings.javaRuntime.apply {
            //检查并设置默认的Java环境
            if (getValue().isEmpty()) save(Jre.JRE_8.jreName)
        }
        launcherMMKV().putInt(FIRST_INSTALL_KEY, FIRST_INSTALL_DONE).apply()
    }
}
