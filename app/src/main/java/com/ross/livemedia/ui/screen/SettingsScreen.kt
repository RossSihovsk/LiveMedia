package com.ross.livemedia.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.ui.res.stringResource
import com.ross.livemedia.R
import com.ross.livemedia.storage.PillContent
import com.ross.livemedia.storage.StorageHelper
import androidx.compose.material.icons.filled.KeyboardArrowRight

@Composable
fun SettingsScreen(
    storageHelper: StorageHelper,
    hasAccessibilityPermission: Boolean,
    onRequestAccessibilityPermission: () -> Unit,
    onNavigateToAppSelection: () -> Unit
) {
    val showAlbumArt = remember { mutableStateOf(storageHelper.showAlbumArt) }
    val showArtistName = remember { mutableStateOf(storageHelper.showArtistName) }
    val showAlbumName = remember { mutableStateOf(storageHelper.showAlbumName) }
    val showActionButtons = remember { mutableStateOf(storageHelper.showActionButtons) }
    val showProgress = remember { mutableStateOf(storageHelper.showProgress) }
    val showMusicProvider = remember { mutableStateOf(storageHelper.showMusicProvider) }
    val showTimestamp = remember { mutableStateOf(storageHelper.showTimestamp) }
    val hideNotificationOnQsOpen =
        remember { mutableStateOf(storageHelper.hideNotificationOnQsOpen) }
    val pillContent = remember { mutableStateOf(storageHelper.pillContent) }
    val isScrollEnabled = remember { mutableStateOf(storageHelper.isScrollEnabled) }

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
                text = stringResource(R.string.settings_title),
                color = Color.White,
                fontSize = 28.sp,
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(top = 24.dp, bottom = 24.dp)
            )

            SectionHeader(stringResource(R.string.section_notification_body))

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 0.dp),
                thickness = 1.dp,
                color = Color(0xFF333333)
            )

            SettingToggle(
                label = stringResource(R.string.setting_show_album_art),
                description = stringResource(R.string.setting_show_album_art_desc),
                checkedState = showAlbumArt,
                onCheckedChange = { storageHelper.showAlbumArt = it }
            )

            SettingToggle(
                label = stringResource(R.string.setting_show_artist_name),
                description = stringResource(R.string.setting_show_artist_name_desc),
                checkedState = showArtistName,
                onCheckedChange = { storageHelper.showArtistName = it }
            )

            SettingToggle(
                label = stringResource(R.string.setting_show_album_name),
                description = stringResource(R.string.setting_show_album_name_desc),
                checkedState = showAlbumName,
                onCheckedChange = { storageHelper.showAlbumName = it }
            )

            SettingToggle(
                label = stringResource(R.string.setting_show_action_buttons),
                description = stringResource(R.string.setting_show_action_buttons_desc),
                checkedState = showActionButtons,
                onCheckedChange = { storageHelper.showActionButtons = it }
            )

            SettingToggle(
                label = stringResource(R.string.setting_show_progress),
                description = stringResource(R.string.setting_show_progress_desc),
                checkedState = showProgress,
                onCheckedChange = { storageHelper.showProgress = it }
            )

            SettingToggle(
                label = stringResource(R.string.setting_show_music_provider),
                description = stringResource(R.string.setting_show_music_provider_desc),
                checkedState = showMusicProvider,
                onCheckedChange = { storageHelper.showMusicProvider = it }
            )

            SettingToggle(
                label = stringResource(R.string.setting_show_timestamp),
                description = stringResource(R.string.setting_show_timestamp_desc),
                checkedState = showTimestamp,
                onCheckedChange = { storageHelper.showTimestamp = it }
            )

            SettingToggle(
                label = stringResource(R.string.setting_hide_on_qs),
                description = stringResource(R.string.setting_hide_on_qs_desc),
                checkedState = hideNotificationOnQsOpen,
                onCheckedChange = { isChecked ->
                    if (isChecked && !hasAccessibilityPermission) {
                        // Reset toggle to false until permission is granted
                        hideNotificationOnQsOpen.value = false
                        onRequestAccessibilityPermission()
                    } else {
                        hideNotificationOnQsOpen.value = isChecked
                        storageHelper.hideNotificationOnQsOpen = isChecked
                    }
                }
            )

            Spacer(modifier = Modifier.padding(top = 24.dp))
            SectionHeader(stringResource(R.string.section_status_bar_pill))

            PillContentOption(
                label = stringResource(R.string.pill_option_title),
                description = stringResource(R.string.pill_option_title_desc),
                selected = pillContent.value == PillContent.TITLE,
                onClick = {
                    pillContent.value = PillContent.TITLE
                    storageHelper.pillContent = PillContent.TITLE
                }
            )

            SettingToggle(
                label = stringResource(R.string.setting_scroll_text),
                description = stringResource(R.string.setting_scroll_text_desc),
                checkedState = isScrollEnabled,
                onCheckedChange = { storageHelper.isScrollEnabled = it }
            )

            PillContentOption(
                label = stringResource(R.string.pill_option_elapsed),
                description = stringResource(R.string.pill_option_elapsed_desc),
                selected = pillContent.value == PillContent.ELAPSED,
                onClick = {
                    pillContent.value = PillContent.ELAPSED
                    storageHelper.pillContent = PillContent.ELAPSED
                }
            )

            PillContentOption(
                label = stringResource(R.string.pill_option_remaining),
                description = stringResource(R.string.pill_option_remaining_desc),
                selected = pillContent.value == PillContent.REMAINING,
                onClick = {
                    pillContent.value = PillContent.REMAINING
                    storageHelper.pillContent = PillContent.REMAINING
                }
            )
            Spacer(modifier = Modifier.padding(top = 24.dp))
            SectionHeader(stringResource(R.string.section_app_selection))

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 0.dp),
                thickness = 1.dp,
                color = Color(0xFF333333)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onNavigateToAppSelection)
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(R.string.setting_app_selection),
                        color = Color.White,
                        fontSize = 18.sp,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.padding(top = 2.dp))
                    Text(
                        text = stringResource(R.string.setting_app_selection_desc),
                        color = Color(0xFFCCCCCC),
                        fontSize = 14.sp,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Color(0xFF888888)
                )
            }
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 0.dp),
                thickness = 1.dp,
                color = Color(0xFF333333)
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
//        HorizontalDivider(color = Color(0xFF333333), thickness = 1.dp)
    }
}

@Composable
fun PillContentOption(
    label: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 0.dp),
        thickness = 1.dp,
        color = Color(0xFF333333)
    )

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
}