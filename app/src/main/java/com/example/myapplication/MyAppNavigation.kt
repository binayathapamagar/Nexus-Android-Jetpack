package com.example.myapplication

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
import com.example.myapplication.screens.Home
import com.example.myapplication.screens.Login
import com.example.myapplication.screens.MainContainer
import com.example.myapplication.screens.NewPost
import com.example.myapplication.screens.PostReplies
import com.example.myapplication.screens.Reply
import com.example.myapplication.screens.SearchScreen
import com.example.myapplication.screens.Settings
import com.example.myapplication.screens.SignUp
import com.example.myapplication.screens.Thread

@Composable
fun MyAppNavigation(authViewModel: AuthViewModel) {
    val navController = rememberNavController()
    val authState by authViewModel.authState.collectAsState()
    val postViewModel: PostViewModel = viewModel()

    // Handle authentication-based navigation
    LaunchedEffect(authState) {
        val destination = when (authState) {
            is AuthState.Authenticated -> Routes.MAIN
            is AuthState.Unauthenticated -> Routes.LOGIN
            else -> null
        }
        destination?.let {
            navController.navigate(it) {
                popUpTo(navController.graph.startDestinationId) { inclusive = true }
            }
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

        composable(Routes.SEARCH) {
            SearchScreen(navController = navController, authViewModel = authViewModel)
        }

        composable(Routes.NEW_POST) {
            NewPost(
                navController = navController,
                postViewModel = postViewModel,
                authViewModel = authViewModel
            )
        }

        // Update MyAppNavigation composable to handle parentReplyId parameter
        composable(
            route = Routes.REPLY,
            arguments = listOf(
                navArgument("postId") { type = NavType.StringType },
                navArgument("replyToUsername") { type = NavType.StringType },
                navArgument("parentReplyId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val postId = backStackEntry.arguments?.getString("postId") ?: return@composable
            val replyToUsername = backStackEntry.arguments?.getString("replyToUsername") ?: return@composable
            val parentReplyId = backStackEntry.arguments?.getString("parentReplyId")

            Reply(
                navController = navController,
                postViewModel = postViewModel,
                authViewModel = authViewModel,
                postId = postId,
                replyToUsername = replyToUsername,
                parentReplyId = parentReplyId
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
                PostReplies(
                    navController = navController,
                    postViewModel = postViewModel,
                    postId = it
                )
            }
        }

        composable(
            route = Routes.THREAD,
            arguments = listOf(
                navArgument("postId") {
                    type = NavType.StringType
                    nullable = false
                }
            )
        ) { entry ->
            Thread(
                navController = navController,
                postViewModel = postViewModel,
                postId = entry.arguments?.getString("postId") ?: ""
            )
        }
    }
}
