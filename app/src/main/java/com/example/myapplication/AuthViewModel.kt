package com.example.myapplication

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel : ViewModel() {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
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

    private var userListener: ValueEventListener? = null

    val currentUserId: String?
        get() = auth.currentUser?.uid

    init {
        checkAuthStatus()
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
        val userRef = database.getReference("users").child(user.uid)

        userListener?.let { userRef.removeEventListener(it) }

        userListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    viewModelScope.launch {
                        _profileImageUrl.emit(snapshot.child("profileImageUrl").getValue(String::class.java))
                        _userName.emit(snapshot.child("fullName").getValue(String::class.java))
                        _userHandle.emit(snapshot.child("username").getValue(String::class.java))
                        _userBio.emit(snapshot.child("bio").getValue(String::class.java))
                        _profileLink.emit(snapshot.child("profileLink").getValue(String::class.java))
                        _followerCount.emit(snapshot.child("followerCount").getValue(Int::class.java) ?: 0)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                viewModelScope.launch {
                    _authState.emit(AuthState.Error(error.message))
                }
            }
        }

        userRef.addValueEventListener(userListener!!)
    }

    fun fetchUserProfile() {
        setupUserListener() // This will now set up real-time updates
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
        val userRef = database.getReference("users").child(userId)
        val user = HashMap<String, Any>()
        user["fullName"] = fullName
        user["username"] = username
        user["email"] = email
        user["bio"] = bio
        profileImageUrl?.let { user["profileImageUrl"] = it }

        try {
            userRef.setValue(user).await()
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
                val userRef = database.getReference("users").child(userId)

                // Upload new image if provided
                val newImageUrl: String? = if (newImageUri != null) {
                    uploadProfileImage(userId, newImageUri)
                } else {
                    _profileImageUrl.value
                }

                // Update user profile in Realtime Database
                val updates = hashMapOf<String, Any>(
                    "fullName" to name,
                    "bio" to bio,
                    "profileLink" to profileLink
                )
                newImageUrl?.let { updates["profileImageUrl"] = it }
                userRef.updateChildren(updates).await()

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
                _authState.emit(AuthState.Error(e.message ?: "Failed to update profile"))
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                // Remove the user listener before logging out
                userListener?.let {
                    database.getReference("users").child(currentUserId ?: "").removeEventListener(it)
                    userListener = null
                }
                auth.signOut()
                _authState.emit(AuthState.Unauthenticated)
                clearUserDataAsync()
            } catch (e: Exception) {
                _authState.emit(AuthState.Error("Failed to log out: ${e.message}"))
            }
        }
    }

    private fun clearUserDataAsync() {
        viewModelScope.launch {
            _profileImageUrl.emit(null)
            _userName.emit(null)
            _userHandle.emit(null)
            _userBio.emit(null)
            _profileLink.emit(null)
            _followerCount.emit(null)
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Clean up listener when ViewModel is cleared
        userListener?.let {
            currentUserId?.let { userId ->
                database.getReference("users").child(userId).removeEventListener(it)
            }
        }
    }
}

sealed class AuthState {
    data object Authenticated : AuthState()
    data object Unauthenticated : AuthState()
    data object Loading : AuthState()
    data class Error(val message: String) : AuthState()
}