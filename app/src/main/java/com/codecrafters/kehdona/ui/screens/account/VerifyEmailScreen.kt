package com.codecrafters.kehdona.ui.screens.account

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.codecrafters.kehdona.domain.SessionStage
import com.codecrafters.kehdona.ui.components.EnterReveal
import com.codecrafters.kehdona.ui.components.FormHeader
import com.codecrafters.kehdona.ui.components.FormScreen
import com.codecrafters.kehdona.ui.components.FormStack
import com.codecrafters.kehdona.ui.components.IconBadge
import com.codecrafters.kehdona.ui.components.InlineMessage
import com.codecrafters.kehdona.ui.components.MessageTone
import com.codecrafters.kehdona.ui.components.PrimaryButton
import com.codecrafters.kehdona.ui.components.QuietButton
import com.codecrafters.kehdona.ui.components.SecondaryButton
import com.codecrafters.kehdona.ui.theme.InkSubtle
import com.codecrafters.kehdona.ui.theme.KehDoNaTheme
import com.codecrafters.kehdona.ui.theme.Lavender300

@Composable
fun VerifyEmailScreen(viewModel: VerifyEmailViewModel, onNext: (SessionStage) -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.next) {
        state.next?.let {
            viewModel.onNavigated()
            onNext(it)
        }
    }
    VerifyEmailScreen(
        state = state,
        onCheck = viewModel::checkVerified,
        onResend = viewModel::resend,
        onSignOut = viewModel::signOut,
    )
}

@Composable
fun VerifyEmailScreen(
    state: VerifyEmailUiState,
    onCheck: () -> Unit,
    onResend: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FormScreen(modifier) {
        FormStack {
            EnterReveal {
                FormHeader(
                    title = "Check your inbox",
                    subtitle = "We sent a verification link to ${state.email ?: "your email"}. " +
                        "Tap it, then come back here.",
                    eyebrow = { IconBadge(Icons.Filled.Email, tint = Lavender300, size = 64.dp) },
                )
            }
            Spacer(Modifier.height(8.dp))
            InlineMessage(state.errorMessage)
            InlineMessage(state.infoMessage, tone = MessageTone.Success)
            PrimaryButton(
                text = "I've verified my email",
                onClick = onCheck,
                loading = state.busy == VerifyEmailBusy.Checking,
                modifier = Modifier.fillMaxWidth(),
            )
            SecondaryButton(
                text = if (state.resendSecondsLeft > 0) {
                    "Resend in 0:%02d".format(state.resendSecondsLeft)
                } else {
                    "Resend email"
                },
                onClick = onResend,
                enabled = state.busy == null && state.resendSecondsLeft == 0,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                "Can't find it? Check spam or promotions.",
                style = MaterialTheme.typography.bodySmall,
                color = InkSubtle,
                textAlign = TextAlign.Center,
            )
            QuietButton(text = "Use a different account", onClick = onSignOut)
        }
    }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun VerifyEmailPreview() {
    KehDoNaTheme { VerifyEmailScreen(VerifyEmailUiState(email = "riya@example.com"), {}, {}, {}) }
}
