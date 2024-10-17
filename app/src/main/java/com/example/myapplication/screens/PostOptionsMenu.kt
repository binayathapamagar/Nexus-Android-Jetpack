package com.example.myapplication.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun PostOptionsMenu(
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    Column {
        Text("Delete", style = MaterialTheme.typography.bodyLarge, color = Color.Red,
            modifier = androidx.compose.ui.Modifier.clickable {
                onDelete()
                onDismiss()
            }.padding(16.dp)
        )
        Divider()
        Text("Cancel", style = MaterialTheme.typography.bodyLarge,
            modifier = androidx.compose.ui.Modifier.clickable {
                onDismiss()
            }.padding(16.dp)
        )
    }
}