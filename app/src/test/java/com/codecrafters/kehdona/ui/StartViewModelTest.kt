package com.codecrafters.kehdona.ui

import com.codecrafters.kehdona.data.profile.ProfileException
import com.codecrafters.kehdona.domain.SessionStage
import com.codecrafters.kehdona.testutil.FakeOnboardingRepository
import com.codecrafters.kehdona.testutil.MainDispatcherRule
import com.codecrafters.kehdona.ui.screens.start.StartDestination
import com.codecrafters.kehdona.ui.screens.start.StartUiState
import com.codecrafters.kehdona.ui.screens.start.StartViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class StartViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun firstLaunch_startsOnWelcome() {
        val viewModel = StartViewModel(FakeOnboardingRepository(completed = false))

        assertEquals(StartUiState.Ready(StartDestination.Welcome), viewModel.uiState.value)
    }

    @Test
    fun returningUser_skipsStraightToAuthEntry() {
        val viewModel = StartViewModel(FakeOnboardingRepository(completed = true))

        assertEquals(StartUiState.Ready(StartDestination.AuthEntry), viewModel.uiState.value)
    }

    @Test
    fun readFailure_showsError_andRetryRecovers() {
        val repository = FakeOnboardingRepository().apply { failReads = true }
        val viewModel = StartViewModel(repository)

        assertTrue(viewModel.uiState.value is StartUiState.Error)

        repository.failReads = false
        viewModel.retry()

        assertEquals(StartUiState.Ready(StartDestination.Welcome), viewModel.uiState.value)
    }

    @Test
    fun signedInMember_skipsWelcome_andResumesTheirStage() {
        val viewModel = StartViewModel(FakeOnboardingRepository(completed = false)) { SessionStage.Home }

        assertEquals(StartUiState.Ready(StartDestination.Home), viewModel.uiState.value)
    }

    @Test
    fun profileLoadFailure_showsError() {
        val viewModel = StartViewModel(FakeOnboardingRepository()) { throw ProfileException("offline") }

        assertTrue(viewModel.uiState.value is StartUiState.Error)
    }
}
