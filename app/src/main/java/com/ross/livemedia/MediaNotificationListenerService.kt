package com.ross.livemedia

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Intent
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Build
import android.provider.Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import androidx.core.widget.ListViewAutoScrollHelper

class MediaNotificationListenerService : NotificationListenerService() {

    private val TAG = "MediaListenerService"
    private val NOTIFICATION_ID = 1337
    private val CHANNEL_ID = "MediaLiveUpdateChannel"

    private var activeMediaController: MediaController? = null
    private var currentTrackTitle: String? = null

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

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "Listener Connected")
        // Find any active media sessions when the service first connects
        findActiveMediaController()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        // A new notification was posted. Check if it's a media session.
        // This is a simple way to trigger a check.
        if (sbn?.notification?.category == Notification.CATEGORY_TRANSPORT) {
            Log.d(TAG, "Media notification detected from: ${sbn.packageName}")
            findActiveMediaController()
        }
    }

    private fun findActiveMediaController() {
        val mediaSessionManager = getSystemService(MEDIA_SESSION_SERVICE) as MediaSessionManager
        val componentName = ComponentName(this, MediaNotificationListenerService::class.java)
        val controllers = mediaSessionManager.getActiveSessions(componentName)

        if (controllers.isNotEmpty()) {
            val newController = controllers[0]
            if (newController != activeMediaController) {
                // Unregister from old controller
                activeMediaController?.unregisterCallback(mediaControllerCallback)
                // Register for new one
                activeMediaController = newController
                activeMediaController?.registerCallback(mediaControllerCallback)
                Log.d(TAG, "Found and registered new media controller: ${newController.packageName}")
                // Initial update
                updateNotification()
            }
        } else {
            // No active sessions, clear our notification
            clearNotification()
            activeMediaController?.unregisterCallback(mediaControllerCallback)
            activeMediaController = null
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

        // Build the notification that will act as our "Live Update"
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(artist)
            .setContentText(title)
            .setOngoing(true) // 1. Must be ongoing
            .setCategory(Notification.CATEGORY_PROGRESS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setShortCriticalText(title)

            // 2. The critical step for Live Updates (Promotion Request)
            .setRequestPromotedOngoing(true)

        if (albumArtBitmap != null) {
            builder.setLargeIcon(albumArtBitmap)
        }

        // This is a simplified way to show play/pause. A real app would use custom actions.
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
                NotificationManager.IMPORTANCE_LOW // Use low importance to be less intrusive
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

