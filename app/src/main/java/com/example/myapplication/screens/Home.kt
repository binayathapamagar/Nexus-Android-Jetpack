package com.example.myapplication.screens


import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
//import android.content.Context
//import android.os.Build
//import android.os.VibrationEffect
//import android.os.Vibrator
import android.view.HapticFeedbackConstants
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.myapplication.AuthViewModel
import com.example.myapplication.Post
import com.example.myapplication.PostViewModel
import com.example.myapplication.R
import com.example.myapplication.navigation.Routes
import com.example.myapplication.utils.toRelativeTimeString

//@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Home(
    navController: NavController,
    postViewModel: PostViewModel = viewModel(),
    authViewModel: AuthViewModel
) {
    val posts by postViewModel.posts.collectAsState()
    val profileImageUrl by authViewModel.profileImageUrl.collectAsState()
    val userName by authViewModel.userName.collectAsState()

    LaunchedEffect(profileImageUrl, userName) {
        postViewModel.fetchPosts()
    }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "App Logo",
                    modifier = Modifier.size(40.dp)
                        .padding(top = 8.dp)
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            NewPostSection(
                profileImageUrl = profileImageUrl,
                userName = userName ?: "User",
                onNewPostClick = { navController.navigate("newPost") }
            )

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                items(posts) { post ->
                    PostItem(
                        post = post,
                        postViewModel = postViewModel,
                        navController = navController,
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

@Composable
fun NewPostSection(
    profileImageUrl: String?,
    userName: String,
    onNewPostClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onNewPostClick)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = profileImageUrl,
                contentDescription = "Profile Picture",
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
                error = painterResource(id = R.drawable.person)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = userName,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "What's new?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(Icons.Default.Image, contentDescription = "Add Image",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            Icon(Icons.Default.Camera, contentDescription = "Take Photo",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            Icon(Icons.Default.Gif, contentDescription = "Add GIF",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            Icon(Icons.Default.Mic, contentDescription = "Voice Recording",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            Icon(Icons.Default.Tag, contentDescription = "Add Hashtag",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Add List",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            Icon(Icons.Default.LocationOn, contentDescription = "Add Location",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }
    }
}


@Composable
fun RepostHeader(
    repostedByName: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.Repeat,
            contentDescription = "Reposted",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "$repostedByName reposted",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun rememberHapticFeedback() = remember {
    HapticFeedback()
}

class HapticFeedback {
    fun performHapticFeedback(view: android.view.View) {
        view.performHapticFeedback(
            HapticFeedbackConstants.VIRTUAL_KEY,
            HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
        )
    }
}

@Composable
fun PostItem(
    post: Post,
    postViewModel: PostViewModel,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    var showOptionsMenu by remember { mutableStateOf(false) }
    var showImageViewer by remember { mutableStateOf(false) }
    var initialPage by remember { mutableIntStateOf(0) }
    var isLiked by remember { mutableStateOf(post.isLikedByCurrentUser) }
    var isReposted by remember { mutableStateOf(post.isRepostedByCurrentUser) }
    var showRepostDialog by remember { mutableStateOf(false) }
    val haptic = rememberHapticFeedback()
    val view = LocalView.current

    // Animation states for repost button
    val repostIconScale by animateFloatAsState(
        targetValue = if (isReposted) 1.2f else 1f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 400f),
        label = ""
    )
    val repostIconColor by animateColorAsState(
        targetValue = if (isReposted) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(durationMillis = 300),
        label = ""
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                navController.navigate(Routes.createThreadRoute(post.id))
            }
            .padding(16.dp)
    ) {
        // Header section
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = post.userProfileImageUrl,
                    contentDescription = "Profile Picture",
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(post.userName, fontWeight = FontWeight.Bold)
                    Text(
                        text = post.timestamp.toRelativeTimeString(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
            IconButton(
                onClick = { showOptionsMenu = true },
                modifier = Modifier.size(40.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    repeat(3) {
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.onSurface,
                                    shape = CircleShape
                                )
                        )
                        if (it != 2) Spacer(modifier = Modifier.height(2.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(post.content)

        // Image gallery
        if (post.imageUrls.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow {
                itemsIndexed(post.imageUrls) { index, imageUrl ->
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = "Post Image",
                        modifier = Modifier
                            .size(200.dp)
                            .padding(end = 8.dp)
                            .clickable {
                                initialPage = index
                                showImageViewer = true
                            },
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Like button
            IconButton(
                onClick = {
                    isLiked = !isLiked
                    postViewModel.likePost(post.id, isLiked)
                },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "Like",
                    tint = if (isLiked) Color.Red else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(22.dp)
                )
            }
            Text(
                text = "${post.likes}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(end = 16.dp)
            )

            // Comment button
            IconButton(
                onClick = {
                    navController.navigate(Routes.createReplyRoute(post.id, post.userName))
                },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Outlined.ChatBubbleOutline,
                    contentDescription = "Comment",
                    modifier = Modifier.size(22.dp)
                )
            }
            Text(
                text = "${post.comments}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(end = 16.dp)
            )

            // Repost button
            IconButton(
                onClick = {
                    if (isReposted) {
                        showRepostDialog = true
                    } else {
                        isReposted = true
                        postViewModel.repostPost(post.id)
                        haptic.performHapticFeedback(view)
                    }
                },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = if (isReposted) Icons.Filled.Repeat else Icons.Outlined.Repeat,
                    contentDescription = if (isReposted) "Remove Repost" else "Repost",
                    tint = repostIconColor,
                    modifier = Modifier
                        .size(22.dp)
                        .scale(repostIconScale)
                )
            }
            Text(
                text = "${post.reposts}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(end = 16.dp)
            )

            // Share button
            IconButton(
                onClick = {
                    // Implement share functionality
                },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Outlined.Share,
                    contentDescription = "Share",
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // Options dialog
        if (showOptionsMenu) {
            AlertDialog(
                onDismissRequest = { showOptionsMenu = false },
                confirmButton = {
                    TextButton(onClick = { postViewModel.deletePost(post.id); showOptionsMenu = false }) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showOptionsMenu = false }) {
                        Text("Cancel")
                    }
                },
                text = { Text("Do you want to delete this post?") }
            )
        }
    }

    // Repost dialog
    if (showRepostDialog) {
        AlertDialog(
            onDismissRequest = { showRepostDialog = false },
            icon = {
                Icon(
                    Icons.Filled.Repeat,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(32.dp)
                )
            },
            title = { Text("Remove Repost?") },
            text = { Text("This post will be removed from your profile's reposts.") },
            confirmButton = {
                Button(
                    onClick = {
                        isReposted = false
                        postViewModel.undoRepost(post.id)
                        showRepostDialog = false
                        haptic.performHapticFeedback(view)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    )
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRepostDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showRepostDialog) {
        AlertDialog(
            onDismissRequest = { showRepostDialog = false },
            icon = {
                Icon(
                    Icons.Filled.Repeat,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(32.dp)
                )
            },
            title = { Text("Remove Repost?") },
            text = { Text("This post will be removed from your profile's reposts.") },
            confirmButton = {
                Button(
                    onClick = {
                        isReposted = false
                        postViewModel.undoRepost(post.id)
                        showRepostDialog = false
                        haptic.performHapticFeedback(view)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    )
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRepostDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Image viewer
    if (showImageViewer) {
        FullScreenImageViewer(
            imageUrls = post.imageUrls,
            initialPage = initialPage,
            onDismiss = { showImageViewer = false }
        )
    }
}

//// Haptic feedback function
//private fun performHapticFeedback(context: Context) {
//    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
//    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//        vibrator.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE))
//    } else {
//        @Suppress("DEPRECATION")
//        vibrator.vibrate(40)
//    }
//}






@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FullScreenImageViewer(
    imageUrls: List<String>,
    initialPage: Int,
    onDismiss: () -> Unit
) {
    val pagerState = rememberPagerState(initialPage = initialPage) { imageUrls.size }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            HorizontalPager(
                state = pagerState
            ) { page ->
                AsyncImage(
                    model = imageUrls[page],
                    contentDescription = "Full-screen Image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) {
                Text(
                    text = "${pagerState.currentPage + 1} / ${imageUrls.size}",
                    color = Color.White
                )
            }
        }
    }
}
