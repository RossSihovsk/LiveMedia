package com.ross.livemedia.utils

import com.ross.livemedia.media.MusicState
import com.ross.livemedia.media.MusicState.Companion.EMPTY_ALBUM
import com.ross.livemedia.media.MusicState.Companion.EMPTY_ARTIST
import java.util.Locale

private const val MAX_LENGTH = 70

fun buildArtisAlbumTitle(
    showArtistName: Boolean,
    showAlbumName: Boolean,
    musicState: MusicState
): String {
    val parts = mutableListOf<String>()

    val showArtist =
        showArtistName && musicState.artist.isNotBlank() && musicState.artist != EMPTY_ARTIST
    val showAlbum =
        showAlbumName && musicState.albumName.isNotBlank() && musicState.albumName != EMPTY_ALBUM

    // 1. Add Artist Name if requested and available
    if (showArtist) {
        parts.add(musicState.artist)
    }

    // 2. Add Album Name if requested and available
    if (showAlbum) {
        // If both are present, they will be separated by the joinToString separator.
        parts.add(musicState.albumName)
    }

    // Combine all parts. Use " • " or " - " as a clear, non-hyphenated separator.
    val result = parts.joinToString(" • ")

    return if (result.length > MAX_LENGTH) {
        result.substring(0, MAX_LENGTH) + "..."
    } else {
        result
    }
}

fun formatMusicProgress(currentPosition: Int, duration: Int): String {
    val positionStr = formatTime(currentPosition)
    val durationStr = formatTime(duration)
    return "$positionStr/$durationStr"
}

private fun formatTime(millis: Int): String {
    if (millis < 0) return "0:00"

    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60

    // Uses Locale.US to ensure numbers are formatted with standard digits
    return String.format(Locale.US, "%d:%02d", minutes, seconds)
}

fun combineProviderAndTimestamp(
    musicProvider: String,
    showMusicProvider: Boolean,
    showTimestamp: Boolean,
    position: Int,
    duration: Int
) = buildList {
    if (showMusicProvider) add(musicProvider)
    if (showTimestamp) add(formatMusicProgress(position, duration))
}.joinToString(" • ").ifBlank { null }
