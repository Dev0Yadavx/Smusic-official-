package com.example.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
    val isLiquidGlassEnabled by themeManager.isLiquidGlassEnabled.collectAsStateWithLifecycle()
    val currentProvider by themeManager.contentProvider.collectAsStateWithLifecycle()
    var showProfileDialog by remember { mutableStateOf(false) }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val currentPlayingTrack by playerManager.currentTrack.collectAsStateWithLifecycle()
    val firebaseUser by viewModel.firebaseManager.currentUser.collectAsStateWithLifecycle()
    val isSyncingFirebase by viewModel.firebaseManager.isSyncing.collectAsStateWithLifecycle()
    val pullRefreshState = rememberPullToRefreshState()

    var selectedTrackForOptions by remember { mutableStateOf<PlayableTrack?>(null) }
    var isTrackLiked by remember { mutableStateOf(false) }

    // Clean Time-Based Greeting (no extra text)
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
                        .padding(top = 160.dp)
                        .align(Alignment.TopCenter)
                ) {
                    LoadingIndicator()
                }
            } else {
                PullToRefreshDefaults.Indicator(
                    state = pullRefreshState,
                    isRefreshing = isRefreshing,
                    modifier = Modifier
                        .padding(top = 160.dp)
                        .align(Alignment.TopCenter)
                )
            }
        }
    ) {
        // Main Scrollable Body
        when (val state = uiState) {
            is HomeUiState.Loading -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 160.dp)
                ) {
                    repeat(3) {
                        ShelfSkeleton()
                    }
                }
            }
            is HomeUiState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp)
                        .padding(top = 160.dp, bottom = 120.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CloudOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Something went wrong",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
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
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 165.dp, bottom = 165.dp) // Leave space for fixed header with filter chips and floating controls
                ) {
                    items(state.shelves, key = { it.id }) { shelf ->
                        HomeShelfSection(
                            shelf = shelf,
                            currentPlayingId = currentPlayingTrack?.id,
                            isFirebaseSignedIn = firebaseUser != null,
                            isSyncingFirebase = isSyncingFirebase,
                            onSyncFirebase = {
                                viewModel.syncPlaylistsToFirebase(context)
                            },
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

        // Fixed Sticky Header with Mask Style and Frosted Greeting Card
        val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        0.0f to MaterialTheme.colorScheme.background.copy(alpha = if (isDark) 0.85f else 0.90f),
                        0.70f to MaterialTheme.colorScheme.background.copy(alpha = if (isDark) 0.50f else 0.55f),
                        1.0f to Color.Transparent
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 10.dp)
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
                        // 1. SMusic Brand Upper (Clear & Vibrant) with Content Provider Pill
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
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

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                                ),
                                modifier = Modifier
                                    .clickable { onNavigateToSettings() }
                                    .testTag("home_provider_badge")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.CloudQueue,
                                        contentDescription = "Content Provider",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = currentProvider.displayName,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }

                        // 2. Good Morning Niche with Mask Styled Frosted Glass Pill (Shows clear Nickname)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(24.dp))
                                .background(
                                    if (isDark) Color(0xFF1B1B24).copy(alpha = 0.72f)
                                    else Color.White.copy(alpha = 0.88f)
                                )
                                .border(
                                    1.dp,
                                    if (isDark) Color.White.copy(alpha = 0.16f)
                                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.50f),
                                    RoundedCornerShape(24.dp)
                                )
                                .clickable { showProfileDialog = true }
                                .padding(horizontal = 9.dp, vertical = 5.dp)
                                .testTag("home_greeting_profile_row")
                        ) {
                            UserAvatarBadge(
                                emoji = userAvatarEmoji,
                                customImageUri = userAvatarImageUri,
                                size = 36.dp,
                                fontSize = 17.sp,
                                onClick = { showProfileDialog = true }
                            )
                            Column(
                                verticalArrangement = Arrangement.spacedBy(0.dp)
                            ) {
                                Text(
                                    text = greeting,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        letterSpacing = 0.3.sp
                                    )
                                )
                                Text(
                                    text = userNickname,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.5.sp
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = onNavigateToSearch,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isDark) Color(0xFF1B1B24).copy(alpha = 0.72f)
                                    else Color.White.copy(alpha = 0.88f)
                                )
                                .border(
                                    1.dp,
                                    if (isDark) Color.White.copy(alpha = 0.16f)
                                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.50f),
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
                                    if (isDark) Color(0xFF1B1B24).copy(alpha = 0.72f)
                                    else Color.White.copy(alpha = 0.88f)
                                )
                                .border(
                                    1.dp,
                                    if (isDark) Color.White.copy(alpha = 0.16f)
                                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.50f),
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

                // YouTube Music Style Mood & Category Filter Chips
                val currentCategory by viewModel.selectedLanguage.collectAsStateWithLifecycle()
                val categories = listOf("All", "Energize", "Workout", "Relax", "Commute", "Focus", "Hindi", "Punjabi", "English")

                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories) { category ->
                        val isSelected = currentCategory.equals(category, ignoreCase = true)
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isDark) 0.60f else 0.85f),
                            contentColor = if (isSelected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurface,
                            border = if (!isSelected) androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            ) else null,
                            modifier = Modifier
                                .clickable { viewModel.selectLanguage(category) }
                                .testTag("category_chip_$category")
                        ) {
                            Text(
                                text = category,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                    fontSize = 12.5.sp
                                ),
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
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
    isFirebaseSignedIn: Boolean = false,
    isSyncingFirebase: Boolean = false,
    onSyncFirebase: (() -> Unit)? = null,
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
        // Clean Bold Category Header with optional Firebase Cloud Sync pill for bottom playlists
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = shelf.title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 22.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (shelf.id == "smusic_playlists" && shelf.subtitle.isNotBlank()) {
                    Text(
                        text = shelf.subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (shelf.id == "smusic_playlists" && onSyncFirebase != null) {
                FilledTonalButton(
                    onClick = onSyncFirebase,
                    enabled = !isSyncingFirebase,
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier
                        .height(36.dp)
                        .testTag("home_firebase_sync_button")
                ) {
                    if (isSyncingFirebase) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(15.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Syncing...", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(
                            imageVector = if (isFirebaseSignedIn) Icons.Default.CloudDone else Icons.Default.CloudUpload,
                            contentDescription = "Sync Firebase Playlists",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isFirebaseSignedIn) "Cloud Synced" else "Sync Firebase",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (shelf.type) {
            ShelfType.SONG_HORIZONTAL -> {
                val songs = shelf.items.mapNotNull { if (it is ShelfItem.SongItem) it.song else null }
                val chunks = songs.chunked(4)
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(chunks) { colSongs ->
                        Column(
                            modifier = Modifier.width(300.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            colSongs.forEach { song ->
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
            ShelfType.ARTIST_HORIZONTAL -> {
                val artists = shelf.items.mapNotNull { if (it is ShelfItem.ArtistItem) it.artist else null }
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(artists, key = { it.id }) { artist ->
                        ArtistCard(
                            artist = artist,
                            onClick = { onArtistClick(artist) }
                        )
                    }
                }
            }
            ShelfType.ALBUM_HORIZONTAL -> {
                val albums = shelf.items.mapNotNull { if (it is ShelfItem.AlbumItem) it.album else null }
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(albums, key = { it.id }) { album ->
                        AlbumCard(
                            album = album,
                            onClick = { onAlbumClick(album) }
                        )
                    }
                }
            }
            ShelfType.PLAYLIST_HORIZONTAL -> {
                val playlists = shelf.items.mapNotNull { if (it is ShelfItem.PlaylistItem) it.playlist else null }
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(playlists, key = { it.id }) { playlist ->
                        PlaylistCard(
                            playlist = playlist,
                            onClick = { onPlaylistClick(playlist) }
                        )
                    }
                }
            }
            else -> {
                // Fallback row
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(shelf.items) { item ->
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

/**
 * Material 3 Loading Indicator for pull-to-refresh
 * Matches: Column(horizontalAlignment = Alignment.CenterHorizontally) { LoadingIndicator() }
 */
@Composable
fun LoadingIndicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 6.dp,
        tonalElevation = 6.dp,
        modifier = modifier.size(42.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = color,
                strokeWidth = 2.8.dp
            )
        }
    }
}

