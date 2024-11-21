package com.example.myapplication.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavController
import com.example.myapplication.PostViewModel
import com.example.myapplication.PostWithReplies

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostReplies(
    navController: NavController,
    postViewModel: PostViewModel,
    postId: String,
    modifier: Modifier = Modifier
) {
    val post by postViewModel.getPost(postId).collectAsState(initial = null)
    val replies by postViewModel.replies.collectAsState()

    LaunchedEffect(postId) {
        postViewModel.fetchReplies(postId)
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
                            "Replies",
                            fontWeight = FontWeight.Bold
                        )
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
                modifier = modifier.padding(padding)
            )
        }
    }
}