package com.example.myapplication.navigation

object Routes {
    const val LOGIN = "login"
    const val SIGNUP = "signup"
    const val MAIN = "main"
    const val HOME = "home"
    const val SEARCH = "search"
    const val SETTINGS = "settings"
    const val NEW_POST = "new_post"
    const val REPLY = "reply/{postId}/{replyToUsername}?parentReplyId={parentReplyId}"
    const val POST_REPLIES = "post_replies/{postId}"
    const val THREAD = "thread/{postId}"
    const val OTHER_USER = "otherUsers/{userId}"
    const val EDIT_PROFILE = "edit_profile"


    fun createReplyRoute(
        postId: String,
        replyToUsername: String,
        parentReplyId: String? = null
    ): String {
        return if (parentReplyId != null) {
            "reply/$postId/$replyToUsername?parentReplyId=$parentReplyId"
        } else {
            "reply/$postId/$replyToUsername"
        }
    }

    fun createThreadRoute(postId: String) = "thread/$postId"
    fun createOtherUserRoute(userId: String) = "otherUsers/$userId"

}

