package com.example.myapplication.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.example.myapplication.models.Notification
import com.google.firebase.firestore.ListenerRegistration

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

    override fun onCleared() {
        super.onCleared()
        notificationsListener?.remove()
    }
}