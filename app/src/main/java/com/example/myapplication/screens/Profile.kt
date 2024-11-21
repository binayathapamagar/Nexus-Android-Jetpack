package com.example.myapplication.screens

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.myapplication.AuthViewModel
import com.example.myapplication.PostViewModel
import com.example.myapplication.R
import com.example.myapplication.custom_color.color

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun Profile(
    navController: NavController,
    parentNavController: NavController,
    authViewModel: AuthViewModel,
    postViewModel: PostViewModel,
    userId: String
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val profileImageUrl by authViewModel.profileImageUrl.collectAsState()
    val userName by authViewModel.userName.collectAsState()
    val userHandle by authViewModel.userHandle.collectAsState()
    val userBio by authViewModel.userBio.collectAsState()
    val followerCount by authViewModel.followerCount.collectAsState()
    val posts by postViewModel.posts.collectAsState()
    val userPosts = posts.filter { it.userId == userId }
    val profileLink by authViewModel.profileLink.collectAsState()
    var showShareSheet by remember { mutableStateOf(false) }
    val userReplies by postViewModel.userReplies.collectAsState()
    val userReposts by postViewModel.userReposts.collectAsState()

    val listState = rememberLazyListState()
    val isScrolled by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0
        }
    }

    LaunchedEffect(userId, selectedTab) {
        when (selectedTab) {
            0 -> postViewModel.fetchPosts()
            1 -> postViewModel.fetchUserReplies(userId)
            2 -> postViewModel.fetchUserReposts(userId)
        }
    }

    // Share Bottom Sheet component
    ShareBottomSheet(
        isVisible = showShareSheet,
        onDismiss = { showShareSheet = false },
        username = userHandle,
        name = userName,
        bio = userBio
    )

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { },
            navigationIcon = {
                IconButton(onClick = { /* Handle language selection */ }) {
                    Icon(Icons.Default.Language, contentDescription = "Language")
                }
            },
            actions = {
                IconButton(onClick = { navController.navigate("settings") }) {
                    Icon(Icons.Default.Menu, contentDescription = "Settings")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = color.White,
                navigationIconContentColor = color.Black,
                actionIconContentColor = color.Black
            )
        )

        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize()
            ) {
                // Profile Info Section
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = userName ?: "User Name",
                                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = userHandle ?: "user_handle",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            AsyncImage(
                                model = profileImageUrl,
                                contentDescription = "Profile Picture",
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = userBio ?: "User bio goes here",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        // Add profile link display after bio
                        if (!profileLink.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            val context = LocalContext.current
                            Text(
                                text = profileLink!!,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.primary,
                                    textDecoration = TextDecoration.Underline
                                ),
                                modifier = Modifier.clickable {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(profileLink))
                                    context.startActivity(intent)
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AsyncImage(
                                model = R.drawable.followers,
                                contentDescription = "Followers",
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${followerCount ?: 0} followers",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    try {
                                        navController.navigate("edit_profile")
                                    } catch (e: Exception) {
                                        Log.e("Navigation", "Error navigating to edit profile: ${e.message}")
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = color.White,
                                    contentColor = color.Black
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Edit profile")
                            }
                            Button(
                                onClick = { showShareSheet = true },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = color.White,
                                    contentColor = color.Black
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Share profile")
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }

                // Sticky Header
                stickyHeader {
                    Surface(
                        color = color.White,
                        modifier = Modifier.fillMaxWidth(),
                        shadowElevation = if (isScrolled) 1.dp else 0.dp
                    ) {
                        TabRow(
                            selectedTabIndex = selectedTab,
                            containerColor = color.White,
                            contentColor = color.Black,
                            divider = {
                                HorizontalDivider(
                                    thickness = 1.dp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                                )
                            }
                        ) {
                            Tab(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 }
                            ) {
                                Text(
                                    "Threads",
                                    modifier = Modifier.padding(vertical = 12.dp),
                                    fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                            Tab(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 }
                            ) {
                                Text(
                                    "Replies",
                                    modifier = Modifier.padding(vertical = 12.dp),
                                    fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                            Tab(
                                selected = selectedTab == 2,
                                onClick = { selectedTab = 2 }
                            ) {
                                Text(
                                    "Reposts",
                                    modifier = Modifier.padding(vertical = 12.dp),
                                    fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                // Posts
                when (selectedTab) {
                    0 -> {
                        items(userPosts) { post ->
                            PostItem(
                                post = post,
                                postViewModel = postViewModel,
                                navController = parentNavController,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            HorizontalDivider(
                                modifier = Modifier.fillMaxWidth(),
                                thickness = 1.dp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                            )
                        }
                    }
                    1 -> {
                        items(userReplies) { (post, reply) ->
                            UserReplyItem(
                                post = post,
                                reply = reply,
                                postViewModel = postViewModel,
                                navController = parentNavController,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            HorizontalDivider(
                                modifier = Modifier.fillMaxWidth(),
                                thickness = 1.dp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                            )
                        }
                    }
                    2 -> {
                        items(userReposts) { post ->
                            if (post.isRepost) {
                                RepostHeader(repostedByName = post.repostedByName ?: "")
                            }
                            PostItem(
                                post = post,
                                postViewModel = postViewModel,
                                navController = parentNavController,
                                modifier = Modifier.padding(horizontal = 16.dp)
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
}

//@Composable
//fun RepostsTab(
//    posts: List<Post>,
//    postViewModel: PostViewModel,
//    navController: NavController
//) {
//    LazyColumn {
//        items(posts) { post ->
//            PostItem(
//                post = post,
//                postViewModel = postViewModel,
//                navController = navController
//            )
//            HorizontalDivider()
//        }
//    }
//}