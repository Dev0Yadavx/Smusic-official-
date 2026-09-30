package com.musicx.app.utils

object PlaylistLinkParser {
    /**
     * Extracts token from a JioSaavn playlist URL or returns the token if input is a direct token string.
     * Examples:
     * - https://www.jiosaavn.com/featured/romantic-hits/c9_7sW8q_
     * - https://www.jiosaavn.com/s/playlist/user/my-favs/v1h3k5L
     * - https://jiosaavn.com/featured/.../xyz123?autoplay=true
     * - c9_7sW8q_ (direct token)
     */
    fun extractToken(url: String): String? {
        val cleanUrl = url.trim()
        if (cleanUrl.isBlank()) return null

        // If it's a URL, extract the last non-empty path segment before query/fragment
        if (cleanUrl.contains("jiosaavn.com", ignoreCase = true) || cleanUrl.startsWith("http://") || cleanUrl.startsWith("https://")) {
            val urlWithoutQuery = cleanUrl.substringBefore("?").substringBefore("#").trimEnd('/')
            val lastSegment = urlWithoutQuery.substringAfterLast("/")
            if (lastSegment.isNotBlank()) {
                return lastSegment.trim()
            }
        }

        // Direct token or listid
        return cleanUrl.substringBefore("?").substringBefore("#").trim().takeIf { it.isNotBlank() }
    }
}
