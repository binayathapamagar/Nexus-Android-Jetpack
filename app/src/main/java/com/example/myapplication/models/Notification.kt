package com.example.myapplication.models

import java.util.Date

enum class NotificationType {
    LIKE,
    COMMENT,
    FOLLOW,
    MENTION,
    REPOST,
    REPLY
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
    val postImageUrl: String? = null,
    val timestamp: Date? = null,
    var read: Boolean = false
) {
    constructor() : this("") // Required for Firebase
}