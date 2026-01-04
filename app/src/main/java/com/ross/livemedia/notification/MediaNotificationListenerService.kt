package com.ross.livemedia.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.ross.livemedia.lockscreen.LockScreenManager
import com.ross.livemedia.media.MediaStateManager
import com.ross.livemedia.media.MusicProvider
import com.ross.livemedia.media.MusicState
import com.ross.livemedia.settings.QSStateListener.Companion.QS_CLOSED_ACTION
import com.ross.livemedia.settings.QSStateListener.Companion.QS_OPENED_ACTION
import com.ross.livemedia.storage.StorageHelper
import com.ross.livemedia.utils.Logger
import com.ross.livemedia.utils.buildArtisAlbumTitle
import com.ross.livemedia.utils.buildBaseBigTextStyle
import com.ross.livemedia.utils.combineProviderAndTimestamp
import com.ross.livemedia.utils.createAction
import com.ross.livemedia.utils.getAppName

class MediaNotificationListenerService : NotificationListenerService() {
    private val logger = Logger("MediaListenerService")
    private lateinit var mediaStateManager: MediaStateManager
    private lateinit var lockScreenManager: LockScreenManager
    private lateinit var notificationUpdateScheduler: NotificationUpdateScheduler
    private lateinit var storageHelper: StorageHelper
    private var isQsOpen = false
    private val qsStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                QS_OPENED_ACTION -> {
                    isQsOpen = true
                    if (storageHelper.hideNotificationOnQsOpen) {
                        clearNotification()
                    }
                }
                QS_CLOSED_ACTION -> {
                    isQsOpen = false
                    mediaStateManager.getUpdatedMusicState()?.let {
                        updateNotification(it)
                    }
                }
            }
        }
    }
    private val notificationManager by lazy { getSystemService(NOTIFICATION_SERVICE) as NotificationManager }

    override fun onCreate() {
        super.onCreate()
        logger.info("onCreate")

        createNotificationChannel()

        storageHelper = StorageHelper(this)

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

        androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(this).registerReceiver(
            qsStateReceiver,
            android.content.IntentFilter().apply {
                addAction(QS_OPENED_ACTION)
                addAction(QS_CLOSED_ACTION)
            }
        )
    }

    override fun onDestroy() {
        LocalBroadcastManager.getInstance(this)
            .unregisterReceiver(qsStateReceiver)
        super.onDestroy()
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
        if (!lockScreenManager.isScreenUnlocked() || (isQsOpen && storageHelper.hideNotificationOnQsOpen)) {
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

        val musicAppName = packageManager.getAppName(musicState.packageName) as String

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(MusicProvider.getByAppName(musicAppName).iconRes)
            .setContentTitle(musicState.title)
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_PROGRESS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setShortCriticalText(musicState.title.take(7).trimEnd())
            .setRequestPromotedOngoing(true)
            .setShowWhen(false)
            .setStyle(buildBaseBigTextStyle())
            .setSubText(
                combineProviderAndTimestamp(
                    musicAppName,
                    storageHelper.showMusicProvider,
                    storageHelper.showTimestamp,
                    musicState.position.toInt(),
                    musicState.duration.toInt()
                )
            )

        if (storageHelper.showAlbumArt) notification.setLargeIcon(musicState.albumArt)

        if (storageHelper.showProgress) {
            notification.setProgress(
                musicState.duration.toInt(),
                musicState.position.toInt(),
                false
            )
        }


        if (storageHelper.showArtistName || storageHelper.showAlbumName) {
            notification.setContentText(
                buildArtisAlbumTitle(
                    storageHelper.showArtistName,
                    storageHelper.showAlbumName,
                    musicState
                )
            )
        }

        if (storageHelper.showActionButtons) {
            notification.addAction(prevMusicAction)
            notification.addAction(if (musicState.isPlaying) playMusicAction else pauseMusicAction)
            notification.addAction(nextMusicAction)
        }

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
            MediaStateManager.ACTION_PLAY_PAUSE,
            MediaStateManager.REQUEST_CODE_PLAY_PAUSE,
            this,
            this::class.java
        )
    }

    val pauseMusicAction by lazy {
        createAction(
            android.R.drawable.ic_media_play,
            "Play",
            MediaStateManager.ACTION_PLAY_PAUSE,
            MediaStateManager.REQUEST_CODE_PLAY_PAUSE,
            this,
            this::class.java
        )
    }

    val prevMusicAction by lazy {
        createAction(
            android.R.drawable.ic_media_previous,
            "Previous",
            MediaStateManager.ACTION_SKIP_TO_PREVIOUS,
            MediaStateManager.REQUEST_CODE_PREVIOUS,
            this,
            this::class.java
        )
    }
    val nextMusicAction by lazy {
        createAction(
            android.R.drawable.ic_media_next,
            "Next",
            MediaStateManager.ACTION_SKIP_TO_NEXT,
            MediaStateManager.REQUEST_CODE_NEXT,
            this,
            this::class.java
        )
    }

    companion object {
        private const val NOTIFICATION_ID = 1337
        private const val CHANNEL_ID = "MediaLiveUpdateChannel"
    }
}