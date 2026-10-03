package com.example.ui.common

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.ui.theme.MiniPlayerScallopedShape
import com.example.ui.theme.ThemeManager
import java.io.File

val PRESET_AVATAR_EMOJIS = listOf(
    "🎧", "🎵", "🔥", "✨", "😎", "👑",
    "🚀", "💜", "🎸", "🎹", "🦊", "⚡"
)

@Composable
fun UserAvatarBadge(
    emoji: String,
    customImageUri: String?,
    size: Dp = 36.dp,
    fontSize: TextUnit = 17.sp,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val avatarShape = MiniPlayerScallopedShape
    val baseModifier = modifier
        .size(size)
        .clip(avatarShape)
        .background(
            Brush.linearGradient(
                colors = listOf(
                    colorScheme.primaryContainer,
                    colorScheme.tertiaryContainer
                )
            )
        )
        .border(
            BorderStroke(
                width = 1.4.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        colorScheme.primary.copy(alpha = 0.75f),
                        colorScheme.tertiary.copy(alpha = 0.55f)
                    )
                )
            ),
            shape = avatarShape
        )
        .then(
            if (onClick != null) {
                Modifier.clickable(onClick = onClick)
            } else {
                Modifier
            }
        )

    Box(
        modifier = baseModifier,
        contentAlignment = Alignment.Center
    ) {
        if (!customImageUri.isNullOrBlank()) {
            val imageModel: Any = remember(customImageUri) {
                if (customImageUri.startsWith("/")) File(customImageUri) else customImageUri
            }
            AsyncImage(
                model = imageModel,
                contentDescription = "Avatar",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(avatarShape)
            )
        } else {
            Text(
                text = emoji,
                fontSize = fontSize,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun UserProfileM3CardDialog(
    themeManager: ThemeManager,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val currentNickname by themeManager.userNickname.collectAsState()
    val currentEmoji by themeManager.userAvatarEmoji.collectAsState()
    val currentImageUri by themeManager.userAvatarImageUri.collectAsState()

    var nicknameInput by remember(currentNickname) { mutableStateOf(currentNickname) }
    var selectedEmoji by remember(currentEmoji) { mutableStateOf(currentEmoji) }
    var useCustomPhoto by remember(currentImageUri) { mutableStateOf(!currentImageUri.isNullOrBlank()) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            themeManager.setUserAvatarCustomImage(context, uri)
            useCustomPhoto = true
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            border = BorderStroke(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.22f),
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                    )
                )
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .widthIn(max = 400.dp)
                .testTag("profile_avatar_m3_card")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Large Interactive Avatar Preview with Camera Action Badge
                Box(
                    contentAlignment = Alignment.BottomEnd,
                    modifier = Modifier.size(86.dp)
                ) {
                    UserAvatarBadge(
                        emoji = selectedEmoji,
                        customImageUri = if (useCustomPhoto) currentImageUri else null,
                        size = 84.dp,
                        fontSize = 38.sp,
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    )

                    Surface(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shadowElevation = 4.dp,
                        border = BorderStroke(2.dp, MaterialTheme.colorScheme.surfaceContainerHigh),
                        modifier = Modifier
                            .size(30.dp)
                            .testTag("avatar_pick_photo_badge")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.PhotoCamera,
                                contentDescription = "Photo",
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }

                // Segmented M3 Pill Selector: Emoji vs Custom Image
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val emojiTabBg by animateColorAsState(
                            targetValue = if (!useCustomPhoto) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                            label = "emoji_tab_bg"
                        )
                        val photoTabBg by animateColorAsState(
                            targetValue = if (useCustomPhoto) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                            label = "photo_tab_bg"
                        )

                        Surface(
                            onClick = { useCustomPhoto = false },
                            shape = CircleShape,
                            color = emojiTabBg,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .testTag("avatar_tab_emoji")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.EmojiEmotions,
                                    contentDescription = null,
                                    tint = if (!useCustomPhoto) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(17.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Emoji",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (!useCustomPhoto) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }

                        Surface(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            shape = CircleShape,
                            color = photoTabBg,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .testTag("avatar_tab_custom_image")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.AddPhotoAlternate,
                                    contentDescription = null,
                                    tint = if (useCustomPhoto) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(17.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Custom",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (useCustomPhoto) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }
                }

                // Emoji Grid (2 rows x 6 columns)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(6),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(96.dp)
                ) {
                    items(PRESET_AVATAR_EMOJIS) { emoji ->
                        val isSelected = !useCustomPhoto && selectedEmoji == emoji
                        val scale by animateFloatAsState(
                            targetValue = if (isSelected) 1.08f else 1.0f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                            label = "emoji_item_scale"
                        )
                        Surface(
                            onClick = {
                                selectedEmoji = emoji
                                useCustomPhoto = false
                            },
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.75f)
                            },
                            border = if (isSelected) {
                                BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                            } else null,
                            modifier = Modifier
                                .size(42.dp)
                                .scale(scale)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = emoji,
                                    fontSize = 20.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                // Clean Nickname M3 Input Field (No extra clutter text)
                OutlinedTextField(
                    value = nicknameInput,
                    onValueChange = { if (it.length <= 24) nicknameInput = it },
                    placeholder = { Text("Nickname") },
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.55f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.35f),
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("nickname_input_field")
                )

                // Action Buttons Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilledTonalButton(
                        onClick = onDismiss,
                        shape = CircleShape,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                    ) {
                        Text(
                            text = "Cancel",
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Button(
                        onClick = {
                            if (!useCustomPhoto) {
                                themeManager.setUserAvatarEmoji(selectedEmoji)
                            }
                            themeManager.setUserNickname(nicknameInput)
                            onDismiss()
                        },
                        shape = CircleShape,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("save_profile_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Save",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
