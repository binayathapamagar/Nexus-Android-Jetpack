package com.example.myapplication.screens

// In Activity.kt

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.myapplication.Notification
import com.example.myapplication.NotificationViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun Activity(
    modifier: Modifier = Modifier,
    navController: NavController,
    notificationViewModel: NotificationViewModel = viewModel()
) {
    val notifications by notificationViewModel.notifications.collectAsState()

    LaunchedEffect(Unit) {
        notificationViewModel.fetchNotifications()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "Notifications",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        if (notifications.isEmpty()) {
            Text("No notifications yet")
        } else {
            LazyColumn {
                items(notifications) { notification ->
                    NotificationItem(notification = notification, notificationViewModel = notificationViewModel)
                    Divider()
                }
            }
        }
    }
}

@Composable
fun NotificationItem(notification: Notification, notificationViewModel: NotificationViewModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = notification.senderName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = when (notification.type) {
                    "like" -> "liked your post"
                    "comment" -> "commented on your post"
                    "repost" -> "reposted your post"
                    else -> "interacted with your post"
                },
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = formatTimestamp(notification.timestamp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        if (!notification.read) {
            Button(
                onClick = { notificationViewModel.markNotificationAsRead(notification.id) },
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text("Mark as Read")
            }
        }
    }
}

@Composable
fun AnimatedNotificationIcon(hasNewNotifications: Boolean) {
    Box {
        Icon(
            imageVector = Icons.Filled.Favorite,
            contentDescription = "Activity"
        )
        if (hasNewNotifications) {
            Canvas(
                modifier = Modifier
                    .size(8.dp)
                    .align(Alignment.TopEnd)
            ) {
                drawCircle(
                    color = Color.Red,
                    radius = size.minDimension / 2
                )
            }
        }
    }
}

fun formatTimestamp(timestamp: Date?): String {
    if (timestamp == null) return ""
    val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    return sdf.format(timestamp)
}