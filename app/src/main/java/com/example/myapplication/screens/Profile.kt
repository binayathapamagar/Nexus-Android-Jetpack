package com.example.myapplication.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.myapplication.viewmodel.AuthViewModel
import com.example.myapplication.viewmodel.PostViewModel
import com.example.myapplication.R
import com.example.myapplication.custom_color.color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Profile(
    navController: NavController,
    authViewModel: AuthViewModel,
    postViewModel: PostViewModel = viewModel(),
    userId: String
) {
    // Observing state from AuthViewModel
    val profileImageUrl by authViewModel.profileImageUrl.collectAsState()
    val userName by authViewModel.userName.collectAsState()
    val userHandle by authViewModel.userHandle.collectAsState()
    val userBio by authViewModel.userBio.collectAsState()
    val followerCount by authViewModel.followerCount.collectAsState()

    // Collecting posts and filtering by userId
    val posts by postViewModel.posts.collectAsState()
    val userPosts = posts.filter { it.userId == userId }

    LaunchedEffect(userId) {
        postViewModel.fetchPosts() // Triggering post fetch
    }

    // Scaffold layout for the Profile screen
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Profile",
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { /* TODO: Handle language selection */ }) {
                        Icon(Icons.Default.Language, contentDescription = "Language")
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate("settings") }) {
                        Icon(Icons.Default.Menu, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.smallTopAppBarColors(
                    containerColor = color.White,
                    titleContentColor = color.Black,
                    navigationIconContentColor = color.Black,
                    actionIconContentColor = color.Black
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            item {
                // Profile Header
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

                // Bio
                Text(
                    text = userBio ?: "User bio goes here",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Followers
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

                // Buttons for Edit and Share Profile
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { /* TODO: Implement edit profile */ },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = color.White, contentColor = color.Black),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Edit profile")
                    }
                    Button(
                        onClick = { /* TODO: Implement share profile */ },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = color.White, contentColor = color.Black),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Share profile")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Tabs for navigating Threads, Replies, Reposts
                TabRow(
                    selectedTabIndex = 0,
                    containerColor = color.White,
                    contentColor = color.Black
                ) {
                    Tab(selected = true, onClick = { /* TODO */ }) {
                        Text("Threads", modifier = Modifier.padding(vertical = 8.dp))
                    }
                    Tab(selected = false, onClick = { /* TODO */ }) {
                        Text("Replies", modifier = Modifier.padding(vertical = 8.dp))
                    }
                    Tab(selected = false, onClick = { /* TODO */ }) {
                        Text("Reposts", modifier = Modifier.padding(vertical = 8.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Display user posts with dividers
            items(userPosts) { post ->
                PostItem(post = post, postViewModel = postViewModel)
                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f), thickness = 1.dp)
            }
        }
    }
}
