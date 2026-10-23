package com.codecrafters.kehdona.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codecrafters.kehdona.data.auth.AuthRepository
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
    data class Ready(val profile: UserProfile, val isSigningOut: Boolean = false) : HomeUiState
    data class Error(val message: String) : HomeUiState
    data object SignedOut : HomeUiState
}

/** The signed-in landing: shows the private profile and handles log-out. */
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
                profiles.getProfile(user.uid)?.let { HomeUiState.Ready(it) }
                    ?: HomeUiState.Error("We couldn't find your profile. Please log in again.")
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
