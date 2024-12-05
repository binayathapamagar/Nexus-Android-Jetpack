package com.example.myapplication.models

import java.util.Date

enum class NotificationType {
    LIKE,
    COMMENT,
    FOLLOW,
    MENTION,
    REPOST,
    REPLY;

    // Add a toString method that returns lowercase values
    override fun toString(): String {
        return name.lowercase()
    }

    companion object {
        // Add a fromString method to parse string values
        fun fromString(value: String): NotificationType {
            return try {
                valueOf(value.uppercase())
            } catch (e: IllegalArgumentException) {
                // Default to LIKE if unknown value is encountered
                LIKE
            }
        }
    }
}

data class Notification(
    val id: String = "",
    val recipientId: String = "",
    val senderId: String = "",
    val senderName: String? = null,
    val senderProfileUrl: String? = null,
    val type: NotificationType = NotificationType.LIKE,
    val postId: String? = null,
    val postContent: String? = null,
    val timestamp: Date? = null,
    val read: Boolean = false
) {
    companion object {
        fun fromMap(map: Map<String, Any>, id: String): Notification {
            return Notification(
                id = id,
                recipientId = map["recipientId"] as? String ?: "",
                senderId = map["senderId"] as? String ?: "",
                senderName = map["senderName"] as? String,
                senderProfileUrl = map["senderProfileUrl"] as? String,
                type = NotificationType.fromString(map["type"] as? String ?: "like"),
                postId = map["postId"] as? String,
                postContent = map["postContent"] as? String,
                timestamp = (map["timestamp"] as? com.google.firebase.Timestamp)?.toDate(),
                read = map["read"] as? Boolean ?: false
            )
        }
    }
}