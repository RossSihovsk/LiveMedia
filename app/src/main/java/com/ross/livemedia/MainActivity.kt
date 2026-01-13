package com.ross.livemedia

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.SideEffect
import com.ross.livemedia.notification.MediaNotificationListenerService
import com.ross.livemedia.storage.StorageHelper
import com.ross.livemedia.permission.PermissionViewModel
import com.ross.livemedia.ui.screen.PermissionScreen
import com.ross.livemedia.ui.screen.SettingsScreen

class MainActivity : ComponentActivity() {

    private val viewModel: PermissionViewModel by viewModels()
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
                        viewModel.onPostNotificationPermissionGranted()
                    }
                }

                SideEffect {
                    viewModel.checkPermissions()
                    if (!viewModel.hasPostNotificationPermission) {
                        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                if (viewModel.shouldShowPermissions()) {
                    PermissionScreen(
                        hasNotificationListenerPermission = viewModel.hasNotificationListenerPermission,
                        hasPostNotificationPermission = viewModel.hasPostNotificationPermission,
                        hasAccessibilityPermission = viewModel.hasAccessibilityPermission,
                        onGrantNotificationListenerPermissionClick = {
                            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                        },
                        onGrantPostNotificationPermissionClick = {
                            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        },
                        onGrantAccessibilityPermissionClick = {
                            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                        },
                        onSkipAccessibilityPermissionClick = {
                            viewModel.skipAccessibilityPermission()
                        }
                    )
                } else {
                    SettingsScreen(
                        storageHelper = storageHelper,
                        hasAccessibilityPermission = viewModel.hasAccessibilityPermission,
                        onRequestAccessibilityPermission = {
                            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkPermissions()
        startServiceIfEnabled()
    }

    private fun startServiceIfEnabled() {
        if (viewModel.hasNotificationListenerPermission && viewModel.hasPostNotificationPermission) {
            val intent = Intent(this, MediaNotificationListenerService::class.java)
            startService(intent)
        }
    }
}