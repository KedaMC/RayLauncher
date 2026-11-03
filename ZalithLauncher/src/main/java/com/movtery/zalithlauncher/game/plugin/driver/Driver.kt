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

package com.movtery.zalithlauncher.game.plugin.driver

import com.movtery.zalithlauncher.game.plugin.ApkPlugin
import com.movtery.zalithlauncher.game.plugin.Plugin

class Driver(
    val id: String,
    appName: String,
    appVersion: String,
    val name: String,
    val summary: String? = null,
    val path: String,
    val isLauncher: Boolean,
    /** 驱动库文件名；null 表示使用默认的 libvulkan_freedreno.so */
    val driverLib: String? = null,
    /** GPU 家族门控：adreno（默认）、mali 或 any */
    val driverGpu: String = DEFAULT_GPU,
): ApkPlugin(
    packageName = id,
    appName = appName,
    appVersion = appVersion
), Plugin {
    override fun getIdentifier(): String = id
    override fun getNativeLibPath(): String = path

    companion object {
        const val DEFAULT_GPU = "adreno"
        private val VALID_GPU = setOf("adreno", "mali", "any")

        /** 规范化 meta-data 中的 driver_gpu，非法值回落到默认 */
        fun normalizeGpu(raw: String?): String {
            val value = raw?.trim()?.lowercase()
            return if (value != null && value in VALID_GPU) value else DEFAULT_GPU
        }
    }
}
