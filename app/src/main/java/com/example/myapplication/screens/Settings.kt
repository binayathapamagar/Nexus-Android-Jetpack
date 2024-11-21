package com.example.myapplication.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.myapplication.AuthState
import com.example.myapplication.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Settings(
    navController: NavController,
    authViewModel: AuthViewModel
) {
    var isLoggingOut by remember { mutableStateOf(false) }
    val authState by authViewModel.authState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Unauthenticated -> {
                isLoggingOut = false
                navController.navigate("login") {
                    popUpTo(navController.graph.id) { inclusive = true }
                }
            }
            is AuthState.Error -> {
                isLoggingOut = false
                // Show error message
                snackbarHostState.showSnackbar((authState as AuthState.Error).message)
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Add other settings options here

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    isLoggingOut = true
                    authViewModel.logout()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ),
                enabled = !isLoggingOut
            ) {
                if (isLoggingOut) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onError,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text("Logout")
                }
            }
        }
    }
}