package com.example.myapplication.screens

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.myapplication.components.CustomIcon
import com.example.myapplication.components.CustomIconType
import com.example.myapplication.components.FollowStats
import com.example.myapplication.components.ShimmerListItem
import com.example.myapplication.navigation.Routes
import com.example.myapplication.ui.theme.AppColors
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await


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
    val posts by postViewModel.posts.collectAsState()
    val userPosts = posts.filter { it.userId == userId }
    val profileLink by authViewModel.profileLink.collectAsState()
    var showShareSheet by remember { mutableStateOf(false) }
    val userReplies by postViewModel.userReplies.collectAsState()
    val userReposts by postViewModel.userReposts.collectAsState()

    var isLoading by remember { mutableStateOf(true) }
    var followersCount by remember { mutableIntStateOf(0) } // Declare followersCount here


    val listState = rememberLazyListState()
    val isScrolled by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0
        }
    }
    // Fetch followers count from Firestore
    LaunchedEffect(userId) {
        try {
            val statsRef = FirebaseFirestore.getInstance().collection("userStats").document(userId)
            val stats = statsRef.get().await()
            followersCount = stats.getLong("followersCount")?.toInt() ?: 0
        } catch (e: Exception) {
            Log.e("Profile", "Error fetching followers count: ${e.message}")
        }
        isLoading = false
    }

    LaunchedEffect(userId, selectedTab) {
        isLoading = true
        when (selectedTab) {
            0 -> postViewModel.fetchPosts()
            1 -> postViewModel.fetchUserReplies(userId)
            2 -> postViewModel.fetchUserReposts(userId)
        }
        delay(1500)
        isLoading = false
    }

    Scaffold(
        containerColor = AppColors.Surface,
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = { /* Handle language selection */ }) {
                        CustomIcon(
                            iconType = CustomIconType.LANGUAGE,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            try {
                                parentNavController.navigate("settings")
                            } catch (e: Exception) {
                                Log.e("Profile", "Error navigating to settings: ${e.message}")
                            }
                        }
                    ) {
                        CustomIcon(
                            iconType = CustomIconType.MENU,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppColors.Surface,
                    navigationIconContentColor = AppColors.TextPrimary,
                    actionIconContentColor = AppColors.TextPrimary
                )
            )
        }
    ) { paddingValues ->
        Surface(
            color = AppColors.Surface,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
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
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = AppColors.TextPrimary
                                )
                                Text(
                                    text = userHandle ?: "user_handle",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = AppColors.TextSecondary
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
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppColors.TextPrimary
                        )

                        if (!profileLink.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            val context = LocalContext.current
                            Text(
                                text = profileLink!!,
                                style = MaterialTheme.typography.bodyMedium,
                                color = AppColors.Primary,
                                textDecoration = TextDecoration.Underline,
                                modifier = Modifier.clickable {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(profileLink))
                                    context.startActivity(intent)
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            followersCount.let {
                                FollowStats(
                                    followersCount = it,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    try {
                                        parentNavController.navigate("EDIT_PROFILE") {
                                            launchSingleTop = true
                                        }
                                    } catch (e: Exception) {
                                        Log.e("Profile", "Error navigating to edit profile: ${e.message}")
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                border = BorderStroke(1.dp, AppColors.Border),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Edit profile", color = AppColors.TextPrimary)
                            }

                            OutlinedButton(
                                onClick = { showShareSheet = true },
                                modifier = Modifier.weight(1f),
                                border = BorderStroke(1.dp, AppColors.Border),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Share profile", color = AppColors.TextPrimary)
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }

                // Sticky Header with Tabs
                stickyHeader {
                    Surface(
                        color = AppColors.Surface,
                        shadowElevation = if (isScrolled) 1.dp else 0.dp
                    ) {
                        TabRow(
                            selectedTabIndex = selectedTab,
                            containerColor = AppColors.Surface,
                            contentColor = AppColors.TextPrimary,
                            indicator = { tabPositions ->
                                SecondaryIndicator(
                                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                    color = AppColors.Primary
                                )
                            },
                            divider = {
                                HorizontalDivider(color = AppColors.Divider)
                            }
                        ) {
                            listOf("Threads", "Replies", "Reposts").forEachIndexed { index, title ->
                                Tab(
                                    selected = selectedTab == index,
                                    onClick = { selectedTab = index },
                                    modifier = Modifier.background(AppColors.Surface)
                                ) {
                                    Text(
                                        text = title,
                                        fontWeight = if (selectedTab == index)
                                            FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier.padding(vertical = 12.dp),
                                        color = if (selectedTab == index)
                                            AppColors.TextPrimary
                                        else
                                            AppColors.TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                // Content based on selected tab
                when (selectedTab) {
                    0 -> {
                        if (isLoading) {
                            items(3) {
                                ShimmerListItem()
                                HorizontalDivider(color = AppColors.Divider)
                            }
                        } else {
                            items(userPosts) { post ->
                                PostItem(
                                    post = post,
                                    postViewModel = postViewModel,
                                    navController = parentNavController,
                                    modifier = Modifier
                                        .padding(horizontal = 16.dp)
                                        .clickable {
                                            parentNavController.navigate(Routes.createThreadRoute(post.id))
                                        }
                                )
                                HorizontalDivider(color = AppColors.Divider)
                            }
                        }
                    }
                    1 -> {
                        if (isLoading) {
                            items(3) {
                                ShimmerListItem()
                                HorizontalDivider(color = AppColors.Divider)
                            }
                        } else {
                            items(userReplies) { (post, reply) ->
                                UserReplyItem(
                                    post = post,
                                    reply = reply,
                                    postViewModel = postViewModel,
                                    navController = parentNavController,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                                HorizontalDivider(color = AppColors.Divider)
                            }
                        }
                    }
                    2 -> {
                        if (isLoading) {
                            items(3) {
                                ShimmerListItem()
                                HorizontalDivider(color = AppColors.Divider)
                            }
                        } else {
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
                                HorizontalDivider(color = AppColors.Divider)
                            }
                        }
                    }
                }}}

            }

    if (showShareSheet) {
        ShareBottomSheet(
            isVisible = true,
            onDismiss = { showShareSheet = false },
            username = userHandle,
            name = userName,
            bio = userBio
        )
    }
}

