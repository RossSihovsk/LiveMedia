//package com.ross.livemedia
//
//import android.accessibilityservice.AccessibilityService
//import android.view.accessibility.AccessibilityEvent
//import android.content.Intent
//import android.util.Log
//import androidx.localbroadcastmanager.content.LocalBroadcastManager
//
//class SystemUiStateService : AccessibilityService() {
//
//    val localBroadcastManager = LocalBroadcastManager.getInstance(this)
//
//    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
//        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
//        val pkg = event.packageName?.toString() ?: return
//        val cls = event.className?.toString() ?: return
//
//        Log.d("SystemUiStateService", "🔽onAccessibilityEvent $pkg $cls")
//        if (pkg == "com.android.systemui" && cls.contains(
//                "android.widget.FrameLayout",
//                ignoreCase = true
//            )
//        ) {
//            Log.d("SystemUiStateService", "🔽 Quick Settings or Notification Shade opened send ")
//            localBroadcastManager.sendBroadcast(
//                Intent(
//                    "com.ross.livemedia.QS_OPENED"
//                )
//            )
//            Log.d("SystemUiStateService", "🔽done send ")
//        } else {
//            Log.d("SystemUiStateService", "🔼 Quick Settings closed")
//            localBroadcastManager.sendBroadcast(Intent("com.ross.livemedia.QS_CLOSED"))
//        }
//    }
//
//    override fun onInterrupt() {}
//}
