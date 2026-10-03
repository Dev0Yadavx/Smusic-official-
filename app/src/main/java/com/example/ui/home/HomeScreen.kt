package com.example.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.*
import com.example.player.PlayerManager
import com.example.ui.common.*
import com.example.ui.theme.AppFontFamily
import com.example.ui.theme.ThemeManager
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    playerManager: PlayerManager,
    onNavigateToSearch: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAlbum: (Album) -> Unit,
    onNavigateToPlaylist: (String) -> Unit,
    onNavigateToArtist: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val themeManager = remember { ThemeManager.getInstance(context) }
    val userNickname by themeManager.userNickname.collectAsStateWithLifecycle()
    val userAvatarEmoji by themeManager.userAvatarEmoji.collectAsStateWithLifecycle()
    val userAvatarImageUri by themeManager.userAvatarImageUri.collectAsStateWithLifecycle()
    var showProfileDialog by remember { mutableStateOf(false) }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val currentPlayingTrack by playerManager.currentTrack.collectAsStateWithLifecycle()
    val pullRefreshState = rememberPullToRefreshState()
    val homeListState = rememberLazyListState()
    val isScrolled by remember {
        derivedStateOf { homeListState.firstVisibleItemIndex > 0 || homeListState.firstVisibleItemScrollOffset > 10 }
    }

    var selectedTrackForOptions by remember { mutableStateOf<PlayableTrack?>(null) }
    var isTrackLiked by remember { mutableStateOf(false) }

    // Clean Time-Based Greeting
    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 4..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            in 17..21 -> "Good evening"
            else -> "Good night"
        }
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.refreshHomeData() },
        state = pullRefreshState,
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent),
        indicator = {
            if (isRefreshing) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(top = 100.dp)
                ) {
                    PullToRefreshDefaults.Indicator(
                        state = pullRefreshState,
                        isRefreshing = true,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    ) {
        when (val state = uiState) {
            is HomeUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.testTag("home_loading_indicator")
                    )
                }
            }
            is HomeUiState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Unable to load music",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { viewModel.loadHomeData() },
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.testTag("retry_button")
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Retry")
                    }
                }
            }
            is HomeUiState.Success -> {
                LazyColumn(
                    state = homeListState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 158.dp, bottom = 165.dp)
                ) {
                    items(state.shelves, key = { it.id }) { shelf ->
                        HomeShelfSection(
                            shelf = shelf,
                            currentPlayingId = currentPlayingTrack?.id,
                            onPlaySong = { song, shelfSongs ->
                                viewModel.playTrack(song.toPlayableTrack(), shelfSongs.map { it.toPlayableTrack() })
                            },
                            onMoreSong = { song ->
                                selectedTrackForOptions = song.toPlayableTrack()
                            },
                            onAlbumClick = { album ->
                                if (album.id.isNotBlank()) onNavigateToAlbum(album)
                            },
                            onPlaylistClick = { playlist ->
                                if (playlist.id.isNotBlank()) onNavigateToPlaylist(playlist.id)
                            },
                            onArtistClick = { artist ->
                                if (artist.id.isNotBlank()) onNavigateToArtist(artist.id)
                            }
                        )
                    }
                }
            }
        }

        // Fixed Sticky Header with Large M3 Bottom Rounding
        val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

        Surface(
            shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp),
            color = if (isDark) MaterialTheme.colorScheme.surface.copy(alpha = if (isScrolled) 0.95f else 0.90f) else Color.White,
            tonalElevation = if (isScrolled) 6.dp else 1.dp,
            shadowElevation = if (isScrolled) 8.dp else 2.dp,
            border = BorderStroke(
                width = 1.dp,
                color = if (isDark) Color.White.copy(alpha = 0.12f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.40f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "SMusic",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 26.sp,
                                letterSpacing = (-0.5).sp,
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.tertiary
                                    )
                                )
                            )
                        )

                        // Greeting Pill with Avatar
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    if (isDark) Color(0xFF1B1B24).copy(alpha = 0.85f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f)
                                )
                                .border(
                                    1.dp,
                                    if (isDark) Color.White.copy(alpha = 0.16f)
                                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.60f),
                                    RoundedCornerShape(20.dp)
                                )
                                .clickable { showProfileDialog = true }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                                .testTag("home_user_profile_pill")
                        ) {
                            UserAvatarBadge(
                                emoji = userAvatarEmoji,
                                customImageUri = userAvatarImageUri,
                                size = 28.dp,
                                fontSize = 14.sp
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                                Text(
                                    text = greeting,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                                Text(
                                    text = userNickname,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Search & Settings Buttons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onNavigateToSearch,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isDark) Color(0xFF1B1B24).copy(alpha = 0.85f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f)
                                )
                                .border(
                                    1.dp,
                                    if (isDark) Color.White.copy(alpha = 0.16f)
                                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.60f),
                                    CircleShape
                                )
                                .testTag("home_search_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(
                            onClick = onNavigateToSettings,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isDark) Color(0xFF1B1B24).copy(alpha = 0.85f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f)
                                )
                                .border(
                                    1.dp,
                                    if (isDark) Color.White.copy(alpha = 0.16f)
                                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.60f),
                                    CircleShape
                                )
                                .testTag("home_settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Settings,
                                contentDescription = "Settings",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showProfileDialog) {
        UserProfileM3CardDialog(
            themeManager = themeManager,
            onDismiss = { showProfileDialog = false }
        )
    }

    var trackForAddToPlaylist by remember { mutableStateOf<PlayableTrack?>(null) }

    selectedTrackForOptions?.let { track ->
        TrackOptionsBottomSheet(
            track = track,
            isLiked = isTrackLiked,
            onDismiss = { selectedTrackForOptions = null },
            onPlayNow = { viewModel.playTrack(track) },
            onPlayNext = { viewModel.playNext(track) },
            onAddToQueue = { viewModel.addToQueue(track) },
            onToggleLike = {
                viewModel.toggleLike(track)
                isTrackLiked = !isTrackLiked
            },
            onAddToPlaylist = {
                trackForAddToPlaylist = track
            },
            onViewAlbum = if (track.albumId.isNotBlank()) {
                { onNavigateToAlbum(Album(id = track.albumId, title = track.album, artist = track.artist, artwork = track.artwork)) }
            } else null
        )
    }

    trackForAddToPlaylist?.let { track ->
        AddToPlaylistBottomSheet(
            track = track,
            repository = viewModel.repository,
            onDismiss = { trackForAddToPlaylist = null }
        )
    }
}

