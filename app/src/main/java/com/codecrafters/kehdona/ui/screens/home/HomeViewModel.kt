package com.codecrafters.kehdona.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codecrafters.kehdona.data.auth.AuthRepository
import com.codecrafters.kehdona.data.profile.PersonalityProfile
import com.codecrafters.kehdona.data.profile.ProfileException
import com.codecrafters.kehdona.data.profile.ProfileRepository
import com.codecrafters.kehdona.data.profile.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Ready(
        val profile: UserProfile,
        /** Null until the person imports their profile; once set it is final. */
        val personality: PersonalityProfile?,
        val isSigningOut: Boolean = false,
    ) : HomeUiState
    data class Error(val message: String) : HomeUiState
    data object SignedOut : HomeUiState
}

/** The signed-in landing: a personal welcome, the way into My Profile, and log-out. */
class HomeViewModel(
    private val auth: AuthRepository,
    private val profiles: ProfileRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        val user = auth.currentUser
        if (user == null) {
            _uiState.value = HomeUiState.SignedOut
            return
        }
        _uiState.value = HomeUiState.Loading
        viewModelScope.launch {
            _uiState.value = try {
                val profile = profiles.getProfile(user.uid)
                if (profile == null) {
                    HomeUiState.Error("We couldn't find your profile. Please log in again.")
                } else {
                    HomeUiState.Ready(profile, profiles.getPersonality(user.uid))
                }
            } catch (e: ProfileException) {
                HomeUiState.Error(e.message ?: "We couldn't load your profile.")
            }
        }
    }

    fun signOut() {
        _uiState.update { if (it is HomeUiState.Ready) it.copy(isSigningOut = true) else it }
        viewModelScope.launch {
            auth.signOut()
            _uiState.value = HomeUiState.SignedOut
        }
    }
}
