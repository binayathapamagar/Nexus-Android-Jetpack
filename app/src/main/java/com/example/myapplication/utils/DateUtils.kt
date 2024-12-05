package com.example.myapplication.utils

import com.google.firebase.Timestamp
import java.util.*
import java.text.SimpleDateFormat

// Extension function for Date to convert to a relative time string
fun Date?.toRelativeTimeString(): String {
    if (this == null) return "Unknown time"

    val now = Date()
    val diff = now.time - this.time
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
        seconds < 60 -> "Just now"
        minutes < 60 -> "${minutes}m"
        hours < 24 -> "${hours}h"
        days < 30 -> "${days}d"
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(this)
    }
}

// Extension function for Firebase Timestamp to convert to a relative time string
fun Timestamp?.toRelativeTimeString(): String {
    if (this == null) return "Unknown time"
    return this.toDate().toRelativeTimeString()
}
