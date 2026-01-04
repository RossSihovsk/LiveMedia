package com.ross.livemedia.settings

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import androidx.localbroadcastmanager.content.LocalBroadcastManager

private const val TAG = "SystemUiStateService"

class SystemUiStateService : AccessibilityService() {

    val localBroadcastManager = LocalBroadcastManager.getInstance(this)

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_WINDOWS_CHANGED
        ) return

        val windows = windows
        var isQsOpen = false

        for (window in windows) {
            if (window.root == null) continue
            val pkgName = window.root?.packageName?.toString()
            if (pkgName == SYSTEM_UI_PACKAGE) {
                val displayMetrics = resources.displayMetrics
                val screenHeight = displayMetrics.heightPixels

                val outBounds = android.graphics.Rect()
                window.getBoundsInScreen(outBounds)

                val windowHeight = outBounds.height()

                if (windowHeight > screenHeight / 2) {
                    isQsOpen = true
                }
                break
            }
        }

        if (isQsOpen) {
            Log.d(TAG, "🔽 Quick Settings or Notification Shade opened")
            localBroadcastManager.sendBroadcast(Intent(QS_OPENED_ACTION))
        } else {
            Log.d(TAG, "🔼 Quick Settings closed")
            localBroadcastManager.sendBroadcast(Intent(QS_CLOSED_ACTION))
        }
    }

    override fun onInterrupt() {}

    companion object {
        const val QS_OPENED_ACTION = "com.ross.livemedia.QS_OPENED"
        const val QS_CLOSED_ACTION = "com.ross.livemedia.QS_CLOSED"
        private const val SYSTEM_UI_PACKAGE = "com.android.systemui"
    }
}
