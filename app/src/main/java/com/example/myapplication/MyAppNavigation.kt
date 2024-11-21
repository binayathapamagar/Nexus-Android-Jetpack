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
import com.example.myapplication.screens.*

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
