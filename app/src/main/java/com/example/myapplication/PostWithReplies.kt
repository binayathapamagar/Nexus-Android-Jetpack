package com.example.myapplication

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.rememberRipple
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
import com.example.myapplication.navigation.Routes
import com.example.myapplication.screens.FullScreenImageViewer
import com.example.myapplication.screens.PostItem
import com.example.myapplication.screens.Reply
import com.example.myapplication.utils.toRelativeTimeString

@Composable
fun PostWithReplies(
    post: Post,
    replies: List<Reply>,
    postViewModel: PostViewModel,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
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
                    .padding(vertical = 12.dp),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
            )

            if (replies.isNotEmpty()) {
                Text(
                    "Replies",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                )
            }
        }

        items(replies) { reply ->
            ReplyItem(
                reply = reply,
                postViewModel = postViewModel,
                postId = post.id,
                navController = navController,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

@Composable
fun ReplyItem(
    reply: Reply,
    postViewModel: PostViewModel,
    postId: String,
    navController: NavController,
    modifier: Modifier = Modifier,
    isNested: Boolean = false // Add parameter to track nested status
) {
    var showOptionsMenu by remember { mutableStateOf(false) }
    var isLiked by remember { mutableStateOf(reply.isLikedByCurrentUser) }
    var showNestedReplies by remember { mutableStateOf(false) }
    var showImageViewer by remember { mutableStateOf(false) }
    var initialPage by remember { mutableIntStateOf(0) }

    // Direct replies filtering
    val directReplies = reply.nestedReplies.filter { it.parentReplyId == reply.id }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        // Main content (profile picture, text, interactions)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            AsyncImage(
                model = reply.userProfileImageUrl,
                contentDescription = "Profile Picture",
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable { navController.navigate("otherUsers/${reply.userId}") },
                contentScale = ContentScale.Crop,
                error = painterResource(id = R.drawable.person)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = reply.userName,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable {
                                navController.navigate("otherUsers/${reply.userId}")
                            }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "· ${reply.timestamp?.toRelativeTimeString() ?: ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }

                    IconButton(
                        onClick = { showOptionsMenu = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.more),
                            contentDescription = "More options",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Text(reply.content)

                if (reply.imageUrls.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow {
                        items(reply.imageUrls) { imageUrl ->
                            AsyncImage(
                                model = imageUrl,
                                contentDescription = "Reply Image",
                                modifier = Modifier
                                    .size(150.dp)
                                    .padding(end = 8.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        initialPage = reply.imageUrls.indexOf(imageUrl)
                                        showImageViewer = true
                                    },
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }

                InteractionButtons(
                    reply = reply,
                    postId = postId,
                    postViewModel = postViewModel,
                    navController = navController
                )
            }
        }

        // Nested replies section
        if (directReplies.isNotEmpty()) {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                Row(
                    modifier = Modifier
                        .clickable { showNestedReplies = !showNestedReplies }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (showNestedReplies) "Hide replies" else "Show replies",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = " (${directReplies.size})",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                AnimatedVisibility(
                    visible = showNestedReplies,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(modifier = Modifier.padding(start = 16.dp, top = 8.dp)) {
                        directReplies.forEach { nestedReply ->
                            ReplyItem(
                                reply = nestedReply,
                                postViewModel = postViewModel,
                                postId = postId,
                                navController = navController,
                                isNested = true
                            )
                        }
                    }
                }
            }
        }

        // Only show divider if not a nested reply or if it has nested replies that aren't shown
        if (!isNested || (directReplies.isNotEmpty() && !showNestedReplies)) {
            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
            )
        }
    }

    // Dialogs for image viewer and options menu
    if (showImageViewer) {
        FullScreenImageViewer(
            imageUrls = reply.imageUrls,
            initialPage = initialPage,
            onDismiss = { showImageViewer = false }
        )
    }

    if (showOptionsMenu) {
        AlertDialog(
            onDismissRequest = { showOptionsMenu = false },
            title = { Text("Delete Reply?") },
            text = { Text("Are you sure you want to delete this reply? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        postViewModel.deleteReply(postId, reply.id)
                        showOptionsMenu = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showOptionsMenu = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}



@Composable
private fun InteractionButtons(
    reply: Reply,
    postId: String,
    postViewModel: PostViewModel,
    navController: NavController
) {
    Row(
        modifier = Modifier.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        InteractionButton(
            iconRes = R.drawable.heart,
            count = reply.likes,
            isActive = reply.isLikedByCurrentUser,
            activeColor = Color.Red,
            onClick = {
                postViewModel.likeReply(postId, reply.id, !reply.isLikedByCurrentUser)
            }
        )

        InteractionButton(
            iconRes = R.drawable.messagetext1,
            count = reply.replies,
            isActive = false,
            onClick = {
                navController.navigate(Routes.createReplyRoute(postId, reply.userName))
            }
        )

        InteractionButton(
            iconRes = R.drawable.repeat,
            count = reply.reposts,
            isActive = false,
            activeColor = Color.Green,
            onClick = { postViewModel.repostReply(postId, reply.id) }
        )

        IconButton(
            onClick = { /* Implement share */ },
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.send2),
                contentDescription = "Share",
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun InteractionButton(
    iconRes: Int,
    count: Int,
    isActive: Boolean,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable(
            onClick = onClick,
            indication = rememberRipple(bounded = false),
            interactionSource = remember { MutableInteractionSource() }
        )
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            tint = if (isActive) activeColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.width(16.dp))
    }
}

