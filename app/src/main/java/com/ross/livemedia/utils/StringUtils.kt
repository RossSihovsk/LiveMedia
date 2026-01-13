package com.ross.livemedia.utils

import com.ross.livemedia.media.MusicState
import com.ross.livemedia.media.MusicState.Companion.EMPTY_ALBUM
import com.ross.livemedia.media.MusicState.Companion.EMPTY_ARTIST
import com.ross.livemedia.storage.PillContent
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
    val result = parts.joinToString(" - ")

    return if (result.length > MAX_LENGTH) {
        result.take(MAX_LENGTH) + "..."
    } else {
        result
    }
}

fun formatMusicProgress(currentPosition: Int, duration: Int): String {
    val positionStr = formatTime(currentPosition)
    val durationStr = formatTime(duration)
    return "$positionStr / $durationStr"
}

fun formatTime(millis: Int): String {
    if (millis <= 0) return "0:00"

    val totalSeconds = millis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return if (hours > 0) {
        // Format: H:MM:SS
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        // Format: M:SS
        String.format(Locale.US, "%d:%02d", minutes, seconds)
    }
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

fun providePillText(
    title: String,
    position: Int,
    duration: Int,
    isPlaying: Boolean,
    pillContent: PillContent,
    isScrollEnabled: Boolean,
    elapsedTimeMs: Long
): String {
    val showTime = isPlaying && duration > 0

    if (pillContent == PillContent.ELAPSED && showTime) return formatTime(position)
    if (pillContent == PillContent.REMAINING && showTime) return formatTime(duration - position)

    // TITLE mode or time not available (or paused)
    val trimmedTitle = title.trim()
    if (!isScrollEnabled || trimmedTitle.length <= 7) return trimmedTitle.take(7)

    return provideScrollableText(trimmedTitle, elapsedTimeMs)
}

private fun provideScrollableText(title: String, elapsedTimeMs: Long): String {
    val speedMs = 500 // Scroll every 300ms
    val waitAtStartSteps = 2 // Pause for 1.2s (4 * 300ms) at the beginning
    val waitAtEndSteps = 2 // Pause for 900ms (3 * 300ms) at the end
    
    val scrollRange = title.length - 7
    val cycleSteps = waitAtStartSteps + scrollRange + waitAtEndSteps
    
    val totalSteps = elapsedTimeMs / speedMs
    val stepInCycle = (totalSteps % cycleSteps).toInt()
    
    val offset = when {
        stepInCycle < waitAtStartSteps -> 0
        stepInCycle < waitAtStartSteps + scrollRange -> stepInCycle - waitAtStartSteps
        else -> scrollRange
    }
    
    return title.substring(offset, offset + 7)
}
