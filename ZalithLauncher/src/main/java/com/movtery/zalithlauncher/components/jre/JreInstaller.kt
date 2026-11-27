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

package com.movtery.zalithlauncher.components.jre

import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.context.GlobalContext
import com.movtery.zalithlauncher.coroutine.Task
import com.movtery.zalithlauncher.game.addons.modloader.ModLoader
import com.movtery.zalithlauncher.game.download.engine.DownloadEngine
import com.movtery.zalithlauncher.game.download.engine.DownloadRequest
import com.movtery.zalithlauncher.game.multirt.RuntimesManager
import com.movtery.zalithlauncher.game.version.installed.Version
import com.movtery.zalithlauncher.game.versioninfo.models.GameManifest
import com.movtery.zalithlauncher.path.PathManager
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.ui.androidText
import com.movtery.zalithlauncher.utils.device.Architecture
import com.movtery.zalithlauncher.utils.file.formatFileSize
import com.movtery.zalithlauncher.utils.logging.Logger
import com.movtery.zalithlauncher.ZLApplication
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

private const val TAG = "JreInstaller"

/** Downloads and installs the launcher runtimes when they are first needed */
object JreInstaller {
    private val mutex = Mutex()

    /**
     * Installs [jre] if it is missing or outdated.
     * An outdated runtime is kept when its update cannot be installed.
     */
    suspend fun ensure(jre: Jre, task: Task? = null): Unit = withContext(Dispatchers.IO) {
        mutex.withLock {
            val release = RuntimeCatalog.releases.getValue(jre)
            val installed = RuntimesManager.loadInternalRuntimeVersion(jre.jreName)
            if (installed == release.version) return@withLock

            try {
                install(jre, release, task)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (RuntimesManager.forceReload(jre.jreName).versionString == null) {
                    throw IOException(
                        GlobalContext.getString(R.string.download_java_runtime_failed, jre.majorVersion), e
                    )
                }
                Logger.warning(TAG, "Failed to update ${jre.jreName}, keeping the installed one", e)
            }
        }
    }

    /** Makes sure a runtime suitable for the given Java version is installed */
    suspend fun ensureForJava(majorVersion: Int, task: Task? = null) {
        val runtimes = RuntimesManager.getRuntimes()
        val exact = runtimes.filter { it.javaVersion == majorVersion }
        if (exact.isNotEmpty()) {
            //Prefer a user runtime over updating the launcher one
            if (exact.all { runtime -> findByName(runtime.name) != null }) {
                findByName(exact.first().name)?.let { ensure(it, task) }
            }
            return
        }
        val jre = Jre.entries.firstOrNull { it.majorVersion >= majorVersion }
        if (jre != null) ensure(jre, task)
    }

    /** Makes sure the runtime that launching [version] is going to pick is installed */
    suspend fun ensureForLaunch(version: Version, manifest: GameManifest, task: Task? = null) {
        val versionRuntime = version.getJavaRuntime()
        if (versionRuntime.isNotEmpty()) {
            findByName(versionRuntime)?.let { ensure(it, task) }
            return
        }

        val globalRuntime = AllSettings.javaRuntime.getValue()
        if (AllSettings.autoPickJavaRuntime.getValue()) {
            val target = when (version.getVersionInfo()?.primaryLoader?.loader) {
                ModLoader.BABRIC -> 17
                else -> manifest.javaVersion?.majorVersion ?: 8
            }
            if (RuntimesManager.loadRuntime(globalRuntime).javaVersion < target) {
                ensureForJava(target, task)
                return
            }
        }
        findByName(globalRuntime)?.let { ensure(it, task) }
    }

    private fun findByName(name: String): Jre? = Jre.entries.find { it.jreName == name }

    private suspend fun install(jre: Jre, release: RuntimeRelease, task: Task?) {
        val binName = "bin-" + Architecture.archAsString(ZLApplication.DEVICE_ARCHITECTURE) + ".tar.xz"
        val names = listOf("universal.tar.xz", binName)
        val packages = names.map { name ->
            release.packages[name] ?: throw IOException("No ${jre.jreName} package for this device: $name")
        }
        val files = names.map { File(PathManager.DIR_CACHE, "${jre.jreName}-$it") }
        val totalSize = packages.sumOf { it.size }
        val directory = jre.jrePath.substringAfterLast('/')

        try {
            var downloaded = 0L
            names.forEachIndexed { index, name ->
                DownloadEngine.download(
                    DownloadRequest(
                        urls = listOf("${RuntimeCatalog.BASE_URL}/$directory/$name"),
                        targetFile = files[index],
                        sha1 = packages[index].sha1,
                        expectedSize = packages[index].size
                    ),
                    sizeCallback = { delta ->
                        downloaded += delta
                        task?.updateProgress(downloaded.toFloat() / totalSize)
                        task?.updateMessage(
                            androidText(
                                R.string.download_java_runtime,
                                jre.majorVersion, formatFileSize(downloaded), formatFileSize(totalSize)
                            )
                        )
                    }
                )
            }

            task?.updateProgress(-1f)
            files[0].inputStream().use { universal ->
                files[1].inputStream().use { binaries ->
                    RuntimesManager.installRuntimeBinPack(
                        universalFileInputStream = universal,
                        platformBinsInputStream = binaries,
                        name = jre.jreName,
                        binPackVersion = release.version,
                        updateProgress = { textRes, textArgs ->
                            task?.updateMessage(androidText(textRes, *textArgs))
                        }
                    )
                }
            }
            RuntimesManager.postPrepare(jre.jreName)
        } finally {
            files.forEach { it.delete() }
        }
    }
}
