package com.example.myapplication.screens

import android.util.Log
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.myapplication.AuthViewModel
import com.example.myapplication.PostViewModel
import com.example.myapplication.R
import com.example.myapplication.components.CustomIcon
import com.example.myapplication.components.CustomIconType
import com.example.myapplication.components.ShimmerListItem
import com.example.myapplication.models.NotificationType
import com.example.myapplication.models.Post
import com.example.myapplication.navigation.Routes
import com.example.myapplication.ui.theme.AppColors
import com.example.myapplication.ui.theme.InterFontFamily
import com.example.myapplication.utils.toRelativeTimeString
import com.example.myapplication.viewmodels.NotificationViewModel
import kotlinx.coroutines.delay

//@OptIn(ExperimentalMaterial3Api::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Home(
    navController: NavController,
    authViewModel: AuthViewModel,
    postViewModel: PostViewModel = viewModel()
) {
    val posts by postViewModel.posts.collectAsState()
    var isLoading by remember { mutableStateOf(true) }
    val listState = rememberLazyListState()
    var isFabPressed by remember { mutableStateOf(false) }

    // Animation values for FAB
    val scale by animateFloatAsState(
        targetValue = if (isFabPressed) 0.8f else 1f,
        animationSpec = spring(
            dampingRatio = 0.4f,
            stiffness = Spring.StiffnessLow
        ),
        label = ""
    )

    LaunchedEffect(Unit) {
        delay(1500) // Loading simulation
        postViewModel.fetchPosts()
        isLoading = false
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = AppColors.White,
            topBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.logo),
                        contentDescription = "App Logo",
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isLoading) {
                    items(9) {
                        ShimmerListItem()
                        HorizontalDivider(
                            thickness = 1.dp,
                            color = AppColors.Divider
                        )
                    }
                } else {
                    items(posts) { post ->
                        PostItem(
                            post = post,
                            postViewModel = postViewModel,
                            navController = navController
                        )
                        HorizontalDivider(
                            thickness = 1.dp,
                            color = AppColors.Divider
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(64.dp))
                }
            }
        }

        // Floating Action Button
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .scale(scale)
                .clip(CircleShape)
                .clickable {
                    isFabPressed = true
                    // Add small delay before navigation
                    navController.navigate(Routes.NEW_POST)
                }
//                .background(Color.Black)
                .size(55.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.add4),
                contentDescription = "New Post",
                tint = Color.Black,
                modifier = Modifier.size(70.dp)
            )
        }
    }

    // Reset FAB press state after animation
    LaunchedEffect(isFabPressed) {
        if (isFabPressed) {
            delay(100)
            isFabPressed = false
        }
    }
}



//@Composable
//private fun ActionIcon(
//    icon: ImageVector,
//    contentDescription: String,
//    onClick: () -> Unit
//) {
//    Icon(
//        icon,
//        contentDescription = contentDescription,
//        modifier = Modifier
//            .clickable(onClick = onClick),
//        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
//    )
//}


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

