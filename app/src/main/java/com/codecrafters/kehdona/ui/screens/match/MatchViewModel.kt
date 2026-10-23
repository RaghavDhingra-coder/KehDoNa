package com.codecrafters.kehdona.ui.screens.match

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codecrafters.kehdona.data.auth.AuthRepository
import com.codecrafters.kehdona.data.match.Match
import com.codecrafters.kehdona.data.match.MatchException
import com.codecrafters.kehdona.data.match.MatchFailure
import com.codecrafters.kehdona.data.match.MatchRepository
import com.codecrafters.kehdona.data.match.MatchStatus
import com.codecrafters.kehdona.data.profile.ProfileException
import com.codecrafters.kehdona.data.profile.ProfileRepository
import com.codecrafters.kehdona.domain.MatchingDetailsInput
import com.codecrafters.kehdona.domain.SessionStage
import com.codecrafters.kehdona.model.Gender
import com.codecrafters.kehdona.model.MatchPreference
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class MatchStep {
    Loading,

    /** Gender or match preference missing: asked for here, never guessed. */
    NeedsDetails,
    NeedsPersonality,
    Ready,
    Searching,
    Waiting,
    Matched,
    Error,
}

data class MatchUiState(
    val step: MatchStep = MatchStep.Loading,
    val details: MatchingDetailsInput = MatchingDetailsInput(),
    val isSavingDetails: Boolean = false,
    val isCancelling: Boolean = false,
    val match: Match? = null,
    val errorMessage: String? = null,
    /** Short confirmation, e.g. after cancelling a search. */
    val infoMessage: String? = null,
    val next: SessionStage? = null,
)

/**
 * The Match flow. The backend decides who matches whom; this screen asks, waits (listening to
 * the live status, so a match made by the other person shows up here too), cancels and shows
 * the result.
 */
