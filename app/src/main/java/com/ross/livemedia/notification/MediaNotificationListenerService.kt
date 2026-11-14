package com.ross.livemedia.notification

import android.app.Notification
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
                notificationManager.notify(NOTIFICATION_ID, notification)
            },
            onCancelNotification = {
                notificationManager.cancel(NOTIFICATION_ID)
            },
            hasOtherLiveUpdateNotification = {
                checkOtherLiveUpdateNotification()
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
        viewModel.onAnyNotificationChanged()
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
        }
        viewModel.onAnyNotificationChanged()
    }

    private fun checkOtherLiveUpdateNotification(): Boolean {
        val active = try {
            activeNotifications
        } catch (e: Exception) {
            null
        }
        if (active.isNullOrEmpty()) return false
        val ourPkg = packageName
        for (sbn in active) {
            if (sbn.packageName == ourPkg) continue
            val notification = sbn.notification ?: continue
            val extras = notification.extras ?: continue

            val isPromotedOngoing = extras.getBoolean("android.requestPromotedOngoing", false) ||
                    extras.getBoolean("android.promotedOngoing", false) ||
                    !extras.getCharSequence("android.shortCriticalText").isNullOrEmpty()

            val isOngoingCategory = (notification.flags and Notification.FLAG_ONGOING_EVENT != 0) &&
                    (notification.category == Notification.CATEGORY_PROGRESS ||
                     notification.category == Notification.CATEGORY_NAVIGATION ||
                     notification.category == Notification.CATEGORY_WORKOUT)

            if (isPromotedOngoing || isOngoingCategory) {
                return true
            }
        }
        return false
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