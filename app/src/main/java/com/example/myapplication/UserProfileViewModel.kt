package com.example.myapplication

import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.screens.Reply
import com.example.myapplication.screens.Repost
import com.example.myapplication.screens.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class UserProfileViewModel : ViewModel() {

    // Mutable state for UI updates
    val userProfile: MutableState<UserProfile?> = mutableStateOf(null)
    val posts: MutableState<List<Post>> = mutableStateOf(emptyList())
    val replies: MutableState<List<Reply>> = mutableStateOf(emptyList())
    val reposts: MutableState<List<Repost>> = mutableStateOf(emptyList())
    val isLoading = mutableStateOf(true)
    val otherUserReplies: MutableState<List<Pair<Post, Reply>>> = mutableStateOf(emptyList()) // New state for replies by other users

    private val database = FirebaseDatabase.getInstance().reference
    private val firestore = FirebaseFirestore.getInstance()  // Firestore instance for posts, replies, and reposts
    private val storage = FirebaseStorage.getInstance().reference
    private val auth = FirebaseAuth.getInstance()  // Firebase Auth instance for current user

    // Function to fetch real data from Firebase Realtime Database and Storage
    fun fetchUserData(userId: String) {
        viewModelScope.launch {
            try {
                // Fetch User Profile from Realtime Database
                val userRef = database.child("users").child(userId)
                userRef.get().addOnSuccessListener { dataSnapshot ->
                    val userProfileData = dataSnapshot.getValue(UserProfile::class.java)
                    if (userProfileData != null) {
                        userProfile.value = userProfileData

                        // Fetch Profile Image URL from Firebase Storage
                        fetchProfileImage(userProfileData.profileImageUrl)
                    }
                }

                // Fetch User Posts, Replies, and Reposts from Firestore
                fetchUserPosts(userId)
                fetchOtherUserReplies(userId)
                fetchUserReposts(userId)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading.value = false
            }
        }
    }

    // Fetch Profile Image from Firebase Storage
    private fun fetchProfileImage(storagePath: String) {
        val profileImageRef: StorageReference = storage.child("profile_pictures/$storagePath")
        profileImageRef.downloadUrl.addOnSuccessListener { uri ->
            val profileImageUrl = uri.toString()
            // Update UserProfile with the image URL
            userProfile.value?.profileImageUrl = profileImageUrl
        }.addOnFailureListener {
            // Handle failure to fetch profile image (optional)
            println("Failed to load profile image.")
        }
    }

    // Fetch Posts from Firestore
    private fun fetchUserPosts(userId: String) {
        firestore.collection("posts")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val postsList = mutableListOf<Post>()
                for (document in querySnapshot.documents) {
                    val post = document.toObject(Post::class.java)
                    post?.let { postsList.add(it) }
                }
                posts.value = postsList
            }.addOnFailureListener {
                // Handle failure to fetch posts (optional)
                println("Failed to load posts.")
            }
    }

    // Fetch Replies by Other Users
    fun fetchOtherUserReplies(targetUserId: String) {
        viewModelScope.launch {
            try {
                Log.d("UserProfileViewModel", "Fetching replies for user $targetUserId")


                val otherUserRepliesList = mutableListOf<Pair<Post, Reply>>()


                // First, get all posts
                val postsSnapshot = firestore.collection("posts")
                    .get()
                    .await()

                // For each post, check for replies by the target user (other user)
                for (postDoc in postsSnapshot.documents) {
                    val post = postDoc.toObject(Post::class.java)?.copy(id = postDoc.id) ?: continue

                    // Fetch replies for the specific post from the other user
                    val repliesSnapshot = firestore.collection("posts")
                        .document(postDoc.id)
                        .collection("comments")
                        .whereEqualTo("userId", targetUserId)
                        .get()
                        .await()

                    for (replyDoc in repliesSnapshot.documents) {
                        try {
                            val content = replyDoc.getString("content") ?: ""
                            val timestamp = replyDoc.getTimestamp("timestamp")?.toDate()
                            val likedBy = (replyDoc.get("likedBy") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                            val userName = replyDoc.getString("userName") ?: ""
                            val userProfileImageUrl = replyDoc.getString("userProfileImageUrl") ?: ""

                            // Create the Reply object for this reply
                            val reply = Reply(
                                id = replyDoc.id,
                                userId = targetUserId,
                                userName = userName,
                                userProfileImageUrl = userProfileImageUrl,
                                content = content,
                                timestamp = timestamp,
                                likes = likedBy.size,
                                isLikedByCurrentUser = likedBy.contains(auth.currentUser?.uid),
                                likedBy = likedBy
                            )

                            // Add the post and its reply to the list
                            otherUserRepliesList.add(Pair(post, reply))

                            // Log the reply details
                            Log.d("UserProfileViewModel", "Reply fetched: Content = $content, UserName = $userName, Timestamp = $timestamp")


                        } catch (e: Exception) {
                            Log.e("PostViewModel", "Error parsing reply: ${e.message}")
                        }
                    }
                }

                // Sort replies by timestamp, most recent first
                otherUserRepliesList.sortByDescending { it.second.timestamp }

                // Update the state
                otherUserReplies.value = otherUserRepliesList
                Log.d("PostViewModel", "Fetched ${otherUserRepliesList.size} replies by other user")
            } catch (e: Exception) {
                Log.e("PostViewModel", "Error fetching other user replies: ${e.message}", e)
            }
        }
    }

    // Fetch Reposts from Firestore
    private fun fetchUserReposts(userId: String) {
        firestore.collection("reposts")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val repostsList = mutableListOf<Repost>()
                for (document in querySnapshot.documents) {
                    val repost = document.toObject(Repost::class.java)
                    repost?.let { repostsList.add(it) }
                }
                reposts.value = repostsList
            }.addOnFailureListener {
                // Handle failure to fetch reposts (optional)
                println("Failed to load reposts.")
            }
    }
}
