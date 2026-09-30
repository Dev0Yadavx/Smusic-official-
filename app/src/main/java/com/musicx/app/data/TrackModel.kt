package com.musicx.app.data

data class TrackModel(
    val id: String,
    val title: String,
    val artist: String,
    val coverArt: String,
    val streamUrl: String,
    val duration: Long = 0L,
    val album: String = ""
)
