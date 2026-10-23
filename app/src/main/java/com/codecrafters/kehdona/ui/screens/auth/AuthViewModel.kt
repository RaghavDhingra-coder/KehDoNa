package com.codecrafters.kehdona.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codecrafters.kehdona.data.auth.AuthError
import com.codecrafters.kehdona.data.auth.AuthException
import com.codecrafters.kehdona.data.auth.AuthRepository
import com.codecrafters.kehdona.data.auth.AuthUser
import com.codecrafters.kehdona.data.auth.GoogleIdTokenProvider
import com.codecrafters.kehdona.data.auth.PhoneCodeResult
import com.codecrafters.kehdona.data.auth.PhoneResendToken
import com.codecrafters.kehdona.data.profile.ProfileException
import com.codecrafters.kehdona.data.profile.ProfileRepository
import com.codecrafters.kehdona.data.profile.UserProfile
import com.codecrafters.kehdona.domain.SessionResolver
import com.codecrafters.kehdona.domain.SessionStage
import com.codecrafters.kehdona.domain.Validation
import com.codecrafters.kehdona.model.AuthMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AuthMethod { Phone, Email }

enum class AuthStep { Form, Otp }

enum class AuthBusy { Submitting, Google, Verifying, Resending }

/** Where to go once signed in; [prefillName] seeds the profile-setup form. */
data class AuthOutcome(val stage: SessionStage, val prefillName: String? = null)

data class AuthUiState(
    val mode: AuthMode,
    val isConfigured: Boolean,
    /** Google needs its own OAuth client in Firebase; phone and email work without it. */
    val isGoogleConfigured: Boolean = true,
    val method: AuthMethod = AuthMethod.Phone,
    val step: AuthStep = AuthStep.Form,
    val name: String = "",
    val countryCode: String = "+91",
    val phone: String = "",
    val email: String = "",
    val password: String = "",
    val otp: String = "",
    val nameError: String? = null,
    val phoneError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val otpError: String? = null,
    val busy: AuthBusy? = null,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    /** The E.164 number the current code was sent to. */
    val codeSentTo: String? = null,
    val resendSecondsLeft: Int = 0,
    val outcome: AuthOutcome? = null,
) {
    val isBusy: Boolean get() = busy != null
    val isSignUp: Boolean get() = mode == AuthMode.SignUp
}

/**
 * Drives sign-up and log-in with phone OTP, email + password (with email verification)
 * and Google. Nothing is reported as success until Firebase confirms it.
 */
