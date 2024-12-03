package com.example.myapplication

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.myapplication.models.Post
import com.example.myapplication.models.Reply
import com.example.myapplication.navigation.Routes
import com.example.myapplication.screens.FullScreenImageViewer
import com.example.myapplication.screens.PostItem
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
    depth: Int = 0,
    maxDepth: Int = 5
) {
    var showOptionsMenu by remember { mutableStateOf(false) }
    var isLiked by remember { mutableStateOf(reply.isLikedByCurrentUser) }
    var likeCount by remember { mutableIntStateOf(reply.likes) }
    var replyCount by remember { mutableIntStateOf(reply.replies) }
    var repostCount by remember { mutableIntStateOf(reply.reposts) }
    var showNestedReplies by remember { mutableStateOf(false) }
    var showImageViewer by remember { mutableStateOf(false) }
    var initialPage by remember { mutableIntStateOf(0) }

    LaunchedEffect(reply) {
        isLiked = reply.isLikedByCurrentUser
        likeCount = reply.likes
        replyCount = reply.replies
        repostCount = reply.reposts
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            // Indentation for nested replies
            if (depth > 0) {
                Spacer(modifier = Modifier.width((depth * 16).dp))
            }

            AsyncImage(
                model = reply.userProfileImageUrl,
                contentDescription = "Profile Picture",
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .clickable { navController.navigate("otherUsers/${reply.userId}") },
                contentScale = ContentScale.Crop,
                error = painterResource(id = R.drawable.person)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Username and timestamp
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = reply.userName,
                            style = MaterialTheme.typography.bodyMedium,
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
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.more),
                            contentDescription = "More options",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Reply content
                Text(
                    text = reply.content,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                // Images section
                if (reply.imageUrls.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        items(reply.imageUrls) { imageUrl ->
                            AsyncImage(
                                model = imageUrl,
                                contentDescription = "Reply Image",
                                modifier = Modifier
                                    .size(120.dp)
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

                // Interaction buttons
                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Like button
                    IconButton(
                        onClick = {
                            isLiked = !isLiked
                            likeCount = if (isLiked) likeCount + 1 else likeCount - 1
                            postViewModel.likeReply(postId, reply.id, isLiked)
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            painter = painterResource(if (isLiked) R.drawable.hearted else R.drawable.heart),
                            contentDescription = "Like",
                            tint = if (isLiked) Color.Red else LocalContentColor.current,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text("$likeCount", style = MaterialTheme.typography.bodySmall)

                    Spacer(modifier = Modifier.width(16.dp))

                    // Reply button - Navigates to Reply screen
                    if (depth < maxDepth) {
                        IconButton(
                            onClick = {
                                navController.navigate(
                                    Routes.createReplyRoute(
                                        postId = postId,
                                        parentReplyId = reply.id,
                                        replyToUsername = reply.userName
                                    )
                                )
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.messagetext1),
                                contentDescription = "Reply",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text("$replyCount", style = MaterialTheme.typography.bodySmall)
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Repost button
                    IconButton(
                        onClick = { postViewModel.repostReply(postId, reply.id) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.repeat),
                            contentDescription = "Repost",
                            tint = if (repostCount > 0) MaterialTheme.colorScheme.primary else LocalContentColor.current,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text("$repostCount", style = MaterialTheme.typography.bodySmall)

                    Spacer(modifier = Modifier.width(16.dp))

                    // Share button
                    IconButton(
                        onClick = { /* Implement share functionality */ },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.send2),
                            contentDescription = "Share",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Nested replies toggle
                if (reply.nestedReplies.isNotEmpty()) {
                    TextButton(
                        onClick = { showNestedReplies = !showNestedReplies },
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Text(
                            if (showNestedReplies) "Hide replies" else "Show replies (${reply.nestedReplies.size})",
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    AnimatedVisibility(
                        visible = showNestedReplies,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column {
                            reply.nestedReplies.forEach { nestedReply ->
                                ReplyItem(
                                    reply = nestedReply,
                                    postViewModel = postViewModel,
                                    postId = postId,
                                    navController = navController,
                                    depth = depth + 1,
                                    maxDepth = maxDepth
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}



