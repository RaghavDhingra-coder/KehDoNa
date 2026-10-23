package com.codecrafters.kehdona.ui.screens.vibe

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.codecrafters.kehdona.domain.PersonalityPrompt
import com.codecrafters.kehdona.domain.SessionStage
import com.codecrafters.kehdona.ui.components.Badge
import com.codecrafters.kehdona.ui.components.BadgeTone
import com.codecrafters.kehdona.ui.components.EnterReveal
import com.codecrafters.kehdona.ui.components.ErrorState
import com.codecrafters.kehdona.ui.components.FormHeader
import com.codecrafters.kehdona.ui.components.FormScreen
import com.codecrafters.kehdona.ui.components.FormStack
import com.codecrafters.kehdona.ui.components.InlineMessage
import com.codecrafters.kehdona.ui.components.KehDoNaTextField
import com.codecrafters.kehdona.ui.components.LinkButton
import com.codecrafters.kehdona.ui.components.PrimaryButton
import com.codecrafters.kehdona.ui.components.PulsingHeart
import com.codecrafters.kehdona.ui.components.QuietButton
import com.codecrafters.kehdona.ui.components.SecondaryButton
import com.codecrafters.kehdona.ui.theme.Blush200
import com.codecrafters.kehdona.ui.theme.Ink
import com.codecrafters.kehdona.ui.theme.InkMuted
import com.codecrafters.kehdona.ui.theme.InkSubtle
import com.codecrafters.kehdona.ui.theme.KehDoNaTheme
import com.codecrafters.kehdona.ui.theme.Lavender200
import com.codecrafters.kehdona.ui.theme.Lavender300
import com.codecrafters.kehdona.ui.theme.Line
import com.codecrafters.kehdona.ui.theme.Night950
import com.codecrafters.kehdona.ui.theme.Plum700
import com.codecrafters.kehdona.ui.theme.Rose300
import com.codecrafters.kehdona.ui.theme.Violet600

/**
 * Stateful entry point. [updating] is true when opened from the home screen to refresh
 * an existing profile: there is a back button and no "maybe later".
 */
@Composable
fun VibeScreen(
    viewModel: VibeViewModel,
    updating: Boolean,
    onNext: (SessionStage) -> Unit,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(state.next) {
        state.next?.let {
            viewModel.onNavigated()
            onNext(it)
        }
    }
    VibeScreen(
        state = state,
        updating = updating,
        actions = VibeActions(
            onBack = onBack,
            onDiscover = { viewModel.onPromptHandedOff(ChatGptHandoff.copyAndOpen(context)) },
            onCopyAgain = { ChatGptHandoff.copyPrompt(context) },
            onOpenChatGpt = { ChatGptHandoff.open(context) },
            onHavePrompt = viewModel::goToPaste,
            onBackToIntro = viewModel::backToIntro,
            onProfileTextChange = viewModel::onProfileTextChange,
            onSave = viewModel::saveProfile,
            onKeepName = viewModel::keepVerifiedName,
            onUseSuggested = viewModel::confirmSuggestedName,
            onCustomNameChange = viewModel::onCustomNameChange,
            onUseCustom = viewModel::confirmCustomName,
            onSkip = viewModel::skipForNow,
            onFinish = viewModel::finish,
            onRetry = viewModel::load,
        ),
    )
}

class VibeActions(
    val onBack: () -> Unit = {},
    val onDiscover: () -> Unit = {},
    val onCopyAgain: () -> Unit = {},
    val onOpenChatGpt: () -> Unit = {},
    val onHavePrompt: () -> Unit = {},
    val onBackToIntro: () -> Unit = {},
    val onProfileTextChange: (String) -> Unit = {},
    val onSave: () -> Unit = {},
    val onKeepName: () -> Unit = {},
    val onUseSuggested: () -> Unit = {},
    val onCustomNameChange: (String) -> Unit = {},
    val onUseCustom: () -> Unit = {},
    val onSkip: () -> Unit = {},
    val onFinish: () -> Unit = {},
    val onRetry: () -> Unit = {},
)

