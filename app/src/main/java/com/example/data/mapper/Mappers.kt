package com.example.data.mapper

import com.example.data.model.*
import com.example.data.remote.ArtistDpManager
import com.example.data.remote.JioSaavnImageResolver
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject

object SongMapper {

    fun map(json: JsonObject): Song {
        val id = json.get("id")?.asString ?: json.get("song_id")?.asString ?: ""
        val token = json.get("perma_url")?.asString ?: json.get("token")?.asString ?: ""
        val title = JioSaavnImageResolver.unescapeHtml(
            json.get("title")?.asString ?: json.get("song")?.asString ?: ""
        )
        val subtitle = JioSaavnImageResolver.unescapeHtml(json.get("subtitle")?.asString ?: "")
        val rawImage = json.get("image")?.asString ?: ""
        val artwork = JioSaavnImageResolver.resolve(rawImage, 500)
        val language = json.get("language")?.asString ?: ""
        val year = json.get("year")?.asString ?: ""

        var album = ""
        var albumId = ""
        var duration = 0L
        var encryptedMediaUrl = ""
        var mediaPreviewUrl = json.get("media_preview_url")?.asString ?: ""
        var lyricsSnippet = ""
        var lyricsId = ""
        var artist = subtitle

        val moreInfo = json.getAsJsonObject("more_info")
        if (moreInfo != null) {
            album = JioSaavnImageResolver.unescapeHtml(moreInfo.get("album")?.asString ?: "")
            albumId = moreInfo.get("album_id")?.asString ?: ""
            duration = moreInfo.get("duration")?.asString?.toLongOrNull() ?: 0L
            encryptedMediaUrl = moreInfo.get("encrypted_media_url")?.asString ?: ""
            lyricsSnippet = JioSaavnImageResolver.unescapeHtml(moreInfo.get("lyrics_snippet")?.asString ?: "")
            lyricsId = moreInfo.get("lyrics_id")?.asString ?: id

            // Extract artist names and direct original DPs from artistMap or music or singers
            val artistMap = moreInfo.getAsJsonObject("artistMap")
            if (artistMap != null) {
                // Populate ArtistDpManager directly with genuine CDN image URLs for all artists in the track
                listOf("primary_artists", "featured_artists", "artists").forEach { key ->
                    val arr = artistMap.getAsJsonArray(key)
                    if (arr != null && arr.size() > 0) {
                        for (i in 0 until arr.size()) {
                            val obj = arr.get(i).asJsonObject
                            val aId = obj.get("id")?.asString ?: ""
                            val aName = obj.get("name")?.asString ?: ""
                            val aImg = obj.get("image")?.asString ?: ""
                            if (aName.isNotBlank() && aImg.isNotBlank()) {
                                ArtistDpManager.put(aName, aImg)
                                if (aId.isNotBlank()) ArtistDpManager.putById(aId, aImg)
                            }
                        }
                    }
                }

                val primary = artistMap.getAsJsonArray("primary_artists")
                if (primary != null && primary.size() > 0) {
                    val names = mutableListOf<String>()
                    for (i in 0 until primary.size()) {
                        val obj = primary.get(i).asJsonObject
                        val name = obj.get("name")?.asString
                        if (!name.isNullOrBlank()) names.add(name)
                    }
                    if (names.isNotEmpty()) {
                        artist = names.joinToString(", ")
                    }
                }
            } else {
                val music = moreInfo.get("music")?.asString
                val singers = moreInfo.get("singers")?.asString
                if (!music.isNullOrBlank()) {
                    artist = JioSaavnImageResolver.unescapeHtml(music)
                } else if (!singers.isNullOrBlank()) {
                    artist = JioSaavnImageResolver.unescapeHtml(singers)
                }
            }
        }

        if (artist.isBlank()) {
            artist = "Various Artists"
        }

        return Song(
            id = id,
            token = token,
            title = if (title.isBlank()) "Untitled Track" else title,
            artist = artist,
            album = album,
            albumId = albumId,
            artwork = artwork,
            duration = duration,
            encryptedMediaUrl = encryptedMediaUrl,
            mediaPreviewUrl = mediaPreviewUrl,
            lyricsId = lyricsId,
            lyricsSnippet = lyricsSnippet,
            language = language,
            year = year
        )
    }

