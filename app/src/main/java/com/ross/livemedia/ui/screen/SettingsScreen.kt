package com.ross.livemedia.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ross.livemedia.storage.StorageHelper

@Composable
fun SettingsScreen(storageHelper: StorageHelper) {
    val showAlbumArt = remember { mutableStateOf(storageHelper.showAlbumArt) }
    val showArtistName = remember { mutableStateOf(storageHelper.showArtistName) }
    val showAlbumName = remember { mutableStateOf(storageHelper.showAlbumName) }
    val showActionButtons = remember { mutableStateOf(storageHelper.showActionButtons) }
    val showProgress = remember { mutableStateOf(storageHelper.showProgress) }
    val showMusicProvider = remember { mutableStateOf(storageHelper.showMusicProvider) }
    val showTimestamp = remember { mutableStateOf(storageHelper.showTimestamp) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF121212) // Dark background color
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Notification Content Settings",
                color = Color.White,
                fontSize = 24.sp,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(top = 24.dp, bottom = 32.dp)
            )

            SettingToggle(
                label = "Show Album Art",
                description = "Display the album cover art in the notification.",
                checkedState = showAlbumArt,
                onCheckedChange = { storageHelper.showAlbumArt = it }
            )

            SettingToggle(
                label = "Show Artist Name",
                description = "Include the artist's name below the track title.",
                checkedState = showArtistName,
                onCheckedChange = { storageHelper.showArtistName = it }
            )

            SettingToggle(
                label = "Show Album Name",
                description = "Display the album name in the notification details.",
                checkedState = showAlbumName,
                onCheckedChange = { storageHelper.showAlbumName = it }
            )

            SettingToggle(
                label = "Show Action Buttons",
                description = "Include controls like Play/Pause, Next, and Previous.",
                checkedState = showActionButtons,
                onCheckedChange = { storageHelper.showActionButtons = it }
            )

            SettingToggle(
                label = "Show Progress",
                description = "Show song progress",
                checkedState = showProgress,
                onCheckedChange = { storageHelper.showProgress = it }
            )

            SettingToggle(
                label = "Show Music provider app name",
                description = "It will show what music app you're using (Spotify/YT Music, etc...)",
                checkedState = showMusicProvider,
                onCheckedChange = { storageHelper.showMusicProvider = it }
            )

            SettingToggle(
                label = "Show timestamps",
                description = "Show elapsed time and total duration on the player.",
                checkedState = showTimestamp,
                onCheckedChange = { storageHelper.showTimestamp = it }
            )
        }
    }
}

@Composable
fun SettingToggle(
    label: String,
    description: String,
    checkedState: MutableState<Boolean>,
    onCheckedChange: (Boolean) -> Unit
) {
    val labelColor = MaterialTheme.colorScheme.onSurface
    val descriptionColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                color = Color.White,
                fontSize = 18.sp,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.padding(top = 2.dp))
            Text(
                text = description,
                color = Color(0xFFCCCCCC),
                fontSize = 14.sp,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Switch(
            checked = checkedState.value,
            onCheckedChange = {
                checkedState.value = it
                onCheckedChange(it)
            },
            thumbContent = if (checkedState.value) {
                {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        modifier = Modifier.size(SwitchDefaults.IconSize),
                    )
                }
            } else {
                null
            },
            modifier = Modifier.padding(start = 16.dp)
        )
    }

    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 0.dp),
        thickness = 1.dp,
        color = Color(0xFF333333)
    )
}