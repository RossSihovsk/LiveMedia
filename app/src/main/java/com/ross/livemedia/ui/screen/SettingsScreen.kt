package com.ross.livemedia.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import com.ross.livemedia.storage.PillContent
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
    val hideNotificationOnQsOpen = remember { mutableStateOf(storageHelper.hideNotificationOnQsOpen) }
    val pillContent = remember { mutableStateOf(storageHelper.pillContent) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF121212) // Dark background color
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Settings",
                color = Color.White,
                fontSize = 28.sp,
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(top = 24.dp, bottom = 24.dp)
            )

            SectionHeader("Notification Body")

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

            SettingToggle(
                label = "Hide on Quick Settings",
                description = "Hide the media notification when Quick Settings or Notification Shade are opened.",
                checkedState = hideNotificationOnQsOpen,
                onCheckedChange = { storageHelper.hideNotificationOnQsOpen = it }
            )

            Spacer(modifier = Modifier.padding(top = 24.dp))
            SectionHeader("Status Bar Pill")

            PillContentOption(
                label = "Song Title",
                description = "Show the first 7 letters of the track title",
                selected = pillContent.value == PillContent.TITLE,
                onClick = {
                    pillContent.value = PillContent.TITLE
                    storageHelper.pillContent = PillContent.TITLE
                }
            )

            PillContentOption(
                label = "Elapsed Time",
                description = "Show current playback position. The title will be shown anyway when music is on pause",
                selected = pillContent.value == PillContent.ELAPSED,
                onClick = {
                    pillContent.value = PillContent.ELAPSED
                    storageHelper.pillContent = PillContent.ELAPSED
                }
            )

            PillContentOption(
                label = "Remaining Time",
                description = "Show time left in the song. The title will be shown anyway when music is on pause",
                selected = pillContent.value == PillContent.REMAINING,
                onClick = {
                    pillContent.value = PillContent.REMAINING
                    storageHelper.pillContent = PillContent.REMAINING
                }
            )
            Spacer(modifier = Modifier.padding(top = 24.dp))
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

@Composable
fun SectionHeader(title: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        Text(
            text = title.uppercase(),
            color = Color(0xFF888888),
            fontSize = 12.sp,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        HorizontalDivider(color = Color(0xFF333333), thickness = 1.dp)
    }
}

@Composable
fun PillContentOption(
    label: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
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

        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = Color.White,
                unselectedColor = Color(0xFF666666)
            )
        )
    }

    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 0.dp),
        thickness = 1.dp,
        color = Color(0xFF333333)
    )
}