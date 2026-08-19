package com.ross.livemedia.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.ross.livemedia.utils.Logger

class MediaNotificationListenerService : NotificationListenerService() {
    private val logger = Logger("MediaListenerService")
    private lateinit var viewModel: NotificationViewModel
    
    // We need to keep NotificationManager here to perform notify/cancel
    private val notificationManager by lazy { getSystemService(NOTIFICATION_SERVICE) as NotificationManager }

    override fun onCreate() {
        super.onCreate()
        logger.info("onCreate")

        createNotificationChannel()

        viewModel = NotificationViewModel(
            application = application,
            onShowNotification = { notification ->
                // Keep the process alive while the media notification is
                // showing: Samsung kills "empty" processes regularly, which
                // would stop the session poll and the recents detection.
                try {
                    startForeground(NOTIFICATION_ID, notification)
                } catch (e: Exception) {
                    logger.e("Cannot start foreground service", e)
                }
            },
            onCancelNotification = {
                stopForeground(STOP_FOREGROUND_REMOVE)
                notificationManager.cancel(NOTIFICATION_ID)
            }
        )
        
        viewModel.init()
    }

    override fun onDestroy() {
        viewModel.cleanup()
        super.onDestroy()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        viewModel.onNotificationPosted(sbn)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        viewModel.onTransportControlAction(intent?.action)
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?, rankingMap: RankingMap?, reason: Int) {
        if (sbn?.id == NOTIFICATION_ID && sbn.packageName == packageName) {
            // REASON_CANCEL = 2 (User dismissed single notification)
            // REASON_CANCEL_ALL = 3 (User dismissed all notifications)
            if (reason == REASON_CANCEL || reason == REASON_CANCEL_ALL) {
                viewModel.onNotificationDismissed()
            }
        } else {
            viewModel.onNotificationRemoved(sbn)
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, "Media Live Updates", NotificationManager.IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(channel)
    }

    companion object {
        private const val NOTIFICATION_ID = 1337
        private const val CHANNEL_ID = "MediaLiveUpdateChannel"
    }
}