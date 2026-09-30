package com.example.data.remote

import java.util.concurrent.ConcurrentHashMap

/**
 * Global Artist DP Manager
 * Provides original, direct high-resolution DP (Display Picture / Avatar)
 * for all artists across the entire application.
 */
object ArtistDpManager {

    private val memoryCache = ConcurrentHashMap<String, String>()
    private val idCache = ConcurrentHashMap<String, String>()

    // Pre-mapped direct high-resolution (500x500) authentic CDN profile pictures
    private val preLoadedArtistDps: Map<String, String> = mapOf(
        // Bollywood & Hindi Legends & Chartbusters
        "arijit singh" to "https://c.saavncdn.com/artists/Arijit_Singh_002_20230323062147_500x500.jpg",
        "shreya ghoshal" to "https://c.saavncdn.com/artists/Shreya_Ghoshal_004_20230419104845_500x500.jpg",
        "neha kakkar" to "https://c.saavncdn.com/artists/Neha_Kakkar_006_20200821105459_500x500.jpg",
        "atif aslam" to "https://c.saavncdn.com/artists/Atif_Aslam_500x500.jpg",
        "badshah" to "https://c.saavncdn.com/artists/Badshah_005_20230608084035_500x500.jpg",
        "diljit dosanjh" to "https://c.saavncdn.com/artists/Diljit_Dosanjh_004_20221018184511_500x500.jpg",
        "sidhu moose wala" to "https://c.saavncdn.com/artists/Sidhu_Moose_Wala_003_20230612044815_500x500.jpg",
        "sidhu moosewala" to "https://c.saavncdn.com/artists/Sidhu_Moose_Wala_003_20230612044815_500x500.jpg",
        "karan aujla" to "https://c.saavncdn.com/artists/Karan_Aujla_005_20230818105435_500x500.jpg",
        "ap dhillon" to "https://c.saavncdn.com/artists/AP_Dhillon_003_20211119154625_500x500.jpg",
        "yo yo honey singh" to "https://c.saavncdn.com/artists/Yo_Yo_Honey_Singh_002_20221124110125_500x500.jpg",
        "honey singh" to "https://c.saavncdn.com/artists/Yo_Yo_Honey_Singh_002_20221124110125_500x500.jpg",
        "guru randhawa" to "https://c.saavncdn.com/artists/Guru_Randhawa_002_20221104111304_500x500.jpg",
        "kk" to "https://c.saavncdn.com/artists/KK_500x500.jpg",
        "sonu nigam" to "https://c.saavncdn.com/artists/Sonu_Nigam_500x500.jpg",
        "sunidhi chauhan" to "https://c.saavncdn.com/artists/Sunidhi_Chauhan_002_20201103094851_500x500.jpg",
        "kumar sanu" to "https://c.saavncdn.com/artists/Kumar_Sanu_500x500.jpg",
        "alka yagnik" to "https://c.saavncdn.com/artists/Alka_Yagnik_500x500.jpg",
        "udit narayan" to "https://c.saavncdn.com/artists/Udit_Narayan_500x500.jpg",
        "lata mangeshkar" to "https://c.saavncdn.com/artists/Lata_Mangeshkar_500x500.jpg",
        "kishore kumar" to "https://c.saavncdn.com/artists/Kishore_Kumar_500x500.jpg",
        "mohammed rafi" to "https://c.saavncdn.com/artists/Mohammed_Rafi_500x500.jpg",
        "asha bhosle" to "https://c.saavncdn.com/artists/Asha_Bhosle_500x500.jpg",
        "mukesh" to "https://c.saavncdn.com/artists/Mukesh_500x500.jpg",
        "jubin nautiyal" to "https://c.saavncdn.com/artists/Jubin_Nautiyal_002_20201029142938_500x500.jpg",
        "armaan malik" to "https://c.saavncdn.com/artists/Armaan_Malik_004_20220610061226_500x500.jpg",
        "darshan raval" to "https://c.saavncdn.com/artists/Darshan_Raval_005_20230419105436_500x500.jpg",
        "anuv jain" to "https://c.saavncdn.com/artists/Anuv_Jain_000_20210203113947_500x500.jpg",
        "prateek kuhad" to "https://c.saavncdn.com/artists/Prateek_Kuhad_002_20220603073934_500x500.jpg",
        "b praak" to "https://c.saavncdn.com/artists/B_Praak_004_20230713070438_500x500.jpg",
        "harrdy sandhu" to "https://c.saavncdn.com/artists/Harrdy_Sandhu_003_20230804071836_500x500.jpg",
        "hardy sandhu" to "https://c.saavncdn.com/artists/Harrdy_Sandhu_003_20230804071836_500x500.jpg",
        "jass manak" to "https://c.saavncdn.com/artists/Jass_Manak_002_20220819124436_500x500.jpg",
        "amrit maan" to "https://c.saavncdn.com/artists/Amrit_Maan_002_20210427103445_500x500.jpg",
        "shubh" to "https://c.saavncdn.com/artists/Shubh_000_20220516104115_500x500.jpg",
        "mc stan" to "https://c.saavncdn.com/artists/MC_Stan_002_20221206132749_500x500.jpg",
        "divine" to "https://c.saavncdn.com/artists/DIVINE_004_20221104104116_500x500.jpg",
        "raftaar" to "https://c.saavncdn.com/artists/Raftaar_004_20230420072935_500x500.jpg",
        "king" to "https://c.saavncdn.com/artists/King_002_20221013063520_500x500.jpg",
        "emiway bantai" to "https://c.saavncdn.com/artists/Emiway_Bantai_003_20230623062024_500x500.jpg",
        "kr\$na" to "https://c.saavncdn.com/artists/KR_NA_002_20220708064848_500x500.jpg",
        "krsna" to "https://c.saavncdn.com/artists/KR_NA_002_20220708064848_500x500.jpg",
        "seedhe maut" to "https://c.saavncdn.com/artists/Seedhe_Maut_002_20230602075638_500x500.jpg",
        "a.r. rahman" to "https://c.saavncdn.com/artists/A_R__Rahman_002_20210323081407_500x500.jpg",
        "ar rahman" to "https://c.saavncdn.com/artists/A_R__Rahman_002_20210323081407_500x500.jpg",
        "pritam" to "https://c.saavncdn.com/artists/Pritam_Chakraborty_500x500.jpg",
        "pritam chakraborty" to "https://c.saavncdn.com/artists/Pritam_Chakraborty_500x500.jpg",
        "sachin-jigar" to "https://c.saavncdn.com/artists/Sachin_Jigar_500x500.jpg",
        "sachin jigar" to "https://c.saavncdn.com/artists/Sachin_Jigar_500x500.jpg",
        "vishal-shekhar" to "https://c.saavncdn.com/artists/Vishal_Shekhar_500x500.jpg",
        "vishal shekhar" to "https://c.saavncdn.com/artists/Vishal_Shekhar_500x500.jpg",
        "amit trivedi" to "https://c.saavncdn.com/artists/Amit_Trivedi_002_20201103094917_500x500.jpg",
        "anirudh ravichander" to "https://c.saavncdn.com/artists/Anirudh_Ravichander_003_20230810052737_500x500.jpg",
        "anirudh" to "https://c.saavncdn.com/artists/Anirudh_Ravichander_003_20230810052737_500x500.jpg",
        "himesh reshammiya" to "https://c.saavncdn.com/artists/Himesh_Reshammiya_500x500.jpg",
        "mika singh" to "https://c.saavncdn.com/artists/Mika_Singh_500x500.jpg",
        "mohit chauhan" to "https://c.saavncdn.com/artists/Mohit_Chauhan_500x500.jpg",
        "papon" to "https://c.saavncdn.com/artists/Papon_500x500.jpg",
        "lucky ali" to "https://c.saavncdn.com/artists/Lucky_Ali_500x500.jpg",
        "jagjit singh" to "https://c.saavncdn.com/artists/Jagjit_Singh_500x500.jpg",
        "rahat fateh ali khan" to "https://c.saavncdn.com/artists/Rahat_Fateh_Ali_Khan_500x500.jpg",
        "nusrat fateh ali khan" to "https://c.saavncdn.com/artists/Nusrat_Fateh_Ali_Khan_500x500.jpg",
        "khesari lal yadav" to "https://c.saavncdn.com/artists/Khesari_Lal_Yadav_002_20230419105436_500x500.jpg",
        "pawan singh" to "https://c.saavncdn.com/artists/Pawan_Singh_002_20230419105436_500x500.jpg",
        "shilpi raj" to "https://c.saavncdn.com/artists/Shilpi_Raj_002_20220624095436_500x500.jpg",

        // Global Pop / International
        "taylor swift" to "https://c.saavncdn.com/artists/Taylor_Swift_500x500.jpg",
        "ed sheeran" to "https://c.saavncdn.com/artists/Ed_Sheeran_500x500.jpg",
        "justin bieber" to "https://c.saavncdn.com/artists/Justin_Bieber_500x500.jpg",
        "drake" to "https://c.saavncdn.com/artists/Drake_500x500.jpg",
        "the weeknd" to "https://c.saavncdn.com/artists/The_Weeknd_500x500.jpg",
        "dua lipa" to "https://c.saavncdn.com/artists/Dua_Lipa_500x500.jpg",
        "billie eilish" to "https://c.saavncdn.com/artists/Billie_Eilish_500x500.jpg",
        "bts" to "https://c.saavncdn.com/artists/BTS_500x500.jpg",
        "selena gomez" to "https://c.saavncdn.com/artists/Selena_Gomez_500x500.jpg",
        "ariana grande" to "https://c.saavncdn.com/artists/Ariana_Grande_500x500.jpg",
        "charlie puth" to "https://c.saavncdn.com/artists/Charlie_Puth_500x500.jpg",
        "shawn mendes" to "https://c.saavncdn.com/artists/Shawn_Mendes_500x500.jpg",
        "alan walker" to "https://c.saavncdn.com/artists/Alan_Walker_500x500.jpg",
        "marshmello" to "https://c.saavncdn.com/artists/Marshmello_500x500.jpg",
        "eminem" to "https://c.saavncdn.com/artists/Eminem_500x500.jpg",
        "bruno mars" to "https://c.saavncdn.com/artists/Bruno_Mars_500x500.jpg",
        "rihanna" to "https://c.saavncdn.com/artists/Rihanna_500x500.jpg",
        "shakira" to "https://c.saavncdn.com/artists/Shakira_500x500.jpg",
        "coldplay" to "https://c.saavncdn.com/artists/Coldplay_500x500.jpg",
        "imagine dragons" to "https://c.saavncdn.com/artists/Imagine_Dragons_500x500.jpg"
    )

