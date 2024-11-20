package com.example.myapplication

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
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

    // Inside AuthViewModel
    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users.asStateFlow()




    val currentUserId: String?
        get() = auth.currentUser?.uid

    init {
        checkAuthStatus()
        fetchUserProfile()
        fetchAllUsers()
    }

    fun checkAuthStatus() {
        viewModelScope.launch {
            if (auth.currentUser == null) {
                _authState.emit(AuthState.Unauthenticated)
            } else {
                _authState.emit(AuthState.Authenticated)
                fetchUserProfile()
            }
        }
    }

    fun fetchUserProfile() {
        viewModelScope.launch {
            val user = auth.currentUser
            if (user != null) {
                val userRef = database.getReference("users").child(user.uid)
                userRef.get().addOnSuccessListener { snapshot ->
                    if (snapshot.exists()) {
                        Log.d("AuthViewModel", "Profile fetched successfully for user: ${user.uid}")
                        _profileImageUrl.value = snapshot.child("profileImageUrl").getValue(String::class.java)
                        _userName.value = snapshot.child("fullName").getValue(String::class.java)
                        _userHandle.value = snapshot.child("username").getValue(String::class.java)
                        _userBio.value = snapshot.child("bio").getValue(String::class.java)
                        _followerCount.value = snapshot.child("followerCount").getValue(Int::class.java) ?: 0
                    }
                }.addOnFailureListener { exception ->
                    Log.e("AuthViewModel", "Error fetching user profile: ${exception.message}")
                }
            }
        }
    }


    fun fetchAllUsers() {
        viewModelScope.launch {
            val userRef = database.getReference("users")
            userRef.get().addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    Log.d("AuthViewModel", "Fetched all users successfully.")
                    val userList = snapshot.children.mapNotNull { dataSnap ->
                        val userId = dataSnap.key ?: return@mapNotNull null
                        val fullName = dataSnap.child("fullName").getValue(String::class.java) ?: "Unknown Name"
                        val username = dataSnap.child("username").getValue(String::class.java) ?: "Unknown Username"
                        val bio = dataSnap.child("bio").getValue(String::class.java) ?: "No bio available"
                        val profileImageUrl = dataSnap.child("profileImageUrl").getValue(String::class.java)

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
                Log.d("AuthViewModel", "Attempting login with email: $email")
                auth.signInWithEmailAndPassword(email, password).await()
                _authState.emit(AuthState.Authenticated)
                fetchUserProfile()
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Login failed: ${e.message}")
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
                Log.d("AuthViewModel", "Attempting signup with email: $email")
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                val userId = result.user?.uid
                if (userId != null) {
                    var imageUrl: String? = null
                    if (profileImageUri != null) {
                        imageUrl = uploadProfileImage(userId, profileImageUri)
                    }
                    saveUserToDatabase(userId, fullName, username, email, bio, imageUrl)
                } else {
                    _authState.emit(AuthState.Error("Failed to get user ID"))
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Signup failed: ${e.message}")
                _authState.emit(AuthState.Error(e.message ?: "Something went wrong"))
            }
        }
    }


    private suspend fun uploadProfileImage(userId: String, imageUri: Uri): String? {
        return try {
            val ref = storage.reference.child("profile_images/$userId/profile.jpg")
            ref.putFile(imageUri).await()
            val downloadUrl = ref.downloadUrl.await()
            downloadUrl.toString()
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
            _profileImageUrl.emit(profileImageUrl)
            fetchUserProfile()
        } catch (e: Exception) {
            _authState.emit(AuthState.Error("Failed to save user data"))
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
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
            _followerCount.emit(null)
        }
    }
}

sealed class AuthState {
    object Authenticated : AuthState()
    object Unauthenticated : AuthState()
    object Loading : AuthState()
    data class Error(val message: String) : AuthState()
}
