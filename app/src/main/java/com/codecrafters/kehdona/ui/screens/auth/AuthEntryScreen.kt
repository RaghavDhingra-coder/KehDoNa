package com.codecrafters.kehdona.ui.screens.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.codecrafters.kehdona.ui.components.AmbientBackground
import com.codecrafters.kehdona.ui.components.EnterReveal
import com.codecrafters.kehdona.ui.components.HeartMark
import com.codecrafters.kehdona.ui.components.KehDoNaLogo
import com.codecrafters.kehdona.ui.components.PrimaryButton
import com.codecrafters.kehdona.ui.components.QuietButton
import com.codecrafters.kehdona.ui.components.SecondaryButton
import com.codecrafters.kehdona.ui.theme.InkMuted
import com.codecrafters.kehdona.ui.theme.KehDoNaTheme
import com.codecrafters.kehdona.ui.theme.Lavender200
import com.codecrafters.kehdona.ui.theme.Lavender300

/** Entry point for authentication: create an account or log back in. */
@Composable
fun AuthEntryScreen(
    onCreateAccount: () -> Unit,
    onLogIn: () -> Unit,
    onReplayIntro: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AmbientBackground(modifier) {
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            val minHeight = maxHeight
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .heightIn(min = minHeight)
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                KehDoNaLogo(Modifier.padding(top = 8.dp))

                EnterReveal(modifier = Modifier.padding(vertical = 32.dp)) {
                    Column(
                        modifier = Modifier.widthIn(max = 440.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        HeartMark(size = 56.dp)
                        Spacer(Modifier.height(24.dp))
                        Text(
                            text = "Ready when you are.",
                            style = MaterialTheme.typography.displaySmall,
                            modifier = Modifier.semantics { heading() },
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "Create your KehDoNa account with your phone, email or Google, or log back in.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = InkMuted,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(24.dp))
                        PrivacyNotice()
                    }
                }

                Column(
                    modifier = Modifier
                        .widthIn(max = 420.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    PrimaryButton(
                        text = "Create an account",
                        onClick = onCreateAccount,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    SecondaryButton(
                        text = "Log in",
                        onClick = onLogIn,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    QuietButton(text = "Replay the introduction", onClick = onReplayIntro)
                }
            }
        }
    }
}

@Composable
private fun PrivacyNotice() {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = Lavender300.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, Lavender300.copy(alpha = 0.25f)),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Icon(Icons.Filled.Lock, contentDescription = null, tint = Lavender300, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Text(
                text = "Your name and profile stay private to your account. Nothing is posted without you.",
                style = MaterialTheme.typography.bodyMedium,
                color = Lavender200,
            )
        }
    }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun AuthEntryScreenPreview() {
    KehDoNaTheme { AuthEntryScreen(onCreateAccount = {}, onLogIn = {}, onReplayIntro = {}) }
}