    fun mapList(array: JsonArray?): List<Song> {
        if (array == null || array.size() == 0) return emptyList()
        val list = mutableListOf<Song>()
        for (i in 0 until array.size()) {
            val item = array.get(i)
            if (item.isJsonObject) {
                list.add(map(item.asJsonObject))
            }
        }
        return list
    }
}

object AlbumMapper {

    fun map(json: JsonObject): Album {
        val moreInfo = json.getAsJsonObject("more_info")
        val rawId = json.get("id")?.asString ?: json.get("albumid")?.asString ?: moreInfo?.get("album_id")?.asString ?: ""
        val permaUrl = json.get("perma_url")?.asString ?: json.get("url")?.asString ?: ""
        val token = if (permaUrl.isNotBlank()) permaUrl.trimEnd('/').substringAfterLast('/') else (json.get("token")?.asString ?: "")
        val id = rawId.ifBlank { token }

        var title = JioSaavnImageResolver.unescapeHtml(
            json.get("title")?.asString 
                ?: json.get("name")?.asString 
                ?: moreInfo?.get("album")?.asString 
                ?: moreInfo?.get("title")?.asString 
                ?: ""
        )
        val subtitle = JioSaavnImageResolver.unescapeHtml(
            json.get("subtitle")?.asString ?: json.get("header_desc")?.asString ?: ""
        )
        val rawImage = json.get("image")?.asString ?: moreInfo?.get("image")?.asString ?: ""
        val artwork = JioSaavnImageResolver.resolve(rawImage, 500)
        val year = json.get("year")?.asString ?: moreInfo?.get("year")?.asString ?: ""

        var songCount = 0
        var artist = subtitle
        if (moreInfo != null) {
            songCount = moreInfo.get("song_count")?.asString?.toIntOrNull() ?: 0

            // Extract artist from artistMap or primary_artists or music or singers
            val artistMap = moreInfo.getAsJsonObject("artistMap")
            if (artistMap != null) {
                val primary = artistMap.getAsJsonArray("primary_artists")
                if (primary != null && primary.size() > 0) {
                    val names = mutableListOf<String>()
                    for (i in 0 until primary.size()) {
                        val obj = primary.get(i).asJsonObject
                        val name = obj.get("name")?.asString
                        if (!name.isNullOrBlank()) names.add(name)
                    }
                    if (names.isNotEmpty()) {
                        artist = names.joinToString(", ")
                    }
                }
            }
            if (artist.isBlank() || artist == "Various Artists") {
                val primaryArtists = moreInfo.get("primary_artists")?.asString
                val music = moreInfo.get("music")?.asString
                val singers = moreInfo.get("singers")?.asString
                if (!primaryArtists.isNullOrBlank()) {
                    artist = JioSaavnImageResolver.unescapeHtml(primaryArtists)
                } else if (!music.isNullOrBlank()) {
                    artist = JioSaavnImageResolver.unescapeHtml(music)
                } else if (!singers.isNullOrBlank()) {
                    artist = JioSaavnImageResolver.unescapeHtml(singers)
                }
            }
        }

        // Comprehensive song extraction: check list, songs, more_info.songs, more_info.song_list
        val songsList = mutableListOf<Song>()
        val listElement = json.get("list") ?: json.get("songs") ?: moreInfo?.get("songs") ?: moreInfo?.get("song_list") ?: moreInfo?.get("list")
        if (listElement != null && listElement.isJsonArray) {
            songsList.addAll(SongMapper.mapList(listElement.asJsonArray))
        }

        // If title is empty, infer from first song's album
        if (title.isBlank() && songsList.isNotEmpty()) {
            title = songsList.first().album.ifBlank { songsList.first().title }
        }

        return Album(
            id = id,
            title = if (title.isBlank()) "Album" else title,
            subtitle = subtitle,
            artist = if (artist.isBlank()) {
                if (songsList.isNotEmpty()) songsList.first().artist else "Various Artists"
            } else artist,
            artwork = if (artwork.isBlank() && songsList.isNotEmpty()) songsList.first().artwork else artwork,
            year = year,
            songCount = if (songsList.isNotEmpty()) songsList.size else songCount,
            songs = songsList
        )
    }

