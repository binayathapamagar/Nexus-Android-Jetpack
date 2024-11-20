package com.example.myapplication.utils

import java.util.*
import java.text.SimpleDateFormat


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