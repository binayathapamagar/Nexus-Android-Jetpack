package com.example.myapplication.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.myapplication.AuthState
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
            is AuthState.Unauthenticated -> {
                isLoggingOut = false
                navController.navigate("login") {
                    popUpTo(navController.graph.id) { inclusive = true }
                }
            }
            is AuthState.Error -> {
                isLoggingOut = false
                snackbarHostState.showSnackbar((authState as AuthState.Error).message)
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