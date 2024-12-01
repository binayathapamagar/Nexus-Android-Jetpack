package com.example.myapplication.navigation

object Routes {
    const val LOGIN = "login"
    const val SIGNUP = "signup"
    const val MAIN = "main"
    const val HOME = "home"
    const val SEARCH = "search"
    const val SETTINGS = "settings"
    const val NEW_POST = "new_post"
    const val REPLY = "reply/{postId}/{username}/{parentReplyId}"
    const val POST_REPLIES = "post_replies/{postId}"
    const val THREAD = "thread/{postId}"

    fun createReplyRoute(postId: String, username: String, parentReplyId: String? = null) =
        "reply/$postId/$username/${parentReplyId ?: "null"}"

    fun createThreadRoute(postId: String) = "thread/$postId"
}