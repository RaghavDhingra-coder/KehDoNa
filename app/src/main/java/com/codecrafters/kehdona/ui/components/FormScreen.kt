package com.codecrafters.kehdona.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.codecrafters.kehdona.ui.theme.InkMuted
import com.codecrafters.kehdona.ui.theme.Line

private val CardBreakpoint = 600.dp

/**
 * Shared frame for account screens: ambient background, back button and logo, and a
 * scrolling column that stays readable on tablets (it floats in a glass card there).
 */
@Composable
fun FormScreen(
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    AmbientBackground(modifier) {
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .imePadding(),
        ) {
            val wide = maxWidth >= CardBreakpoint
            val minHeight = maxHeight
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .heightIn(min = minHeight),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(48.dp)) {
                        if (onBack != null) {
                            IconButton(onClick = onBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        }
                    }
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { KehDoNaLogo(markSize = 24.dp) }
                    Spacer(Modifier.size(48.dp))
                }
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = if (wide) 32.dp else 24.dp, vertical = 16.dp),
                    contentAlignment = if (wide) Alignment.Center else Alignment.TopCenter,
                ) {
                    val column = @Composable {
                        Column(
                            Modifier
                                .widthIn(max = 460.dp)
                                .fillMaxWidth()
                                .padding(if (wide) 32.dp else 0.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            content = content,
                        )
                    }
                    if (wide) {
                        Surface(
                            shape = MaterialTheme.shapes.extraLarge,
                            color = Color.White.copy(alpha = 0.035f),
                            border = BorderStroke(1.dp, Line),
                        ) { column() }
                    } else {
                        column()
                    }
                }
            }
        }
    }
}

/** Serif heading plus muted subtitle, centred, used at the top of account screens. */
@Composable
fun FormHeader(
    title: String,
    subtitle: String?,
    modifier: Modifier = Modifier,
    eyebrow: (@Composable () -> Unit)? = null,
) {
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        if (eyebrow != null) {
            eyebrow()
            Spacer(Modifier.height(16.dp))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        if (subtitle != null) {
            Spacer(Modifier.height(10.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = InkMuted, textAlign = TextAlign.Center)
        }
    }
}

/** Vertical stack with the standard spacing between form controls. */
@Composable
fun FormStack(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content,
    )
}
