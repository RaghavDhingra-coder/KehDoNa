package com.codecrafters.kehdona.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codecrafters.kehdona.data.onboarding.OnboardingContent
import com.codecrafters.kehdona.data.onboarding.OnboardingRepository
import com.codecrafters.kehdona.model.OnboardingPage
import java.io.IOException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OnboardingUiState(
    val pages: List<OnboardingPage>,
    val isSaving: Boolean = false,
    /** True once the flow is finished and the screen should move on. */
    val isComplete: Boolean = false,
)

class OnboardingViewModel(
    private val repository: OnboardingRepository,
    pages: List<OnboardingPage> = OnboardingContent.pages,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState(pages))
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    /** Called from both "Skip" and the final "Continue". Safe to call repeatedly. */
    fun complete() {
        val current = _uiState.value
        if (current.isSaving || current.isComplete) return
        _uiState.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            try {
                repository.setOnboardingCompleted()
            } catch (_: IOException) {
                // Not critical: the worst case is seeing the introduction again next launch.
            }
            _uiState.update { it.copy(isSaving = false, isComplete = true) }
        }
    }
}
