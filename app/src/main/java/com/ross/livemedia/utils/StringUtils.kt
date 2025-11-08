package com.ross.livemedia.utils

import com.ross.livemedia.media.MusicState
import com.ross.livemedia.media.MusicState.Companion.EMPTY_ALBUM

fun buildArtisAlbumTitle(musicState: MusicState) =
    if (musicState.albumName == EMPTY_ALBUM) musicState.artist else "${musicState.artist} - ${musicState.albumName}"