package com.example.myapplication.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.myapplication.PostViewModel
import com.example.myapplication.R
import com.example.myapplication.UserProfileViewModel
import com.example.myapplication.models.Post
import com.example.myapplication.models.Reply
import com.example.myapplication.models.UserProfile
import com.example.myapplication.navigation.Routes


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtherUsers(
    navController: NavController,
    parentNavController: NavController,  // Add this parameter
    userId: String,
) {
    val userProfileViewModel: UserProfileViewModel = viewModel()
    val postViewModel: PostViewModel = viewModel()

    // Fetch user data when the screen is first displayed
    LaunchedEffect(userId) {
        userProfileViewModel.fetchUserData(userId)
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    val userProfile = userProfileViewModel.userProfile.value
    val posts by postViewModel.posts.collectAsState()
    val userPosts = posts.filter { it.userId == userId }
    val userReplies by postViewModel.userReplies.collectAsState()
    val userReposts by postViewModel.userReposts.collectAsState()
    val isLoading = userProfileViewModel.isLoading.value

    // Fetch appropriate data based on selected tab
    LaunchedEffect(selectedTab) {
        when (selectedTab) {
            0 -> postViewModel.fetchPosts() // Threads
            1 -> postViewModel.fetchUserReplies(userId) // Replies
            2 -> postViewModel.fetchUserReposts(userId) // Reposts
        }
    }

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
                userProfile?.let { profile ->
                    UserProfileHeader(
                        profile = profile,
                        onFollowClick = { /* Implement follow functionality */ }
                    )
                }

                // Tabs and Content
                TabSection(
                    selectedTabIndex = selectedTab,
                    onTabSelected = { selectedTab = it },
                    userPosts = userPosts,
                    userReplies = userReplies,
                    userReposts = userReposts,
                    postViewModel = postViewModel,
                    navController = navController,
                    parentNavController = parentNavController  // Add this
                )
            }
        }
    }
}

@Composable
private fun UserProfileHeader(
    profile: UserProfile,
    onFollowClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = profile.fullName,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = profile.username,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }

            AsyncImage(
                model = profile.profileImageUrl.ifEmpty { R.drawable.person },
                contentDescription = "Profile Image",
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = profile.bio,
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.followers),
                contentDescription = "Followers",
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = "${profile.followersCount} followers",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onFollowClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Follow")
        }
    }
}

@Composable
private fun TabSection(
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    userPosts: List<Post>,
    userReplies: List<Pair<Post, Reply>>,
    userReposts: List<Post>,
    postViewModel: PostViewModel,
    navController: NavController,
    parentNavController: NavController  // Add this parameter
) {
    Column {
        TabRow(selectedTabIndex = selectedTabIndex) {
            listOf("Threads", "Replies", "Reposts").forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { onTabSelected(index) },
                    text = { Text(title) }
                )
            }
        }

        when (selectedTabIndex) {
            0 -> ThreadsTab(userPosts, postViewModel, navController, parentNavController)
            1 -> RepliesTab(userReplies, postViewModel, navController, parentNavController)
            2 -> RepostsTab(userReposts, postViewModel, navController, parentNavController)
        }
    }
}

@Composable
private fun ThreadsTab(
    posts: List<Post>,
    postViewModel: PostViewModel,
    navController: NavController,
    parentNavController: NavController
) {
    LazyColumn {
        items(posts) { post ->
            PostItem(
                post = post,
                postViewModel = postViewModel,
                navController = parentNavController,  // Use parent controller for Thread navigation
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .clickable {
                        parentNavController.navigate(Routes.createThreadRoute(post.id))
                    }
            )
            HorizontalDivider()
        }
    }
}


@Composable
private fun RepliesTab(
    replies: List<Pair<Post, Reply>>,
    postViewModel: PostViewModel,
    navController: NavController,
    parentNavController: NavController
) {
    if (replies.isEmpty()) {
        EmptyStateMessage("No replies yet")
    } else {
        LazyColumn {
            items(replies) { (originalPost, reply) ->
                Column(
                    modifier = Modifier
                        .clickable {
                            parentNavController.navigate(Routes.createThreadRoute(originalPost.id))
                        }
                        .padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = "Replying to @${originalPost.userName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    Text(
                        text = originalPost.content,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Update this line to use parentNavController
                    UserReplyItem(
                        post = originalPost,
                        reply = reply,
                        postViewModel = postViewModel,
                        navController = parentNavController  // Changed from navController to parentNavController
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

@Composable
private fun RepostsTab(
    reposts: List<Post>,
    postViewModel: PostViewModel,
    navController: NavController,
    parentNavController: NavController
) {
    if (reposts.isEmpty()) {
        EmptyStateMessage("No reposts yet")
    } else {
        LazyColumn {
            items(reposts) { post ->
                Column(
                    modifier = Modifier
                        .clickable {
                            parentNavController.navigate(Routes.createThreadRoute(post.originalPostId ?: post.id))
                        }
                        .padding(horizontal = 16.dp)
                ) {
                    if (post.isRepost) {
                        Text(
                            text = "${post.repostedByName ?: "User"} reposted",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    PostItem(
                        post = post,
                        postViewModel = postViewModel,
                        navController = parentNavController,  // Make sure to use parentNavController here
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                        // Remove the clickable here since it's already in the Column
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}


@Composable
private fun EmptyStateMessage(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}


