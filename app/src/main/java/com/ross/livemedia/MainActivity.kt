package com.ross.livemedia

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import com.ross.livemedia.notification.MediaNotificationListenerService
import com.ross.livemedia.storage.StorageHelper
import com.ross.livemedia.ui.screen.PermissionScreen
import com.ross.livemedia.ui.screen.SettingsScreen

class MainActivity : ComponentActivity() {

    private val hasNotificationListenerPermission = mutableStateOf(false)
    private val hasPostNotificationPermission = mutableStateOf(false)
    private val hasAccessibilityPermission = mutableStateOf(false)
    private lateinit var storageHelper: StorageHelper // Initialized in onCreate

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        storageHelper = StorageHelper(this) // Initialize SettingsManager

        setContent {
            MaterialTheme {
                val requestPermissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { isGranted: Boolean ->
                    if (isGranted) {
                        hasPostNotificationPermission.value = true
                    }
                }

                SideEffect {
                    checkPermissions()
                    if (!hasPostNotificationPermission.value) {
                        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                // Determine which screen to show
                val showPermissions =
                    !hasNotificationListenerPermission.value || !hasPostNotificationPermission.value || !hasAccessibilityPermission.value

                if (showPermissions) {
                    PermissionScreen(
                        hasNotificationListenerPermission = hasNotificationListenerPermission.value,
                        hasPostNotificationPermission = hasPostNotificationPermission.value,
                        hasAccessibilityPermission = hasAccessibilityPermission.value,
                        onGrantNotificationListenerPermissionClick = {
                            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                        },
                        onGrantPostNotificationPermissionClick = {
                            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        },
                        onGrantAccessibilityPermissionClick = {
                            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                        }
                    )
                } else {
                    SettingsScreen(storageHelper = storageHelper)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkPermissions()
    }

    private fun checkPermissions() {
        hasNotificationListenerPermission.value = isNotificationListenerEnabled()
        hasPostNotificationPermission.value =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        hasAccessibilityPermission.value = isAccessibilityServiceEnabled()

        if (hasNotificationListenerPermission.value && hasPostNotificationPermission.value) {
            val intent = Intent(this, MediaNotificationListenerService::class.java)
            startService(intent)
        }
    }

    private fun isNotificationListenerEnabled(): Boolean {
        val enabledListeners =
            Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        return enabledListeners?.contains(packageName) == true
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val accessibilityEnabled = Settings.Secure.getInt(
            contentResolver,
            Settings.Secure.ACCESSIBILITY_ENABLED, 0
        )
        if (accessibilityEnabled == 1) {
            val service =
                "$packageName/${com.ross.livemedia.settings.QSStateListener::class.java.canonicalName}"
            val enabledServices = Settings.Secure.getString(
                contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            return enabledServices?.contains(service) == true
        }
        return false
    }
}