@Composable
fun VibeScreen(state: VibeUiState, updating: Boolean, actions: VibeActions, modifier: Modifier = Modifier) {
    val onBack = when (state.step) {
        VibeStep.Paste -> actions.onBackToIntro
        VibeStep.Intro, VibeStep.LoadFailed -> actions.onBack.takeIf { updating }
        else -> null
    }
    FormScreen(modifier, onBack = onBack) {
        AnimatedContent(
            targetState = state.step,
            transitionSpec = {
                (fadeIn(tween(380)) + slideInVertically(tween(380)) { it / 12 })
                    .togetherWith(fadeOut(tween(180)))
                    .using(SizeTransform(clip = false))
            },
            label = "vibeStep",
        ) { step ->
            when (step) {
                VibeStep.Loading -> FormStack(Modifier.padding(top = 96.dp)) {
                    PulsingHeart()
                    Text("Getting things ready…", style = MaterialTheme.typography.bodyMedium, color = InkMuted)
                }
                VibeStep.LoadFailed -> ErrorState(
                    title = "We couldn't load your profile",
                    message = state.errorMessage ?: "Please try again.",
                    onRetry = actions.onRetry,
                    modifier = Modifier.heightIn(max = 560.dp),
                )
                VibeStep.Intro -> IntroStep(state, updating, actions)
                VibeStep.Paste -> PasteStep(state, actions)
                VibeStep.ConfirmName -> ConfirmNameStep(state, actions)
                VibeStep.Done -> DoneStep(state, actions)
            }
        }
    }
}

@Composable
private fun IntroStep(state: VibeUiState, updating: Boolean, actions: VibeActions) {
    FormStack {
        EnterReveal { VibeHero() }
        EnterReveal(delayMillis = 120) {
            FormHeader(
                title = "Let's discover your vibe ✨",
                subtitle = "ChatGPT already knows how you talk. Let it sketch your personality, " +
                    "so KehDoNa can find people you'll genuinely click with.",
            )
        }
        Spacer(Modifier.height(4.dp))
        EnterReveal(delayMillis = 240) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                StepRow(1, "We copy a carefully written prompt and open ChatGPT.")
                StepRow(2, "Paste it into ChatGPT and send it.")
                StepRow(3, "Copy ChatGPT's reply and paste it back here.")
            }
        }
        PrivacyNote(
            "ChatGPT is asked to use only what's in your chats, label guesses as guesses, and leave out " +
                "sensitive details. Your profile stays private to your account.",
        )
        InlineMessage(state.errorMessage)
        PrimaryButton(
            text = "Discover My Personality",
            onClick = actions.onDiscover,
            trailingIcon = Icons.AutoMirrored.Filled.Send,
            modifier = Modifier.fillMaxWidth(),
        )
        SecondaryButton(
            text = "I already have my profile",
            onClick = actions.onHavePrompt,
            modifier = Modifier.fillMaxWidth(),
        )
        if (!updating) {
            if (state.isSaving) {
                Text("Saving…", style = MaterialTheme.typography.bodyMedium, color = InkSubtle)
            } else {
                QuietButton(text = "Maybe later", onClick = actions.onSkip)
            }
        }
    }
}

@Composable
private fun PasteStep(state: VibeUiState, actions: VibeActions) {
    FormStack {
        FormHeader(
            title = "Paste your vibe",
            subtitle = null,
            eyebrow = { Badge(text = "Almost there", tone = BadgeTone.Rose) },
        )
        Instructions(state.openedIn)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            LinkButton(text = "Copy prompt again", onClick = actions.onCopyAgain)
            LinkButton(text = "Open ChatGPT", onClick = actions.onOpenChatGpt)
        }
        KehDoNaTextField(
            value = state.profileText,
            onValueChange = actions.onProfileTextChange,
            label = "ChatGPT's personality profile",
            error = state.profileError,
            supportingText = "${state.profileText.length} / ${PersonalityPrompt.MaxProfileLength}",
            singleLine = false,
            minLines = 8,
            maxLines = 14,
            enabled = !state.isSaving,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        )
        InlineMessage(state.errorMessage)
        PrimaryButton(
            text = "Save my profile",
            onClick = actions.onSave,
            loading = state.isSaving,
            trailingIcon = Icons.Filled.Check,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun Instructions(openedIn: ChatGptTarget?) {
    val where = when (openedIn) {
        ChatGptTarget.App -> "We opened the ChatGPT app and copied the prompt for you."
        ChatGptTarget.Browser -> "We opened ChatGPT in your browser and copied the prompt for you."
        ChatGptTarget.None -> "We copied the prompt, but couldn't open ChatGPT. Open it yourself at chatgpt.com."
        null -> "Use the prompt from KehDoNa in ChatGPT (copy it again below if needed)."
    }
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = Lavender300.copy(alpha = 0.07f),
        border = BorderStroke(1.dp, Lavender300.copy(alpha = 0.25f)),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(where, style = MaterialTheme.typography.bodyMedium, color = Lavender200)
            Text(
                "In ChatGPT: long-press the message box, tap Paste, and send. When it replies, copy the whole " +
                    "profile and paste it below.",
                style = MaterialTheme.typography.bodyMedium,
                color = InkMuted,
            )
        }
    }
}

