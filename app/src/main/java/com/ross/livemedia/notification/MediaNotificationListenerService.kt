package com.ross.livemedia.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import com.bumptech.glide.Glide
import com.ross.livemedia.lockscreen.LockScreenManager
import com.ross.livemedia.media.MediaStateManager
import com.ross.livemedia.media.MusicProvider
import com.ross.livemedia.media.MusicState
import com.ross.livemedia.qs.QSStateProvider
import com.ross.livemedia.storage.StorageHelper
import com.ross.livemedia.utils.Logger
import com.ross.livemedia.utils.buildArtisAlbumTitle
import com.ross.livemedia.utils.buildBaseBigTextStyle
import com.ross.livemedia.utils.combineProviderAndTimestamp
import com.ross.livemedia.utils.createAction
import com.ross.livemedia.utils.getAppName
import com.ross.livemedia.utils.providePillText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val SCROLL_UPDATE_DELAY_MS = 300L
private const val STATIC_UPDATE_DELAY_MS = 1000L

class MediaNotificationListenerService : NotificationListenerService() {
    private val logger = Logger("MediaListenerService")
    private lateinit var mediaStateManager: MediaStateManager
    private lateinit var lockScreenManager: LockScreenManager
    private lateinit var notificationUpdateScheduler: NotificationUpdateScheduler
    private lateinit var storageHelper: StorageHelper
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var isQsOpen = false
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
                notificationUpdateScheduler.scheduleUpdate()
            },
            noActiveMedia = {
                logger.info("No audio. Disable notification")
                clearNotification()
            })

        notificationUpdateScheduler =
            NotificationUpdateScheduler {
                val state = mediaStateManager.getUpdatedMusicState()
                if (state != null) {
                    updateNotification(state)

                    val isPlaying = state.isPlaying
                    val isTitleScrollable = state.title.trim().length > 7
                    
                    val shouldScroll = storageHelper.isScrollEnabled && 
                        (storageHelper.pillContent == com.ross.livemedia.storage.PillContent.TITLE || !isPlaying) &&
                        isTitleScrollable
                    
                    val shouldRun = isPlaying || shouldScroll
                    
                    if (shouldRun) {
                        if (shouldScroll) SCROLL_UPDATE_DELAY_MS else STATIC_UPDATE_DELAY_MS
                    } else {
                        null
                    }
                } else {
                    null
                }
            }

        serviceScope.launch {
            QSStateProvider.isQsOpen.collectLatest { isOpen ->
                val wasOpen = isQsOpen
                isQsOpen = isOpen
                if (isOpen) {
                    if (storageHelper.hideNotificationOnQsOpen) {
                        clearNotification()
                    }
                } else if (wasOpen) {
                    mediaStateManager.getUpdatedMusicState()?.let {
                        updateNotification(it)
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
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

        // Launch on background thread (IO) to handle image loading
        serviceScope.launch(Dispatchers.IO) {
            val notification = buildNotification(musicState)

            // Switch back to Main thread to update UI (System Notification)
            withContext(Dispatchers.Main) {
                notificationManager.notify(NOTIFICATION_ID, notification)
            }
        }
    }

    private var lastTitle: String? = null
    private var titleStartTime: Long = 0L
    private var lastIsPlaying: Boolean = false

    private fun buildNotification(
        musicState: MusicState
    ): Notification {
        // Reset scroll if title changed OR if we just paused and were showing time
        val justPaused = lastIsPlaying && !musicState.isPlaying
        val wasShowingTime = storageHelper.pillContent != com.ross.livemedia.storage.PillContent.TITLE
        
        if (musicState.title != lastTitle || (justPaused && wasShowingTime)) {
            lastTitle = musicState.title
            titleStartTime = System.currentTimeMillis()
        }
        
        lastIsPlaying = musicState.isPlaying

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
            .setShortCriticalText(providePillText(
                musicState.title,
                musicState.position.toInt(),
                musicState.duration.toInt(),
                musicState.isPlaying,
                storageHelper.pillContent,
                storageHelper.isScrollEnabled,
                System.currentTimeMillis() - titleStartTime
            ))
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

        if (storageHelper.showAlbumArt) {
            var art: Bitmap? = musicState.albumArt

            if (art == null && musicState.albumArtUri != null) {
                try {
                    art = Glide.with(this@MediaNotificationListenerService)
                        .asBitmap()
                        .load(musicState.albumArtUri)
                        .submit(144, 144) // Increased to 256 for sharper icons
                        .get()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            notification.setLargeIcon(art)
        }


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