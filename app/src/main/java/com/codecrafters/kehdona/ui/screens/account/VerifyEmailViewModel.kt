package com.codecrafters.kehdona.ui.screens.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codecrafters.kehdona.data.auth.AuthException
import com.codecrafters.kehdona.data.auth.AuthRepository
import com.codecrafters.kehdona.data.profile.ProfileException
import com.codecrafters.kehdona.data.profile.ProfileRepository
import com.codecrafters.kehdona.domain.SessionResolver
import com.codecrafters.kehdona.domain.SessionStage
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class VerifyEmailBusy { Checking, Resending }

data class VerifyEmailUiState(
    val email: String?,
    val busy: VerifyEmailBusy? = null,
    val resendSecondsLeft: Int = 0,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    val next: SessionStage? = null,
)

/** Waits for the person to click the Firebase verification link, then moves them on. */
class VerifyEmailViewModel(
    private val auth: AuthRepository,
    profiles: ProfileRepository,
    private val sessionResolver: SessionResolver = SessionResolver(auth, profiles),
) : ViewModel() {

    private val _uiState = MutableStateFlow(VerifyEmailUiState(email = auth.currentUser?.email))
    val uiState: StateFlow<VerifyEmailUiState> = _uiState.asStateFlow()

    private var countdown: Job? = null

    /** "I've verified my email". */
    fun checkVerified() {
        if (_uiState.value.busy != null) return
        _uiState.update { it.copy(busy = VerifyEmailBusy.Checking, errorMessage = null, infoMessage = null) }
        viewModelScope.launch {
            try {
                val user = auth.reloadUser()
                when {
                    user == null -> _uiState.update { it.copy(next = SessionStage.SignedOut) }
                    !user.isEmailVerified -> _uiState.update {
                        it.copy(errorMessage = "Not verified yet. Tap the link in the email we sent, then try again.")
                    }
                    else -> {
                        val next = sessionResolver.resolve()
                        _uiState.update { it.copy(next = next) }
                    }
                }
            } catch (e: AuthException) {
                _uiState.update { it.copy(errorMessage = e.message) }
            } catch (e: ProfileException) {
                _uiState.update { it.copy(errorMessage = e.message) }
            } finally {
                _uiState.update { it.copy(busy = null) }
            }
        }
    }

    fun resend() {
        val state = _uiState.value
        if (state.busy != null || state.resendSecondsLeft > 0) return
        _uiState.update { it.copy(busy = VerifyEmailBusy.Resending, errorMessage = null, infoMessage = null) }
        viewModelScope.launch {
            try {
                auth.sendEmailVerification()
                _uiState.update { it.copy(infoMessage = "Sent! Check your inbox and spam folder.") }
                startCountdown()
            } catch (e: AuthException) {
                _uiState.update { it.copy(errorMessage = e.message) }
            } finally {
                _uiState.update { it.copy(busy = null) }
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            auth.signOut()
            _uiState.update { it.copy(next = SessionStage.SignedOut) }
        }
    }

    fun onNavigated() = _uiState.update { it.copy(next = null) }

    private fun startCountdown() {
        countdown?.cancel()
        countdown = viewModelScope.launch {
            for (seconds in 30 downTo 0) {
                _uiState.update { it.copy(resendSecondsLeft = seconds) }
                if (seconds > 0) delay(1_000)
            }
        }
    }
}
