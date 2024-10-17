package com.example.myapplication.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Search(modifier: Modifier = Modifier, navController: NavController) {
    var searchQuery by remember { mutableStateOf("") }
    val recentSearches = remember { listOf("Android", "Kotlin", "Jetpack Compose", "Firebase") }
    val trendingTopics = remember { listOf("Tech", "Programming", "AI", "Mobile Development") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Search Bar
        TextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder ={ Text("Search...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            colors = TextFieldDefaults.textFieldColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedIndicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedIndicatorColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(50)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Recent Searches
        Text("Recent Searches", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        LazyColumn(modifier = Modifier.height(120.dp)) {
            items(recentSearches) { search ->
                Text(
                    text = search,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { /* Handle click */ }
                        .padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Trending Topics
        Text("Trending Topics", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        LazyColumn(modifier = Modifier.fillMaxHeight()) {
            items(trendingTopics) { topic ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { /* Handle click */ }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.TrendingUp,
                        contentDescription = "Trending",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = topic)
                }
            }
        }
    }
}