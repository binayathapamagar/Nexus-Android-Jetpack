package com.example.myapplication.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.models.NotificationType
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await


class FollowViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _followStatus = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val followStatus: StateFlow<Map<String, Boolean>> = _followStatus

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error


    init {
        refreshFollowStatuses()
    }

    private fun refreshFollowStatuses() {
        viewModelScope.launch {
            try {
                val currentUser = auth.currentUser?.uid ?: return@launch
                val followingSnapshot = firestore.collection("following")
                    .whereEqualTo("followerId", currentUser)
                    .get()
                    .await()

                val newStatus = followingSnapshot.documents.associate {
                    it.getString("followedId")!! to true
                }
                _followStatus.value = newStatus
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    private suspend fun ensureUserStatsExists(userId: String) {
        try {
            val statsRef = firestore.collection("userStats").document(userId)
            val stats = statsRef.get().await()

            if (!stats.exists()) {
                statsRef.set(
                    hashMapOf(
                        "followersCount" to 0,
                        "followingCount" to 0
                    )
                ).await()
            }
        } catch (e: Exception) {
            Log.e("FollowViewModel", "Error ensuring userStats: ${e.message}")
        }
    }

    fun toggleFollow(targetUserId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val currentUser = auth.currentUser ?: return@launch

                val isCurrentlyFollowing = _followStatus.value[targetUserId] ?: false

                if (isCurrentlyFollowing) {
                    unfollowUser(currentUser.uid, targetUserId)
                } else {
                    followUser(currentUser.uid, targetUserId)
                }

                // Update local state immediately for better UX
                _followStatus.value = _followStatus.value + (targetUserId to !isCurrentlyFollowing)

            } catch (e: Exception) {
                Log.e("FollowViewModel", "Toggle follow error: ${e.message}")
                // Revert local state if operation failed
                _followStatus.value = _followStatus.value - targetUserId
                _error.value = "Failed to update follow status"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun followUser(currentUserId: String, targetUserId: String) {
        try {
            // Ensure both users have stats documents
            ensureUserStatsExists(currentUserId)
            ensureUserStatsExists(targetUserId)

            val batch = firestore.batch()

            // Create following relationship
            val followingRef = firestore.collection("following")
                .document("${currentUserId}_${targetUserId}")

            val followData = hashMapOf(
                "followerId" to currentUserId,
                "followedId" to targetUserId,
                "timestamp" to FieldValue.serverTimestamp()
            )

            batch.set(followingRef, followData)

            // Update follower counts
            val targetStatsRef = firestore.collection("userStats").document(targetUserId)
            batch.update(targetStatsRef, "followersCount", FieldValue.increment(1))

            val currentStatsRef = firestore.collection("userStats").document(currentUserId)
            batch.update(currentStatsRef, "followingCount", FieldValue.increment(1))

            batch.commit().await()

            // Update follower count in Realtime Database
            val realtimeDb = FirebaseDatabase.getInstance()
            realtimeDb.getReference("users")
                .child(targetUserId)
                .child("followerCount")
                .runTransaction(object : Transaction.Handler {
                    override fun doTransaction(mutableData: MutableData): Transaction.Result {
                        val currentCount = mutableData.getValue(Int::class.java) ?: 0
                        mutableData.value = currentCount + 1
                        return Transaction.success(mutableData)
                    }

                    override fun onComplete(
                        error: DatabaseError?,
                        committed: Boolean,
                        currentData: DataSnapshot?
                    ) {
                        if (error != null) {
                            Log.e("FollowViewModel", "Error updating follower count: ${error.message}")
                        }
                    }
                })

            // Create follow notification
            val notificationViewModel = NotificationViewModel()
            notificationViewModel.saveNotification(
                recipientID = targetUserId,
                actionType = NotificationType.FOLLOW,
                postId = "",
                postContent = ""
            )

            // Update local state
            _followStatus.value = _followStatus.value + (targetUserId to true)

        } catch (e: Exception) {
            Log.e("FollowViewModel", "Follow error: ${e.message}")
            throw e
        }
    }

    private suspend fun unfollowUser(currentUserId: String, targetUserId: String) {
        try {
            firestore.runTransaction { transaction ->
                // Remove following relationship
                transaction.delete(
                    firestore.collection("following")
                        .document("${currentUserId}_${targetUserId}")
                )

                // Update follower counts
                transaction.update(
                    firestore.collection("userStats").document(targetUserId),
                    "followersCount", FieldValue.increment(-1)
                )
                transaction.update(
                    firestore.collection("userStats").document(currentUserId),
                    "followingCount", FieldValue.increment(-1)
                )
            }.await()

            // Update local state
            _followStatus.value -= targetUserId

        } catch (e: Exception) {
            _error.value = "Failed to unfollow user: ${e.message}"
            throw e
        }
    }

    fun subscribeToFollowUpdates(userId: String) {
        firestore.collection("following")
            .whereEqualTo("followedId", userId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    return@addSnapshotListener
                }

                snapshot?.let {
                    val currentUser = auth.currentUser?.uid
                    val isFollowing = it.documents.any { doc ->
                        doc.getString("followerId") == currentUser
                    }
                    _followStatus.value += (userId to isFollowing)
                }
            }
    }

    private suspend fun initializeUserStatsIfNeeded(userId: String) {
        try {
            val userStatsRef = firestore.collection("userStats").document(userId)
            val userStats = userStatsRef.get().await()

            if (!userStats.exists()) {
                // Initialize user stats document
                val initialStats = hashMapOf(
                    "followersCount" to 0,
                    "followingCount" to 0
                )
                userStatsRef.set(initialStats).await()
            }
        } catch (e: Exception) {
            _error.value = "Failed to initialize user stats: ${e.message}"
            throw e
        }
    }
}