package com.example.myapplication

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    val reposts: Int = 0,val isLikedByCurrentUser: Boolean = false,
    val likedBy: List<String> = emptyList(),
    // New repost-related fields
    val isRepost: Boolean = false,
    val originalPostId: String? = null,
    val repostedBy: String? = null,
    val repostedByName: String? = null,
    val repostTimestamp: Date? = null,
    val isRepostedByCurrentUser: Boolean = false
)

class PostViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private val _posts = MutableStateFlow<List<Post>>(emptyList())
    val posts: StateFlow<List<Post>> = _posts

    private val _replies = MutableStateFlow<List<Reply>>(emptyList())
    val replies: StateFlow<List<Reply>> = _replies.asStateFlow()

    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()

    private val _userReplies = MutableStateFlow<List<Pair<Post, Reply>>>(emptyList())
    val userReplies: StateFlow<List<Pair<Post, Reply>>> = _userReplies.asStateFlow()

    private val _userReposts = MutableStateFlow<List<Post>>(emptyList())
    val userReposts: StateFlow<List<Post>> = _userReposts.asStateFlow()




    init {
        try {
            Log.d("PostViewModel", "Firebase services initialized successfully")
            fetchPosts()
        } catch (e: Exception) {
            Log.e("PostViewModel", "Error initializing Firebase services: ${e.message}")
            throw e
        }
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
                        "likedBy" to listOf<String>(),
                        "isRepost" to false,
                        "originalPostId" to null,
                        "repostedBy" to null,
                        "repostedByName" to null,
                        "repostTimestamp" to null,
                        "isRepostedByCurrentUser" to false
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
                        val likedBy = try {
                            (doc.get("likedBy") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                        } catch (e: Exception) {
                            emptyList()
                        }

                        post.copy(
                            id = doc.id,
                            isLikedByCurrentUser = likedBy.contains(currentUser.uid),
                            likedBy = likedBy,
                            likes = likedBy.size
                        )
                    }
                }

                _posts.value = fetchedPosts
            } catch (e: Exception) {
                Log.e("PostViewModel", "Error fetching posts: ${e.message}")
            }
        }
    }

    fun likePost(postId: String, isLiked: Boolean) {
        viewModelScope.launch {
            try {
                val currentUser = auth.currentUser ?: return@launch
                val postRef = firestore.collection("posts").document(postId)

                firestore.runTransaction { transaction ->
                    val snapshot = transaction.get(postRef)
                    val likedBy = try {
                        (snapshot.get("likedBy") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                    } catch (e: Exception) {
                        Log.w("PostViewModel", "Error parsing likedBy field", e)
                        emptyList()
                    }

                    val newLikedBy = if (isLiked) {
                        if (currentUser.uid !in likedBy) likedBy + currentUser.uid else likedBy
                    } else {
                        likedBy - currentUser.uid
                    }

                    transaction.update(postRef, mapOf(
                        "likedBy" to newLikedBy,
                        "likes" to newLikedBy.size
                    ))
                }.await()

                if (isLiked) {
                    createNotification(postId, "like")
                }

                _posts.value = _posts.value.map { post ->
                    if (post.id == postId) {
                        post.copy(
                            likes = if (isLiked) post.likedBy.size + 1 else post.likedBy.size - 1,
                            isLikedByCurrentUser = isLiked,  // Changed property name
                            likedBy = if (isLiked) post.likedBy + currentUser.uid else post.likedBy - currentUser.uid
                        )
                    } else post
                }
            } catch (e: Exception) {
                Log.e("PostViewModel", "Error updating like: ${e.message}")
            }
        }
    }

    @Suppress("unused")
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
                val currentUser = auth.currentUser ?: return@launch
                val postRef = firestore.collection("posts").document(postId)
                val userDoc = firestore.collection("users").document(currentUser.uid).get().await()
                val userName = userDoc.getString("fullName") ?: currentUser.displayName ?: "Unknown User"

                // Create repost document with timestamp
                val repostId = UUID.randomUUID().toString()
                val timestamp = FieldValue.serverTimestamp()
                val repost = hashMapOf(
                    "id" to repostId,
                    "originalPostId" to postId,
                    "repostedByUserId" to currentUser.uid,
                    "repostedByUserName" to userName,
                    "timestamp" to timestamp
                )

                // Use a batch write for atomicity
                firestore.runBatch { batch ->
                    // Add repost to reposts collection
                    batch.set(firestore.collection("reposts").document(repostId), repost)

                    // Increment repost count and update post
                    batch.update(postRef, mapOf(
                        "reposts" to FieldValue.increment(1),
                        "isRepostedByCurrentUser" to true
                    ))

                    // Add to user's reposts collection
                    batch.set(
                        firestore.collection("users")
                            .document(currentUser.uid)
                            .collection("reposts")
                            .document(repostId),
                        repost
                    )
                }.await()

                // Update local state immediately
                _posts.value = _posts.value.map { post ->
                    if (post.id == postId) {
                        post.copy(
                            reposts = post.reposts + 1,
                            isRepostedByCurrentUser = true
                        )
                    } else post
                }

                // Fetch updated reposts
                fetchUserReposts(currentUser.uid)

                // Create notification
                createNotification(postId, "repost")

            } catch (e: Exception) {
                Log.e("PostViewModel", "Error creating repost: ${e.message}", e)
            }
        }
    }

    private suspend fun createNotification(postId: String, type: String) {
        try {
            val currentUser = auth.currentUser ?: return
            val userDoc = firestore.collection("users").document(currentUser.uid).get().await()
            val senderName = userDoc.getString("fullName") ?: return

            val postDoc = firestore.collection("posts").document(postId).get().await()
            val post = postDoc.toObject(Post::class.java) ?: return

            if (post.userId != currentUser.uid) {
                val notificationRef = firestore.collection("notifications").document()
                val notification = hashMapOf(
                    "id" to notificationRef.id,
                    "recipientId" to post.userId,
                    "senderId" to currentUser.uid,
                    "senderName" to senderName,
                    "type" to type,
                    "postId" to postId,
                    "timestamp" to FieldValue.serverTimestamp()
                )
                notificationRef.set(notification).await()
                Log.d("PostViewModel", "Notification created successfully")
            }
        } catch (e: Exception) {
            Log.e("PostViewModel", "Error creating notification: ${e.message}", e)
        }
    }

    fun addReply(postId: String, replyContent: String) {
        viewModelScope.launch {
            try {
                val currentUser = auth.currentUser ?: return@launch

                // Fetch current user data from Realtime Database
                val userSnapshot = database.getReference("users")
                    .child(currentUser.uid)
                    .get()
                    .await()

                // Get user information
                val userName = userSnapshot.child("fullName").getValue(String::class.java)
                    ?: currentUser.displayName
                    ?: "User"
                val userProfileImageUrl = userSnapshot.child("profileImageUrl").getValue(String::class.java)
                    ?: currentUser.photoUrl?.toString()
                    ?: ""

                Log.d("PostViewModel", "Adding reply with user: $userName, profileUrl: $userProfileImageUrl")

                // Create the reply document
                val replyRef = firestore.collection("posts")
                    .document(postId)
                    .collection("comments")
                    .document()

                val reply = hashMapOf(
                    "id" to replyRef.id,
                    "userId" to currentUser.uid,
                    "userName" to userName,
                    "userProfileImageUrl" to userProfileImageUrl,
                    "content" to replyContent,
                    "timestamp" to FieldValue.serverTimestamp(),
                    "likes" to 0,
                    "likedBy" to listOf<String>()
                )

                // Add the reply
                replyRef.set(reply).await()

                // Update the comments count on the post
                val postRef = firestore.collection("posts").document(postId)
                postRef.update("comments", FieldValue.increment(1)).await()

                // Update local state immediately for real-time feedback
                _posts.value = _posts.value.map { post ->
                    if (post.id == postId) {
                        post.copy(comments = post.comments + 1)
                    } else post
                }

                // Update replies immediately in local state
                val newReply = Reply(
                    id = replyRef.id,
                    userId = currentUser.uid,
                    userName = userName,
                    userProfileImageUrl = userProfileImageUrl,
                    content = replyContent,
                    timestamp = Date(),
                    likes = 0,
                    isLikedByCurrentUser = false,
                    likedBy = listOf()
                )

                _replies.value += newReply

                // Create notification for the post owner
                createNotification(postId, "comment")

                Log.d("PostViewModel", "Reply added successfully")
            } catch (e: Exception) {
                Log.e("PostViewModel", "Error adding reply: ${e.message}", e)
                throw e
            }
        }
    }


    fun getPost(postId: String): StateFlow<Post?> {
        val _post = MutableStateFlow<Post?>(null)

        viewModelScope.launch {
            try {
                firestore.collection("posts")
                    .document(postId)
                    .get()
                    .addOnSuccessListener { document ->
                        if (document != null) {
                            _post.value = document.toObject(Post::class.java)?.copy(id = document.id)
                        }
                    }
            } catch (e: Exception) {
                Log.e("PostViewModel", "Error fetching post: ${e.message}")
            }
        }

        return _post.asStateFlow()
    }

    fun likeReply(postId: String, replyId: String, isLiked: Boolean) {
        viewModelScope.launch {
            try {
                val currentUser = auth.currentUser ?: return@launch
                val replyRef = firestore.collection("posts")
                    .document(postId)
                    .collection("comments")
                    .document(replyId)

                firestore.runTransaction { transaction ->
                    val snapshot = transaction.get(replyRef)
                    val likedBy = (snapshot.get("likedBy") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()

                    val newLikedBy = if (isLiked) {
                        if (currentUser.uid !in likedBy) likedBy + currentUser.uid else likedBy
                    } else {
                        likedBy - currentUser.uid
                    }

                    transaction.update(replyRef, mapOf(
                        "likedBy" to newLikedBy,
                        "likes" to newLikedBy.size
                    ))
                }.await()

                // Update local state
                _replies.value = _replies.value.map { reply ->
                    if (reply.id == replyId) {
                        reply.copy(
                            likes = if (isLiked) reply.likedBy.size + 1 else reply.likedBy.size - 1,
                            isLikedByCurrentUser = isLiked,
                            likedBy = if (isLiked) reply.likedBy + currentUser.uid else reply.likedBy - currentUser.uid
                        )
                    } else reply
                }

            } catch (e: Exception) {
                Log.e("PostViewModel", "Error updating reply like: ${e.message}", e)
            }
        }
    }

    fun fetchReplies(postId: String) {
        viewModelScope.launch {
            try {
                val repliesSnapshot = firestore.collection("posts")
                    .document(postId)
                    .collection("comments")
                    .orderBy("timestamp", Query.Direction.ASCENDING)
                    .get()
                    .await()

                val currentUser = auth.currentUser
                val fetchedReplies = mutableListOf<Reply>()

                for (doc in repliesSnapshot.documents) {
                    try {
                        val userId = doc.getString("userId") ?: continue
                        val content = doc.getString("content") ?: ""
                        val timestamp = doc.getTimestamp("timestamp")?.toDate()
                        val likedBy = (doc.get("likedBy") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()

                        // Fetch user data from Realtime Database
                        val userSnapshot = database.getReference("users")
                            .child(userId)
                            .get()
                            .await()

                        val userName = userSnapshot.child("fullName").getValue(String::class.java) ?: "Unknown User"
                        val userProfileImageUrl = userSnapshot.child("profileImageUrl").getValue(String::class.java) ?: ""

                        Log.d("PostViewModel", "Fetching user data for ID: $userId, Found name: $userName")

                        val reply = Reply(
                            id = doc.id,
                            userId = userId,
                            userName = userName,
                            userProfileImageUrl = userProfileImageUrl,
                            content = content,
                            timestamp = timestamp,
                            likes = likedBy.size,
                            isLikedByCurrentUser = currentUser?.uid?.let { uid -> likedBy.contains(uid) } ?: false,
                            likedBy = likedBy
                        )

                        fetchedReplies.add(reply)
                    } catch (e: Exception) {
                        Log.e("PostViewModel", "Error parsing reply: ${e.message}", e)
                    }
                }

                _replies.value = fetchedReplies
                Log.d("PostViewModel", "Fetched ${fetchedReplies.size} replies")
            } catch (e: Exception) {
                Log.e("PostViewModel", "Error fetching replies: ${e.message}", e)
            }
        }
    }
    fun fetchUserReplies(userId: String) {
        viewModelScope.launch {
            try {
                val userRepliesList = mutableListOf<Pair<Post, Reply>>()

                // First, get all posts
                val postsSnapshot = firestore.collection("posts")
                    .get()
                    .await()

                // For each post, check for comments by the user
                for (postDoc in postsSnapshot.documents) {
                    val post = postDoc.toObject(Post::class.java)?.copy(id = postDoc.id) ?: continue

                    val repliesSnapshot = firestore.collection("posts")
                        .document(postDoc.id)
                        .collection("comments")
                        .whereEqualTo("userId", userId)
                        .get()
                        .await()

                    for (replyDoc in repliesSnapshot.documents) {
                        try {
                            val content = replyDoc.getString("content") ?: ""
                            val timestamp = replyDoc.getTimestamp("timestamp")?.toDate()
                            val likedBy = (replyDoc.get("likedBy") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                            val userName = replyDoc.getString("userName") ?: ""
                            val userProfileImageUrl = replyDoc.getString("userProfileImageUrl") ?: ""

                            val reply = Reply(
                                id = replyDoc.id,
                                userId = userId,
                                userName = userName,
                                userProfileImageUrl = userProfileImageUrl,
                                content = content,
                                timestamp = timestamp,
                                likes = likedBy.size,
                                isLikedByCurrentUser = likedBy.contains(auth.currentUser?.uid),
                                likedBy = likedBy
                            )

                            userRepliesList.add(Pair(post, reply))
                        } catch (e: Exception) {
                            Log.e("PostViewModel", "Error parsing reply: ${e.message}")
                        }
                    }
                }

                // Sort replies by timestamp, most recent first
                userRepliesList.sortByDescending { it.second.timestamp }

                _userReplies.value = userRepliesList
                Log.d("PostViewModel", "Fetched ${userRepliesList.size} user replies")
            } catch (e: Exception) {
                Log.e("PostViewModel", "Error fetching user replies: ${e.message}", e)
            }
        }
    }

    fun repostReply(postId: String, replyId: String) {
        viewModelScope.launch {
            try {
                val replyRef = firestore.collection("posts")
                    .document(postId)
                    .collection("comments")
                    .document(replyId)
                replyRef.update("reposts", FieldValue.increment(1))
            } catch (e: Exception) {
                Log.e("PostViewModel", "Error reposting reply: ${e.message}")
            }
        }
    }

    fun deleteReply(postId: String, replyId: String) {
        viewModelScope.launch {
            try {
                firestore.collection("posts")
                    .document(postId)
                    .collection("comments")
                    .document(replyId)
                    .delete()
                fetchReplies(postId)
            } catch (e: Exception) {
                Log.e("PostViewModel", "Error deleting reply: ${e.message}")
            }
        }
    }

    fun undoRepost(postId: String) {
        viewModelScope.launch {
            try {
                val currentUser = auth.currentUser ?: return@launch
                val postRef = firestore.collection("posts").document(postId)

                // Find the repost document
                val repostQuery = firestore.collection("reposts")
                    .whereEqualTo("originalPostId", postId)
                    .whereEqualTo("repostedByUserId", currentUser.uid)
                    .get()
                    .await()

                if (!repostQuery.isEmpty) {
                    val repostDoc = repostQuery.documents[0]

                    firestore.runBatch { batch ->
                        // Delete repost document
                        batch.delete(firestore.collection("reposts").document(repostDoc.id))

                        // Decrement repost count
                        batch.update(postRef, mapOf(
                            "reposts" to FieldValue.increment(-1),
                            "isRepostedByCurrentUser" to false
                        ))

                        // Remove from user's reposts
                        batch.delete(
                            firestore.collection("users")
                                .document(currentUser.uid)
                                .collection("reposts")
                                .document(repostDoc.id)
                        )
                    }.await()

                    // Update local states
                    _posts.value = _posts.value.map { post ->
                        if (post.id == postId) {
                            post.copy(
                                reposts = post.reposts - 1,
                                isRepostedByCurrentUser = false
                            )
                        } else post
                    }

                    // Remove the post from userReposts if we're in the profile view
                    _userReposts.value = _userReposts.value.filter { it.id != postId }

                    // Fetch updated reposts
                    fetchUserReposts(currentUser.uid)
                }

            } catch (e: Exception) {
                Log.e("PostViewModel", "Error removing repost: ${e.message}", e)
            }
        }
    }

    fun fetchUserReposts(userId: String) {
        viewModelScope.launch {
            try {
                val repostsSnapshot = firestore.collection("users")
                    .document(userId)
                    .collection("reposts")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .get()
                    .await()

                val repostedPosts = mutableListOf<Post>()

                for (repostDoc in repostsSnapshot.documents) {
                    val originalPostId = repostDoc.getString("originalPostId") ?: continue
                    val originalPost = firestore.collection("posts")
                        .document(originalPostId)
                        .get()
                        .await()
                        .toObject(Post::class.java)
                        ?.copy(
                            isRepost = true,
                            repostedBy = repostDoc.getString("repostedByUserId"),
                            repostedByName = repostDoc.getString("repostedByUserName"),
                            repostTimestamp = repostDoc.getTimestamp("timestamp")?.toDate()
                        )

                    originalPost?.let { repostedPosts.add(it) }
                }

                _userReposts.value = repostedPosts

            } catch (e: Exception) {
                Log.e("PostViewModel", "Error fetching user reposts: ${e.message}", e)
            }
        }
    }
}
