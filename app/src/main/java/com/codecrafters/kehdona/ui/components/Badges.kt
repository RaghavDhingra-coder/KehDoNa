package com.codecrafters.kehdona.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.codecrafters.kehdona.ui.theme.Blush200
import com.codecrafters.kehdona.ui.theme.InkMuted
import com.codecrafters.kehdona.ui.theme.Lavender200
import com.codecrafters.kehdona.ui.theme.Lavender300
import com.codecrafters.kehdona.ui.theme.LineStrong
import com.codecrafters.kehdona.ui.theme.Rose300

enum class BadgeTone(val content: Color, val container: Color, val border: Color) {
    Rose(Blush200, Rose300.copy(alpha = 0.1f), Rose300.copy(alpha = 0.3f)),
    Lavender(Lavender200, Lavender300.copy(alpha = 0.1f), Lavender300.copy(alpha = 0.3f)),
    Neutral(InkMuted, Color.White.copy(alpha = 0.04f), LineStrong),
}

/** Small uppercase pill label, e.g. "Coming soon". */
@Composable
fun Badge(
    text: String,
    modifier: Modifier = Modifier,
    tone: BadgeTone = BadgeTone.Neutral,
    icon: ImageVector? = null,
) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = tone.container,
        contentColor = tone.content,
        border = BorderStroke(1.dp, tone.border),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (icon != null) Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp))
            Text(text.uppercase(), style = MaterialTheme.typography.labelSmall)
        }
    }
}

/** Rounded square that frames a single icon. */
@Composable
fun IconBadge(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    tint: Color = Rose300,
    size: Dp = 48.dp,
) {
    Surface(
        modifier = modifier.size(size),
        shape = MaterialTheme.shapes.medium,
        color = tint.copy(alpha = 0.12f),
        contentColor = tint,
        border = BorderStroke(1.dp, tint.copy(alpha = 0.2f)),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.padding(size / 4))
    }
}
