package com.example.myapplication.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.myapplication.AuthViewModel
import com.example.myapplication.components.CustomIcon
import com.example.myapplication.components.CustomIconType
import com.example.myapplication.ui.theme.AppColors

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
            is AuthViewModel.AuthState.Unauthenticated -> {
                isLoggingOut = false
                navController.navigate("login") {
                    popUpTo(navController.graph.id) { inclusive = true }
                }
            }
            is AuthViewModel.AuthState.Error -> {
                isLoggingOut = false
                snackbarHostState.showSnackbar((authState as AuthViewModel.AuthState.Error).message)
            }
            else -> {}
        }
    }

    Scaffold(
        containerColor = AppColors.Surface,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Settings",
                        color = AppColors.TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            navController.navigateUp() // This will go back to the previous screen
                        }
                    ) {
                        CustomIcon(
                            iconType = CustomIconType.ARROW_BACK,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppColors.Surface,
                    navigationIconContentColor = AppColors.TextPrimary
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Surface(
            color = AppColors.Surface,
            modifier = Modifier.fillMaxSize()
        ) {
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
                        containerColor = AppColors.Error
                    ),
                    enabled = !isLoggingOut
                ) {
                    if (isLoggingOut) {
                        CircularProgressIndicator(
                            color = AppColors.White,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Text("Logout", color = AppColors.White)
                    }
                }
            }
        }
    }
}