class MatchViewModel(
    private val auth: AuthRepository,
    private val profiles: ProfileRepository,
    private val matches: MatchRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MatchUiState())
    val uiState: StateFlow<MatchUiState> = _uiState.asStateFlow()

    private var statusJob: Job? = null

    init {
        load()
    }

    fun load() {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            _uiState.update { it.copy(next = SessionStage.SignedOut) }
            return
        }
        _uiState.update { it.copy(step = MatchStep.Loading, errorMessage = null) }
        viewModelScope.launch {
            try {
                val profile = profiles.getProfile(uid)
                when {
                    profile == null -> _uiState.update { it.copy(next = SessionStage.ProfileSetup) }
                    !profile.hasMatchingDetails -> _uiState.update {
                        it.copy(
                            step = MatchStep.NeedsDetails,
                            details = MatchingDetailsInput.from(
                                profile.gender,
                                profile.genderSelfDescription,
                                profile.matchPreference,
                            ),
                        )
                    }
                    profiles.getPersonality(uid) == null -> _uiState.update { it.copy(step = MatchStep.NeedsPersonality) }
                    else -> watchStatus(uid)
                }
            } catch (e: ProfileException) {
                showError(e.message)
            }
        }
    }

    fun onGenderChange(value: Gender) = _uiState.update { it.copy(details = it.details.withGender(value)) }

    fun onSelfDescriptionChange(value: String) = _uiState.update { it.copy(details = it.details.withSelfDescription(value)) }

    fun onPreferenceChange(value: MatchPreference) = _uiState.update { it.copy(details = it.details.withPreference(value)) }

    /** "Save and continue" on the complete-your-profile step. */
    fun saveDetails() {
        val state = _uiState.value
        val uid = auth.currentUser?.uid ?: return
        if (state.isSavingDetails) return
        val details = state.details.validated()
        if (details.hasErrors) {
            _uiState.update { it.copy(details = details) }
            return
        }
        _uiState.update { it.copy(isSavingDetails = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                profiles.updateMatchingDetails(
                    uid,
                    requireNotNull(details.gender),
                    details.cleanSelfDescription,
                    requireNotNull(details.preference),
                )
                _uiState.update { it.copy(isSavingDetails = false) }
                load()
            } catch (e: ProfileException) {
                _uiState.update { it.copy(isSavingDetails = false, errorMessage = e.message) }
            }
        }
    }

    /** The Match button: the backend picks the most compatible available member, or queues us. */
    fun findMatch() {
        val step = _uiState.value.step
        if (step == MatchStep.Searching || step == MatchStep.Matched) return
        _uiState.update { it.copy(step = MatchStep.Searching, errorMessage = null, infoMessage = null) }
        viewModelScope.launch { request() }
    }

    fun cancelSearch() {
        if (_uiState.value.isCancelling) return
        _uiState.update { it.copy(isCancelling = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                when (val status = matches.cancelRequest()) {
                    // Matched just before the cancel landed: the match stands and is shown.
                    is MatchStatus.Matched -> showMatch(status.matchId)
                    else -> _uiState.update {
                        it.copy(step = MatchStep.Ready, infoMessage = "Search cancelled. Tap Match whenever you're ready.")
                    }
                }
            } catch (e: MatchException) {
                _uiState.update { it.copy(errorMessage = e.message) }
            } finally {
                _uiState.update { it.copy(isCancelling = false) }
            }
        }
    }

    fun onNavigated() = _uiState.update { it.copy(next = null) }

    private fun watchStatus(uid: String) {
        statusJob?.cancel()
        statusJob = viewModelScope.launch {
            var first = true
            matches.observeStatus(uid)
                .distinctUntilChanged()
                .catch { e -> showError((e as? MatchException)?.message ?: "We couldn't load your match status.") }
                .collect { status ->
                    when (status) {
                        is MatchStatus.Matched -> showMatch(status.matchId)
                        MatchStatus.Waiting -> {
                            if (_uiState.value.step != MatchStep.Searching) _uiState.update { it.copy(step = MatchStep.Waiting) }
                            // Reopening while queued: look again, someone compatible may be waiting now.
                            if (first) launch { request(quiet = true) }
                        }
                        MatchStatus.Idle -> if (_uiState.value.step != MatchStep.Searching) {
                            _uiState.update { it.copy(step = MatchStep.Ready) }
                        }
                    }
                    first = false
                }
        }
    }

    private suspend fun request(quiet: Boolean = false) {
        try {
            when (val status = matches.requestMatch()) {
                is MatchStatus.Matched -> showMatch(status.matchId)
                MatchStatus.Waiting -> _uiState.update { if (it.step == MatchStep.Matched) it else it.copy(step = MatchStep.Waiting) }
                MatchStatus.Idle -> _uiState.update { it.copy(step = MatchStep.Ready) }
            }
        } catch (e: MatchException) {
            when (e.failure) {
                MatchFailure.DetailsMissing, MatchFailure.PersonalityMissing, MatchFailure.ProfileMissing -> load()
                MatchFailure.NotSignedIn -> _uiState.update { it.copy(next = SessionStage.SignedOut) }
                else -> if (quiet) {
                    _uiState.update { it.copy(errorMessage = e.message) }
                } else {
                    showError(e.message)
                }
            }
        }
    }

    private suspend fun showMatch(matchId: String) {
        val uid = auth.currentUser?.uid ?: return
        if (_uiState.value.match?.id == matchId) {
            _uiState.update { it.copy(step = MatchStep.Matched) }
            return
        }
        try {
            val match = matches.getMatch(uid, matchId)
            _uiState.update { it.copy(step = MatchStep.Matched, match = match, errorMessage = null, infoMessage = null) }
        } catch (e: MatchException) {
            showError(e.message)
        }
    }

    private fun showError(message: String?) = _uiState.update {
        it.copy(step = MatchStep.Error, errorMessage = message ?: "Something went wrong. Please try again.")
    }
}
