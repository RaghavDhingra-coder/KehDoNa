package com.codecrafters.kehdona.ui.screens.start

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codecrafters.kehdona.data.onboarding.OnboardingRepository
import com.codecrafters.kehdona.data.profile.ProfileException
import com.codecrafters.kehdona.domain.SessionStage
import java.io.IOException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Where the app opens: first-time visitors see the welcome screen, members resume their account flow. */
enum class StartDestination { Welcome, AuthEntry, VerifyEmail, ProfileSetup, DiscoverVibe, Home }

sealed interface StartUiState {
    data object Loading : StartUiState
    data class Ready(val destination: StartDestination) : StartUiState
    /** [canSignOut] when a signed-in account couldn't be loaded, so the person isn't stuck. */
    data class Error(val message: String, val canSignOut: Boolean = false) : StartUiState
}

/** Decides the first screen from the signed-in session, then from whether onboarding was completed. */
class StartViewModel(
    private val repository: OnboardingRepository,
    private val signOut: suspend () -> Unit = {},
    // Last, so `StartViewModel(repository) { stage }` keeps meaning "resolve the session".
    private val resolveSession: suspend () -> SessionStage = { SessionStage.SignedOut },
) : ViewModel() {

    private val _uiState = MutableStateFlow<StartUiState>(StartUiState.Loading)
    val uiState: StateFlow<StartUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun retry() = load()

    /** Escape hatch from a profile-load error: sign out and start from the sign-in screen. */
    fun signOutAndRestart() {
        viewModelScope.launch {
            signOut()
            load()
        }
    }

    private fun load() {
        _uiState.value = StartUiState.Loading
        viewModelScope.launch {
            _uiState.value = try {
                val destination = when (resolveSession()) {
                    SessionStage.SignedOut -> {
                        val completed = repository.hasCompletedOnboarding.first()
                        if (completed) StartDestination.AuthEntry else StartDestination.Welcome
                    }
                    SessionStage.VerifyEmail -> StartDestination.VerifyEmail
                    SessionStage.ProfileSetup -> StartDestination.ProfileSetup
                    SessionStage.DiscoverVibe -> StartDestination.DiscoverVibe
                    SessionStage.Home -> StartDestination.Home
                }
                StartUiState.Ready(destination)
            } catch (_: IOException) {
                StartUiState.Error("We couldn't read KehDoNa's saved settings on this device.")
            } catch (e: ProfileException) {
                if (e.code == "UNAUTHENTICATED") {
                    // The saved session is no longer valid: sign out and show sign-in, not an error.
                    signOut()
                    val completed = runCatching { repository.hasCompletedOnboarding.first() }.getOrDefault(true)
                    StartUiState.Ready(if (completed) StartDestination.AuthEntry else StartDestination.Welcome)
                } else {
                    // Keep the repository's message: it names the real cause and ends with the
                    // Firestore code (e.g. PERMISSION_DENIED vs UNAVAILABLE).
                    StartUiState.Error(
                        e.message ?: "We couldn't load your account. Please try again.",
                        canSignOut = true,
                    )
                }
            }
        }
    }
}
