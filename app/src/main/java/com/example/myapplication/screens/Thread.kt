package com.example.myapplication.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.myapplication.PostViewModel
import com.example.myapplication.PostWithReplies

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

    LaunchedEffect(postId) {
        postViewModel.fetchReplies(postId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Thread",
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