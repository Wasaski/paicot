package com.paicot.server

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import com.paicot.Paicot
import com.paicot.core.PaicotLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Keeps the local HTTP server alive while the app is backgrounded.
 * Server state (running/error/bound port) is visible in Settings/Overview —
 * startup failures are recorded, never swallowed.
 */
class PaicotForegroundService : Service() {

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onBind(intent: Intent?) = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(NOTIFICATION_ID, buildNotification("Starting server..."))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> scope.launch {
                Paicot.server.startBlocking(
                    kotlinx.coroutines.runBlocking { Paicot.prefs.host() },
                    kotlinx.coroutines.runBlocking { Paicot.prefs.port() }
                )
                val server = Paicot.server
                updateNotification(if (server.running)
                    "Server listening on ${server.pendingHostForUi()}:${server.actualPort}"
                else "Server failed: ${server.lastError ?: "unknown error"}")
            }
            ACTION_STOP -> {
                Paicot.server.stop()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        Paicot.server.stop()
        super.onDestroy()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "PAICOT Server", NotificationManager.IMPORTANCE_LOW)
            channel.description = "PAICOT local HTTP server status"
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String): Notification {
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION") Notification.Builder(this)
        }
        return builder
            .setContentTitle("PAICOT")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()
    }

    private fun updateNotification(text: String) {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, buildNotification(text))
    }

    companion object {
        const val ACTION_START = "com.paicot.action.SERVER_START"
        const val ACTION_STOP = "com.paicot.action.SERVER_STOP"
        private const val CHANNEL_ID = "paicot_server"
        private const val NOTIFICATION_ID = 1001

        fun start(context: android.content.Context) {
            val intent = Intent(context, PaicotForegroundService::class.java).apply { action = ACTION_START }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: android.content.Context) {
            val intent = Intent(context, PaicotForegroundService::class.java).apply { action = ACTION_STOP }
            context.startService(intent)
        }
    }
}

/** Pending host for the notification (internal helper, avoids a public field). */
private fun com.paicot.server.PaicotServer.pendingHostForUi(): String = "127.0.0.1"