//@Composable
//fun rememberHapticFeedback() = remember {
//    HapticFeedback()
//}

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
    val notificationViewModel: NotificationViewModel = viewModel()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)

    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // Left column for profile picture and vertical line
            Box(
                modifier = Modifier.width(72.dp)
                    .clickable { // Add clickable to profile picture
                        navController.navigate("otherUsers/${post.userId}")
                    }
            ) {
                // Profile picture
                Box(
                    modifier = Modifier
                        .padding(start = 12.dp)
                ) {
                    AsyncImage(
                        model = post.userProfileImageUrl,
                        contentDescription = "Profile Picture",
                        modifier = Modifier
//                            .padding(start = 3.dp) // Single padding from the left edge
                            .size(48.dp) // Standard size used by Threads/Twitter
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop,
                        error = painterResource(id = R.drawable.person)
                    )
                }

                // Vertical line under profile picture with 21dp spacing
                Box(
                    modifier = Modifier
                        .padding(start = 41.dp) // Centered with profile pic (16 + 22 - 1)
                        .padding(top = 69.dp) // 44dp (profile) + 21dp spacing
                        .width(2.dp)
                        .fillMaxHeight()
                        .background(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(1.dp)
                        )
                )
            }

            // Right column for content with 8dp spacing from profile
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp, end = 16.dp)
                    .clickable { navController.navigate(Routes.createThreadRoute(post.id)) }
            ) {
                // Username row - Keep the existing clickable on username
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {

                        Text(
                            text = post.userName,
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            modifier = Modifier
                                .clickable {
                                    navController.navigate(Routes.createOtherUserRoute(post.userId))
                                }
                                .padding(vertical = 4.dp)
                        )

                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "· ${post.timestamp.toRelativeTimeString()}",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 14.sp,
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

                // Post content with 4dp spacing from header
                if (post.content.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = post.content,
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 16.sp
                    )
                }

                // Media content with 8dp spacing
                if (post.imageUrls.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    when (post.imageUrls.size) {
                        1 -> SingleImage(post.imageUrls[0]) { showImageViewer = true }
                        2 -> TwoImages(post.imageUrls) { index ->
                            initialPage = index
                            showImageViewer = true
                        }
                        else -> MultipleImages(post.imageUrls) { index ->
                            initialPage = index
                            showImageViewer = true
                        }
                    }
                }

                // Action buttons with 12dp spacing from content
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
//                        .padding(top = 8.dp)
                        .padding(end = 12.dp), // Align with content
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Like button
                    ActionButton(
                        isActive = isLiked,
                        count = post.likes,
                        iconType = CustomIconType.LIKE,
                        onClick = {
                            isLiked = !isLiked
                            postViewModel.likePost(post.id, isLiked)

                            try{
                            notificationViewModel.saveNotification(
                                recipientID = post.userId,
                                actionType = NotificationType.LIKE,
                                postId = post.id,
                                postContent = post.content

                            )} catch (e: Exception) {
                        Log.e("NotificationError", "Error saving notification: ${e.message}")
                    }

                        }
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Comment button
                    ActionButton(
                        count = post.comments,
                        iconType = CustomIconType.COMMENT,
                        onClick = {
                            navController.navigate(Routes.createReplyRoute(post.id, post.userName))
                        }
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Repost button
                    ActionButton(
                        isActive = isReposted,
                        activeColor = Color(0xFF4CAF50),
                        count = post.reposts,
                        iconType = CustomIconType.REPOST,
                        onClick = {
                            if (isReposted) {
                                showRepostDialog = true
                            } else {
                                postViewModel.repostPost(post.id)
                                isReposted = true
                            }
                        }
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Share button
                    IconButton(
                        onClick = { /* Implement share */ },
                        modifier = Modifier.size(40.dp)
                    ) {
                        CustomIcon(
                            iconType = CustomIconType.SHARE,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
    }

    // Dialogs and image viewer remain unchanged
    if (showOptionsMenu) {
        AlertDialog(
            onDismissRequest = { showOptionsMenu = false },
            confirmButton = {
                TextButton(onClick = {
                    postViewModel.deletePost(post.id)
                    showOptionsMenu = false
                }) {
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
                        postViewModel.undoRepost(post.id)
                        isReposted = false
                        showRepostDialog = false
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

    if (showImageViewer) {
        FullScreenImageViewer(
            imageUrls = post.imageUrls,
            initialPage = initialPage,
            onDismiss = { showImageViewer = false }
        )
    }
}

@Composable
private fun ActionButton(
    isActive: Boolean = false,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    count: Int,
    iconType: CustomIconType,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(end = 16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            if (iconType == CustomIconType.LIKE) {
                Icon(
                    painter = painterResource(
                        id = if (isActive) R.drawable.hearted3 else R.drawable.heart
                    ),
                    contentDescription = "Like",
                    modifier = Modifier.size(22.dp),
                    tint = if (isActive) Color.Red else MaterialTheme.colorScheme.onSurface
                )
            } else {
                CustomIcon(
                    iconType = iconType,
                    tint = if (isActive) activeColor else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        if (count > 0) {
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "$count",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun SingleImage(
    imageUrl: String,
    onClick: () -> Unit
) {
    AsyncImage(
        model = imageUrl,
        contentDescription = "Post Image",
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentScale = ContentScale.Crop
    )
}

@Composable
private fun TwoImages(
    imageUrls: List<String>,
    onImageClick: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        imageUrls.take(2).forEachIndexed { index, url ->
            AsyncImage(
                model = url,
                contentDescription = "Post Image",
                modifier = Modifier
                    .weight(1f)
                    .height(300.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onImageClick(index) },
                contentScale = ContentScale.Crop
            )
        }
    }
}

@Composable
private fun MultipleImages(
    imageUrls: List<String>,
    onImageClick: (Int) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        AsyncImage(
            model = imageUrls[0],
            contentDescription = "Post Image",
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable { onImageClick(0) },
            contentScale = ContentScale.Crop
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            imageUrls.drop(1).take(3).forEachIndexed { index, url ->
                AsyncImage(
                    model = url,
                    contentDescription = "Post Image",
                    modifier = Modifier
                        .weight(1f)
                        .height(150.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onImageClick(index + 1) },
                    contentScale = ContentScale.Crop
                )
            }
        }
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
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // Horizontal pager for displaying images
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

            // Close button at the top right
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White
                )
            }

            // Page indicator at the bottom center
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