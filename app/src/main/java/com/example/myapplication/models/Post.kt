package com.example.myapplication.models


import java.util.Date

data class Post(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userProfileImageUrl: String = "",
    val content: String = "",
    val imageUrls: List<String> = emptyList(),
    val timestamp: com.google.firebase.Timestamp? = null,
    val likes: Int = 0,
    val comments: Int = 0,
    val reposts: Int = 0,
    val likedBy: List<String> = emptyList(),
    val isRepost: Boolean = false,
    val originalPostId: String? = null,
    val repostedBy: String? = null,
    val repostedByName: String? = null,
    val repostTimestamp: com.google.firebase.Timestamp? = null,
    val isLikedByCurrentUser: Boolean = false,
    val isRepostedByCurrentUser: Boolean = false,
    val repostStatus: RepostStatus = RepostStatus()
) {
    // Remove unused computed properties and keep only necessary ones
    val hasImages: Boolean
        get() = imageUrls.isNotEmpty()
}

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
    val imageUrls: List<String> = emptyList(),
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
    val followersCount: Int = 0,
    val followingCount: Int = 0,  // Add this field
    var isFollowedByCurrentUser: Boolean = false  // Add this field
)

// Represents a following relationship between users
data class FollowRelation(
    val id: String = "",
    val followerId: String = "",  // User who is following
    val followedId: String = "",  // User being followed
    val timestamp: Date = Date(),
    val followerName: String = "",
    val followerProfileUrl: String = "",
    val followedName: String = "",
    val followedProfileUrl: String = ""
)

// For real-time following status updates
data class FollowStatus(
    val isFollowing: Boolean = false,
    val followerId: String = "",
    val followedId: String = "",
    val timestamp: Date = Date()
)


data class User(
    val id: String = "",
    val username: String = "",
    val fullName: String = "",
    val profileImageUrl: String? = null,
    val bio: String? = null,
    val followerCount: Int = 0
)
