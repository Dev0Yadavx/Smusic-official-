package com.example.player

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioManager
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class EqualizerPreset(
    val id: String,
    val displayName: String,
    val defaultBandsMb: List<Int>,
    val bassStrength: Int
) {
    NORMAL("normal", "Normal", listOf(0, 0, 0, 0, 0), 0),
    BASS_BOOST("bass_boost", "Bass Boost", listOf(1100, 800, 100, 200, 350), 850),
    JAZZ("jazz", "Jazz", listOf(600, 350, -200, 350, 700), 300),
    POP("pop", "Pop", listOf(-150, 500, 850, 450, -150), 350),
    ROCK("rock", "Rock", listOf(800, 500, -150, 500, 900), 550),
    CLASSICAL("classical", "Classical", listOf(700, 450, -200, 600, 800), 150),
    HIP_HOP("hip_hop", "Hip Hop", listOf(950, 650, 0, 300, 650), 750),
    DANCE("dance", "Dance", listOf(900, 350, 200, 650, 450), 650),
    VOCAL("vocal", "Vocal", listOf(-300, 250, 900, 700, 200), 100),
    HEAVY_METAL("heavy_metal", "Heavy Metal", listOf(600, 200, 800, 450, 900), 600),
    FLAT("flat", "Flat", listOf(0, 0, 0, 0, 0), 0),
    CUSTOM("custom", "Custom", listOf(0, 0, 0, 0, 0), 400);

    companion object {
        fun fromId(id: String?): EqualizerPreset {
            return entries.firstOrNull { it.id == id } ?: NORMAL
        }
    }
}

class EqualizerManager private constructor(context: Context) {

    private val tag = "EqualizerManager"
    private val appContext = context.applicationContext
    private val prefs: SharedPreferences =
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val audioManager =
        appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var boundAudioSessionId: Int = AudioManager.ERROR

    private val _isEnabled = MutableStateFlow(prefs.getBoolean(KEY_EQ_ENABLED, true))
    val isEnabled: StateFlow<Boolean> = _isEnabled.asStateFlow()

    private val _selectedPreset = MutableStateFlow(
        EqualizerPreset.fromId(prefs.getString(KEY_EQ_PRESET, EqualizerPreset.NORMAL.id))
    )
    val selectedPreset: StateFlow<EqualizerPreset> = _selectedPreset.asStateFlow()

    private val _minBandLevel = MutableStateFlow(-1500)
    val minBandLevel: StateFlow<Int> = _minBandLevel.asStateFlow()

    private val _maxBandLevel = MutableStateFlow(1500)
    val maxBandLevel: StateFlow<Int> = _maxBandLevel.asStateFlow()

    private val _bandFrequencies = MutableStateFlow(
        listOf("60 Hz", "230 Hz", "910 Hz", "3.6 kHz", "14 kHz")
    )
    val bandFrequencies: StateFlow<List<String>> = _bandFrequencies.asStateFlow()

    private val _bandLevels = MutableStateFlow(loadSavedBands())
    val bandLevels: StateFlow<List<Int>> = _bandLevels.asStateFlow()

    private val _bassBoostStrength = MutableStateFlow(
        prefs.getInt(KEY_BASS_STRENGTH, _selectedPreset.value.bassStrength)
    )
    val bassBoostStrength: StateFlow<Int> = _bassBoostStrength.asStateFlow()

    init {
        val initialSession = try {
            audioManager.generateAudioSessionId()
        } catch (_: Exception) {
            0
        }
        if (initialSession > 0) {
            bindToAudioSession(initialSession)
        }
    }

    private fun loadSavedBands(): List<Int> {
        val preset = EqualizerPreset.fromId(prefs.getString(KEY_EQ_PRESET, EqualizerPreset.NORMAL.id))
        val savedCsv = prefs.getString(KEY_CUSTOM_BANDS, null)
        if (preset == EqualizerPreset.CUSTOM && !savedCsv.isNullOrBlank()) {
            val parsed = savedCsv.split(",").mapNotNull { it.trim().toIntOrNull() }
            if (parsed.size == 5) return parsed
        }
        return preset.defaultBandsMb
    }

    private fun saveBands(bands: List<Int>) {
        prefs.edit().putString(KEY_CUSTOM_BANDS, bands.joinToString(",")).apply()
    }

    fun getOrGenerateAudioSessionId(): Int {
        if (boundAudioSessionId <= 0) {
            boundAudioSessionId = try {
                audioManager.generateAudioSessionId()
            } catch (_: Exception) {
                0
            }
        }
        return boundAudioSessionId
    }

    @Synchronized
    fun bindToAudioSession(audioSessionId: Int) {
        if (audioSessionId < 0) return
        if (boundAudioSessionId == audioSessionId && equalizer != null) {
            applyCurrentStateToHardware()
            return
        }

        releaseHardwareEffects()
        boundAudioSessionId = audioSessionId

        try {
            val eq = Equalizer(0, audioSessionId)
            equalizer = eq

            val numBands = eq.numberOfBands.toInt()
            if (numBands > 0) {
                val range = eq.bandLevelRange
                if (range != null && range.size >= 2) {
                    _minBandLevel.value = range[0].toInt()
                    _maxBandLevel.value = range[1].toInt()
                }
                val freqs = (0 until numBands).map { bandIdx ->
                    val freqMilliHz = eq.getCenterFreq(bandIdx.toShort())
                    val freqHz = freqMilliHz / 1000
                    if (freqHz >= 1000) {
                        val khz = freqHz / 1000f
                        if (khz % 1f == 0f) "${khz.toInt()} kHz" else String.format("%.1f kHz", khz)
                    } else {
                        "$freqHz Hz"
                    }
                }
                if (freqs.isNotEmpty()) {
                    _bandFrequencies.value = freqs
                }
            }
        } catch (e: Exception) {
            Log.w(tag, "Android Equalizer hardware init fallback for session $audioSessionId: ${e.message}")
        }

        try {
            val bb = BassBoost(0, audioSessionId)
            bassBoost = bb
        } catch (e: Exception) {
            Log.w(tag, "Android BassBoost hardware init fallback: ${e.message}")
        }

        applyCurrentStateToHardware()
    }

