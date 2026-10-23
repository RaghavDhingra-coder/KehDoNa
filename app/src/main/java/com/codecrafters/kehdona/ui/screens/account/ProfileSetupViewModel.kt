package com.codecrafters.kehdona.ui.screens.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codecrafters.kehdona.data.auth.AuthRepository
import com.codecrafters.kehdona.data.profile.ProfileException
import com.codecrafters.kehdona.data.profile.ProfileRepository
import com.codecrafters.kehdona.data.profile.UserProfile
import com.codecrafters.kehdona.domain.MatchingDetailsInput
import com.codecrafters.kehdona.domain.SessionResolver
import com.codecrafters.kehdona.domain.SessionStage
import com.codecrafters.kehdona.domain.Validation
import com.codecrafters.kehdona.model.Gender
import com.codecrafters.kehdona.model.MatchPreference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileSetupUiState(
    val name: String,
    val contact: String?,
    val nameError: String? = null,
    val matching: MatchingDetailsInput = MatchingDetailsInput(),
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

    fun onGenderChange(value: Gender) = _uiState.update { it.copy(matching = it.matching.withGender(value)) }

    fun onSelfDescriptionChange(value: String) =
        _uiState.update { it.copy(matching = it.matching.withSelfDescription(value)) }

    fun onPreferenceChange(value: MatchPreference) =
        _uiState.update { it.copy(matching = it.matching.withPreference(value)) }

    fun save() {
        val state = _uiState.value
        if (state.isSaving) return
        val user = auth.currentUser
        if (user == null) {
            _uiState.update { it.copy(next = SessionStage.SignedOut) }
            return
        }
        val nameError = Validation.nameError(state.name)
        val matching = state.matching.validated()
        if (nameError != null || matching.hasErrors) {
            _uiState.update { it.copy(nameError = nameError, matching = matching) }
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
                        gender = matching.gender,
                        genderSelfDescription = matching.cleanSelfDescription,
                        matchPreference = matching.preference,
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
