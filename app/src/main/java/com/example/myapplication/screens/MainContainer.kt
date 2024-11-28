package com.example.myapplication.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.myapplication.AuthViewModel
import com.example.myapplication.PostViewModel
import com.example.myapplication.components.CustomIcon
import com.example.myapplication.components.CustomIconType
import com.example.myapplication.viewmodels.NotificationViewModel

@Composable
fun MainContainer(
    parentNavController: NavController,
    authViewModel: AuthViewModel,
    parentPostViewModel: PostViewModel
) {
    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route ?: "home"
    val profileImageUrl by authViewModel.profileImageUrl.collectAsState()

    val notificationViewModel: NotificationViewModel = viewModel()

    val hasNewNotifications by notificationViewModel.hasNewNotifications.collectAsState()

    LaunchedEffect(authViewModel.currentUserId) {
        if (authViewModel.currentUserId != null) {
            notificationViewModel.startListeningForNotifications()
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                listOf("home", "search", "newPost", "activity", "profile").forEach { screen ->
                    NavigationBarItem(
                        icon = {
                            when (screen) {
                                "home" -> CustomIcon(CustomIconType.HOME)
                                "search" -> CustomIcon(CustomIconType.SEARCH)
                                "newPost" -> CustomIcon(CustomIconType.ADD)
                                "activity" -> {
                                    if (hasNewNotifications) {
                                        CustomIcon(CustomIconType.NOTIFICATION)
                                    } else {
                                        CustomIcon(CustomIconType.NOTIFICATION_INACTIVE)
                                    }
                                }
                                "profile" -> CustomIcon(
                                    iconType = CustomIconType.PROFILE,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        },

                        selected = currentRoute == screen,
                        onClick = {
                            if (currentRoute != screen) {
                                navController.navigate(screen) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                                if (screen == "activity") {
                                    notificationViewModel.markAllNotificationsAsRead()
                                }
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("home") {
                Home(
                    navController = parentNavController, // Use parent NavController for reply navigation
                    authViewModel = authViewModel,
                    postViewModel = parentPostViewModel
                )
            }
            composable("search") {
                SearchScreen(
                    navController = navController,
                    authViewModel = authViewModel
                )
            }
            composable("newPost") {
                NewPost(
                    navController = navController,
                    authViewModel = authViewModel,
                    postViewModel = parentPostViewModel
                )
            }
            composable("activity") {
                Activity(
                    navController = navController,
                    notificationViewModel = viewModel()
                )
                LaunchedEffect(Unit) {
                    notificationViewModel.markAllNotificationsAsRead()
                }
            }
            composable("profile") {
                Profile(
                    navController = navController,
                    parentNavController = parentNavController, // Add this
                    authViewModel = authViewModel,
                    postViewModel = parentPostViewModel,
                    userId = authViewModel.currentUserId ?: ""
                )
            }
            composable("settings") {
                Settings(
                    navController = parentNavController,
                    authViewModel = authViewModel
                )
            }
            composable("edit_profile") {
                EditProfile(
                    navController = navController,
                    authViewModel = authViewModel
                )
            }
            composable("otherUsers/{userId}") { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId") ?: ""
                OtherUsers(
                    navController = navController,
                    userId = userId,
                )
            }
        }
    }
}