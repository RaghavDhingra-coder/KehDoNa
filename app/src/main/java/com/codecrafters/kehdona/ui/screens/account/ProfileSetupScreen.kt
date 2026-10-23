package com.codecrafters.kehdona.ui.screens.account

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.codecrafters.kehdona.domain.SessionStage
import com.codecrafters.kehdona.ui.components.Badge
import com.codecrafters.kehdona.ui.components.BadgeTone
import com.codecrafters.kehdona.ui.components.EnterReveal
import com.codecrafters.kehdona.ui.components.FormHeader
import com.codecrafters.kehdona.ui.components.FormScreen
import com.codecrafters.kehdona.ui.components.FormStack
import com.codecrafters.kehdona.ui.components.InlineMessage
import com.codecrafters.kehdona.ui.components.KehDoNaTextField
import com.codecrafters.kehdona.ui.components.PrimaryButton
import com.codecrafters.kehdona.ui.components.PulsingHeart
import com.codecrafters.kehdona.ui.components.QuietButton
import com.codecrafters.kehdona.ui.theme.KehDoNaTheme

@Composable
fun ProfileSetupScreen(viewModel: ProfileSetupViewModel, onNext: (SessionStage) -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.next) {
        state.next?.let {
            viewModel.onNavigated()
            onNext(it)
        }
    }
    ProfileSetupScreen(
        state = state,
        onNameChange = viewModel::onNameChange,
        onSave = viewModel::save,
        onSignOut = viewModel::signOut,
    )
}

@Composable
fun ProfileSetupScreen(
    state: ProfileSetupUiState,
    onNameChange: (String) -> Unit,
    onSave: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FormScreen(modifier) {
        FormStack {
            EnterReveal {
                FormHeader(
                    title = "What should we call you?",
                    subtitle = buildString {
                        append("This is the name on your KehDoNa profile.")
                        state.contact?.let { append(" Signed in as $it.") }
                    },
                    eyebrow = { PulsingHeart(size = 48.dp) },
                )
            }
            Spacer(Modifier.height(8.dp))
            Badge(text = "Only you can change it", tone = BadgeTone.Lavender)
            KehDoNaTextField(
                value = state.name,
                onValueChange = onNameChange,
                label = "Your name",
                error = state.nameError,
                leadingIcon = Icons.Filled.Person,
                enabled = !state.isSaving,
                contentType = ContentType.PersonFullName,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onSave() }),
            )
            InlineMessage(state.errorMessage)
            PrimaryButton(
                text = "Continue",
                onClick = onSave,
                loading = state.isSaving,
                trailingIcon = Icons.AutoMirrored.Filled.ArrowForward,
                modifier = Modifier.fillMaxWidth(),
            )
            QuietButton(text = "Use a different account", onClick = onSignOut)
        }
    }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun ProfileSetupPreview() {
    KehDoNaTheme {
        ProfileSetupScreen(ProfileSetupUiState(name = "Riya Sharma", contact = "riya@example.com"), {}, {}, {})
    }
}
