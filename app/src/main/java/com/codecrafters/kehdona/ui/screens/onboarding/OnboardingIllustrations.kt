package com.codecrafters.kehdona.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.codecrafters.kehdona.model.OnboardingIllustration
import com.codecrafters.kehdona.ui.components.ConfessionNote
import com.codecrafters.kehdona.ui.components.InitialsAvatar
import com.codecrafters.kehdona.ui.components.LavenderAvatarColors
import com.codecrafters.kehdona.ui.components.RoseAvatarColors
import com.codecrafters.kehdona.ui.components.VibeChip
import com.codecrafters.kehdona.ui.theme.Fraunces
import com.codecrafters.kehdona.ui.theme.Lavender300
import com.codecrafters.kehdona.ui.theme.Lavender400
import com.codecrafters.kehdona.ui.theme.Night950
import com.codecrafters.kehdona.ui.theme.Rose300
import com.codecrafters.kehdona.ui.theme.Rose400

/** Decorative artwork for each onboarding page. Hidden from screen readers. */
@Composable
fun OnboardingIllustration(illustration: OnboardingIllustration, modifier: Modifier = Modifier) {
    Box(modifier.clearAndSetSemantics { }, contentAlignment = Alignment.Center) {
        when (illustration) {
            OnboardingIllustration.Vibes -> VibesArt()
            OnboardingIllustration.Confessions -> ConfessionArt()
            OnboardingIllustration.SecretCrush -> SecretCrushArt()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VibesArt() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row {
            InitialsAvatar("R", LavenderAvatarColors, size = 68.dp)
            InitialsAvatar("S", RoseAvatarColors, Modifier.offset(x = (-12).dp), size = 68.dp)
        }
        Spacer(Modifier.height(24.dp))
        FlowRow(
            modifier = Modifier.widthIn(max = 320.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            VibeChip("Late-night chai", shared = true)
            VibeChip("Debate club")
            VibeChip("Indie playlists", shared = true)
            VibeChip("Sunset walks", shared = true)
            VibeChip("Film cameras")
            VibeChip("Poetry nights", shared = true)
        }
    }
}

@Composable
private fun ConfessionArt() {
    ConfessionNote(
        label = "Library · 2nd floor",
        text = "“To the person who always saves the window seat — I've been taking the long way past it all semester.”",
        footer = "Posted anonymously",
        modifier = Modifier
            .widthIn(max = 300.dp)
            .rotate(-2.5f),
    )
}

@Composable
private fun SecretCrushArt() {
    Box(Modifier.width(260.dp).height(170.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .offset(x = (-44).dp)
                .size(140.dp)
                .clip(CircleShape)
                .background(Rose400.copy(alpha = 0.1f))
                .border(1.dp, Rose300.copy(alpha = 0.35f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            QuestionMark(Modifier.offset(x = (-22).dp))
        }
        Box(
            Modifier
                .offset(x = 44.dp)
                .size(140.dp)
                .clip(CircleShape)
                .background(Lavender400.copy(alpha = 0.1f))
                .border(1.dp, Lavender300.copy(alpha = 0.35f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            QuestionMark(Modifier.offset(x = 22.dp))
        }
        Box(
            Modifier
                .size(60.dp)
                .shadow(16.dp, CircleShape, ambientColor = Rose400, spotColor = Rose400)
                .background(Brush.linearGradient(listOf(Rose400, Lavender400)), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Lock, contentDescription = null, tint = Night950, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
private fun QuestionMark(modifier: Modifier = Modifier) {
    Text(
        "?",
        modifier = modifier,
        style = MaterialTheme.typography.displaySmall.copy(fontFamily = Fraunces, fontStyle = FontStyle.Italic),
        color = Rose300.copy(alpha = 0.8f),
    )
}
