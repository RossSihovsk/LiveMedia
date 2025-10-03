package com.ross.livemedia

import android.app.NotificationManager
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat

class MainActivity : ComponentActivity() {

    // A state to hold the permission status, which the Compose UI will observe.
    private val hasPermissionState = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            // MaterialTheme is the standard theming for Compose.
            MaterialTheme {
                // Reading the value from the mutable state.
                // When this value changes, the UI will automatically re-render.
                val hasPermission = hasPermissionState.value
                PermissionScreen(
                    hasPermission = hasPermission,
                    onGrantPermissionClick = {
                        // This intent opens the system settings screen where the user can grant
                        // Notification Listener permission to this app.
                        startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Every time the user comes back to the app, check the permission status and update the state.
        checkPermissionAndUpdateState()
    }

    private fun isNotificationListenerEnabled(): Boolean {
        // Check if our service is in the list of enabled notification listeners.
        val enabledListeners = NotificationManagerCompat.getEnabledListenerPackages(this)
        return enabledListeners.contains(packageName)
    }

    private fun checkPermissionAndUpdateState() {
        val hasPermission = isNotificationListenerEnabled()
        // Update the state. This will trigger a recomposition of the UI.
        hasPermissionState.value = hasPermission

        if (hasPermission) {
            // If permission is granted, ensure the background service is running.
            val intent = Intent(this, MediaNotificationListenerService::class.java)
            startService(intent)
        }
    }
}

// This is our main UI, defined as a Composable function.
@Composable
fun PermissionScreen(hasPermission: Boolean, onGrantPermissionClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF121212) // Dark background color
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (hasPermission) "Permission Granted!" else "Permission Needed",
                color = Color.White,
                fontSize = 20.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "This app requires Notification Access to detect media playback and create a live update. Please grant permission in the system settings.",
                color = Color(0xFFCCCCCC),
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onGrantPermissionClick,
                // The button is disabled if permission has already been granted.
                enabled = !hasPermission,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF3F51B5),
                    disabledContainerColor = Color.Gray
                ),
                contentPadding = PaddingValues(horizontal = 32.dp, vertical = 12.dp)
            ) {
                Text(
                    text = if (hasPermission) "Service is Active" else "Grant Permission",
                    color = Color.White
                )
            }
        }
    }
}
