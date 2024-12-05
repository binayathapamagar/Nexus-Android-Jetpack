package com.example.myapplication.components

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.myapplication.models.NotificationType
import com.example.myapplication.viewmodels.FollowViewModel
import com.example.myapplication.viewmodels.NotificationViewModel

@Composable
fun FollowButton(
    userId: String,
    isFollowing: Boolean,
    modifier: Modifier = Modifier,
    followViewModel: FollowViewModel,
    notificationViewModel: NotificationViewModel
) {
    val isLoading by followViewModel.isLoading.collectAsState()
    var showUnfollowDialog by remember { mutableStateOf(false) }

    // Unfollow confirmation dialog
    if (showUnfollowDialog) {
        AlertDialog(
            onDismissRequest = { showUnfollowDialog = false },
            title = { Text("Unfollow") },
            text = { Text("Are you sure you want to unfollow this user?") },
            confirmButton = {
                Button(
                    onClick = {
                        followViewModel.toggleFollow(userId)
                        showUnfollowDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Unfollow")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnfollowDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    OutlinedButton(
        onClick = {
            if (isFollowing) {
                showUnfollowDialog = true
            } else {
                followViewModel.toggleFollow(userId)
                try{
                    notificationViewModel.saveNotification(
                        recipientID = userId,
                        actionType = NotificationType.FOLLOW,
                        postId = "",
                        postContent = ""

                    )} catch (e: Exception) {
                    Log.e("NotificationError", "Error saving notification: ${e.message}")
                }

            }

        },
        modifier = modifier,
        enabled = !isLoading,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isFollowing) MaterialTheme.colorScheme.surface else Color.Black,
            contentColor = if (isFollowing) Color.Black else Color.White
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = if (isFollowing) Color.Black else Color.White,
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = if (isFollowing) "Following" else "Follow",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold
                )
            )
        }
    }
}


@Composable
fun FollowStats(
    followersCount: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
            Text(
                text = followersCount.toString(),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Followers",
                style = MaterialTheme.typography.titleMedium
            )


    }
}