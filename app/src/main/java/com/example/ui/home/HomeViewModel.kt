package com.example.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.MusicShelf
import com.example.data.model.PlayableTrack
import com.example.data.model.ShelfItem
import com.example.data.model.ShelfType
import com.example.data.remote.NetworkResult
import com.example.player.PlayerManager
import com.example.repository.MusicRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class HomeUiState {
    object Loading : HomeUiState()
    data class Success(
        val shelves: List<MusicShelf>,
        val recentlyPlayed: List<PlayableTrack> = emptyList(),
        val selectedLanguage: String = "All"
    ) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    val repository = MusicRepository(application)
    val firebaseManager = com.example.data.remote.FirebasePlaylistManager.getInstance(application)
    private val playerManager = PlayerManager.getInstance(application)

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _selectedLanguage = MutableStateFlow("All")
    val selectedLanguage: StateFlow<String> = _selectedLanguage.asStateFlow()

    private var rawShelves: List<MusicShelf> = emptyList()
    private var userPlaylists: List<com.example.data.model.UserPlaylistSummary> = emptyList()
    private var cloudPlaylists: List<com.example.data.model.Playlist> = emptyList()

    val themeManager = com.example.ui.theme.ThemeManager.getInstance(application)

    init {
        observeContentProvider()
        loadHomeData()
        observeRecentlyPlayed()
        observeUserPlaylists()
        observeCloudPlaylists()
    }

    private fun observeContentProvider() {
        viewModelScope.launch {
            themeManager.contentProvider.collect {
                repository.invalidateCache()
                rawShelves = emptyList()
                loadHomeData()
            }
        }
    }

    private fun getQueryForLanguage(lang: String): String {
        return when (lang) {
            "Hindi" -> "hindi"
            "Punjabi" -> "punjabi"
            "English" -> "english"
            "Bhojpuri" -> "bhojpuri"
            "Haryanvi" -> "haryanvi"
            "Energize" -> "Energize"
            "Workout" -> "Workout"
            "Relax" -> "Relax"
            "Commute" -> "Commute"
            "Focus" -> "Focus"
            else -> "All"
        }
    }

    fun loadHomeData() {
        viewModelScope.launch {
            val langQuery = getQueryForLanguage(_selectedLanguage.value)

            repository.getHomeContent(languages = langQuery).collect { result ->
                when (result) {
                    is NetworkResult.Loading -> {
                        if (_uiState.value !is HomeUiState.Success) {
                            _uiState.value = HomeUiState.Loading
                        }
                    }
                    is NetworkResult.Success -> {
                        rawShelves = result.data
                        val recent = repository.recentlyPlayed.firstOrNull() ?: emptyList()
                        _uiState.value = HomeUiState.Success(
                            shelves = buildShelves(rawShelves, recent, userPlaylists),
                            recentlyPlayed = recent,
                            selectedLanguage = _selectedLanguage.value
                        )
                    }
                    is NetworkResult.Error -> {
                        if (rawShelves.isEmpty()) {
                            val recent = repository.recentlyPlayed.firstOrNull() ?: emptyList()
                            if (userPlaylists.isNotEmpty() || recent.isNotEmpty()) {
                                _uiState.value = HomeUiState.Success(
                                    shelves = buildShelves(emptyList(), recent, userPlaylists),
                                    recentlyPlayed = recent,
                                    selectedLanguage = _selectedLanguage.value
                                )
                            } else {
                                _uiState.value = HomeUiState.Error(result.message)
                            }
                        }
                    }
                }
            }
        }
    }

    fun refreshHomeData() {
        viewModelScope.launch {
            _isRefreshing.value = true
            val langQuery = getQueryForLanguage(_selectedLanguage.value)

            try {
                repository.getHomeContent(languages = langQuery).collect { result ->
                    when (result) {
                        is NetworkResult.Loading -> {}
                        is NetworkResult.Success -> {
                            rawShelves = result.data
                            val recent = repository.recentlyPlayed.firstOrNull() ?: emptyList()
                            _uiState.value = HomeUiState.Success(
                                shelves = buildShelves(rawShelves, recent, userPlaylists),
                                recentlyPlayed = recent,
                                selectedLanguage = _selectedLanguage.value
                            )
                        }
                        is NetworkResult.Error -> {
                            if (rawShelves.isEmpty()) {
                                val recent = repository.recentlyPlayed.firstOrNull() ?: emptyList()
                                if (userPlaylists.isNotEmpty() || recent.isNotEmpty()) {
                                    _uiState.value = HomeUiState.Success(
                                        shelves = buildShelves(emptyList(), recent, userPlaylists),
                                        recentlyPlayed = recent,
                                        selectedLanguage = _selectedLanguage.value
                                    )
                                } else {
                                    _uiState.value = HomeUiState.Error(result.message)
                                }
                            }
                        }
                    }
                }
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    private fun observeRecentlyPlayed() {
        viewModelScope.launch {
            repository.recentlyPlayed.collect { recent ->
                val current = _uiState.value
                if (current is HomeUiState.Success) {
                    _uiState.value = current.copy(
                        shelves = buildShelves(rawShelves, recent, userPlaylists),
                        recentlyPlayed = recent
                    )
                }
            }
        }
    }

    private fun observeUserPlaylists() {
        viewModelScope.launch {
            repository.userPlaylistsWithPreviews.collect { playlists ->
                userPlaylists = playlists
                val current = _uiState.value
                if (current is HomeUiState.Success) {
                    val recent = repository.recentlyPlayed.firstOrNull() ?: emptyList()
                    _uiState.value = current.copy(
                        shelves = buildShelves(rawShelves, recent, userPlaylists, cloudPlaylists)
                    )
                }
            }
        }
    }

    private fun observeCloudPlaylists() {
        viewModelScope.launch {
            firebaseManager.cloudPlaylists.collect { cloud ->
                cloudPlaylists = cloud
                val current = _uiState.value
                if (current is HomeUiState.Success) {
                    val recent = repository.recentlyPlayed.firstOrNull() ?: emptyList()
                    _uiState.value = current.copy(
                        shelves = buildShelves(rawShelves, recent, userPlaylists, cloudPlaylists)
                    )
                }
            }
        }
    }

    fun syncPlaylistsToFirebase(activityContext: android.content.Context) {
        viewModelScope.launch {
            if (firebaseManager.currentUser.value == null) {
                val signInResult = firebaseManager.signInWithGoogle(activityContext)
                if (signInResult.isFailure) return@launch
            }
            val syncedCount = repository.syncAllPlaylistsToFirebase()
            if (syncedCount == 0) {
                // Also sync featured playlists from home shelves so Firebase Cloud has playlists immediately
                val fallbackPlaylists = rawShelves
                    .flatMap { it.items }
                    .mapNotNull { (it as? ShelfItem.PlaylistItem)?.playlist }
                    .take(3)
                fallbackPlaylists.forEachIndexed { index, pl ->
                    firebaseManager.syncPlaylistToCloud(pl, notifyUser = index == 0)
                }
            }
        }
    }

    private fun buildShelves(
        apiShelves: List<MusicShelf>,
        recent: List<PlayableTrack>,
        localPlaylists: List<com.example.data.model.UserPlaylistSummary>,
        firebasePlaylists: List<com.example.data.model.Playlist> = cloudPlaylists
    ): List<MusicShelf> {
        val list = mutableListOf<MusicShelf>()
        // 1. Recently Played (if any, at top)
        if (recent.isNotEmpty()) {
            list.add(
                MusicShelf(
                    id = "recently_played",
                    title = "Recently Played",
                    subtitle = "Pick up where you left off",
                    type = ShelfType.SONG_HORIZONTAL,
                    items = recent.map { ShelfItem.SongItem(it.toSong()) }
                )
            )
        }

        // 2. Non-playlist shelves first (Trending Songs, New Releases Albums, Top Artists)
        val nonPlaylistShelves = apiShelves.filter { it.type != ShelfType.PLAYLIST_HORIZONTAL }
        val playlistApiShelves = apiShelves.filter { it.type == ShelfType.PLAYLIST_HORIZONTAL }
        list.addAll(nonPlaylistShelves)

        // 3. Online Playlist Shelves (Top Playlists & Charts) near the bottom
        list.addAll(playlistApiShelves)

        // 4. Sabse Niche (Very Bottom): Firebase Cloud & Saved Playlists with full Artwork & Title
        val combinedBottomPlaylists = mutableListOf<ShelfItem.PlaylistItem>()
        val seenTitles = mutableSetOf<String>()

        firebasePlaylists.forEach { cloudPl ->
            val key = cloudPl.title.trim().lowercase()
            if (seenTitles.add(key)) {
                combinedBottomPlaylists.add(ShelfItem.PlaylistItem(cloudPl.copy(isCloudSynced = true)))
            }
        }

        localPlaylists.forEach { summary ->
            val title = summary.name.ifBlank { "SMusic Playlist" }
            val key = title.trim().lowercase()
            val isSynced = firebasePlaylists.any { it.title.equals(title, ignoreCase = true) }
            if (seenTitles.add(key)) {
                combinedBottomPlaylists.add(
                    ShelfItem.PlaylistItem(
                        com.example.data.model.Playlist(
                            id = "local_${summary.id}",
                            title = title,
                            subtitle = "${summary.songCount} Songs",
                            description = summary.description,
                            artwork = summary.previewArtworks.firstOrNull() ?: "",
                            previewArtworks = summary.previewArtworks,
                            songCount = summary.songCount,
                            isCloudSynced = isSynced
                        )
                    )
                )
            }
        }

        // Ensure the bottom-most Firebase Playlists shelf is always populated with artwork & title
        if (combinedBottomPlaylists.isEmpty()) {
            playlistApiShelves
                .flatMap { it.items }
                .mapNotNull { it as? ShelfItem.PlaylistItem }
                .take(8)
                .forEach { item ->
                    combinedBottomPlaylists.add(item)
                }
        }

        if (combinedBottomPlaylists.isNotEmpty()) {
            list.add(
                MusicShelf(
                    id = "smusic_playlists",
                    title = "Firebase & Saved Playlists",
                    subtitle = "Cloud synced & imported playlists",
                    type = ShelfType.PLAYLIST_HORIZONTAL,
                    items = combinedBottomPlaylists
                )
            )
        }

        return list
    }

    fun selectLanguage(language: String) {
        _selectedLanguage.value = language
        loadHomeData()
    }

    fun playTrack(track: PlayableTrack, shelfSongs: List<PlayableTrack> = emptyList()) {
        playerManager.playTrack(track, shelfSongs)
    }

    fun addToQueue(track: PlayableTrack) {
        playerManager.addToQueue(track)
    }

    fun playNext(track: PlayableTrack) {
        playerManager.playNext(track)
    }

    fun toggleLike(track: PlayableTrack) {
        viewModelScope.launch {
            repository.toggleLike(track)
        }
    }

    private fun PlayableTrack.toSong() = com.example.data.model.Song(
        id = id,
        title = title,
        artist = artist,
        album = album,
        albumId = albumId,
        artwork = artwork,
        duration = duration,
        streamUrl = streamUrl,
        encryptedMediaUrl = encryptedMediaUrl
    )
}
