package com.example.myapplication

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.EaseInOutCirc
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.myapplication.navigation.Routes
import com.example.myapplication.screens.EditProfile
import com.example.myapplication.screens.Home
import com.example.myapplication.screens.Login
import com.example.myapplication.screens.MainContainer
import com.example.myapplication.screens.NewPost
import com.example.myapplication.screens.OtherUsers
import com.example.myapplication.screens.PostReplies
import com.example.myapplication.screens.Reply
import com.example.myapplication.screens.SearchScreen
import com.example.myapplication.screens.Settings
import com.example.myapplication.screens.SignUp
import com.example.myapplication.screens.Thread
import com.example.myapplication.viewmodels.FollowViewModel
import com.example.myapplication.viewmodels.NotificationViewModel

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
                authViewModel = authViewModel,
                postViewModel = postViewModel
            )
        }

        composable(Routes.SETTINGS) {
            Settings(navController = navController, authViewModel = authViewModel)
        }

        composable(Routes.SEARCH) {
            SearchScreen(
                navController = navController, authViewModel = authViewModel,
                modifier = Modifier,
                followViewModel = FollowViewModel()
            )
        }

        composable(
            route = Routes.NEW_POST,
            enterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Up,
                    animationSpec = tween(
                        durationMillis = 400,
                        easing = EaseInOutCirc
                    )
                ) + fadeIn(
                    animationSpec = tween(400)
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Down,
                    animationSpec = tween(
                        durationMillis = 400,
                        easing = EaseInOutCirc
                    )
                ) + fadeOut(
                    animationSpec = tween(400)
                )
            },
            popEnterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Up,
                    animationSpec = tween(
                        durationMillis = 400,
                        easing = EaseInOutCirc
                    )
                ) + fadeIn(
                    animationSpec = tween(400)
                )
            },
            popExitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Down,
                    animationSpec = tween(
                        durationMillis = 400,
                        easing = EaseInOutCirc
                    )
                ) + fadeOut(
                    animationSpec = tween(400)
                )
            }
        ) {
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
                parentReplyId = parentReplyId,
                notificationViewModel = NotificationViewModel()
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

        composable(Routes.EDIT_PROFILE) {
            EditProfile(
                navController = navController,
                authViewModel = authViewModel
            )
        }

        composable(Routes.SETTINGS) {
            Settings(
                navController = navController,
                authViewModel = authViewModel
            )
        }
    }
}
