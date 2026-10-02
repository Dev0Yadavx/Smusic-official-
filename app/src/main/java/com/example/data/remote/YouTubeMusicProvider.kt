package com.example.data.remote

import android.util.Log
import com.example.data.model.*
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * YouTube Music (InnerTube) Content Provider
 * Provides search (songs, albums, artists, playlists), curated home shelves,
 * artist profiles, album tracklists, and audio stream resolution in authentic YouTube Music style.
 */
object YouTubeMusicProvider {
    private const val TAG = "YouTubeMusicProvider"
    private const val YTM_API_URL = "https://music.youtube.com/youtubei/v1"
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    private val gson = Gson()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private fun createBaseContext(clientName: String = "WEB_REMIX", clientVersion: String = "1.20240101.01.00"): JsonObject {
        val root = JsonObject()
        val context = JsonObject()
        val client = JsonObject()
        client.addProperty("clientName", clientName)
        client.addProperty("clientVersion", clientVersion)
        client.addProperty("hl", "en")
        client.addProperty("gl", "US")
        context.add("client", client)
        root.add("context", context)
        return root
    }

    /**
     * Search songs on YouTube Music
     * params: EgWKAQIIAWoKEAkQBRAKEAMQBA%3D%3D (Songs filter)
     */
    suspend fun searchSongs(query: String): List<Song> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()

        try {
            val payload = createBaseContext()
            payload.addProperty("query", query)
            payload.addProperty("params", "EgWKAQIIAWoKEAkQBRAKEAMQBA%3D%3D")

            val request = Request.Builder()
                .url("$YTM_API_URL/search")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36")
                .header("Content-Type", "application/json")
                .header("X-YouTube-Client-Name", "67")
                .header("X-YouTube-Client-Version", "1.20240101.01.00")
                .header("Referer", "https://music.youtube.com/")
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.w(TAG, "Song search failed: code ${response.code}")
                return@withContext emptyList()
            }

            val bodyStr = response.body?.string() ?: return@withContext emptyList()
            val json = gson.fromJson(bodyStr, JsonObject::class.java)

