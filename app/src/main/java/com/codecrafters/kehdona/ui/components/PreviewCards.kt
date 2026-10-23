package com.codecrafters.kehdona.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codecrafters.kehdona.ui.theme.Blush200
import com.codecrafters.kehdona.ui.theme.Cream200
import com.codecrafters.kehdona.ui.theme.Cream50
import com.codecrafters.kehdona.ui.theme.Fraunces
import com.codecrafters.kehdona.ui.theme.Ink
import com.codecrafters.kehdona.ui.theme.InkDark
import com.codecrafters.kehdona.ui.theme.InkMuted
import com.codecrafters.kehdona.ui.theme.InkSubtle
import com.codecrafters.kehdona.ui.theme.Lavender200
import com.codecrafters.kehdona.ui.theme.Lavender300
import com.codecrafters.kehdona.ui.theme.Lavender400
import com.codecrafters.kehdona.ui.theme.Line
import com.codecrafters.kehdona.ui.theme.LineStrong
import com.codecrafters.kehdona.ui.theme.Night850
import com.codecrafters.kehdona.ui.theme.Night900
import com.codecrafters.kehdona.ui.theme.Night950
import com.codecrafters.kehdona.ui.theme.Plum700
import com.codecrafters.kehdona.ui.theme.Rose300
import com.codecrafters.kehdona.ui.theme.Rose400
import com.codecrafters.kehdona.ui.theme.Rose500
import com.codecrafters.kehdona.ui.theme.Violet600
import com.codecrafters.kehdona.ui.theme.Violet700

/*
 * Decorative, illustrative UI used on the welcome and onboarding screens.
 * Every person and post shown here is fictional and nothing is backed by data.
 */

/** A compact illustrative profile card for a fictional student. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IllustrativeProfileCard(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = Night850,
        border = BorderStroke(1.dp, LineStrong),
        shadowElevation = 12.dp,
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(104.dp)
                    .background(Brush.linearGradient(listOf(Plum700, Violet600, Rose400))),
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    drawCircle(
                        Brush.radialGradient(
                            listOf(Blush200.copy(alpha = 0.95f), Rose300.copy(alpha = 0.5f)),
                            center = Offset(size.width / 2, size.height * 0.5f),
                            radius = size.height * 0.36f,
                        ),
                        radius = size.height * 0.34f,
                        center = Offset(size.width / 2, size.height * 0.5f),
                    )
                    drawRect(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Night850),
                            startY = size.height * 0.45f,
                            endY = size.height,
                        ),
                    )
                }
                Text(
                    "PREVIEW",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = InkMuted,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .clip(CircleShape)
                        .background(Night950.copy(alpha = 0.55f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
            Column(Modifier.padding(start = 18.dp, end = 18.dp, bottom = 18.dp)) {
                Text("Mehr", style = MaterialTheme.typography.headlineMedium, color = Ink)
                Text("B.Des · 2nd year", style = MaterialTheme.typography.bodyMedium, color = InkMuted)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.LocationOn,
                        contentDescription = null,
                        tint = InkSubtle,
                        modifier = Modifier.size(13.dp),
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        "Usually at the campus café",
                        style = MaterialTheme.typography.bodySmall,
                        color = InkSubtle,
                    )
                }
                Spacer(Modifier.height(12.dp))
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .background(Color.White.copy(alpha = 0.03f))
                        .border(1.dp, Line, MaterialTheme.shapes.medium)
                        .padding(12.dp),
                ) {
                    Text(
                        "THE WAY TO MY HEART",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = Lavender300,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Send me a song that sounds like a rainy evening.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = Fraunces,
                            fontStyle = FontStyle.Italic,
                            fontSize = 15.sp,
                            lineHeight = 20.sp,
                        ),
                        color = Ink,
                    )
                }
                Spacer(Modifier.height(12.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    listOf("Indie playlists", "Poetry").forEach { VibeChip(it) }
                }
            }
        }
    }
}

/** Small pill for an interest. `shared` highlights interests two people have in common. */
@Composable
fun VibeChip(text: String, modifier: Modifier = Modifier, shared: Boolean = false) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = if (shared) Lavender200 else InkMuted,
        modifier = modifier
            .clip(CircleShape)
            .background(if (shared) Lavender400.copy(alpha = 0.15f) else Color.Transparent)
            .border(1.dp, if (shared) Lavender300.copy(alpha = 0.4f) else LineStrong, CircleShape)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

/** "92% vibe match" chip with a progress ring. Illustrative only. */
@Composable
fun VibeMatchChip(percent: Int, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = Night900.copy(alpha = 0.94f),
        border = BorderStroke(1.dp, LineStrong),
        shadowElevation = 8.dp,
    ) {
        Row(
            modifier = Modifier.padding(start = 10.dp, end = 14.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(40.dp)) {
                    val stroke = 3.dp.toPx()
                    drawCircle(LineStrong, style = Stroke(stroke), radius = size.minDimension / 2 - stroke)
                    drawArc(
                        brush = Brush.sweepGradient(listOf(Rose300, Lavender300, Rose300)),
                        startAngle = -90f,
                        sweepAngle = 360f * percent / 100f,
                        useCenter = false,
                        topLeft = Offset(stroke, stroke),
                        size = Size(size.width - stroke * 2, size.height - stroke * 2),
                        style = Stroke(stroke, cap = StrokeCap.Round),
                    )
                }
                Text("$percent%", style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.sp), color = Ink)
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text("Vibe match", style = MaterialTheme.typography.titleSmall, color = Ink)
                Text("4 shared interests", style = MaterialTheme.typography.bodySmall, color = InkSubtle)
            }
        }
    }
}

/** A cream "confession" note. Illustrative only. */
@Composable
fun ConfessionNote(
    label: String,
    text: String,
    modifier: Modifier = Modifier,
    footer: String? = null,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = Cream50,
        border = BorderStroke(1.dp, Cream200),
        shadowElevation = 14.dp,
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(label.uppercase(), style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = Plum700)
            Spacer(Modifier.height(6.dp))
            Text(
                text,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = Fraunces,
                    fontStyle = FontStyle.Italic,
                    lineHeight = 22.sp,
                ),
                color = InkDark,
            )
            if (footer != null) {
                Spacer(Modifier.height(10.dp))
                Text(footer, style = MaterialTheme.typography.bodySmall, color = Plum700)
            }
        }
    }
}

/** "It's mutual" pill with a lock. Illustrative only. */
@Composable
fun MutualChip(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = Night900.copy(alpha = 0.94f),
        border = BorderStroke(1.dp, Rose300.copy(alpha = 0.35f)),
        shadowElevation = 8.dp,
    ) {
        Row(
            modifier = Modifier.padding(start = 6.dp, end = 14.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Rose400.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Lock, contentDescription = null, tint = Rose300, modifier = Modifier.size(14.dp))
            }
            Spacer(Modifier.width(8.dp))
            Text("It's mutual", style = MaterialTheme.typography.labelLarge, color = Blush200)
        }
    }
}

/** Gradient initials avatar. No photos of real people are used. */
@Composable
fun InitialsAvatar(initials: String, colors: List<Color>, modifier: Modifier = Modifier, size: Dp = 52.dp) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Brush.linearGradient(colors))
            .border(2.dp, Night900, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            initials,
            style = MaterialTheme.typography.headlineSmall.copy(fontSize = (size.value * 0.38f).sp),
            color = Night950,
        )
    }
}

val RoseAvatarColors = listOf(Rose300, Rose500, Plum700)
val LavenderAvatarColors = listOf(Lavender200, Lavender400, Violet700)
