package com.example.myapplication

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.models.Post
import com.example.myapplication.models.Reply
import com.example.myapplication.models.Repost
import com.example.myapplication.models.UserProfile
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.launch

class UserProfileViewModel : ViewModel() {

    // Mutable state for UI updates
    val userProfile: MutableState<UserProfile?> = mutableStateOf(null)
    val posts: MutableState<List<Post>> = mutableStateOf(emptyList())
    val replies: MutableState<List<Reply>> = mutableStateOf(emptyList())
    val reposts: MutableState<List<Repost>> = mutableStateOf(emptyList())
    val isLoading = mutableStateOf(true)

    private val firestore = FirebaseFirestore.getInstance()  // Firestore instance for all data
    private val storage = FirebaseStorage.getInstance().reference

    // Function to fetch real data from Firestore
    fun fetchUserData(userId: String) {
        viewModelScope.launch {
            try {
                // Fetch User Profile from Firestore
                val userRef = firestore.collection("users").document(userId)
                userRef.get().addOnSuccessListener { documentSnapshot ->
                    val userProfileData = documentSnapshot.toObject(UserProfile::class.java)
                    if (userProfileData != null) {
                        userProfile.value = userProfileData

                        // Fetch Profile Image URL from Firebase Storage
                        fetchProfileImage(userProfileData.profileImageUrl)
                    }
                }

                // Fetch User Posts, Replies, and Reposts from Firestore
                fetchUserPosts(userId)
                fetchUserReplies(userId)
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

    // Fetch Replies from Firestore
    private fun fetchUserReplies(userId: String) {
        firestore.collection("replies")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val repliesList = mutableListOf<Reply>()
                for (document in querySnapshot.documents) {
                    val reply = document.toObject(Reply::class.java)
                    reply?.let { repliesList.add(it) }
                }
                replies.value = repliesList
            }.addOnFailureListener {
                // Handle failure to fetch replies (optional)
                println("Failed to load replies.")
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
