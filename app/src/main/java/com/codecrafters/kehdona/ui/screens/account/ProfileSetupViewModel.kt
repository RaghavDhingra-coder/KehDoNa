package com.codecrafters.kehdona.ui.screens.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codecrafters.kehdona.data.auth.AuthRepository
import com.codecrafters.kehdona.data.profile.ProfileException
import com.codecrafters.kehdona.data.profile.ProfileRepository
import com.codecrafters.kehdona.data.profile.UserProfile
import com.codecrafters.kehdona.domain.SessionResolver
import com.codecrafters.kehdona.domain.SessionStage
import com.codecrafters.kehdona.domain.Validation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileSetupUiState(
    val name: String,
    val contact: String?,
    val nameError: String? = null,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val next: SessionStage? = null,
)

/** Asks for the profile name when an account has none yet (e.g. first Google or phone log-in). */
class ProfileSetupViewModel(
    prefillName: String?,
    private val auth: AuthRepository,
    private val profiles: ProfileRepository,
    private val sessionResolver: SessionResolver = SessionResolver(auth, profiles),
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ProfileSetupUiState(
            name = prefillName ?: auth.currentUser?.displayName.orEmpty(),
            contact = auth.currentUser?.let { it.email ?: it.phoneNumber },
        ),
    )
    val uiState: StateFlow<ProfileSetupUiState> = _uiState.asStateFlow()

    fun onNameChange(value: String) = _uiState.update {
        it.copy(name = value.take(Validation.NameMaxLength + 10), nameError = null)
    }

    fun save() {
        val state = _uiState.value
        if (state.isSaving) return
        val user = auth.currentUser
        if (user == null) {
            _uiState.update { it.copy(next = SessionStage.SignedOut) }
            return
        }
        Validation.nameError(state.name)?.let { error ->
            _uiState.update { it.copy(nameError = error) }
            return
        }
        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                profiles.createProfile(
                    UserProfile(
                        uid = user.uid,
                        name = Validation.normalizeName(state.name),
                        email = user.email,
                        phoneNumber = user.phoneNumber,
                    ),
                )
                val next = sessionResolver.resolve()
                _uiState.update { it.copy(isSaving = false, next = next) }
            } catch (e: ProfileException) {
                _uiState.update { it.copy(isSaving = false, errorMessage = e.message) }
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
}
