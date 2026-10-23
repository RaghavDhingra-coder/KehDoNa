package com.codecrafters.kehdona.ui.screens.match

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.codecrafters.kehdona.data.match.Match
import com.codecrafters.kehdona.domain.SessionStage
import com.codecrafters.kehdona.model.Gender
import com.codecrafters.kehdona.model.MatchPreference
import com.codecrafters.kehdona.ui.components.Badge
import com.codecrafters.kehdona.ui.components.BadgeTone
import com.codecrafters.kehdona.ui.components.EnterReveal
import com.codecrafters.kehdona.ui.components.ErrorState
import com.codecrafters.kehdona.ui.components.FormHeader
import com.codecrafters.kehdona.ui.components.FormScreen
import com.codecrafters.kehdona.ui.components.FormStack
import com.codecrafters.kehdona.ui.components.InlineMessage
import com.codecrafters.kehdona.ui.components.LoadingState
import com.codecrafters.kehdona.ui.components.MatchingDetailsFields
import com.codecrafters.kehdona.ui.components.MessageTone
import com.codecrafters.kehdona.ui.components.PrimaryButton
import com.codecrafters.kehdona.ui.components.PulsingHeart
import com.codecrafters.kehdona.ui.components.QuietButton
import com.codecrafters.kehdona.ui.components.SecondaryButton
import com.codecrafters.kehdona.ui.theme.Blush200
import com.codecrafters.kehdona.ui.theme.InkMuted
import com.codecrafters.kehdona.ui.theme.InkSubtle
import com.codecrafters.kehdona.ui.theme.KehDoNaTheme
import com.codecrafters.kehdona.ui.theme.Line
import com.codecrafters.kehdona.ui.theme.Plum700
import com.codecrafters.kehdona.ui.theme.Rose300
import com.codecrafters.kehdona.ui.theme.Violet600

@Composable
fun MatchScreen(
    viewModel: MatchViewModel,
    onBack: () -> Unit,
    onDiscoverVibe: () -> Unit,
    onNext: (SessionStage) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.next) {
        state.next?.let {
            viewModel.onNavigated()
            onNext(it)
        }
    }
    MatchScreen(
        state = state,
        actions = MatchActions(
            onBack = onBack,
            onDiscoverVibe = onDiscoverVibe,
            onGenderChange = viewModel::onGenderChange,
            onSelfDescriptionChange = viewModel::onSelfDescriptionChange,
            onPreferenceChange = viewModel::onPreferenceChange,
            onSaveDetails = viewModel::saveDetails,
            onFindMatch = viewModel::findMatch,
            onCancel = viewModel::cancelSearch,
            onRetry = viewModel::load,
        ),
    )
}

class MatchActions(
    val onBack: () -> Unit = {},
    val onDiscoverVibe: () -> Unit = {},
    val onGenderChange: (Gender) -> Unit = {},
    val onSelfDescriptionChange: (String) -> Unit = {},
    val onPreferenceChange: (MatchPreference) -> Unit = {},
    val onSaveDetails: () -> Unit = {},
    val onFindMatch: () -> Unit = {},
    val onCancel: () -> Unit = {},
    val onRetry: () -> Unit = {},
)

@Composable
fun MatchScreen(state: MatchUiState, actions: MatchActions, modifier: Modifier = Modifier) {
    FormScreen(modifier, onBack = actions.onBack) {
        AnimatedContent(
            targetState = state.step,
            transitionSpec = {
                val enter = if (targetState == MatchStep.Matched) fadeIn(tween(420)) + scaleIn(tween(420), 0.92f) else fadeIn(tween(320))
                (enter togetherWith fadeOut(tween(160))).using(SizeTransform(clip = false))
            },
            label = "matchStep",
        ) { step ->
            when (step) {
                MatchStep.Loading -> LoadingState(Modifier.heightIn(max = 480.dp))
                MatchStep.NeedsDetails -> DetailsStep(state, actions)
                MatchStep.NeedsPersonality -> NeedsPersonalityStep(actions)
                MatchStep.Ready -> ReadyStep(state, actions)
                MatchStep.Searching -> SearchingStep()
                MatchStep.Waiting -> WaitingStep(state, actions)
                MatchStep.Matched -> state.match?.let { MatchedStep(it) } ?: SearchingStep()
                MatchStep.Error -> ErrorState(
                    title = "Matching didn't work",
                    message = state.errorMessage ?: "Please try again.",
                    onRetry = actions.onRetry,
                    modifier = Modifier.heightIn(max = 560.dp),
                )
            }
        }
    }
}

