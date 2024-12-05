package com.example.myapplication

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.models.NotificationType
import com.example.myapplication.models.Post
import com.example.myapplication.models.Reply
import com.example.myapplication.models.RepostStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.*



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

    fun createPost(
        content: String,
        userName: String,
        userProfileImageUrl: String,
        imageUris: List<Uri>
    ) {
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

                // Fetch all reposts for current user first
                val userRepostsSnapshot = firestore.collection("users")
                    .document(currentUser.uid)
                    .collection("reposts")
                    .get()
                    .await()

                val userReposts = userRepostsSnapshot.documents.associate {
                    it.getString("originalPostId") to RepostStatus(
                        isReposted = true,
                        repostedBy = it.getString("repostedByUserId"),
                        repostedByName = it.getString("repostedByUserName"),
                        repostTimestamp = it.getTimestamp("timestamp")?.toDate(),
                        repostId = it.id
                    )
                }

                val fetchedPosts = postsSnapshot.documents.mapNotNull { doc ->
                    doc.toObject(Post::class.java)?.let { post ->
                        val likedBy = (doc.get("likedBy") as? List<*>)?.mapNotNull { it as? String }
                            ?: emptyList()

                        post.copy(
                            id = doc.id,
                            isLikedByCurrentUser = likedBy.contains(currentUser.uid),
                            likedBy = likedBy,
                            likes = likedBy.size,
                            repostStatus = userReposts[doc.id] ?: RepostStatus()
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
                        (snapshot.get("likedBy") as? List<*>)?.mapNotNull { it as? String }
                            ?: emptyList()
                    } catch (e: Exception) {
                        Log.w("PostViewModel", "Error parsing likedBy field", e)
                        emptyList()
                    }

                    val newLikedBy = if (isLiked) {
                        if (currentUser.uid !in likedBy) likedBy + currentUser.uid else likedBy
                    } else {
                        likedBy - currentUser.uid
                    }

                    transaction.update(
                        postRef, mapOf(
                            "likedBy" to newLikedBy,
                            "likes" to newLikedBy.size
                        )
                    )
                }.await()

                // Update local post data
                _posts.value = _posts.value.map { post ->
                    if (post.id == postId) {
                        post.copy(
                            likes = if (isLiked) post.likedBy.size + 1 else post.likedBy.size - 1,
                            isLikedByCurrentUser = isLiked,  // Updated property name
                            likedBy = if (isLiked) post.likedBy + currentUser.uid else post.likedBy - currentUser.uid
                        )
                    } else post
                }

                // Send notification for liking the post
                if (isLiked) {
                    createNotification(postId, NotificationType.LIKE, emptyList())
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

                // Updated to use NotificationType
                createNotification(postId, NotificationType.COMMENT, emptyList())
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

                // Check if post is already reposted by current user
                val existingRepost = firestore.collection("reposts")
                    .whereEqualTo("originalPostId", postId)
                    .whereEqualTo("repostedByUserId", currentUser.uid)
                    .get()
                    .await()

                if (!existingRepost.isEmpty) {
                    return@launch
                }

                val postRef = firestore.collection("posts").document(postId)
                val userDoc = firestore.collection("users").document(currentUser.uid).get().await()
                val userName =
                    userDoc.getString("fullName") ?: currentUser.displayName ?: "Unknown User"

                val repostId = UUID.randomUUID().toString()
                val timestamp = FieldValue.serverTimestamp()
                val repost = hashMapOf(
                    "id" to repostId,
                    "originalPostId" to postId,
                    "repostedByUserId" to currentUser.uid,
                    "repostedByUserName" to userName,
                    "timestamp" to timestamp
                )

                firestore.runBatch { batch ->
                    batch.set(firestore.collection("reposts").document(repostId), repost)
                    batch.update(postRef, "reposts", FieldValue.increment(1))
                    batch.set(
                        firestore.collection("users")
                            .document(currentUser.uid)
                            .collection("reposts")
                            .document(repostId),
                        repost
                    )
                }.await()

                _posts.value = _posts.value.map { post ->
                    if (post.id == postId) {
                        post.copy(
                            reposts = post.reposts + 1,
                            isRepostedByCurrentUser = true
                        )
                    } else post
                }

                // Single notification call using NotificationType
                createNotification(
                    postId = postId,
                    type = NotificationType.REPOST,
                    mentionedUserIds = emptyList()
                )

                fetchUserReposts(currentUser.uid)

            } catch (e: Exception) {
                Log.e("PostViewModel", "Error creating repost: ${e.message}")
            }
        }
    }

    // In PostViewModel:

    fun addReply(postId: String, replyContent: String, parentReplyId: String? = null) {
        viewModelScope.launch {
            try {
                val currentUser = auth.currentUser ?: return@launch

                // Fetch user data first
                val userSnapshot = database.getReference("users")
                    .child(currentUser.uid)
                    .get()
                    .await()

                val userName = userSnapshot.child("fullName").getValue(String::class.java)
                    ?: currentUser.displayName
                    ?: "User"
                val userProfileImageUrl = userSnapshot.child("profileImageUrl").getValue(String::class.java)
                    ?: currentUser.photoUrl?.toString()
                    ?: ""

                val replyRef = if (parentReplyId != null) {
                    firestore.collection("posts")
                        .document(postId)
                        .collection("comments")
                        .document(parentReplyId)
                        .collection("replies")
                        .document()
                } else {
                    firestore.collection("posts")
                        .document(postId)
                        .collection("comments")
                        .document()
                }

                val reply = hashMapOf(
                    "id" to replyRef.id,
                    "userId" to currentUser.uid,
                    "userName" to userName,
                    "userProfileImageUrl" to userProfileImageUrl,
                    "content" to replyContent,
                    "timestamp" to FieldValue.serverTimestamp(),
                    "likes" to 0,
                    "likedBy" to listOf<String>(),
                    "replies" to 0,
                    "reposts" to 0,
                    "imageUrls" to emptyList<String>(),
                    "parentReplyId" to parentReplyId
                )

                firestore.runBatch { batch ->
                    // Add the reply
                    batch.set(replyRef, reply)

                    // Update parent reply count or post comment count
                    if (parentReplyId != null) {
                        val parentRef = firestore.collection("posts")
                            .document(postId)
                            .collection("comments")
                            .document(parentReplyId)
                        batch.update(parentRef, "replies", FieldValue.increment(1))
                    } else {
                        val postRef = firestore.collection("posts").document(postId)
                        batch.update(postRef, "comments", FieldValue.increment(1))
                    }
                }.await()

                // Create notification
                createNotification(postId, NotificationType.COMMENT, emptyList())

                // Refresh local state
                if (parentReplyId != null) {
                    fetchReplies(postId)
                } else {
                    fetchPosts()
                }

            } catch (e: Exception) {
                Log.e("PostViewModel", "Error adding reply: ${e.message}", e)
                throw e
            }
        }
    }


    // Helper function to build nested reply structure


    fun getPost(postId: String): StateFlow<Post?> {
        val postFlow = MutableStateFlow<Post?>(null)

        viewModelScope.launch {
            try {
                val document = withContext(Dispatchers.IO) {
                    firestore.collection("posts")
                        .document(postId)
                        .get()
                        .await()
                }

                val post = document.toObject(Post::class.java)
                postFlow.value = post?.copy(id = document.id)

            } catch (e: Exception) {
                Log.e("PostViewModel", "Error fetching post: ${e.message}")
            }
        }

        return postFlow.asStateFlow()
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
                    val likedBy =
                        (snapshot.get("likedBy") as? List<*>)?.mapNotNull { it as? String }
                            ?: emptyList()

                    val newLikedBy = if (isLiked) {
                        if (currentUser.uid !in likedBy) likedBy + currentUser.uid else likedBy
                    } else {
                        likedBy - currentUser.uid
                    }

                    transaction.update(
                        replyRef, mapOf(
                            "likedBy" to newLikedBy,
                            "likes" to newLikedBy.size
                        )
                    )
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
                val topLevelReplies = mutableListOf<Reply>()
                val currentUser = auth.currentUser

                // Fetch top-level replies
                val repliesSnapshot = firestore.collection("posts")
                    .document(postId)
                    .collection("comments")
                    .orderBy("timestamp", Query.Direction.ASCENDING)
                    .get()
                    .await()

                for (doc in repliesSnapshot.documents) {
                    try {
                        // Parse main reply
                        val reply = parseReplyDocument(doc, currentUser?.uid)

                        // Fetch nested replies
                        val nestedRepliesSnapshot = doc.reference
                            .collection("replies")
                            .orderBy("timestamp", Query.Direction.ASCENDING)
                            .get()
                            .await()

                        val nestedReplies = nestedRepliesSnapshot.documents.mapNotNull { nestedDoc ->
                            try {
                                parseReplyDocument(nestedDoc, currentUser?.uid)
                            } catch (e: Exception) {
                                Log.e("PostViewModel", "Error parsing nested reply: ${e.message}")
                                null
                            }
                        }

                        // Add reply with its nested replies
                        topLevelReplies.add(reply.copy(nestedReplies = nestedReplies))

                    } catch (e: Exception) {
                        Log.e("PostViewModel", "Error processing reply: ${e.message}")
                    }
                }

                _replies.value = topLevelReplies

            } catch (e: Exception) {
                Log.e("PostViewModel", "Error fetching replies: ${e.message}")
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
                            val likedBy =
                                (replyDoc.get("likedBy") as? List<*>)?.mapNotNull { it as? String }
                                    ?: emptyList()
                            val userName = replyDoc.getString("userName") ?: ""
                            val userProfileImageUrl =
                                replyDoc.getString("userProfileImageUrl") ?: ""

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
                val currentUser = auth.currentUser ?: return@launch
                val replyRef = firestore.collection("posts")
                    .document(postId)
                    .collection("comments")
                    .document(replyId)

                // Update the reposts count
                replyRef.update("reposts", FieldValue.increment(1)).await()

                // Update local state
                updateReplyInState(replyId)
            } catch (e: Exception) {
                Log.e("PostViewModel", "Error reposting reply: ${e.message}")
            }
        }
    }

    private fun updateReplyInState(replyId: String) {
        _replies.value = _replies.value.map { reply ->
            if (reply.id == replyId) {
                reply.copy(
                    reposts = reply.reposts + 1
                    // Don't set isRepostedByCurrentUser since it doesn't exist in Reply data class
                )
            } else reply
        }
    }

    fun deleteReply(postId: String, replyId: String) {
        viewModelScope.launch {
            try {
                // Delete the reply document
                firestore.collection("posts")
                    .document(postId)
                    .collection("comments")
                    .document(replyId)
                    .delete()
                    .await()

                // Update the comments count on the post
                firestore.collection("posts")
                    .document(postId)
                    .update("comments", FieldValue.increment(-1))
                    .await()

                // Update local state for replies
                _replies.value = _replies.value.filter { it.id != replyId }

                // Update local state for posts to reflect the new comment count
                _posts.value = _posts.value.map { post ->
                    if (post.id == postId) {
                        post.copy(comments = (post.comments - 1).coerceAtLeast(0))
                    } else {
                        post
                    }
                }

                // Update user replies if we're in the profile view
                _userReplies.value = _userReplies.value.filter { (_, reply) -> reply.id != replyId }

                Log.d("PostViewModel", "Reply deleted successfully")
            } catch (e: Exception) {
                Log.e("PostViewModel", "Error deleting reply: ${e.message}", e)
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
                        batch.update(postRef, "reposts", FieldValue.increment(-1))

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
                                reposts = (post.reposts - 1).coerceAtLeast(0),
                                isRepostedByCurrentUser = false
                            )
                        } else post
                    }

                    // Update userReposts if we're in the profile view
                    _userReposts.value = _userReposts.value.filter { it.id != postId }

                    // Fetch updated reposts
                    fetchUserReposts(currentUser.uid)
                }

            } catch (e: Exception) {
                Log.e("PostViewModel", "Error removing repost: ${e.message}")
            }
        }
    }

    fun fetchUserReposts(userId: String) {
        viewModelScope.launch {
            try {
                auth.currentUser ?: return@launch
                val repostsSnapshot = firestore.collection("users")
                    .document(userId)
                    .collection("reposts")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .get()
                    .await()

                val repostedPosts = mutableListOf<Post>()

                for (repostDoc in repostsSnapshot.documents) {
                    val originalPostId = repostDoc.getString("originalPostId") ?: continue
                    val originalPostDoc = firestore.collection("posts")
                        .document(originalPostId)
                        .get()
                        .await()

                    val post = originalPostDoc.toObject(Post::class.java)?.copy(
                        id = originalPostId,
                        repostStatus = RepostStatus(
                            isReposted = true,
                            repostedBy = repostDoc.getString("repostedByUserId"),
                            repostedByName = repostDoc.getString("repostedByUserName"),
                            repostTimestamp = repostDoc.getTimestamp("timestamp")?.toDate(),
                            repostId = repostDoc.id
                        )
                    )

                    post?.let { repostedPosts.add(it) }
                }

                _userReposts.value = repostedPosts
            } catch (e: Exception) {
                Log.e("PostViewModel", "Error fetching user reposts: ${e.message}")
            }
        }
    }


    private suspend fun parseReplyDocument(doc: DocumentSnapshot, currentUserId: String?): Reply {
        val userId = doc.getString("userId") ?: throw Exception("Missing userId")
        val content = doc.getString("content") ?: ""
        val timestamp = doc.getTimestamp("timestamp")?.toDate()
        val likedBy = (doc.get("likedBy") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
        val imageUrls = (doc.get("imageUrls") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
        val replies = (doc.getLong("replies") ?: 0).toInt()

        // Fetch user data from Realtime Database
        val userSnapshot = database.getReference("users")
            .child(userId)
            .get()
            .await()

        return Reply(
            id = doc.id,
            userId = userId,
            userName = userSnapshot.child("fullName").getValue(String::class.java) ?: "Unknown User",
            userProfileImageUrl = userSnapshot.child("profileImageUrl").getValue(String::class.java) ?: "",
            content = content,
            imageUrls = imageUrls,
            timestamp = timestamp,
            likes = likedBy.size,
            replies = replies,
            isLikedByCurrentUser = currentUserId?.let { uid -> likedBy.contains(uid) } ?: false,
            likedBy = likedBy,
            parentReplyId = doc.getString("parentReplyId"),
            nestedReplies = emptyList() // This will be populated separately for top-level replies
        )
    }

    private suspend fun createNotification(
        postId: String,
        type: NotificationType,
        mentionedUserIds: List<String> = emptyList()
    ) {
        withContext(Dispatchers.IO) {
            try {
                val currentUser = auth.currentUser ?: return@withContext
                val userDoc = firestore.collection("users").document(currentUser.uid).get().await()
                val senderName = userDoc.getString("fullName") ?: return@withContext
                val senderProfileUrl = userDoc.getString("profileImageUrl")

                val postDoc = firestore.collection("posts").document(postId).get().await()
                val post = postDoc.toObject(Post::class.java) ?: return@withContext

                if (post.userId == currentUser.uid) return@withContext

                val batch = firestore.batch()

                // Create notification for the post's owner
                if (type != NotificationType.MENTION && post.userId != currentUser.uid) {
                    val notificationRef = firestore.collection("notifications").document()
                    val notification = hashMapOf(
                        "id" to notificationRef.id,
                        "recipientId" to post.userId,
                        "senderId" to currentUser.uid,
                        "senderName" to senderName,
                        "senderProfileUrl" to senderProfileUrl,
                        "type" to type.name,  // Enum usage
                        "postId" to postId,
                        "postContent" to post.content,
                        "postImageUrl" to (post.imageUrls.firstOrNull() ?: ""),
                        "timestamp" to FieldValue.serverTimestamp(),
                        "read" to false
                    )
                    batch.set(notificationRef, notification)
                }

                // Create mention notifications
                mentionedUserIds.forEach { userId ->
                    if (userId != currentUser.uid) {
                        val mentionNotificationRef = firestore.collection("notifications").document()
                        val mentionNotification = hashMapOf(
                            "id" to mentionNotificationRef.id,
                            "recipientId" to userId,
                            "senderId" to currentUser.uid,
                            "senderName" to senderName,
                            "senderProfileUrl" to senderProfileUrl,
                            "type" to NotificationType.MENTION.name,  // Explicit mention type
                            "postId" to postId,
                            "postContent" to post.content,
                            "postImageUrl" to (post.imageUrls.firstOrNull() ?: ""),
                            "timestamp" to FieldValue.serverTimestamp(),
                            "read" to false
                        )
                        batch.set(mentionNotificationRef, mentionNotification)
                    }
                }

                batch.commit().await()
                Log.d("PostViewModel", "Notification(s) created successfully")
            } catch (e: Exception) {
                Log.e("PostViewModel", "Error creating notification: ${e.message}", e)
            }
        }
    }

}
