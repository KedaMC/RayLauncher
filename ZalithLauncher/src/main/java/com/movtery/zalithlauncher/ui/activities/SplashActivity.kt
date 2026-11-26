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

package com.movtery.zalithlauncher.ui.activities

import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.core.graphics.drawable.toDrawable
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.movtery.zalithlauncher.startup.StartupAnimation
import com.movtery.zalithlauncher.startup.StartupTransition
import com.movtery.zalithlauncher.ui.base.BaseAppCompatActivity
import com.movtery.zalithlauncher.ui.screens.splash.StartupScreen
import com.movtery.zalithlauncher.utils.logging.Logger
import com.movtery.zalithlauncher.viewmodel.StartupPhase
import com.movtery.zalithlauncher.viewmodel.StartupViewModel
import dagger.hilt.android.AndroidEntryPoint

private const val TAG = "SplashActivity"

const val EXTRA_IMPORT_ACTION = "EXTRA_IMPORT_ACTION"
const val EXTRA_IMPORT_URI    = "EXTRA_IMPORT_URI"
const val EXTRA_IMPORT_TYPE   = "EXTRA_IMPORT_TYPE"

const val IMPORT_TYPE_MODPACK = "modpack"
const val IMPORT_TYPE_CONTROLS = "controls"
const val IMPORT_TYPE_UNKNOWN = "unknown"

@SuppressLint("CustomSplashScreen")
@AndroidEntryPoint
class SplashActivity : BaseAppCompatActivity() {
    private var pendingImportIntent: Intent? = null

    private val startupViewModel: StartupViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        window.setBackgroundDrawable(Color.BLACK.toDrawable())

        //An external import goes straight to its task, without the startup video
        val video = if (isExternalImport(intent)) null else StartupAnimation.videoToPlay()

        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                StartupScreen(
                    phase = startupViewModel.phase,
                    isFirstInstall = startupViewModel.isFirstInstall,
                    video = video,
                    onInstall = startupViewModel::install,
                    onFinished = ::finishSplash
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        //若依赖未完成，暂存外部导入，待依赖完成后处理
        if (startupViewModel.phase != StartupPhase.Ready) {
            if (isExternalImport(intent)) {
                pendingImportIntent = intent
                Logger.info(TAG, "Import intent received but dependencies are not ready, deferring.")
            }
            return
        }

        if (isExternalImport(intent)) {
            handleImportIntent(intent)
            closeWithoutAnimation()
        }
    }

    private fun finishSplash() {
        if (isFinishing) return
        val import = pendingImportIntent ?: intent.takeIf { isExternalImport(it) }

        if (import == null || !handleImportIntent(import)) {
            startMain(Intent(this, MainActivity::class.java))
        }
        closeWithoutAnimation()
    }

    /** Opens the main page without an activity animation, it fades in from black by itself */
    private fun startMain(forward: Intent) {
        forward.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        forward.putExtra(StartupTransition.EXTRA_REVEAL, true)
        startActivity(forward, ActivityOptions.makeCustomAnimation(this, 0, 0).toBundle())
    }

    @Suppress("DEPRECATION")
    private fun closeWithoutAnimation() {
        finish()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
        } else {
            overridePendingTransition(0, 0)
        }
    }

    private fun handleImportIntent(source: Intent): Boolean {
        if (!isImportIntent(source)) return false

        val uri: Uri? = when (source.action) {
            Intent.ACTION_SEND -> {
                source.clipData?.getItemAt(0)?.uri
                    ?: source.getParcelableExtra(Intent.EXTRA_STREAM)
            }
            Intent.ACTION_VIEW -> source.data
            else -> null
        }

        if (uri == null) {
            Logger.warning(TAG, "No valid import Uri found")
            return false
        } else {
            try {
                //可持久化访问授权
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: Exception) {
                Logger.warning(TAG, "No persistable permission granted for $uri")
            }
        }

        val forward = Intent(this, MainActivity::class.java).apply {
            putExtra(EXTRA_IMPORT_ACTION, source.action)
            putExtra(EXTRA_IMPORT_URI, uri)
            putExtra(EXTRA_IMPORT_TYPE, resolveImportType(source))
        }

        startMain(forward)
        return true
    }

    /**
     * 根据 AndroidManifest 内为 activity-alias 配置的 meta-data 来判断导入类型
     */
    private fun resolveImportType(intent: Intent): String {
        val comp = intent.component ?: return IMPORT_TYPE_UNKNOWN
        val info = packageManager.getActivityInfo(comp, PackageManager.GET_META_DATA)
        return info.metaData?.getString("import_type") ?: IMPORT_TYPE_UNKNOWN
    }

    private fun isExternalImport(intent: Intent): Boolean =
        isImportIntent(intent) && !isLauncherIntent(intent)

    private fun isLauncherIntent(intent: Intent?): Boolean {
        return intent != null && intent.action == Intent.ACTION_MAIN &&
                intent.categories?.contains(Intent.CATEGORY_LAUNCHER) == true
    }

    private fun isImportIntent(intent: Intent?): Boolean {
        val comp = intent?.component ?: return false
        val info = packageManager.getActivityInfo(comp, PackageManager.GET_META_DATA)
        return info.metaData?.getString("import_type") != null
    }
}
