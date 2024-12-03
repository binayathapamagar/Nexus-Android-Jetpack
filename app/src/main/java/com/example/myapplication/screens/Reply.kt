package com.example.myapplication.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
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
    replyToUsername: String,
    parentReplyId: String? = null
) {
    var replyText by remember { mutableStateOf("") }
    var isPosting by remember { mutableStateOf(false) }
    var selectedImages by remember { mutableStateOf<List<Uri>>(emptyList()) }
    val profileImageUrl by authViewModel.profileImageUrl.collectAsState()
    val userName by authViewModel.userName.collectAsState()
    val post by postViewModel.getPost(postId).collectAsState(initial = null)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        selectedImages = uris
        uris.forEach { uri ->
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
    }

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
                        // Original post content
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

                        Box(
                            modifier = Modifier
                                .padding(start = 19.dp)
                                .padding(vertical = 4.dp)
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

                                // Selected images preview
                                if (selectedImages.isNotEmpty()) {
                                    LazyRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(selectedImages) { uri ->
                                            Box(
                                                modifier = Modifier
                                                    .size(100.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                            ) {
                                                AsyncImage(
                                                    model = uri,
                                                    contentDescription = "Selected Image",
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop
                                                )
                                                IconButton(
                                                    onClick = {
                                                        selectedImages = selectedImages.filter { it != uri }
                                                    },
                                                    modifier = Modifier
                                                        .align(Alignment.TopEnd)
                                                        .padding(4.dp)
                                                        .size(24.dp)
                                                        .background(
                                                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                                            shape = CircleShape
                                                        )
                                                ) {
                                                    Icon(
                                                        Icons.Default.Close,
                                                        contentDescription = "Remove Image",
                                                        modifier = Modifier.size(16.dp),
                                                        tint = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    ActionButton(Icons.Default.Image, "Add Images") {
                                        imagePickerLauncher.launch("image/*")
                                    }
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

            Button(
                onClick = {
                    if (replyText.isNotBlank()) {
                        isPosting = true
                        scope.launch {
                            try {
                                postViewModel.addReply(
                                    postId = postId,
                                    replyContent = replyText,
                                    imageUris = selectedImages,
                                    parentReplyId = parentReplyId
                                )
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
