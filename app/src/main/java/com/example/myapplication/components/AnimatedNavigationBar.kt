package com.example.myapplication.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.myapplication.ui.theme.AppColors
import com.example.myapplication.viewmodels.NotificationViewModel
import androidx.compose.ui.Modifier
import com.example.myapplication.navigation.Routes

@Composable
fun AnimatedNavigationBar(
    navController: NavController,
    parentNavController: NavController,
    listState: LazyListState,
    notificationViewModel: NotificationViewModel,
    hasNewNotifications: Boolean,
    modifier: Modifier = Modifier
) {
    val isScrollingDown by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0
        }
    }

    val navBarOffset by animateFloatAsState(
        targetValue = if (isScrollingDown) 80f else 0f,
        label = "navBarOffset"
    )

    val currentRoute = navController.currentDestination?.route

    NavigationBar(
        modifier = modifier
            .fillMaxWidth()
            .background(AppColors.Surface)
            .offset(y = navBarOffset.dp),
        containerColor = AppColors.Surface,
        tonalElevation = 0.dp
    ) {
        listOf(
            Pair("home", if (currentRoute == "home") CustomIconType.HOME else CustomIconType.HOME2),
            Pair("search", if (currentRoute == "search") CustomIconType.SEARCH_ON else CustomIconType.SEARCH),
            Pair("newPost", CustomIconType.ADD),
            Pair(
                "activity",
                if (currentRoute == "activity") CustomIconType.NOTIFICATION_ON
                else if (hasNewNotifications) CustomIconType.NOTIFICATION
                else CustomIconType.NOTIFICATION_INACTIVE
            ),
            Pair("profile", if (currentRoute == "profile") CustomIconType.PROFILE_ON else CustomIconType.PROFILE)
        ).forEach { (route, icon) ->
            NavigationBarItem(
                icon = { CustomIcon(icon, modifier = Modifier.size(24.dp)) },
                selected = currentRoute == route,
                onClick = {
                    when (route) {
                        "newPost" -> {
                            parentNavController.navigate(Routes.NEW_POST) {
                                launchSingleTop = true
                            }
                        }
                        else -> {
                            if (currentRoute != route) {
                                navController.navigate(route) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                                if (route == "activity") {
                                    notificationViewModel.markAllNotificationsAsRead()
                                }
                            }
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