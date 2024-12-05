import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.myapplication.R
import com.example.myapplication.components.CustomIcon
import com.example.myapplication.components.CustomIconType

@Composable
fun ProfileAvatar(
    imageUrl: String,
    size: Int = 48,
    showFollowButton: Boolean = false,
    onFollowClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
    ) {
        AsyncImage(
            model = imageUrl.ifEmpty { R.drawable.person },
            contentDescription = "Profile Picture",
            modifier = Modifier
                .size(size.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )

        if (showFollowButton) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 4.dp, y = 4.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp
            ) {
                IconButton(
                    onClick = onFollowClick,
                    modifier = Modifier.size(20.dp)
                ) {
                    CustomIcon(
                        iconType = CustomIconType.FOLLOW,
                        modifier = Modifier.size(16.dp),
                        tint = Color.Black
                    )
                }
            }
        }
    }
}