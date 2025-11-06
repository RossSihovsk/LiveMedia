package com.ross.livemedia

import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.PlaybackState

data class MusicState(
    val title: String,
    val artist: String,
    val albumArt: Bitmap?,
    val isPlaying: Boolean,
    val duration: Long,
    val position: Long,
    val packageName: String,
    val mediaSessionActive: Boolean // Added to simplify check for existence
) {
    constructor(
        metadata: MediaMetadata,
        playbackState: PlaybackState,
        packageName: String
    ) : this(
        title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "Unknown Title",
        artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: "Unknown Artist",
        albumArt = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART),
        isPlaying = playbackState.state == PlaybackState.STATE_PLAYING,
        duration = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION),
        position = playbackState.position,
        packageName = packageName,
        mediaSessionActive = true
    )
}