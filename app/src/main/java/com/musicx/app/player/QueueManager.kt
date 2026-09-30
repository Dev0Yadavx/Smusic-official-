package com.musicx.app.player

import android.util.Log
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.musicx.app.data.TrackModel
import com.musicx.app.network.JioSaavnNetworkClient
import com.musicx.app.network.JioSaavnSecurity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class QueueManager {

    private val api = JioSaavnNetworkClient.api

    suspend fun fetchNextQueueTracks(currentSongId: String): List<TrackModel> = withContext(Dispatchers.IO) {
        val queueTracks = mutableListOf<TrackModel>()
        try {
            val response = api.getQueueRecommendations(currentSongId)
            if (!response.isSuccessful || response.body() == null) {
                return@withContext emptyList()
            }

            val rootElement: JsonElement = response.body()!!
            val songObjects = mutableListOf<JsonObject>()

            when {
                rootElement.isJsonArray -> {
                    val arr = rootElement.asJsonArray
                    for (i in 0 until arr.size()) {
                        if (arr.get(i).isJsonObject) songObjects.add(arr.get(i).asJsonObject)
                    }
                }
                rootElement.isJsonObject -> {
                    val obj = rootElement.asJsonObject
                    val array = when {
                        obj.has("results") && obj.get("results").isJsonArray -> obj.getAsJsonArray("results")
                        obj.has("data") && obj.get("data").isJsonArray -> obj.getAsJsonArray("data")
                        obj.has("songs") && obj.get("songs").isJsonArray -> obj.getAsJsonArray("songs")
                        else -> null
                    }

                    if (array != null) {
                        for (i in 0 until array.size()) {
                            if (array.get(i).isJsonObject) songObjects.add(array.get(i).asJsonObject)
                        }
                    } else {
                        // Fallback: iterate numeric associative keys ("0", "1", "2")
                        for (entry in obj.entrySet()) {
                            if (entry.value.isJsonObject) {
                                songObjects.add(entry.value.asJsonObject)
                            }
                        }
                    }
                }
            }

            songObjects.forEach { songObj ->
                val more = songObj.getAsJsonObject("more_info")
                val encUrl = more?.get("encrypted_media_url")?.asString
                    ?: songObj.get("encrypted_media_url")?.asString

                val id = songObj.get("id")?.asString ?: songObj.get("song_id")?.asString ?: ""
                val title = (songObj.get("title")?.asString ?: songObj.get("song")?.asString ?: "")
                    .replace("&quot;", "\"")
                    .replace("&amp;", "&")
                    .replace("&#039;", "'")
                val artist = songObj.get("subtitle")?.asString
                    ?: more?.get("singers")?.asString
                    ?: "Unknown Artist"
                val image = JioSaavnSecurity.getHdImage(songObj.get("image")?.asString)

                val stream = if (!encUrl.isNullOrEmpty()) {
                    JioSaavnSecurity.decryptTo320Stream(encUrl)
                } else ""

                if (id.isNotEmpty() && (stream.isNotEmpty() || !encUrl.isNullOrEmpty())) {
                    queueTracks.add(
                        TrackModel(
                            id = id,
                            title = if (title.isBlank()) "Recommended Song" else title,
                            artist = artist,
                            coverArt = image,
                            streamUrl = stream
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("QueueManager", "Failed to fetch recommendations: ${e.message}")
        }
        return@withContext queueTracks
    }
}
