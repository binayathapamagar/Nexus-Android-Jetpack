package com.example.myapplication.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Repeat
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
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.myapplication.Post
import com.example.myapplication.PostViewModel
import com.example.myapplication.R
import com.example.myapplication.Reply
import com.example.myapplication.navigation.Routes
import com.example.myapplication.utils.toRelativeTimeString

@Composable
fun UserReplyItem(
    post: Post,
    reply: Reply,
    postViewModel: PostViewModel,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    var showOptionsMenu by remember { mutableStateOf(false) }
    var isLiked by remember { mutableStateOf(reply.isLikedByCurrentUser) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable { navController.navigate(Routes.createThreadRoute(post.id)) }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // Left column for profile pictures and connecting line
            Box(modifier = Modifier.width(40.dp)) {
                // Vertical connecting line that spans full height
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .fillMaxHeight()
                        .align(Alignment.Center)
                        .background(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(1.dp)
                        )
                )

                // Profile pictures column
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    AsyncImage(
                        model = post.userProfileImageUrl,
                        contentDescription = "Profile Picture",
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop,
                        error = painterResource(id = R.drawable.person)
                    )
                }
            }

            // Right column for content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                // Original post content
                Column(modifier = Modifier.padding(bottom = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = post.userName, fontWeight = FontWeight.Bold)
                        Text(
                            text = post.timestamp?.toRelativeTimeString() ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    Text(text = post.content)

                    if (post.imageUrls.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        AsyncImage(
                            model = post.imageUrls.first(),
                            contentDescription = "Post Image",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }

                    InteractionButtons(
                        likes = post.likes,
                        comments = post.comments,
                        reposts = post.reposts,
                        isLiked = post.isLikedByCurrentUser,
                        onLikeClick = { postViewModel.likePost(post.id, !post.isLikedByCurrentUser) },
                        onCommentClick = { navController.navigate(Routes.createReplyRoute(post.id, post.userName)) },
                        onRepostClick = { postViewModel.repostPost(post.id) }
                    )
                }

                // Reply content with profile picture next to username
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top // Changed to Top alignment
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = reply.userProfileImageUrl,
                                contentDescription = "Reply Profile Picture",
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop,
                                error = painterResource(id = R.drawable.person)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = reply.userName, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "· ${reply.timestamp?.toRelativeTimeString() ?: ""}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                                Text(text = reply.content)
                            }
                        }
                        IconButton(
                            onClick = { showOptionsMenu = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "More options",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    InteractionButtons(
                        likes = reply.likes,
                        comments = reply.replies,
                        reposts = reply.reposts,
                        isLiked = isLiked,
                        onLikeClick = {
                            isLiked = !isLiked
                            postViewModel.likeReply(post.id, reply.id, isLiked)
                        },
                        onCommentClick = { /* Handle nested reply */ },
                        onRepostClick = { postViewModel.repostReply(post.id, reply.id) }
                    )
                }
            }
        }
    }

    if (showOptionsMenu) {
        AlertDialog(
            onDismissRequest = { showOptionsMenu = false },
            confirmButton = {
                TextButton(onClick = {
                    postViewModel.deleteReply(post.id, reply.id)
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

@Composable
private fun InteractionButtons(
    likes: Int,
    comments: Int,
    reposts: Int,
    isLiked: Boolean,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onRepostClick: () -> Unit
) {
    Row(
        modifier = Modifier.padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onLikeClick) {
            Icon(
                imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = "Like",
                tint = if (isLiked) Color.Red else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp)
            )
        }
        Text(text = "$likes")
        Spacer(modifier = Modifier.width(16.dp))

        IconButton(onClick = onCommentClick) {
            Icon(
                Icons.Outlined.ChatBubbleOutline,
                contentDescription = "Comment",
                modifier = Modifier.size(20.dp)
            )
        }
        Text(text = "$comments")
        Spacer(modifier = Modifier.width(16.dp))

        IconButton(onClick = onRepostClick) {
            Icon(
                imageVector = if (reposts > 0) Icons.Filled.Repeat else Icons.Outlined.Repeat,
                contentDescription = "Repost",
                tint = if (reposts > 0) Color.Green else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp)
            )
        }
        Text(text = "$reposts")
    }
}
