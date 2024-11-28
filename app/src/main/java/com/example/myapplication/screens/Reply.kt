package com.example.myapplication.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
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
import com.example.myapplication.R
import com.example.myapplication.PostViewModel
import com.example.myapplication.AuthViewModel
import com.example.myapplication.ui.theme.AppColors
import kotlinx.coroutines.launch

@Composable
private fun VerticalLine() {
    Box(
        modifier = Modifier
            .width(2.dp)
            .height(20.dp)
            .padding(vertical = 4.dp)
            .background(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                shape = RoundedCornerShape(1.dp)
            )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Reply(
    navController: NavController,
    postViewModel: PostViewModel,
    authViewModel: AuthViewModel,
    postId: String,
    replyToUsername: String
) {
    var replyText by remember { mutableStateOf("") }
    var isPosting by remember { mutableStateOf(false) }
    val profileImageUrl by authViewModel.profileImageUrl.collectAsState()
    val userName by authViewModel.userName.collectAsState()
    val post by postViewModel.getPost(postId).collectAsState(initial = null)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Reply",
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel")
                    }
                },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = Color.Transparent)
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                post?.let { originalPost ->
                    Column(horizontalAlignment = Alignment.Start) {
                        // First profile section
                        Row(verticalAlignment = Alignment.Top) {
                            AsyncImage(
                                model = originalPost.userProfileImageUrl,
                                contentDescription = "Original Post Profile Picture",
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop,
                                error = painterResource(id = R.drawable.person)
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = originalPost.userName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = originalPost.content,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // Centered vertical line
                        Box(
                            modifier = Modifier
                                .padding(start = 19.dp)  // Center align with profile pics (40/2 - 1)
                                .padding(vertical = 4.dp)  // Space from profile pics
                        ) {
                            VerticalLine()
                        }

                        // Reply section
                        Row(
                            verticalAlignment = Alignment.Top
                        ) {
                            AsyncImage(
                                model = profileImageUrl,
                                contentDescription = "Your Profile Picture",
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop,
                                error = painterResource(id = R.drawable.person)
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = userName ?: "User",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = " · Reply to @$replyToUsername",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                TextField(
                                    value = replyText,
                                    onValueChange = { replyText = it },
                                    placeholder = { Text("Write your reply") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        disabledContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent
                                    ),
                                    enabled = !isPosting
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    ActionButton(Icons.Default.Image, "Add Images") { /* TODO */ }
                                    ActionButton(Icons.Default.Camera, "Take Photo") { /* TODO */ }
                                    ActionButton(Icons.Default.Gif, "Add GIF") { /* TODO */ }
                                    ActionButton(Icons.Default.Mic, "Voice Recording") { /* TODO */ }
                                    ActionButton(Icons.Default.Tag, "Add Hashtag") { /* TODO */ }
                                    ActionButton(Icons.AutoMirrored.Filled.List, "Add List") { /* TODO */ }
                                    ActionButton(Icons.Default.LocationOn, "Add Location") { /* TODO */ }
                                }
                            }
                        }
                    }
                }
            }

            // Reply button
            Button(
                onClick = {
                    if (replyText.isNotBlank()) {
                        isPosting = true
                        scope.launch {
                            try {
                                postViewModel.addReply(postId, replyText)
                                navController.navigateUp()
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar(
                                    "Failed to post reply. Please try again."
                                )
                            } finally {
                                isPosting = false
                            }
                        }
                    }
                },
                enabled = replyText.isNotBlank() && !isPosting,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.Primary,
                    contentColor = AppColors.White
                )
            ) {
                if (isPosting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White
                    )
                } else {
                    Text("Reply", color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(40.dp)
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(24.dp)
        )
    }
}
