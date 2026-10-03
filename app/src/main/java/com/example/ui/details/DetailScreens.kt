package com.example.ui.details

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import com.example.download.SongDownloadManager
import com.example.download.DownloadStatus
import com.example.data.model.Album
import com.example.data.model.Artist
import com.example.data.model.PlayableTrack
import com.example.data.model.Playlist
import com.example.data.remote.NetworkResult
import com.example.player.PlayerManager
import com.example.repository.MusicRepository
import com.example.ui.common.AlbumCard
import com.example.ui.common.AddToPlaylistBottomSheet
import com.example.ui.common.FourSongGridCover
import com.example.ui.common.SongRowItem
import com.example.ui.common.TrackOptionsBottomSheet

@Composable
fun AlbumDetailScreen(
    albumId: String,
    initialTitle: String = "",
    initialArtist: String = "",
    initialArtwork: String = "",
    repository: MusicRepository,
    playerManager: PlayerManager,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val scope = rememberCoroutineScope()
    var albumResult by remember { mutableStateOf<NetworkResult<Album>>(NetworkResult.Loading) }
    val currentTrack by playerManager.currentTrack.collectAsState()
    val isPlaying by playerManager.isPlaying.collectAsState()
    var selectedTrackForOptions by remember { mutableStateOf<PlayableTrack?>(null) }
    var isLiked by remember { mutableStateOf(false) }

    LaunchedEffect(albumId, initialTitle) {
        albumResult = repository.getAlbumDetails(albumId, initialTitle, initialArtist, initialArtwork)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        when (val result = albumResult) {
            is NetworkResult.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            is NetworkResult.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CloudOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Failed to load album",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            scope.launch {
                                albumResult = NetworkResult.Loading
                                albumResult = repository.getAlbumDetails(albumId)
                            }
                        },
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text("Retry")
                    }
                }
            }
            is NetworkResult.Success -> {
                val album = result.data
                val playableTracks = album.songs.map { it.toPlayableTrack() }
                val isCurrentAlbumPlaying = playableTracks.any { it.id == currentTrack?.id } && isPlaying

                val context = LocalContext.current
                val downloadManager = remember { SongDownloadManager.getInstance(context) }
                val activeDownloads by downloadManager.activeDownloads.collectAsState()
                val allDownloadedTracks by downloadManager.allDownloadedTracks.collectAsState(initial = emptyList())
                val downloadedIds = remember(allDownloadedTracks) { allDownloadedTracks.map { it.id }.toSet() }
                val downloadedCount = remember(playableTracks, downloadedIds) {
                    playableTracks.count { downloadedIds.contains(it.id) }
                }
                val totalSongsCount = playableTracks.size
                val isAllDownloaded = remember(playableTracks, downloadedIds) {
                    playableTracks.isNotEmpty() && playableTracks.all { downloadedIds.contains(it.id) }
                }
                val isAnyDownloading = remember(playableTracks, activeDownloads) {
                    playableTracks.any { track ->
                        val state = activeDownloads[track.id]
                        state?.status == DownloadStatus.DOWNLOADING || state?.status == DownloadStatus.QUEUED
                    }
                }
                val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 165.dp)
                ) {
                    // Double Exposure / Ambient Mask Header (Zero Gap to Edge)
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(440.dp)
                        ) {
                            // Ambient Backdrop
                            AsyncImage(
                                model = album.artwork,
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .blur(50.dp),
                                contentScale = ContentScale.Crop,
                                alpha = 0.45f
                            )

                            // Double Exposure Theme-Aware Gradient Mask
                            val scrimColors = if (isDark) {
                                listOf(
                                    Color.Black.copy(alpha = 0.55f),
                                    Color.Black.copy(alpha = 0.25f),
                                    MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
                                    MaterialTheme.colorScheme.background
                                )
                            } else {
                                listOf(
                                    Color.White.copy(alpha = 0.35f),
                                    Color.White.copy(alpha = 0.65f),
                                    MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
                                    MaterialTheme.colorScheme.background
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Brush.verticalGradient(colors = scrimColors))
                            )

                            // Foreground Artwork & Metadata
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .statusBarsPadding()
                                    .padding(horizontal = 20.dp, vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Spacer(modifier = Modifier.height(28.dp))

                                // Large Rounded Glow Album Artwork (240.dp)
                                Surface(
                                    shape = RoundedCornerShape(24.dp),
                                    shadowElevation = 18.dp,
                                    modifier = Modifier.size(240.dp)
                                ) {
                                    AsyncImage(
                                        model = album.artwork,
                                        contentDescription = album.title,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Text(
                                    text = album.title,
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 24.sp
                                    ),
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = "${album.artist} • ${album.year.ifBlank { "Album" }} • ${album.songs.size} Songs",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 24.dp)
                                )
                            }
                        }
                    }

                    // Action Bar Row (Play, Shuffle, Like, Share)
                    item {
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 1. Play Button (Filled Pill)
                            item {
                                Button(
                                    onClick = {
                                        if (playableTracks.isNotEmpty()) {
                                            if (isCurrentAlbumPlaying) {
                                                playerManager.playPause()
                                            } else {
                                                playerManager.playTrack(playableTracks.first(), playableTracks)
                                            }
                                        }
                                    },
                                    shape = RoundedCornerShape(24.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                                    modifier = Modifier.testTag("album_play_button")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isCurrentAlbumPlaying) Icons.Rounded.Pause else com.example.ui.theme.AppIcons.PlayArrow,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = if (isCurrentAlbumPlaying) "Pause" else "Play",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 13.5.sp
                                        )
                                    }
                                }
                            }

                            // 2. Shuffle Button (Tonal Pill)
                            item {
                                Surface(
                                    shape = RoundedCornerShape(24.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.70f),
                                    modifier = Modifier
                                        .clickable {
                                            if (playableTracks.isNotEmpty()) {
                                                val shuffled = playableTracks.shuffled()
                                                playerManager.playTrack(shuffled.first(), shuffled)
                                            }
                                        }
                                        .testTag("album_shuffle_pill")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Shuffle,
                                            contentDescription = "Shuffle",
                                            tint = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.size(17.dp)
                                        )
                                        Text(
                                            text = "Shuffle",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }

                            // 3. Save / Library Pill
                            item {
                                Surface(
                                    shape = RoundedCornerShape(24.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.70f),
                                    modifier = Modifier
                                        .clickable { isLiked = !isLiked }
                                        .testTag("album_save_pill")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isLiked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                                            contentDescription = "Save to Library",
                                            tint = if (isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.size(17.dp)
                                        )
                                        Text(
                                            text = if (isLiked) "Saved" else "Save",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }

                            // 4. Download Full Album Pill
                            item {
                                Surface(
                                    shape = RoundedCornerShape(24.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.70f),
                                    modifier = Modifier
                                        .clickable {
                                            if (playableTracks.isNotEmpty()) {
                                                if (isAllDownloaded) {
                                                    Toast.makeText(context, "All ${playableTracks.size} songs are downloaded", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    Toast.makeText(context, "Downloading all ${playableTracks.size} songs...", Toast.LENGTH_SHORT).show()
                                                    playableTracks.forEach { track ->
                                                        downloadManager.startDownload(track, "320")
                                                    }
                                                }
                                            }
                                        }
                                        .testTag("album_download_pill")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
                                    ) {
                                        if (isAnyDownloading) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(15.dp),
                                                strokeWidth = 2.dp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = "$downloadedCount/$totalSongsCount",
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp
                                                ),
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        } else {
                                            Icon(
                                                imageVector = if (isAllDownloaded) com.example.ui.theme.AppIcons.DownloadForOffline else com.example.ui.theme.AppIcons.Download,
                                                contentDescription = "Download All",
                                                tint = if (isAllDownloaded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.size(17.dp)
                                            )
                                            Text(
                                                text = if (isAllDownloaded) "Downloaded" else "Download",
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp
                                                ),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Album Songs List Header
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Songs (${album.songs.size})",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (isAnyDownloading) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
                                ) {
                                    Text(
                                        text = "Downloading $downloadedCount/$totalSongsCount",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }

                    itemsIndexed(album.songs, key = { _, s -> s.id }) { index, song ->
                        SongRowItem(
                            song = song,
                            isPlaying = currentTrack?.id == song.id,
                            onClick = { playerManager.playTrack(song.toPlayableTrack(), playableTracks) },
                            onMoreClick = { selectedTrackForOptions = song.toPlayableTrack() }
                        )
                    }
                }
            }
        }

        // Clean Floating Back Button (Zero Background Gap)
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .statusBarsPadding()
                .padding(start = 8.dp, top = 4.dp)
                .size(44.dp)
                .align(Alignment.TopStart)
                .testTag("album_back_button")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(26.dp)
            )
        }
    }

    selectedTrackForOptions?.let { track ->
        TrackOptionsBottomSheet(
            track = track,
            isLiked = false,
            onDismiss = { selectedTrackForOptions = null },
            onPlayNow = { playerManager.playTrack(track) },
            onPlayNext = { playerManager.playNext(track) },
            onAddToQueue = { playerManager.addToQueue(track) },
            onToggleLike = {
                scope.launch {
                    repository.toggleLike(track)
                }
            },
            onAddToPlaylist = {}
        )
    }
}

