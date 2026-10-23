package com.codecrafters.kehdona.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.codecrafters.kehdona.di.AppContainer
import com.codecrafters.kehdona.domain.SessionResolver
import com.codecrafters.kehdona.navigation.KehDoNaNavHost
import com.codecrafters.kehdona.ui.components.AmbientBackground
import com.codecrafters.kehdona.ui.components.ErrorState
import com.codecrafters.kehdona.ui.components.LoadingState
import com.codecrafters.kehdona.ui.screens.start.StartUiState
import com.codecrafters.kehdona.ui.screens.start.StartViewModel

/** Root composable: resolves the start screen, then hands off to navigation. */
@Composable
fun KehDoNaApp(container: AppContainer) {
    val startViewModel: StartViewModel = viewModel {
        val sessionResolver = SessionResolver(container.authRepository, container.profileRepository)
        StartViewModel(container.onboardingRepository, sessionResolver::resolve)
    }
    val state by startViewModel.uiState.collectAsStateWithLifecycle()

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when (val current = state) {
            StartUiState.Loading -> AmbientBackground { LoadingState() }
            is StartUiState.Error -> AmbientBackground {
                ErrorState(
                    title = "Something went wrong",
                    message = current.message,
                    onRetry = startViewModel::retry,
                )
            }
            is StartUiState.Ready -> KehDoNaNavHost(
                navController = rememberNavController(),
                startDestination = current.destination,
                container = container,
            )
        }
    }
}
