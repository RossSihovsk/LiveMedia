package com.ross.livemedia.settings

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.content.Intent
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager

private const val TAG = "SystemUiStateService"

class SystemUiStateService : AccessibilityService() {

    val localBroadcastManager = LocalBroadcastManager.getInstance(this)

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        val cls = event.className?.toString() ?: return

        Log.d(TAG, "🔽onAccessibilityEvent $pkg $cls")
        if (pkg == SYSTEM_UI_PACKAGE && cls.contains(FRAME_LAYOUT_PACKAGE, ignoreCase = true)
        ) {
            Log.d(TAG, "🔽 Quick Settings or Notification Shade opened send ")
            localBroadcastManager.sendBroadcast(
                Intent(QS_OPENED_ACTION)
            )
            Log.d(TAG, "🔽done send ")
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
        private const val FRAME_LAYOUT_PACKAGE = "android.widget.FrameLayout"
    }
}