@Composable
fun HomeShelfSection(
    shelf: MusicShelf,
    currentPlayingId: String?,
    onPlaySong: (Song, List<Song>) -> Unit,
    onMoreSong: (Song) -> Unit,
    onAlbumClick: (Album) -> Unit,
    onPlaylistClick: (Playlist) -> Unit,
    onArtistClick: (Artist) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .testTag("home_shelf_${shelf.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = shelf.title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (shelf.type) {
            ShelfType.SONG_HORIZONTAL -> {
                val songs = remember(shelf) {
                    shelf.items.mapNotNull { if (it is ShelfItem.SongItem) it.song else null }
                }
                val chunks = remember(songs) {
                    songs.chunked(4)
                }
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(chunks, key = { chunk -> "col_${shelf.id}_${chunk.firstOrNull()?.id ?: chunk.hashCode()}" }) { colSongs ->
                        Column(
                            modifier = Modifier.width(300.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            colSongs.forEach { song ->
                                key(song.id) {
                                    QuickPickRowItem(
                                        song = song,
                                        isPlaying = currentPlayingId == song.id,
                                        onClick = { onPlaySong(song, songs) },
                                        onMoreClick = { onMoreSong(song) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
            ShelfType.ARTIST_HORIZONTAL -> {
                val artists = remember(shelf) {
                    shelf.items.mapNotNull { if (it is ShelfItem.ArtistItem) it.artist else null }
                }
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(artists, key = { "art_${shelf.id}_${it.id}" }) { artist ->
                        ArtistCard(
                            artist = artist,
                            onClick = { onArtistClick(artist) }
                        )
                    }
                }
            }
            ShelfType.ALBUM_HORIZONTAL -> {
                val albums = remember(shelf) {
                    shelf.items.mapNotNull { if (it is ShelfItem.AlbumItem) it.album else null }
                }
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(albums, key = { "alb_${shelf.id}_${it.id}" }) { album ->
                        AlbumCard(
                            album = album,
                            onClick = { onAlbumClick(album) }
                        )
                    }
                }
            }
            ShelfType.PLAYLIST_HORIZONTAL -> {
                val playlists = remember(shelf) {
                    shelf.items.mapNotNull { if (it is ShelfItem.PlaylistItem) it.playlist else null }
                }
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(playlists, key = { "pl_${shelf.id}_${it.id}" }) { playlist ->
                        PlaylistCard(
                            playlist = playlist,
                            onClick = { onPlaylistClick(playlist) }
                        )
                    }
                }
            }
            else -> {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(shelf.items, key = { item ->
                        when (item) {
                            is ShelfItem.SongItem -> "s_${shelf.id}_${item.song.id}"
                            is ShelfItem.AlbumItem -> "a_${shelf.id}_${item.album.id}"
                            is ShelfItem.PlaylistItem -> "p_${shelf.id}_${item.playlist.id}"
                            is ShelfItem.ArtistItem -> "ar_${shelf.id}_${item.artist.id}"
                        }
                    }) { item ->
                        when (item) {
                            is ShelfItem.SongItem -> {
                                AlbumCard(
                                    album = Album(id = item.song.id, title = item.song.title, artist = item.song.artist, artwork = item.song.artwork),
                                    onClick = { onPlaySong(item.song, emptyList()) }
                                )
                            }
                            is ShelfItem.AlbumItem -> AlbumCard(album = item.album, onClick = { onAlbumClick(item.album) })
                            is ShelfItem.PlaylistItem -> PlaylistCard(playlist = item.playlist, onClick = { onPlaylistClick(item.playlist) })
                            is ShelfItem.ArtistItem -> ArtistCard(artist = item.artist, onClick = { onArtistClick(item.artist) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuickPickRowItem(
    song: Song,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isPlaying) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else Color.Transparent,
        modifier = modifier
            .fillMaxWidth()
            .testTag("quick_pick_item_${song.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                AsyncImage(
                    model = song.artwork,
                    contentDescription = song.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                if (isPlaying) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.55f)),
                        contentAlignment = Alignment.Center
                    ) {
                        ThreeLineVisualizer(
                            isPlaying = true,
                            color = MaterialTheme.colorScheme.primary,
                            barWidth = 3.dp,
                            maxHeight = 20.dp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    fontFamily = AppFontFamily,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = AppFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.5.sp,
                        color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = song.artist,
                    fontFamily = AppFontFamily,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Normal,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = AppFontFamily,
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = onMoreClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More options",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
