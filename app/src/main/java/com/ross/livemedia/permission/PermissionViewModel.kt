package com.ross.livemedia.permission

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import com.ross.livemedia.qs.QSStateListener
import com.ross.livemedia.storage.StorageHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class PermissionUiState(
    val hasNotificationListenerPermission: Boolean = false,
    val hasPostNotificationPermission: Boolean = false,
    val hasAccessibilityPermission: Boolean = false,
    val accessibilitySkipped: Boolean = false
) {
    val shouldShowPermissions: Boolean
        get() = !hasNotificationListenerPermission ||
                !hasPostNotificationPermission ||
                (!hasAccessibilityPermission && !accessibilitySkipped)
}

class PermissionViewModel(application: Application) : AndroidViewModel(application) {

    private val storageHelper = StorageHelper(application)

    private val _uiState = MutableStateFlow(
        PermissionUiState(
            accessibilitySkipped = storageHelper.accessibilityPermissionSkipped
        )
    )
    val uiState = _uiState.asStateFlow()

    fun checkPermissions() {
        val context = getApplication<Application>()
        _uiState.update { currentState ->
            currentState.copy(
                hasNotificationListenerPermission = isNotificationListenerEnabled(context),
                hasPostNotificationPermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED,
                hasAccessibilityPermission = isAccessibilityServiceEnabled(context)
            )
        }
    }

    fun onPostNotificationPermissionGranted() {
        _uiState.update { it.copy(hasPostNotificationPermission = true) }
    }

    fun skipAccessibilityPermission() {
        storageHelper.accessibilityPermissionSkipped = true
        _uiState.update { it.copy(accessibilitySkipped = true) }
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
}
