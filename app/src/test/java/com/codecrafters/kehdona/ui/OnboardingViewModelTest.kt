package com.codecrafters.kehdona.ui

import com.codecrafters.kehdona.data.onboarding.OnboardingContent
import com.codecrafters.kehdona.testutil.FakeOnboardingRepository
import com.codecrafters.kehdona.testutil.MainDispatcherRule
import com.codecrafters.kehdona.ui.screens.onboarding.OnboardingViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class OnboardingViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun exposesThreeIntroPages() {
        val viewModel = OnboardingViewModel(FakeOnboardingRepository())

        assertEquals(OnboardingContent.pages, viewModel.uiState.value.pages)
        assertEquals(3, viewModel.uiState.value.pages.size)
    }

    @Test
    fun complete_persistsFlag_andMarksFlowComplete() {
        val repository = FakeOnboardingRepository()
        val viewModel = OnboardingViewModel(repository)

        viewModel.complete()

        assertTrue(repository.isCompleted)
        assertTrue(viewModel.uiState.value.isComplete)
        assertFalse(viewModel.uiState.value.isSaving)
    }

    @Test
    fun complete_isIdempotent() {
        val repository = FakeOnboardingRepository()
        val viewModel = OnboardingViewModel(repository)

        viewModel.complete()
        viewModel.complete()

        assertEquals(1, repository.writeCount)
    }

    @Test
    fun complete_stillFinishesWhenSavingFails() {
        val repository = FakeOnboardingRepository().apply { failWrites = true }
        val viewModel = OnboardingViewModel(repository)

        viewModel.complete()

        assertFalse(repository.isCompleted)
        assertTrue(viewModel.uiState.value.isComplete)
    }
}