@Composable
fun PlaylistDetailScreen(
    playlistId: String,
    repository: MusicRepository,
    playerManager: PlayerManager,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val scope = rememberCoroutineScope()
    var playlistResult by remember { mutableStateOf<NetworkResult<Playlist>>(NetworkResult.Loading) }
    val currentTrack by playerManager.currentTrack.collectAsState()
    val isPlaying by playerManager.isPlaying.collectAsState()
    var selectedTrackForOptions by remember { mutableStateOf<PlayableTrack?>(null) }
    var isLiked by remember { mutableStateOf(false) }

    LaunchedEffect(playlistId) {
        playlistResult = repository.getPlaylistDetails(playlistId)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        when (val result = playlistResult) {
            is NetworkResult.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            is NetworkResult.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CloudOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Failed to load playlist",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
            is NetworkResult.Success -> {
                val playlist = result.data
                val playableTracks = playlist.songs.map { it.toPlayableTrack() }
                val isCurrentPlaylistPlaying = playableTracks.any { it.id == currentTrack?.id } && isPlaying

                val context = LocalContext.current
                val downloadManager = remember { SongDownloadManager.getInstance(context) }
                val activeDownloads by downloadManager.activeDownloads.collectAsState()
                val allDownloadedTracks by downloadManager.allDownloadedTracks.collectAsState(initial = emptyList())
                val downloadedIds = remember(allDownloadedTracks) { allDownloadedTracks.map { it.id }.toSet() }
                val downloadedCount = remember(playableTracks, downloadedIds) {
                    playableTracks.count { downloadedIds.contains(it.id) }
                }
                val totalSongsCount = playableTracks.size
                val isAllDownloaded = remember(playableTracks, downloadedIds) {
                    playableTracks.isNotEmpty() && playableTracks.all { downloadedIds.contains(it.id) }
                }
                val isAnyDownloading = remember(playableTracks, activeDownloads) {
                    playableTracks.any { track ->
                        val state = activeDownloads[track.id]
                        state?.status == DownloadStatus.DOWNLOADING || state?.status == DownloadStatus.QUEUED
                    }
                }
                val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

                val previewArtworks = remember(playlist.songs, playlist.artwork, playlist.previewArtworks) {
                    val fromSongs = playlist.songs.mapNotNull { it.artwork.takeIf { art -> art.isNotBlank() } }.distinct().take(4)
                    if (fromSongs.isNotEmpty()) fromSongs
                    else if (playlist.previewArtworks.isNotEmpty()) playlist.previewArtworks
                    else if (playlist.artwork.isNotBlank()) listOf(playlist.artwork)
                    else emptyList()
                }
                val blurArtwork = remember(previewArtworks, playlist.artwork) {
                    previewArtworks.firstOrNull() ?: playlist.artwork
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 165.dp)
                ) {
                    // Double Exposure Header
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(440.dp)
                        ) {
                            if (blurArtwork.isNotBlank()) {
                                AsyncImage(
                                    model = blurArtwork,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .blur(50.dp),
                                    contentScale = ContentScale.Crop,
                                    alpha = 0.45f
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(
                                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                                    MaterialTheme.colorScheme.background
                                                )
                                            )
                                        )
                                    )
                            }

                            // Theme-Aware Gradient Mask for 100% Text Clarity in Dark & Light
                            val scrimColors = if (isDark) {
                                listOf(
                                    Color.Black.copy(alpha = 0.55f),
                                    Color.Black.copy(alpha = 0.25f),
                                    MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
                                    MaterialTheme.colorScheme.background
                                )
                            } else {
                                listOf(
                                    Color.White.copy(alpha = 0.35f),
                                    Color.White.copy(alpha = 0.65f),
                                    MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
                                    MaterialTheme.colorScheme.background
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Brush.verticalGradient(colors = scrimColors))
                            )

                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .statusBarsPadding()
                                    .padding(horizontal = 20.dp, vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Spacer(modifier = Modifier.height(28.dp))

                                // Large Playlist Cover (240.dp)
                                Surface(
                                    shape = RoundedCornerShape(24.dp),
                                    shadowElevation = 18.dp,
                                    modifier = Modifier.size(240.dp)
                                ) {
                                    FourSongGridCover(
                                        artworks = previewArtworks,
                                        modifier = Modifier.fillMaxSize(),
                                        cornerRadius = 24.dp,
                                        fallbackTitle = playlist.title
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Playlist Name (Perfect Contrast in Dark & Light Themes)
                                Text(
                                    text = playlist.title,
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 24.sp
                                    ),
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                val infoText = if (playlist.description.isNotBlank()) {
                                    "${playlist.songs.size} Songs • ${playlist.description}"
                                } else {
                                    "${playlist.songs.size} Songs • Just Updated"
                                }
                                Text(
                                    text = infoText,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 24.dp)
                                )
                            }
                        }
                    }

                    // Large Square Floating Action Row
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { isLiked = !isLiked },
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                ) {
                                    Icon(
                                        imageVector = if (isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                        contentDescription = "Favorite",
                                        tint = if (isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        if (playableTracks.isNotEmpty()) {
                                            val shuffled = playableTracks.shuffled()
                                            playerManager.playTrack(shuffled.first(), shuffled)
                                        }
                                    },
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Shuffle,
                                        contentDescription = "Shuffle",
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                // Save / Import Playlist to Local Library
                                var isSavedToLibrary by remember { mutableStateOf(false) }
                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            if (!playlistId.startsWith("local_")) {
                                                repository.importJioSaavnPlaylistToLocal(playlist)
                                            }
                                            isSavedToLibrary = true
                                            Toast.makeText(context, "Playlist saved to Library", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        .testTag("playlist_save_button")
                                ) {
                                    Icon(
                                        imageVector = if (isSavedToLibrary) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                                        contentDescription = "Save to Library",
                                        tint = if (isSavedToLibrary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            // Right: Square Floating Download Full Playlist & Play FAB
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Download Full Playlist Button (Large Square Floating Style)
                                FloatingActionButton(
                                    onClick = {
                                        if (playableTracks.isNotEmpty()) {
                                            if (isAllDownloaded) {
                                                Toast.makeText(context, "All ${playableTracks.size} songs are already downloaded", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "Downloading all ${playableTracks.size} songs...", Toast.LENGTH_SHORT).show()
                                                playableTracks.forEach { track ->
                                                    downloadManager.startDownload(track, "320")
                                                }
                                            }
                                        }
                                    },
                                    shape = RoundedCornerShape(16.dp),
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (isAllDownloaded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
                                    modifier = Modifier
                                        .size(54.dp)
                                        .testTag("playlist_download_all_fab")
                                ) {
                                    if (isAnyDownloading) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.size(48.dp)
                                        ) {
                                            CircularProgressIndicator(
                                                strokeWidth = 2.5.dp,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(36.dp)
                                            )
                                            Text(
                                                text = "$downloadedCount",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 12.sp
                                                ),
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    } else {
                                        Icon(
                                            imageVector = if (isAllDownloaded) com.example.ui.theme.AppIcons.DownloadForOffline else com.example.ui.theme.AppIcons.Download,
                                            contentDescription = "Download All Songs",
                                            tint = if (isAllDownloaded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                }

                                // Play FAB (Large Square Floating Style)
                                FloatingActionButton(
                                    onClick = {
                                        if (playableTracks.isNotEmpty()) {
                                            if (isCurrentPlaylistPlaying) {
                                                playerManager.playPause()
                                            } else {
                                                playerManager.playTrack(playableTracks.first(), playableTracks)
                                            }
                                        }
                                    },
                                    shape = RoundedCornerShape(16.dp),
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
                                    modifier = Modifier
                                        .size(54.dp)
                                        .testTag("playlist_play_fab")
                                ) {
                                    Icon(
                                        imageVector = if (isCurrentPlaylistPlaying) Icons.Rounded.Pause else com.example.ui.theme.AppIcons.PlayArrow,
                                        contentDescription = "Play",
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Songs (${playlist.songs.size})",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (isAnyDownloading) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
                                ) {
                                    Text(
                                        text = "Downloading $downloadedCount/$totalSongsCount",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }

                    itemsIndexed(playlist.songs, key = { _, s -> s.id }) { index, song ->
                        SongRowItem(
                            song = song,
                            isPlaying = currentTrack?.id == song.id,
                            onClick = { playerManager.playTrack(song.toPlayableTrack(), playableTracks) },
                            onMoreClick = { selectedTrackForOptions = song.toPlayableTrack() }
                        )
                    }
                }
            }
        }

        // Clean Floating Back Button (Zero Background Gap)
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .statusBarsPadding()
                .padding(start = 8.dp, top = 4.dp)
                .size(44.dp)
                .align(Alignment.TopStart)
                .testTag("playlist_back_button")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(26.dp)
            )
        }
    }

    var trackForAddToPlaylist by remember { mutableStateOf<PlayableTrack?>(null) }

    selectedTrackForOptions?.let { track ->
        TrackOptionsBottomSheet(
            track = track,
            isLiked = false,
            onDismiss = { selectedTrackForOptions = null },
            onPlayNow = { playerManager.playTrack(track) },
            onPlayNext = { playerManager.playNext(track) },
            onAddToQueue = { playerManager.addToQueue(track) },
            onToggleLike = {
                scope.launch {
                    repository.toggleLike(track)
                }
            },
            onAddToPlaylist = {
                trackForAddToPlaylist = track
            }
        )
    }

    trackForAddToPlaylist?.let { track ->
        AddToPlaylistBottomSheet(
            track = track,
            repository = repository,
            onDismiss = { trackForAddToPlaylist = null }
        )
    }
}

@Composable
fun ArtistDetailScreen(
    artistId: String,
    repository: MusicRepository,
    playerManager: PlayerManager,
    onNavigateToAlbum: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val scope = rememberCoroutineScope()
    var artistResult by remember { mutableStateOf<NetworkResult<Artist>>(NetworkResult.Loading) }
    val currentTrack by playerManager.currentTrack.collectAsState()
    val isPlaying by playerManager.isPlaying.collectAsState()
    var selectedTrackForOptions by remember { mutableStateOf<PlayableTrack?>(null) }
    var isFollowing by remember { mutableStateOf(false) }

    LaunchedEffect(artistId) {
        artistResult = repository.getArtistDetails(artistId)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        when (val result = artistResult) {
            is NetworkResult.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            is NetworkResult.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CloudOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Failed to load artist",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            scope.launch {
                                artistResult = NetworkResult.Loading
                                artistResult = repository.getArtistDetails(artistId)
                            }
                        },
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text("Retry")
                    }
                }
            }
            is NetworkResult.Success -> {
                val artist = result.data
                val playableTracks = artist.topSongs.map { it.toPlayableTrack() }
                val isCurrentArtistPlaying = playableTracks.any { it.id == currentTrack?.id } && isPlaying

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 165.dp)
                ) {
                    // Double Exposure / Ambient Hero Mask Header (Full Bleed - Zero Upper Gap)
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(340.dp)
                        ) {
                            // Full-Bleed High-Res Artist Backdrop Photo or Custom SMusic Artwork
                            val rawDirectHero = com.example.data.remote.ArtistDpManager.getOriginalDp(artist.name, artist.image)
                            val isHeroPlaceholder = com.example.data.remote.ArtistDpManager.isJioPlaceholder(rawDirectHero)
                            val directHero = if (!isHeroPlaceholder) rawDirectHero else ""
                            val fallbackSongArt = artist.topSongs.firstOrNull { it.artwork.isNotBlank() && !com.example.data.remote.ArtistDpManager.isJioPlaceholder(it.artwork) }?.artwork ?: ""

                            if (directHero.isNotBlank()) {
                                AsyncImage(
                                    model = coil.request.ImageRequest.Builder(LocalContext.current)
                                        .data(directHero)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = artist.name,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else if (fallbackSongArt.isNotBlank()) {
                                AsyncImage(
                                    model = coil.request.ImageRequest.Builder(LocalContext.current)
                                        .data(fallbackSongArt)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = artist.name,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                // Custom SMusic Artist Artwork Banner
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.linearGradient(
                                                listOf(
                                                    Color(0xFF6366F1),
                                                    Color(0xFF8B5CF6),
                                                    Color(0xFFEC4899),
                                                    Color(0xFF1E1B4B)
                                                )
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(86.dp)
                                                .clip(CircleShape)
                                                .background(Color.White.copy(alpha = 0.2f))
                                                .border(2.dp, Color.White.copy(alpha = 0.6f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = artist.name.trim().take(1).uppercase(),
                                                style = MaterialTheme.typography.headlineLarge.copy(
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 40.sp,
                                                    color = Color.White
                                                )
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "SMUSIC ARTIST",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.ExtraBold,
                                                letterSpacing = 2.sp,
                                                color = Color.White.copy(alpha = 0.85f)
                                            )
                                        )
                                    }
                                }
                            }

                            // Double Exposure Atmospheric Gradient Mask
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Black.copy(alpha = 0.55f),
                                                Color.Transparent,
                                                Color.Black.copy(alpha = 0.35f),
                                                MaterialTheme.colorScheme.background.copy(alpha = 0.85f),
                                                MaterialTheme.colorScheme.background
                                            )
                                        )
                                    )
                            )

                            // Artist Title & Verified Badge at Bottom of Hero
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(horizontal = 20.dp, vertical = 12.dp)
                            ) {
                                // Verified Badge Pill
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.Black.copy(alpha = 0.4f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Verified,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "VERIFIED ARTIST",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 10.sp,
                                                letterSpacing = 1.sp,
                                                color = Color.White
                                            )
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Giant Artist Name
                                Text(
                                    text = artist.name,
                                    style = MaterialTheme.typography.headlineLarge.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 32.sp
                                    ),
                                    color = Color.White,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(2.dp))

                                Text(
                                    text = "${playableTracks.size} Top Songs",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = Color.White.copy(alpha = 0.85f),
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                        }
                    }

                    // Action Bar Row (Follow, Shuffle, Radio)
                    item {
                        val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Artist Follow Pill Button
                            Button(
                                onClick = { isFollowing = !isFollowing },
                                shape = RoundedCornerShape(24.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isFollowing) MaterialTheme.colorScheme.surfaceVariant else if (isDark) Color.White else Color.Black,
                                    contentColor = if (isFollowing) MaterialTheme.colorScheme.onSurfaceVariant else if (isDark) Color.Black else Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
                                modifier = Modifier.testTag("artist_follow_button")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (isFollowing) {
                                        Icon(
                                            imageVector = Icons.Rounded.Favorite,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(17.dp)
                                        )
                                        Text(
                                            text = "Following",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Rounded.FavoriteBorder,
                                            contentDescription = null,
                                            modifier = Modifier.size(17.dp)
                                        )
                                        Text(
                                            text = "Follow",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }

                            // Shuffle Pill Button
                            Surface(
                                shape = RoundedCornerShape(24.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.70f),
                                modifier = Modifier
                                    .clickable {
                                        if (playableTracks.isNotEmpty()) {
                                            val shuffled = playableTracks.shuffled()
                                            playerManager.playTrack(shuffled.first(), shuffled)
                                        }
                                    }
                                    .testTag("artist_shuffle_pill")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Shuffle,
                                        contentDescription = "Shuffle",
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Shuffle",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.5.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            // Radio / Play Pill Button
                            Surface(
                                shape = RoundedCornerShape(24.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                                modifier = Modifier
                                    .clickable {
                                        if (playableTracks.isNotEmpty()) {
                                            if (isCurrentArtistPlaying) {
                                                playerManager.playPause()
                                            } else {
                                                playerManager.playTrack(playableTracks.first(), playableTracks)
                                            }
                                        }
                                    }
                                    .testTag("artist_radio_pill")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isCurrentArtistPlaying) Icons.Rounded.Pause else Icons.Rounded.Radio,
                                        contentDescription = "Radio",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = if (isCurrentArtistPlaying) "Pause" else "Radio",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.5.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }

                    // Section 1: Top Songs
                    if (artist.topSongs.isNotEmpty()) {
                        item {
                            Text(
                                text = "Top Songs",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 20.sp
                                ),
                                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 4.dp),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        itemsIndexed(artist.topSongs, key = { _, s -> s.id }) { index, song ->
                            SongRowItem(
                                song = song,
                                isPlaying = currentTrack?.id == song.id,
                                onClick = { playerManager.playTrack(song.toPlayableTrack(), playableTracks) },
                                onMoreClick = { selectedTrackForOptions = song.toPlayableTrack() }
                            )
                        }
                    }

                    // Section 2: Albums & Singles Discography (if available)
                    if (artist.topAlbums.isNotEmpty()) {
                        item {
                            Column(modifier = Modifier.padding(top = 20.dp)) {
                                Text(
                                    text = "Albums & Singles",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 20.sp
                                    ),
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 20.dp),
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    items(artist.topAlbums, key = { it.id }) { album ->
                                        AlbumCard(
                                            album = album,
                                            onClick = { onNavigateToAlbum(album.id) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Section 3: About the Artist Bio Card
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 20.dp)
                        ) {
                            Text(
                                text = "About",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 20.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "${artist.name} on SMusic with ${artist.topSongs.size} popular tracks.",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontSize = 14.sp,
                                            lineHeight = 20.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "${playableTracks.size * 128}K Monthly Listeners",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Clean Floating Back Button (Zero Background Gap)
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .statusBarsPadding()
                .padding(start = 8.dp, top = 4.dp)
                .size(44.dp)
                .align(Alignment.TopStart)
                .testTag("artist_back_button")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(26.dp)
            )
        }
    }

    selectedTrackForOptions?.let { track ->
        TrackOptionsBottomSheet(
            track = track,
            isLiked = false,
            onDismiss = { selectedTrackForOptions = null },
            onPlayNow = { playerManager.playTrack(track) },
            onPlayNext = { playerManager.playNext(track) },
            onAddToQueue = { playerManager.addToQueue(track) },
            onToggleLike = {
                scope.launch {
                    repository.toggleLike(track)
                }
            },
            onAddToPlaylist = {}
        )
    }
}
