package com.codecrafters.kehdona.ui.screens.vibe

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codecrafters.kehdona.data.auth.AuthRepository
import com.codecrafters.kehdona.data.profile.ProfileException
import com.codecrafters.kehdona.data.profile.ProfileRepository
import com.codecrafters.kehdona.domain.PersonalityPrompt
import com.codecrafters.kehdona.domain.SessionStage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class VibeStep { Loading, LoadFailed, Intro, Paste }

/** Where the prompt was opened, so the paste screen can say the right thing. */
enum class ChatGptTarget { App, Browser, None }

data class VibeUiState(
    val step: VibeStep = VibeStep.Loading,
    val profileText: String = "",
    val profileError: String? = null,
    val openedIn: ChatGptTarget? = null,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val next: SessionStage? = null,
)

/**
 * "Discover your vibe": hands the fixed prompt to ChatGPT, then saves the reply the person
 * pastes back. A valid paste is saved and locked automatically and the person goes straight
 * home; the profile is final from then on. If saving fails nothing is locked and the pasted
 * text is kept (even across process death) so they can retry.
 */
class VibeViewModel(
    private val auth: AuthRepository,
    private val profiles: ProfileRepository,
    private val savedState: SavedStateHandle,
) : ViewModel() {

    private val _uiState = MutableStateFlow(VibeUiState(profileText = savedState[DraftKey] ?: ""))
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
                when {
                    profiles.getProfile(user.uid) == null -> _uiState.update { it.copy(next = SessionStage.ProfileSetup) }
                    // Already finalized: there is no re-import, so go home.
                    profiles.getPersonality(user.uid) != null -> _uiState.update { it.copy(next = SessionStage.Home) }
                    else -> _uiState.update {
                        it.copy(step = if (it.profileText.isBlank()) VibeStep.Intro else VibeStep.Paste)
                    }
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

    /** Skip ahead when the person already has a reply from ChatGPT. */
    fun goToPaste() = _uiState.update { it.copy(step = VibeStep.Paste) }

    fun backToIntro() = _uiState.update { it.copy(step = VibeStep.Intro, profileError = null, errorMessage = null) }

    /**
     * Typing just edits the draft. A paste (a large chunk arriving at once) that is a valid
     * reply is saved immediately, with no extra confirmation step.
     */
    fun onProfileTextChange(value: String) {
        val previous = _uiState.value.profileText
        val text = value.take(PersonalityPrompt.MaxProfileLength + 1)
        setDraft(text)
        val pasted = text.length - previous.length >= PasteThreshold
        if (pasted && PersonalityPrompt.profileError(text) == null) saveProfile()
    }

    /** "Paste from clipboard": replaces the draft and saves straight away if it's valid. */
    fun onPasteFromClipboard(text: String?) {
        if (text.isNullOrBlank()) {
            _uiState.update { it.copy(profileError = "Your clipboard is empty. Copy ChatGPT's reply first.") }
            return
        }
        setDraft(text.take(PersonalityPrompt.MaxProfileLength + 1))
        saveProfile()
    }

    fun saveProfile() {
        val state = _uiState.value
        if (state.isSaving) return
        PersonalityPrompt.profileError(state.profileText)?.let { error ->
            _uiState.update { it.copy(profileError = error) }
            return
        }
        val uid = auth.currentUser?.uid
        if (uid == null) {
            _uiState.update { it.copy(next = SessionStage.SignedOut) }
            return
        }
        _uiState.update { it.copy(isSaving = true, errorMessage = null, profileError = null) }
        viewModelScope.launch {
            try {
                profiles.finalizePersonality(uid, state.profileText.trim())
                savedState.remove<String>(DraftKey)
                _uiState.update { it.copy(isSaving = false, next = SessionStage.Home) }
            } catch (e: ProfileException) {
                // Nothing was locked; keep the pasted reply so the person can simply retry.
                _uiState.update { it.copy(isSaving = false, errorMessage = e.message) }
            }
        }
    }

    fun skipForNow() {
        val uid = auth.currentUser?.uid ?: return
        if (_uiState.value.isSaving) return
        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                profiles.skipPersonality(uid)
                _uiState.update { it.copy(isSaving = false, next = SessionStage.Home) }
            } catch (e: ProfileException) {
                _uiState.update { it.copy(isSaving = false, errorMessage = e.message) }
            }
        }
    }

    fun onNavigated() = _uiState.update { it.copy(next = null) }

    private fun setDraft(text: String) {
        savedState[DraftKey] = text
        _uiState.update { it.copy(profileText = text, profileError = null) }
    }

    private companion object {
        const val DraftKey = "personalityDraft"

        /** Characters added in one change that count as a paste rather than typing. */
        const val PasteThreshold = 40
    }
}
