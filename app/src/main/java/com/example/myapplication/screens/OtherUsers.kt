package com.example.myapplication.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import coil.compose.rememberImagePainter
import com.example.myapplication.Post
import com.example.myapplication.R
import com.example.myapplication.UserProfileViewModel
import com.example.myapplication.ui.theme.AppColors
import com.example.myapplication.ui.theme.AppColors.Divider
import com.example.myapplication.utils.toRelativeTimeString
import java.text.SimpleDateFormat
import java.util.Date

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
                                containerColor = AppColors.Primary,
                                contentColor = AppColors.White
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
                            color = Divider
                        )
                    }
                    1 -> ContentList(replies, "No replies available") { reply ->
                        ReplyItem(
                            reply,
                            post = Post(),
                            onLikeClick = TODO(),
                            onProfileClick = TODO()
                        )
                        HorizontalDivider(
                            modifier = Modifier.fillMaxWidth(),
                            thickness = 1.dp,
                            color = Divider
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
                            color = Divider
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

@SuppressLint("SimpleDateFormat")
@Composable
fun ReplyItem(
    reply: Reply,
    post: Post,
    onLikeClick: (Reply) -> Unit,
    onProfileClick: (String) -> Unit // For handling profile click
) {
    Column(modifier = Modifier.padding(16.dp)) {
        // User profile image and name
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = rememberImagePainter(reply.userProfileImageUrl),
                contentDescription = "Profile Image",
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable { onProfileClick(reply.userId) } // Handle profile click
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = reply.userName,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyLarge
            )
        }

        // Content of the reply
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = reply.content,
            style = MaterialTheme.typography.bodyMedium
        )

        // Timestamp and like functionality
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(onClick = { onLikeClick(reply) }) {
                Icon(
                    imageVector = if (reply.isLikedByCurrentUser) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = "Like"
                )
            }

            Text(text = "${reply.likes} likes", style = MaterialTheme.typography.bodyMedium)

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = reply.timestamp?.let { SimpleDateFormat("HH:mm").format(it) } ?: "Just now",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        // Divider or separator between replies
        Divider(modifier = Modifier.padding(vertical = 8.dp))
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
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userProfileImageUrl: String = "",
    val content: String = "",
    val imageUrls: List<String> = emptyList(), // Added image support
    val timestamp: Date? = null,
    val likes: Int = 0,
    val replies: Int = 0,
    val reposts: Int = 0,
    val isLikedByCurrentUser: Boolean = false,
    val likedBy: List<String> = emptyList(),
    val parentReplyId: String? = null, // Added to track parent reply
    val nestedReplies: List<Reply> = emptyList()
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
