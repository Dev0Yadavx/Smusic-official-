package com.example.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Equalizer
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Speaker
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.player.EqualizerManager
import com.example.player.EqualizerPreset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    equalizerManager: EqualizerManager = EqualizerManager.getInstance(LocalContext.current)
) {
    BackHandler { onBack() }

    val isEnabled by equalizerManager.isEnabled.collectAsState()
    val selectedPreset by equalizerManager.selectedPreset.collectAsState()
    val bandLevels by equalizerManager.bandLevels.collectAsState()
    val bandFrequencies by equalizerManager.bandFrequencies.collectAsState()
    val minBandLevel by equalizerManager.minBandLevel.collectAsState()
    val maxBandLevel by equalizerManager.maxBandLevel.collectAsState()
    val bassBoostStrength by equalizerManager.bassBoostStrength.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Equalizer",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("equalizer_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { equalizerManager.setEnabled(it) },
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .testTag("equalizer_master_switch")
                    )
                }
            )
        },
        modifier = modifier.testTag("equalizer_screen")
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 8.dp),
            contentPadding = PaddingValues(bottom = 165.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Presets Item Options Card (Only clean item options, no extra text)
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        val presets = EqualizerPreset.entries
                        presets.forEachIndexed { index, preset ->
                            val isSelected = selectedPreset == preset
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        equalizerManager.selectPreset(preset)
                                    }
                                    .padding(horizontal = 18.dp, vertical = 14.dp)
                                    .testTag("equalizer_preset_${preset.id}"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (preset == EqualizerPreset.BASS_BOOST) {
                                        Icons.Outlined.Speaker
                                    } else if (preset == EqualizerPreset.CUSTOM) {
                                        Icons.Outlined.Tune
                                    } else {
                                        Icons.Outlined.GraphicEq
                                    },
                                    contentDescription = preset.displayName,
                                    tint = if (isSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = preset.displayName,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { equalizerManager.selectPreset(preset) },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }
                            if (index < presets.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 18.dp),
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.14f)
                                )
                            }
                        }
                    }
                }
            }

            // Frequency Bands & Bass Boost Controls Card
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(if (isEnabled) 1f else 0.5f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Bass Boost Level Slider Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Speaker,
                                contentDescription = "Bass Boost",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Bass Boost",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                modifier = Modifier.width(84.dp)
                            )
                            Slider(
                                value = bassBoostStrength.toFloat(),
                                onValueChange = {
                                    if (isEnabled) {
                                        equalizerManager.setBassBoostStrength(it.toInt())
                                    }
                                },
                                valueRange = 0f..1000f,
                                enabled = isEnabled,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("equalizer_bass_slider")
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${(bassBoostStrength / 10)}%",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                ),
                                textAlign = TextAlign.End,
                                modifier = Modifier.width(42.dp)
                            )
                        }

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.14f)
                        )

                        // Individual Band Sliders (60 Hz .. 14 kHz)
                        bandFrequencies.forEachIndexed { index, freqLabel ->
                            val currentMb = bandLevels.getOrElse(index) { 0 }
                            val dbValue = currentMb / 100f
                            val dbText = if (dbValue >= 0) "+${String.format("%.1f", dbValue)} dB"
                            else "${String.format("%.1f", dbValue)} dB"

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Equalizer,
                                    contentDescription = freqLabel,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = freqLabel,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    modifier = Modifier.width(84.dp)
                                )
                                Slider(
                                    value = currentMb.toFloat(),
                                    onValueChange = { newMb ->
                                        if (isEnabled) {
                                            equalizerManager.setBandLevel(index, newMb.toInt())
                                        }
                                    },
                                    valueRange = minBandLevel.toFloat()..maxBandLevel.toFloat(),
                                    enabled = isEnabled,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("equalizer_band_slider_$index")
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = dbText,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.width(56.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