            parseSearchSongs(json)
        } catch (e: Exception) {
            Log.e(TAG, "Song search error: ${e.message}", e)
            emptyList()
        }
    }

    private fun parseSearchSongs(json: JsonObject): List<Song> {
        val songs = mutableListOf<Song>()
        try {
            val tabs = json.getAsJsonObject("contents")
                ?.getAsJsonObject("tabbedSearchResultsRenderer")
                ?.getAsJsonArray("tabs") ?: return emptyList()

            val tabContent = tabs.get(0)?.asJsonObject
                ?.getAsJsonObject("tabRenderer")
                ?.getAsJsonObject("content")
                ?.getAsJsonObject("sectionListRenderer")
                ?.getAsJsonArray("contents") ?: return emptyList()

            for (sec in tabContent) {
                val secObj = sec.asJsonObject
                val musicShelf = secObj.getAsJsonObject("musicShelfRenderer")
                val itemSection = secObj.getAsJsonObject("itemSectionRenderer")
                    ?.getAsJsonArray("contents")

                val contentsArray = musicShelf?.getAsJsonArray("contents")
                    ?: itemSection?.get(0)?.asJsonObject?.getAsJsonObject("musicShelfRenderer")?.getAsJsonArray("contents")
                    ?: continue

                for (item in contentsArray) {
                    val listItem = item.asJsonObject.getAsJsonObject("musicResponsiveListItemRenderer") ?: continue

                    // 1. Extract videoId
                    var videoId = listItem.getAsJsonObject("playlistItemData")?.get("videoId")?.asString
                    if (videoId.isNullOrBlank()) {
                        videoId = listItem.getAsJsonObject("overlay")
                            ?.getAsJsonObject("musicItemThumbnailOverlayRenderer")
                            ?.getAsJsonObject("content")
                            ?.getAsJsonObject("musicPlayButtonRenderer")
                            ?.getAsJsonObject("playNavigationEndpoint")
                            ?.getAsJsonObject("watchEndpoint")
                            ?.get("videoId")?.asString
                    }
                    if (videoId.isNullOrBlank()) {
                        videoId = listItem.getAsJsonObject("navigationEndpoint")
                            ?.getAsJsonObject("watchEndpoint")
                            ?.get("videoId")?.asString
                    }
                    if (videoId.isNullOrBlank()) continue

                    // 2. Extract Title
                    val flexColumns = listItem.getAsJsonArray("flexColumns") ?: continue
                    val firstCol = flexColumns.get(0)?.asJsonObject
                        ?.getAsJsonObject("musicResponsiveListItemFlexColumnRenderer")
                        ?.getAsJsonObject("text")
                        ?.getAsJsonArray("runs")

                    val title = firstCol?.get(0)?.asJsonObject?.get("text")?.asString ?: "Unknown Title"

                    // 3. Extract Artist & Album
                    var artist = "YouTube Artist"
                    var album = "YouTube Music"
                    var durationSec = 180L

                    if (flexColumns.size() > 1) {
                        val secondCol = flexColumns.get(1)?.asJsonObject
                            ?.getAsJsonObject("musicResponsiveListItemFlexColumnRenderer")
                            ?.getAsJsonObject("text")
                            ?.getAsJsonArray("runs")

                        if (secondCol != null && secondCol.size() > 0) {
                            val runsList = mutableListOf<String>()
                            for (r in secondCol) {
                                val t = r.asJsonObject.get("text")?.asString
                                if (!t.isNullOrBlank() && t != " • " && t != "&") {
                                    runsList.add(t)
                                }
                            }
                            if (runsList.isNotEmpty()) {
                                artist = runsList[0]
                            }
                            if (runsList.size > 1 && !runsList[1].contains(":")) {
                                album = runsList[1]
                            }
                            val durStr = runsList.lastOrNull { it.contains(":") }
                            if (durStr != null) {
                                durationSec = parseDurationToSeconds(durStr)
                            }
                        }
                    }

                    // 4. Extract Artwork thumbnail
                    val thumbnails = listItem.getAsJsonObject("thumbnail")
                        ?.getAsJsonObject("musicThumbnailRenderer")
                        ?.getAsJsonObject("thumbnail")
                        ?.getAsJsonArray("thumbnails")

                    val thumbUrl = thumbnails?.lastOrNull()?.asJsonObject?.get("url")?.asString
                        ?: "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"

                    val cleanThumb = thumbUrl.replace(Regex("=w\\d+-h\\d+.*"), "=w500-h500-l90-rj")

                    songs.add(
                        Song(
                            id = "yt_$videoId",
                            token = videoId,
                            title = title,
                            artist = artist,
                            album = album,
                            artwork = cleanThumb,
                            duration = durationSec,
                            streamUrl = "",
                            language = "YouTube Music",
                            year = "2026"
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Parse songs error: ${e.message}")
        }
        return songs
    }

    /**
     * Search albums on YouTube Music
     * params: EgWKAQIYAWoKEAkQBRAKEAMQBA%3D%3D (Albums filter)
     */
    suspend fun searchAlbums(query: String): List<Album> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()

        try {
            val payload = createBaseContext()
            payload.addProperty("query", query)
            payload.addProperty("params", "EgWKAQIYAWoKEAkQBRAKEAMQBA%3D%3D")

            val request = Request.Builder()
                .url("$YTM_API_URL/search")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36")
                .header("Content-Type", "application/json")
                .header("X-YouTube-Client-Name", "67")
                .header("X-YouTube-Client-Version", "1.20240101.01.00")
                .header("Referer", "https://music.youtube.com/")
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) return@withContext emptyList()

            val bodyStr = response.body?.string() ?: return@withContext emptyList()
            val json = gson.fromJson(bodyStr, JsonObject::class.java)

            parseSearchAlbums(json)
        } catch (e: Exception) {
            Log.e(TAG, "Album search error: ${e.message}", e)
            emptyList()
        }
    }

    private fun parseSearchAlbums(json: JsonObject): List<Album> {
        val albums = mutableListOf<Album>()
        try {
            val tabs = json.getAsJsonObject("contents")
                ?.getAsJsonObject("tabbedSearchResultsRenderer")
                ?.getAsJsonArray("tabs") ?: return emptyList()

            val tabContent = tabs.get(0)?.asJsonObject
                ?.getAsJsonObject("tabRenderer")
                ?.getAsJsonObject("content")
                ?.getAsJsonObject("sectionListRenderer")
                ?.getAsJsonArray("contents") ?: return emptyList()

            for (sec in tabContent) {
                val secObj = sec.asJsonObject
                val musicShelf = secObj.getAsJsonObject("musicShelfRenderer")
                val itemSection = secObj.getAsJsonObject("itemSectionRenderer")
                    ?.getAsJsonArray("contents")

                val contentsArray = musicShelf?.getAsJsonArray("contents")
                    ?: itemSection?.get(0)?.asJsonObject?.getAsJsonObject("musicShelfRenderer")?.getAsJsonArray("contents")
                    ?: continue

                for (item in contentsArray) {
                    val listItem = item.asJsonObject.getAsJsonObject("musicResponsiveListItemRenderer") ?: continue

                    // Browse ID for the album
                    val browseId = listItem.getAsJsonObject("navigationEndpoint")
                        ?.getAsJsonObject("browseEndpoint")
                        ?.get("browseId")?.asString ?: ""

                    val flexColumns = listItem.getAsJsonArray("flexColumns") ?: continue
                    val firstCol = flexColumns.get(0)?.asJsonObject
                        ?.getAsJsonObject("musicResponsiveListItemFlexColumnRenderer")
                        ?.getAsJsonObject("text")
                        ?.getAsJsonArray("runs")
                    val title = firstCol?.get(0)?.asJsonObject?.get("text")?.asString ?: continue

                    var artist = "YouTube Artist"
                    var year = "2026"

                    if (flexColumns.size() > 1) {
                        val secondCol = flexColumns.get(1)?.asJsonObject
                            ?.getAsJsonObject("musicResponsiveListItemFlexColumnRenderer")
                            ?.getAsJsonObject("text")
                            ?.getAsJsonArray("runs")

                        if (secondCol != null) {
                            val runs = mutableListOf<String>()
                            for (r in secondCol) {
                                val t = r.asJsonObject.get("text")?.asString
                                if (!t.isNullOrBlank() && t != " • " && t != "&") runs.add(t)
                            }
                            if (runs.size >= 2) {
                                artist = runs[1]
                            } else if (runs.isNotEmpty() && runs[0] != "Album" && runs[0] != "Single") {
                                artist = runs[0]
                            }
                            val y = runs.lastOrNull { it.matches(Regex("\\d{4}")) }
                            if (y != null) year = y
                        }
                    }

                    val thumbnails = listItem.getAsJsonObject("thumbnail")
                        ?.getAsJsonObject("musicThumbnailRenderer")
                        ?.getAsJsonObject("thumbnail")
                        ?.getAsJsonArray("thumbnails")
                    val thumbUrl = thumbnails?.lastOrNull()?.asJsonObject?.get("url")?.asString ?: ""
                    val cleanThumb = thumbUrl.replace(Regex("=w\\d+-h\\d+.*"), "=w500-h500-l90-rj")

                    val id = if (browseId.isNotBlank()) "yt_$browseId" else "yt_alb_${title.hashCode()}"
                    albums.add(
                        Album(
                            id = id,
                            title = title,
                            subtitle = "$artist • $year",
                            artist = artist,
                            artwork = cleanThumb,
                            year = year,
                            songCount = 0,
                            songs = emptyList()
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Parse albums error: ${e.message}")
        }
        return albums
    }

    /**
     * Search artists on YouTube Music
     * params: EgWKAQIgAWoKEAkQBRAKEAMQBA%3D%3D (Artists filter)
     */
    suspend fun searchArtists(query: String): List<Artist> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()

        try {
            val payload = createBaseContext()
            payload.addProperty("query", query)
            payload.addProperty("params", "EgWKAQIgAWoKEAkQBRAKEAMQBA%3D%3D")

            val request = Request.Builder()
                .url("$YTM_API_URL/search")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36")
                .header("Content-Type", "application/json")
                .header("X-YouTube-Client-Name", "67")
                .header("X-YouTube-Client-Version", "1.20240101.01.00")
                .header("Referer", "https://music.youtube.com/")
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) return@withContext emptyList()

            val bodyStr = response.body?.string() ?: return@withContext emptyList()
            val json = gson.fromJson(bodyStr, JsonObject::class.java)

            parseSearchArtists(json)
        } catch (e: Exception) {
            Log.e(TAG, "Artist search error: ${e.message}", e)
            emptyList()
        }
    }

    private fun parseSearchArtists(json: JsonObject): List<Artist> {
        val artists = mutableListOf<Artist>()
        try {
            val tabs = json.getAsJsonObject("contents")
                ?.getAsJsonObject("tabbedSearchResultsRenderer")
                ?.getAsJsonArray("tabs") ?: return emptyList()

            val tabContent = tabs.get(0)?.asJsonObject
                ?.getAsJsonObject("tabRenderer")
                ?.getAsJsonObject("content")
                ?.getAsJsonObject("sectionListRenderer")
                ?.getAsJsonArray("contents") ?: return emptyList()

            for (sec in tabContent) {
                val secObj = sec.asJsonObject
                val musicShelf = secObj.getAsJsonObject("musicShelfRenderer")
                val itemSection = secObj.getAsJsonObject("itemSectionRenderer")
                    ?.getAsJsonArray("contents")

                val contentsArray = musicShelf?.getAsJsonArray("contents")
                    ?: itemSection?.get(0)?.asJsonObject?.getAsJsonObject("musicShelfRenderer")?.getAsJsonArray("contents")
                    ?: continue

                for (item in contentsArray) {
                    val listItem = item.asJsonObject.getAsJsonObject("musicResponsiveListItemRenderer") ?: continue

                    val browseId = listItem.getAsJsonObject("navigationEndpoint")
                        ?.getAsJsonObject("browseEndpoint")
                        ?.get("browseId")?.asString ?: ""

                    val flexColumns = listItem.getAsJsonArray("flexColumns") ?: continue
                    val firstCol = flexColumns.get(0)?.asJsonObject
                        ?.getAsJsonObject("musicResponsiveListItemFlexColumnRenderer")
                        ?.getAsJsonObject("text")
                        ?.getAsJsonArray("runs")
                    val name = firstCol?.get(0)?.asJsonObject?.get("text")?.asString ?: continue

                    var role = "Artist"
                    if (flexColumns.size() > 1) {
                        val secondCol = flexColumns.get(1)?.asJsonObject
                            ?.getAsJsonObject("musicResponsiveListItemFlexColumnRenderer")
                            ?.getAsJsonObject("text")
                            ?.getAsJsonArray("runs")
                        val runs = secondCol?.mapNotNull { it.asJsonObject.get("text")?.asString }?.joinToString(" ")
                        if (!runs.isNullOrBlank()) role = runs
                    }

                    val thumbnails = listItem.getAsJsonObject("thumbnail")
                        ?.getAsJsonObject("musicThumbnailRenderer")
                        ?.getAsJsonObject("thumbnail")
                        ?.getAsJsonArray("thumbnails")
                    val thumbUrl = thumbnails?.lastOrNull()?.asJsonObject?.get("url")?.asString ?: ""
                    val cleanThumb = thumbUrl.replace(Regex("=w\\d+-h\\d+.*"), "=w500-h500-l90-rj")

                    val id = if (browseId.isNotBlank()) "yt_$browseId" else "yt_art_${name.hashCode()}"
                    artists.add(
                        Artist(
                            id = id,
                            name = name,
                            image = cleanThumb,
                            role = role,
                            topSongs = emptyList(),
                            topAlbums = emptyList()
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Parse artists error: ${e.message}")
        }
        return artists
    }

    /**
     * Search playlists on YouTube Music
     * params: EgWKAQIoAWoKEAkQBRAKEAMQBA%3D%3D (Community Playlists filter)
     */
    suspend fun searchPlaylists(query: String): List<Playlist> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()

        try {
            val payload = createBaseContext()
            payload.addProperty("query", query)
            payload.addProperty("params", "EgWKAQIoAWoKEAkQBRAKEAMQBA%3D%3D")

            val request = Request.Builder()
                .url("$YTM_API_URL/search")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36")
                .header("Content-Type", "application/json")
                .header("X-YouTube-Client-Name", "67")
                .header("X-YouTube-Client-Version", "1.20240101.01.00")
                .header("Referer", "https://music.youtube.com/")
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) return@withContext emptyList()

            val bodyStr = response.body?.string() ?: return@withContext emptyList()
            val json = gson.fromJson(bodyStr, JsonObject::class.java)

            parseSearchPlaylists(json)
        } catch (e: Exception) {
            Log.e(TAG, "Playlist search error: ${e.message}", e)
            emptyList()
        }
    }

    private fun parseSearchPlaylists(json: JsonObject): List<Playlist> {
        val playlists = mutableListOf<Playlist>()
        try {
            val tabs = json.getAsJsonObject("contents")
                ?.getAsJsonObject("tabbedSearchResultsRenderer")
                ?.getAsJsonArray("tabs") ?: return emptyList()

            val tabContent = tabs.get(0)?.asJsonObject
                ?.getAsJsonObject("tabRenderer")
                ?.getAsJsonObject("content")
                ?.getAsJsonObject("sectionListRenderer")
                ?.getAsJsonArray("contents") ?: return emptyList()

            for (sec in tabContent) {
                val secObj = sec.asJsonObject
                val musicShelf = secObj.getAsJsonObject("musicShelfRenderer")
                val itemSection = secObj.getAsJsonObject("itemSectionRenderer")
                    ?.getAsJsonArray("contents")

                val contentsArray = musicShelf?.getAsJsonArray("contents")
                    ?: itemSection?.get(0)?.asJsonObject?.getAsJsonObject("musicShelfRenderer")?.getAsJsonArray("contents")
                    ?: continue

                for (item in contentsArray) {
                    val listItem = item.asJsonObject.getAsJsonObject("musicResponsiveListItemRenderer") ?: continue

                    val browseId = listItem.getAsJsonObject("navigationEndpoint")
                        ?.getAsJsonObject("browseEndpoint")
                        ?.get("browseId")?.asString ?: ""

                    val flexColumns = listItem.getAsJsonArray("flexColumns") ?: continue
                    val firstCol = flexColumns.get(0)?.asJsonObject
                        ?.getAsJsonObject("musicResponsiveListItemFlexColumnRenderer")
                        ?.getAsJsonObject("text")
                        ?.getAsJsonArray("runs")
                    val title = firstCol?.get(0)?.asJsonObject?.get("text")?.asString ?: continue

                    var subtitle = "Playlist • YouTube Music"
                    if (flexColumns.size() > 1) {
                        val secondCol = flexColumns.get(1)?.asJsonObject
                            ?.getAsJsonObject("musicResponsiveListItemFlexColumnRenderer")
                            ?.getAsJsonObject("text")
                            ?.getAsJsonArray("runs")
                        val runs = secondCol?.mapNotNull { it.asJsonObject.get("text")?.asString }?.joinToString(" ")
                        if (!runs.isNullOrBlank()) subtitle = runs
                    }

                    val thumbnails = listItem.getAsJsonObject("thumbnail")
                        ?.getAsJsonObject("musicThumbnailRenderer")
                        ?.getAsJsonObject("thumbnail")
                        ?.getAsJsonArray("thumbnails")
                    val thumbUrl = thumbnails?.lastOrNull()?.asJsonObject?.get("url")?.asString ?: ""
                    val cleanThumb = thumbUrl.replace(Regex("=w\\d+-h\\d+.*"), "=w500-h500-l90-rj")

                    val id = if (browseId.isNotBlank()) "yt_$browseId" else "yt_pl_${title.hashCode()}"
                    playlists.add(
                        Playlist(
                            id = id,
                            title = title,
                            subtitle = subtitle,
                            description = "YouTube Music Curated Playlist",
                            artwork = cleanThumb,
                            previewArtworks = if (cleanThumb.isNotBlank()) listOf(cleanThumb) else emptyList(),
                            songCount = 25,
                            songs = emptyList()
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Parse playlists error: ${e.message}")
        }
        return playlists
    }

    /**
     * Resolves playable audio stream URL for a YouTube video/audio track
     */
    suspend fun resolveStreamUrl(videoId: String): String? = withContext(Dispatchers.IO) {
        val cleanId = videoId.removePrefix("yt_")
        println("[SMusic-Terminal] [YTM-Resolver] Attempting stream extraction for YouTube ID: $cleanId")

        // 1. Try InnerTube Player API using iOS client (known for unencrypted audio streams)
        try {
            val payload = JsonObject()
            val context = JsonObject()
            val client = JsonObject()
            client.addProperty("clientName", "IOS")
            client.addProperty("clientVersion", "19.29.1")
            client.addProperty("deviceMake", "Apple")
            client.addProperty("deviceModel", "iPhone16,2")
            client.addProperty("hl", "en")
            client.addProperty("gl", "US")
            context.add("client", client)
            payload.add("context", context)
            payload.addProperty("videoId", cleanId)

            val request = Request.Builder()
                .url("$YTM_API_URL/player")
                .header("User-Agent", "com.google.ios.youtube/19.29.1 (iPhone16,2; U; CPU iOS 17_5_1 like Mac OS X; en_US)")
                .header("Content-Type", "application/json")
                .header("X-YouTube-Client-Name", "5")
                .header("X-YouTube-Client-Version", "19.29.1")
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val bodyStr = response.body?.string()
                if (!bodyStr.isNullOrBlank()) {
                    val json = gson.fromJson(bodyStr, JsonObject::class.java)
                    val streamingData = json.getAsJsonObject("streamingData")
                    val adaptiveFormats = streamingData?.getAsJsonArray("adaptiveFormats")
                    if (adaptiveFormats != null) {
                        var bestUrl: String? = null
                        var highestBitrate = 0
                        for (f in adaptiveFormats) {
                            val fObj = f.asJsonObject
                            val mimeType = fObj.get("mimeType")?.asString ?: ""
                            if (mimeType.contains("audio/")) {
                                val url = fObj.get("url")?.asString
                                val bitrate = fObj.get("bitrate")?.asInt ?: 0
                                if (!url.isNullOrBlank() && bitrate > highestBitrate) {
                                    bestUrl = url
                                    highestBitrate = bitrate
                                }
                            }
                        }
                        if (!bestUrl.isNullOrBlank()) {
                            println("[SMusic-Terminal-SUCCESS] [YTM-Resolver] Found direct iOS audio stream ($highestBitrate bps)")
                            return@withContext bestUrl
                        }
                    }
                }
            } else {
                println("[SMusic-Terminal-WARN] [YTM-Resolver] iOS player endpoint returned HTTP ${response.code}")
            }
        } catch (e: Exception) {
            println("[SMusic-Terminal-WARN] [YTM-Resolver] iOS player endpoint failed: ${e.message}")
        }

        // 2. Try InnerTube Player API using Android Music client
        try {
            val payload = JsonObject()
            val context = JsonObject()
            val client = JsonObject()
            client.addProperty("clientName", "ANDROID_MUSIC")
            client.addProperty("clientVersion", "6.41.52")
            client.addProperty("androidSdkVersion", 34)
            client.addProperty("hl", "en")
            client.addProperty("gl", "US")
            context.add("client", client)
            payload.add("context", context)
            payload.addProperty("videoId", cleanId)

            val request = Request.Builder()
                .url("$YTM_API_URL/player")
                .header("User-Agent", "com.google.android.apps.youtube.music/6.41.52 (Linux; U; Android 14)")
                .header("Content-Type", "application/json")
                .header("X-YouTube-Client-Name", "21")
                .header("X-YouTube-Client-Version", "6.41.52")
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val bodyStr = response.body?.string()
                if (!bodyStr.isNullOrBlank()) {
                    val json = gson.fromJson(bodyStr, JsonObject::class.java)
                    val streamingData = json.getAsJsonObject("streamingData")
                    val adaptiveFormats = streamingData?.getAsJsonArray("adaptiveFormats")
                    if (adaptiveFormats != null) {
                        var bestUrl: String? = null
                        var highestBitrate = 0
                        for (f in adaptiveFormats) {
                            val fObj = f.asJsonObject
                            val mimeType = fObj.get("mimeType")?.asString ?: ""
                            if (mimeType.contains("audio/")) {
                                val url = fObj.get("url")?.asString
                                val bitrate = fObj.get("bitrate")?.asInt ?: 0
                                if (!url.isNullOrBlank() && bitrate > highestBitrate) {
                                    bestUrl = url
                                    highestBitrate = bitrate
                                }
                            }
                        }
                        if (!bestUrl.isNullOrBlank()) {
                            return@withContext bestUrl
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Android player endpoint warning: ${e.message}")
        }

        // 3. Fallback: Invidious / Piped public audio proxy
        val invidiousInstances = listOf(
            "https://inv.tux.pizza/api/v1/videos/$cleanId",
            "https://invidious.jing.rocks/api/v1/videos/$cleanId",
            "https://vid.puffyan.us/api/v1/videos/$cleanId"
        )
        for (instance in invidiousInstances) {
            try {
                val request = Request.Builder()
                    .url(instance)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    .build()
                val response = httpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (!body.isNullOrBlank()) {
                        val json = gson.fromJson(body, JsonObject::class.java)
                        val formats = json.getAsJsonArray("adaptiveFormats")
                        if (formats != null) {
                            for (f in formats) {
                                val fObj = f.asJsonObject
                                val type = fObj.get("type")?.asString ?: ""
                                val url = fObj.get("url")?.asString
                                if (type.startsWith("audio/") && !url.isNullOrBlank()) {
                                    return@withContext url
                                }
                            }
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        null
    }

    /**
     * Get Complete Artist Details for YouTube Music (Top Songs, Albums, Singles, Bio, High-Res Header)
     */
    suspend fun getArtistDetails(artistIdOrName: String): Artist? = withContext(Dispatchers.IO) {
        val cleanQuery = artistIdOrName
            .removePrefix("yt_")
            .removePrefix("UC")
            .replace("_", " ")
            .trim()

        val actualQuery = if (cleanQuery.all { it.isDigit() || it.isLetter() } && cleanQuery.length > 20) {
            "Top Artist"
        } else {
            cleanQuery
        }

        try {
            // 1. Fetch Top Songs by Artist from YouTube Music
            val songs = searchSongs("$actualQuery songs")
            val topSongs = if (songs.isNotEmpty()) songs else searchSongs(actualQuery)

            // 2. Fetch Albums by Artist from YouTube Music
            val albums = searchAlbums("$actualQuery album")

            // 3. High-res Artist DP
            val directDp = com.example.data.remote.ArtistDpManager.getOriginalDp(actualQuery)
            val fallbackImg = if (directDp.isNotBlank() && !com.example.data.remote.ArtistDpManager.isJioPlaceholder(directDp)) {
                directDp
            } else {
                topSongs.firstOrNull { it.artwork.isNotBlank() }?.artwork ?: ""
            }

            Artist(
                id = if (artistIdOrName.startsWith("yt_")) artistIdOrName else "yt_art_${artistIdOrName.hashCode()}",
                name = actualQuery.ifBlank { "YouTube Music Artist" },
                image = fallbackImg,
                role = "Official Artist Channel",
                topSongs = topSongs.take(25),
                topAlbums = albums.take(15)
            )
        } catch (e: Exception) {
            Log.e(TAG, "getArtistDetails error: ${e.message}")
            null
        }
    }

    /**
     * Get Complete Album Details for YouTube Music (Tracklist, Metadata, Artwork)
     */
    suspend fun getAlbumDetails(
        albumIdOrTitle: String,
        fallbackTitle: String = "",
        fallbackArtist: String = ""
    ): Album? = withContext(Dispatchers.IO) {
        val cleanTitle = if (fallbackTitle.isNotBlank()) fallbackTitle else albumIdOrTitle.removePrefix("yt_").replace("_", " ").trim()
        val query = if (fallbackArtist.isNotBlank()) "$cleanTitle $fallbackArtist" else "$cleanTitle album"

        try {
            val songs = searchSongs(query)
            if (songs.isEmpty()) return@withContext null

            val firstSong = songs.first()
            val albumTitle = if (fallbackTitle.isNotBlank()) fallbackTitle else firstSong.album.ifBlank { cleanTitle }
            val artistName = if (fallbackArtist.isNotBlank()) fallbackArtist else firstSong.artist
            val artwork = firstSong.artwork

            Album(
                id = if (albumIdOrTitle.startsWith("yt_")) albumIdOrTitle else "yt_alb_${albumIdOrTitle.hashCode()}",
                title = albumTitle,
                subtitle = "Album • $artistName • 2026",
                artist = artistName,
                artwork = artwork,
                year = "2026",
                songCount = songs.size,
                songs = songs
            )
        } catch (e: Exception) {
            Log.e(TAG, "getAlbumDetails error: ${e.message}")
            null
        }
    }

    /**
     * Get Complete Playlist Details for YouTube Music
     */
    suspend fun getPlaylistDetails(playlistIdOrTitle: String): Playlist? = withContext(Dispatchers.IO) {
        val cleanTitle = playlistIdOrTitle.removePrefix("yt_").replace("_", " ").trim()
        try {
            val songs = searchSongs("$cleanTitle hits")
            if (songs.isEmpty()) return@withContext null

            val artwork = songs.firstOrNull { it.artwork.isNotBlank() }?.artwork ?: ""
            Playlist(
                id = if (playlistIdOrTitle.startsWith("yt_")) playlistIdOrTitle else "yt_pl_${playlistIdOrTitle.hashCode()}",
                title = cleanTitle.ifBlank { "YouTube Music Hits" },
                subtitle = "Playlist • YouTube Music • ${songs.size} tracks",
                description = "Curated trending tracks from YouTube Music",
                artwork = artwork,
                previewArtworks = songs.take(4).map { it.artwork },
                songCount = songs.size,
                songs = songs
            )
        } catch (e: Exception) {
            Log.e(TAG, "getPlaylistDetails error: ${e.message}")
            null
        }
    }

    /**
     * Curated YouTube Music Home Shelves
     * Creates authentic YouTube Music sections:
     * 1. Quick picks (4 rows per column chunk)
     * 2. Mixed for you / Trending Hits
     * 3. Albums & Singles (square cards)
     * 4. Featured Artists (circular cards)
     * 5. Mood & Activity Playlists
     */
    suspend fun getHomeShelves(mood: String = "All"): List<MusicShelf> = withContext(Dispatchers.IO) {
        val shelves = mutableListOf<MusicShelf>()

        // Queries mapped to YouTube Music vibes
        val quickPickQuery = when (mood.lowercase()) {
            "energize" -> "Upbeat Dance Party Hits 2026"
            "workout" -> "High Energy Gym Workout Music"
            "relax" -> "Acoustic Chill Lo-Fi Relaxing"
            "commute" -> "Feel Good Roadtrip Pop Hits"
            "focus" -> "Deep Focus Ambient Study Music"
            "hindi" -> "Top Bollywood Hindi Songs 2026"
            "punjabi" -> "Latest Punjabi Hits 2026"
            "english" -> "Billboard Global Top 50 Hits"
            else -> "YouTube Music Top Hits 2026 India"
        }

        try {
            // 1. Quick picks (Songs chunked into 4 rows per column)
            val quickSongs = searchSongs(quickPickQuery)
            if (quickSongs.isNotEmpty()) {
                shelves.add(
                    MusicShelf(
                        id = "yt_shelf_quick_picks",
                        title = "Quick picks",
                        subtitle = "START RADIO FROM A SONG",
                        type = ShelfType.SONG_HORIZONTAL,
                        items = quickSongs.take(24).map { ShelfItem.SongItem(it) }
                    )
                )
            }

            // 2. Mixed for you / Trending Songs
            val trendingSongs = searchSongs(
                if (mood.equals("All", ignoreCase = true)) "Global Viral Top Tracks 2026"
                else "$mood Top Trending Songs"
            )
            if (trendingSongs.isNotEmpty()) {
                shelves.add(
                    MusicShelf(
                        id = "yt_shelf_trending",
                        title = "Trending now on YouTube Music",
                        subtitle = "Most played songs this week",
                        type = ShelfType.SONG_HORIZONTAL,
                        items = trendingSongs.take(20).map { ShelfItem.SongItem(it) }
                    )
                )
            }

            // 3. New Releases & Albums
            val albums = searchAlbums(
                if (mood.equals("All", ignoreCase = true)) "Top Albums 2026"
                else "$mood Album"
            )
            if (albums.isNotEmpty()) {
                shelves.add(
                    MusicShelf(
                        id = "yt_shelf_albums",
                        title = "Albums & Singles",
                        subtitle = "New releases on YouTube Music",
                        type = ShelfType.ALBUM_HORIZONTAL,
                        items = albums.take(12).map { ShelfItem.AlbumItem(it) }
                    )
                )
            }

            // 4. Featured Artists
            val artists = searchArtists(
                if (mood.equals("All", ignoreCase = true)) "Top Indian Artists"
                else "$mood Artists"
            )
            if (artists.isNotEmpty()) {
                shelves.add(
                    MusicShelf(
                        id = "yt_shelf_artists",
                        title = "Featured Artists",
                        subtitle = "Popular creators and musicians",
                        type = ShelfType.ARTIST_HORIZONTAL,
                        items = artists.take(12).map { ShelfItem.ArtistItem(it) }
                    )
                )
            }

            // 5. Community & Featured Playlists
            val playlists = searchPlaylists(
                if (mood.equals("All", ignoreCase = true)) "Top Hits Playlist 2026"
                else "$mood Playlist"
            )
            if (playlists.isNotEmpty()) {
                shelves.add(
                    MusicShelf(
                        id = "yt_shelf_playlists",
                        title = "Community Playlists",
                        subtitle = "Curated by YouTube Music community",
                        type = ShelfType.PLAYLIST_HORIZONTAL,
                        items = playlists.take(12).map { ShelfItem.PlaylistItem(it) }
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error building home shelves: ${e.message}")
        }

        shelves
    }

    private fun parseDurationToSeconds(durStr: String): Long {
        return try {
            val parts = durStr.trim().split(":")
            if (parts.size == 2) {
                parts[0].toLong() * 60 + parts[1].toLong()
            } else if (parts.size == 3) {
                parts[0].toLong() * 3600 + parts[1].toLong() * 60 + parts[2].toLong()
            } else 180L
        } catch (_: Exception) {
            180L
        }
    }
}
