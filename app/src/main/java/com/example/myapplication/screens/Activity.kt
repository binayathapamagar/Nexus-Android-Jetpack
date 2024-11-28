package com.example.myapplication.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.myapplication.R
import com.example.myapplication.models.Notification
import com.example.myapplication.models.NotificationType
import com.example.myapplication.ui.theme.AppColors
import com.example.myapplication.viewmodels.NotificationViewModel
import com.example.myapplication.utils.toRelativeTimeString

//@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Activity(
    modifier: Modifier = Modifier,
    notificationViewModel: NotificationViewModel = viewModel(),
    navController: NavHostController // Add navigation controller
) {
    val notifications by notificationViewModel.notifications.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("All", "Follows", "Replies", "Reposts")

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        Text(
            text = "Activity",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp)
        )

        TabRow(
            selectedTabIndex = selectedTab,
            modifier = Modifier.fillMaxWidth(),
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        if (notifications.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No notifications yet",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(notifications.filter { notification ->
                    when (selectedTab) {
                        1 -> notification.type == NotificationType.FOLLOW
                        2 -> notification.type == NotificationType.COMMENT
                        3 -> notification.type == NotificationType.REPOST
                        else -> true
                    }
                }) { notification ->
                    NotificationItem(
                        notification = notification,
                        onMarkAsRead = { notificationViewModel.markAsRead(notification.id) },
                        onNotificationClick = {
                            // Navigate to post when notification is clicked
                            notification.postId?.let { postId ->
                                navController.navigate("thread/$postId")
                            }
                        }
                    )

                    // Add divider after each notification
                    HorizontalDivider(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        thickness = 1.dp,
                        color = AppColors.Divider
                    )
                }
            }
        }
    }
}

@Composable
fun NotificationItem(
    notification: Notification,
    onMarkAsRead: () -> Unit,
    onNotificationClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (!notification.read)
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                else Color.Transparent
            )
            .clickable(onClick = onNotificationClick) // Make entire notification clickable
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Profile Image
        AsyncImage(
            model = notification.senderProfileUrl,
            contentDescription = "Profile Picture",
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop,
            error = painterResource(id = R.drawable.person)
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Notification Content
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp)
        ) {
            Text(
                text = notification.senderName ?: "Someone",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = when (notification.type) {
                    NotificationType.LIKE -> "liked your post"
                    NotificationType.COMMENT -> "commented on your post"
                    NotificationType.FOLLOW -> "started following you"
                    NotificationType.MENTION -> "mentioned you"
                    NotificationType.REPOST -> "reposted your post"
                },
                style = MaterialTheme.typography.bodyMedium
            )

            notification.postContent?.let { content ->
                Text(
                    text = content,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 2
                )
            }

            Text(
                text = notification.timestamp?.toRelativeTimeString() ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }

        // Add ripple effect for better touch feedback
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
        ) {
            Icon(
                imageVector = when (notification.type) {
                    NotificationType.LIKE -> Icons.Filled.Favorite
                    NotificationType.COMMENT -> Icons.Outlined.ChatBubble
                    NotificationType.FOLLOW -> Icons.Filled.PersonAdd
                    NotificationType.MENTION -> Icons.Filled.AlternateEmail
                    NotificationType.REPOST -> Icons.Filled.Repeat
                },
                contentDescription = null,
                tint = when (notification.type) {
                    NotificationType.LIKE -> Color.Red
                    NotificationType.FOLLOW -> Color.Green
                    else -> MaterialTheme.colorScheme.primary
                },
                modifier = Modifier.size(24.dp)
            )
        }

        if (!notification.read) {
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = onMarkAsRead,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Mark as Read",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun AnimatedNotificationIcon(hasNewNotifications: Boolean) {
    Box {
        Icon(
            imageVector = Icons.Default.Notifications,
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