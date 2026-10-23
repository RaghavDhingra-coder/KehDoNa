package com.codecrafters.kehdona.ui.screens.auth

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.codecrafters.kehdona.domain.Validation
import com.codecrafters.kehdona.model.AuthMode
import com.codecrafters.kehdona.ui.components.Badge
import com.codecrafters.kehdona.ui.components.BadgeTone
import com.codecrafters.kehdona.ui.components.EnterReveal
import com.codecrafters.kehdona.ui.components.FormHeader
import com.codecrafters.kehdona.ui.components.FormScreen
import com.codecrafters.kehdona.ui.components.FormStack
import com.codecrafters.kehdona.ui.components.GoogleSignInButton
import com.codecrafters.kehdona.ui.components.InlineMessage
import com.codecrafters.kehdona.ui.components.KehDoNaTextField
import com.codecrafters.kehdona.ui.components.LinkButton
import com.codecrafters.kehdona.ui.components.MessageTone
import com.codecrafters.kehdona.ui.components.OrDivider
import com.codecrafters.kehdona.ui.components.OtpInput
import com.codecrafters.kehdona.ui.components.PasswordField
import com.codecrafters.kehdona.ui.components.PrimaryButton
import com.codecrafters.kehdona.ui.components.SegmentedToggle
import com.codecrafters.kehdona.ui.theme.InkMuted
import com.codecrafters.kehdona.ui.theme.InkSubtle
import com.codecrafters.kehdona.ui.theme.KehDoNaTheme

/** Stateful entry point: wires [AuthViewModel] to the screen and reports where to go next. */
@Composable
fun AuthScreen(
    viewModel: AuthViewModel,
    onBack: () -> Unit,
    onSwitchMode: () -> Unit,
    onSignedIn: (AuthOutcome) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.outcome) {
        state.outcome?.let {
            viewModel.onOutcomeHandled()
            onSignedIn(it)
        }
    }
    BackHandler(enabled = state.step == AuthStep.Otp && !state.isBusy, onBack = viewModel::backToForm)

    AuthScreen(
        state = state,
        actions = AuthActions(
            onBack = onBack,
            onSwitchMode = onSwitchMode,
            onMethodChange = viewModel::onMethodChange,
            onNameChange = viewModel::onNameChange,
            onCountryCodeChange = viewModel::onCountryCodeChange,
            onPhoneChange = viewModel::onPhoneChange,
            onEmailChange = viewModel::onEmailChange,
            onPasswordChange = viewModel::onPasswordChange,
            onSubmit = viewModel::submit,
            onGoogle = viewModel::signInWithGoogle,
            onForgotPassword = viewModel::sendPasswordReset,
            onOtpChange = viewModel::onOtpChange,
            onVerify = viewModel::verifyOtp,
            onResend = viewModel::resendCode,
            onChangeNumber = viewModel::backToForm,
        ),
    )
}

class AuthActions(
    val onBack: () -> Unit = {},
    val onSwitchMode: () -> Unit = {},
    val onMethodChange: (AuthMethod) -> Unit = {},
    val onNameChange: (String) -> Unit = {},
    val onCountryCodeChange: (String) -> Unit = {},
    val onPhoneChange: (String) -> Unit = {},
    val onEmailChange: (String) -> Unit = {},
    val onPasswordChange: (String) -> Unit = {},
    val onSubmit: () -> Unit = {},
    val onGoogle: () -> Unit = {},
    val onForgotPassword: () -> Unit = {},
    val onOtpChange: (String) -> Unit = {},
    val onVerify: () -> Unit = {},
    val onResend: () -> Unit = {},
    val onChangeNumber: () -> Unit = {},
)

