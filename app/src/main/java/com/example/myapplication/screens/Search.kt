package com.example.myapplication.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.myapplication.AuthViewModel
import com.example.myapplication.R
import com.example.myapplication.components.FollowButton
import com.example.myapplication.models.User
import com.example.myapplication.viewmodels.FollowViewModel
import com.example.myapplication.viewmodels.NotificationViewModel
import com.google.firebase.auth.FirebaseAuth


@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SearchScreen(
    modifier: Modifier = Modifier,
    navController: NavController,
    authViewModel: AuthViewModel,
    followViewModel: FollowViewModel
) {
    // Mutable state for the search query
    var searchQuery by remember { mutableStateOf("") }

    // Fetch users from AuthViewModel
    val users by authViewModel.users.collectAsState(initial = emptyList())

    // Firebase Authentication instance to get the current user
    val currentUser = FirebaseAuth.getInstance().currentUser
    val currentUserId = currentUser?.uid

    // Filter out the current user
    val filteredUsers = remember(users, currentUserId) {
        users.filter { user -> user.id != currentUserId }
    }

    // Filtered search results based on the search query
    val searchResults = remember(searchQuery, filteredUsers) {
        filteredUsers.filter { user ->
            searchQuery.isEmpty() ||
                    user.username.contains(searchQuery, ignoreCase = true) ||
                    user.fullName.contains(searchQuery, ignoreCase = true)
        }
    }

    // Ensure the user is authenticated
    val context = LocalContext.current
    LaunchedEffect(users, currentUserId, currentUser) {
        if (currentUser == null) {
            Toast.makeText(context, "Please log in to search users.", Toast.LENGTH_SHORT).show()
            navController.navigate("login") // Navigate to login screen
        }
    }

    // UI elements
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Search Bar
        @OptIn(ExperimentalMaterial3Api::class)
        TextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search users...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Color.White,
                    RoundedCornerShape(50)
                ), // Ensure the background color is white
            shape = RoundedCornerShape(50),
            colors = TextFieldDefaults.textFieldColors(
                containerColor = Color.LightGray, // Explicitly set the background color to white
                focusedIndicatorColor = Color.Transparent, // Removes the focused underline
                unfocusedIndicatorColor = Color.Transparent // Removes the unfocused underline
            ),
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    // Hide the keyboard and clear focus when the search button is pressed
                    keyboardController?.hide()
                    focusManager.clearFocus()
                }
            )
        )


        Spacer(modifier = Modifier.height(16.dp))

        // Show users if no query is entered
        val displayUsers = if (searchQuery.isEmpty()) filteredUsers else searchResults

        if (displayUsers.isEmpty()) {
            Text("No users found.", style = MaterialTheme.typography.bodyLarge)
        } else {
            // Display the users
            LazyColumn {
                items(displayUsers) { user ->
                    UserRow(
                        user = user, navController = navController,
                        followViewModel = followViewModel
                    )
                }
            }
        }
    }
}

@Composable
fun UserRow(user: User, navController: NavController, followViewModel: FollowViewModel) {

    val followStatus by followViewModel.followStatus.collectAsState()
    val isFollowing = followStatus[user.id] ?: false
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .height(60.dp) // Reduced height for the row
                .clickable {
                    // Navigate to the user's profile, passing the user ID
                    navController.navigate("otherUsers/${user.id}")
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile Picture with Circular Border - Using AsyncImage
            AsyncImage(
                model = user.profileImageUrl?.takeIf { it.isNotBlank() }
                    ?: R.drawable.person, // Fallback to person image if URL is empty or null
                contentDescription = "User Profile Picture",
                modifier = Modifier
                    .size(40.dp) // Reduced size of the profile picture
                    .clip(CircleShape), // Circular shape for the image
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp)) // Reduced space between image and text

            // Username, Full Name, and Followers
            Column(
                modifier = Modifier
                    .weight(1f) // Ensures the text takes up available space and pushes the button to the right
            ) {
                // Username in dark color
                Text(
                    text = user.username,
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color.Black) // Dark color without bold
                )
                Spacer(modifier = Modifier.height(2.dp)) // Reduced spacing
                // Full Name in lighter grey
                Text(
                    text = user.fullName,
                    style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray) // Lighter grey color for the name
                )
                Spacer(modifier = Modifier.height(2.dp)) // Reduced spacing
                // Followers in dark color
                Text(
                    text = "${user.followerCount} Followers",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color.Black) // Dark color without bold
                )
            }

            Spacer(modifier = Modifier.width(8.dp)) // Reduced space between text and button

            // Follow Button with Original Border and Black Text
            FollowButton(
                userId = user.id,
                isFollowing = isFollowing,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .height(36.dp) // Reduced height for the button
                    .widthIn(min = 80.dp) // Set a minimum width for the button
                ,
                followViewModel = followViewModel,
                notificationViewModel = NotificationViewModel()
            )
        }

        // Divider after the profile picture and user details, spanning the full width of the screen
        HorizontalDivider(
            modifier = Modifier
                .fillMaxWidth() // Ensures the divider spans the full width
                .padding(start = 56.dp), // Offset the divider to start after the profile picture
            thickness = 0.5.dp,
            color = Color.Gray
        )
    }
}
