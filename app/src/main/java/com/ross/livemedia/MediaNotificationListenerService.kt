package com.ross.livemedia

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
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

    private var activeMediaController: MediaController? = null
    private var currentTrackTitle: String? = null
    private var currentPackagePlaying: String? = null

    companion object {
        private const val NOTIFICATION_ID = 1337
        private const val CHANNEL_ID = "MediaLiveUpdateChannel"
        private const val ACTION_PLAY_PAUSE = "com.ross.livemedia.ACTION_PLAY_PAUSE"
        private const val ACTION_SKIP_TO_NEXT = "com.ross.livemedia.ACTION_SKIP_TO_NEXT"
        private const val ACTION_SKIP_TO_PREVIOUS = "com.ross.livemedia.ACTION_SKIP_TO_PREVIOUS"
        private const val REQUEST_CODE_PLAY_PAUSE = 100
        private const val REQUEST_CODE_NEXT = 101
        private const val REQUEST_CODE_PREVIOUS = 102
    }

    private val mediaControllerCallback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            Log.d(TAG, "Playback state changed: $state")
            updateNotification()
        }

        override fun onMetadataChanged(metadata: MediaMetadata?) {
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

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.action?.let { action ->
            activeMediaController?.transportControls?.let {
                when (action) {
                    ACTION_PLAY_PAUSE -> if (activeMediaController?.playbackState?.state == PlaybackState.STATE_PLAYING) it.pause() else it.play()
                    ACTION_SKIP_TO_NEXT -> it.skipToNext()
                    ACTION_SKIP_TO_PREVIOUS -> it.skipToPrevious()
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
        if (sbn?.notification?.category == Notification.CATEGORY_TRANSPORT) {
            Log.d(TAG, "Media notification detected from: ${sbn.packageName}")
            findActiveMediaController()
        }
    }

    private fun findActiveMediaController() {
        val mediaSessionManager = getSystemService(MEDIA_SESSION_SERVICE) as MediaSessionManager
        val componentName = ComponentName(this, this::class.java)
        val controllers = mediaSessionManager.getActiveSessions(componentName)

        controllers.firstOrNull().let { newController ->
            if (newController != activeMediaController) {
                activeMediaController?.unregisterCallback(mediaControllerCallback)
                activeMediaController = newController?.also {
                    it.registerCallback(mediaControllerCallback)
                    Log.d(TAG, "Found and registered new media controller: ${it.packageName}")
                    currentPackagePlaying = it.packageName
                    updateNotification()
                }
            }
        }

        if (controllers.isEmpty()) {
            clearNotification()
            activeMediaController = null
            currentPackagePlaying = null
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun updateNotification() {
        activeMediaController?.let { controller ->
            val metadata = controller.metadata ?: return
            val playbackState = controller.playbackState ?: return
            val isPlaying = playbackState.state == PlaybackState.STATE_PLAYING

            val title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "Unknown Title"
            val artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: "Unknown Artist"

            val notification = buildNotification(title, artist, isPlaying, metadata)
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(title: String, artist: String, isPlaying: Boolean, metadata: MediaMetadata): Notification {
        val playPauseAction = createAction(
            if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
            if (isPlaying) "Pause" else "Play",
            ACTION_PLAY_PAUSE,
            REQUEST_CODE_PLAY_PAUSE
        )
        val prevAction = createAction(android.R.drawable.ic_media_previous, "Previous", ACTION_SKIP_TO_PREVIOUS, REQUEST_CODE_PREVIOUS)
        val nextAction = createAction(android.R.drawable.ic_media_next, "Next", ACTION_SKIP_TO_NEXT, REQUEST_CODE_NEXT)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(artist)
            .setContentText(title)
            .setLargeIcon(metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART))
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_PROGRESS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setShortCriticalText(title.take(7))
            .addAction(prevAction)
            .addAction(playPauseAction)
            .addAction(nextAction)
            .setRequestPromotedOngoing(true)
            .setSubText(if (isPlaying) "Playing" else "Paused")
            .build()
    }

    private fun createAction(icon: Int, title: String, action: String, requestCode: Int): NotificationCompat.Action {
        val intent = Intent(this, this::class.java).setAction(action)
        val pendingIntent = PendingIntent.getService(this, requestCode, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        return NotificationCompat.Action(icon, title, pendingIntent)
    }

    private fun clearNotification() {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Media Live Updates", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        activeMediaController?.unregisterCallback(mediaControllerCallback)
        activeMediaController = null
        currentPackagePlaying = null
        clearNotification()
    }
}