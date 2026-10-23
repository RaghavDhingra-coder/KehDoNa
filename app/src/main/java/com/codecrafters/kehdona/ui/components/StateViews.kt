package com.codecrafters.kehdona.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.codecrafters.kehdona.ui.theme.ErrorRose
import com.codecrafters.kehdona.ui.theme.InkMuted
import com.codecrafters.kehdona.ui.theme.Lavender300
import com.codecrafters.kehdona.ui.theme.Rose300

/** Full-area loading indicator with an optional message. */
@Composable
fun LoadingState(
    modifier: Modifier = Modifier,
    message: String? = null,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        HeartMark(size = 40.dp)
        Spacer(Modifier.height(20.dp))
        CircularProgressIndicator(
            modifier = Modifier.size(28.dp),
            color = Rose300,
            trackColor = Rose300.copy(alpha = 0.15f),
            strokeWidth = 2.5.dp,
        )
        if (message != null) {
            Spacer(Modifier.height(16.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium, color = InkMuted)
        }
    }
}

/** Something went wrong; explains what and offers a retry when possible. */
@Composable
fun ErrorState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
    retryLabel: String = "Try again",
    secondaryAction: (@Composable () -> Unit)? = null,
) {
    MessageState(
        icon = Icons.Filled.Warning,
        iconTint = ErrorRose,
        title = title,
        message = message,
        modifier = modifier.semantics { liveRegion = LiveRegionMode.Assertive },
    ) {
        if (onRetry != null) {
            PrimaryButton(text = retryLabel, onClick = onRetry, trailingIcon = Icons.Filled.Refresh)
        }
        secondaryAction?.invoke()
    }
}

/** Nothing to show yet; explains why and optionally offers a next step. */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    badge: String? = null,
    action: (@Composable () -> Unit)? = null,
) {
    MessageState(
        icon = icon,
        iconTint = Lavender300,
        title = title,
        message = message,
        modifier = modifier,
        badge = badge,
    ) {
        action?.invoke()
    }
}

@Composable
private fun MessageState(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    badge: String? = null,
    action: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier.widthIn(max = 420.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            IconBadge(icon = icon, tint = iconTint, size = 64.dp)
            if (badge != null) {
                Spacer(Modifier.height(20.dp))
                Badge(text = badge, tone = BadgeTone.Lavender)
            }
            Spacer(Modifier.height(20.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = InkMuted,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(28.dp))
            action()
        }
    }
}
