package com.example.nexusandroid.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.nexusandroid.screens.Splash
import com.example.nexusandroid.screens.Home
import com.example.nexusandroid.screens.AddThreads
import com.example.nexusandroid.screens.Search
import com.example.nexusandroid.screens.Notification
import com.example.nexusandroid.screens.Profile
import com.example.nexusandroid.screens.BottomNav
import com.example.nexusandroid.screens.Login
import com.example.nexusandroid.screens.Register



@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Routes.Splash.routes
    ) {
        composable(Routes.Splash.routes) {
            Splash(navController)
        }
        composable(Routes.Home.routes) {
            Home(navController)
        }
        composable(Routes.AddThreads.routes) {
            AddThreads(navController)
        }
        composable(Routes.Search.routes) {
            Search(navController)
        }
        composable(Routes.Notification.routes) {
            Notification()
        }
        composable(Routes.Profile.routes) {
            Profile(navController)
        }
        composable(Routes.BottomNav.routes) {
            BottomNav(navController)
        }
        composable(Routes.Login.routes) {
            Login(navController)
        }
        composable(Routes.Register.routes) {
            Register(navController)
        }

    }
}
