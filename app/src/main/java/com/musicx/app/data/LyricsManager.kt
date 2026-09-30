package com.musicx.app.data

import com.musicx.app.network.JioSaavnNetworkClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LyricsManager {

    private val api = JioSaavnNetworkClient.api

    suspend fun getLyricsForTrack(songId: String): String = withContext(Dispatchers.IO) {
        try {
            // Step 1: Song details se lyrics_id aur has_lyrics check karein
            val detailsResponse = api.getSongDetails(songId)
            if (!detailsResponse.isSuccessful || detailsResponse.body() == null) {
                return@withContext "Unable to load song details."
            }

            val rootObj = detailsResponse.body()!!
            val songObj = rootObj.getAsJsonObject(songId) ?: return@withContext "Track details not found."
            val moreInfo = songObj.getAsJsonObject("more_info")

            val hasLyrics = moreInfo?.get("has_lyrics")?.asString
            if (hasLyrics != "true") {
                return@withContext "Lyrics not available for this song."
            }

            val lyricsId = moreInfo.get("lyrics_id")?.asString
            if (lyricsId.isNullOrEmpty()) {
                return@withContext "Lyrics identifier missing."
            }

            // Step 2: Exact lyrics API hit karein
            val lyricsResponse = api.getLyrics(lyricsId)
            if (lyricsResponse.isSuccessful && lyricsResponse.body() != null) {
                val lyricsJson = lyricsResponse.body()!!
                val rawLyrics = lyricsJson.get("lyrics")?.asString ?: ""

                if (rawLyrics.isNotBlank()) {
                    // HTML tags aur entity cleaning
                    return@withContext rawLyrics
                        .replace("<br\\s*/?>".toRegex(), "\n")
                        .replace("&quot;", "\"")
                        .replace("&amp;", "&")
                        .replace("&#039;", "'")
                        .trim()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext "Lyrics could not be loaded."
    }
}
