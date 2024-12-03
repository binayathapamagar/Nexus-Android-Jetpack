package com.example.myapplication.screens

import androidx.compose.foundation.background
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
import com.example.myapplication.ui.theme.AppColors

@Composable
fun MainContainer(
    parentNavController: NavController,  // Pass this down
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
        containerColor = AppColors.Surface,
        bottomBar = {
            Surface(
                color = AppColors.Surface,
                tonalElevation = 0.dp
            ) {
                NavigationBar(
                    containerColor = AppColors.Surface,
                    modifier = Modifier.background(AppColors.Surface),
                    tonalElevation = 0.dp
                ) {
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
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = AppColors.Black,
                                unselectedIconColor = AppColors.Gray,
                                indicatorColor = AppColors.Surface
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Surface(
            color = AppColors.Surface,
            tonalElevation = 0.dp,
            modifier = Modifier.fillMaxSize()
        ) {
            NavHost(
                navController = navController,
                startDestination = "home",
                modifier = Modifier.padding(innerPadding)
            ) {
                composable("home") {
                    Home(
                        navController = parentNavController,
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
                        parentNavController = parentNavController,
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
                        parentNavController = parentNavController,  // Pass parent controller
                        userId = userId
                    )
                }
            }
        }
    }
}
