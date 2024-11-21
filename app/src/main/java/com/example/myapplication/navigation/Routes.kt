package com.example.myapplication.navigation

import android.widget.SearchView

object Routes {
    const val LOGIN = "login"
    const val SIGNUP = "signup"
    const val MAIN = "main"
    const val HOME = "home"
    const val SEARCH = "search"
    const val SETTINGS = "settings"
    const val NEW_POST = "new_post"
    const val REPLY = "reply/{postId}/{username}"
    const val POST_REPLIES = "post_replies/{postId}"
    const val THREAD = "thread/{postId}"

    fun createReplyRoute(postId: String, username: String) = "reply/$postId/$username"
    fun createPostRepliesRoute(postId: String) = "post_replies/$postId"
    fun createThreadRoute(postId: String) = "thread/$postId"
}
