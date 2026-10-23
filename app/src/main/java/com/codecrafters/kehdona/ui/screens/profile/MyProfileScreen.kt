package com.codecrafters.kehdona.ui.screens.profile

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.codecrafters.kehdona.data.profile.PersonalityProfile
import com.codecrafters.kehdona.data.profile.UserProfile
import com.codecrafters.kehdona.domain.Evidence
import com.codecrafters.kehdona.domain.PersonalityItem
import com.codecrafters.kehdona.domain.PersonalityParser
import com.codecrafters.kehdona.domain.PersonalitySection
import com.codecrafters.kehdona.domain.PersonalitySectionKind
import com.codecrafters.kehdona.ui.components.Badge
import com.codecrafters.kehdona.ui.components.BadgeTone
import com.codecrafters.kehdona.ui.components.EnterReveal
import com.codecrafters.kehdona.ui.components.ErrorState
import com.codecrafters.kehdona.ui.components.FormScreen
import com.codecrafters.kehdona.ui.components.FormStack
import com.codecrafters.kehdona.ui.components.LinkButton
import com.codecrafters.kehdona.ui.components.LoadingState
import com.codecrafters.kehdona.ui.components.PrimaryButton
import com.codecrafters.kehdona.ui.components.PulsingHeart
import com.codecrafters.kehdona.ui.theme.Blush200
import com.codecrafters.kehdona.ui.theme.Ink
import com.codecrafters.kehdona.ui.theme.InkMuted
import com.codecrafters.kehdona.ui.theme.InkSubtle
import com.codecrafters.kehdona.ui.theme.KehDoNaTheme
import com.codecrafters.kehdona.ui.theme.Lavender200
import com.codecrafters.kehdona.ui.theme.Lavender300
import com.codecrafters.kehdona.ui.theme.Line
import com.codecrafters.kehdona.ui.theme.Rose300
import java.text.DateFormat
import java.util.Date

/** Stateful entry point for the member's own, read-only profile. */
@Composable
fun MyProfileScreen(
    viewModel: MyProfileViewModel,
    onBack: () -> Unit,
    onDiscoverVibe: () -> Unit,
    onSignedOut: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state) {
        if (state == MyProfileUiState.SignedOut) onSignedOut()
    }
    MyProfileScreen(state, onBack, onDiscoverVibe, viewModel::load)
}

@Composable
fun MyProfileScreen(
    state: MyProfileUiState,
    onBack: () -> Unit,
    onDiscoverVibe: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FormScreen(modifier, onBack = onBack) {
        when (state) {
            MyProfileUiState.Loading, MyProfileUiState.SignedOut ->
                LoadingState(Modifier.heightIn(max = 480.dp), message = "Opening your profile…")
            is MyProfileUiState.Error -> ErrorState(
                title = "We couldn't open your profile",
                message = state.message,
                onRetry = onRetry,
                modifier = Modifier.heightIn(max = 560.dp),
            )
            is MyProfileUiState.Ready -> ProfileContent(state, onDiscoverVibe)
        }
    }
}

@Composable
private fun ProfileContent(state: MyProfileUiState.Ready, onDiscoverVibe: () -> Unit) {
    FormStack {
        EnterReveal { ProfileHeader(state.profile, state.personality) }
        val personality = state.personality
        if (personality == null) {
            NoPersonalityYet(onDiscoverVibe)
            return@FormStack
        }
        val traits = state.sections.filter { it.kind != PersonalitySectionKind.ConfidenceNote }
        if (traits.isNotEmpty()) {
            if (traits.any { section -> section.items.any { it.evidence != null } }) EvidenceLegend()
            traits.forEachIndexed { index, section ->
                EnterReveal(delayMillis = 60 * index.coerceAtMost(6)) { SectionCard(section) }
            }
        }
        state.sections.firstOrNull { it.kind == PersonalitySectionKind.ConfidenceNote }?.let { note ->
            Text(
                note.items.joinToString(" ") { it.text },
                style = MaterialTheme.typography.bodySmall,
                color = InkSubtle,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        FullResponseCard(personality.response, startExpanded = traits.isEmpty())
    }
}

@Composable
private fun ProfileHeader(profile: UserProfile, personality: PersonalityProfile?) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(88.dp)
                .background(Brush.linearGradient(listOf(Rose300.copy(alpha = 0.35f), Lavender300.copy(alpha = 0.25f))), CircleShape),
            contentAlignment = Alignment.Center,
        ) { PulsingHeart(size = 48.dp) }
        Spacer(Modifier.height(16.dp))
        Text(
            profile.name,
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        (profile.email ?: profile.phoneNumber)?.let {
            Spacer(Modifier.height(4.dp))
            Text(it, style = MaterialTheme.typography.bodyMedium, color = InkMuted)
        }
        if (personality != null) {
            Spacer(Modifier.height(14.dp))
            Badge(text = "Final · read-only", tone = BadgeTone.Lavender, icon = Icons.Filled.Lock)
            personality.importedAtMillis?.let { millis ->
                Spacer(Modifier.height(8.dp))
                Text(
                    "Imported from ChatGPT on ${DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(millis))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSubtle,
                )
            }
        }
    }
}

