package com.example.myapplication.screens

import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.outlined.ChatBubble
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.myapplication.R
import com.example.myapplication.models.Notification
import com.example.myapplication.models.NotificationType
import com.example.myapplication.navigation.Routes
import com.example.myapplication.ui.theme.AppColors
import com.example.myapplication.utils.toRelativeTimeString
import com.example.myapplication.viewmodels.NotificationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Activity(
    modifier: Modifier = Modifier,
    notificationViewModel: NotificationViewModel = viewModel(),
    navController: NavController
) {
    val notifications by notificationViewModel.notifications.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("All", "Follows", "Replies", "Reposts", "Mentions")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.White)
    ) {
        Text(
            text = "Activity",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp)
        )

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentPadding = PaddingValues(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp) // Space between tabs
        ) {
            items(tabs.size) { index ->
                val isSelected = selectedTab == index
                Box(
                    modifier = Modifier
                        .size(width = 112.dp, height = 42.dp) // Set fixed size
                        .clip(MaterialTheme.shapes.medium)
                        .background(
                            if (isSelected) AppColors.Black else AppColors.White
                        )
                        .border(
                            width = 1.dp,
                            color = AppColors.Gray,
                            shape = MaterialTheme.shapes.medium
                        )
                        .clickable { selectedTab = index }
                        .padding(vertical = 12.dp, horizontal = 16.dp), // Adjust padding for better width
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tabs[index],
                        color = if (isSelected) AppColors.White else AppColors.Black,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
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
                        4 -> notification.type == NotificationType.MENTION
                        else -> true
                    }
                }) { notification ->
                    NotificationItem(
                        notification = notification,
                        onMarkAsRead = { notificationViewModel.markAsRead(notification.id) },
                        onNotificationClick = {
                            notification.postId?.let { postId ->
                                // Log the postId to verify it's not null
                                Log.d("Activity", "Navigating to thread with postId: $postId")
                                navController.navigate(Routes.createThreadRoute(postId))
                            } ?: run {
                                // Log an error message if postId is null
                                Log.e("Activity", "Post ID is null for notification: ${notification.id}")
                                // Optionally, show a message to the user or handle the error gracefully
                            }
                        }
                    )

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
                    NotificationType.REPLY -> "replied to your comment"
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
                    NotificationType.REPLY -> Icons.Outlined.ChatBubbleOutline
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