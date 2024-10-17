// Search.kt
package com.example.myapplication.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.myapplication.dataclass.User
import com.example.myapplication.viewmodel.AuthViewModel
import com.example.myapplication.viewmodel.UserViewModel
import com.example.myapplication.viewmodel.SearchViewModel

@Composable
fun Search(
    navController: NavController,
    authViewModel: AuthViewModel,
    searchViewModel: SearchViewModel // Pass the SearchViewModel as a parameter
) {
    val searchResults by searchViewModel.searchResults.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        TextField(
            value = searchQuery,
            onValueChange = {
                searchQuery = it
                searchViewModel.searchUsers(searchQuery) // Trigger search on query change
            },
            label = { Text("Search") },
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Search
            ),
            keyboardActions = KeyboardActions(
                onSearch = {
                    searchViewModel.searchUsers(searchQuery) // Trigger search on search action
                }
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn {
            items(searchResults) { user ->
                UserItem(user, navController)
            }
        }
    }
}

@Composable
fun UserItem(user: User, navController: NavController) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable {
                // Handle user item click, e.g., navigate to user profile
                navController.navigate("userProfile/${user.id}") // Example of navigation
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = user.fullName, fontSize = 20.sp)
        Spacer(modifier = Modifier.weight(1f))
        Text(text = "@${user.username}", fontSize = 16.sp)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = "${user.followerCount} followers", fontSize = 16.sp)
    }
}