@Composable
fun AuthScreen(state: AuthUiState, actions: AuthActions, modifier: Modifier = Modifier) {
    FormScreen(
        modifier = modifier,
        onBack = if (state.step == AuthStep.Otp) actions.onChangeNumber else actions.onBack,
    ) {
        AnimatedContent(
            targetState = state.step,
            transitionSpec = {
                val forward = targetState == AuthStep.Otp
                (fadeIn(tween(320)) + slideInHorizontally(tween(320)) { if (forward) it / 6 else -it / 6 })
                    .togetherWith(fadeOut(tween(200)) + slideOutHorizontally(tween(200)) { if (forward) -it / 6 else it / 6 })
                    .using(SizeTransform(clip = false))
            },
            label = "authStep",
        ) { step ->
            when (step) {
                AuthStep.Form -> AuthForm(state, actions)
                AuthStep.Otp -> OtpStep(state, actions)
            }
        }
    }
}

@Composable
private fun AuthForm(state: AuthUiState, actions: AuthActions) {
    val enabled = !state.isBusy && state.isConfigured
    FormStack {
        EnterReveal {
            FormHeader(
                title = if (state.isSignUp) "Say it, softly." else "Welcome back.",
                subtitle = if (state.isSignUp) {
                    "Create your KehDoNa account. Your name stays private until you choose to share it."
                } else {
                    "Log in to pick up right where you left off."
                },
                eyebrow = {
                    Badge(text = if (state.isSignUp) "Create account" else "Log in", tone = BadgeTone.Rose)
                },
            )
        }
        Spacer(Modifier.height(8.dp))

        if (state.isSignUp) {
            KehDoNaTextField(
                value = state.name,
                onValueChange = actions.onNameChange,
                label = "Your name",
                error = state.nameError,
                supportingText = "This is the name on your profile.",
                leadingIcon = Icons.Filled.Person,
                enabled = enabled,
                contentType = ContentType.PersonFullName,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
            )
        }

        SegmentedToggle(
            options = listOf(AuthMethod.Phone to "Phone", AuthMethod.Email to "Email"),
            selected = state.method,
            onSelect = actions.onMethodChange,
            enabled = !state.isBusy,
        )

        AnimatedContent(
            targetState = state.method,
            transitionSpec = { fadeIn(tween(250)).togetherWith(fadeOut(tween(150))).using(SizeTransform(clip = false)) },
            label = "authMethod",
        ) { method ->
            FormStack {
                when (method) {
                    AuthMethod.Phone -> PhoneFields(state, actions, enabled)
                    AuthMethod.Email -> EmailFields(state, actions, enabled)
                }
            }
        }

        InlineMessage(state.errorMessage, tone = if (state.isConfigured) MessageTone.Error else MessageTone.Info)
        InlineMessage(state.infoMessage, tone = MessageTone.Success)

        PrimaryButton(
            text = when {
                state.method == AuthMethod.Phone -> "Send OTP"
                state.isSignUp -> "Create account"
                else -> "Log in"
            },
            onClick = actions.onSubmit,
            enabled = state.isConfigured,
            loading = state.busy == AuthBusy.Submitting,
            trailingIcon = Icons.AutoMirrored.Filled.ArrowForward,
            modifier = Modifier.fillMaxWidth(),
        )

        OrDivider()

        GoogleSignInButton(
            onClick = actions.onGoogle,
            enabled = state.isConfigured && state.isGoogleConfigured && !state.isBusy,
            loading = state.busy == AuthBusy.Google,
            text = if (state.isSignUp) "Sign up with Google" else "Continue with Google",
            modifier = Modifier.fillMaxWidth(),
        )
        if (state.isConfigured && !state.isGoogleConfigured) {
            Text(
                "Google sign-in isn't set up for KehDoNa yet. Use phone or email for now.",
                style = MaterialTheme.typography.bodySmall,
                color = InkSubtle,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (state.isSignUp) "Already have an account?" else "New to KehDoNa?",
                style = MaterialTheme.typography.bodyMedium,
                color = InkMuted,
            )
            LinkButton(
                text = if (state.isSignUp) "Log in" else "Create one",
                onClick = actions.onSwitchMode,
                enabled = !state.isBusy,
            )
        }
    }
}

