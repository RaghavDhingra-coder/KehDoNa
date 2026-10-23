package com.codecrafters.kehdona.ui.screens.welcome

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.codecrafters.kehdona.ui.components.AmbientBackground
import com.codecrafters.kehdona.ui.components.ConfessionNote
import com.codecrafters.kehdona.ui.components.EnterReveal
import com.codecrafters.kehdona.ui.components.IllustrativeProfileCard
import com.codecrafters.kehdona.ui.components.KehDoNaLogo
import com.codecrafters.kehdona.ui.components.MutualChip
import com.codecrafters.kehdona.ui.components.PrimaryButton
import com.codecrafters.kehdona.ui.components.SecondaryButton
import com.codecrafters.kehdona.ui.components.VibeMatchChip
import com.codecrafters.kehdona.ui.theme.InkMuted
import com.codecrafters.kehdona.ui.theme.InkSubtle
import com.codecrafters.kehdona.ui.theme.KehDoNaTheme
import com.codecrafters.kehdona.ui.theme.Line
import com.codecrafters.kehdona.ui.theme.Plum800
import com.codecrafters.kehdona.ui.theme.Rose300
import com.codecrafters.kehdona.ui.theme.Violet700

/** Width at which the welcome screen switches to a side-by-side layout. */
private val WideLayoutBreakpoint = 600.dp

@Composable
fun WelcomeScreen(
    onGetStarted: () -> Unit,
    onLogIn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AmbientBackground(modifier) {
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            if (maxWidth >= WideLayoutBreakpoint) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 48.dp, vertical = 32.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(48.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.Center,
                    ) {
                        KehDoNaLogo()
                        Spacer(Modifier.height(40.dp))
                        WelcomeCopy(textAlign = TextAlign.Start)
                        Spacer(Modifier.height(36.dp))
                        WelcomeActions(onGetStarted, onLogIn, Modifier.widthIn(max = 400.dp))
                    }
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        EnterReveal(delayMillis = 150) { WelcomePreview() }
                    }
                }
            } else {
                val minHeight = maxHeight
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .heightIn(min = minHeight)
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    KehDoNaLogo(Modifier.padding(top = 8.dp))
                    EnterReveal(delayMillis = 100, modifier = Modifier.padding(vertical = 12.dp)) {
                        WelcomePreview()
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        WelcomeCopy(textAlign = TextAlign.Center)
                        Spacer(Modifier.height(24.dp))
                        WelcomeActions(onGetStarted, onLogIn, Modifier.widthIn(max = 420.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun WelcomeCopy(textAlign: TextAlign) {
    val horizontal = if (textAlign == TextAlign.Center) Alignment.CenterHorizontally else Alignment.Start
    Column(horizontalAlignment = horizontal) {
        Text(
            text = buildAnnotatedString {
                append("Some feelings are meant to be ")
                withStyle(SpanStyle(color = Rose300, fontStyle = FontStyle.Italic)) { append("found.") }
            },
            style = MaterialTheme.typography.displaySmall,
            textAlign = textAlign,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "A little mystery. A real connection. A place to find the people who just get you.",
            style = MaterialTheme.typography.bodyLarge,
            color = InkMuted,
            textAlign = textAlign,
        )
    }
}

@Composable
private fun WelcomeActions(onGetStarted: () -> Unit, onLogIn: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PrimaryButton(
            text = "Get started",
            onClick = onGetStarted,
            trailingIcon = Icons.AutoMirrored.Filled.ArrowForward,
            modifier = Modifier.fillMaxWidth(),
        )
        SecondaryButton(
            text = "I already have an account",
            onClick = onLogIn,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Layered, illustrative product preview. All people and posts are fictional. */
@Composable
private fun WelcomePreview(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = "Illustrative preview of a KehDoNa profile. The person shown is fictional."
        },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .width(320.dp)
                .padding(top = 20.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            // Card peeking out behind the main profile
            Box(
                Modifier
                    .width(220.dp)
                    .height(300.dp)
                    .offset(x = 18.dp, y = 4.dp)
                    .rotate(7f)
                    .clip(MaterialTheme.shapes.large)
                    .background(Brush.linearGradient(listOf(Violet700, Plum800)))
                    .border(1.dp, Line, MaterialTheme.shapes.large),
            )
            IllustrativeProfileCard(Modifier.width(240.dp))
            VibeMatchChip(
                percent = 92,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(y = (-18).dp),
            )
            // Sits over the card artwork so it never covers the profile text.
            MutualChip(
                Modifier
                    .align(Alignment.TopEnd)
                    .offset(y = 72.dp),
            )
        }
        // Overlaps only the card's bottom edge (by a fixed amount), whatever its own height.
        Box(
            Modifier
                .width(320.dp)
                .layout { measurable, constraints ->
                    val overlap = 20.dp.roundToPx()
                    val placeable = measurable.measure(constraints)
                    layout(placeable.width, placeable.height - overlap) { placeable.place(0, -overlap) }
                },
        ) {
            ConfessionNote(
                label = "Confession #214",
                text = "“To whoever leaves sticky notes in library books — you made my week.”",
                modifier = Modifier.width(236.dp),
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "Illustrative preview · people shown are fictional",
            style = MaterialTheme.typography.bodySmall,
            color = InkSubtle,
        )
    }
}

@Preview(name = "Phone", widthDp = 390, heightDp = 844)
@Composable
private fun WelcomeScreenPhonePreview() {
    KehDoNaTheme { WelcomeScreen(onGetStarted = {}, onLogIn = {}) }
}

@Preview(name = "Tablet", widthDp = 1024, heightDp = 720)
@Composable
private fun WelcomeScreenTabletPreview() {
    KehDoNaTheme { WelcomeScreen(onGetStarted = {}, onLogIn = {}) }
}
