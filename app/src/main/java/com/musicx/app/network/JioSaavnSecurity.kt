package com.musicx.app.network

import android.util.Base64
import android.util.Log
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

object JioSaavnSecurity {
    private const val TAG = "JioSaavnSecurity"
    private const val DES_SECRET_KEY = "38346591"

    /**
     * Decrypts encrypted_media_url to 320kbps MP4 / AAC stream
     */
    fun decryptTo320Stream(encryptedMediaUrl: String?): String {
        if (encryptedMediaUrl.isNullOrEmpty()) return ""
        return try {
            val keyBytes = DES_SECRET_KEY.toByteArray(Charsets.UTF_8)
            val keySpec = SecretKeySpec(keyBytes, "DES")
            val cipher = Cipher.getInstance("DES/ECB/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, keySpec)

            val decodedBytes = Base64.decode(encryptedMediaUrl.trim(), Base64.DEFAULT)
            val decryptedBytes = cipher.doFinal(decodedBytes)
            var baseLink = String(decryptedBytes, Charsets.UTF_8).trim()
            baseLink = baseLink.filter { it >= ' ' && it <= '~' }

            if (baseLink.startsWith("http://")) {
                baseLink = "https://" + baseLink.substring(7)
            }

            // Prefer high-fidelity 320kbps stream
            baseLink.replace("_96.mp4", "_320.mp4")
                .replace("_160.mp4", "_320.mp4")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decrypt stream: ${e.message}")
            ""
        }
    }

    /**
     * Resolves 500x500 HD Cover Art
     */
    fun getHdImage(url: String?): String {
        if (url.isNullOrEmpty()) return ""
        return url.replace("\\/", "/")
            .replace("http://", "https://")
            .replace("150x150", "500x500")
            .replace("50x50", "500x500")
            .replace("250x250", "500x500")
    }
}
