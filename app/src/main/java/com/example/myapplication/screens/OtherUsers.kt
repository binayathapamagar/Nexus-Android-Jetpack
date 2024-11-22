package com.example.myapplication.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.myapplication.Post
import com.example.myapplication.R
import com.example.myapplication.Reply
import com.example.myapplication.UserProfileViewModel
import com.example.myapplication.custom_color.color
import com.example.myapplication.utils.toRelativeTimeString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtherUsers(
    navController: NavController,
    userId: String
) {
    val userProfileViewModel: UserProfileViewModel = viewModel()

    // Fetch user data when the screen is first displayed
    LaunchedEffect(userId) {
        userProfileViewModel.fetchUserData(userId)
    }

    val userProfile = userProfileViewModel.userProfile.value
    val posts = userProfileViewModel.posts.value
    val replies = userProfileViewModel.replies.value
    val reposts = userProfileViewModel.reposts.value
    val isLoading = userProfileViewModel.isLoading.value

    var selectedTabIndex by remember { mutableIntStateOf(0) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                // TopAppBar with back navigation
                TopAppBar(
                    title = { Text("") },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )

                // User Profile Section
                userProfile?.let {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = it.fullName,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = it.username,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            // Profile image or default icon
                            AsyncImage(
                                model = it.profileImageUrl.ifEmpty { "drawable/person" },
                                contentDescription = "Profile Image",
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = it.bio,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AsyncImage(
                                model = R.drawable.followers,
                                contentDescription = "Followers",
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "${it.followersCount} Followers",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = { /* Handle follow button */ },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = color.Black,
                                contentColor = color.White
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(text = "Follow")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Tab Row for Threads, Replies, Reposts
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Tab(
                        selected = selectedTabIndex == 0,
                        onClick = { selectedTabIndex = 0 },
                        text = { Text("Threads") }
                    )
                    Tab(
                        selected = selectedTabIndex == 1,
                        onClick = { selectedTabIndex = 1 },
                        text = { Text("Replies") }
                    )
                    Tab(
                        selected = selectedTabIndex == 2,
                        onClick = { selectedTabIndex = 2 },
                        text = { Text("Reposts") }
                    )
                }

                // Content Display Based on Selected Tab
                when (selectedTabIndex) {
                    0 -> ContentList(posts, "No threads available") { post ->
                        PostItem(post)
                        HorizontalDivider(
                            modifier = Modifier.fillMaxWidth(),
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                        )
                    }
                    1 -> ContentList(replies, "No replies available") { reply ->
                        ReplyItem(reply)
                        HorizontalDivider(
                            modifier = Modifier.fillMaxWidth(),
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                        )
                    }
                    2 -> ContentList(reposts, "No reposts available") { repost ->
                        RepostItem(
                            repost = repost,
                            posts = posts
                        )
                        HorizontalDivider(
                            modifier = Modifier.fillMaxWidth(),
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                        )
                    }
                }
            }
        }
    }
}

// Generic Content List Composable
@Composable
fun <T> ContentList(
    items: List<T>,
    emptyMessage: String,
    content: @Composable (T) -> Unit
) {
    if (items.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = emptyMessage,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(items) { item ->
                content(item)
            }
        }
    }
}

@Composable
fun PostItem(
    post: Post,
    modifier: Modifier = Modifier
) {
    var isLiked by remember { mutableStateOf(false) }
    val isReposted by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Post Header with three-dot options
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Post Header: User Profile Image and Name
            AsyncImage(
                model = post.userProfileImageUrl.ifEmpty { "drawable/person" },
                contentDescription = "User Profile Image",
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = post.userName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = post.timestamp.toRelativeTimeString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            // Three-dot options menu
            IconButton(onClick = { /* Handle 3-dot options */ }) {
                Icon(Icons.Filled.MoreVert, contentDescription = "More Options")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Post content
        Text(post.content)

        // Post Image (if available)
        post.imageUrls.let {
            if (it.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                AsyncImage(
                    model = it,
                    contentDescription = "Post Image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            }
        }

        // Action buttons (Like, Share, Repost, Comment)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically, // Vertically center content


        ) {
            IconButton(onClick = { isLiked = !isLiked }) {
                Icon(
                    imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "Like",
                    tint = if (isLiked) Color.Red else MaterialTheme.colorScheme.onSurface

                )

            }
            Text(
                text = "${post.likes}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(end = 16.dp)
            )
            IconButton(onClick = { /* Handle repost */ }) {
                Icon(
                    imageVector = if (isReposted) Icons.Filled.Repeat else Icons.Outlined.Repeat,
                    contentDescription = "Repost",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(22.dp)
                )

            }
            Text(
                text = "${post.reposts}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(end = 16.dp)
            )
            IconButton(onClick = { /* Handle comment */ }) {
                Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = "Comment",
                    modifier = Modifier.size(22.dp))


            }
            Text(
                text = "${post.comments}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(end = 16.dp)
            )
            IconButton(onClick = { /* Handle share */ }) {
                Icon(Icons.Filled.Share, contentDescription = "Share",
                        modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
fun ReplyItem(reply: Reply) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = reply.content,
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Replied by: ${reply.userName}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
fun RepostItem(repost: Repost, posts: List<Post>) {
    Column(modifier = Modifier.padding(16.dp)) {
        // Show repost content
        Text(
            text = repost.repostedByUserName,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = repost.originalPost.content,
            style = MaterialTheme.typography.bodyMedium
        )

        // Display posts (if you want to show posts related to the repost)
        posts.forEach { post ->
            Text(text = post.content, style = MaterialTheme.typography.bodySmall)
        }
    }
}


// UserProfile model
data class UserProfile(
    val userId: String = "", // Unique user ID
    val fullName: String = "", // Full name of the user
    val username: String = "", // Username of the user
    val bio: String = "", // Short bio
    var profileImageUrl: String = "", // URL to the user's profile image
    val followersCount: Int = 0 // Number of followers
)

// Post model
data class Post(
    val id: String = "", // Unique ID for the post
    val content: String = "", // Post content
    val comments: Int = 0, // Number of comments
    val imageUrls: List<String> = emptyList(), // List of image URLs
    val likedBy: List<String> = emptyList(), // List of user IDs who liked the post
    val likes: Int = 0, // Total likes
    val reposts: Int = 0, // Total reposts
    val timestamp: String = "", // Timestamp of the post
    val userId: String = "", // ID of the post creator
    val userName: String = "", // Name of the post creator
    val userProfileImageUrl: String = "" // Profile image URL of the post creator
)

// Reply model
data class Reply(
    val id: String = "", // Unique ID for the reply
    val postId: String = "", // ID of the post being replied to
    val content: String = "", // Reply content
    val userId: String = "", // ID of the user who made the reply
    val userName: String = "", // Name of the reply creator
    val timestamp: String = "" // Timestamp of the reply
)

// Repost model
data class Repost(
    val id: String = "", // Unique ID for the repost
    val originalPostId: String = "", // ID of the original post being reposted
    val repostedByUserId: String = "", // ID of the user who reposted
    val repostedByUserName: String = "", // Username of the user who reposted
    val timestamp: String = "",// Timestamp of the repost
    val originalPost: com.example.myapplication.screens.Post // Content of the original post

)
