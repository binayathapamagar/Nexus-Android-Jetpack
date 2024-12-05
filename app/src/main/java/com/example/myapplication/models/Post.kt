package com.example.myapplication.models


import java.util.Date

data class Post(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userProfileImageUrl: String = "",
    val content: String = "",
    val timestamp: Date? = null,
    val likes: Int = 0,
    val reposts: Int = 0,
    val replyCount: Int = 0,
    val imageUrl: List<String> = listOf(),
    val isLikedByCurrentUser: Boolean = false,
    val repostStatus: RepostStatus? = null // Change from Boolean to RepostStatus
)

data class RepostStatus(
    val isReposted: Boolean = false,
    val repostedBy: String? = null,
    val repostedByName: String? = null,
    val repostTimestamp: Date? = null,
    val repostId: String? = null
)

// Reply model
data class Reply(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userProfileImageUrl: String = "",
    val content: String = "",
    val imageUrl: List<String> = emptyList(),
    val timestamp: Date? = null,
    val likes: Int = 0,
    val replies: Int = 0,
    val reposts: Int = 0,
    val isLikedByCurrentUser: Boolean = false,
    val likedBy: List<String> = emptyList(),
    val parentReplyId: String? = null,
    val nestedReplies: List<Reply> = emptyList()
)

data class Repost(
    val id: String = "",
    val originalPostId: String = "",
    val repostedByUserId: String = "",
    val repostedByUserName: String = "",
    val timestamp: String = "",
    val originalPost: Post // Use the correct Post model reference
)

data class UserProfile(
    val userId: String = "",
    val fullName: String = "",
    val username: String = "",
    val bio: String = "",
    var profileImageUrl: String = "",
    val followersCount: Int = 0
)
