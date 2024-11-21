package com.example.myapplication.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.myapplication.utils.ShareUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareBottomSheet(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    username: String?,
    name: String?,
    bio: String?
) {
    if (isVisible) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    "Share Profile",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                val context = LocalContext.current

                // Copy Link Option
                ListItem(
                    headlineContent = { Text("Copy profile link") },
                    leadingContent = {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = "Copy Link"
                        )
                    },
                    modifier = Modifier.clickable {
                        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clipData = ClipData.newPlainText(
                            "Profile Link",
                            "https://nexus.com/profile/$username"
                        )
                        clipboardManager.setPrimaryClip(clipData)
                        Toast.makeText(context, "Link copied to clipboard", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    }
                )

                // Share Option
                ListItem(
                    headlineContent = { Text("Share via...") },
                    leadingContent = {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "Share"
                        )
                    },
                    modifier = Modifier.clickable {
                        ShareUtils.shareProfile(context, username, name, bio)
                        onDismiss()
                    }
                )

                // QR Code Option
                ListItem(
                    headlineContent = { Text("QR code") },
                    leadingContent = {
                        Icon(
                            Icons.Default.QrCode2,
                            contentDescription = "QR Code"
                        )
                    },
                    modifier = Modifier.clickable {
                        // TODO: Implement QR code generation and display
                        onDismiss()
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}