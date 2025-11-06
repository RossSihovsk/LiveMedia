package com.ross.livemedia.settings

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.ross.livemedia.settings.SystemUiStateService.Companion.QS_CLOSED_ACTION
import com.ross.livemedia.settings.SystemUiStateService.Companion.QS_OPENED_ACTION

private const val TAG = "QSManager"

class QSManager(
    private val context: Context,
    private val settingsOpened: () -> Unit,
    private val settingsClosed: () -> Unit

) {
    @Volatile
    private var isQuickSettingsOpen = false

    private val qsReceiver = SettingsReceiver()
    private val localBroadcastManager by lazy { LocalBroadcastManager.getInstance(context) }

    fun start() {

        val qsFilter = IntentFilter().apply {
            addAction(QS_OPENED_ACTION)
            addAction(QS_CLOSED_ACTION)
        }

        localBroadcastManager.registerReceiver(qsReceiver, qsFilter)
    }

    fun stop() {
        localBroadcastManager.unregisterReceiver(qsReceiver)
    }

    private inner class SettingsReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d(TAG, "onReceive ${intent?.action}")
            when (intent?.action) {
                QS_OPENED_ACTION -> {
                    if (!isQuickSettingsOpen) {
                        isQuickSettingsOpen = true
                        Log.d(TAG, "Quick Settings opened → hiding notification")
                        settingsOpened()
                    }
                }

                QS_CLOSED_ACTION -> {
                    if (isQuickSettingsOpen) {
                        isQuickSettingsOpen = false
                        Log.d(TAG, "Quick Settings closed → restoring notification")
                        settingsClosed()
                    }
                }
            }
        }
    }
}