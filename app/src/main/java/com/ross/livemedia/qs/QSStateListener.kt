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
private const val RECENTS_SCAN_INTERVAL_MS = 500L

class QSStateListener : AccessibilityService() {

    private var isRecentsOpen = false
    private var recentsPackage: String? = null
    private var cardVisible = false
    private var closeReported = false
    private var lastScanAt = 0L
    private var pendingCloseCheck: Runnable? = null
    private var recentsGeneration = 0L
    private var recentsCardWasPresent = false

    private val mainHandler = Handler(Looper.getMainLooper())

    private val recentsScanRunnable = object : Runnable {
        override fun run() {
            val generation = recentsGeneration
            if (!isRecentsOpen || generation != recentsGeneration) return
            // The tree walk talks to the accessibility framework over Binder and
            // can throw (e.g. while the event stream is stalled). Never let an
            // exception kill the loop: the reschedule must always happen.
            try {
                scanForMediaAppCard()
            } catch (e: Exception) {
                Log.w(TAG, "Recents scan failed", e)
            }
            mainHandler.postDelayed(this, RECENTS_SCAN_INTERVAL_MS)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i(TAG, "onServiceConnected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        try {
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
        } catch (e: Exception) {
            // The tree walks talk to the accessibility framework over Binder and
            // can throw while the event stream is stalled; never crash the
            // service because of it.
            Log.w(TAG, "Event handling failed", e)
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
            Log.i(TAG, "Recents opened pkg=$packageName")
            isRecentsOpen = true
            recentsPackage = packageName
            closeReported = false
            recentsGeneration++
            mainHandler.removeCallbacks(recentsScanRunnable)

            // Baseline for the real-time swipe detection and the late fallback.
            // The first scan also fires the fallback: if the tracked app's card
            // was present in the previous recents session but is gone now, it
            // was closed from recents (Samsung sometimes never delivers the
            // card-removal events, so the card can only be missed on the next
            // visit to the recents screen).
            val wasPresent = recentsCardWasPresent
            val appLabel = tracked?.let { resolveAppLabel(it) }
            val isPresent = appLabel?.let { isMediaAppCardVisible(it) } ?: false
            recentsCardWasPresent = isPresent
            cardVisible = isPresent
            if (wasPresent && !isPresent) {
                val closedPackage = tracked
                if (closedPackage != null) {
                    Log.i(TAG, "Media app card no longer in recents: $closedPackage")
                    MediaAppRecentsProvider.onMediaAppClosedFromRecents(closedPackage)
                }
            }

            mainHandler.postDelayed(recentsScanRunnable, RECENTS_SCAN_INTERVAL_MS)
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

        // Samsung does not always emit content-changed events when a card is
        // swiped away, so scan on window changes too. Do NOT reset the recents
        // state here: the a11y window list is incomplete during transitions and
        // the recents window only truly closes on a state change away from it.
        if (windows.any { window -> window.root?.packageName?.toString() == recentsPackage }) {
            scanForMediaAppCard()
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
        // The recents window must be fully rendered to tell a real card removal
        // (swipe / close-all) apart from a teardown: while the window closes,
        // the a11y tree still exposes a root, but its content (task cards, the
        // "N active apps" counter, the Close all button) is already detached.
        val recentsWindowValid = windows.any { window ->
            val root = window.root
            root != null &&
                root.packageName?.toString() == recentsPackage &&
                recentsStructureIntact(root)
        }
        val visible = recentsWindowValid && isMediaAppCardVisible(label)
        if (visible) {
            if (!cardVisible) Log.i(TAG, "Media app card visible: $tracked")
            cardVisible = true
            pendingCloseCheck?.let { mainHandler.removeCallbacks(it) }
            pendingCloseCheck = null
        } else if (cardVisible && !closeReported && recentsWindowValid) {
            // The card left the tree while the recents window was still fully
            // present (swiped away or "Close all"). Debounce to ignore scroll
            // flicks. A disappearing window (root gone, recents closing) is NOT
            // treated as a card removal: it fires the reset path instead.
            Log.i(TAG, "Media app card disappeared: $tracked")
            pendingCloseCheck?.let { mainHandler.removeCallbacks(it) }
            val closeCheck = Runnable {
                pendingCloseCheck = null
                // Note: do not require the recents window to still be open here.
                // The recents may close right after the swipe, but the card is
                // genuinely gone, so the disappearance still stands.
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

    private fun recentsStructureIntact(root: AccessibilityNodeInfo): Boolean {
        if (findNodeWithText(root, "Close all")) return true
        return findNodeMatchingText(root) { text -> TASK_COUNTER_REGEX.matches(text) }
    }

    private fun findNodeMatchingText(
        node: AccessibilityNodeInfo?,
        predicate: (String) -> Boolean
    ): Boolean {
        if (node == null) return false
        val text = node.text?.toString()
        if (text != null && predicate(text)) return true
        for (i in 0 until node.childCount) {
            if (findNodeMatchingText(node.getChild(i), predicate)) return true
        }
        return false
    }

    private fun resetRecentsState() {
        // Do NOT cancel pendingCloseCheck here: a card swipe is often followed
        // by the recents screen closing within the debounce window. The pending
        // re-check only fires when the card is actually gone, so letting it run
        // after the recents closed is safe (it just re-scans the window list).
        mainHandler.removeCallbacks(recentsScanRunnable)
        recentsGeneration++
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
        private val TASK_COUNTER_REGEX = Regex("\\d+ active apps?")
    }
}