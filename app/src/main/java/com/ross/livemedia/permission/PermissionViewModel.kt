package com.ross.livemedia.permission

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import com.ross.livemedia.qs.QSStateListener
import com.ross.livemedia.storage.StorageHelper

class PermissionViewModel(application: Application) : AndroidViewModel(application) {

    private val storageHelper = StorageHelper(application)

    var hasNotificationListenerPermission by mutableStateOf(false)
        private set

    var hasPostNotificationPermission by mutableStateOf(false)
        private set

    var hasAccessibilityPermission by mutableStateOf(false)
        private set

    var accessibilitySkippedState by mutableStateOf(storageHelper.accessibilityPermissionSkipped)
        private set

    fun checkPermissions() {
        val context = getApplication<Application>()
        hasNotificationListenerPermission = isNotificationListenerEnabled(context)
        hasPostNotificationPermission =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        hasAccessibilityPermission = isAccessibilityServiceEnabled(context)
    }

    fun onPostNotificationPermissionGranted() {
        hasPostNotificationPermission = true
    }

    fun skipAccessibilityPermission() {
        storageHelper.accessibilityPermissionSkipped = true
        accessibilitySkippedState = true
    }

    private fun isNotificationListenerEnabled(context: Application): Boolean {
        val enabledListeners =
            Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        return enabledListeners?.contains(context.packageName) == true
    }

    private fun isAccessibilityServiceEnabled(context: Application): Boolean {
        val accessibilityEnabled = Settings.Secure.getInt(
            context.contentResolver,
            Settings.Secure.ACCESSIBILITY_ENABLED, 0
        )
        if (accessibilityEnabled == 1) {
            val service =
                "${context.packageName}/${QSStateListener::class.java.canonicalName}"
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            return enabledServices?.contains(service) == true
        }
        return false
    }

    fun shouldShowPermissions(): Boolean {
        val missingNotification = !hasNotificationListenerPermission
        val missingPostNotification = !hasPostNotificationPermission
        val missingAccessibility = !hasAccessibilityPermission
        val accessibilitySkipped = accessibilitySkippedState

        return missingNotification || missingPostNotification || (missingAccessibility && !accessibilitySkipped)
    }
}
