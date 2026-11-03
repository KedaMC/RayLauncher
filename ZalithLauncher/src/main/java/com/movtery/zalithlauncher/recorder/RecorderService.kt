/*
 * Zalith Launcher Lite - Recorder projection service
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.movtery.zalithlauncher.recorder

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.notification.NOTIFICATION_ID_RECORDING_SERVICE
import com.movtery.zalithlauncher.notification.NotificationChannelData
import com.movtery.zalithlauncher.notification.NotificationManager

/**
 * Foreground service that holds the MediaProjection token while recording.
 *
 * Android only lets an app capture its own playback (game audio) through a
 * MediaProjection, and since Android 14 the token may only be created while a
 * foreground service of type mediaProjection is running, after the user has accepted
 * the "start recording" prompt. So the order is always:
 * prompt -> [start] -> startForeground -> getMediaProjection -> recorder start.
 * It runs in the game process, next to [GameRecorder].
 */
class RecorderService : Service() {
    private var projection: MediaProjection? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action != ACTION_START) {
            stopSelf()
            return START_NOT_STICKY
        }

        val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
        val resultData = if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra<Intent>(EXTRA_RESULT_DATA)
        }

        var started = false
        try {
            startAsForeground()
            if (resultData != null) started = startRecording(resultCode, resultData)
        } catch (error: Exception) {
            Log.e(TAG, "Unable to start recording service", error)
        }

        toast(if (started) R.string.game_menu_recorder_started else R.string.game_menu_recorder_start_failed)
        if (!started) stopSelf()
        return START_NOT_STICKY
    }

    private fun startAsForeground() {
        NotificationManager.createNotificationChannel(this, NotificationChannelData.RECORDING_SERVICE_CHANNEL)
        val notification: Notification = NotificationCompat.Builder(
            this, NotificationChannelData.RECORDING_SERVICE_CHANNEL.channelId
        )
            .setContentTitle(getString(R.string.recorder_notification_title))
            .setContentText(getString(R.string.recorder_notification_text))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID_RECORDING_SERVICE,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        } else {
            startForeground(NOTIFICATION_ID_RECORDING_SERVICE, notification)
        }
    }

    private fun startRecording(resultCode: Int, data: Intent): Boolean {
        val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val token = manager.getMediaProjection(resultCode, data) ?: return false
        projection = token

        // Required from Android 14 on; also tells us when the user stops it from the system UI.
        token.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() {
                Log.i(TAG, "MediaProjection stopped")
                if (projection === token) {
                    projection = null
                    RecorderController.stop(applicationContext)
                }
            }
        }, mainHandler)

        // On success GameRecorder owns the projection and stops it when the video is saved.
        val started = RecorderController.start(this, token)
        if (!started) {
            projection = null
            runCatching { token.stop() }
        }
        return started
    }

    private fun toast(textRes: Int) {
        Toast.makeText(applicationContext, textRes, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        projection = null
        super.onDestroy()
    }

    companion object {
        private const val TAG = "RecorderService"
        private const val ACTION_START = "com.movtery.zalithlauncher.recorder.START"
        private const val EXTRA_RESULT_CODE = "result_code"
        private const val EXTRA_RESULT_DATA = "result_data"

        /** Call with the result of the screen capture prompt. */
        fun start(context: Context, resultCode: Int, resultData: Intent) {
            val intent = Intent(context, RecorderService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_RESULT_CODE, resultCode)
                putExtra(EXTRA_RESULT_DATA, resultData)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, RecorderService::class.java))
        }
    }
}
