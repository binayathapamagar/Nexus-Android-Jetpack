package com.example.myapplication.screens

import NewPost
import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.key.Key.Companion.Home
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.myapplication.AuthViewModel
import com.example.myapplication.NotificationViewModel
import com.example.myapplication.PostViewModel
import com.example.myapplication.R
import com.example.myapplication.screens.*

@Composable
fun MainContainer(authViewModel: AuthViewModel) {
    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route ?: "home"
    val profileImageUrl by authViewModel.profileImageUrl.collectAsState()

    val postViewModel: PostViewModel = viewModel()
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
                                "home" -> Icon(Icons.Filled.Home, contentDescription = "Home")
                                "search" -> Icon(Icons.Filled.Search, contentDescription = "Search")
                                "newPost" -> Icon(Icons.Filled.Add, contentDescription = "New Post")
                                "activity" -> AnimatedNotificationIcon(hasNewNotifications)
                                "profile" -> AsyncImage(
                                    model = profileImageUrl,
                                    contentDescription = "Profile",
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop,
                                    error = painterResource(id = R.drawable.person)
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
                                    // Remove this line:
                                    // hasNewNotifications = false
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
                Home(navController = navController, authViewModel = authViewModel, postViewModel = postViewModel)
            }
            composable("search") {
                SearchScreen(navController = navController, authViewModel = authViewModel)
            }
            composable("newPost") {
                NewPost(navController = navController, authViewModel = authViewModel, postViewModel = postViewModel)
            }
            composable("activity") {
                Activity(
                    navController = navController,
                    notificationViewModel = notificationViewModel
                )
                LaunchedEffect(Unit) {
                    notificationViewModel.markAllNotificationsAsRead()
                }
            }
            composable("profile") {
                Profile(
                    navController = navController,
                    authViewModel = authViewModel,
                    postViewModel = postViewModel,
                    userId = authViewModel.currentUserId ?: ""
                )
            }
            composable("settings") {
                Settings(navController = navController, authViewModel = authViewModel)
            }
            // Adding login route here
            composable("login") {
                Login(navController = navController, authViewModel = authViewModel)
            }
        }
    }
}
