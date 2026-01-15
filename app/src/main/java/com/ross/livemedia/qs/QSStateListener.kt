package com.ross.livemedia.qs

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent

private const val TAG = "SystemUiStateService"

class QSStateListener : AccessibilityService() {

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
                val windowWidth = outBounds.width()
                val screenWidth = displayMetrics.widthPixels
                val childCount = window.root?.childCount ?: 0

                // QS/Notification shade is a complex view with multiple children (header, QS tiles, notifications, etc).
                // Screen overlays like Screenshot UI usually have a simpler hierarchy (e.g., childCount = 1).
                if (windowHeight > screenHeight / 2 && 
                    windowWidth > screenWidth * 0.9 && 
                    childCount > 2
                ) {
                    isQsOpen = true
                }
                break
            }
        }

        Log.d(TAG, "Is QS opened: $isQsOpen")
        QSStateProvider.updateQsState(isQsOpen)
    }

    override fun onInterrupt() {}

    companion object {
        private const val SYSTEM_UI_PACKAGE = "com.android.systemui"
    }
}
