package com.ross.livemedia.utils

import com.ross.livemedia.media.MusicState
import com.ross.livemedia.media.MusicState.Companion.EMPTY_ALBUM

private const val MAX_LENGTH = 70

fun buildArtisAlbumTitle(musicState: MusicState): String {
    val baseResult =
        if (musicState.albumName == EMPTY_ALBUM) musicState.artist else "${musicState.artist} - ${musicState.albumName}"

    return if (baseResult.length > MAX_LENGTH) {
        baseResult.substring(0, MAX_LENGTH) + "..."
    } else {
        baseResult
    }
}
