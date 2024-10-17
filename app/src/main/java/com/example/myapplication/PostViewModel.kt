package com.example.myapplication

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.*

data class Post(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userProfileImageUrl: String = "",
    val content: String = "",
    val imageUrls: List<String> = emptyList(),
    val timestamp: Date? = null,
    val likes: Int = 0,
    val comments: Int = 0,
    val reposts: Int = 0,
    val likedByCurrentUser: Boolean = false,
    val likedBy: List<String> = emptyList()
)

class PostViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private var postsListener: ListenerRegistration? = null

    private val _posts = MutableStateFlow<List<Post>>(emptyList())
    val posts: StateFlow<List<Post>> = _posts

    init {
        Log.d("PostViewModel", "Firestore instance: $firestore")
        fetchPosts()
    }

    fun createPost(content: String, userName: String, userProfileImageUrl: String, imageUris: List<Uri>) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            viewModelScope.launch {
                try {
                    val imageUrls = uploadImages(imageUris)
                    val postId = UUID.randomUUID().toString()
                    val timestamp = FieldValue.serverTimestamp()

                    val post = hashMapOf(
                        "id" to postId,
                        "userId" to currentUser.uid,
                        "userName" to userName,
                        "userProfileImageUrl" to userProfileImageUrl,
                        "content" to content,
                        "imageUrls" to imageUrls,
                        "timestamp" to timestamp,
                        "likes" to 0,
                        "comments" to 0,
                        "reposts" to 0,
                        "likedBy" to listOf<String>()  // Initialize as an empty list
                    )

                    firestore.collection("posts").document(postId).set(post)
                        .addOnSuccessListener {
                            Log.d("PostViewModel", "Post successfully written!")
                            fetchPosts()
                        }
                        .addOnFailureListener { e ->
                            Log.w("PostViewModel", "Error writing post", e)
                        }

                    addToGlobalFeed(postId, timestamp)
                } catch (e: Exception) {
                    Log.e("PostViewModel", "Error creating post: ${e.message}", e)
                }
            }
        }
    }

    private suspend fun addToGlobalFeed(postId: String, timestamp: FieldValue) {
        try {
            val globalFeedRef = firestore.collection("globalFeed").document(postId)
            globalFeedRef.set(mapOf("timestamp" to timestamp)).await()
            Log.d("PostViewModel", "Post added to global feed successfully")
        } catch (e: Exception) {
            Log.e("PostViewModel", "Error adding post to global feed: ${e.message}", e)
        }
    }

    private suspend fun uploadImages(imageUris: List<Uri>): List<String> {
        return imageUris.mapNotNull { uri ->
            try {
                val filename = UUID.randomUUID().toString()
                val ref = storage.reference.child("post_images/$filename")
                ref.putFile(uri).await()
                ref.downloadUrl.await().toString()
            } catch (e: Exception) {
                Log.e("PostViewModel", "Error uploading image: ${e.message}", e)
                null
            }
        }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            try {
                firestore.collection("posts").document(postId).delete().await()
                fetchPosts()
            } catch (e: Exception) {
                Log.e("PostViewModel", "Error deleting post: ${e.message}", e)
            }
        }
    }

    fun fetchPosts(limit: Long = 20) {
        viewModelScope.launch {
            try {
                val currentUser = auth.currentUser ?: return@launch
                val postsSnapshot = firestore.collection("posts")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(limit)
                    .get()
                    .await()

                val fetchedPosts = postsSnapshot.documents.mapNotNull { doc ->
                    doc.toObject(Post::class.java)?.let { post ->
                        val likedBy = doc.get("likedBy") as? List<String> ?: emptyList()
                        post.copy(
                            id = doc.id,
                            likedByCurrentUser = likedBy.contains(currentUser.uid),
                            likedBy = likedBy,
                            likes = likedBy.size
                        )
                    }
                }

                _posts.value = fetchedPosts
            } catch (e: Exception) {
                Log.e("PostViewModel", "Error fetching posts: ${e.message}", e)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        postsListener?.remove()
    }

    fun likePost(postId: String, isLiked: Boolean) {
        viewModelScope.launch {
            try {
                val currentUser = auth.currentUser ?: return@launch
                val postRef = firestore.collection("posts").document(postId)

                firestore.runTransaction { transaction ->
                    val snapshot = transaction.get(postRef)
                    val likedBy = snapshot.get("likedBy") as? List<String> ?: listOf()

                    val newLikedBy = if (isLiked) {
                        if (currentUser.uid !in likedBy) likedBy + currentUser.uid else likedBy
                    } else {
                        likedBy - currentUser.uid
                    }

                    transaction.update(postRef, mapOf(
                        "likedBy" to newLikedBy,
                        "likes" to newLikedBy.size  // Set likes to the size of likedBy
                    ))
                }.await()

                if (isLiked) {
                    createNotification(postId, "like")
                }

                // Update the local post object
                val updatedPosts = _posts.value.map { post ->
                    if (post.id == postId) {
                        post.copy(
                            likes = if (isLiked) post.likedBy.size + 1 else post.likedBy.size - 1,
                            likedByCurrentUser = isLiked,
                            likedBy = if (isLiked) post.likedBy + currentUser.uid else post.likedBy - currentUser.uid
                        )
                    } else {
                        post
                    }
                }
                _posts.value = updatedPosts

            } catch (e: Exception) {
                Log.e("PostViewModel", "Error updating like: ${e.message}", e)
            }
        }
    }

    fun commentOnPost(postId: String, comment: String) {
        viewModelScope.launch {
            try {
                val commentRef = firestore.collection("posts").document(postId)
                    .collection("comments").document()
                val commentData = hashMapOf(
                    "userId" to auth.currentUser?.uid,
                    "content" to comment,
                    "timestamp" to FieldValue.serverTimestamp()
                )
                commentRef.set(commentData).await()

                val postRef = firestore.collection("posts").document(postId)
                postRef.update("comments", FieldValue.increment(1)).await()

                createNotification(postId, "comment")
                fetchPosts()
            } catch (e: Exception) {
                Log.e("PostViewModel", "Error commenting on post: ${e.message}", e)
            }
        }
    }

    fun repostPost(postId: String) {
        viewModelScope.launch {
            try {
                val postRef = firestore.collection("posts").document(postId)
                postRef.update("reposts", FieldValue.increment(1)).await()
                createNotification(postId, "repost")
                fetchPosts()
            } catch (e: Exception) {
                Log.e("PostViewModel", "Error reposting: ${e.message}", e)
            }
        }
    }

    private suspend fun createNotification(postId: String, type: String) {
        val currentUser = auth.currentUser ?: return
        val postSnapshot = firestore.collection("posts").document(postId).get().await()
        val post = postSnapshot.toObject(Post::class.java) ?: return

        if (post.userId != currentUser.uid) {
            val notificationRef = firestore.collection("notifications").document()
            val notification = mapOf(
                "id" to notificationRef.id,
                "recipientId" to post.userId,
                "senderId" to currentUser.uid,
                "senderName" to (currentUser.displayName ?: "A user"),
                "type" to type,
                "postId" to postId,
                "timestamp" to FieldValue.serverTimestamp(),
                "read" to false
            )
            notificationRef.set(notification).await()
        }
    }
}
