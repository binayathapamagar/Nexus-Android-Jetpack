package com.example.myapplication.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
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
    val timestamp: Date = Date(),
    val likes: Int = 0,
    val comments: Int = 0,
    val reposts: Int = 0
)

class PostViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private val _posts = MutableStateFlow<List<Post>>(emptyList())
    val posts: StateFlow<List<Post>> = _posts

    init {
        fetchPosts()
    }

    fun createPost(content: String, userName: String, userProfileImageUrl: String, imageUris: List<Uri>) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            viewModelScope.launch {
                try {
                    val imageUrls = uploadImages(imageUris)
                    val post = Post(
                        id = UUID.randomUUID().toString(),
                        userId = currentUser.uid,
                        userName = userName,
                        userProfileImageUrl = userProfileImageUrl,
                        content = content,
                        imageUrls = imageUrls,
                        timestamp = Date()
                    )

                    firestore.collection("posts").document(post.id).set(post).await()
                    fetchPosts()
                } catch (e: Exception) {
                    println("Error creating post: ${e.message}")
                }
            }
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
                println("Error uploading image: ${e.message}")
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
                println("Error deleting post: ${e.message}")
            }
        }
    }

    fun fetchPosts() {
        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("posts")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .get()
                    .await()
                _posts.value = snapshot.toObjects(Post::class.java)
            } catch (e: Exception) {
                println("Error fetching posts: ${e.message}")
            }
        }
    }
}