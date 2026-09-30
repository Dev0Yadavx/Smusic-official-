package com.musicx.app.player

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.musicx.app.data.TrackModel

object MediaItemBuilder {
    fun build(track: TrackModel): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(track.title)
            .setDisplayTitle(track.title)
            .setArtist(track.artist)
            .setAlbumTitle(track.album)
            .setArtworkUri(if (track.coverArt.isNotBlank()) Uri.parse(track.coverArt) else null)
            .setIsPlayable(true)
            .setIsBrowsable(false)
            .build()

        return MediaItem.Builder()
            .setMediaId(track.id)
            .setUri(track.streamUrl)
            .setMediaMetadata(metadata)
            .build()
    }
}
