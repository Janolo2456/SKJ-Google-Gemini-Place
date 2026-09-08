package com.example.firebase

import android.content.Context
import android.util.Log
import com.example.gemini.ChatMessage
import com.example.gemini.CreationItem
import com.example.gemini.CreationType
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

data class UserProfile(
    val uid: String,
    val displayName: String?,
    val email: String?,
    val photoUrl: String?,
    val isAnonymous: Boolean = false
)

class FirebaseSyncManager(private val context: Context) {

    private val TAG = "FirebaseSyncManager"

    private var auth: FirebaseAuth? = null
    private var firestore: FirebaseFirestore? = null

    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    private val _creations = MutableStateFlow<List<CreationItem>>(emptyList())
    val creations: StateFlow<List<CreationItem>> = _creations.asStateFlow()

    private val _syncStatus = MutableStateFlow("Initialized")
    val syncStatus: StateFlow<String> = _syncStatus.asStateFlow()

    private var creationsListener: ListenerRegistration? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        initFirebase()
    }

    private fun initFirebase() {
        try {
            auth = FirebaseAuth.getInstance()
            firestore = FirebaseFirestore.getInstance()

            auth?.addAuthStateListener { firebaseAuth ->
                val user = firebaseAuth.currentUser
                if (user != null) {
                    val isAnon = user.isAnonymous
                    val displayEmail = if (isAnon) null else user.email
                    val name = user.displayName ?: if (isAnon) "Guest Explorer" else (user.email?.substringBefore("@") ?: "User")
                    _currentUser.value = UserProfile(
                        uid = user.uid,
                        displayName = name,
                        email = displayEmail,
                        photoUrl = user.photoUrl?.toString(),
                        isAnonymous = isAnon
                    )
                    _syncStatus.value = if (displayEmail != null) "Connected • $displayEmail" else "Connected • Anonymous Guest"
                    attachRealtimeSync(user.uid)
                } else {
                    _currentUser.value = null
                    _syncStatus.value = "Signed out"
                    detachRealtimeSync()
                }
            }

            // Auto-sign in anonymously if not already signed in for instant cross-device readiness
            if (auth?.currentUser == null) {
                signInAnonymously()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Firebase initialization fallback: ${e.message}")
            _syncStatus.value = "Local Sync Mode"
            _currentUser.value = UserProfile(
                uid = "local-user-id",
                displayName = "Guest Explorer",
                email = null,
                photoUrl = null,
                isAnonymous = true
            )
        }
    }

    fun signInAnonymously() {
        scope.launch {
            try {
                auth?.signInAnonymously()?.await()
                _syncStatus.value = "Anonymous Realtime Active"
            } catch (e: Exception) {
                Log.e(TAG, "Sign in anonymous error: ${e.message}")
                _syncStatus.value = "Local Sync Mode"
            }
        }
    }

    fun signInWithEmail(email: String, pass: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        scope.launch {
            try {
                if (auth != null) {
                    try {
                        auth?.signInWithEmailAndPassword(email, pass)?.await()
                    } catch (signInErr: Exception) {
                        val msg = signInErr.message.orEmpty()
                        if (msg.contains("no user record", ignoreCase = true) ||
                            msg.contains("user-not-found", ignoreCase = true) ||
                            msg.contains("invalid-credential", ignoreCase = true)
                        ) {
                            try {
                                auth?.createUserWithEmailAndPassword(email, pass)?.await()
                            } catch (_: Exception) {
                                throw signInErr
                            }
                        } else {
                            throw signInErr
                        }
                    }
                    withContext(Dispatchers.Main) { onSuccess() }
                } else {
                    _currentUser.value = UserProfile(
                        uid = "user-" + email.hashCode(),
                        displayName = email.substringBefore("@"),
                        email = email,
                        photoUrl = null,
                        isAnonymous = false
                    )
                    withContext(Dispatchers.Main) { onSuccess() }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onError(e.localizedMessage ?: "Sign in failed") }
            }
        }
    }

    suspend fun signInWithGoogleToken(idToken: String): Result<UserProfile> = withContext(Dispatchers.IO) {
        try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth?.signInWithCredential(credential)?.await()
            val user = authResult?.user
            if (user != null) {
                val profile = UserProfile(
                    uid = user.uid,
                    displayName = user.displayName,
                    email = user.email,
                    photoUrl = user.photoUrl?.toString(),
                    isAnonymous = false
                )
                _currentUser.value = profile
                Result.success(profile)
            } else {
                Result.failure(Exception("Google Sign-In user null"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun signOut() {
        scope.launch {
            try {
                auth?.signOut()
                _creations.value = emptyList()
                signInAnonymously()
            } catch (e: Exception) {
                _currentUser.value = null
            }
        }
    }

    private fun attachRealtimeSync(uid: String) {
        detachRealtimeSync()
        try {
            val db = firestore ?: return
            creationsListener = db.collection("users")
                .document(uid)
                .collection("creations")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshots, error ->
                    if (error != null) {
                        Log.e(TAG, "Firestore sync error: ${error.message}")
                        return@addSnapshotListener
                    }

                    if (snapshots != null) {
                        val list = snapshots.documents.mapNotNull { doc ->
                            try {
                                val typeStr = doc.getString("type") ?: CreationType.IMAGE.name
                                val type = try { CreationType.valueOf(typeStr) } catch (e: Exception) { CreationType.IMAGE }
                                CreationItem(
                                    id = doc.id,
                                    type = type,
                                    title = doc.getString("title") ?: "",
                                    prompt = doc.getString("prompt") ?: "",
                                    model = doc.getString("model") ?: "",
                                    mediaUrl = doc.getString("mediaUrl"),
                                    base64Data = doc.getString("base64Data"),
                                    textResult = doc.getString("textResult"),
                                    audioDurationSec = doc.getLong("audioDurationSec")?.toInt() ?: 0,
                                    videoAspectRatio = doc.getString("videoAspectRatio") ?: "16:9",
                                    imageResolution = doc.getString("imageResolution") ?: "1K",
                                    timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis(),
                                    isSynced = true
                                )
                            } catch (e: Exception) {
                                null
                            }
                        }
                        _creations.value = list
                        _syncStatus.value = "Synced ${list.size} items in real-time"
                    }
                }
        } catch (e: Exception) {
            Log.w(TAG, "Firestore sync attach failed: ${e.message}")
        }
    }

    private fun detachRealtimeSync() {
        creationsListener?.remove()
        creationsListener = null
    }

    fun syncCreation(item: CreationItem) {
        // Optimistically add to local state immediately
        val current = _creations.value.toMutableList()
        current.removeAll { it.id == item.id }
        current.add(0, item.copy(isSynced = true))
        _creations.value = current

        scope.launch {
            val uid = _currentUser.value?.uid ?: auth?.currentUser?.uid
            val db = firestore
            if (uid != null && db != null) {
                try {
                    val data = hashMapOf(
                        "id" to item.id,
                        "type" to item.type.name,
                        "title" to item.title,
                        "prompt" to item.prompt,
                        "model" to item.model,
                        "mediaUrl" to item.mediaUrl,
                        "base64Data" to item.base64Data,
                        "textResult" to item.textResult,
                        "audioDurationSec" to item.audioDurationSec,
                        "videoAspectRatio" to item.videoAspectRatio,
                        "imageResolution" to item.imageResolution,
                        "timestamp" to item.timestamp
                    )
                    db.collection("users")
                        .document(uid)
                        .collection("creations")
                        .document(item.id)
                        .set(data)
                        .await()
                    _syncStatus.value = "Synced: ${item.title.take(15)}"
                } catch (e: Exception) {
                    Log.w(TAG, "Firestore write error: ${e.message}")
                }
            }
        }
    }

    fun deleteCreation(id: String) {
        _creations.value = _creations.value.filterNot { it.id == id }
        scope.launch {
            val uid = _currentUser.value?.uid ?: auth?.currentUser?.uid
            val db = firestore
            if (uid != null && db != null) {
                try {
                    db.collection("users")
                        .document(uid)
                        .collection("creations")
                        .document(id)
                        .delete()
                        .await()
                } catch (e: Exception) {
                    Log.w(TAG, "Firestore delete error: ${e.message}")
                }
            }
        }
    }
}
