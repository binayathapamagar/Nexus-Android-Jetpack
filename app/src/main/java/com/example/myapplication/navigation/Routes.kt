package com.example.myapplication.navigation

object Routes {
    // Basic routes
    const val LOGIN = "login"
    const val SIGNUP = "signup"
    const val MAIN = "main"
    const val HOME = "home"
    const val SETTINGS = "settings"
    const val NEW_POST = "newPost"

    // Routes with arguments
    const val REPLY = "reply/{postId}/{username}"
    const val POST_REPLIES = "post/{postId}/replies"
    const val THREAD = "thread/{postId}"  // New route for thread view

    // Helper functions to create routes with arguments
    fun createReplyRoute(postId: String, username: String) = "reply/$postId/$username"
    fun createPostRepliesRoute(postId: String) = "post/$postId/replies"
    fun createThreadRoute(postId: String) = "thread/$postId"  // New helper function for thread route
}