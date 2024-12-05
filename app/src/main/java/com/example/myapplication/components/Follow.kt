package com.example.myapplication.components

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.myapplication.viewmodels.FollowViewModel

@Composable
fun FollowButton(
    userId: String,
    isFollowing: Boolean,
    modifier: Modifier = Modifier,
    followViewModel: FollowViewModel
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

    Button(
        onClick = {
            if (isFollowing) {
                showUnfollowDialog = true
            } else {
                followViewModel.toggleFollow(userId)
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
    followingCount: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
            Text(
                text = followersCount.toString(),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Followers",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
            Text(
                text = followingCount.toString(),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Following",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}