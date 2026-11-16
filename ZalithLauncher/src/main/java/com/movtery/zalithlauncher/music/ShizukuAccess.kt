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

package com.movtery.zalithlauncher.music

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.session.MediaSessionManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import com.movtery.zalithlauncher.utils.logging.Logger
import org.lsposed.hiddenapibypass.HiddenApiBypass
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper

private const val TAG = "ShizukuAccess"
private const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
private const val SHIZUKU_DOWNLOAD_URL = "https://shizuku.rikka.app/download/"
private const val PERMISSION_REQUEST_CODE = 6021

/** Where the Shizuku connection stands, from the point of view of the music of other apps */
enum class ShizukuState {
    /** The Shizuku app is not installed */
    NOT_INSTALLED,

    /** Shizuku is installed but its service is not running */
    NOT_RUNNING,

    /** Shizuku runs, but RayLauncher was not allowed to use it yet */
    PERMISSION_NEEDED,

    /** Ready: the media sessions of other apps can be read */
    GRANTED
}

/**
 * Reads the media sessions of other apps through Shizuku, so the launcher does not need
 * notification access (or any other sensitive permission in its manifest).
 * Shizuku runs the call with the shell identity, which is allowed to list every media session.
 */
object ShizukuAccess {
    fun state(context: Context): ShizukuState {
        val running = runCatching { Shizuku.pingBinder() }.getOrDefault(false)
        if (running) {
            val granted = runCatching { Shizuku.checkSelfPermission() }
                .getOrDefault(PackageManager.PERMISSION_DENIED) == PackageManager.PERMISSION_GRANTED
            return if (granted) ShizukuState.GRANTED else ShizukuState.PERMISSION_NEEDED
        }
        return if (isInstalled(context)) ShizukuState.NOT_RUNNING else ShizukuState.NOT_INSTALLED
    }

    private fun isInstalled(context: Context): Boolean =
        runCatching { context.packageManager.getPackageInfo(SHIZUKU_PACKAGE, 0) }.isSuccess

    /**
     * Asks Shizuku for the permission. When the user chose "deny and don't ask again" the system
     * dialog cannot appear anymore, so the Shizuku app is opened to change it there.
     */
    fun requestPermission(context: Context) {
        val denied = runCatching { Shizuku.shouldShowRequestPermissionRationale() }.getOrDefault(false)
        if (denied) {
            openShizuku(context)
        } else {
            runCatching { Shizuku.requestPermission(PERMISSION_REQUEST_CODE) }
                .onFailure { Logger.warning(TAG, "Failed to request the Shizuku permission.", it) }
        }
    }

    /** Opens the Shizuku app, or its download page when it is not installed */
    fun openShizuku(context: Context) {
        val intent = context.packageManager.getLaunchIntentForPackage(SHIZUKU_PACKAGE)
            ?: Intent(Intent.ACTION_VIEW, Uri.parse(SHIZUKU_DOWNLOAD_URL))
        runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }

    /** Runs [onChanged] when Shizuku starts, stops, or answers the permission request. Call [AutoCloseable.close] to stop. */
    fun subscribe(onChanged: () -> Unit): AutoCloseable {
        val received = Shizuku.OnBinderReceivedListener { onChanged() }
        val dead = Shizuku.OnBinderDeadListener { onChanged() }
        val result = Shizuku.OnRequestPermissionResultListener { _, _ -> onChanged() }
        runCatching {
            Shizuku.addBinderReceivedListenerSticky(received)
            Shizuku.addBinderDeadListener(dead)
            Shizuku.addRequestPermissionResultListener(result)
        }.onFailure { Logger.warning(TAG, "Failed to listen to Shizuku.", it) }
        return AutoCloseable {
            runCatching {
                Shizuku.removeBinderReceivedListener(received)
                Shizuku.removeBinderDeadListener(dead)
                Shizuku.removeRequestPermissionResultListener(result)
            }
        }
    }

    /**
     * A private [MediaSessionManager] whose binder is relayed through Shizuku. The framework code
     * keeps doing the marshalling for every Android version, only the identity of the call changes.
     * The manager the rest of the app uses is left untouched.
     *
     * @return null when the hidden API is not reachable on this device
     */
    fun createSessionManager(context: Context): MediaSessionManager? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            HiddenApiBypass.addHiddenApiExemptions("Landroid/media/session/")
        }
        val shellBinder: IBinder = ShizukuBinderWrapper(SystemServiceHelper.getSystemService(Context.MEDIA_SESSION_SERVICE))
        val stub = Class.forName("android.media.session.ISessionManager\$Stub")
        val service = stub.getMethod("asInterface", IBinder::class.java).invoke(null, shellBinder)

        val manager = MediaSessionManager::class.java
            .getDeclaredConstructor(Context::class.java)
            .apply { isAccessible = true }
            .newInstance(context)
        MediaSessionManager::class.java
            .getDeclaredField("mService")
            .apply { isAccessible = true }
            .set(manager, service)
        manager
    } catch (e: Throwable) {
        Logger.warning(TAG, "Failed to open the media sessions through Shizuku.", e)
        null
    }
}
