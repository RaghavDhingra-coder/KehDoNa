package com.codecrafters.kehdona.ui.screens.vibe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codecrafters.kehdona.data.auth.AuthRepository
import com.codecrafters.kehdona.data.profile.ProfileException
import com.codecrafters.kehdona.data.profile.ProfileRepository
import com.codecrafters.kehdona.domain.PersonalityPrompt
import com.codecrafters.kehdona.domain.SessionStage
import com.codecrafters.kehdona.domain.Validation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class VibeStep { Loading, LoadFailed, Intro, Paste, ConfirmName, Done }

/** Where the prompt was opened, so the paste screen can say the right thing. */
enum class ChatGptTarget { App, Browser, None }

data class VibeUiState(
    val step: VibeStep = VibeStep.Loading,
    /** The verified profile name; never changed without the person's explicit confirmation. */
    val profileName: String = "",
    val profileText: String = "",
    val profileError: String? = null,
    val openedIn: ChatGptTarget? = null,
    /** A different name found in the AI's text. Unverified: only shown back for confirmation. */
    val suggestedName: String? = null,
    val customName: String = "",
    val customNameError: String? = null,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val next: SessionStage? = null,
)

/**
 * "Discover your vibe": hands the fixed prompt to ChatGPT, saves the profile the person
 * pastes back, and asks before any AI-suggested name could touch their profile.
 */
class VibeViewModel(
    private val auth: AuthRepository,
    private val profiles: ProfileRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(VibeUiState())
    val uiState: StateFlow<VibeUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        val user = auth.currentUser
        if (user == null) {
            _uiState.update { it.copy(next = SessionStage.SignedOut) }
            return
        }
        _uiState.update { it.copy(step = VibeStep.Loading, errorMessage = null) }
        viewModelScope.launch {
            try {
                val profile = profiles.getProfile(user.uid)
                if (profile == null) {
                    _uiState.update { it.copy(next = SessionStage.ProfileSetup) }
                } else {
                    _uiState.update { it.copy(step = VibeStep.Intro, profileName = profile.name) }
                }
            } catch (e: ProfileException) {
                _uiState.update { it.copy(step = VibeStep.LoadFailed, errorMessage = e.message) }
            }
        }
    }

    /** The prompt is on the clipboard and ChatGPT was opened (or couldn't be). */
    fun onPromptHandedOff(target: ChatGptTarget) = _uiState.update {
        it.copy(step = VibeStep.Paste, openedIn = target, errorMessage = null)
    }

    /** Skip ahead when the person already has a profile from ChatGPT. */
    fun goToPaste() = _uiState.update { it.copy(step = VibeStep.Paste) }

    fun backToIntro() = _uiState.update { it.copy(step = VibeStep.Intro, profileError = null, errorMessage = null) }

    fun onProfileTextChange(value: String) = _uiState.update {
        it.copy(profileText = value.take(PersonalityPrompt.MaxProfileLength + 1), profileError = null)
    }

    fun saveProfile() {
        val state = _uiState.value
        if (state.isSaving) return
        PersonalityPrompt.profileError(state.profileText)?.let { error ->
            _uiState.update { it.copy(profileError = error) }
            return
        }
        val text = state.profileText.trim()
        withUser { uid ->
            profiles.savePersonality(uid, text)
            val suggested = PersonalityPrompt.extractSuggestedName(text)
            _uiState.update {
                if (PersonalityPrompt.isNameConflict(it.profileName, suggested)) {
                    it.copy(step = VibeStep.ConfirmName, suggestedName = suggested, customName = it.profileName)
                } else {
                    it.copy(step = VibeStep.Done)
                }
            }
        }
    }

    /** Explicit choice: the signup name stays. */
    fun keepVerifiedName() = _uiState.update { it.copy(step = VibeStep.Done, suggestedName = null) }

    /** Explicit choice: the person confirms the AI-suggested name is really theirs. */
    fun confirmSuggestedName() {
        val suggested = _uiState.value.suggestedName ?: return
        updateName(suggested)
    }

    fun onCustomNameChange(value: String) = _uiState.update {
        it.copy(customName = value.take(Validation.NameMaxLength + 10), customNameError = null)
    }

    /** Explicit choice: the person types the correct name themselves. */
    fun confirmCustomName() {
        val name = _uiState.value.customName
        Validation.nameError(name)?.let { error ->
            _uiState.update { it.copy(customNameError = error) }
            return
        }
        updateName(Validation.normalizeName(name))
    }

    fun skipForNow() {
        if (_uiState.value.isSaving) return
        withUser { uid ->
            profiles.skipPersonality(uid)
            _uiState.update { it.copy(next = SessionStage.Home) }
        }
    }

    fun finish() = _uiState.update { it.copy(next = SessionStage.Home) }

    fun dismissError() = _uiState.update { it.copy(errorMessage = null) }

    fun onNavigated() = _uiState.update { it.copy(next = null) }

    private fun updateName(name: String) {
        if (_uiState.value.isSaving) return
        if (name == _uiState.value.profileName) {
            keepVerifiedName()
            return
        }
        withUser { uid ->
            profiles.updateName(uid, name)
            _uiState.update { it.copy(step = VibeStep.Done, profileName = name, suggestedName = null) }
        }
    }

    private fun withUser(block: suspend (uid: String) -> Unit) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            _uiState.update { it.copy(next = SessionStage.SignedOut) }
            return
        }
        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                block(uid)
            } catch (e: ProfileException) {
                _uiState.update { it.copy(errorMessage = e.message) }
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }
}
