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
import androidx.compose.ui.res.stringResource
import com.ross.livemedia.R

@Composable
fun PermissionScreen(
    hasNotificationListenerPermission: Boolean,
    hasPostNotificationPermission: Boolean,
    hasAccessibilityPermission: Boolean,
    onGrantNotificationListenerPermissionClick: () -> Unit,
    onGrantPostNotificationPermissionClick: () -> Unit,
    onGrantAccessibilityPermissionClick: () -> Unit
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
                    text = stringResource(R.string.permission_notification_listener_title),
                    color = Color.White,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.permission_notification_listener_desc),
                    color = Color(0xFFCCCCCC),
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(32.dp))
                Button(onClick = onGrantNotificationListenerPermissionClick) {
                    Text(text = stringResource(R.string.permission_notification_listener_button))
                }
            }

            if (!hasPostNotificationPermission) {
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = stringResource(R.string.permission_post_notification_title),
                    color = Color.White,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.permission_post_notification_desc),
                    color = Color(0xFFCCCCCC),
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(32.dp))
                Button(onClick = onGrantPostNotificationPermissionClick) {
                    Text(text = stringResource(R.string.permission_post_notification_button))
                }
            }

            if (!hasAccessibilityPermission) {
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = stringResource(R.string.permission_accessibility_title),
                    color = Color.White,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.permission_accessibility_desc),
                    color = Color(0xFFCCCCCC),
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(32.dp))
                Button(onClick = onGrantAccessibilityPermissionClick) {
                    Text(text = stringResource(R.string.permission_accessibility_button))
                }
            }

            if (hasNotificationListenerPermission && hasPostNotificationPermission && hasAccessibilityPermission) {
                Text(
                    text = stringResource(R.string.permission_all_granted),
                    color = Color.White,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}