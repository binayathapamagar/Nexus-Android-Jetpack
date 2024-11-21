package com.example.myapplication.models

data class User(
    val id: String = "",
    val username: String = "",
    val fullName: String = "",
    val profileImageUrl: String? = null,
    val bio: String? = null,
    val followerCount: Int = 0
)