    fun setEnabled(enabled: Boolean) {
        _isEnabled.value = enabled
        prefs.edit().putBoolean(KEY_EQ_ENABLED, enabled).apply()
        applyCurrentStateToHardware()
    }

    fun selectPreset(preset: EqualizerPreset) {
        _selectedPreset.value = preset
        if (!_isEnabled.value) {
            _isEnabled.value = true
            prefs.edit().putBoolean(KEY_EQ_ENABLED, true).apply()
        }
        val targetBands = if (preset == EqualizerPreset.CUSTOM) {
            loadSavedBands()
        } else {
            preset.defaultBandsMb
        }
        _bandLevels.value = targetBands
        _bassBoostStrength.value = preset.bassStrength

        prefs.edit()
            .putString(KEY_EQ_PRESET, preset.id)
            .putInt(KEY_BASS_STRENGTH, preset.bassStrength)
            .apply()

        if (preset == EqualizerPreset.CUSTOM) {
            saveBands(targetBands)
        }

        applyCurrentStateToHardware()
    }

    fun setBandLevel(bandIndex: Int, levelMb: Int) {
        val clamped = levelMb.coerceIn(_minBandLevel.value, _maxBandLevel.value)
        val updated = _bandLevels.value.toMutableList()
        if (bandIndex in updated.indices) {
            updated[bandIndex] = clamped
            _bandLevels.value = updated
            _selectedPreset.value = EqualizerPreset.CUSTOM
            prefs.edit().putString(KEY_EQ_PRESET, EqualizerPreset.CUSTOM.id).apply()
            saveBands(updated)

            try {
                equalizer?.let { eq ->
                    if (bandIndex < eq.numberOfBands.toInt()) {
                        eq.setBandLevel(bandIndex.toShort(), clamped.toShort())
                    }
                }
            } catch (e: Exception) {
                Log.w(tag, "Failed to set band $bandIndex level: ${e.message}")
            }
        }
    }

    fun setBassBoostStrength(strength: Int) {
        val clamped = strength.coerceIn(0, 1000)
        _bassBoostStrength.value = clamped
        prefs.edit().putInt(KEY_BASS_STRENGTH, clamped).apply()
        try {
            bassBoost?.let { bb ->
                if (bb.strengthSupported) {
                    bb.setStrength(clamped.toShort())
                }
            }
        } catch (e: Exception) {
            Log.w(tag, "Failed to set bass boost strength: ${e.message}")
        }
    }

    @Synchronized
    private fun applyCurrentStateToHardware() {
        val enabled = _isEnabled.value
        val preset = _selectedPreset.value
        val levels = _bandLevels.value
        val minMb = _minBandLevel.value
        val maxMb = _maxBandLevel.value

        try {
            equalizer?.let { eq ->
                eq.enabled = enabled
                if (enabled) {
                    // First check if Android's Equalizer has a built-in preset matching this name
                    var matchedHardwarePreset: Short = -1
                    if (preset != EqualizerPreset.CUSTOM && preset != EqualizerPreset.BASS_BOOST) {
                        val presetCount = eq.numberOfPresets.toInt()
                        for (i in 0 until presetCount) {
                            val hwName = eq.getPresetName(i.toShort()) ?: ""
                            if (hwName.equals(preset.displayName, ignoreCase = true)) {
                                matchedHardwarePreset = i.toShort()
                                break
                            }
                        }
                    }

                    if (matchedHardwarePreset >= 0) {
                        try {
                            eq.usePreset(matchedHardwarePreset)
                        } catch (_: Exception) {}
                    }

                    // Always apply exact target band levels so 'Bass Boost', 'Jazz', 'Pop', etc. shape the audio
                    val numBands = eq.numberOfBands.toInt()
                    for (b in 0 until numBands) {
                        val level = levels.getOrElse(b) { 0 }.coerceIn(minMb, maxMb)
                        eq.setBandLevel(b.toShort(), level.toShort())
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(tag, "Equalizer hardware apply warning: ${e.message}")
        }

        try {
            bassBoost?.let { bb ->
                bb.enabled = enabled && _bassBoostStrength.value > 0
                if (enabled && bb.strengthSupported) {
                    bb.setStrength(_bassBoostStrength.value.coerceIn(0, 1000).toShort())
                }
            }
        } catch (e: Exception) {
            Log.w(tag, "BassBoost hardware apply warning: ${e.message}")
        }
    }

    @Synchronized
    private fun releaseHardwareEffects() {
        try {
            equalizer?.release()
        } catch (_: Exception) {}
        equalizer = null

        try {
            bassBoost?.release()
        } catch (_: Exception) {}
        bassBoost = null
    }

    companion object {
        private const val PREFS_NAME = "smusic_equalizer_prefs"
        private const val KEY_EQ_ENABLED = "key_eq_enabled"
        private const val KEY_EQ_PRESET = "key_eq_preset"
        private const val KEY_CUSTOM_BANDS = "key_custom_bands"
        private const val KEY_BASS_STRENGTH = "key_bass_strength"

        @Volatile
        private var instance: EqualizerManager? = null

        fun getInstance(context: Context): EqualizerManager {
            return instance ?: synchronized(this) {
                instance ?: EqualizerManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
