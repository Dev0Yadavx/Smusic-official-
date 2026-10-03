package com.example.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.PlayableTrack
import com.example.ui.theme.AppFontFamily
import com.example.ui.theme.AppIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackOptionsBottomSheet(
    track: PlayableTrack,
    isLiked: Boolean,
    onDismiss: () -> Unit,
    onPlayNow: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onToggleLike: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onViewAlbum: (() -> Unit)? = null
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val downloadManager = remember { com.example.download.SongDownloadManager.getInstance(context) }
    val activeDownloads by downloadManager.activeDownloads.collectAsState()
    val isDownloaded by downloadManager.isDownloaded(track.id).collectAsState(initial = track.isDownloaded)

    val activeTask = activeDownloads[track.id]
    val isDownloading = activeTask?.status == com.example.download.DownloadStatus.DOWNLOADING || activeTask?.status == com.example.download.DownloadStatus.QUEUED
    val downloadProgress = activeTask?.progress ?: 0

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 36.dp)
        ) {
            // Track header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    AsyncImage(
                        model = track.artwork,
                        contentDescription = track.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        fontFamily = AppFontFamily,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = AppFontFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = track.artist,
                        fontFamily = AppFontFamily,
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Normal,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = AppFontFamily,
                            fontStyle = FontStyle.Italic,
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            )

            // Segmented 3-Card Row: Like (Start Rounding), Download (Middle), Add to Playlist (Last Rounding)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Like Card (Start Rounding)
                val likeShape = RoundedCornerShape(
                    topStart = 24.dp,
                    bottomStart = 24.dp,
                    topEnd = 6.dp,
                    bottomEnd = 6.dp
                )
                Surface(
                    onClick = {
                        onToggleLike()
                        onDismiss()
                    },
                    shape = likeShape,
                    color = if (isLiked) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    },
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = if (isLiked) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                        } else {
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.30f)
                        }
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(72.dp)
                        .testTag("sheet_card_like")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = if (isLiked) "Liked" else "Like",
                            tint = if (isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isLiked) "Liked" else "Like",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = if (isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // 2. Download Card (Middle)
                val downloadShape = RoundedCornerShape(6.dp)
                val dlActive = isDownloaded || isDownloading
                val dlLabel = when {
                    isDownloading -> "$downloadProgress%"
                    isDownloaded -> "Downloaded"
                    else -> "Download"
                }
                Surface(
                    onClick = {
                        if (isDownloading) {
                            downloadManager.cancelDownload(track.id)
                            com.example.ui.common.AppToast.show(context, "Download cancelled", isDownload = true)
                        } else if (isDownloaded) {
                            downloadManager.deleteDownloadedSong(track.id)
                            com.example.ui.common.AppToast.show(context, "Removed from downloads", isDownload = true)
                        } else {
                            downloadManager.startDownload(track, "320")
                            com.example.ui.common.AppToast.show(context, "Download started for ${track.title}", isDownload = true)
                        }
                        onDismiss()
                    },
                    shape = downloadShape,
                    color = if (dlActive) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    },
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = if (dlActive) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                        } else {
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.30f)
                        }
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(72.dp)
                        .testTag("sheet_card_download")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = when {
                                isDownloading -> Icons.Outlined.Downloading
                                isDownloaded -> AppIcons.DownloadForOffline
                                else -> AppIcons.Download
                            },
                            contentDescription = dlLabel,
                            tint = if (dlActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = dlLabel,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = if (dlActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // 3. Add to Playlist Card (Last Rounding)
                val playlistShape = RoundedCornerShape(
                    topStart = 6.dp,
                    bottomStart = 6.dp,
                    topEnd = 24.dp,
                    bottomEnd = 24.dp
                )
                Surface(
                    onClick = {
                        onAddToPlaylist()
                        onDismiss()
                    },
                    shape = playlistShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.30f)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(72.dp)
                        .testTag("sheet_card_add_to_playlist")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = AppIcons.AddCircle,
                            contentDescription = "Add to Playlist",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Add to Playlist",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Closely-grouped M3 Asymmetrical Inner-Corner Vertical Option List
            val hasAlbumAction = track.albumId.isNotBlank() && onViewAlbum != null
            val totalOptions = if (hasAlbumAction) 4 else 3

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                OptionItem(
                    icon = AppIcons.PlayArrow,
                    title = "Play Now",
                    shape = m3VerticalGroupItemShape(index = 0, count = totalOptions),
                    onClick = {
                        onPlayNow()
                        onDismiss()
                    }
                )

                OptionItem(
                    icon = Icons.Outlined.QueuePlayNext,
                    title = "Play Next",
                    shape = m3VerticalGroupItemShape(index = 1, count = totalOptions),
                    onClick = {
                        onPlayNext()
                        onDismiss()
                    }
                )

                OptionItem(
                    icon = Icons.Outlined.PlaylistAdd,
                    title = "Add to Queue",
                    shape = m3VerticalGroupItemShape(index = 2, count = totalOptions),
                    onClick = {
                        onAddToQueue()
                        onDismiss()
                    }
                )

                if (hasAlbumAction) {
                    OptionItem(
                        icon = Icons.Outlined.Album,
                        title = "View Album (${track.album})",
                        shape = m3VerticalGroupItemShape(index = 3, count = totalOptions),
                        onClick = {
                            onViewAlbum?.invoke()
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}

private fun m3VerticalGroupItemShape(index: Int, count: Int): RoundedCornerShape {
    return when {
        count <= 1 -> RoundedCornerShape(24.dp)
        index == 0 -> RoundedCornerShape(
            topStart = 24.dp,
            topEnd = 24.dp,
            bottomStart = 6.dp,
            bottomEnd = 6.dp
        )
        index == count - 1 -> RoundedCornerShape(
            topStart = 6.dp,
            topEnd = 6.dp,
            bottomStart = 24.dp,
            bottomEnd = 24.dp
        )
        else -> RoundedCornerShape(6.dp)
    }
}

@Composable
private fun OptionItem(
    icon: ImageVector,
    title: String,
    shape: RoundedCornerShape,
    iconTint: androidx.compose.ui.graphics.Color? = null,
    onClick: () -> Unit
) {
    val resolvedTint = iconTint ?: MaterialTheme.colorScheme.onSurface
    Surface(
        onClick = onClick,
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.30f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = resolvedTint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = resolvedTint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
