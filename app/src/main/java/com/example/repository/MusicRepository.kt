package com.example.repository

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import com.example.data.local.*
import com.example.data.mapper.*
import com.example.data.model.*
import com.example.data.remote.ArtistDpManager
import com.example.data.remote.JioSaavnApiService
import com.example.data.remote.NetworkResult
import com.example.data.remote.RetrofitClient
import com.example.data.remote.StreamUrlResolver
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class MusicRepository(
    private val context: Context,
    private val api: JioSaavnApiService = RetrofitClient.apiService,
    private val db: SMusicDatabase = SMusicDatabase.getInstance(context)
) {
    private val tag = "MusicRepository"
    private val songDao = db.songDao()
    private val playlistDao = db.playlistDao()
    private val searchHistoryDao = db.searchHistoryDao()
    private val downloadedDao = db.downloadedSongDao()

    // Cached home shelves for instant startup & offline support
    @Volatile
    private var cachedShelves: List<MusicShelf> = emptyList()
    @Volatile
    private var cachedProvider: ContentProvider? = null

    fun invalidateCache() {
        cachedShelves = emptyList()
        cachedProvider = null
    }

    /**
     * Loads Home content with shelves (Trending, Albums, Playlists, etc.)
     */
    fun getHomeContent(languages: String = "hindi,english,punjabi,bhojpuri,haryanvi"): Flow<NetworkResult<List<MusicShelf>>> = flow {
        emit(NetworkResult.Loading)

        val provider = com.example.ui.theme.ThemeManager.getInstance(context).contentProvider.value
        if (cachedProvider != provider) {
            cachedShelves = emptyList()
            cachedProvider = provider
        } else if (cachedShelves.isNotEmpty()) {
            emit(NetworkResult.Success(cachedShelves))
        }

        if (provider == ContentProvider.YT_MUSIC) {
            try {
                val ytShelves = com.example.data.remote.YouTubeMusicProvider.getHomeShelves(languages)
                if (ytShelves.isNotEmpty()) {
                    cachedShelves = ytShelves
                    cachedProvider = ContentProvider.YT_MUSIC
                    emit(NetworkResult.Success(ytShelves))
                    return@flow
                }
            } catch (e: Exception) {
                Log.w(tag, "YT Music home shelves fallback to JioSaavn: ${e.message}")
            }
        }

        try {
            val response = api.getLaunchData(languages = languages)
            if (response.isSuccessful && response.body() != null) {
                val shelves = HomeMapper.map(response.body()!!)
                cachedShelves = shelves
                cachedProvider = provider
                emit(NetworkResult.Success(shelves))
            } else {
                if (cachedShelves.isEmpty()) {
                    emit(NetworkResult.Error("Failed to load music: ${response.code()} ${response.message()}"))
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(tag, "Home content error: ${e.message}", e)
            if (cachedShelves.isEmpty()) {
                emit(NetworkResult.Error("Unable to connect to music service. Please check your internet connection.", e))
            }
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Search songs
     */
     suspend fun searchSongs(query: String, page: Int = 1, limit: Int = 500): NetworkResult<List<Song>> = withContext(Dispatchers.IO) {
         try {
             val provider = com.example.ui.theme.ThemeManager.getInstance(context).contentProvider.value
             if (provider == ContentProvider.YT_MUSIC) {
                 val ytSongs = com.example.data.remote.YouTubeMusicProvider.searchSongs(query)
                 if (ytSongs.isNotEmpty()) {
                     return@withContext NetworkResult.Success(ytSongs)
                 }
             }

             val response = api.searchSongs(query = query, page = page, limit = limit)
             if (response.isSuccessful && response.body() != null) {
                 val resultsArr = response.body()!!.getAsJsonArray("results")
                 val songs = SongMapper.mapList(resultsArr)
                 NetworkResult.Success(songs)
             } else {
                 NetworkResult.Error("Search failed: ${response.message()}")
             }
         } catch (e: CancellationException) {
             throw e
         } catch (e: Exception) {
             NetworkResult.Error(e.message ?: "Unknown search error", e)
         }
     }

    /**
     * Search albums
     */
     suspend fun searchAlbums(query: String, page: Int = 1, limit: Int = 500): NetworkResult<List<Album>> = withContext(Dispatchers.IO) {
         try {
             val provider = com.example.ui.theme.ThemeManager.getInstance(context).contentProvider.value
             if (provider == ContentProvider.YT_MUSIC) {
                 val ytAlbums = com.example.data.remote.YouTubeMusicProvider.searchAlbums(query)
                 if (ytAlbums.isNotEmpty()) {
                     return@withContext NetworkResult.Success(ytAlbums)
                 }
             }
             val response = api.searchAlbums(query = query, page = page, limit = limit)
             if (response.isSuccessful && response.body() != null) {
                 val resultsArr = response.body()!!.getAsJsonArray("results")
                 val albums = AlbumMapper.mapList(resultsArr)
                 NetworkResult.Success(albums)
             } else {
                 NetworkResult.Error("Album search failed")
             }
         } catch (e: CancellationException) {
             throw e
         } catch (e: Exception) {
             NetworkResult.Error(e.message ?: "Album search error", e)
         }
     }

    /**
     * Search playlists
     */
     suspend fun searchPlaylists(query: String, page: Int = 1, limit: Int = 500): NetworkResult<List<Playlist>> = withContext(Dispatchers.IO) {
         try {
             val provider = com.example.ui.theme.ThemeManager.getInstance(context).contentProvider.value
             if (provider == ContentProvider.YT_MUSIC) {
                 val ytPlaylists = com.example.data.remote.YouTubeMusicProvider.searchPlaylists(query)
                 if (ytPlaylists.isNotEmpty()) {
                     return@withContext NetworkResult.Success(ytPlaylists)
                 }
             }
             val response = api.searchPlaylists(query = query, page = page, limit = limit)
             if (response.isSuccessful && response.body() != null) {
                 val resultsArr = response.body()!!.getAsJsonArray("results")
                 val playlists = PlaylistMapper.mapList(resultsArr)
                 NetworkResult.Success(playlists)
             } else {
                 NetworkResult.Error("Playlist search failed")
             }
         } catch (e: CancellationException) {
             throw e
         } catch (e: Exception) {
             NetworkResult.Error(e.message ?: "Playlist search error", e)
         }
     }

    /**
     * Search artists
     */
     suspend fun searchArtists(query: String, page: Int = 1, limit: Int = 500): NetworkResult<List<Artist>> = withContext(Dispatchers.IO) {
         try {
             val provider = com.example.ui.theme.ThemeManager.getInstance(context).contentProvider.value
             if (provider == ContentProvider.YT_MUSIC) {
                 val ytArtists = com.example.data.remote.YouTubeMusicProvider.searchArtists(query)
                 if (ytArtists.isNotEmpty()) {
                     return@withContext NetworkResult.Success(ytArtists)
                 }
             }
             val response = api.searchArtists(query = query, page = page, limit = limit)
             if (response.isSuccessful && response.body() != null) {
                 val body = response.body()!!
                 val resultsArr = body.getAsJsonArray("results")
                     ?: body.getAsJsonArray("data")
                     ?: body.getAsJsonObject("artists")?.getAsJsonArray("data")
                 val artists = ArtistMapper.mapList(resultsArr)
                 if (artists.isNotEmpty()) {
                     return@withContext NetworkResult.Success(artists)
                 }
             }

             // Fast fallback: autocomplete artists
             val ac = getAutocomplete(query)
             if (ac.artists.isNotEmpty()) {
                 return@withContext NetworkResult.Success(ac.artists)
             }

             // Single direct artist fallback with original DP
             val directDp = ArtistDpManager.getOriginalDp(query)
             if (directDp.isNotBlank()) {
                 val directArtist = Artist(
                     id = query,
                     name = query,
                     image = directDp,
                     role = "Artist"
                 )
                 return@withContext NetworkResult.Success(listOf(directArtist))
             }

             NetworkResult.Error("Artist search failed")
         } catch (e: CancellationException) {
             throw e
         } catch (e: Exception) {
             NetworkResult.Error(e.message ?: "Artist search error", e)
         }
     }

    /**
     * Autocomplete suggestions
     */
    suspend fun getAutocomplete(query: String): SearchResultCategory = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext SearchResultCategory(query)
        try {
            val response = api.getAutocomplete(query = query)
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                val songs = body.getAsJsonObject("songs")?.getAsJsonArray("data")?.let { SongMapper.mapList(it) } ?: emptyList()
                val albums = body.getAsJsonObject("albums")?.getAsJsonArray("data")?.let { AlbumMapper.mapList(it) } ?: emptyList()
                val artists = body.getAsJsonObject("artists")?.getAsJsonArray("data")?.let { ArtistMapper.mapList(it) } ?: emptyList()
                val playlists = body.getAsJsonObject("playlists")?.getAsJsonArray("data")?.let { PlaylistMapper.mapList(it) } ?: emptyList()
                return@withContext SearchResultCategory(query, songs, albums, artists, playlists)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(tag, "Autocomplete error: ${e.message}")
        }
        SearchResultCategory(query)
    }

    private fun isRealAlbum(album: Album?): Boolean {
        if (album == null) return false
        val badPhrases = listOf("sample trailer", "testing", "sample trailer - testing")
        if (badPhrases.any { album.title.contains(it, ignoreCase = true) }) return false
        if (album.songs.any { song -> badPhrases.any { song.title.contains(it, ignoreCase = true) } }) return false
        return album.songs.isNotEmpty()
    }

    /**
     * Album details (handles numeric album ID, string token, single-song albums, and title-search fallback)
     */
    suspend fun getAlbumDetails(
        albumId: String,
        titleFallback: String = "",
        artistFallback: String = "",
        artworkFallback: String = ""
    ): NetworkResult<Album> = withContext(Dispatchers.IO) {
        try {
            val provider = com.example.ui.theme.ThemeManager.getInstance(context).contentProvider.value
            if (provider == ContentProvider.YT_MUSIC || albumId.startsWith("yt_")) {
                val ytAlbum = com.example.data.remote.YouTubeMusicProvider.getAlbumDetails(
                    albumIdOrTitle = albumId,
                    fallbackTitle = titleFallback,
                    fallbackArtist = artistFallback
                )
                if (ytAlbum != null && ytAlbum.songs.isNotEmpty()) {
                    return@withContext NetworkResult.Success(ytAlbum)
                }
            }

            var album: Album? = null
            val isNumeric = albumId.all { it.isDigit() }

            // 1. Primary endpoint: webapi.get by id or by token (modern JioSaavn API)
            val primaryResponse = if (isNumeric) {
                api.getAlbumById(id = albumId)
            } else {
                api.getAlbumByToken(token = albumId)
            }

            if (primaryResponse.isSuccessful && primaryResponse.body() != null) {
                val mapped = AlbumMapper.map(primaryResponse.body()!!)
                if (isRealAlbum(mapped)) {
                    album = mapped
                }
            }

            // 2. Fallback: album.getDetailsSimple (numeric) or token fallback
            if (album == null) {
                val fallbackResponse = if (isNumeric) {
                    api.getAlbumDetailsSimple(albumId = albumId)
                } else {
                    api.getAlbumById(id = albumId)
                }
                if (fallbackResponse.isSuccessful && fallbackResponse.body() != null) {
                    val mapped = AlbumMapper.map(fallbackResponse.body()!!)
                    if (isRealAlbum(mapped)) {
                        album = mapped
                    }
                }
            }

            // 3. Fallback: content.getAlbumDetails (legacy) or getAlbumByToken
            if (album == null) {
                val tokenResponse = if (isNumeric) {
                    api.getAlbumDetails(albumId = albumId)
                } else {
                    api.getAlbumByToken(token = albumId)
                }
                if (tokenResponse.isSuccessful && tokenResponse.body() != null) {
                    val mapped = AlbumMapper.map(tokenResponse.body()!!)
                    if (isRealAlbum(mapped)) {
                        album = mapped
                    }
                }
            }

            // 4. Fallback: Single track lookup (in case the release is a single song ID)
            if (album == null && albumId.isNotBlank()) {
                val song = getSongDetails(albumId)
                if (song != null && !song.title.contains("sample trailer", ignoreCase = true)) {
                    album = Album(
                        id = song.albumId.ifBlank { albumId },
                        title = if (titleFallback.isNotBlank()) titleFallback else song.album.ifBlank { song.title },
                        artist = if (artistFallback.isNotBlank()) artistFallback else song.artist,
                        artwork = if (artworkFallback.isNotBlank()) artworkFallback else song.artwork,
                        year = song.year,
                        songCount = 1,
                        songs = listOf(song)
                    )
                }
            }

            // 5. Fallback: Search by title query (e.g. "Aara Ke Sara")
            if (album == null && titleFallback.isNotBlank()) {
                val cleanTitle = titleFallback.replace(Regex("\\(From [^)]*\\)"), "").trim()
                val searchResult = searchSongs(cleanTitle)
                if (searchResult is NetworkResult.Success && searchResult.data.isNotEmpty()) {
                    val validSongs = searchResult.data.filter { !it.title.contains("sample trailer", ignoreCase = true) }
                    if (validSongs.isNotEmpty()) {
                        val first = validSongs.first()
                        album = Album(
                            id = albumId,
                            title = titleFallback,
                            artist = if (artistFallback.isNotBlank()) artistFallback else first.artist,
                            artwork = if (artworkFallback.isNotBlank()) artworkFallback else first.artwork,
                            year = first.year,
                            songCount = validSongs.size,
                            songs = validSongs
                        )
                    }
                }
            }

            if (album != null) {
                NetworkResult.Success(album)
            } else {
                NetworkResult.Error("Failed to load album tracks")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            NetworkResult.Error(e.message ?: "Failed to load album", e)
        }
    }

    /**
     * Playlist details (handles numeric listid, token fallbacks, and local user playlists)
     */
    suspend fun getPlaylistDetails(playlistId: String): NetworkResult<Playlist> = withContext(Dispatchers.IO) {
        try {
            val provider = com.example.ui.theme.ThemeManager.getInstance(context).contentProvider.value
            if (provider == ContentProvider.YT_MUSIC || playlistId.startsWith("yt_")) {
                val ytPlaylist = com.example.data.remote.YouTubeMusicProvider.getPlaylistDetails(playlistId)
                if (ytPlaylist != null && ytPlaylist.songs.isNotEmpty()) {
                    return@withContext NetworkResult.Success(ytPlaylist)
                }
            }

            // 1. Check if this is a Firebase Cloud playlist
            if (playlistId.startsWith("firebase_")) {
                val cloudPl = com.example.data.remote.FirebasePlaylistManager.getInstance(context)
                    .getCachedCloudPlaylist(playlistId)
                if (cloudPl != null) {
                    return@withContext NetworkResult.Success(cloudPl)
                }
            }

            // 2. Check if this is a local user playlist
            if (playlistId.startsWith("local_")) {
                val localId = playlistId.removePrefix("local_").toLongOrNull()
                if (localId != null) {
                    val entity = playlistDao.getPlaylistById(localId)
                    if (entity != null) {
                        val songs = playlistDao.getSongsForPlaylistSync(localId)
                        val songsMapped = songs.map { it.toSong() }
                        val previews = buildList {
                            songsMapped.mapNotNull { it.artwork.takeIf { art -> art.isNotBlank() } }.distinct().take(4).forEach { add(it) }
                            if (entity.artwork.isNotBlank() && !contains(entity.artwork)) add(entity.artwork)
                        }.take(4)
                        val firstArt = entity.artwork.ifBlank { previews.firstOrNull() ?: "" }
                        return@withContext NetworkResult.Success(
                            Playlist(
                                id = playlistId,
                                title = entity.name,
                                subtitle = "${songsMapped.size} Songs",
                                description = entity.description,
                                artwork = firstArt,
                                previewArtworks = previews,
                                songCount = songsMapped.size,
                                songs = songsMapped
                            )
                        )
                    }
                }
            }

            var playlist: Playlist? = null

            val isNumeric = playlistId.all { it.isDigit() }
            val firstResponse = if (isNumeric) {
                api.getPlaylistDetails(listId = playlistId, n = 1000)
            } else {
                api.getPlaylistByToken(token = playlistId, n = 1000)
            }

            if (firstResponse.isSuccessful && firstResponse.body() != null) {
                val mapped = PlaylistMapper.map(firstResponse.body()!!)
                if (mapped.songs.isNotEmpty()) {
                    playlist = mapped
                }
            }

            if (playlist == null || playlist.songs.isEmpty()) {
                val fallbackResponse = if (isNumeric) {
                    api.getPlaylistByToken(token = playlistId, n = 1000)
                } else {
                    api.getPlaylistDetails(listId = playlistId, n = 1000)
                }
                if (fallbackResponse.isSuccessful && fallbackResponse.body() != null) {
                    val mapped = PlaylistMapper.map(fallbackResponse.body()!!)
                    if (mapped.songs.isNotEmpty() || mapped.title != "Playlist") {
                        playlist = mapped
                    }
                }
            }

            // If we have a playlist with a numeric listid and relatively few songs compared to songCount, try playlist.getDetails
            if (playlist != null && playlist.id.isNotBlank() && playlist.id.all { it.isDigit() } && playlist.id != playlistId) {
                try {
                    val detailResp = api.getPlaylistDetails(listId = playlist.id, n = 1000)
                    if (detailResp.isSuccessful && detailResp.body() != null) {
                        val fullMapped = PlaylistMapper.map(detailResp.body()!!)
                        if (fullMapped.songs.size > playlist.songs.size) {
                            playlist = fullMapped
                        }
                    }
                } catch (e: Exception) {
                    Log.w(tag, "Secondary playlist detail fetch error: ${e.message}")
                }
            }

            if (playlist != null) {
                NetworkResult.Success(playlist)
            } else {
                NetworkResult.Error("Failed to load playlist details")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            NetworkResult.Error(e.message ?: "Failed to load playlist", e)
        }
    }

    /**
     * Artist details (handles numeric artistId, artist token, or artist name search)
     */
    suspend fun getArtistDetails(artistId: String, songLimit: Int = 500, albumLimit: Int = 500): NetworkResult<Artist> = withContext(Dispatchers.IO) {
        try {
            val provider = com.example.ui.theme.ThemeManager.getInstance(context).contentProvider.value
            if (provider == ContentProvider.YT_MUSIC || artistId.startsWith("yt_")) {
                val ytArtist = com.example.data.remote.YouTubeMusicProvider.getArtistDetails(artistId)
                if (ytArtist != null && ytArtist.topSongs.isNotEmpty()) {
                    return@withContext NetworkResult.Success(ytArtist)
                }
            }

            if (artistId.all { it.isDigit() }) {
                val response = api.getArtistDetails(artistId = artistId, nSong = songLimit, nAlbum = albumLimit, limit = songLimit)
                if (response.isSuccessful && response.body() != null) {
                    val mapped = ArtistMapper.map(response.body()!!)
                    if (mapped.name.isNotBlank()) {
                        return@withContext NetworkResult.Success(mapped)
                    }
                }
            }

            // Search artist by name or token
            val searchRes = searchArtists(artistId, limit = songLimit)
            if (searchRes is NetworkResult.Success && searchRes.data.isNotEmpty()) {
                val found = searchRes.data.first()
                if (found.id.isNotBlank() && found.id.all { it.isDigit() }) {
                    val detailResp = api.getArtistDetails(artistId = found.id, nSong = songLimit, nAlbum = albumLimit, limit = songLimit)
                    if (detailResp.isSuccessful && detailResp.body() != null) {
                        val mapped = ArtistMapper.map(detailResp.body()!!)
                        if (mapped.name.isNotBlank()) {
                            return@withContext NetworkResult.Success(mapped)
                        }
                    }
                }
                // Fallback using found artist and song search for top songs
                val songsRes = searchSongs(found.name, limit = songLimit)
                val topSongs = if (songsRes is NetworkResult.Success) songsRes.data else emptyList()
                val directImage = ArtistDpManager.getOriginalDp(found.name, found.image)
                return@withContext NetworkResult.Success(
                    Artist(
                        id = found.id.ifBlank { artistId },
                        name = found.name,
                        image = directImage,
                        role = "Singer / Performer",
                        topSongs = topSongs,
                        topAlbums = emptyList()
                    )
                )
            }

            // Fallback: search songs by artist name to build artist profile
            val songsRes = searchSongs(artistId, limit = songLimit)
            if (songsRes is NetworkResult.Success && songsRes.data.isNotEmpty()) {
                val songs = songsRes.data
                val directDp = ArtistDpManager.getOriginalDp(artistId)
                val fallbackImg = if (directDp.isNotBlank()) directDp else (songs.firstOrNull { it.artwork.isNotBlank() }?.artwork ?: "")
                return@withContext NetworkResult.Success(
                    Artist(
                        id = artistId,
                        name = artistId,
                        image = fallbackImg,
                        role = "Featured Artist",
                        topSongs = songs,
                        topAlbums = emptyList()
                    )
                )
            }

            NetworkResult.Error("Failed to load artist details")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            NetworkResult.Error(e.message ?: "Failed to load artist", e)
        }
    }

    /**
     * Helper to safely extract a Song JsonObject from either a JsonArray or JsonObject payload
     */
    private fun extractSongJsonObject(element: JsonElement): JsonObject? {
        if (element.isJsonArray) {
            val arr = element.asJsonArray
            for (i in 0 until arr.size()) {
                val item = arr.get(i)
                if (item.isJsonObject) {
                    val obj = item.asJsonObject
                    if (obj.has("id") || obj.has("song") || obj.has("title") || obj.has("token")) {
                        return obj
                    }
                }
            }
            if (arr.size() > 0 && arr.get(0).isJsonObject) {
                return arr.get(0).asJsonObject
            }
            return null
        }
        if (element.isJsonObject) {
            val obj = element.asJsonObject
            val songsArr = obj.getAsJsonArray("songs") ?: obj.getAsJsonArray("results")
            if (songsArr != null && songsArr.size() > 0) {
                val first = songsArr.get(0)
                if (first.isJsonObject) return first.asJsonObject
            }
            if (obj.has("id") || obj.has("song") || obj.has("title")) {
                return obj
            }
            // Check if key is the song id with nested object:
            for (entry in obj.entrySet()) {
                if (entry.value.isJsonObject) {
                    val inner = entry.value.asJsonObject
                    if (inner.has("id") || inner.has("song") || inner.has("title")) {
                        return inner
                    }
                }
            }
            return obj
        }
        return null
    }

    /**
     * Song details (with numeric PID and string Token fallback)
     */
    suspend fun getSongDetails(songId: String): Song? = withContext(Dispatchers.IO) {
        try {
            // First try by PID if numeric, otherwise try by Token
            val response = if (songId.all { it.isDigit() }) {
                api.getSongByPid(pid = songId)
            } else {
                api.getSongByToken(token = songId)
            }
            if (response.isSuccessful && response.body() != null) {
                val songObj = extractSongJsonObject(response.body()!!)
                if (songObj != null) {
                    return@withContext SongMapper.map(songObj)
                }
            }
            // Fallback: try the other method if first returned empty
            if (songId.all { it.isDigit() }) {
                val fallbackResp = api.getSongByToken(token = songId)
                if (fallbackResp.isSuccessful && fallbackResp.body() != null) {
                    val songObj = extractSongJsonObject(fallbackResp.body()!!)
                    if (songObj != null) {
                        return@withContext SongMapper.map(songObj)
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(tag, "Song details error: ${e.message}")
        }
        null
    }

    /**
     * Recommendations / Autoplay next songs (Endless Radio Engine)
     */
    suspend fun getRecommendations(songId: String, currentTrack: PlayableTrack? = null): List<Song> = withContext(Dispatchers.IO) {
        val targetPid = if (songId.all { it.isDigit() }) {
            songId
        } else {
            // Lookup numeric PID from token / custom ID
            val details = getSongDetails(songId)
            details?.id?.takeIf { it.all { ch -> ch.isDigit() } } ?: songId
        }

        if (targetPid.isNotBlank()) {
            try {
                // 1. Direct Recommendation Queue
                val response = api.getRecommendations(pid = targetPid)
                if (response.isSuccessful && response.body() != null) {
                    val songs = RecommendationMapper.map(response.body()!!)
                    if (songs.isNotEmpty()) {
                        return@withContext songs.filter { it.id != targetPid }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(tag, "Direct recommendation error for pid $targetPid: ${e.message}")
            }

            try {
                // 2. AutoPlay queue fallback
                val autoPlayResp = api.getAutoPlayQueue(songId = targetPid)
                if (autoPlayResp.isSuccessful && autoPlayResp.body() != null) {
                    val songs = RecommendationMapper.map(autoPlayResp.body()!!)
                    if (songs.isNotEmpty()) {
                        return@withContext songs.filter { it.id != targetPid }
                    }
                }
            } catch (_: Exception) {}

            try {
                // 3. WebRadio Station fallback
                val radioResp = api.createRadioStation(entityId = targetPid)
                if (radioResp.isSuccessful && radioResp.body() != null) {
                    val radioObj = radioResp.body()!!.let { if (it.isJsonObject) it.asJsonObject else null }
                    val stationId = radioObj?.get("stationid")?.asString
                    if (!stationId.isNullOrBlank()) {
                        val stationSongsResp = api.getRadioSongs(stationId = stationId, count = 25)
                        if (stationSongsResp.isSuccessful && stationSongsResp.body() != null) {
                            val stationSongs = RecommendationMapper.map(stationSongsResp.body()!!)
                            if (stationSongs.isNotEmpty()) {
                                return@withContext stationSongs.filter { it.id != targetPid }
                            }
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        // 4. Artist top songs fallback for high-relevance radio continuation
        val artistQuery = currentTrack?.artist ?: ""
        if (artistQuery.isNotBlank() && !artistQuery.equals("Unknown", ignoreCase = true) && !artistQuery.equals("Various Artists", ignoreCase = true)) {
            try {
                val primaryArtist = artistQuery.split(",", "&", "feat.", "ft.").firstOrNull()?.trim() ?: artistQuery
                val artistSearchResult = searchSongs(query = primaryArtist, limit = 25)
                if (artistSearchResult is NetworkResult.Success && artistSearchResult.data.isNotEmpty()) {
                    val filtered = artistSearchResult.data.filter { it.id != targetPid && it.id != songId }
                    if (filtered.isNotEmpty()) {
                        return@withContext filtered
                    }
                }
            } catch (_: Exception) {}
        }

        // 5. Final fallback: use trending songs or cached home shelf if available
        val trendingSongs = cachedShelves.firstOrNull { it.id == "trending" || it.id.contains("trending", ignoreCase = true) }
            ?.items?.mapNotNull { if (it is ShelfItem.SongItem) it.song else null }
            ?.filter { it.id != targetPid && it.id != songId }
            ?: emptyList()

        if (trendingSongs.isNotEmpty()) {
            return@withContext trendingSongs
        }

        // 6. If even cached is empty, query fresh home launch songs
        try {
            val homeLaunch = api.getHomeLaunch()
            if (homeLaunch.isSuccessful && homeLaunch.body() != null) {
                val shelves = HomeMapper.map(homeLaunch.body()!!)
                val firstSongs = shelves.flatMap { shelf ->
                    shelf.items.mapNotNull { if (it is ShelfItem.SongItem) it.song else null }
                }.filter { it.id != targetPid && it.id != songId }
                if (firstSongs.isNotEmpty()) {
                    return@withContext firstSongs
                }
            }
        } catch (_: Exception) {}

        emptyList()
    }

    /**
     * Lyrics
     */
    suspend fun getLyrics(lyricsId: String, song: Song?): String = withContext(Dispatchers.IO) {
        if (lyricsId.isNotBlank()) {
            try {
                val response = api.getLyrics(lyricsId = lyricsId)
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    val text = LyricsMapper.map(body)
                    if (text.isNotBlank()) return@withContext text
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(tag, "Lyrics endpoint error: ${e.message}")
            }
        }

        // Fallback to song's lyrics snippet if present
        if (!song?.lyricsSnippet.isNullOrBlank()) {
            return@withContext song!!.lyricsSnippet
        }
        "Lyrics not available for this song."
    }

    /**
     * Resilient audio stream fallback: searches title and artist to obtain pristine 320kbps audio.
     */
    suspend fun resolveAudioStreamFallback(
        track: PlayableTrack,
        quality: StreamUrlResolver.AudioQuality = StreamUrlResolver.AudioQuality.HIGH
    ): String? = withContext(Dispatchers.IO) {
        println("[SMusic-Terminal] Resolving audio stream fallback for: '${track.title}' by '${track.artist}' (ID: ${track.id})")
        val cleanTitle = track.title
            .replace(Regex("\\[.*?\\]|\\(.*?\\)"), "")
            .replace(Regex("(?i)ft\\.?|feat\\.?|official|video|audio|lyrics|hd|4k"), "")
            .trim()
        val queries = listOf(
            "$cleanTitle ${track.artist}".trim(),
            cleanTitle,
            track.title
        ).distinct().filter { it.isNotBlank() }

        for (q in queries) {
            try {
                println("[SMusic-Terminal] Searching audio match query: '$q'")
                val searchRes = api.searchSongs(query = q, limit = 5)
                if (searchRes.isSuccessful && searchRes.body() != null) {
                    val arr = searchRes.body()!!.getAsJsonArray("results")
                    if (arr != null && arr.size() > 0) {
                        val songs = SongMapper.mapList(arr)
                        val match = songs.firstOrNull { it.encryptedMediaUrl.isNotBlank() } ?: songs.firstOrNull()
                        if (match != null) {
                            val matchTrack = match.toPlayableTrack()
                            val fallbackStream = StreamUrlResolver.resolve(matchTrack, quality)
                            if (!fallbackStream.isNullOrBlank()) {
                                println("[SMusic-Terminal-SUCCESS] Audio stream resolved via match '${match.title}': $fallbackStream")
                                return@withContext fallbackStream
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                println("[SMusic-Terminal-WARN] Fallback search error for query '$q': ${e.message}")
            }
        }
        println("[SMusic-Terminal-ERROR] No fallback audio stream could be resolved for '${track.title}'")
        null
    }

    /**
     * Stream URL resolution (with automatic details lookup and resilient audio engine fallback)
     */
    suspend fun resolveStreamUrl(
        track: PlayableTrack,
        quality: StreamUrlResolver.AudioQuality = StreamUrlResolver.AudioQuality.HIGH
    ): String? = withContext(Dispatchers.IO) {
        println("[SMusic-Terminal] Starting stream URL resolution for: '${track.title}' (ID: ${track.id}, Source: ${track.source})")

        if (track.id.startsWith("yt_") || track.source == TrackSource.YOUTUBE_MUSIC) {
            val ytUrl = com.example.data.remote.YouTubeMusicProvider.resolveStreamUrl(track.id)
            if (!ytUrl.isNullOrBlank()) {
                println("[SMusic-Terminal-SUCCESS] Resolved direct YouTube Music stream for '${track.title}'")
                return@withContext ytUrl
            }
            println("[SMusic-Terminal] Direct YouTube stream unavailable. Engaging high-fidelity audio stream fallback...")
            val fallbackUrl = resolveAudioStreamFallback(track, quality)
            if (!fallbackUrl.isNullOrBlank()) {
                return@withContext fallbackUrl
            }
        }

        var resolved = StreamUrlResolver.resolve(track, quality)
        if (resolved.isNullOrBlank() && track.source != TrackSource.LOCAL && track.id.isNotBlank()) {
            // Proactively query song details if track was generated without encrypted media URL
            val details = getSongDetails(track.id)
            if (details != null && details.encryptedMediaUrl.isNotBlank()) {
                val updatedTrack = track.copy(
                    encryptedMediaUrl = details.encryptedMediaUrl,
                    mediaPreviewUrl = details.mediaPreviewUrl,
                    artwork = if (track.artwork.isBlank()) details.artwork else track.artwork,
                    duration = if (track.duration <= 0) details.duration else track.duration
                )
                resolved = StreamUrlResolver.resolve(updatedTrack, quality)
            }
        }

        if (resolved.isNullOrBlank() && track.source != TrackSource.LOCAL) {
            resolved = resolveAudioStreamFallback(track, quality)
        }

        if (resolved != null) {
            println("[SMusic-Terminal-SUCCESS] Final resolved stream URL for '${track.title}': $resolved")
        } else {
            println("[SMusic-Terminal-ERROR] Failed to resolve any stream URL for '${track.title}' (ID: ${track.id})")
        }
        resolved
    }

    // --- LOCAL PERSISTENCE ---

    val likedSongs: Flow<List<PlayableTrack>> = songDao.getAllLikedSongs()
        .map { list -> list.map { it.toPlayableTrack() } }
        .flowOn(Dispatchers.IO)

    fun isSongLiked(id: String): Flow<Boolean> = songDao.isLiked(id).flowOn(Dispatchers.IO)

    suspend fun toggleLike(track: PlayableTrack) = withContext(Dispatchers.IO) {
        val currentlyLiked = songDao.isLikedSync(track.id)
        if (currentlyLiked) {
            songDao.deleteLikedSong(track.id)
        } else {
            songDao.insertLikedSong(
                LikedSongEntity(
                    id = track.id,
                    title = track.title,
                    artist = track.artist,
                    album = track.album,
                    albumId = track.albumId,
                    artwork = track.artwork,
                    duration = track.duration,
                    streamUrl = track.streamUrl,
                    encryptedMediaUrl = track.encryptedMediaUrl,
                    mediaPreviewUrl = track.mediaPreviewUrl
                )
            )
        }
    }

    val recentlyPlayed: Flow<List<PlayableTrack>> = songDao.getRecentlyPlayed()
        .map { list -> list.map { it.toPlayableTrack() } }
        .flowOn(Dispatchers.IO)

    suspend fun recordRecentlyPlayed(track: PlayableTrack) = withContext(Dispatchers.IO) {
        songDao.deleteRecentlyPlayedBySongId(track.id)
        songDao.insertRecentlyPlayed(
            RecentlyPlayedEntity(
                songId = track.id,
                title = track.title,
                artist = track.artist,
                album = track.album,
                albumId = track.albumId,
                artwork = track.artwork,
                duration = track.duration,
                streamUrl = track.streamUrl,
                encryptedMediaUrl = track.encryptedMediaUrl,
                mediaPreviewUrl = track.mediaPreviewUrl,
                playedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun clearRecentlyPlayed() = withContext(Dispatchers.IO) {
        songDao.clearRecentlyPlayed()
    }

    // --- DOWNLOADED SONGS & OFFLINE PLAYBACK ---

    val downloadedSongs: Flow<List<PlayableTrack>> = downloadedDao.getAllDownloadedSongs()
        .map { list ->
            list.filter { it.downloadStatus == "COMPLETED" && (it.localFilePath.isBlank() || java.io.File(it.localFilePath).exists()) }
                .map { it.toPlayableTrack() }
        }
        .flowOn(Dispatchers.IO)

    fun isSongDownloaded(id: String): Flow<Boolean> = downloadedDao.isDownloaded(id).flowOn(Dispatchers.IO)

    suspend fun isSongDownloadedSync(id: String): Boolean = withContext(Dispatchers.IO) {
        downloadedDao.isDownloadedSync(id)
    }

    suspend fun getDownloadedSong(id: String): DownloadedSongEntity? = withContext(Dispatchers.IO) {
        downloadedDao.getDownloadedSong(id)
    }

    suspend fun deleteDownload(id: String) = withContext(Dispatchers.IO) {
        val entity = downloadedDao.getDownloadedSong(id)
        if (entity != null) {
            if (entity.localFilePath.isNotBlank()) {
                val f = java.io.File(entity.localFilePath)
                if (f.exists()) f.delete()
            }
            if (entity.localArtworkPath.isNotBlank()) {
                val artUri = Uri.parse(entity.localArtworkPath)
                artUri.path?.let { p ->
                    val af = java.io.File(p)
                    if (af.exists()) af.delete()
                }
            }
            downloadedDao.deleteDownloadedSong(id)
        }
    }

    // Playlists
    val userPlaylists: Flow<List<PlaylistEntity>> = playlistDao.getAllPlaylists().flowOn(Dispatchers.IO)

    val userPlaylistsWithPreviews: Flow<List<com.example.data.model.UserPlaylistSummary>> =
        kotlinx.coroutines.flow.combine(
            playlistDao.getAllPlaylists(),
            playlistDao.getAllPlaylistSongs()
        ) { playlists, allSongs ->
            val songsByPlaylist = allSongs.groupBy { it.playlistId }
            playlists.map { playlist ->
                val songs = songsByPlaylist[playlist.id] ?: emptyList()
                val previews = buildList {
                    songs.mapNotNull { it.artwork.takeIf { art -> art.isNotBlank() } }
                        .map { com.example.data.remote.JioSaavnImageResolver.resolve(it, 500).ifBlank { it } }
                        .distinct()
                        .take(4)
                        .forEach { add(it) }
                    if (playlist.artwork.isNotBlank()) {
                        val resolved = com.example.data.remote.JioSaavnImageResolver.resolve(playlist.artwork, 500).ifBlank { playlist.artwork }
                        if (!contains(resolved)) add(resolved)
                    }
                }.take(4)
                com.example.data.model.UserPlaylistSummary(
                    id = playlist.id,
                    name = playlist.name,
                    description = playlist.description,
                    songCount = songs.size,
                    previewArtworks = previews,
                    createdAt = playlist.createdAt
                )
            }
        }.flowOn(Dispatchers.IO)

    suspend fun createPlaylist(name: String, description: String = "", artwork: String = ""): Long = withContext(Dispatchers.IO) {
        val id = playlistDao.insertPlaylist(PlaylistEntity(name = name, description = description, artwork = artwork))
        syncLocalPlaylistToFirebase(id, notifyUser = false)
        id
    }

    suspend fun deletePlaylist(id: Long) = withContext(Dispatchers.IO) {
        playlistDao.deletePlaylist(id)
        playlistDao.deleteSongsByPlaylist(id)
        com.example.data.remote.FirebasePlaylistManager.getInstance(context).deleteCloudPlaylist("local_$id")
    }

    fun getPlaylistSongs(playlistId: Long): Flow<List<PlayableTrack>> =
        playlistDao.getSongsForPlaylist(playlistId)
            .map { list -> list.map { it.toPlayableTrack() } }
            .flowOn(Dispatchers.IO)

    suspend fun addSongToPlaylist(playlistId: Long, track: PlayableTrack) = withContext(Dispatchers.IO) {
        playlistDao.insertPlaylistSong(
            PlaylistSongEntity(
                playlistId = playlistId,
                songId = track.id,
                title = track.title,
                artist = track.artist,
                album = track.album,
                albumId = track.albumId,
                artwork = track.artwork,
                duration = track.duration,
                streamUrl = track.streamUrl,
                encryptedMediaUrl = track.encryptedMediaUrl
            )
        )
        syncLocalPlaylistToFirebase(playlistId, notifyUser = false)
    }

    suspend fun removeSongFromPlaylist(playlistId: Long, songId: String) = withContext(Dispatchers.IO) {
        playlistDao.removeSongFromPlaylist(playlistId, songId)
        syncLocalPlaylistToFirebase(playlistId, notifyUser = false)
    }

    suspend fun syncLocalPlaylistToFirebase(playlistId: Long, notifyUser: Boolean = true) = withContext(Dispatchers.IO) {
        val entity = playlistDao.getPlaylistById(playlistId) ?: return@withContext
        val songs = playlistDao.getSongsForPlaylistSync(playlistId).map { it.toSong() }
        val previews = buildList {
            songs.mapNotNull { it.artwork.takeIf { art -> art.isNotBlank() } }.distinct().take(4).forEach { add(it) }
            if (entity.artwork.isNotBlank() && !contains(entity.artwork)) add(entity.artwork)
        }.take(4)
        val cloudPl = Playlist(
            id = "local_${entity.id}",
            title = entity.name.ifBlank { "SMusic Playlist" },
            subtitle = "${songs.size} Songs",
            description = entity.description,
            artwork = entity.artwork.ifBlank { previews.firstOrNull() ?: "" },
            previewArtworks = previews,
            songCount = songs.size,
            songs = songs,
            isCloudSynced = true
        )
        com.example.data.remote.FirebasePlaylistManager.getInstance(context)
            .syncPlaylistToCloud(cloudPl, notifyUser = notifyUser)
    }

    suspend fun syncAllPlaylistsToFirebase(): Int = syncAllLocalPlaylistsToFirebase()

    suspend fun syncAllLocalPlaylistsToFirebase(): Int = withContext(Dispatchers.IO) {
        val allLocal = playlistDao.getAllPlaylists().firstOrNull() ?: emptyList()
        var syncedCount = 0
        for (entity in allLocal) {
            syncLocalPlaylistToFirebase(entity.id, notifyUser = false)
            syncedCount++
        }
        // If user has no local playlists yet, also sync top playlists from cached shelves so Firebase has playlists
        if (syncedCount == 0 && cachedShelves.isNotEmpty()) {
            val topPl = cachedShelves
                .flatMap { it.items }
                .mapNotNull { (it as? ShelfItem.PlaylistItem)?.playlist }
                .take(4)
            for (pl in topPl) {
                val ok = com.example.data.remote.FirebasePlaylistManager.getInstance(context)
                    .syncPlaylistToCloud(pl, notifyUser = false)
                if (ok) syncedCount++
            }
        }
        syncedCount
    }

    /**
     * Import a JioSaavn playlist into user's local Room playlists and Firebase Cloud
     */
    suspend fun importJioSaavnPlaylistToLocal(playlist: Playlist): Long = withContext(Dispatchers.IO) {
        val primaryArt = playlist.artwork.ifBlank {
            playlist.songs.firstOrNull { it.artwork.isNotBlank() }?.artwork ?: ""
        }
        val playlistId = playlistDao.insertPlaylist(
            PlaylistEntity(
                name = playlist.title.ifBlank { "SMusic Playlist" },
                description = playlist.description.ifBlank { "Imported from JioSaavn (${playlist.songs.size} tracks)" },
                artwork = primaryArt
            )
        )
        val entities = playlist.songs.map { song ->
            PlaylistSongEntity(
                playlistId = playlistId,
                songId = song.id,
                title = song.title,
                artist = song.artist,
                album = song.album,
                albumId = song.albumId,
                artwork = song.artwork,
                duration = song.duration,
                streamUrl = song.streamUrl,
                encryptedMediaUrl = song.encryptedMediaUrl
            )
        }
        if (entities.isNotEmpty()) {
            playlistDao.insertPlaylistSongs(entities)
        }
        // Notify & sync to Firebase Cloud if signed in
        com.example.player.SMusicNotificationHelper.showPlaylistSyncNotification(
            context = context,
            playlistId = "local_$playlistId",
            playlistTitle = playlist.title.ifBlank { "SMusic Playlist" },
            songCount = playlist.songs.size,
            artworkUrl = primaryArt
        )
        syncLocalPlaylistToFirebase(playlistId, notifyUser = false)
        playlistId
    }

    /**
     * Fetch playlist using JioSaavn token or URL
     */
    suspend fun fetchPlaylistByToken(tokenOrUrl: String): NetworkResult<Playlist> = withContext(Dispatchers.IO) {
        val token = com.musicx.app.utils.PlaylistLinkParser.extractToken(tokenOrUrl) ?: tokenOrUrl.trim()
        if (token.isBlank()) {
            return@withContext NetworkResult.Error("Invalid JioSaavn playlist link or token")
        }
        getPlaylistDetails(token)
    }

    // Search History
    val searchHistory: Flow<List<String>> = searchHistoryDao.getRecentSearches()
        .map { list -> list.map { it.query } }
        .flowOn(Dispatchers.IO)

    suspend fun addSearchQuery(query: String) = withContext(Dispatchers.IO) {
        if (query.isNotBlank()) {
            searchHistoryDao.deleteSearch(query.trim())
            searchHistoryDao.insertSearch(SearchHistoryEntity(query = query.trim()))
        }
    }

    suspend fun clearSearchHistory() = withContext(Dispatchers.IO) {
        searchHistoryDao.clearHistory()
    }

    /**
     * Scans local audio files from MediaStore
     */
    suspend fun scanLocalAudio(): List<PlayableTrack> = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<PlayableTrack>()
        try {
            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.YEAR,
                MediaStore.Audio.Media.ALBUM_ID
            )

            val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
            val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val yearCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
                val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

                while (cursor.moveToNext()) {
                    val mediaId = cursor.getLong(idCol)
                    val title = cursor.getString(titleCol) ?: "Unknown Track"
                    val artist = cursor.getString(artistCol) ?: "Unknown Artist"
                    val album = cursor.getString(albumCol) ?: "Unknown Album"
                    val durationMs = cursor.getLong(durationCol)
                    val year = cursor.getString(yearCol) ?: ""
                    val albumId = cursor.getLong(albumIdCol)

                    val contentUri: Uri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        mediaId
                    )

                    // Album art URI
                    val artworkUri = ContentUris.withAppendedId(
                        Uri.parse("content://media/external/audio/albumart"),
                        albumId
                    ).toString()

                    tracks.add(
                        PlayableTrack(
                            id = "local_$mediaId",
                            title = title,
                            artist = artist,
                            album = album,
                            artwork = artworkUri,
                            duration = durationMs / 1000,
                            streamUrl = contentUri.toString(),
                            source = TrackSource.LOCAL,
                            year = year
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Local audio scan failed: ${e.message}", e)
        }
        tracks
    }
}
