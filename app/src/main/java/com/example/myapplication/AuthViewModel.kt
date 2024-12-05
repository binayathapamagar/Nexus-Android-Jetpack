package com.example.myapplication

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel : ViewModel() {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _profileImageUrl = MutableStateFlow<String?>(null)
    val profileImageUrl: StateFlow<String?> = _profileImageUrl.asStateFlow()

    private val _userName = MutableStateFlow<String?>(null)
    val userName: StateFlow<String?> = _userName.asStateFlow()

    private val _userHandle = MutableStateFlow<String?>(null)
    val userHandle: StateFlow<String?> = _userHandle.asStateFlow()

    private val _userBio = MutableStateFlow<String?>(null)
    val userBio: StateFlow<String?> = _userBio.asStateFlow()

    private val _followerCount = MutableStateFlow<Int?>(null)
    val followerCount: StateFlow<Int?> = _followerCount.asStateFlow()

    private val _profileLink = MutableStateFlow<String?>(null)
    val profileLink: StateFlow<String?> = _profileLink.asStateFlow()

    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users.asStateFlow()

    private var userListener: ListenerRegistration? = null

    val currentUserId: String?
        get() = auth.currentUser?.uid

    init {
        checkAuthStatus()
        fetchAllUsers()
    }

    private fun checkAuthStatus() {
        viewModelScope.launch {
            _authState.emit(
                if (auth.currentUser == null) {
                    AuthState.Unauthenticated
                } else {
                    setupUserListener()
                    AuthState.Authenticated
                }
            )
        }
    }

    private fun setupUserListener() {
        val user = auth.currentUser ?: return
        val userRef = firestore.collection("users").document(user.uid)

        userListener?.remove()

        userListener = userRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                viewModelScope.launch {
                    _authState.emit(AuthState.Error(error.message ?: "Error fetching user data"))
                }
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                val userData = snapshot.data
                viewModelScope.launch {
                    _profileImageUrl.emit(userData?.get("profileImageUrl") as? String)
                    _userName.emit(userData?.get("fullName") as? String)
                    _userHandle.emit(userData?.get("username") as? String)
                    _userBio.emit(userData?.get("bio") as? String)
                    _profileLink.emit(userData?.get("profileLink") as? String)
                    _followerCount.emit((userData?.get("followerCount") as? Long)?.toInt() ?: 0)
                }
            }
        }
    }

    private fun fetchAllUsers() {
        viewModelScope.launch {
            firestore.collection("users").get().addOnSuccessListener { snapshot ->
                if (!snapshot.isEmpty) {
                    Log.d("AuthViewModel", "Fetched all users successfully.")
                    val userList = snapshot.documents.mapNotNull { document ->
                        val userId = document.id
                        val fullName = document.getString("fullName") ?: "Unknown Name"
                        val username = document.getString("username") ?: "Unknown Username"
                        val bio = document.getString("bio") ?: "No bio available"
                        val profileImageUrl = document.getString("profileImageUrl")

                        User(
                            id = userId,
                            fullName = fullName,
                            username = username,
                            bio = bio,
                            profileImageUrl = profileImageUrl
                        )
                    }
                    _users.value = userList
                } else {
                    Log.d("AuthViewModel", "No users found.")
                    _users.value = emptyList()
                }
            }.addOnFailureListener { exception ->
                Log.e("AuthViewModel", "Error fetching users: ${exception.message}")
            }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            if (email.isEmpty() || password.isEmpty()) {
                _authState.emit(AuthState.Error("Email or password can't be empty"))
                return@launch
            }
            _authState.emit(AuthState.Loading)
            try {
                auth.signInWithEmailAndPassword(email, password).await()
                _authState.emit(AuthState.Authenticated)
                setupUserListener() // Set up real-time updates after login
            } catch (e: Exception) {
                _authState.emit(AuthState.Error(e.message ?: "Something went wrong"))
            }
        }
    }

    fun signup(
        fullName: String, username: String, email: String, password: String, bio: String, profileImageUri: Uri?
    ) {
        viewModelScope.launch {
            if (fullName.isEmpty() || username.isEmpty() || email.isEmpty() || password.isEmpty()) {
                _authState.emit(AuthState.Error("All fields except bio are required"))
                return@launch
            }
            _authState.emit(AuthState.Loading)
            try {
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                val userId = result.user?.uid
                if (userId != null) {
                    var imageUrl: String? = null
                    if (profileImageUri != null) {
                        imageUrl = uploadProfileImage(userId, profileImageUri)
                    }
                    saveUserToDatabase(userId, fullName, username, email, bio, imageUrl)
                    setupUserListener() // Set up real-time updates after signup
                } else {
                    _authState.emit(AuthState.Error("Failed to get user ID"))
                }
            } catch (e: Exception) {
                _authState.emit(AuthState.Error(e.message ?: "Something went wrong"))
            }
        }
    }

    private suspend fun uploadProfileImage(userId: String, imageUri: Uri): String? {
        return try {
            val ref = storage.reference.child("profile_images/$userId/profile.jpg")
            ref.putFile(imageUri).await()
            ref.downloadUrl.await().toString()
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun saveUserToDatabase(
        userId: String, fullName: String, username: String, email: String, bio: String, profileImageUrl: String?
    ) {
        val userRef = firestore.collection("users").document(userId)
        val user = HashMap<String, Any>()
        user["fullName"] = fullName
        user["username"] = username
        user["email"] = email
        user["bio"] = bio
        profileImageUrl?.let { user["profileImageUrl"] = it }

        try {
            userRef.set(user).await()
            _authState.emit(AuthState.Authenticated)
        } catch (e: Exception) {
            _authState.emit(AuthState.Error("Failed to save user data"))
        }
    }

    fun updateProfile(
        name: String,
        bio: String,
        profileLink: String = "",
        newImageUri: Uri? = null
    ) {
        viewModelScope.launch {
            try {
                val userId = auth.currentUser?.uid ?: return@launch
                val userRef = firestore.collection("users").document(userId)

                // Upload new image if provided
                val newImageUrl: String? = if (newImageUri != null) {
                    uploadProfileImage(userId, newImageUri)
                } else {
                    _profileImageUrl.value
                }

                // Update user profile in Firestore
                val updates = hashMapOf<String, Any>(
                    "fullName" to name,
                    "bio" to bio,
                    "profileLink" to profileLink
                )
                newImageUrl?.let { updates["profileImageUrl"] = it }
                userRef.update(updates).await()

                // Update posts in Firestore
                val firestore = FirebaseFirestore.getInstance()
                val batch = firestore.batch()

                // Get all user's posts
                val postsQuery = firestore.collection("posts")
                    .whereEqualTo("userId", userId)
                    .get()
                    .await()

                // Update each post's userName and profile image
                postsQuery.documents.forEach { doc ->
                    val postRef = firestore.collection("posts").document(doc.id)
                    val postUpdates = hashMapOf<String, Any>("userName" to name)
                    newImageUrl?.let { postUpdates["userProfileImageUrl"] = it }
                    batch.update(postRef, postUpdates)
                }

                // Commit the batch update
                batch.commit().await()

                // Update local state
                _userName.emit(name)
                _userBio.emit(bio)
                _profileLink.emit(profileLink)
                newImageUrl?.let { _profileImageUrl.emit(it) }

            } catch (e: Exception) {
                _authState.emit(AuthState.Error("Failed to update profile"))
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            auth.signOut()
            _authState.emit(AuthState.Unauthenticated)
        }
    }

    sealed class AuthState {
        object Loading : AuthState()
        object Unauthenticated : AuthState()
        object Authenticated : AuthState()
        data class Error(val message: String) : AuthState()
    }
}
