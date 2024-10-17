package com.example.nexusandroid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.navigation.compose.rememberNavController
import com.example.nexusandroid.navigation.NavGraph
import com.example.nexusandroid.ui.theme.NexusAndroidTheme
import com.google.firebase.FirebaseApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        setContent {
            NexusAndroidTheme {
                val navController = rememberNavController()
                Surface(
                    color = MaterialTheme.colorScheme.background,
                ) {
                    NavGraph(navController = navController)
                }
            }
        }
    }
}
