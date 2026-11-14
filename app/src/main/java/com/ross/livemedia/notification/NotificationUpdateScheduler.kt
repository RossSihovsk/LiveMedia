package com.ross.livemedia.notification

import android.os.Handler
import android.os.Looper
import com.ross.livemedia.utils.Logger

class NotificationUpdateScheduler(
    private val nextDelayProvider: () -> Long?
) {
    private val logger = Logger("NotificationUpdateScheduler")
    private val updateHandler = Handler(Looper.getMainLooper())

    private val updateRunnable = object : Runnable {
        override fun run() {
            val delay = nextDelayProvider()
            if (delay != null && delay > 0) {
                updateHandler.postDelayed(this, delay)
            } else {
                logger.info("Runnable stopped.")
            }
        }
    }

    fun scheduleUpdate() {
        // Cancel any existing run to avoid duplicates
        updateHandler.removeCallbacks(updateRunnable)
        // Schedule the next update after the specified delay instead of immediate post
        val delay = nextDelayProvider()
        if (delay != null && delay > 0) {
            updateHandler.postDelayed(updateRunnable, delay)
        }
    }

    fun stop() {
        updateHandler.removeCallbacks(updateRunnable)
    }
}