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
    data class Error(val message: String) : StartUiState
}

/** Decides the first screen from the signed-in session, then from whether onboarding was completed. */
class StartViewModel(
    private val repository: OnboardingRepository,
    private val resolveSession: suspend () -> SessionStage = { SessionStage.SignedOut },
) : ViewModel() {

    private val _uiState = MutableStateFlow<StartUiState>(StartUiState.Loading)
    val uiState: StateFlow<StartUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun retry() = load()

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
            } catch (_: ProfileException) {
                StartUiState.Error("We couldn't load your account. Check your connection and try again.")
            }
        }
    }
}
