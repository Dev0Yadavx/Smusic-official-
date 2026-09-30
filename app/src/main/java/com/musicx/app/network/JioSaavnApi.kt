package com.musicx.app.network

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query

interface JioSaavnApi {

    // 1. Song Details (lyrics_id extract karne ke liye)
    @Headers("User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
    @GET("api.php?__call=song.getDetails&_format=json&_marker=0&api_version=4&ctx=web6dot0")
    suspend fun getSongDetails(
        @Query("pids") pids: String
    ): Response<JsonObject>

    // 2. Lyrics API (Passing accurate lyrics_id)
    @Headers("User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
    @GET("api.php?__call=lyrics.getLyrics&_format=json&_marker=0&api_version=4&ctx=web6dot0")
    suspend fun getLyrics(
        @Query("lyrics_id") lyricsId: String
    ): Response<JsonObject>

    // 3. Queue / Auto-play recommendations (Accepts JsonElement: Handles both Array & Object structures)
    @Headers("User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
    @GET("api.php?__call=reco.getreco&_format=json&_marker=0&api_version=4&ctx=web6dot0")
    suspend fun getQueueRecommendations(
        @Query("pid") songId: String
    ): Response<JsonElement>
}
