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

    fun invalidateCache() {
        cachedShelves = emptyList()
    }

    /**
     * Loads Home content with shelves (Trending, Albums, Playlists, etc.)
     */
    fun getHomeContent(languages: String = "hindi,english,punjabi,bhojpuri,haryanvi"): Flow<NetworkResult<List<MusicShelf>>> = flow {
        emit(NetworkResult.Loading)

        if (cachedShelves.isNotEmpty()) {
            emit(NetworkResult.Success(cachedShelves))
        }

        try {
            val response = api.getLaunchData(languages = languages)
            if (response.isSuccessful && response.body() != null) {
                val shelves = HomeMapper.map(response.body()!!)
                if (shelves.isNotEmpty()) {
                    cachedShelves = shelves
                    emit(NetworkResult.Success(shelves))
                } else if (cachedShelves.isNotEmpty()) {
                    emit(NetworkResult.Success(cachedShelves))
                } else {
                    val fallback = loadFallbackTrendingShelves()
                    if (fallback.isNotEmpty()) {
                        cachedShelves = fallback
                        emit(NetworkResult.Success(fallback))
                    } else {
                        emit(NetworkResult.Error("No music shelves found"))
                    }
                }
            } else {
                if (cachedShelves.isNotEmpty()) {
                    emit(NetworkResult.Success(cachedShelves))
                } else {
                    val fallback = loadFallbackTrendingShelves()
                    if (fallback.isNotEmpty()) {
                        cachedShelves = fallback
                        emit(NetworkResult.Success(fallback))
                    } else {
                        emit(NetworkResult.Error("Failed to load music: ${response.code()} ${response.message()}"))
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            Log.e(tag, "Home content error: ${e.message}", e)
            if (cachedShelves.isNotEmpty()) {
                emit(NetworkResult.Success(cachedShelves))
            } else {
                val fallback = loadFallbackTrendingShelves()
                if (fallback.isNotEmpty()) {
                    cachedShelves = fallback
                    emit(NetworkResult.Success(fallback))
                } else {
                    emit(NetworkResult.Error("Unable to connect to music service. Please check your internet connection.", e))
                }
            }
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun loadFallbackTrendingShelves(): List<MusicShelf> {
        val shelves = mutableListOf<MusicShelf>()
        try {
            val trendingRes = api.searchSongs(query = "Trending", page = 1, limit = 20)
            if (trendingRes.isSuccessful && trendingRes.body() != null) {
                val arr = trendingRes.body()!!.getAsJsonArray("results")
                val songs = SongMapper.mapList(arr)
                if (songs.isNotEmpty()) {
                    shelves.add(
                        MusicShelf(
                            id = "fallback_trending",
                            title = "Trending Now",
                            subtitle = "Popular tracks for you",
                            type = ShelfType.SONG_HORIZONTAL,
                            items = songs.map { ShelfItem.SongItem(it) }
                        )
                    )
                }
            }
        } catch (_: Throwable) {}

        try {
            val topHitsRes = api.searchSongs(query = "Top Hits", page = 1, limit = 20)
            if (topHitsRes.isSuccessful && topHitsRes.body() != null) {
                val arr = topHitsRes.body()!!.getAsJsonArray("results")
                val songs = SongMapper.mapList(arr)
                if (songs.isNotEmpty()) {
                    shelves.add(
                        MusicShelf(
                            id = "fallback_top_hits",
                            title = "Top Hits",
                            subtitle = "Chart toppers & favorites",
                            type = ShelfType.SONG_HORIZONTAL,
                            items = songs.map { ShelfItem.SongItem(it) }
                        )
                    )
                }
            }
        } catch (_: Throwable) {}

        return shelves
    }

    /**
     * Search songs
     */
    suspend fun searchSongs(query: String, page: Int = 1, limit: Int = 500): NetworkResult<List<Song>> = withContext(Dispatchers.IO) {
        try {
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

            // 5. Fallback: Search by title query
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
            // Check if this is a local user playlist
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
                        role = "Artist",
                        topSongs = songs,
                        topAlbums = emptyList()
                    )
                )
            }

            NetworkResult.Error("Artist details not found")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            NetworkResult.Error(e.message ?: "Artist error", e)
        }
    }

    /**
     * Song details (supports numeric PID, encrypted ID token, and string token fallbacks)
     */
    suspend fun getSongDetails(songIdOrToken: String): Song? = withContext(Dispatchers.IO) {
        try {
            if (songIdOrToken.isBlank()) return@withContext null
            val isNumeric = songIdOrToken.all { it.isDigit() }

            if (isNumeric) {
                val response = api.getSongByPid(pid = songIdOrToken)
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    val parsed = parseSongFromJsonElement(body, songIdOrToken)
                    if (parsed != null) return@withContext parsed
                }
            }

            val tokenResponse = api.getSongByToken(token = songIdOrToken)
            if (tokenResponse.isSuccessful && tokenResponse.body() != null) {
                val body = tokenResponse.body()!!
                val parsed = parseSongFromJsonElement(body, songIdOrToken)
                if (parsed != null) return@withContext parsed
            }

            // Fallback: searchSongs by ID or query
            val search = api.searchSongs(query = songIdOrToken, limit = 5)
            if (search.isSuccessful && search.body() != null) {
                val arr = search.body()!!.getAsJsonArray("results")
                val songs = SongMapper.mapList(arr)
                val matched = songs.firstOrNull { it.id == songIdOrToken || it.token == songIdOrToken } ?: songs.firstOrNull()
                if (matched != null) return@withContext matched
            }
        } catch (e: Exception) {
            Log.w(tag, "Song detail fetch error for $songIdOrToken: ${e.message}")
        }
        null
    }

    private fun parseSongFromJsonElement(element: JsonElement, fallbackId: String): Song? {
        try {
            if (element.isJsonObject) {
                val obj = element.asJsonObject
                if (obj.has(fallbackId)) {
                    val inner = obj.getAsJsonObject(fallbackId)
                    return SongMapper.map(inner)
                }
                if (obj.has("songs")) {
                    val songsArr = obj.getAsJsonArray("songs")
                    if (songsArr != null && songsArr.size() > 0) {
                        return SongMapper.map(songsArr[0].asJsonObject)
                    }
                }
                if (obj.has("id") || obj.has("song") || obj.has("title")) {
                    return SongMapper.map(obj)
                }
            } else if (element.isJsonArray) {
                val arr = element.asJsonArray
                if (arr.size() > 0 && arr[0].isJsonObject) {
                    return SongMapper.map(arr[0].asJsonObject)
                }
            }
        } catch (e: Exception) {
            Log.w(tag, "parseSongFromJsonElement error: ${e.message}")
        }
        return null
    }

    /**
     * Get auto-play recommendations
     */
    suspend fun getRecommendations(songId: String, currentTrack: PlayableTrack? = null): List<Song> = withContext(Dispatchers.IO) {
        try {
            if (songId.isNotBlank() && songId.all { it.isDigit() }) {
                val response = api.getAutoPlayQueue(songId = songId)
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    val songs = if (body.isJsonArray) {
                        SongMapper.mapList(body.asJsonArray)
                    } else if (body.isJsonObject) {
                        val obj = body.asJsonObject
                        val arr = obj.getAsJsonArray("results") ?: obj.getAsJsonArray("data") ?: obj.getAsJsonArray("station")
                        if (arr != null) SongMapper.mapList(arr) else emptyList()
                    } else emptyList()

                    if (songs.isNotEmpty()) {
                        return@withContext songs.filter { it.id != songId }
                    }
                }
            }

            // Fallback: search artist songs for radio mix
            if (currentTrack != null && currentTrack.artist.isNotBlank()) {
                val cleanArtist = currentTrack.artist.split(",", "&", "ft.", "feat.").firstOrNull()?.trim() ?: currentTrack.artist
                val artistSearch = api.searchSongs(query = cleanArtist, limit = 20)
                if (artistSearch.isSuccessful && artistSearch.body() != null) {
                    val arr = artistSearch.body()!!.getAsJsonArray("results")
                    val songs = SongMapper.mapList(arr).filter { it.id != songId && it.title != currentTrack.title }
                    if (songs.isNotEmpty()) {
                        return@withContext songs
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Recommendations error: ${e.message}")
        }
        emptyList()
    }

    /**
     * Resilient audio stream fallback: searches title and artist to obtain pristine 320kbps audio.
     */
    suspend fun resolveAudioStreamFallback(
        track: PlayableTrack,
        quality: StreamUrlResolver.AudioQuality = StreamUrlResolver.AudioQuality.HIGH
    ): String? = withContext(Dispatchers.IO) {
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
                                return@withContext fallbackStream
                            }
                        }
                    }
                }
            } catch (_: Exception) {}
        }
        null
    }

    /**
     * Stream URL resolution
     */
    suspend fun resolveStreamUrl(
        track: PlayableTrack,
        quality: StreamUrlResolver.AudioQuality = StreamUrlResolver.AudioQuality.HIGH
    ): String? = withContext(Dispatchers.IO) {
        var resolved = StreamUrlResolver.resolve(track, quality)
        if (resolved.isNullOrBlank() && track.source != TrackSource.LOCAL && track.id.isNotBlank()) {
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
        playlistDao.insertPlaylist(PlaylistEntity(name = name, description = description, artwork = artwork))
    }

    suspend fun deletePlaylist(id: Long) = withContext(Dispatchers.IO) {
        playlistDao.deletePlaylist(id)
        playlistDao.deleteSongsByPlaylist(id)
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
    }

    suspend fun removeSongFromPlaylist(playlistId: Long, songId: String) = withContext(Dispatchers.IO) {
        playlistDao.removeSongFromPlaylist(playlistId, songId)
    }

    /**
     * Import a JioSaavn playlist into user's local Room playlists
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

    /**
     * Fetch lyrics by lyricsId or songId
     */
    suspend fun getLyrics(lyricsId: String?, songId: String? = null): String? = withContext(Dispatchers.IO) {
        val targetId = when {
            !lyricsId.isNullOrBlank() -> lyricsId
            !songId.isNullOrBlank() -> songId
            else -> return@withContext null
        }
        try {
            val res = api.getLyrics(targetId)
            if (res.isSuccessful) {
                val body = res.body()
                val elem = body?.get("lyrics")
                val lyricsRaw = if (elem != null && !elem.isJsonNull) elem.asString else null
                if (!lyricsRaw.isNullOrBlank()) {
                    return@withContext lyricsRaw.replace("<br>", "\n").replace("<br/>", "\n")
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to fetch lyrics: ${e.message}")
        }
        null
    }
}
