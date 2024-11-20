package com.example.myapplication

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date

data class Notification(
    val id: String = "",
    val recipientId: String = "",
    val senderId: String = "",
    val senderName: String? = null,
    val type: String = "",
    val postId: String? = null,
    val timestamp: Date? = null,
    var read: Boolean = false
) {
    constructor() : this("", "", "", null, "", null, null)
}

class NotificationViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    val notifications: StateFlow<List<Notification>> = _notifications.asStateFlow()

    private val _hasNewNotifications = MutableStateFlow(false)
    val hasNewNotifications: StateFlow<Boolean> = _hasNewNotifications.asStateFlow()

    private var notificationsListener: ListenerRegistration? = null

    init {
        try {
            Log.d("NotificationViewModel", "Firebase services initialized successfully")
        } catch (e: Exception) {
            Log.e("NotificationViewModel", "Error initializing Firebase services: ${e.message}")
        }
    }

    fun startListeningForNotifications() {
        val currentUser = auth.currentUser ?: return

        notificationsListener?.remove()

        notificationsListener = firestore.collection("notifications")
            .whereEqualTo("recipientId", currentUser.uid)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("NotificationViewModel", "Listen failed.", e)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    viewModelScope.launch {
                        try {
                            val notificationsList = snapshot.documents.mapNotNull { doc ->
                                try {
                                    doc.toObject(Notification::class.java)?.copy(id = doc.id)
                                } catch (e: Exception) {
                                    Log.e("NotificationViewModel", "Error converting notification: ${e.message}")
                                    null
                                }
                            }
                            _notifications.emit(notificationsList)

                            // Update hasNewNotifications based on unread notifications
                            _hasNewNotifications.emit(notificationsList.any { !it.read })
                        } catch (e: Exception) {
                            Log.e("NotificationViewModel", "Error processing notifications: ${e.message}")
                        }
                    }
                }
            }
    }

    fun markAllNotificationsAsRead() {
        viewModelScope.launch {
            try {
                val currentUser = auth.currentUser ?: return@launch
                val batch = firestore.batch()

                // Get all unread notifications for the current user
                val unreadNotifications = firestore.collection("notifications")
                    .whereEqualTo("recipientId", currentUser.uid)
                    .whereEqualTo("read", false)
                    .get()
                    .await()

                // Mark each notification as read in a batch
                unreadNotifications.documents.forEach { doc ->
                    batch.update(doc.reference, "read", true)
                }

                // Commit the batch
                batch.commit().await()

                // Update local state
                _hasNewNotifications.emit(false)
            } catch (e: Exception) {
                Log.e("NotificationViewModel", "Error marking all notifications as read: ${e.message}")
            }
        }
    }

    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            try {
                firestore.collection("notifications")
                    .document(notificationId)
                    .update("read", true)
                    .await()

                // Update local state if this was the last unread notification
                val updatedNotifications = _notifications.value.map {
                    if (it.id == notificationId) it.copy(read = true) else it
                }
                _notifications.emit(updatedNotifications)
                _hasNewNotifications.emit(updatedNotifications.any { !it.read })
            } catch (e: Exception) {
                Log.e("NotificationViewModel", "Error marking notification as read: ${e.message}")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        notificationsListener?.remove()
    }
}