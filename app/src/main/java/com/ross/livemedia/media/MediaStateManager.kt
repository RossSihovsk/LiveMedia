package com.ross.livemedia.media

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import com.ross.livemedia.notification.MediaNotificationListenerService
import com.ross.livemedia.utils.Logger

class MediaStateManager(
    private val context: Context,
    private val onStateUpdated: (MusicState) -> Unit,
    private val noActiveMedia: () -> Unit,
) {
    private val logger = Logger("MediaManager")
    private var activeMediaController: MediaController? = null
    private var currentState: MusicState? = null

    private val mediaControllerCallback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            handleOnPlaybackStateChanged(state = state)
        }

        override fun onMetadataChanged(metadata: MediaMetadata?) {
            handleOnMetadataChanged(metadata)
        }

        override fun onSessionDestroyed() {
            handleSessionDestroyed()
        }
    }

    private fun handleSessionDestroyed() {
        logger.info("Media session destroyed")
        activeMediaController?.unregisterCallback(mediaControllerCallback)
        activeMediaController = null
        currentState = null
        maybeUpdateMediaController()
    }

    init {
        logger.info("start")
        maybeUpdateMediaController()
    }

    fun getUpdatedMusicState(
        metadata: MediaMetadata? = null,
        state: PlaybackState? = null
    ): MusicState? {
        activeMediaController?.let { controller ->
            val metadata = metadata ?: controller.metadata
            val playbackState = state ?: controller.playbackState

            if (playbackState == null) {
                logger.info("playbackState == null")

                return null
            }

            if (playbackState.state == PlaybackState.STATE_STOPPED || playbackState.state == PlaybackState.STATE_NONE) {
                logger.info("STATE_STOPPED")

                return null
            }

            if (metadata != null) {
                val newState = MusicState(metadata, playbackState, controller.packageName)
                return newState
            }
        }

        return null
    }

    fun maybeUpdateMediaController() {
        val controllers = getActiveSessions() ?: return

        // If the session we are tracking is no longer in the active list,
        // the media app was killed/removed. Clear the state regardless of
        // whether other (unrelated) sessions still exist.
        val activeToken = activeMediaController?.sessionToken
        if (activeToken != null && controllers.none { it.sessionToken == activeToken }) {
            logger.info("Active media session no longer exists. Notify that the media is stopped")
            activeMediaController?.unregisterCallback(mediaControllerCallback)
            activeMediaController = null
            currentState = null
            noActiveMedia()
            return
        }

        val newController = controllers.firstOrNull()

        // MediaController does not override equals(), so compare session tokens
        // to detect whether the active session actually changed.
        if (newController?.sessionToken != activeMediaController?.sessionToken) {
            activeMediaController?.unregisterCallback(mediaControllerCallback)
            activeMediaController = newController?.also {
                it.registerCallback(mediaControllerCallback)
                logger.info("Found and registered new media controller: ${it.packageName}")
                pushCurrentState()
            }
        }

        // If no controllers are found, notify that the media is stopped
        if (controllers.isEmpty()) {
            logger.info("No controllers are found, notify that the media is stopped")
            activeMediaController?.unregisterCallback(mediaControllerCallback)
            activeMediaController = null
            currentState = null
            noActiveMedia()
        }
    }

    private fun getActiveSessions(): List<MediaController>? {
        return try {
            val mediaSessionManager =
                context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
            val componentName = ComponentName(context, MediaNotificationListenerService::class.java)
            mediaSessionManager.getActiveSessions(componentName)
        } catch (e: SecurityException) {
            logger.e("Cannot access active media sessions", e)
            null
        }
    }

    fun handleTransportControl(action: String?) {
        logger.info("handleTransportControl action: $action")
        if (action == null) return

        // Ensure we have the latest valid controller before attempting to control it
        maybeUpdateMediaController()

        activeMediaController?.transportControls?.let { controls ->
            when (action) {
                ACTION_PLAY_PAUSE -> if (activeMediaController?.playbackState?.state == PlaybackState.STATE_PLAYING) controls.pause() else controls.play()
                ACTION_SKIP_TO_NEXT -> controls.skipToNext()
                ACTION_SKIP_TO_PREVIOUS -> controls.skipToPrevious()
            }
        }
    }

    private fun pushCurrentState() {
        logger.info("pushCurrentState")

        val newState = getUpdatedMusicState()

        if (newState != null && newState != currentState) {
            currentState = newState
            logger.info("State was updated to: $newState")
            onStateUpdated(newState)
        }
    }

    private fun handleOnMetadataChanged(metadata: MediaMetadata?) {
        val newState = getUpdatedMusicState(metadata = metadata)

        logger.info("old state: $currentState")
        logger.info("newState: $newState")

        if (currentState != newState) {
            logger.info("Some changes")
            pushCurrentState()
        }
    }

    private fun handleOnPlaybackStateChanged(state: PlaybackState?) {
        logger.info("Playback state changed: ${state?.state}")
        val newState = getUpdatedMusicState(state = state)

        logger.info("old state: $currentState")
        logger.info("newState: $newState")

        if (currentState != newState) {
            logger.info("Some changes")
            pushCurrentState()
        }
    }


    companion object {
        const val ACTION_PLAY_PAUSE = "com.ross.livemedia.ACTION_PLAY_PAUSE"
        const val ACTION_SKIP_TO_NEXT = "com.ross.livemedia.ACTION_SKIP_TO_NEXT"
        const val ACTION_SKIP_TO_PREVIOUS = "com.ross.livemedia.ACTION_SKIP_TO_PREVIOUS"
        const val REQUEST_CODE_PLAY_PAUSE = 100
        const val REQUEST_CODE_NEXT = 101
        const val REQUEST_CODE_PREVIOUS = 102
    }
}