package com.ross.livemedia

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent // <--- Import is key
import android.content.ComponentName
import android.content.Intent // <--- Import is key
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat

class MediaNotificationListenerService : NotificationListenerService() {

    private val TAG = "MediaListenerService"
    private val NOTIFICATION_ID = 1337
    private val CHANNEL_ID = "MediaLiveUpdateChannel"

    private var activeMediaController: MediaController? = null
    private var currentTrackTitle: String? = null
    private var curretPackagePlaying: String? = null

    // --- NEW: Custom Action Constants for Button Presses ---
    companion object {
        const val ACTION_PLAY_PAUSE = "com.ross.livemedia.ACTION_PLAY_PAUSE"
        const val ACTION_SKIP_TO_NEXT = "com.ross.livemedia.ACTION_SKIP_TO_NEXT"
        const val ACTION_SKIP_TO_PREVIOUS = "com.ross.livemedia.ACTION_SKIP_TO_PREVIOUS"
        const val REQUEST_CODE_PLAY_PAUSE = 100
        const val REQUEST_CODE_NEXT = 101
        const val REQUEST_CODE_PREVIOUS = 102
    }
    // --------------------------------------------------------

    private val mediaControllerCallback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            super.onPlaybackStateChanged(state)
            Log.d(TAG, "Playback state changed: $state")
            updateNotification()
        }

        override fun onMetadataChanged(metadata: MediaMetadata?) {
            super.onMetadataChanged(metadata)
            val newTitle = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
            Log.d(TAG, "Metadata changed. New title: $newTitle")
            if (newTitle != currentTrackTitle) {
                currentTrackTitle = newTitle
                updateNotification()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        Log.d(TAG, "Service Created")
    }

    // --- NEW: Handle commands from notification buttons ---
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            val controller = activeMediaController
            if (controller != null) {
                when (intent.action) {
                    ACTION_PLAY_PAUSE -> {
                        val playbackState = controller.playbackState
                        if (playbackState?.state == PlaybackState.STATE_PLAYING) {
                            controller.transportControls.pause()
                        } else {
                            controller.transportControls.play()
                        }
                    }
                    ACTION_SKIP_TO_NEXT -> {
                        controller.transportControls.skipToNext()
                    }
                    ACTION_SKIP_TO_PREVIOUS -> {
                        controller.transportControls.skipToPrevious()
                    }
                }
                updateNotification()
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "Listener Connected")
        findActiveMediaController()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn?.notification?.category == Notification.CATEGORY_TRANSPORT) {
            Log.d(TAG, "Media notification detected from: ${sbn.packageName}")
            findActiveMediaController()
        }
    }

    private fun findActiveMediaController() {
        // ... (findActiveMediaController implementation remains the same)
        val mediaSessionManager = getSystemService(MEDIA_SESSION_SERVICE) as MediaSessionManager
        val componentName = ComponentName(this, MediaNotificationListenerService::class.java)
        val controllers = mediaSessionManager.getActiveSessions(componentName)

        if (controllers.isNotEmpty()) {
            val newController = controllers[0]
            if (newController != activeMediaController) {
                activeMediaController?.unregisterCallback(mediaControllerCallback)
                activeMediaController = newController
                activeMediaController?.registerCallback(mediaControllerCallback)
                Log.d(TAG, "Found and registered new media controller: ${newController.packageName}")
                curretPackagePlaying = newController.packageName
                updateNotification()
            }
        } else {
            clearNotification()
            activeMediaController?.unregisterCallback(mediaControllerCallback)
            activeMediaController = null
            curretPackagePlaying = null
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun updateNotification() {
        val controller = activeMediaController ?: return
        val metadata = controller.metadata ?: return
        val playbackState = controller.playbackState ?: return

        val title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "Unknown Title"
        val artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: "Unknown Artist"
        val albumArtBitmap = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)

        val isPlaying = playbackState.state == PlaybackState.STATE_PLAYING


        // --- 1. Define Intents using the existing logic ---
        val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT

        // Previous Intent
        val prevIntent = Intent(this, MediaNotificationListenerService::class.java).apply {
            action = ACTION_SKIP_TO_PREVIOUS
        }
        val prevPendingIntent = PendingIntent.getService(this, REQUEST_CODE_PREVIOUS, prevIntent, flags)

        // Play/Pause Intents
        // NOTE: ActionType is not used here, only for clarity in the old code.
        val playPauseIntent = Intent(this, MediaNotificationListenerService::class.java).apply {
            action = ACTION_PLAY_PAUSE
        }
        val playPausePendingIntent = PendingIntent.getService(this, REQUEST_CODE_PLAY_PAUSE, playPauseIntent, flags)

        // Next Intent
        val nextIntent = Intent(this, MediaNotificationListenerService::class.java).apply {
            action = ACTION_SKIP_TO_NEXT
        }
        val nextPendingIntent = PendingIntent.getService(this, REQUEST_CODE_NEXT, nextIntent, flags)


        // --- 2. Use Android System Icons (android.R.drawable) ---

        // Use the stock system icon for small icon (e.g., a simple star or volume icon)
        // You can also use a simple transparent icon you add, but this eliminates the risk.
        val smallIcon = android.R.drawable.ic_media_play

        val previousIcon = android.R.drawable.ic_media_previous
        val playPauseIcon = if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        val nextIcon = android.R.drawable.ic_media_next

        val playPauseTitle = if (isPlaying) "Pause" else "Play"


        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(smallIcon)
            .setContentTitle(artist)
            .setContentText(title)
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_PROGRESS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setShortCriticalText(title.take(7))

            .addAction(previousIcon, "Previous", prevPendingIntent)
            .addAction(playPauseIcon, playPauseTitle, playPausePendingIntent)
            .addAction(nextIcon, "Next", nextPendingIntent)
            .setRequestPromotedOngoing(true)

        if (albumArtBitmap != null) {
            builder.setLargeIcon(albumArtBitmap)
        }

        builder.setSubText(if (isPlaying) "Playing" else "Paused")

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }

    private fun clearNotification() {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Media Live Updates",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        activeMediaController?.unregisterCallback(mediaControllerCallback)
    }
}