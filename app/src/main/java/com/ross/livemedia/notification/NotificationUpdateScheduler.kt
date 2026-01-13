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
            if (delay != null) {
                updateHandler.postDelayed(this, delay)
            } else {
                logger.info("Runnable stopped.")
            }
        }
    }

    fun scheduleUpdate() {
        // Cancel any existing run to avoid duplicates
        updateHandler.removeCallbacks(updateRunnable)
        // Run immediately to update and schedule next
        updateHandler.post(updateRunnable)
    }

    fun stop() {
        updateHandler.removeCallbacks(updateRunnable)
    }
}