@Composable
private fun EvidenceLegend() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        EvidenceChip(Evidence.Observed)
        Text("seen in your chats", style = MaterialTheme.typography.bodySmall, color = InkSubtle)
        Spacer(Modifier.width(4.dp))
        EvidenceChip(Evidence.Inferred)
        Text("a careful guess", style = MaterialTheme.typography.bodySmall, color = InkSubtle)
    }
}

@Composable
private fun SectionCard(section: PersonalitySection) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = Color.White.copy(alpha = 0.04f),
        border = BorderStroke(1.dp, Line),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                section.kind.title,
                style = MaterialTheme.typography.titleMedium,
                color = Blush200,
                modifier = Modifier.semantics { heading() },
            )
            section.items.forEach { ItemRow(it) }
        }
    }
}

@Composable
private fun ItemRow(item: PersonalityItem) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            Modifier
                .padding(top = 9.dp)
                .size(6.dp)
                .background(Rose300, CircleShape),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(item.text, style = MaterialTheme.typography.bodyLarge, color = Ink)
            item.evidence?.let { EvidenceChip(it) }
        }
    }
}

@Composable
private fun EvidenceChip(evidence: Evidence) {
    val (label, color) = when (evidence) {
        Evidence.Observed -> "Observed" to Lavender200
        Evidence.Inferred -> "Inferred" to InkMuted
    }
    Surface(
        shape = CircleShape,
        color = color.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f)),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = color,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
        )
    }
}

@Composable
private fun FullResponseCard(response: String, startExpanded: Boolean) {
    var expanded by rememberSaveable { mutableStateOf(startExpanded) }
    Surface(
        shape = MaterialTheme.shapes.large,
        color = Color.White.copy(alpha = 0.03f),
        border = BorderStroke(1.dp, Line),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp).animateContentSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("FULL CHATGPT RESPONSE", style = MaterialTheme.typography.labelSmall, color = InkSubtle)
            Text(
                "Exactly as you imported it.",
                style = MaterialTheme.typography.bodySmall,
                color = InkSubtle,
            )
            if (expanded) {
                SelectionContainer {
                    Text(response, style = MaterialTheme.typography.bodyMedium, color = InkMuted)
                }
            }
            LinkButton(
                text = if (expanded) "Hide full response" else "Show full response",
                onClick = { expanded = !expanded },
            )
        }
    }
}

@Composable
private fun NoPersonalityYet(onDiscoverVibe: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = Color.White.copy(alpha = 0.04f),
        border = BorderStroke(1.dp, Line),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Your vibe isn't here yet", style = MaterialTheme.typography.titleMedium)
            Text(
                "Import your personality profile from ChatGPT. Once saved, it's final and always readable here.",
                style = MaterialTheme.typography.bodyMedium,
                color = InkMuted,
            )
            PrimaryButton(text = "Discover my vibe ✨", onClick = onDiscoverVibe, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Preview(widthDp = 390, heightDp = 1400)
@Composable
private fun MyProfilePreview() {
    val response = """
        Communication style: [Observed] You write in quick, playful bursts.
        Interests:
        - [Observed] Indie music
        - [Inferred] Late-night food runs
        Values: Not enough context
        Relationship preferences: [Inferred] You value steady, thoughtful partners.
        Confidence note: Based on a few months of casual chats.
    """.trimIndent()
    KehDoNaTheme {
        MyProfileScreen(
            MyProfileUiState.Ready(
                UserProfile("1", "Riya Sharma", "riya@example.com", null),
                PersonalityProfile(response, importedAtMillis = 1_761_000_000_000),
                PersonalityParser.parse(response),
            ),
            {}, {}, {},
        )
    }
}
