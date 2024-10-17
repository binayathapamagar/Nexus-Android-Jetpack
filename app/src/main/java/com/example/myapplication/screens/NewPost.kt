import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.myapplication.AuthViewModel
import com.example.myapplication.PostViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewPost(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    postViewModel: PostViewModel
) {
    var postText by remember { mutableStateOf("") }
    var imageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    val profileImageUrl by authViewModel.profileImageUrl.collectAsState()
    val userName by authViewModel.userName.collectAsState()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        imageUris = uris
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "New Post",
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel")
                    }
                },
                actions = {
                    // Placeholder to balance the layout
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = Color.Transparent)
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = profileImageUrl,
                        contentDescription = "Profile Picture",
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = userName ?: "User")
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextField(
                    value = postText,
                    onValueChange = { postText = it },
                    placeholder = { Text("What's happening?") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.textFieldColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Display Selected Images
                if (imageUris.isNotEmpty()) {
                    LazyRow {
                        items(imageUris) { uri ->
                            AsyncImage(
                                model = uri,
                                contentDescription = "Selected Image",
                                modifier = Modifier
                                    .size(100.dp)
                                    .padding(end = 8.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Updated action icons row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { launcher.launch("image/*") },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.Image,
                            contentDescription = "Add Images",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    IconButton(
                        onClick = { /* Implement camera functionality */ },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.Camera,
                            contentDescription = "Take Photo",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    IconButton(
                        onClick = { /* Implement GIF functionality */ },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.Gif,
                            contentDescription = "Add GIF",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    IconButton(
                        onClick = { /* Implement voice recording functionality */ },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.Mic,
                            contentDescription = "Voice Recording",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    IconButton(
                        onClick = { /* Implement hashtag functionality */ },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.Tag,
                            contentDescription = "Add Hashtag",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    IconButton(
                        onClick = { /* Implement list functionality */ },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.List,
                            contentDescription = "Add List",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    IconButton(
                        onClick = { /* Implement location functionality */ },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = "Add Location",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // Post Button at bottom right
            Button(
                onClick = {
                    if (postText.isNotBlank() || imageUris.isNotEmpty()) {
                        postViewModel.createPost(
                            content = postText,
                            userName = userName ?: "User",
                            userProfileImageUrl = profileImageUrl ?: "",
                            imageUris = imageUris
                        )
                        navController.popBackStack()
                    }
                },
                enabled = postText.isNotBlank() || imageUris.isNotEmpty(),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
            ) {
                Text("Post", color = Color.White)
            }
        }
    }
}