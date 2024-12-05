package com.example.myapplication.transitions

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.EaseInOutCirc
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.navigation.NavBackStackEntry

object CustomTransitions {
    // Duration for the animations
    private const val ANIMATION_DURATION = 400

    fun enterTransition(duration: Int = ANIMATION_DURATION): EnterTransition {
        return slideInVertically(
            animationSpec = tween(
                durationMillis = duration,
                easing = EaseInOutCirc
            ),
            initialOffsetY = { fullHeight -> fullHeight }
        ) + fadeIn(
            animationSpec = tween(
                durationMillis = duration,
                easing = EaseInOutCirc
            )
        )
    }

    fun exitTransition(duration: Int = ANIMATION_DURATION): ExitTransition {
        return slideOutVertically(
            animationSpec = tween(
                durationMillis = duration,
                easing = EaseInOutCirc
            ),
            targetOffsetY = { fullHeight -> -fullHeight }
        ) + fadeOut(
            animationSpec = tween(
                durationMillis = duration,
                easing = EaseInOutCirc
            )
        )
    }

    fun popEnterTransition(duration: Int = ANIMATION_DURATION): EnterTransition {
        return slideInVertically(
            animationSpec = tween(
                durationMillis = duration,
                easing = EaseInOutCirc
            ),
            initialOffsetY = { fullHeight -> -fullHeight }
        ) + fadeIn(
            animationSpec = tween(
                durationMillis = duration,
                easing = EaseInOutCirc
            )
        )
    }

    fun popExitTransition(duration: Int = ANIMATION_DURATION): ExitTransition {
        return slideOutVertically(
            animationSpec = tween(
                durationMillis = duration,
                easing = EaseInOutCirc
            ),
            targetOffsetY = { fullHeight -> fullHeight }
        ) + fadeOut(
            animationSpec = tween(
                durationMillis = duration,
                easing = EaseInOutCirc
            )
        )
    }
}