package com.example.myapplication.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.Icon
import com.example.myapplication.R

@Composable
fun CustomIcon(
    iconType: CustomIconType,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
    contentDescription: String? = null
) {
    val iconRes = when (iconType) {
        CustomIconType.LIKE -> R.drawable.heart
        CustomIconType.COMMENT -> R.drawable.messagetext1
        CustomIconType.REPOST -> R.drawable.repeat
        CustomIconType.SHARE -> R.drawable.send2
        CustomIconType.SETTINGS -> R.drawable.more
        CustomIconType.HOME -> R.drawable.home
        CustomIconType.HOME2 -> R.drawable.home2
        CustomIconType.SEARCH -> R.drawable.searchnormal1
        CustomIconType.SEARCH_ON -> R.drawable.search3
        CustomIconType.ADD -> R.drawable.exportsquare
        CustomIconType.NOTIFICATION -> R.drawable.frame_12
        CustomIconType.NOTIFICATION_INACTIVE -> R.drawable.heart1
        CustomIconType.NOTIFICATION_ON -> R.drawable.hearted3
        CustomIconType.PROFILE -> R.drawable.user3
        CustomIconType.PROFILE_ON -> R.drawable.user
        CustomIconType.BACK -> R.drawable.arrowleft
        CustomIconType.LANGUAGE -> R.drawable.global
        CustomIconType.MENU -> R.drawable.hambergermenu
        CustomIconType.ARROW_BACK -> R.drawable.arrowleft
        CustomIconType.FOLLOW -> R.drawable.follow
    }

    Icon(
        painter = painterResource(id = iconRes),
        contentDescription = contentDescription,
        modifier = modifier,
        tint = tint
    )
}

enum class CustomIconType {
    LIKE,
    COMMENT,
    REPOST,
    SHARE,
    SETTINGS,
    HOME,
    SEARCH,
    ADD,
    NOTIFICATION,
    NOTIFICATION_INACTIVE,
    PROFILE,
    BACK,
    LANGUAGE,
    MENU,
    ARROW_BACK,
    HOME2,
    NOTIFICATION_ON,
    SEARCH_ON,
    PROFILE_ON,
    FOLLOW
}