package com.ross.livemedia.notification

import android.app.Application
import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
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
import androidx.lifecycle.AndroidViewModel


private const val SCROLL_UPDATE_DELAY_MS = 500L
private const val STATIC_UPDATE_DELAY_MS = 1000L
private const val CHANNEL_ID = "MediaLiveUpdateChannel"


class NotificationViewModel(
    application: Application,
    private val onShowNotification: (Notification) -> Unit,
    private val onCancelNotification: () -> Unit
) : AndroidViewModel(application) {
    private val logger = Logger("NotificationViewModel")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    private val storageHelper = StorageHelper(application)
    private lateinit var mediaStateManager: MediaStateManager
    private lateinit var lockScreenManager: LockScreenManager
    private lateinit var notificationUpdateScheduler: NotificationUpdateScheduler

    private var isQsOpen = false
    private var lastTitle: String? = null
    private var titleStartTime: Long = 0L
    private var lastIsPlaying: Boolean = false

    fun init() {
        logger.info("init")
        val context = getApplication<Application>()

        lockScreenManager = LockScreenManager(
            context,
            deviceLocked = {
                logger.info("Device Locked. Clear notification")
                onCancelNotification()
            },
            deviceUnlocked = {
                logger.info("Device unlocked. Show notification")
                mediaStateManager.getUpdatedMusicState()?.let {
                    updateNotification(it)
                }
            })

        mediaStateManager = MediaStateManager(
            context,
            onStateUpdated = { state ->
                logger.info("StateUpdated")
                updateNotification(state)
                notificationUpdateScheduler.scheduleUpdate()
            },
            noActiveMedia = {
                logger.info("No audio. Disable notification")
                onCancelNotification()
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

        scope.launch {
            QSStateProvider.isQsOpen.collectLatest { isOpen ->
                val wasOpen = isQsOpen
                isQsOpen = isOpen
                if (isOpen) {
                    if (storageHelper.hideNotificationOnQsOpen) {
                        onCancelNotification()
                    }
                } else if (wasOpen) {
                    mediaStateManager.getUpdatedMusicState()?.let {
                        updateNotification(it)
                    }
                }
            }
        }
    }

    fun cleanup() {
        scope.cancel()
    }

    fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn?.notification?.category == Notification.CATEGORY_TRANSPORT) {
            logger.info("Media notification detected from: ${sbn.packageName}")
            mediaStateManager.maybeUpdateMediaController()
        }
    }

    fun onTransportControlAction(action: String?) {
        mediaStateManager.handleTransportControl(action)
    }

    private fun updateNotification(musicState: MusicState) {
        if (!lockScreenManager.isScreenUnlocked() || (isQsOpen && storageHelper.hideNotificationOnQsOpen) || !storageHelper.isAppEnabled(musicState.packageName)) {
            onCancelNotification()
            return
        }

        // Launch on background thread (IO) to handle image loading
        scope.launch(Dispatchers.IO) {
            val notification = buildNotification(musicState)
            // Dispatch to main thread done by scope (Main) but callback might be called from IO if not careful?
            // Wait, scope is Main. launch(IO) runs block on IO.
            // We need to switch back to Main for callback? 
            // Usually notify is thread safe, but let's be safe if UI operations are involved on consumer side.
            // But Service.notify IS thread safe.
            onShowNotification(notification)
        }
    }

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
        val context = getApplication<Application>()

        val launchIntent = context.packageManager.getLaunchIntentForPackage(musicState.packageName)

        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)

            contentIntent = PendingIntent.getActivity(
                context, 0, // Request code
                launchIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val musicAppName = context.packageManager.getAppName(musicState.packageName) as String

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
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
                    art = Glide.with(context)
                        .asBitmap()
                        .load(musicState.albumArtUri)
                        .submit(144, 144)
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

    private val playMusicAction by lazy {
        createAction(
            android.R.drawable.ic_media_pause,
            "Pause",
            MediaStateManager.ACTION_PLAY_PAUSE,
            MediaStateManager.REQUEST_CODE_PLAY_PAUSE,
            getApplication(),
            MediaNotificationListenerService::class.java
        )
    }

    private val pauseMusicAction by lazy {
        createAction(
            android.R.drawable.ic_media_play,
            "Play",
            MediaStateManager.ACTION_PLAY_PAUSE,
            MediaStateManager.REQUEST_CODE_PLAY_PAUSE,
            getApplication(),
            MediaNotificationListenerService::class.java
        )
    }

    private val prevMusicAction by lazy {
        createAction(
            android.R.drawable.ic_media_previous,
            "Previous",
            MediaStateManager.ACTION_SKIP_TO_PREVIOUS,
            MediaStateManager.REQUEST_CODE_PREVIOUS,
            getApplication(),
            MediaNotificationListenerService::class.java
        )
    }
    private val nextMusicAction by lazy {
        createAction(
            android.R.drawable.ic_media_next,
            "Next",
            MediaStateManager.ACTION_SKIP_TO_NEXT,
            MediaStateManager.REQUEST_CODE_NEXT,
            getApplication(),
            MediaNotificationListenerService::class.java
        )
    }
}