@Composable
private fun DetailsStep(state: MatchUiState, actions: MatchActions) {
    FormStack {
        FormHeader(
            title = "Complete your profile",
            subtitle = "Matching needs two quick answers. We never guess anyone's gender, and your answers stay private.",
            eyebrow = { Badge(text = "Before you match", tone = BadgeTone.Lavender, icon = Icons.Filled.Lock) },
        )
        MatchingDetailsFields(
            gender = state.details.gender,
            selfDescription = state.details.selfDescription,
            preference = state.details.preference,
            onGenderChange = actions.onGenderChange,
            onSelfDescriptionChange = actions.onSelfDescriptionChange,
            onPreferenceChange = actions.onPreferenceChange,
            genderError = state.details.genderError,
            selfDescriptionError = state.details.selfDescriptionError,
            preferenceError = state.details.preferenceError,
            enabled = !state.isSavingDetails,
        )
        InlineMessage(state.errorMessage)
        PrimaryButton(
            text = "Save and continue",
            onClick = actions.onSaveDetails,
            loading = state.isSavingDetails,
            trailingIcon = Icons.AutoMirrored.Filled.ArrowForward,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun NeedsPersonalityStep(actions: MatchActions) {
    FormStack(Modifier.padding(top = 32.dp)) {
        EnterReveal { MatchHero() }
        FormHeader(
            title = "Discover your vibe first",
            subtitle = "Matching compares personality profiles, so we need yours before we can find someone.",
        )
        PrimaryButton(text = "Discover my vibe ✨", onClick = actions.onDiscoverVibe, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun ReadyStep(state: MatchUiState, actions: MatchActions) {
    FormStack(Modifier.padding(top = 24.dp)) {
        EnterReveal { MatchHero() }
        FormHeader(
            title = "Find your match",
            subtitle = "We compare your vibe with members who are looking right now and match you with the most " +
                "compatible person who's also open to you. No swiping.",
        )
        InlineMessage(state.infoMessage, tone = MessageTone.Info)
        InlineMessage(state.errorMessage)
        PrimaryButton(
            text = "Match",
            onClick = actions.onFindMatch,
            trailingIcon = Icons.Filled.Favorite,
            modifier = Modifier.fillMaxWidth(),
        )
        PrivacyLine()
    }
}

@Composable
private fun SearchingStep() {
    FormStack(Modifier.padding(top = 96.dp).semantics { liveRegion = LiveRegionMode.Polite }) {
        PulsingHeart(size = 72.dp)
        Text("Finding your best match…", style = MaterialTheme.typography.titleMedium)
        Text("Comparing vibes with members who are looking.", style = MaterialTheme.typography.bodyMedium, color = InkMuted)
    }
}

@Composable
private fun WaitingStep(state: MatchUiState, actions: MatchActions) {
    FormStack(Modifier.padding(top = 48.dp).semantics { liveRegion = LiveRegionMode.Polite }) {
        PulsingHeart(size = 64.dp)
        FormHeader(
            title = "You're in the queue",
            subtitle = "No one compatible is looking right now. We'll match you as soon as someone is, and it " +
                "will show up here. You can leave this screen.",
        )
        InlineMessage(state.errorMessage)
        SecondaryButton(
            text = if (state.isCancelling) "Cancelling…" else "Cancel search",
            onClick = actions.onCancel,
            enabled = !state.isCancelling,
            modifier = Modifier.fillMaxWidth(),
        )
        QuietButton(text = "Check again", onClick = actions.onFindMatch)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MatchedStep(match: Match) {
    FormStack(Modifier.padding(top = 24.dp)) {
        EnterReveal { MatchHero() }
        Badge(text = "It's a match", tone = BadgeTone.Rose, icon = Icons.Filled.Favorite)
        FormHeader(
            title = "You and ${match.partnerFirstName} ✨",
            subtitle = "${compatibilityLabel(match.score)}. ${match.partnerFirstName} sees this match too.",
        )
        if (match.sharedThemes.isNotEmpty()) {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = Color.White.copy(alpha = 0.04f),
                border = BorderStroke(1.dp, Line),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("YOU BOTH MENTION", style = MaterialTheme.typography.labelSmall, color = InkSubtle)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        match.sharedThemes.forEach { Badge(text = it, tone = BadgeTone.Lavender) }
                    }
                }
            }
        }
        Text(
            "Chat with your match arrives in the next update.",
            style = MaterialTheme.typography.bodySmall,
            color = InkSubtle,
            textAlign = TextAlign.Center,
        )
    }
}

/** Plain-language band for the server's 0..1 score; the raw number isn't meaningful to people. */
internal fun compatibilityLabel(score: Double): String = when {
    score >= 0.25 -> "Strong compatibility"
    score >= 0.12 -> "Good compatibility"
    else -> "Some common ground, and a fresh perspective"
}

@Composable
private fun MatchHero() {
    Box(
        Modifier
            .size(120.dp)
            .background(Brush.radialGradient(listOf(Rose300.copy(alpha = 0.35f), Violet600.copy(alpha = 0.15f), Color.Transparent)), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(80.dp).background(Brush.linearGradient(listOf(Plum700, Violet600.copy(alpha = 0.7f))), CircleShape),
            contentAlignment = Alignment.Center,
        ) { PulsingHeart(size = 44.dp) }
    }
}

@Composable
private fun PrivacyLine() {
    Text(
        "Your match sees only your first name and the themes you share: never your contact details or full profile.",
        style = MaterialTheme.typography.bodySmall,
        color = Blush200.copy(alpha = 0.7f),
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Preview(widthDp = 390, heightDp = 900)
@Composable
private fun MatchedPreview() {
    KehDoNaTheme {
        MatchScreen(
            MatchUiState(step = MatchStep.Matched, match = Match("a_b", "Arjun", 0.31, listOf("music", "travel", "honesty"))),
            MatchActions(),
        )
    }
}
