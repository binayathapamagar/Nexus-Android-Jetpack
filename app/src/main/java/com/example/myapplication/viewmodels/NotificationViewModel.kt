package com.example.myapplication.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.models.Notification
import com.example.myapplication.models.NotificationType
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date

class NotificationViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    val notifications: StateFlow<List<Notification>> = _notifications.asStateFlow()

    private val _hasNewNotifications = MutableStateFlow(false)
    val hasNewNotifications: StateFlow<Boolean> = _hasNewNotifications.asStateFlow()

    private var notificationsListener: ListenerRegistration? = null

    init {
        startListeningForNotifications()
    }

    // Start listening for notifications related to the current user
    fun startListeningForNotifications() {
        val currentUser = auth.currentUser ?: return

        notificationsListener?.remove()

        notificationsListener = firestore.collection("notifications")
            .whereEqualTo("recipientId", currentUser.uid)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    println("Error listening for notifications: $e")
                    return@addSnapshotListener
                }

                viewModelScope.launch {
                    try {
                        val notificationsList = snapshot?.documents?.mapNotNull { doc ->
                            try {
                                doc.toObject(Notification::class.java)?.copy(id = doc.id)
                            } catch (e: Exception) {
                                null
                            }
                        } ?: emptyList()

                        _notifications.emit(notificationsList)
                        _hasNewNotifications.emit(notificationsList.any { !it.read })
                    } catch (e: Exception) {
                        println("Error processing notifications: $e")
                    }
                }
            }
    }

    // Mark all notifications as read
    fun markAllNotificationsAsRead() {
        viewModelScope.launch {
            try {
                val currentUser = auth.currentUser ?: return@launch
                val batch = firestore.batch()

                val unreadNotifications = firestore.collection("notifications")
                    .whereEqualTo("recipientId", currentUser.uid)
                    .whereEqualTo("read", false)
                    .get()
                    .await()

                unreadNotifications.documents.forEach { doc ->
                    batch.update(doc.reference, "read", true)
                }

                batch.commit().await()
                _hasNewNotifications.emit(false)

            } catch (e: Exception) {
                println("Error marking notifications as read: $e")
            }
        }
    }

    // Mark a specific notification as read
    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            try {
                firestore.collection("notifications")
                    .document(notificationId)
                    .update("read", true)
                    .await()

                val updatedNotifications = _notifications.value.map {
                    if (it.id == notificationId) it.copy(read = true) else it
                }
                _notifications.emit(updatedNotifications)
                _hasNewNotifications.emit(updatedNotifications.any { !it.read })
            } catch (e: Exception) {
                println("Error marking notification as read: $e")
            }
        }
    }

    fun saveNotification(recipientID:String, actionType: NotificationType, postId: String, postContent:String) {
        val currentUser  = auth.currentUser
        if (currentUser  == null) {
            println("Error: User not logged in")
            return
        }

        // Reference to the user's data in the Realtime Database
        val userDatabaseRef = FirebaseDatabase.getInstance().getReference("users").child(currentUser .uid)

        viewModelScope.launch {
            try {
                // Fetch user details from the Realtime Database
                val userSnapshot = userDatabaseRef.get().await()
                if (userSnapshot.exists()) {
                    val username = userSnapshot.child("username").getValue(String::class.java) ?: "Unknown User"
                    val profileImageUrl = userSnapshot.child("profileImageUrl").getValue(String::class.java) ?: ""

                    // Create the notification object
                    val notification = Notification(
                        id = "",
                        recipientId = recipientID,
                        senderId = currentUser .uid,
                        senderName = username,
                        senderProfileUrl = profileImageUrl,
                        type = actionType,
                        postId = postId,
                        postContent = postContent,
                        timestamp = Date(),
                        read = false
                    )

                    Log.d("NotificationDebug", "Recipient ID: $recipientID, Action Type: $actionType, Post ID: $postId, Post Content: $postContent")
                    // Save the notification to Firestore
                    firestore.collection("notifications")
                        .add(notification)
                        .await()
                    println("Notification saved for action: $actionType")
                } else {
                    println("User  data not found for UID: ${currentUser .uid}")
                }
            } catch (e: Exception) {
                println("Error saving notification: $e")
            }
        }
    }

    // Clear the notifications listener when the ViewModel is cleared
    override fun onCleared() {
        super.onCleared()
        notificationsListener?.remove()
    }
}
