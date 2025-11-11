package com.ross.livemedia.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PermissionScreen(
    hasNotificationListenerPermission: Boolean,
    hasPostNotificationPermission: Boolean,
    onGrantNotificationListenerPermissionClick: () -> Unit,
    onGrantPostNotificationPermissionClick: () -> Unit
) {
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
            if (!hasNotificationListenerPermission) {
                Text(
                    text = "Notification Listener Permission Needed",
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
                Button(onClick = onGrantNotificationListenerPermissionClick) {
                    Text(text = "Grant Notification Listener Permission")
                }
            }

            if (!hasPostNotificationPermission) {
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = "Post Notification Permission Needed",
                    color = Color.White,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "This app requires permission to post notifications to show the media controls.",
                    color = Color(0xFFCCCCCC),
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(32.dp))
                Button(onClick = onGrantPostNotificationPermissionClick) {
                    Text(text = "Grant Post Notification Permission")
                }
            }

            if (hasNotificationListenerPermission && hasPostNotificationPermission) {
                Text(
                    text = "All Permissions Granted!",
                    color = Color.White,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}