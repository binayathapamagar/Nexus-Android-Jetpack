package com.example.myapplication.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.myapplication.PostViewModel
import com.example.myapplication.PostWithReplies
import com.example.myapplication.components.ShimmerListItem
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Thread(
    navController: NavController,
    postViewModel: PostViewModel,
    postId: String,
    modifier: Modifier = Modifier
) {
    val post by postViewModel.getPost(postId).collectAsState(initial = null)
    val replies by postViewModel.replies.collectAsState()
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(postId) {
        isLoading = true
        postViewModel.fetchReplies(postId)
        delay(500) // Add small delay to ensure loading state is visible
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(end = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Thread", fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (post != null) {
            PostWithReplies(
                post = post!!,
                replies = replies,
                postViewModel = postViewModel,
                navController = navController,
                modifier = modifier.padding(padding),
                isLoading = isLoading
            )
        }
    }
}