    fun mapList(array: JsonArray?): List<Album> {
        if (array == null || array.size() == 0) return emptyList()
        val list = mutableListOf<Album>()
        for (i in 0 until array.size()) {
            val item = array.get(i)
            if (item.isJsonObject) {
                list.add(map(item.asJsonObject))
            }
        }
        return list
    }
}

object PlaylistMapper {

    fun map(json: JsonObject): Playlist {
        val moreInfo = json.getAsJsonObject("more_info")
        val id = json.get("id")?.asString 
            ?: json.get("listid")?.asString 
            ?: moreInfo?.get("listid")?.asString 
            ?: json.get("token")?.asString 
            ?: ""

        val title = JioSaavnImageResolver.unescapeHtml(
            json.get("title")?.asString 
                ?: json.get("listname")?.asString 
                ?: moreInfo?.get("listname")?.asString 
                ?: ""
        )
        val subtitle = JioSaavnImageResolver.unescapeHtml(json.get("subtitle")?.asString ?: json.get("header_desc")?.asString ?: "")
        val rawImage = json.get("image")?.asString ?: moreInfo?.get("image")?.asString ?: ""
        val artwork = JioSaavnImageResolver.resolve(rawImage, 500)

        var songCount = 0
        var description = subtitle
        if (moreInfo != null) {
            songCount = moreInfo.get("song_count")?.asString?.toIntOrNull() ?: 0
            val desc = moreInfo.get("description")?.asString ?: moreInfo.get("subtitle")?.asString
            if (!desc.isNullOrBlank()) description = JioSaavnImageResolver.unescapeHtml(desc)
        }

        val songsList = mutableListOf<Song>()
        val listElement = json.get("list") ?: json.get("songs") ?: moreInfo?.get("songs") ?: moreInfo?.get("song_list") ?: moreInfo?.get("list")
        if (listElement != null && listElement.isJsonArray) {
            songsList.addAll(SongMapper.mapList(listElement.asJsonArray))
        }

        val resolvedArt = if (artwork.isBlank() && songsList.isNotEmpty()) songsList.first().artwork else artwork
        val previews = buildList {
            songsList.mapNotNull { it.artwork.takeIf { art -> art.isNotBlank() } }.distinct().take(4).forEach { add(it) }
            if (isEmpty() && resolvedArt.isNotBlank()) add(resolvedArt)
        }
        val finalCount = if (songsList.isNotEmpty()) songsList.size else songCount

        return Playlist(
            id = id,
            title = if (title.isBlank()) "Playlist" else title,
            subtitle = if (subtitle.isBlank() && finalCount > 0) "$finalCount Songs" else subtitle,
            description = description,
            artwork = resolvedArt,
            previewArtworks = previews,
            songCount = finalCount,
            songs = songsList
        )
    }

    fun mapList(array: JsonArray?): List<Playlist> {
        if (array == null || array.size() == 0) return emptyList()
        val list = mutableListOf<Playlist>()
        for (i in 0 until array.size()) {
            val item = array.get(i)
            if (item.isJsonObject) {
                list.add(map(item.asJsonObject))
            }
        }
        return list
    }
}

object ArtistMapper {

