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
import androidx.compose.runtime.collectAsState
import com.ross.livemedia.notification.MediaNotificationListenerService
import com.ross.livemedia.storage.StorageHelper
import com.ross.livemedia.permission.PermissionViewModel
import com.ross.livemedia.ui.screen.PermissionScreen
import com.ross.livemedia.ui.screen.SettingsScreen
import com.ross.livemedia.ui.screen.AppSelectionScreen
import androidx.compose.runtime.*

class MainActivity : ComponentActivity() {

    private val viewModel: PermissionViewModel by viewModels()
    private lateinit var storageHelper: StorageHelper // Initialized in onCreate

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        storageHelper = StorageHelper(this) // Initialize SettingsManager

        setContent {
            MaterialTheme {
                val uiState = viewModel.uiState.collectAsState().value
                val requestPermissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { isGranted: Boolean ->
                    if (isGranted) {
                        viewModel.onPostNotificationPermissionGranted()
                    }
                }

                SideEffect {
                    viewModel.checkPermissions()
                    if (!uiState.hasPostNotificationPermission) {
                        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                if (uiState.shouldShowPermissions) {
                    PermissionScreen(
                        hasNotificationListenerPermission = uiState.hasNotificationListenerPermission,
                        hasPostNotificationPermission = uiState.hasPostNotificationPermission,
                        hasAccessibilityPermission = uiState.hasAccessibilityPermission,
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
                    var currentScreen by remember { mutableStateOf("settings") }

                    when (currentScreen) {
                        "settings" -> SettingsScreen(
                            storageHelper = storageHelper,
                            hasAccessibilityPermission = uiState.hasAccessibilityPermission,
                            onRequestAccessibilityPermission = {
                                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                            },
                            onNavigateToAppSelection = {
                                currentScreen = "app_selection"
                            }
                        )

                        "app_selection" -> AppSelectionScreen(
                            onBack = { currentScreen = "settings" }
                        )
                    }
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
        // We can access current value directly for non-compose logic if needed,
        // or just check uiState's value if we were in Compose, but here we are in Activity.
        // viewModel.uiState.value is safe to access for the current state snapshot.
        val state = viewModel.uiState.value
        if (state.hasNotificationListenerPermission && state.hasPostNotificationPermission) {
            val intent = Intent(this, MediaNotificationListenerService::class.java)
            startService(intent)
        }
    }
}