@Composable
private fun ConfirmNameStep(state: VibeUiState, actions: VibeActions) {
    val suggested = state.suggestedName.orEmpty()
    FormStack {
        FormHeader(
            title = "Quick check: is this you?",
            subtitle = null,
            eyebrow = { Badge(text = "Private · only you see this", tone = BadgeTone.Lavender, icon = Icons.Filled.Lock) },
        )
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = Color.White.copy(alpha = 0.04f),
            border = BorderStroke(1.dp, Line),
        ) {
            Text(
                buildAnnotatedString {
                    append("Your profile name is ")
                    withStyle(SpanStyle(color = Ink, fontWeight = FontWeight.SemiBold)) { append(state.profileName) }
                    append(", but ChatGPT's text calls you ")
                    withStyle(SpanStyle(color = Blush200, fontWeight = FontWeight.SemiBold)) { append(suggested) }
                    append(". AI output isn't proof of identity, so KehDoNa never changes your name unless you say so.")
                },
                style = MaterialTheme.typography.bodyLarge,
                color = InkMuted,
                modifier = Modifier.padding(16.dp),
            )
        }
        InlineMessage(state.errorMessage)
        PrimaryButton(
            text = "Keep “${state.profileName}”",
            onClick = actions.onKeepName,
            enabled = !state.isSaving,
            modifier = Modifier.fillMaxWidth(),
        )
        SecondaryButton(
            text = "Yes, my name is “$suggested”",
            onClick = actions.onUseSuggested,
            enabled = !state.isSaving,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        Text("Or correct it yourself", style = MaterialTheme.typography.titleSmall, color = InkMuted)
        KehDoNaTextField(
            value = state.customName,
            onValueChange = actions.onCustomNameChange,
            label = "Your name",
            error = state.customNameError,
            leadingIcon = Icons.Filled.Person,
            enabled = !state.isSaving,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
        )
        SecondaryButton(
            text = "Save this name",
            onClick = actions.onUseCustom,
            enabled = !state.isSaving,
            leadingIcon = Icons.Filled.Create,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun DoneStep(state: VibeUiState, actions: VibeActions) {
    FormStack(Modifier.padding(top = 48.dp)) {
        EnterReveal { VibeHero() }
        FormHeader(
            title = "Your vibe is saved ✨",
            subtitle = "Nice to meet you properly, ${state.profileName}. We'll use this to find people on your wavelength.",
        )
        Spacer(Modifier.height(8.dp))
        PrimaryButton(
            text = "Continue",
            onClick = actions.onFinish,
            trailingIcon = Icons.AutoMirrored.Filled.ArrowForward,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Heart inside a soft violet-to-rose halo. */
@Composable
private fun VibeHero() {
    Box(
        Modifier
            .size(132.dp)
            .drawBehind {
                drawCircle(
                    Brush.radialGradient(
                        listOf(Rose300.copy(alpha = 0.35f), Violet600.copy(alpha = 0.18f), Color.Transparent),
                    ),
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(84.dp)
                .background(Brush.linearGradient(listOf(Plum700, Violet600.copy(alpha = 0.7f))), CircleShape),
            contentAlignment = Alignment.Center,
        ) { PulsingHeart(size = 48.dp) }
    }
}

@Composable
private fun StepRow(number: Int, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Box(
            Modifier
                .size(32.dp)
                .background(Brush.linearGradient(listOf(Rose300, Lavender300)), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text("$number", style = MaterialTheme.typography.labelLarge, color = Night950)
        }
        Spacer(Modifier.width(14.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun PrivacyNote(text: String) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = Color.White.copy(alpha = 0.035f),
        border = BorderStroke(1.dp, Line),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            Icon(
                Icons.Filled.Lock,
                contentDescription = null,
                tint = Lavender300,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(text, style = MaterialTheme.typography.bodySmall, color = InkMuted)
        }
    }
}

@Preview(widthDp = 390, heightDp = 960)
@Composable
private fun VibeIntroPreview() {
    KehDoNaTheme { VibeScreen(VibeUiState(step = VibeStep.Intro, profileName = "Riya"), false, VibeActions()) }
}

@Preview(widthDp = 390, heightDp = 900)
@Composable
private fun ConfirmNamePreview() {
    KehDoNaTheme {
        VibeScreen(
            VibeUiState(step = VibeStep.ConfirmName, profileName = "Riya Sharma", suggestedName = "Priya", customName = "Riya Sharma"),
            false,
            VibeActions(),
        )
    }
}
