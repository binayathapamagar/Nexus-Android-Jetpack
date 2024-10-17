package com.example.nexusandroid.navigation

sealed class Routes(val routes:String) {

    object Home : Routes("home")
    object Profile : Routes("profile")
    object Search : Routes("search")
    object AddThreads : Routes("add_threads")
    object Notification : Routes("notification")
    object Splash : Routes("splash")
    object BottomNav : Routes("bottomN_nav")
    object Login : Routes("login")
    object Register : Routes("register")
}