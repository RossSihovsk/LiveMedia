package com.ross.livemedia.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class StorageHelper(context: Context) {
    private val preferences: SharedPreferences =
        context.getSharedPreferences("NotificationPrefs", Context.MODE_PRIVATE)

    var showAlbumArt: Boolean
        get() = preferences.getBoolean(KEY_SHOW_ALBUM_ART, DEFAULT_VALUE)
        set(value) = preferences.edit { putBoolean(KEY_SHOW_ALBUM_ART, value) }

    var showArtistName: Boolean
        get() = preferences.getBoolean(KEY_SHOW_ARTIST_NAME, DEFAULT_VALUE)
        set(value) = preferences.edit { putBoolean(KEY_SHOW_ARTIST_NAME, value) }

    var showAlbumName: Boolean
        get() = preferences.getBoolean(KEY_SHOW_ALBUM_NAME, DEFAULT_VALUE)
        set(value) = preferences.edit { putBoolean(KEY_SHOW_ALBUM_NAME, value) }

    var showActionButtons: Boolean
        get() = preferences.getBoolean(KEY_SHOW_ACTION_BUTTONS, DEFAULT_VALUE)
        set(value) = preferences.edit { putBoolean(KEY_SHOW_ACTION_BUTTONS, value) }

    var showProgress: Boolean
        get() = preferences.getBoolean(KEY_SHOW_PROGRESS, DEFAULT_VALUE)
        set(value) = preferences.edit { putBoolean(KEY_SHOW_PROGRESS, value) }

    var showMusicProvider: Boolean
        get() = preferences.getBoolean(KEY_SHOW_MUSIC_PROVIDER_NAME, DEFAULT_VALUE)
        set(value) = preferences.edit { putBoolean(KEY_SHOW_MUSIC_PROVIDER_NAME, value) }

    //False is the default value only for this field
    var showTimestamp: Boolean
        get() = preferences.getBoolean(KEY_SHOW_TIMESTAMP, false)
        set(value) = preferences.edit { putBoolean(KEY_SHOW_TIMESTAMP, value) }

    var hideNotificationOnQsOpen: Boolean
        get() = preferences.getBoolean(KEY_HIDE_NOTIFICATION_ON_QS_OPEN, DEFAULT_VALUE)
        set(value) = preferences.edit { putBoolean(KEY_HIDE_NOTIFICATION_ON_QS_OPEN, value) }


    companion object {
        private const val KEY_SHOW_ALBUM_ART = "show_album_art"
        private const val KEY_SHOW_ARTIST_NAME = "show_artist_name"
        private const val KEY_SHOW_ALBUM_NAME = "show_album_name"
        private const val KEY_SHOW_ACTION_BUTTONS = "show_action_buttons"
        private const val KEY_SHOW_PROGRESS = "show_progress"
        private const val KEY_SHOW_TIMESTAMP = "show_song_timestamp"
        private const val KEY_SHOW_MUSIC_PROVIDER_NAME = "show_music_provider"
        private const val KEY_HIDE_NOTIFICATION_ON_QS_OPEN = "hide_notification_on_qs_open"
        private const val DEFAULT_VALUE = true
    }
}