    init {
        memoryCache.putAll(preLoadedArtistDps)
    }

    /**
     * Store an artist's original DP by Name
     */
    fun put(name: String, url: String) {
        val cleanName = cleanKey(name)
        val resolved = JioSaavnImageResolver.resolve(url, 500)
        if (cleanName.isNotBlank() && resolved.isNotBlank() && isValidUrl(resolved)) {
            memoryCache[cleanName] = resolved
        }
    }

    /**
     * Store an artist's original DP by ID
     */
    fun putById(id: String, url: String) {
        val cleanId = id.trim()
        val resolved = JioSaavnImageResolver.resolve(url, 500)
        if (cleanId.isNotBlank() && resolved.isNotBlank() && isValidUrl(resolved)) {
            idCache[cleanId] = resolved
        }
    }

    /**
     * Lookup artist original DP by Name or ID
     */
    fun get(nameOrId: String): String? {
        val key = cleanKey(nameOrId)
        return memoryCache[key] ?: idCache[nameOrId.trim()] ?: findPartialMatch(key)
    }

    /**
     * Returns the genuine original direct DP URL for an artist.
     * Returns empty string if no authentic photo exists (allowing custom SMusic artwork to render).
     */
    fun getOriginalDp(name: String, existingImage: String? = null): String {
        // 1. If existing image is present and is NOT a dummy/default placeholder, resolve to 500x500
        if (!existingImage.isNullOrBlank() && isValidUrl(existingImage)) {
            val resolved = JioSaavnImageResolver.resolve(existingImage, 500)
            if (isValidUrl(resolved)) {
                put(name, resolved)
                return resolved
            }
        }

        // 2. Check memory cache by clean name or ID
        val cached = get(name)
        if (!cached.isNullOrBlank() && isValidUrl(cached)) {
            return cached
        }

        return ""
    }

    fun isJioPlaceholder(url: String?): Boolean {
        if (url.isNullOrBlank()) return true
        val lower = url.lowercase()
        return lower.contains("artist-default") ||
               lower.contains("default-artist") ||
               lower.contains("default_artist") ||
               lower.contains("artist_default") ||
               lower.contains("playlist-default") ||
               lower.contains("album-default") ||
               lower.contains("/_i/3.0/") ||
               lower.contains("/_i/") ||
               lower.contains("default_") ||
               lower.contains("dummy") ||
               lower.contains("placeholder")
    }

    private fun cleanKey(key: String): String {
        return key.trim().lowercase()
            .replace(".", "")
            .replace("-", " ")
            .replace("_", " ")
    }

    private fun isValidUrl(url: String): Boolean {
        if (url.isBlank()) return false
        val lower = url.lowercase()
        return !isJioPlaceholder(lower) &&
               (lower.startsWith("http://") || lower.startsWith("https://"))
    }

    private fun findPartialMatch(key: String): String? {
        for ((cachedKey, url) in memoryCache) {
            if (cachedKey.length >= 3 && (key.contains(cachedKey) || cachedKey.contains(key))) {
                return url
            }
        }
        return null
    }
}
