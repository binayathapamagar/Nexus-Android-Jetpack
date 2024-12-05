package com.example.myapplication

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.mutableStateOf
import com.example.myapplication.models.Post
import com.example.myapplication.models.Reply
import com.example.myapplication.models.Repost
import com.example.myapplication.models.UserProfile
import com.google.firebase.database.FirebaseDatabase

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await

class UserProfileViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    private val database = FirebaseDatabase.getInstance()
    val isLoading = mutableStateOf(true)

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile

    private val _posts = MutableStateFlow<List<Post>>(emptyList())
    val posts: StateFlow<List<Post>> = _posts

    private val _replies = MutableStateFlow<List<Reply>>(emptyList())
    val replies: StateFlow<List<Reply>> = _replies

    private val _reposts = MutableStateFlow<List<Repost>>(emptyList())
    val reposts: StateFlow<List<Repost>> = _reposts

    fun fetchUserData(userId: String) {
        viewModelScope.launch {
            try {
                isLoading.value = true

                // Fetch user data from Realtime Database
                val userSnapshot = database.getReference("users")
                    .child(userId)
                    .get()
                    .await()

                // Fetch user stats from Firestore
                val statsDoc = firestore.collection("userStats")
                    .document(userId)
                    .get()
                    .await()

                val followersCount = statsDoc.getLong("followersCount")?.toInt() ?: 0
                val followingCount = statsDoc.getLong("followingCount")?.toInt() ?: 0

                val profile = UserProfile(
                    userId = userId,
                    fullName = userSnapshot.child("fullName").getValue(String::class.java) ?: "",
                    username = userSnapshot.child("username").getValue(String::class.java) ?: "",
                    bio = userSnapshot.child("bio").getValue(String::class.java) ?: "",
                    profileImageUrl = userSnapshot.child("profileImageUrl").getValue(String::class.java) ?: "",
                    followersCount = followersCount,
                    followingCount = followingCount
                )

                _userProfile.value = profile

                // Load user's content
                loadUserContent(userId)
            } catch (e: Exception) {
                Log.e("UserProfileViewModel", "Error fetching user data: ${e.message}")
            } finally {
                isLoading.value = false
            }
        }
    }

    private suspend fun loadUserContent(userId: String) {
        try {
            // Fetch posts
            val postsSnapshot = firestore.collection("posts")
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()

            val userPosts = postsSnapshot.documents.mapNotNull { doc ->
                doc.toObject(Post::class.java)?.copy(id = doc.id)
            }
            _posts.value = userPosts

            // Fetch replies
            val allPosts = firestore.collection("posts").get().await()
            val userReplies = mutableListOf<Reply>()

            allPosts.documents.forEach { postDoc ->
                val repliesSnapshot = postDoc.reference.collection("comments")
                    .whereEqualTo("userId", userId)
                    .get()
                    .await()

                repliesSnapshot.documents.forEach { replyDoc ->
                    replyDoc.toObject(Reply::class.java)?.let {
                        userReplies.add(it.copy(id = replyDoc.id))
                    }
                }
            }
            _replies.value = userReplies

            // Fetch reposts
            val repostsSnapshot = firestore.collection("reposts")
                .whereEqualTo("repostedByUserId", userId)
                .get()
                .await()

            val userReposts = repostsSnapshot.documents.mapNotNull { doc ->
                doc.toObject(Repost::class.java)?.copy(id = doc.id)
            }
            _reposts.value = userReposts

        } catch (e: Exception) {
            Log.e("UserProfileViewModel", "Error loading user content: ${e.message}")
        }
    }

    fun refreshUserStats(userId: String) {
        viewModelScope.launch {
            try {
                val statsDoc = firestore.collection("userStats")
                    .document(userId)
                    .get()
                    .await()

                val followersCount = statsDoc.getLong("followersCount")?.toInt() ?: 0
                val followingCount = statsDoc.getLong("followingCount")?.toInt() ?: 0

                _userProfile.value = _userProfile.value?.copy(
                    followersCount = followersCount,
                    followingCount = followingCount
                )
            } catch (e: Exception) {
                Log.e("UserProfileViewModel", "Error refreshing stats: ${e.message}")
            }
        }
    }
}