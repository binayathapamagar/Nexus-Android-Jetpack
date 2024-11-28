package com.example.myapplication

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.myapplication.components.CustomIcon
import com.example.myapplication.components.CustomIconType
import com.example.myapplication.screens.PostItem
import com.example.myapplication.utils.toRelativeTimeString


data class Reply(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userProfileImageUrl: String = "",
    val content: String = "",
    val timestamp: java.util.Date? = null,
    val likes: Int = 0,
    val replies: Int = 0,
    val reposts: Int = 0,
    val isLikedByCurrentUser: Boolean = false,
    val likedBy: List<String> = emptyList(),
    val mentionedUsers: List<String> = emptyList() // Added this field
)


@Composable
fun PostWithReplies(
    post: Post,
    replies: List<Reply>,
    postViewModel: PostViewModel,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth()
    ) {
        // Original post
        item {
            PostItem(
                post = post,
                postViewModel = postViewModel,
                navController = navController,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )
            if (replies.isNotEmpty()) {
                Text(
                    "Replies",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        // Replies
        items(replies) { reply ->
            ReplyItem(
                reply = reply,
                postViewModel = postViewModel,
                postId = post.id,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )
        }
    }
}
@Composable
fun ReplyItem(
    reply: Reply,
    postViewModel: PostViewModel,
    postId: String,
    modifier: Modifier = Modifier
) {
    var showOptionsMenu by remember { mutableStateOf(false) }
    var isLiked by remember { mutableStateOf(reply.isLikedByCurrentUser) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Header with profile picture and username
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = reply.userProfileImageUrl,
                    contentDescription = "Profile Picture",
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(reply.userName, fontWeight = FontWeight.Bold)
                    Text(
                        text = reply.timestamp.toRelativeTimeString(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
            // Settings icon
            IconButton(
                onClick = { showOptionsMenu = true },
                modifier = Modifier.size(40.dp)
            ) {
                CustomIcon(
                    iconType = CustomIconType.SETTINGS,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(reply.content)
        Spacer(modifier = Modifier.height(8.dp))

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Like button
            IconButton(
                onClick = {
                    isLiked = !isLiked
                    postViewModel.likeReply(postId, reply.id, isLiked)
                }
            ) {
                Icon(
                    imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "Like",
                    tint = if (isLiked) Color.Red else MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = "${reply.likes}",
                modifier = Modifier.padding(end = 16.dp)
            )

            // Comment button
            IconButton(
                onClick = {
                    // Handle nested reply
                }
            ) {
                Icon(
                    Icons.Outlined.ChatBubbleOutline,
                    contentDescription = "Reply"
                )
            }
            Text(
                text = "${reply.replies}",
                modifier = Modifier.padding(end = 16.dp)
            )

            // Repost button
            IconButton(
                onClick = { postViewModel.repostReply(postId, reply.id) }
            ) {
                Icon(
                    imageVector = if (reply.reposts > 0) Icons.Filled.Repeat else Icons.Outlined.Repeat,
                    contentDescription = "Repost",
                    tint = if (reply.reposts > 0) Color.Green else MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = "${reply.reposts}",
                modifier = Modifier.padding(end = 16.dp)
            )

            // Share button
            IconButton(onClick = { /* Implement share functionality */ }) {
                Icon(
                    Icons.Outlined.Share,
                    contentDescription = "Share"
                )
            }
        }

        // Options menu dialog
        if (showOptionsMenu) {
            AlertDialog(
                onDismissRequest = { showOptionsMenu = false },
                confirmButton = {
                    TextButton(onClick = {
                        postViewModel.deleteReply(postId, reply.id)
                        showOptionsMenu = false
                    }) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showOptionsMenu = false }) {
                        Text("Cancel")
                    }
                },
                text = { Text("Do you want to delete this reply?") }
            )
        }
    }
}