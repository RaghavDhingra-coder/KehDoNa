package com.codecrafters.kehdona.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

/**
 * Fades and lifts its content in the first time it is composed. Durations follow the
 * system animator scale, so "Remove animations" in accessibility settings disables it.
 */
@Composable
fun EnterReveal(
    modifier: Modifier = Modifier,
    delayMillis: Int = 0,
    content: @Composable () -> Unit,
) {
    val state = remember { MutableTransitionState(false).apply { targetState = true } }
    AnimatedVisibility(
        visibleState = state,
        modifier = modifier,
        enter = fadeIn(tween(700, delayMillis)) +
            slideInVertically(tween(700, delayMillis)) { it / 10 },
    ) {
        content()
    }
}