    fun map(json: JsonObject): Artist {
        val id = json.get("artistId")?.asString ?: json.get("id")?.asString ?: ""
        val name = JioSaavnImageResolver.unescapeHtml(
            json.get("name")?.asString ?: json.get("title")?.asString ?: ""
        )
        val rawImage = json.get("image")?.asString
            ?: json.get("img")?.asString
            ?: json.get("artist_image")?.asString
            ?: json.getAsJsonObject("more_info")?.get("image")?.asString
            ?: ""
        val artwork = ArtistDpManager.getOriginalDp(name, rawImage)
        if (id.isNotBlank() && artwork.isNotBlank()) {
            ArtistDpManager.putById(id, artwork)
        }
        val role = json.get("role")?.asString ?: ""

        val topSongs = mutableListOf<Song>()
        val topSongsElem = json.get("topSongs")
        if (topSongsElem != null && topSongsElem.isJsonArray) {
            topSongs.addAll(SongMapper.mapList(topSongsElem.asJsonArray))
        }

        val topAlbums = mutableListOf<Album>()
        val topAlbumsElem = json.get("topAlbums")
        if (topAlbumsElem != null && topAlbumsElem.isJsonArray) {
            topAlbums.addAll(AlbumMapper.mapList(topAlbumsElem.asJsonArray))
        }

        return Artist(
            id = id,
            name = if (name.isBlank()) "Artist" else name,
            image = artwork,
            role = role,
            topSongs = topSongs,
            topAlbums = topAlbums
        )
    }

    fun mapList(array: JsonArray?): List<Artist> {
        if (array == null || array.size() == 0) return emptyList()
        val list = mutableListOf<Artist>()
        for (i in 0 until array.size()) {
            val item = array.get(i)
            if (item.isJsonObject) {
                list.add(map(item.asJsonObject))
            }
        }
        return list
    }
}

object LyricsMapper {
    fun map(json: JsonObject?): String {
        if (json == null) return ""
        val lyrics = json.get("lyrics")?.asString ?: ""
        return JioSaavnImageResolver.unescapeHtml(lyrics)
            .replace("<br>", "\n")
            .replace("<br/>", "\n")
            .replace("<br />", "\n")
    }
}

object RecommendationMapper {
    fun map(element: JsonElement?): List<Song> {
        return com.example.data.remote.JioSaavnRecoHelper.parseRecommendations(element)
    }
}

object HomeMapper {

    fun map(json: JsonObject): List<MusicShelf> {
        val shelves = mutableListOf<MusicShelf>()
        val modules = json.getAsJsonObject("modules")

        fun getModuleTitle(key: String, fallback: String): String {
            val mod = modules?.getAsJsonObject(key)
            val title = mod?.get("title")?.asString
            return if (!title.isNullOrBlank()) JioSaavnImageResolver.unescapeHtml(title) else fallback
        }

        fun getModuleSubtitle(key: String, fallback: String): String {
            val mod = modules?.getAsJsonObject(key)
            val sub = mod?.get("subtitle")?.asString
            return if (!sub.isNullOrBlank()) JioSaavnImageResolver.unescapeHtml(sub) else fallback
        }

        val processedKeys = mutableSetOf<String>()

        fun processModule(key: String, defaultTitle: String, defaultSubtitle: String, defaultType: ShelfType? = null) {
            val arr = json.getAsJsonArray(key) ?: return
            if (arr.size() == 0) return
            processedKeys.add(key)
            val items = parseShelfItems(arr)
            if (items.isNotEmpty()) {
                val resolvedType = defaultType ?: determineShelfType(items)
                shelves.add(
                    MusicShelf(
                        id = key,
                        title = getModuleTitle(key, defaultTitle),
                        subtitle = getModuleSubtitle(key, defaultSubtitle),
                        type = resolvedType,
                        items = items
                    )
                )
            }
        }

        // 1. Trending Now
        processModule("new_trending", "Trending Now", "Hottest tracks and albums right now", ShelfType.SONG_HORIZONTAL)

        // 2. New Releases / Albums
        processModule("new_albums", "New Releases", "Fresh albums & singles", ShelfType.ALBUM_HORIZONTAL)

        // 3. Top Playlists
        processModule("top_playlists", "Top Playlists", "Handcrafted for every vibe", ShelfType.PLAYLIST_HORIZONTAL)

        // 4. Top Charts
        processModule("charts", "Top Charts", "Leading music countdowns", ShelfType.PLAYLIST_HORIZONTAL)

        // 5. Featured / Top Artists
        processModule("artist_recos", "Top Artists", "Popular singers & performers", ShelfType.ARTIST_HORIZONTAL)

        // 6. City / Regional Hits
        processModule("city_mod", "City Hits", "Trending music in your city & region", ShelfType.PLAYLIST_HORIZONTAL)

        // 7. Live Radio & Stations
        processModule("radio", "Live Radio & Stations", "Non-stop music by mood & genre", ShelfType.PLAYLIST_HORIZONTAL)

        // 8. Discover & Explore
        processModule("browse_discover", "Discover & Explore", "Handpicked gems and editorial selections", ShelfType.PLAYLIST_HORIZONTAL)

        // 9. Mood & Genre Mixes
        processModule("tag_mixes", "Mood & Genre Mixes", "Curated playlists for every occasion", ShelfType.PLAYLIST_HORIZONTAL)

        // 10. Heavy Rotation
        processModule("heavy_rotation", "Heavy Rotation", "Most played on repeat", ShelfType.SONG_HORIZONTAL)

        // 11. Dynamically render ALL remaining JioSaavn modules present in response
        if (modules != null) {
            for (key in modules.keySet()) {
                if (!processedKeys.contains(key) && json.has(key) && json.get(key).isJsonArray) {
                    val rawTitle = getModuleTitle(
                        key,
                        key.replace("_", " ").split(" ").joinToString(" ") { it.replaceFirstChar(Char::uppercase) }
                    )
                    processModule(key, rawTitle, "Curated music for you")
                }
            }
        }

        return shelves
    }

