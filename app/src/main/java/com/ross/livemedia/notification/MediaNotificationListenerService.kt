package com.ross.livemedia.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.annotation.ColorInt
import androidx.compose.ui.graphics.Color
import androidx.core.app.NotificationCompat
import com.ross.livemedia.R
import com.ross.livemedia.lockscreen.LockScreenManager
import com.ross.livemedia.media.MediaStateManager
import com.ross.livemedia.media.MusicState
import com.ross.livemedia.utils.Logger
import com.ross.livemedia.utils.buildBaseProgressStyle
import com.ross.livemedia.utils.createAction
import com.ross.livemedia.utils.getAppName

class MediaNotificationListenerService : NotificationListenerService() {
    private val logger = Logger("MediaListenerService")
    private lateinit var mediaStateManager: MediaStateManager
    private lateinit var lockScreenManager: LockScreenManager
    private lateinit var notificationUpdateScheduler: NotificationUpdateScheduler
    private val notificationManager by lazy { getSystemService(NOTIFICATION_SERVICE) as NotificationManager }

    override fun onCreate() {
        super.onCreate()
        logger.info("onCreate")

        createNotificationChannel()

        lockScreenManager = LockScreenManager(
            this,
            deviceLocked = {
                logger.info("Device Locked. Clear notification")
                clearNotification()
            },
            deviceUnlocked = {
                logger.info("Device unlocked. Show notification")
                mediaStateManager.getUpdatedMusicState()?.let {
                    updateNotification(it)
                }
            })


        mediaStateManager = MediaStateManager(
            this,
            onStateUpdated = { state ->
                logger.info("StateUpdated")
                updateNotification(state)
                notificationUpdateScheduler.scheduleUpdate(state)
            },
            noActiveMedia = {
                logger.info("No audio. Disable notification")
                clearNotification()
            })

        notificationUpdateScheduler =
            NotificationUpdateScheduler {
                logger.info("Time to check new state")
                mediaStateManager.getUpdatedMusicState()?.let {
                    updateNotification(it)
                }

            }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn?.notification?.category == Notification.CATEGORY_TRANSPORT) {
            logger.info("Media notification detected from: ${sbn.packageName}")
            mediaStateManager.maybeUpdateMediaController()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        mediaStateManager.handleTransportControl(intent?.action)
        return super.onStartCommand(intent, flags, startId)
    }

    private fun updateNotification(musicState: MusicState) {
        if (!lockScreenManager.isScreenUnlocked()) {
            clearNotification()
            return
        }

        val notification = buildNotification(musicState)

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(
        musicState: MusicState
    ): Notification {
        var contentIntent: PendingIntent? = null

        val launchIntent = packageManager.getLaunchIntentForPackage(musicState.packageName)

        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)

            contentIntent = PendingIntent.getActivity(
                this, 0, // Request code
                launchIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.outline_artist_24)
            .setContentTitle(musicState.title)
            .setContentText(musicState.artist)
            .setLargeIcon(musicState.albumArt)
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_PROGRESS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setShortCriticalText(musicState.title.take(7))
            .addAction(prevMusicAction)
            .addAction(if (musicState.isPlaying) playMusicAction else pauseMusicAction)
            .addAction(nextMusicAction)
            .setRequestPromotedOngoing(true)
            .setShowWhen(false)
            .setSubText(packageManager.getAppName(musicState.packageName))
            .setStyle(buildBaseProgressStyle(musicState.duration, musicState.position))
            .setProgress(musicState.duration.toInt(), musicState.position.toInt(), false)

        if (contentIntent != null) {
            notification.setContentIntent(contentIntent)
        }

        return notification.build()
    }

    private fun clearNotification() {
        notificationManager.cancel(NOTIFICATION_ID)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, "Media Live Updates", NotificationManager.IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(channel)
    }

    val playMusicAction by lazy {
        createAction(
            android.R.drawable.ic_media_pause,
            "Pause",
            MediaStateManager.Companion.ACTION_PLAY_PAUSE,
            MediaStateManager.Companion.REQUEST_CODE_PLAY_PAUSE,
            this,
            this::class.java
        )
    }

    val pauseMusicAction by lazy {
        createAction(
            android.R.drawable.ic_media_play,
            "Play",
            MediaStateManager.Companion.ACTION_PLAY_PAUSE,
            MediaStateManager.Companion.REQUEST_CODE_PLAY_PAUSE,
            this,
            this::class.java
        )
    }

    val prevMusicAction by lazy {
        createAction(
            android.R.drawable.ic_media_previous,
            "Previous",
            MediaStateManager.Companion.ACTION_SKIP_TO_PREVIOUS,
            MediaStateManager.Companion.REQUEST_CODE_PREVIOUS,
            this,
            this::class.java
        )
    }
    val nextMusicAction by lazy {
        createAction(
            android.R.drawable.ic_media_next,
            "Next",
            MediaStateManager.Companion.ACTION_SKIP_TO_NEXT,
            MediaStateManager.Companion.REQUEST_CODE_NEXT,
            this,
            this::class.java
        )
    }

    companion object {
        private const val NOTIFICATION_ID = 1337
        private const val CHANNEL_ID = "MediaLiveUpdateChannel"
    }
}