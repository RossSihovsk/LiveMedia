package com.ross.livemedia.media

import com.ross.livemedia.R

enum class MusicProvider(
    val appName: String,
    val packageName: String,
    val iconRes: Int
) {
    UNKNOWN(
        "Unknown Player",
        "unknown.package",
        R.drawable.apple_music
    ),
    SPOTIFY(
        "Spotify",
        "com.spotify.music",
        R.drawable.spotify
    ),
    YOUTUBE(
        "YouTube",
        "com.google.android.youtube",
        R.drawable.youtube
    ),
    YOUTUBE_MUSIC(
        "YouTube Music",
        "com.google.android.apps.youtube.music",
        R.drawable.youtube_music
    ),
    SOUNDCLOUD(
        "SoundCloud",
        "com.soundcloud.android",
        R.drawable.soundcloud
    ),
    APPLE_MUSIC(
        "Apple Music",
        "com.apple.android.music",
        R.drawable.apple_music
    ),
    DEEZER(
        "Deezer",
        "deezer.android.app",
        R.drawable.deezer
    ),
    TIDAL(
        "TIDAL",
        "com.aspiro.tidal",
        R.drawable.tidal
    ),
    AMAZON_MUSIC(
        "Amazon Music",
        "com.amazon.mp3",
        R.drawable.amazon_music
    ),
    TELEGRAM(
        "Telegram",
        "org.telegram.messenger",
        R.drawable.telegram
    ),
    NETFLIX(
        "Netflix",
        "com.netflix.mediaclient",
        R.drawable.netflix
    ),
    TWITCH(
        "Twitch",
        "tv.twitch.android.app",
        R.drawable.twitch
    ),
    NOTEBOOK_LM(
        "NotebookLM",
        "com.google.android.apps.labs.language.tailwind",
        R.drawable.notebook_lm
    );

    companion object {
        fun getByAppName(appName: String): MusicProvider {
            return entries.firstOrNull { it.appName == appName } ?: UNKNOWN
        }
    }
}