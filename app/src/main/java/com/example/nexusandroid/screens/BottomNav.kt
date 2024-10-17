package com.example.nexusandroid.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.nexusandroid.model.BottomNavItem
import com.example.nexusandroid.navigation.Routes

@Composable
fun BottomNav(navController: NavHostController) {
    val navController1= rememberNavController()

    Scaffold(
        bottomBar = { MyBottomBar(navController) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.Home.routes,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(route = Routes.Home.routes) {
                Home(navController)
            }
            composable(route = Routes.AddThreads.routes) {
                AddThreads(navController1)
            }
            composable(route = Routes.Search.routes) {
                Search(navController1)
            }
            composable(route = Routes.Notification.routes) {
                Notification()
            }
            composable(route = Routes.Profile.routes) {
                Profile(navController1)
            }
        }
    }
}


@Composable
fun MyBottomBar(navController: NavHostController) {

    val list: List<BottomNavItem> = listOf(
        BottomNavItem(
            title = "Home",
            route = Routes.Home.routes,
            icon = Icons.Rounded.Home
        ),
        BottomNavItem(
            title = "Search",
            route = Routes.Search.routes,
            icon = Icons.Rounded.Search
        ),
        BottomNavItem(
            title = "Add Threads",
            route = Routes.AddThreads.routes,
            icon = Icons.Rounded.Add
        ),
        BottomNavItem(
            title = "Notification",
            route = Routes.Notification.routes,
            icon = Icons.Rounded.Notifications
        ),
        BottomNavItem(
            title = "Profile",
            route = Routes.Profile.routes,
            icon = Icons.Rounded.Person
        )
    )

    val backStackEntry by navController.currentBackStackEntryAsState()

    BottomAppBar {
        list.forEach { item ->
            val selected = item.route == backStackEntry?.destination?.route
            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                    }
                },
                icon = {
                    Icon(imageVector = item.icon, contentDescription = item.title)
                }
            )
        }
    }
}