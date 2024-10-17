package com.example.myapplication

import com.example.myapplication.screens.MainContainer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.AuthViewModel
import com.example.myapplication.screens.*

@Composable
fun MyAppNavigation(authViewModel: AuthViewModel) {
    val navController = rememberNavController()
    val authState by authViewModel.authState.collectAsState()

    // Reactive navigation based on auth state
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Authenticated -> navController.navigate("main") {
                popUpTo("login") { inclusive = true }
            }
            is AuthState.Unauthenticated -> navController.navigate("login") {
                popUpTo("main") { inclusive = true }
            }
            is AuthState.Loading -> {
                // Optionally show a loading indicator or do nothing as the default case
            }
            is AuthState.Error -> {
                // Handle the error case, for example by showing a message
                // navController.navigate("error") // Optionally, navigate to an error screen
            }
        }
    }

    NavHost(navController = navController, startDestination = "login") {
        composable("login") { Login(navController = navController, authViewModel = authViewModel) }
        composable("signup") { SignUp(navController = navController, authViewModel = authViewModel) }
        composable("main") { MainContainer(authViewModel = authViewModel) }
        composable("settings") { Settings(navController = navController, authViewModel = authViewModel) }
    }
}