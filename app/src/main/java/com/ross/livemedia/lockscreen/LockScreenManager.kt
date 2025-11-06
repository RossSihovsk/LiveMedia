package com.ross.livemedia.lockscreen

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.ross.livemedia.utils.Logger

private const val TAG = "LockScreenManager"

class LockScreenManager(
    private val context: Context,
    private val deviceLocked: () -> Unit,
    private val deviceUnlocked: () -> Unit
) {
    private val lockStateReceiver = LockStateReceiver()
    private val logger = Logger(TAG)

    fun start() {
        logger.debug("start and registerReceiver lockStateReceiver")
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        context.registerReceiver(lockStateReceiver, filter)
    }

    fun stop() {
        logger.debug("stop and unregisterReceiver lockStateReceiver")
        context.unregisterReceiver(lockStateReceiver)
    }

    fun isScreenUnlocked(): Boolean {
        val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        return !(keyguardManager?.isDeviceLocked ?: false)
    }

    private inner class LockStateReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    logger.debug("Screen OFF broadcast received.")
                    deviceLocked()
                }

                Intent.ACTION_USER_PRESENT -> {
                    logger.debug("User PRESENT broadcast received.")
                    deviceUnlocked()
                }
            }
        }
    }
}