@Composable
private fun PhoneFields(state: AuthUiState, actions: AuthActions, enabled: Boolean) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
        KehDoNaTextField(
            value = state.countryCode,
            onValueChange = actions.onCountryCodeChange,
            label = "Code",
            enabled = enabled,
            modifier = Modifier.width(96.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
        )
        KehDoNaTextField(
            value = state.phone,
            onValueChange = actions.onPhoneChange,
            label = "Phone number",
            error = state.phoneError,
            leadingIcon = Icons.Filled.Phone,
            enabled = enabled,
            modifier = Modifier.weight(1f),
            contentType = ContentType.PhoneNumber,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { actions.onSubmit() }),
        )
    }
    Text(
        "We'll text you a 6-digit code to verify it's you. Carrier rates may apply.",
        style = MaterialTheme.typography.bodySmall,
        color = InkSubtle,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun EmailFields(state: AuthUiState, actions: AuthActions, enabled: Boolean) {
    KehDoNaTextField(
        value = state.email,
        onValueChange = actions.onEmailChange,
        label = "Email",
        error = state.emailError,
        leadingIcon = Icons.Filled.Email,
        enabled = enabled,
        contentType = ContentType.EmailAddress,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
    )
    PasswordField(
        value = state.password,
        onValueChange = actions.onPasswordChange,
        label = "Password",
        error = state.passwordError,
        supportingText = if (state.isSignUp) "At least ${Validation.PasswordMinLength} characters, with letters and numbers." else null,
        leadingIcon = Icons.Filled.Lock,
        enabled = enabled,
        isNewPassword = state.isSignUp,
        onImeAction = actions.onSubmit,
    )
    if (state.isSignUp) {
        Text(
            "We'll email you a link to verify your address.",
            style = MaterialTheme.typography.bodySmall,
            color = InkSubtle,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    } else {
        LinkButton(text = "Forgot password?", onClick = actions.onForgotPassword, enabled = enabled)
    }
}

@Composable
private fun OtpStep(state: AuthUiState, actions: AuthActions) {
    FormStack {
        EnterReveal {
            FormHeader(
                title = "Check your messages",
                subtitle = "Enter the 6-digit code we sent to ${state.codeSentTo.orEmpty()}.",
                eyebrow = { Badge(text = "Verify it's you", tone = BadgeTone.Lavender) },
            )
        }
        Spacer(Modifier.height(8.dp))
        OtpInput(
            value = state.otp,
            onValueChange = actions.onOtpChange,
            isError = state.otpError != null,
            enabled = !state.isBusy,
        )
        state.otpError?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
        InlineMessage(state.errorMessage)
        InlineMessage(state.infoMessage, tone = MessageTone.Success)

        PrimaryButton(
            text = "Verify",
            onClick = actions.onVerify,
            enabled = state.otp.length == Validation.OtpLength,
            loading = state.busy == AuthBusy.Verifying,
            modifier = Modifier.fillMaxWidth(),
        )
        if (state.resendSecondsLeft > 0) {
            Text(
                "Resend code in 0:%02d".format(state.resendSecondsLeft),
                style = MaterialTheme.typography.bodyMedium,
                color = InkSubtle,
            )
        } else {
            LinkButton(text = "Resend code", onClick = actions.onResend, enabled = !state.isBusy)
        }
        LinkButton(text = "Change number", onClick = actions.onChangeNumber, enabled = !state.isBusy)
    }
}

@Preview(widthDp = 390, heightDp = 900)
@Composable
private fun SignUpPreview() {
    KehDoNaTheme {
        AuthScreen(AuthUiState(mode = AuthMode.SignUp, isConfigured = true, name = "Riya"), AuthActions())
    }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun OtpPreview() {
    KehDoNaTheme {
        AuthScreen(
            AuthUiState(
                mode = AuthMode.SignUp,
                isConfigured = true,
                step = AuthStep.Otp,
                otp = "482",
                codeSentTo = "+919876543210",
                resendSecondsLeft = 42,
            ),
            AuthActions(),
        )
    }
}
