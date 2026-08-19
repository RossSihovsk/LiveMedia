package com.ross.livemedia.qs

import android.accessibilityservice.AccessibilityService
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

private const val TAG = "SystemUiStateService"

private const val MIN_SCAN_INTERVAL_MS = 250L
private const val CARD_GONE_DEBOUNCE_MS = 500L

class QSStateListener : AccessibilityService() {

    private var isRecentsOpen = false
    private var recentsPackage: String? = null
    private var cardVisible = false
    private var closeReported = false
    private var lastScanAt = 0L
    private var pendingCloseCheck: Runnable? = null

    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i(TAG, "onServiceConnected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                updateQsState()
                handleWindowStateChanged(event)
            }
            AccessibilityEvent.TYPE_WINDOWS_CHANGED -> {
                updateQsState()
                handleWindowsChanged(event)
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                handleContentChanged(event)
            }
            AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                handleViewClicked(event)
            }
        }
    }

    private fun handleViewClicked(event: AccessibilityEvent) {
        if (!isRecentsOpen) return
        val tracked = MediaAppRecentsProvider.trackedPackage ?: return
        val source = event.source ?: return
        val label = resolveAppLabel(tracked) ?: return

        val resourceId = source.viewIdResourceName?.toString()
        val text = source.text?.toString()
        val contentDesc = source.contentDescription?.toString()

        if (resourceId?.contains("clear_all") == true ||
            text?.equals("Close all", ignoreCase = true) == true ||
            contentDesc?.equals("Close all", ignoreCase = true) == true
        ) {
            // The user tapped "Close all" in the recents screen.
            Log.i(TAG, "Close all tapped. Media app closed: $tracked")
            closeReported = true
            MediaAppRecentsProvider.onMediaAppClosedFromRecents(tracked)
            resetRecentsState()
        } else if (text?.equals(label, ignoreCase = true) == true ||
            contentDesc?.equals(label, ignoreCase = true) == true
        ) {
            // The user tapped the media app's card.
            Log.i(TAG, "Media app card tapped: $tracked")
            MediaAppRecentsProvider.onMediaAppReopened(tracked)
            resetRecentsState()
        }
    }

    private fun handleWindowStateChanged(event: AccessibilityEvent) {
        val className = event.className?.toString() ?: return
        val packageName = event.packageName?.toString() ?: return

        val tracked = MediaAppRecentsProvider.trackedPackage

        if (className.contains("Recents", ignoreCase = true)) {
            isRecentsOpen = true
            recentsPackage = packageName
            cardVisible = false
            closeReported = false
            scanForMediaAppCard()
            return
        }

        if (isRecentsOpen) {
            if (tracked != null && packageName == tracked) {
                // The user opened the media app from recents.
                MediaAppRecentsProvider.onMediaAppReopened(tracked)
            }
            // Closing from the recents screen never means the app was closed:
            // the app card disappearing is detected via content changes while
            // the recents window is still open.
            resetRecentsState()
        } else if (tracked != null && packageName == tracked) {
            // The media app came back to the foreground from anywhere.
            MediaAppRecentsProvider.onMediaAppReopened(tracked)
        }
    }

    private fun handleWindowsChanged(event: AccessibilityEvent) {
        if (!isRecentsOpen) return

        // The recents window may disappear without a state change to a
        // non-recents window (e.g. quick gesture dismiss).
        val recentsWindowStillPresent = windows.any { window ->
            window.root?.packageName?.toString() == recentsPackage
        }
        if (!recentsWindowStillPresent) {
            resetRecentsState()
        }
    }

    private fun handleContentChanged(event: AccessibilityEvent) {
        if (!isRecentsOpen) return
        if (event.packageName?.toString() != recentsPackage) return
        scanForMediaAppCard()
    }

    private fun scanForMediaAppCard() {
        val now = SystemClock.elapsedRealtime()
        if (now - lastScanAt < MIN_SCAN_INTERVAL_MS) return
        lastScanAt = now

        val tracked = MediaAppRecentsProvider.trackedPackage ?: return
        val label = resolveAppLabel(tracked) ?: return

        val visible = isMediaAppCardVisible(label)
        if (visible) {
            cardVisible = true
            pendingCloseCheck?.let { mainHandler.removeCallbacks(it) }
            pendingCloseCheck = null
        } else if (cardVisible && !closeReported) {
            // The card disappeared while the recents window was still open
            // (swiped away or "Close all"). Debounce to ignore scroll flicks.
            pendingCloseCheck?.let { mainHandler.removeCallbacks(it) }
            val closeCheck = Runnable {
                pendingCloseCheck = null
                if (!isRecentsOpen) return@Runnable
                val stillGone = !isMediaAppCardVisible(label)
                if (stillGone && !closeReported) {
                    closeReported = true
                    Log.i(TAG, "Media app closed from recents: $tracked")
                    MediaAppRecentsProvider.onMediaAppClosedFromRecents(tracked)
                }
            }
            pendingCloseCheck = closeCheck
            mainHandler.postDelayed(closeCheck, CARD_GONE_DEBOUNCE_MS)
        }
    }

    private fun isMediaAppCardVisible(label: String): Boolean {
        for (window in windows) {
            if (window.root == null) continue
            if (window.root?.packageName?.toString() != recentsPackage) continue
            if (findNodeWithText(window.root, label)) return true
        }
        return false
    }

    private fun findNodeWithText(node: AccessibilityNodeInfo?, label: String): Boolean {
        if (node == null) return false
        val text = node.text?.toString()
        if (text != null &&
            (text.equals(label, ignoreCase = true) || text.startsWith(label, ignoreCase = true))
        ) {
            return true
        }
        for (i in 0 until node.childCount) {
            if (findNodeWithText(node.getChild(i), label)) return true
        }
        return false
    }

    private fun resetRecentsState() {
        pendingCloseCheck?.let { mainHandler.removeCallbacks(it) }
        pendingCloseCheck = null
        isRecentsOpen = false
        recentsPackage = null
        cardVisible = false
        closeReported = false
    }

    private fun resolveAppLabel(packageName: String): String? {
        return try {
            val packageManager = packageManager
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    private fun updateQsState() {
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

        Log.i(TAG, "Is QS opened: $isQsOpen")
        QSStateProvider.updateQsState(isQsOpen)
    }

    override fun onInterrupt() {}

    companion object {
        private const val SYSTEM_UI_PACKAGE = "com.android.systemui"
    }
}