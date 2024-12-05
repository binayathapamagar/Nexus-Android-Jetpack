package com.example.myapplication

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.models.NotificationType
import com.example.myapplication.models.Post
import com.example.myapplication.models.Reply
import com.example.myapplication.models.RepostStatus
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Date
import java.util.UUID


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
                    val threadId = UUID.randomUUID().toString()
                    val timestamp = FieldValue.serverTimestamp()

                    val post = hashMapOf(
                        "id" to threadId,
                        "userId" to currentUser.uid,
                        "content" to content,
                        "imageUrls" to imageUrls,
                        "timestamp" to timestamp,
                        "likes" to 0,
                        "comments" to 0,
                        "reposts" to 0,
                    )

                    firestore.collection("threads").document(threadId).set(post)
                        .addOnSuccessListener {
                            Log.d("PostViewModel", "Post successfully written!")
                            fetchPosts()
                        }
                        .addOnFailureListener { e ->
                            Log.w("PostViewModel", "Error writing post", e)
                        }

                    addToGlobalFeed(threadId, timestamp)
                } catch (e: Exception) {
                    Log.e("PostViewModel", "Error creating post: ${e.message}", e)
                }
            }
        }
    }

    private suspend fun addToGlobalFeed(threadId: String, timestamp: FieldValue) {
        try {
            val globalFeedRef = firestore.collection("globalFeed").document(threadId)
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
                val ref = storage.reference.child("thread_images/$filename")
                ref.putFile(uri).await()
                ref.downloadUrl.await().toString()
            } catch (e: Exception) {
                Log.e("PostViewModel", "Error uploading image: ${e.message}", e)
                null
            }
        }
    }

    fun deletePost(threadId: String) {
        viewModelScope.launch {
            try {
                firestore.collection("posts").document(threadId).delete().await()
                fetchPosts()
            } catch (e: Exception) {
                Log.e("PostViewModel", "Error deleting post: ${e.message}", e)
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun fetchPosts(limit: Long = 10) {
        viewModelScope.launch {
            try {
                val currentUser = auth.currentUser ?: return@launch

                // Fetch threads ordered by timestamp
                val postsSnapshot = firestore.collection("threads")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(limit)
                    .get()
                    .await()

                // Fetch reposts for the current user
                val userRepostsSnapshot = firestore.collection("reposts")
                    .whereEqualTo("userId", currentUser.uid)
                    .get()
                    .await()

                // Fetch likes for the current user
                val userLikesSnapshot = firestore.collection("likes")
                    .whereEqualTo("userId", currentUser.uid)
                    .get()
                    .await()

                // Create a map of repost statuses (threadId -> RepostStatus)
                val userReposts = userRepostsSnapshot.documents.associate { doc ->
                    val threadId = doc.getString("threadId") ?: return@associate null to null
                    threadId to RepostStatus(
                        isReposted = true,
                        repostedBy = doc.getString("userId"),
                        repostedByName = doc.getString("repostedByUserName"),
                        repostTimestamp = doc.getTimestamp("timestamp")?.toDate(),
                        repostId = doc.id
                    )
                }

                // Create a set of threadIds liked by the user
                val likedThreads = userLikesSnapshot.documents.mapNotNull { it.getString("threadId") }.toSet()

                // Map threads to enriched Post objects
                val fetchedPosts = postsSnapshot.documents.mapNotNull { doc ->
                    val postId = doc.id
                    doc.toObject(Post::class.java)?.let { post ->
                        doc.getString("content")?.let {
                            post.copy(
                                id = postId,
                                isLikedByCurrentUser = likedThreads.contains(postId),
                                likedBy = doc.get("likes") as? List<String> ?: emptyList(),
                                likes = (doc.getLong("likes") ?: 0).toInt(),
                                reposts = (doc.getLong("reposts") ?: 0).toInt(),
                                replyCount = (doc.getLong("replyCount") ?: 0).toInt(),
                                repostStatus = userReposts[postId] ?: RepostStatus(),
                                timestamp = doc.getTimestamp("timestamp")?.toDate(),
                                content = it,
                                imageUrls = doc.get("imageUrls") as? List<String> ?: emptyList(),
                                userId = doc.getString("ownerUid")!!
                            )
                        }
                    }
                }

                // Emit the fetched posts
                _posts.value = fetchedPosts
            } catch (e: Exception) {
                Log.e("PostViewModel", "Error fetching posts: ${e.message}")
            }
        }
    }


    fun likePost(threadId: String, isLiked: Boolean) {
        viewModelScope.launch {
            try {
                val currentUser = auth.currentUser ?: return@launch
                val postRef = firestore.collection("threads").document(threadId)

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
                    if (post.id == threadId) {
                        post.copy(
                            likes = if (isLiked) post.likedBy.size + 1 else post.likedBy.size - 1,
                            isLikedByCurrentUser = isLiked,  // Updated property name
                            likedBy = if (isLiked) post.likedBy + currentUser.uid else post.likedBy - currentUser.uid
                        )
                    } else post
                }

                // Send notification for liking the post
                if (isLiked) {
                    createNotification(threadId, NotificationType.LIKE, emptyList())
                }
            } catch (e: Exception) {
                Log.e("PostViewModel", "Error updating like: ${e.message}")
            }
        }
    }

    @Suppress("unused")
    fun commentOnPost(threadId: String, comment: String) {
        viewModelScope.launch {
            try {
                val commentRef = firestore.collection("posts").document(threadId)
                    .collection("comments").document()
                val commentData = hashMapOf(
                    "userId" to auth.currentUser?.uid,
                    "content" to comment,
                    "timestamp" to FieldValue.serverTimestamp()
                )
                commentRef.set(commentData).await()

                val postRef = firestore.collection("posts").document(threadId)
                postRef.update("comments", FieldValue.increment(1)).await()

                // Updated to use NotificationType
                createNotification(threadId, NotificationType.COMMENT, emptyList())
                fetchPosts()
            } catch (e: Exception) {
                Log.e("PostViewModel", "Error commenting on post: ${e.message}", e)
            }
        }
    }

    fun repostPost(threadId: String) {
        viewModelScope.launch {
            try {
                val currentUser = auth.currentUser ?: return@launch

                // Check if post is already reposted by current user
                val existingRepost = firestore.collection("reposts")
                    .whereEqualTo("originalPostId", threadId)
                    .whereEqualTo("repostedByUserId", currentUser.uid)
                    .get()
                    .await()

                if (!existingRepost.isEmpty) {
                    return@launch
                }

                val postRef = firestore.collection("posts").document(threadId)
                val userDoc = firestore.collection("users").document(currentUser.uid).get().await()
                val userName =
                    userDoc.getString("fullName") ?: currentUser.displayName ?: "Unknown User"

                val rethreadId = UUID.randomUUID().toString()
                val timestamp = FieldValue.serverTimestamp()
                val repost = hashMapOf(
                    "id" to rethreadId,
                    "originalPostId" to threadId,
                    "repostedByUserId" to currentUser.uid,
                    "repostedByUserName" to userName,
                    "timestamp" to timestamp
                )

                firestore.runBatch { batch ->
                    batch.set(firestore.collection("reposts").document(rethreadId), repost)
                    batch.update(postRef, "reposts", FieldValue.increment(1))
                    batch.set(
                        firestore.collection("users")
                            .document(currentUser.uid)
                            .collection("reposts")
                            .document(rethreadId),
                        repost
                    )
                }.await()

                _posts.value = _posts.value.map { post ->
                    if (post.id == threadId) {
                        post.copy(
                            reposts = post.reposts + 1,
                            isRepostedByCurrentUser = true
                        )
                    } else post
                }

                // Single notification call using NotificationType
                createNotification(
                    threadId = threadId,
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

    fun addReply(
        threadId: String,
        replyContent: String,
        imageUris: List<Uri> = emptyList(),
        parentReplyId: String? = null
    ) {
        viewModelScope.launch {
            try {
                val currentUser = auth.currentUser ?: return@launch
                val userSnapshot = database.getReference("users")
                    .child(currentUser.uid)
                    .get()
                    .await()

                val userName = userSnapshot.child("fullName").getValue(String::class.java)
                    ?: currentUser.displayName
                    ?: "User"
                val userProfileImageUrl = userSnapshot.child("profileImageUrl").getValue(String::class.java)
                    ?: ""

                // Upload images if any
                val imageUrls = uploadImages(imageUris)

                // Create the reply document
                val replyRef = if (parentReplyId != null) {
                    // For nested replies, store under the parent reply
                    firestore.collection("posts")
                        .document(threadId)
                        .collection("comments")
                        .document(parentReplyId)
                        .collection("replies")
                        .document()
                } else {
                    // For top-level replies
                    firestore.collection("posts")
                        .document(threadId)
                        .collection("comments")
                        .document()
                }

                val reply = hashMapOf(
                    "id" to replyRef.id,
                    "userId" to currentUser.uid,
                    "userName" to userName,
                    "userProfileImageUrl" to userProfileImageUrl,
                    "content" to replyContent,
                    "imageUrls" to imageUrls,
                    "timestamp" to FieldValue.serverTimestamp(),
                    "likes" to 0,
                    "likedBy" to listOf<String>(),
                    "parentReplyId" to parentReplyId,
                    "replies" to 0
                )

                firestore.runTransaction { transaction ->
                    // Add the reply
                    transaction.set(replyRef, reply)

                    // Update reply count on parent
                    if (parentReplyId != null) {
                        val parentReplyRef = firestore.collection("posts")
                            .document(threadId)
                            .collection("comments")
                            .document(parentReplyId)

                        val parentSnapshot = transaction.get(parentReplyRef)
                        val currentReplies = parentSnapshot.getLong("replies") ?: 0
                        transaction.update(parentReplyRef, "replies", currentReplies + 1)
                    }

                    // Update comment count on post
                    val postRef = firestore.collection("posts").document(threadId)
                    transaction.update(postRef, "comments", FieldValue.increment(1))
                }.await()

                // Create notification
                createNotification(
                    threadId = threadId,
                    type = NotificationType.COMMENT,
                    mentionedUserIds = emptyList()
                )

                // Refresh data
                fetchReplies(threadId)
            } catch (e: Exception) {
                Log.e("PostViewModel", "Error adding reply: ${e.message}", e)
                throw e
            }
        }
    }

    // New helper function to fetch and build nested replies
    private suspend fun fetchAndBuildNestedReplies(threadId: String): List<Reply> {
        val repliesSnapshot = firestore.collection("threads")
            .document(threadId)
            .collection("comments")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .get()
            .await()

        val currentUser = auth.currentUser
        val allReplies = mutableListOf<Reply>()

        for (doc in repliesSnapshot.documents) {
            try {
                // Parse main reply
                val reply = parseReplyDocument(doc, currentUser?.uid)

                // Fetch nested replies from the 'replies' subcollection
                val nestedRepliesSnapshot = fetchNestedRepliesForComment(threadId, doc.id).await()

                // Parse nested replies
                val nestedReplies = nestedRepliesSnapshot.documents.mapNotNull { nestedDoc ->
                    try {
                        parseReplyDocument(nestedDoc, currentUser?.uid)
                    } catch (e: Exception) {
                        Log.e("PostViewModel", "Error parsing nested reply: ${e.message}", e)
                        null
                    }
                }

                // Add reply with its nested replies
                allReplies.add(reply.copy(nestedReplies = nestedReplies))
            } catch (e: Exception) {
                Log.e("PostViewModel", "Error parsing reply: ${e.message}", e)
            }
        }

        return buildNestedReplies(allReplies)
    }


    // Helper function to build nested reply structure

    private fun buildNestedReplies(replies: List<Reply>): List<Reply> {
        // Create a map of parent ID to child replies
        val replyChildrenMap = replies.groupBy { it.parentReplyId }

        // Get top-level replies (no parent)
        val rootReplies = replyChildrenMap[null] ?: emptyList()

        // Function to recursively build the reply tree
        fun buildReplyTree(reply: Reply): Reply {
            val childReplies = replyChildrenMap[reply.id] ?: emptyList()
            val nestedReplies = childReplies.map { buildReplyTree(it) }
            return reply.copy(
                nestedReplies = nestedReplies,
                replies = childReplies.size + nestedReplies.sumOf { it.replies }
            )
        }

        return rootReplies.map { buildReplyTree(it) }
    }


    fun addNestedReply(
        threadId: String,
        parentReplyId: String,
        content: String,
        imageUris: List<Uri> = emptyList()
    ) {
        viewModelScope.launch {
            try {
                val currentUser = auth.currentUser ?: return@launch

                // First get all necessary data before starting transaction
                val userSnapshot = database.getReference("users")
                    .child(currentUser.uid)
                    .get()
                    .await()

                val userName = userSnapshot.child("fullName").getValue(String::class.java)
                    ?: currentUser.displayName
                    ?: "User"
                val userProfileImageUrl = userSnapshot.child("profileImageUrl").getValue(String::class.java)
                    ?: ""

                // Upload images
                val imageUrls = uploadImages(imageUris)

                // Get parent reply reference
                val parentReplyRef = firestore.collection("posts")
                    .document(threadId)
                    .collection("replies")
                    .document(parentReplyId)

                val replyRef = parentReplyRef.collection("replies").document()

                // Create reply data
                val reply = hashMapOf(
                    "id" to replyRef.id,
                    "userId" to currentUser.uid,
                    "userName" to userName,
                    "userProfileImageUrl" to userProfileImageUrl,
                    "content" to content,
                    "imageUrls" to imageUrls,
                    "timestamp" to FieldValue.serverTimestamp(),
                    "likes" to 0,
                    "likedBy" to listOf<String>(),
                    "parentReplyId" to parentReplyId,
                    "replies" to 0
                )

                // Perform all reads first
                val parentReplySnapshot = parentReplyRef.get().await()
                val currentReplies = parentReplySnapshot.getLong("replies") ?: 0
                val postRef = firestore.collection("posts").document(threadId)

                // Then perform all writes in a transaction
                firestore.runTransaction { transaction ->
                    // Add the reply
                    transaction.set(replyRef, reply)

                    // Update parent reply count
                    transaction.update(parentReplyRef, "replies", currentReplies + 1)

                    // Update post comments count
                    transaction.update(postRef, "comments", FieldValue.increment(1))
                }.await()

                // Create notification for parent reply owner
                val parentReplyUserId = parentReplySnapshot.getString("userId")
                if (parentReplyUserId != null && parentReplyUserId != currentUser.uid) {
                    createNotification(threadId, NotificationType.COMMENT, listOf(parentReplyUserId))
                }

                // Create Reply object for immediate state update
                val newReply = Reply(
                    id = replyRef.id,
                    userId = currentUser.uid,
                    userName = userName,
                    userProfileImageUrl = userProfileImageUrl,
                    content = content,
                    imageUrls = imageUrls,
                    timestamp = Date(),
                    parentReplyId = parentReplyId,
                    nestedReplies = emptyList()
                )

                // Update local state immediately
                _replies.value = _replies.value.map { existingReply ->
                    if (existingReply.id == parentReplyId) {
                        existingReply.copy(
                            replies = existingReply.replies + 1,
                            nestedReplies = existingReply.nestedReplies + newReply
                        )
                    } else existingReply
                }

                // Wait briefly to ensure Firestore has propagated the changes
                delay(300)

                // Refresh the entire replies tree to ensure consistency
                fetchReplies(threadId)

            } catch (e: Exception) {
                Log.e("PostViewModel", "Error adding nested reply: ${e.message}", e)
                throw e
            }
        }
    }


    private fun fetchNestedRepliesForComment(
        threadId: String,
        commentId: String
    ): Task<QuerySnapshot> {
        return firestore.collection("threads")
            .document(threadId)
            .collection("comments")
            .document(commentId)
            .collection("replies")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .get()
    }

    private fun updateNestedRepliesInState(threadId: String, parentReplyId: String, newReply: Reply) {
        _replies.value = _replies.value.map { reply ->
            if (reply.id == parentReplyId) {
                // Directly update the parent reply
                reply.copy(
                    replies = reply.replies + 1,
                    nestedReplies = reply.nestedReplies + newReply
                )
            } else {
                // Recursively update nested replies
                reply.copy(
                    nestedReplies = updateNestedRepliesRecursively(reply.nestedReplies, parentReplyId, newReply)
                )
            }
        }
    }

    private fun updateNestedRepliesRecursively(
        replies: List<Reply>,
        parentReplyId: String,
        newReply: Reply
    ): List<Reply> {
        return replies.map { reply ->
            if (reply.id == parentReplyId) {
                reply.copy(
                    replies = reply.replies + 1,
                    nestedReplies = reply.nestedReplies + newReply
                )
            } else {
                reply.copy(
                    nestedReplies = updateNestedRepliesRecursively(
                        reply.nestedReplies,
                        parentReplyId,
                        newReply
                    )
                )
            }
        }
    }

    fun getPost(threadId: String): StateFlow<Post?> {
        val postFlow = MutableStateFlow<Post?>(null)

        viewModelScope.launch {
            try {
                val document = withContext(Dispatchers.IO) {
                    firestore.collection("threads")
                        .document(threadId)
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

    fun likeReply(threadId: String, replyId: String, isLiked: Boolean) {
        viewModelScope.launch {
            try {
                val currentUser = auth.currentUser ?: return@launch
                val replyRef = firestore.collection("threads")
                    .document(threadId)
                    firestore.collection("replies")
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

    fun fetchReplies(threadId: String) {
        viewModelScope.launch {
            try {
                val topLevelReplies = mutableListOf<Reply>()
                val currentUser = auth.currentUser

                // Fetch top-level replies
                val repliesSnapshot = firestore.collection("posts")
                    .document(threadId)
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

    fun repostReply(threadId: String, replyId: String) {
        viewModelScope.launch {
            try {
                val currentUser = auth.currentUser ?: return@launch
                val replyRef = firestore.collection("posts")
                    .document(threadId)
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

    fun deleteReply(threadId: String, replyId: String) {
        viewModelScope.launch {
            try {
                // Delete the reply document
                firestore.collection("posts")
                    .document(threadId)
                    .collection("comments")
                    .document(replyId)
                    .delete()
                    .await()

                // Update the comments count on the post
                firestore.collection("posts")
                    .document(threadId)
                    .update("comments", FieldValue.increment(-1))
                    .await()

                // Update local state for replies
                _replies.value = _replies.value.filter { it.id != replyId }

                _posts.value = _posts.value.map { post ->
                    if (post.id == threadId) {
                        post.copy(
                            replyCount = (post.replyCount - 1).coerceAtLeast(0) // Use replyCount for better semantics
                        )
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

    fun undoRepost(threadId: String) {
        viewModelScope.launch {
            try {
                val currentUser = auth.currentUser ?: return@launch
                val postRef = firestore.collection("posts").document(threadId)

                // Find the repost document
                val repostQuery = firestore.collection("reposts")
                    .whereEqualTo("originalPostId", threadId)
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
                        if (post.id == threadId) {
                            post.copy(
                                reposts = (post.reposts - 1).coerceAtLeast(0),
                                isRepostedByCurrentUser = false
                            )
                        } else post
                    }

                    // Update userReposts if we're in the profile view
                    _userReposts.value = _userReposts.value.filter { it.id != threadId }

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
        threadId: String,
        type: NotificationType,
        mentionedUserIds: List<String> = emptyList()
    ) {
        withContext(Dispatchers.IO) {
            try {
                val currentUser = auth.currentUser ?: return@withContext
                val userDoc = firestore.collection("users").document(currentUser.uid).get().await()
                val senderName = userDoc.getString("fullName") ?: return@withContext
                val senderProfileUrl = userDoc.getString("profileImageUrl")

                val postDoc = firestore.collection("posts").document(threadId).get().await()
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
                        "threadId" to threadId,
                        "postContent" to post.content,
                        "imageUrls" to post.imageUrls,
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
                            "threadId" to threadId,
                            "postContent" to post.content,
                            "imageUrls" to post.imageUrls,
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
