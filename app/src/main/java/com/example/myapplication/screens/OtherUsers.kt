package com.example.myapplication.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun OtherUserScreen(userId: String?, navController: NavController) {
    if (userId.isNullOrEmpty()) {
        Toast.makeText(LocalContext.current, "User not found", Toast.LENGTH_SHORT).show()
        navController.popBackStack()
    } else {
        // You can fetch user details using the userId here
        // For now, we're just displaying the userId as a placeholder

        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("User Details for User ID: $userId")

            Spacer(modifier = Modifier.height(20.dp))

            // Display user details here (e.g., username, bio, profile picture)
            Button(
                onClick = { /* Implement action like follow or message */ },
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text("Follow")
            }
        }
    }
}
