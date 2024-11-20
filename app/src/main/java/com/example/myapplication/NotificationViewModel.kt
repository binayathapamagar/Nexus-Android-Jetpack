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
    val senderName: String = "",
    val type: String = "",
    val postId: String = "",
    val timestamp: Date? = null,
    val read: Boolean = false
)

class NotificationViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    val notifications: StateFlow<List<Notification>> = _notifications

    private var notificationsListener: ListenerRegistration? = null

    // New state flow to track if there are new notifications
    private val _hasNewNotifications = MutableStateFlow(false)
    val hasNewNotifications: StateFlow<Boolean> = _hasNewNotifications.asStateFlow()

    init {
        startListeningForNotifications()
    }

    fun fetchNotifications() {
        viewModelScope.launch {
            try {
                val currentUser = auth.currentUser ?: return@launch
                val notificationsSnapshot = firestore.collection("notifications")
                    .whereEqualTo("recipientId", currentUser.uid)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .get()
                    .await()

                val fetchedNotifications = notificationsSnapshot.documents.mapNotNull { doc ->
                    doc.toObject(Notification::class.java)?.copy(id = doc.id)
                }

                _notifications.value = fetchedNotifications
            } catch (e: Exception) {
                Log.e("NotificationViewModel", "Error fetching notifications: ${e.message}", e)
            }
        }
    }

    fun startListeningForNotifications() {
        val currentUser = auth.currentUser ?: return
        notificationsListener?.remove()
        notificationsListener = firestore.collection("notifications")
            .whereEqualTo("recipientId", currentUser.uid)
            .orderBy("timestamp", Query.Direction.DESCENDING) // Listen to all notifications ordered by timestamp
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("NotificationViewModel", "Listen failed.", e)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val fetchedNotifications = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Notification::class.java)?.copy(id = doc.id)
                    }
                    _notifications.value = fetchedNotifications
                    // Check if there are any unread notifications
                    _hasNewNotifications.value = fetchedNotifications.any { !it.read }
                }
            }
    }

    fun markNotificationAsRead(notificationId: String) {
        viewModelScope.launch {
            try {
                firestore.collection("notifications").document(notificationId)
                    .update("read", true)
                    .await()
                fetchNotifications()
            } catch (e: Exception) {
                Log.e("NotificationViewModel", "Error marking notification as read: ${e.message}", e)
            }
        }
    }

    fun markAllNotificationsAsRead() {
        viewModelScope.launch {
            try {
                val currentUser = auth.currentUser ?: return@launch
                val notificationsSnapshot = firestore.collection("notifications")
                    .whereEqualTo("recipientId", currentUser.uid)
                    .whereEqualTo("read", false)
                    .get()
                    .await()

                for (doc in notificationsSnapshot.documents) {
                    doc.reference.update("read", true)
                }

                _hasNewNotifications.value = false // Reset the new notification flag
            } catch (e: Exception) {
                Log.e("NotificationViewModel", "Error marking notifications as read: ${e.message}", e)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        notificationsListener?.remove()
    }
}