class AuthViewModel(
    mode: AuthMode,
    private val auth: AuthRepository,
    private val profiles: ProfileRepository,
    private val googleTokens: GoogleIdTokenProvider,
    private val sessionResolver: SessionResolver = SessionResolver(auth, profiles),
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AuthUiState(
            mode = mode,
            isConfigured = auth.isConfigured,
            isGoogleConfigured = googleTokens.isConfigured,
            errorMessage = AuthError.NotConfigured.message.takeUnless { auth.isConfigured },
        ),
    )
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private var verificationId: String? = null
    private var resendToken: PhoneResendToken? = null
    private var countdown: Job? = null

    fun onMethodChange(method: AuthMethod) = _uiState.update {
        if (it.isBusy) it else it.copy(method = method, errorMessage = notConfiguredMessage(), infoMessage = null)
    }

    fun onNameChange(value: String) = _uiState.update {
        it.copy(name = value.take(Validation.NameMaxLength + 10), nameError = null)
    }

    fun onCountryCodeChange(value: String) = _uiState.update {
        val digits = value.filter { c -> c.isDigit() }.take(3)
        it.copy(countryCode = "+$digits", phoneError = null)
    }

    fun onPhoneChange(value: String) = _uiState.update {
        it.copy(phone = value.filter { c -> c.isDigit() || c == ' ' }.take(16), phoneError = null)
    }

    fun onEmailChange(value: String) = _uiState.update { it.copy(email = value.trim(), emailError = null) }

    fun onPasswordChange(value: String) = _uiState.update { it.copy(password = value, passwordError = null) }

    fun onOtpChange(value: String) {
        val code = value.filter { it.isDigit() }.take(Validation.OtpLength)
        _uiState.update { it.copy(otp = code, otpError = null, errorMessage = null) }
        if (code.length == Validation.OtpLength) verifyOtp()
    }

    fun dismissMessage() = _uiState.update { it.copy(errorMessage = notConfiguredMessage(), infoMessage = null) }

    /** Primary button: send an OTP (phone) or create / log in to an email account. */
    fun submit() {
        val state = _uiState.value
        if (state.isBusy || !requireConfigured()) return
        when (state.method) {
            AuthMethod.Phone -> sendCode(resend = false)
            AuthMethod.Email -> submitEmail()
        }
    }

    fun resendCode() {
        val state = _uiState.value
        if (state.isBusy || state.resendSecondsLeft > 0) return
        sendCode(resend = true)
    }

    /** "Change number" on the OTP step. */
    fun backToForm() {
        countdown?.cancel()
        verificationId = null
        _uiState.update {
            it.copy(step = AuthStep.Form, otp = "", otpError = null, errorMessage = null, resendSecondsLeft = 0)
        }
    }

    fun verifyOtp() {
        val state = _uiState.value
        val id = verificationId
        if (state.isBusy || id == null) return
        Validation.otpError(state.otp)?.let { error ->
            _uiState.update { it.copy(otpError = error) }
            return
        }
        run(AuthBusy.Verifying, onError = { it.copy(otp = "") }) {
            finishSignIn(auth.verifyPhoneCode(id, state.otp))
        }
    }

    fun signInWithGoogle() {
        val state = _uiState.value
        if (state.isBusy || !requireConfigured()) return
        if (!googleTokens.isConfigured) {
            val notSetUp = AuthException(AuthError.GoogleNotConfigured, code = "MISSING_DEFAULT_WEB_CLIENT_ID")
            _uiState.update { it.copy(errorMessage = notSetUp.message, infoMessage = null) }
            return
        }
        // On sign-up the name is optional here; if it's filled in it has to be valid.
        if (state.isSignUp && state.name.isNotBlank()) {
            Validation.nameError(state.name)?.let { error ->
                _uiState.update { it.copy(nameError = error) }
                return
            }
        }
        run(AuthBusy.Google) {
            finishSignIn(auth.signInWithGoogle(googleTokens.requestIdToken()))
        }
    }

    fun sendPasswordReset() {
        val state = _uiState.value
        if (state.isBusy || !requireConfigured()) return
        Validation.emailError(state.email)?.let { error ->
            _uiState.update { it.copy(emailError = error) }
            return
        }
        run(AuthBusy.Submitting) {
            auth.sendPasswordReset(state.email)
            _uiState.update {
                it.copy(infoMessage = "If an account exists for ${state.email}, a reset link is on its way.")
            }
        }
    }

    fun onOutcomeHandled() = _uiState.update { it.copy(outcome = null) }

    private fun sendCode(resend: Boolean) {
        val state = _uiState.value
        val nameError = if (state.isSignUp && !resend) Validation.nameError(state.name) else null
        val phoneError = Validation.phoneError(state.countryCode, state.phone)
        if (nameError != null || phoneError != null) {
            _uiState.update { it.copy(nameError = nameError, phoneError = phoneError) }
            return
        }
        val number = requireNotNull(Validation.toE164(state.countryCode, state.phone))
        run(if (resend) AuthBusy.Resending else AuthBusy.Submitting) {
            when (val result = auth.sendPhoneCode(number, if (resend) resendToken else null)) {
                is PhoneCodeResult.CodeSent -> {
                    verificationId = result.verificationId
                    resendToken = result.resendToken ?: resendToken
                    _uiState.update {
                        it.copy(
                            step = AuthStep.Otp,
                            codeSentTo = number,
                            otp = "",
                            infoMessage = if (resend) "A new code is on its way." else null,
                        )
                    }
                    startResendCountdown()
                }
                is PhoneCodeResult.AutoVerified -> finishSignIn(result.user)
            }
        }
    }

    private fun submitEmail() {
        val state = _uiState.value
        val nameError = if (state.isSignUp) Validation.nameError(state.name) else null
        val emailError = Validation.emailError(state.email)
        val passwordError = if (state.isSignUp) {
            Validation.passwordError(state.password)
        } else {
            "Enter your password.".takeIf { state.password.isEmpty() }
        }
        if (nameError != null || emailError != null || passwordError != null) {
            _uiState.update { it.copy(nameError = nameError, emailError = emailError, passwordError = passwordError) }
            return
        }
        run(AuthBusy.Submitting, onError = { it.copy(password = "") }) {
            val user = if (state.isSignUp) {
                auth.signUpWithEmail(state.email, state.password)
            } else {
                auth.signInWithEmail(state.email, state.password)
            }
            // Don't keep the password in memory once Firebase has it.
            _uiState.update { it.copy(password = "") }
            finishSignIn(user)
        }
    }

    /** Creates the profile on sign-up when we have a valid name, then decides where to go. */
    private suspend fun finishSignIn(user: AuthUser) {
        val state = _uiState.value
        val typedName = Validation.normalizeName(state.name)
            .takeIf { state.isSignUp && Validation.nameError(it) == null }
        val prefill = typedName ?: user.displayName
        try {
            if (typedName != null && profiles.getProfile(user.uid) == null) {
                profiles.createProfile(
                    UserProfile(uid = user.uid, name = typedName, email = user.email, phoneNumber = user.phoneNumber),
                )
            }
            val stage = sessionResolver.resolve()
            _uiState.update { it.copy(outcome = AuthOutcome(stage, prefill)) }
        } catch (_: ProfileException) {
            // Signed in, but the profile couldn't be saved or read: let the person finish it there.
            val stage = if (sessionResolver.needsEmailVerification(user)) SessionStage.VerifyEmail else SessionStage.ProfileSetup
            _uiState.update { it.copy(outcome = AuthOutcome(stage, prefill)) }
        }
    }

    private fun startResendCountdown() {
        countdown?.cancel()
        countdown = viewModelScope.launch {
            for (seconds in ResendCooldownSeconds downTo 0) {
                _uiState.update { it.copy(resendSecondsLeft = seconds) }
                if (seconds > 0) delay(1_000)
            }
        }
    }

    private fun requireConfigured(): Boolean {
        if (auth.isConfigured) return true
        _uiState.update { it.copy(errorMessage = AuthError.NotConfigured.message) }
        return false
    }

    private fun notConfiguredMessage(): String? = AuthError.NotConfigured.message.takeUnless { auth.isConfigured }

    private fun run(
        busy: AuthBusy,
        onError: (AuthUiState) -> AuthUiState = { it },
        block: suspend () -> Unit,
    ) {
        _uiState.update { it.copy(busy = busy, errorMessage = null, infoMessage = null) }
        viewModelScope.launch {
            try {
                block()
            } catch (e: AuthException) {
                _uiState.update {
                    onError(it).copy(errorMessage = e.message.takeUnless { e.error == AuthError.Cancelled })
                }
            } finally {
                _uiState.update { it.copy(busy = null) }
            }
        }
    }

    private companion object {
        const val ResendCooldownSeconds = 60
    }
}
