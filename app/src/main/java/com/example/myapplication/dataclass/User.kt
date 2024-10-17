package com.example.myapplication.dataclass

data class User(
    val id: String = "", // Unique identifier for each user
    val fullName: String = "",
    val username: String = "",
    val followerCount: Int = 0,
    val profileImageUrl: String = ""
)
