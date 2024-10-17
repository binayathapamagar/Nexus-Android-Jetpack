package com.example.nexusandroid.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.navigation.NavController
import com.example.nexusandroid.R
import com.example.nexusandroid.navigation.Routes
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay

@Composable
fun Splash(navController: NavController) {
    ConstraintLayout(modifier = Modifier.fillMaxSize()) {
        val (image) = createRefs()

        Image(
            painter = painterResource(id = R.drawable.logo),
            contentDescription = "Logo",
            modifier = Modifier
                .constrainAs(image) {
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
                .size(120.dp),
            contentScale = ContentScale.Fit
        )
    }

    // Ensure that the effect only runs once when the composable is first launched
    LaunchedEffect(Unit) {
        delay(3000)  // Delay for splash screen

        // Check if the user is logged in and navigate accordingly
        if (FirebaseAuth.getInstance().currentUser != null) {
            navController.navigate(Routes.BottomNav.routes) {
                popUpTo(navController.graph.startDestinationId)
                launchSingleTop = true
            }
        } else {
            navController.navigate(Routes.Login.routes) {

                    popUpTo(navController.graph.startDestinationId)
                    launchSingleTop = true

            }
        }
    }
}
