package com.example.myapplication

import MainContainer
import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.viewmodel.AuthViewModel
import com.example.myapplication.screens.*
import com.example.myapplication.viewmodel.SearchViewModel
import com.example.myapplication.viewmodel.UserViewModel

//@Composable
//fun MyAppNavigation(authViewModel: AuthViewModel) {
//    val navController = rememberNavController()
//
//    NavHost(navController = navController, startDestination = "login") {
//        composable("login") { Login(navController = navController, authViewModel = authViewModel) }
//        composable("signup") { SignUp(navController = navController, authViewModel = authViewModel) }
//        composable("main") { MainContainer(authViewModel = authViewModel) }
//        composable("settings") { Settings(navController = navController, authViewModel = authViewModel) }
//        composable("search") { Search(
//            navController = navController, viewModel = SearchViewModel(userViewModel = UserViewModel()),
//            authViewModel = authViewModel
//        ) }
//
//    }
//}

@Composable
fun MyAppNavigation(authViewModel: AuthViewModel) {
    val navController = rememberNavController()

    // Obtain ViewModels from Compose’s viewModel() function
    val userViewModel: UserViewModel = viewModel()
    val searchViewModel: SearchViewModel = viewModel(
        factory = SearchViewModel.Factory(userViewModel)
    )

    NavHost(navController = navController, startDestination = "login") {
        composable("login") {
            Login(navController = navController, authViewModel = authViewModel)
        }
        composable("signup") {
            SignUp(navController = navController, authViewModel = authViewModel)
        }
        composable("main") {
            MainContainer(authViewModel = authViewModel)
        }
        composable("settings") {
            Settings(navController = navController, authViewModel = authViewModel)
        }
        composable("search") {
            Search(
                navController = navController,
                viewModel = searchViewModel,
                authViewModel = authViewModel
            )
        }
    }
}