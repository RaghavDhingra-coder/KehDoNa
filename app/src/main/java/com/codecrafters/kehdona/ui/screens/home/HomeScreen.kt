package com.codecrafters.kehdona.ui.screens.home

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.codecrafters.kehdona.data.profile.UserProfile
import com.codecrafters.kehdona.ui.components.Badge
import com.codecrafters.kehdona.ui.components.BadgeTone
import com.codecrafters.kehdona.ui.components.EnterReveal
import com.codecrafters.kehdona.ui.components.ErrorState
import com.codecrafters.kehdona.ui.components.FormHeader
import com.codecrafters.kehdona.ui.components.FormScreen
import com.codecrafters.kehdona.ui.components.FormStack
import com.codecrafters.kehdona.ui.components.LinkButton
import com.codecrafters.kehdona.ui.components.LoadingState
import com.codecrafters.kehdona.ui.components.PrimaryButton
import com.codecrafters.kehdona.ui.components.PulsingHeart
import com.codecrafters.kehdona.ui.components.SecondaryButton
import com.codecrafters.kehdona.ui.theme.InkMuted
import com.codecrafters.kehdona.ui.theme.InkSubtle
import com.codecrafters.kehdona.ui.theme.KehDoNaTheme
import com.codecrafters.kehdona.ui.theme.Line

/**
 * Signed-in landing for this phase: the private profile and log-out. Confessions,
 * matching and chat arrive in later phases and are only mentioned as coming soon.
 */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onUpdateVibe: () -> Unit,
    onSignedOut: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state) {
        if (state == HomeUiState.SignedOut) onSignedOut()
    }
    HomeScreen(state, onUpdateVibe, viewModel::signOut, viewModel::refresh)
}

@Composable
fun HomeScreen(
    state: HomeUiState,
    onUpdateVibe: () -> Unit,
    onSignOut: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FormScreen(modifier) {
        when (state) {
            HomeUiState.Loading, HomeUiState.SignedOut -> LoadingState(Modifier.heightIn(max = 480.dp))
            is HomeUiState.Error -> {
                ErrorState(
                    title = "Something went wrong",
                    message = state.message,
                    onRetry = onRetry,
                    modifier = Modifier.heightIn(max = 520.dp),
                )
                SecondaryButton(text = "Log out", onClick = onSignOut, modifier = Modifier.fillMaxWidth())
            }
            is HomeUiState.Ready -> ReadyContent(state, onUpdateVibe, onSignOut)
        }
    }
}

@Composable
private fun ReadyContent(state: HomeUiState.Ready, onUpdateVibe: () -> Unit, onSignOut: () -> Unit) {
    val profile = state.profile
    FormStack {
        EnterReveal {
            FormHeader(
                title = "Hi, ${profile.name.substringBefore(' ')} 💗",
                subtitle = "You're all set. Your account is ready for what's coming next.",
                eyebrow = { PulsingHeart(size = 52.dp) },
            )
        }
        Spacer(Modifier.height(4.dp))
        ProfileCard(profile, onUpdateVibe)
        Badge(text = "Confessions & matching arrive soon", tone = BadgeTone.Lavender, icon = Icons.Filled.Favorite)
        Spacer(Modifier.height(8.dp))
        SecondaryButton(
            text = if (state.isSigningOut) "Logging out…" else "Log out",
            onClick = onSignOut,
            enabled = !state.isSigningOut,
            leadingIcon = Icons.AutoMirrored.Filled.ExitToApp,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ProfileCard(profile: UserProfile, onUpdateVibe: () -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Surface(
        shape = MaterialTheme.shapes.large,
        color = Color.White.copy(alpha = 0.04f),
        border = BorderStroke(1.dp, Line),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp).animateContentSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("YOUR PROFILE", style = MaterialTheme.typography.labelSmall, color = InkSubtle)
            Text(profile.name, style = MaterialTheme.typography.headlineSmall)
            (profile.email ?: profile.phoneNumber)?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = InkMuted)
            }
            Spacer(Modifier.height(8.dp))
            Text("YOUR VIBE", style = MaterialTheme.typography.labelSmall, color = InkSubtle)
            val personality = profile.personalityProfile
            if (personality.isNullOrBlank()) {
                Text(
                    "You haven't added your personality profile yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkMuted,
                )
                PrimaryButton(text = "Discover my vibe ✨", onClick = onUpdateVibe, modifier = Modifier.fillMaxWidth())
            } else {
                Text(
                    personality,
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkMuted,
                    maxLines = if (expanded) Int.MAX_VALUE else 5,
                    overflow = TextOverflow.Ellipsis,
                )
                LinkButton(text = if (expanded) "Show less" else "Read all", onClick = { expanded = !expanded })
                SecondaryButton(text = "Update my vibe", onClick = onUpdateVibe, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Preview(widthDp = 390, heightDp = 900)
@Composable
private fun HomePreview() {
    KehDoNaTheme {
        HomeScreen(
            HomeUiState.Ready(
                UserProfile(
                    uid = "1",
                    name = "Riya Sharma",
                    email = "riya@example.com",
                    phoneNumber = null,
                    personalityProfile = "Communication style: [Observed] You write in short, warm bursts…",
                ),
            ),
            {}, {}, {},
        )
    }
}
