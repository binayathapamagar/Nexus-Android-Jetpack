package com.example.myapplication

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.myapplication.navigation.Routes
import com.example.myapplication.screens.*

@Composable
fun MyAppNavigation(authViewModel: AuthViewModel) {
    val navController = rememberNavController()
    val authState by authViewModel.authState.collectAsState()
    val postViewModel: PostViewModel = viewModel()

    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Authenticated -> {
                try {
                    navController.navigate(Routes.MAIN) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                } catch (e: Exception) {
                    Log.e("Navigation", "Error navigating to main: ${e.message}")
                }
            }
            is AuthState.Unauthenticated -> {
                try {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                    }
                } catch (e: Exception) {
                    Log.e("Navigation", "Error navigating to login: ${e.message}")
                }
            }
            else -> {}
        }
    }

    NavHost(
        navController = navController,
        startDestination = Routes.LOGIN
    ) {
        composable(Routes.LOGIN) {
            Login(navController = navController, authViewModel = authViewModel)
        }

        composable(Routes.SIGNUP) {
            SignUp(navController = navController, authViewModel = authViewModel)
        }

        composable(Routes.MAIN) {
            MainContainer(
                parentNavController = navController,
                authViewModel = authViewModel,
                parentPostViewModel = postViewModel
            )
        }

        composable(Routes.HOME) {
            Home(
                navController = navController,
                postViewModel = postViewModel,
                authViewModel = authViewModel
            )
        }

        composable(Routes.SETTINGS) {
            Settings(navController = navController, authViewModel = authViewModel)
        }

        composable(Routes.NEW_POST) {
            NewPost(
                navController = navController,
                postViewModel = postViewModel,
                authViewModel = authViewModel
            )
        }

        composable(
            route = Routes.REPLY,
            arguments = listOf(
                navArgument("postId") { type = NavType.StringType },
                navArgument("username") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            Reply(
                navController = navController,
                postViewModel = postViewModel,
                authViewModel = authViewModel,
                postId = backStackEntry.arguments?.getString("postId") ?: "",
                replyToUsername = backStackEntry.arguments?.getString("username") ?: ""
            )
        }

        composable(
            route = Routes.POST_REPLIES,
            arguments = listOf(
                navArgument("postId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val postId = backStackEntry.arguments?.getString("postId")
            postId?.let {
                val post by postViewModel.getPost(it).collectAsState(initial = null)
                val replies by postViewModel.replies.collectAsState()

                LaunchedEffect(postId) {
                    postViewModel.fetchReplies(postId)
                }

                post?.let { p ->
                    PostWithReplies(
                        post = p,
                        replies = replies,
                        postViewModel = postViewModel,
                        navController = navController
                    )
                }
            }
        }

        // New Thread route
        composable(
            route = Routes.THREAD,
            arguments = listOf(
                navArgument("postId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            ThreadScreen(
                navController = navController,
                postViewModel = postViewModel,
                postId = backStackEntry.arguments?.getString("postId") ?: ""
            )
        }
    }
}