package com.kododake.aabrowser.car

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.kododake.aabrowser.R

/**
 * A `mediaPlayback` foreground service that keeps the app's process alive
 * while the projected browser is on screen, so the OS does not reclaim it (and
 * kill audio/video) during driving-state transitions. Mirrors the pattern used
 * by the working projected-WebView apps.
 */
class ForegroundService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startNotification()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) stopSelf()
        return START_STICKY
    }

    override fun onDestroy() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    private fun startNotification() {
        val manager = getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.car_notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.car_notification_channel_description)
                setShowBadge(false)
            }
            manager.createNotificationChannel(channel)
        }

        val stopIntent = Intent(this, ForegroundService::class.java).setAction(ACTION_STOP)
        val stopPending = PendingIntent.getService(
            this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(getString(R.string.car_notification_title))
            .setContentText(getString(R.string.car_notification_text))
            .setContentIntent(stopPending)
            .setShowWhen(true)
            .build()

        startForeground(
            ONGOING_NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
        )
    }

    companion object {
        private const val CHANNEL_ID = "com.kododake.aabrowser.car"
        private const val ONGOING_NOTIFICATION_ID = 1
        private const val ACTION_STOP = "com.kododake.aabrowser.car.STOP"

        fun start(context: Context) {
            context.startForegroundService(Intent(context, ForegroundService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, ForegroundService::class.java))
        }
    }
}
