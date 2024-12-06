package com.example.myapplication.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.myapplication.AuthViewModel
import com.example.myapplication.PostViewModel
import com.example.myapplication.components.AnimatedNavigationBar
import com.example.myapplication.navigation.Routes
import com.example.myapplication.ui.theme.AppColors
import com.example.myapplication.viewmodels.FollowViewModel
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
    val listState = rememberLazyListState()
    val followViewModel: FollowViewModel = viewModel()

    LaunchedEffect(authViewModel.currentUserId) {
        authViewModel.currentUserId?.let {
            notificationViewModel.startListeningForNotifications()
        }
    }

    Scaffold(
        bottomBar = {
            AnimatedNavigationBar(
                navController = navController,
                listState = listState,
                parentNavController = parentNavController,
                notificationViewModel = notificationViewModel,
                hasNewNotifications = hasNewNotifications,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppColors.Surface)
            )
        },
        containerColor = AppColors.Surface
    ) { innerPadding ->
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
                    authViewModel = authViewModel,
                    modifier = Modifier,
                    followViewModel = FollowViewModel()
                )
            }
            composable("activity") {
                Activity(
                    navController = navController,
                    notificationViewModel = notificationViewModel,
                    followViewModel = followViewModel
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
            composable(
                route = Routes.OTHER_USER,
                arguments = listOf(
                    navArgument("userId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId") ?: return@composable
                OtherUsers(
                    navController = navController,
                    parentNavController = navController,
                    userId = userId
                )
            }
            composable("thread/{postId}") { backStackEntry ->
                val postId = backStackEntry.arguments?.getString("postId") ?: ""
                Thread(
                    navController = navController,
                    postViewModel = parentPostViewModel,
                    postId
                )
            }

        }
    }
}