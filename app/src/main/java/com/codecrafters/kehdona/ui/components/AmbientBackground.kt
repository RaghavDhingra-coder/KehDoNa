package com.codecrafters.kehdona.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.codecrafters.kehdona.ui.theme.Ink
import com.codecrafters.kehdona.ui.theme.Night950
import com.codecrafters.kehdona.ui.theme.Plum800
import com.codecrafters.kehdona.ui.theme.Rose500
import com.codecrafters.kehdona.ui.theme.Violet600

/**
 * Dark aubergine canvas with soft plum, violet and rose light. Screens use it as their
 * root, so it also sets the default text/icon colour for everything inside.
 */
@Composable
fun AmbientBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Night950)
            .drawBehind {
                val w = size.width
                val h = size.height
                val longest = maxOf(w, h)
                drawRect(
                    Brush.radialGradient(
                        listOf(Plum800, Color.Transparent),
                        center = Offset(w * 0.9f, 0f),
                        radius = longest * 0.75f,
                    ),
                )
                drawRect(
                    Brush.radialGradient(
                        listOf(Violet600.copy(alpha = 0.3f), Color.Transparent),
                        center = Offset(0f, h),
                        radius = longest * 0.6f,
                    ),
                )
                drawRect(
                    Brush.radialGradient(
                        listOf(Rose500.copy(alpha = 0.14f), Color.Transparent),
                        center = Offset(w * 0.75f, h * 0.35f),
                        radius = longest * 0.35f,
                    ),
                )
            },
    ) {
        CompositionLocalProvider(LocalContentColor provides Ink) { content() }
    }
}
