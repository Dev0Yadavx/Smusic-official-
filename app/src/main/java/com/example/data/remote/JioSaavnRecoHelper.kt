package com.example.data.remote

import android.util.Log
import com.example.data.mapper.SongMapper
import com.example.data.model.PlayableTrack
import com.example.data.model.Song
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject

/**
 * JioSaavn Recommendation & Radio Helper.
 * Robustly parses raw JSON recommendation and radio responses across all JSON structures
 * (JSON arrays, objects with "results"/"data"/"songs", and numeric associative key-value maps).
 */
object JioSaavnRecoHelper {

    private const val TAG = "JioSaavnRecoHelper"

    fun parseRecommendations(rawResponse: JsonElement?): List<Song> {
        val tracks = mutableListOf<Song>()
        if (rawResponse == null) return tracks

        try {
            when {
                rawResponse.isJsonArray -> {
                    extractSongsFromArray(rawResponse.asJsonArray, tracks)
                }
                rawResponse.isJsonObject -> {
                    val obj = rawResponse.asJsonObject
                    // Check standard array fields first
                    val foundArray = when {
                        obj.has("results") && obj.get("results").isJsonArray -> obj.getAsJsonArray("results")
                        obj.has("data") && obj.get("data").isJsonArray -> obj.getAsJsonArray("data")
                        obj.has("songs") && obj.get("songs").isJsonArray -> obj.getAsJsonArray("songs")
                        obj.has("items") && obj.get("items").isJsonArray -> obj.getAsJsonArray("items")
                        else -> null
                    }

                    if (foundArray != null && foundArray.size() > 0) {
                        extractSongsFromArray(foundArray, tracks)
                    } else {
                        // Fallback: iterate object entries (numeric keys like "0", "1", "2" or station songs)
                        for (entry in obj.entrySet()) {
                            val value = entry.value
                            if (value.isJsonObject) {
                                val itemObj = value.asJsonObject
                                if (isSongJsonObject(itemObj)) {
                                    val song = SongMapper.map(itemObj)
                                    if (song.id.isNotBlank() && tracks.none { it.id == song.id }) {
                                        tracks.add(song)
                                    }
                                } else if (itemObj.has("song") && itemObj.get("song").isJsonObject) {
                                    val song = SongMapper.map(itemObj.getAsJsonObject("song"))
                                    if (song.id.isNotBlank() && tracks.none { it.id == song.id }) {
                                        tracks.add(song)
                                    }
                                }
                            } else if (value.isJsonArray) {
                                extractSongsFromArray(value.asJsonArray, tracks)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing recommendations: ${e.message}", e)
        }

        return tracks
    }

    private fun extractSongsFromArray(array: JsonArray, output: MutableList<Song>) {
        for (i in 0 until array.size()) {
            val element = array.get(i)
            if (element.isJsonObject) {
                val songObj = element.asJsonObject
                if (isSongJsonObject(songObj)) {
                    val song = SongMapper.map(songObj)
                    if (song.id.isNotBlank() && output.none { it.id == song.id }) {
                        output.add(song)
                    }
                }
            }
        }
    }

    private fun isSongJsonObject(obj: JsonObject): Boolean {
        return obj.has("id") || obj.has("song") || obj.has("title") ||
                obj.has("encrypted_media_url") || obj.has("perma_url") ||
                (obj.has("more_info") && obj.get("more_info").isJsonObject)
    }

    fun parseRecommendationsAsPlayable(rawResponse: JsonElement?): List<PlayableTrack> {
        return parseRecommendations(rawResponse).map { it.toPlayableTrack() }
    }
}
