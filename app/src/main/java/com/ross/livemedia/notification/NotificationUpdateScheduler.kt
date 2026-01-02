package com.ross.livemedia.notification

import android.os.Handler
import android.os.Looper
import com.ross.livemedia.media.MusicState
import com.ross.livemedia.utils.Logger

private const val STATE_UPDATE_DELAY_MS = 1000L

class NotificationUpdateScheduler(
    private val updateAction: () -> Unit
) {
    private val logger = Logger("NotificationUpdateScheduler")
    private val updateHandler = Handler(Looper.getMainLooper())
    private var isPlaying = false

    private val updateRunnable = object : Runnable {
        override fun run() {
            if (isPlaying) {
                updateAction()
                updateHandler.postDelayed(this, STATE_UPDATE_DELAY_MS)
            } else {
                logger.info("Runnable stopped: Not playing.")
            }
        }
    }

    fun scheduleUpdate(state: MusicState) {
        updateAction()

        val wasPlaying = isPlaying
        isPlaying = state.isPlaying

        if (isPlaying && !wasPlaying) {
            logger.info("Started playing, scheduling periodic updates.")
            updateHandler.removeCallbacks(updateRunnable)
            updateHandler.postDelayed(updateRunnable, 2000)
        } else if (!isPlaying) {
            logger.info("Paused/Stopped, removing periodic updates.")
            updateHandler.removeCallbacks(updateRunnable)
        }
    }
}