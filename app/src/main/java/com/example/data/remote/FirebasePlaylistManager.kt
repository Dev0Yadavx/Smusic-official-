package com.example.data.remote

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.example.R
import com.example.data.model.Playlist
import com.example.data.model.Song
import com.example.player.SMusicNotificationHelper
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

enum class OperationType {
    CREATE, UPDATE, DELETE, LIST, GET, WRITE
}

@OptIn(ExperimentalCoroutinesApi::class)
class FirebasePlaylistManager private constructor(private val appContext: Context) {

    private val tag = "FirebasePlaylistManager"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val gson = Gson()

    private val auth: FirebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }

    private val firestore: FirebaseFirestore? by lazy {
        try {
            val dbId = appContext.getString(R.string.firestore_database_id)
            FirebaseFirestore.getInstance(dbId)
        } catch (e: Exception) {
            Log.e(tag, "Firestore initialization failed: ${e.message}", e)
            null
        }
    }

    private val _currentUser = MutableStateFlow<FirebaseUser?>(try { auth.currentUser } catch (_: Exception) { null })
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    fun authStateChanges(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            _currentUser.value = firebaseAuth.currentUser
            trySend(firebaseAuth.currentUser)
        }
        try {
            auth.addAuthStateListener(listener)
        } catch (e: Exception) {
            Log.e(tag, "AuthStateListener registration warning: ${e.message}")
            trySend(null)
        }
        awaitClose {
            try {
                auth.removeAuthStateListener(listener)
            } catch (_: Exception) {}
        }
    }

    val cloudPlaylists: StateFlow<List<Playlist>> = authStateChanges()
        .flatMapLatest { user ->
            val db = firestore
            if (user == null || db == null) {
                flowOf(emptyList())
            } else {
                observeUserCloudPlaylists(db, user.uid)
            }
        }
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun observeUserCloudPlaylists(db: FirebaseFirestore, uid: String): Flow<List<Playlist>> = callbackFlow {
        val path = "playlists"
        val registration = db.collection(path)
            .whereEqualTo("ownerId", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, path)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val playlists = snapshot.documents.mapNotNull { doc ->
                        try {
                            val id = doc.getString("id") ?: doc.id
                            val title = doc.getString("title") ?: "Cloud Playlist"
                            val subtitle = doc.getString("subtitle") ?: ""
                            val description = doc.getString("description") ?: ""
                            val rawArt = doc.getString("artwork") ?: ""
                            val artwork = JioSaavnImageResolver.resolve(rawArt, 500).ifBlank { rawArt }
                            @Suppress("UNCHECKED_CAST")
                            val rawPreviews = (doc.get("previewArtworks") as? List<*>)
                                ?.mapNotNull { it as? String }
                                ?.map { JioSaavnImageResolver.resolve(it, 500).ifBlank { it } }
                                ?: emptyList()
                            val songCount = (doc.getLong("songCount") ?: 0L).toInt()
                            val songsJson = doc.getString("songsJson") ?: "[]"
                            val songs: List<Song> = try {
                                val listType = object : TypeToken<List<Song>>() {}.type
                                gson.fromJson<List<Song>>(songsJson, listType) ?: emptyList()
                            } catch (_: Exception) {
                                emptyList()
                            }

                            val finalPreviews = if (rawPreviews.isNotEmpty()) {
                                rawPreviews
                            } else {
                                songs.mapNotNull { it.artwork.takeIf { a -> a.isNotBlank() } }.distinct().take(4)
                            }
                            val finalArt = artwork.ifBlank { finalPreviews.firstOrNull() ?: "" }
                            val finalCount = if (songs.isNotEmpty()) songs.size else songCount

                            Playlist(
                                id = if (id.startsWith("firebase_")) id else "firebase_$id",
                                title = title,
                                subtitle = subtitle.ifBlank { "$finalCount Songs • Firebase Cloud" },
                                description = description.ifBlank { "Synced with Firebase Cloud" },
                                artwork = finalArt,
                                previewArtworks = finalPreviews,
                                songCount = finalCount,
                                songs = songs,
                                isCloudSynced = true
                            )
                        } catch (e: Exception) {
                            Log.w(tag, "Error mapping cloud playlist doc ${doc.id}: ${e.message}")
                            null
                        }
                    }
                    trySend(playlists)
                }
            }
        awaitClose {
            registration.remove()
        }
    }

    suspend fun signInWithGoogle(activityContext: Context): Result<FirebaseUser> = withContext(Dispatchers.IO) {
        try {
            val credentialManager = CredentialManager.create(activityContext)
            val webClientId = try {
                val resId = activityContext.resources.getIdentifier(
                    "default_web_client_id",
                    "string",
                    activityContext.packageName
                )
                if (resId != 0) activityContext.getString(resId)
                else "870879087798-dv815etfl7d1gjapakel70hg72b89ch6.apps.googleusercontent.com"
            } catch (_: Exception) {
                "870879087798-dv815etfl7d1gjapakel70hg72b89ch6.apps.googleusercontent.com"
            }

            val signInOption = GetSignInWithGoogleOption.Builder(webClientId).build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(signInOption)
                .build()

            val response = credentialManager.getCredential(activityContext, request)
            val credential = response.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val firebaseCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
                val authResult = auth.signInWithCredential(firebaseCredential).await()
                val user = authResult.user
                if (user != null) {
                    _currentUser.value = user
                    _syncMessage.value = "Signed in as ${user.displayName ?: user.email ?: "User"}"
                    return@withContext Result.success(user)
                }
            }
            Result.failure(IllegalStateException("Unsupported credential type"))
        } catch (e: Exception) {
            Log.e(tag, "Google Sign-In failed: ${e.message}", e)
            _syncMessage.value = "Sign-in cancelled or unavailable: ${e.localizedMessage ?: e.message}"
            Result.failure(e)
        }
    }

    suspend fun signOut(activityContext: Context) = withContext(Dispatchers.IO) {
        try {
            auth.signOut()
            _currentUser.value = null
            val credentialManager = CredentialManager.create(activityContext)
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
            _syncMessage.value = "Signed out of Firebase Cloud"
        } catch (e: Exception) {
            Log.w(tag, "Sign-out warning: ${e.message}")
        }
    }

    suspend fun syncPlaylistToCloud(
        playlist: Playlist,
        notifyUser: Boolean = true
    ): Boolean = withContext(Dispatchers.IO) {
        val user = try { auth.currentUser } catch (_: Exception) { null } ?: return@withContext false
        val db = firestore ?: return@withContext false

        _isSyncing.value = true
        val cleanBaseId = playlist.id
            .removePrefix("firebase_")
            .removePrefix("local_")
            .replace(Regex("[^a-zA-Z0-9_-]"), "_")
            .take(80)
            .ifBlank { "pl_${System.currentTimeMillis()}" }
        val docId = "${user.uid}_$cleanBaseId".take(120)
        val path = "playlists/$docId"

        try {
            val cleanPreviews = (playlist.previewArtworks + playlist.songs.map { it.artwork } + listOf(playlist.artwork))
                .map { JioSaavnImageResolver.resolve(it, 500).ifBlank { it } }
                .filter { it.isNotBlank() && it.length <= 1500 }
                .distinct()
                .take(4)

            val primaryArt = cleanPreviews.firstOrNull() ?: ""
            val songsToSerialize = playlist.songs.take(250)
            val songsJsonStr = gson.toJson(songsToSerialize).let {
                if (it.length <= 480_000) it else "[]"
            }
            val finalCount = if (playlist.songs.isNotEmpty()) playlist.songs.size else playlist.songCount.coerceAtLeast(0)
            val safeTitle = playlist.title.trim().ifBlank { "SMusic Playlist" }.take(190)
            val safeSubtitle = "${finalCount} Songs • Firebase Cloud".take(250)
            val safeDesc = playlist.description.trim().ifBlank { "Synced with SMusic Firebase Cloud" }.take(950)
            val safeOwnerName = (user.displayName ?: user.email ?: "SMusic Listener").take(140)

            val docRef = db.collection("playlists").document(docId)
            val existingSnap = try { docRef.get().await() } catch (_: Exception) { null }

            val payload = hashMapOf<String, Any>(
                "id" to docId,
                "title" to safeTitle,
                "subtitle" to safeSubtitle,
                "description" to safeDesc,
                "artwork" to primaryArt,
                "previewArtworks" to cleanPreviews,
                "songCount" to finalCount,
                "ownerId" to user.uid,
                "ownerName" to safeOwnerName,
                "isPublic" to true,
                "songsJson" to songsJsonStr,
                "updatedAt" to FieldValue.serverTimestamp()
            )

            if (existingSnap != null && existingSnap.exists() && existingSnap.getTimestamp("createdAt") != null) {
                payload["createdAt"] = existingSnap.getTimestamp("createdAt")!!
                docRef.set(payload).await()
            } else {
                payload["createdAt"] = FieldValue.serverTimestamp()
                docRef.set(payload).await()
            }

            if (notifyUser) {
                SMusicNotificationHelper.showPlaylistSyncNotification(
                    context = appContext,
                    playlistId = docId,
                    playlistTitle = safeTitle,
                    songCount = finalCount,
                    artworkUrl = primaryArt
                )
            }
            _syncMessage.value = "Synced \"$safeTitle\" to Firebase"
            return@withContext true
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            return@withContext false
        } finally {
            _isSyncing.value = false
        }
    }

    suspend fun deleteCloudPlaylist(playlistId: String): Boolean = withContext(Dispatchers.IO) {
        val user = try { auth.currentUser } catch (_: Exception) { null } ?: return@withContext false
        val db = firestore ?: return@withContext false
        val docId = playlistId.removePrefix("firebase_")
        val path = "playlists/$docId"
        try {
            db.collection("playlists").document(docId).delete().await()
            return@withContext true
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            return@withContext false
        }
    }

    fun getCachedCloudPlaylist(playlistId: String): Playlist? {
        val clean = playlistId.removePrefix("firebase_")
        return cloudPlaylists.value.firstOrNull {
            it.id == playlistId || it.id.removePrefix("firebase_") == clean
        }
    }

    private fun handleFirestoreError(error: Exception, operationType: OperationType, path: String?) {
        val currentUser = try { auth.currentUser } catch (_: Exception) { null }
        val providerArray = JSONArray()
        currentUser?.providerData?.forEach { profile ->
            val providerObj = JSONObject().apply {
                put("providerId", profile.providerId)
                put("displayName", profile.displayName ?: JSONObject.NULL)
                put("email", profile.email ?: JSONObject.NULL)
                put("photoUrl", profile.photoUrl?.toString() ?: JSONObject.NULL)
            }
            providerArray.put(providerObj)
        }

        val authObj = JSONObject().apply {
            put("userId", currentUser?.uid ?: JSONObject.NULL)
            put("email", currentUser?.email ?: JSONObject.NULL)
            put("displayName", currentUser?.displayName ?: JSONObject.NULL)
            put("emailVerified", currentUser?.isEmailVerified ?: JSONObject.NULL)
            put("isAnonymous", currentUser?.isAnonymous ?: JSONObject.NULL)
            put("providerInfo", providerArray)
        }

        val errInfo = JSONObject().apply {
            put("error", error.message ?: "Unknown Firestore error")
            put("operationType", operationType.name.lowercase())
            put("path", path ?: JSONObject.NULL)
            put("authInfo", authObj)
        }

        Log.e("FirestoreError", errInfo.toString(), error)
    }

    companion object {
        @Volatile
        private var INSTANCE: FirebasePlaylistManager? = null

        fun getInstance(context: Context): FirebasePlaylistManager {
            return INSTANCE ?: synchronized(this) {
                val instance = FirebasePlaylistManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
