package com.codecrafters.kehdona.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codecrafters.kehdona.data.auth.AuthRepository
import com.codecrafters.kehdona.data.profile.PersonalityProfile
import com.codecrafters.kehdona.data.profile.ProfileException
import com.codecrafters.kehdona.data.profile.ProfileRepository
import com.codecrafters.kehdona.data.profile.UserProfile
import com.codecrafters.kehdona.domain.PersonalityParser
import com.codecrafters.kehdona.domain.PersonalitySection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface MyProfileUiState {
    data object Loading : MyProfileUiState
    data class Ready(
        val profile: UserProfile,
        val personality: PersonalityProfile?,
        /** Sections found in the imported reply; empty if it couldn't be split into sections. */
        val sections: List<PersonalitySection>,
    ) : MyProfileUiState
    data class Error(val message: String) : MyProfileUiState
    data object SignedOut : MyProfileUiState
}

/** Read-only view of the member's own profile, loaded from Firestore each time it opens. */
class MyProfileViewModel(
    private val auth: AuthRepository,
    private val profiles: ProfileRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<MyProfileUiState>(MyProfileUiState.Loading)
    val uiState: StateFlow<MyProfileUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        val user = auth.currentUser
        if (user == null) {
            _uiState.value = MyProfileUiState.SignedOut
            return
        }
        _uiState.value = MyProfileUiState.Loading
        viewModelScope.launch {
            _uiState.value = try {
                val profile = profiles.getProfile(user.uid)
                if (profile == null) {
                    MyProfileUiState.Error("We couldn't find your profile. Please log in again.")
                } else {
                    val personality = profiles.getPersonality(user.uid)
                    MyProfileUiState.Ready(
                        profile = profile,
                        personality = personality,
                        sections = personality?.let { PersonalityParser.parse(it.response) }.orEmpty(),
                    )
                }
            } catch (e: ProfileException) {
                MyProfileUiState.Error(e.message ?: "We couldn't load your profile.")
            }
        }
    }
}
