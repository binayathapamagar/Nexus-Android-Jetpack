package com.example.myapplication.utils

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

object ShareUtils {
    fun shareProfile(context: Context, username: String?, name: String?, bio: String?) {
        val shareText = buildString {
            append("Check out ${name ?: "this profile"} on Nexus\n\n")
            if (!username.isNullOrEmpty()) {
                append("@$username\n")
            }
            if (!bio.isNullOrEmpty()) {
                append("$bio\n\n")
            }
            append("https://nexus.com/profile/$username")
        }

        val shareIntent = Intent.createChooser(Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }, "Share Profile")

        ContextCompat.startActivity(context, shareIntent, null)
    }
}