    private fun parseShelfItems(arr: JsonArray): List<ShelfItem> {
        val items = mutableListOf<ShelfItem>()
        for (i in 0 until arr.size()) {
            try {
                val elem = arr.get(i)
                if (!elem.isJsonObject) continue
                val item = elem.asJsonObject
                val type = item.get("type")?.asString?.lowercase() ?: ""
                when (type) {
                    "song" -> items.add(ShelfItem.SongItem(SongMapper.map(item)))
                    "album" -> items.add(ShelfItem.AlbumItem(AlbumMapper.map(item)))
                    "playlist", "chart", "radio", "radio_station", "channel" -> items.add(ShelfItem.PlaylistItem(PlaylistMapper.map(item)))
                    "artist" -> items.add(ShelfItem.ArtistItem(ArtistMapper.map(item)))
                    else -> {
                        if (item.has("header_desc") || item.has("listid") || item.has("list_count")) {
                            items.add(ShelfItem.PlaylistItem(PlaylistMapper.map(item)))
                        } else if (item.has("music") || item.has("primary_artists") || item.has("media_preview_url")) {
                            items.add(ShelfItem.SongItem(SongMapper.map(item)))
                        } else if (item.has("artist") && item.has("year")) {
                            items.add(ShelfItem.AlbumItem(AlbumMapper.map(item)))
                        } else if (item.has("role") || item.has("follower_count")) {
                            items.add(ShelfItem.ArtistItem(ArtistMapper.map(item)))
                        } else {
                            items.add(ShelfItem.PlaylistItem(PlaylistMapper.map(item)))
                        }
                    }
                }
            } catch (_: Exception) {}
        }
        return items
    }

    private fun determineShelfType(items: List<ShelfItem>): ShelfType {
        val songCount = items.count { it is ShelfItem.SongItem }
        val albumCount = items.count { it is ShelfItem.AlbumItem }
        val artistCount = items.count { it is ShelfItem.ArtistItem }
        val playlistCount = items.count { it is ShelfItem.PlaylistItem }

        return when {
            songCount >= albumCount && songCount >= artistCount && songCount >= playlistCount -> ShelfType.SONG_HORIZONTAL
            albumCount >= artistCount && albumCount >= playlistCount -> ShelfType.ALBUM_HORIZONTAL
            artistCount >= playlistCount -> ShelfType.ARTIST_HORIZONTAL
            else -> ShelfType.PLAYLIST_HORIZONTAL
        }
    }
}
