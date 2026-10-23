package com.codecrafters.kehdona.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.codecrafters.kehdona.di.AppContainer
import com.codecrafters.kehdona.domain.SessionStage
import com.codecrafters.kehdona.model.AuthMode
import com.codecrafters.kehdona.ui.screens.account.ProfileSetupScreen
import com.codecrafters.kehdona.ui.screens.account.ProfileSetupViewModel
import com.codecrafters.kehdona.ui.screens.account.VerifyEmailScreen
import com.codecrafters.kehdona.ui.screens.account.VerifyEmailViewModel
import com.codecrafters.kehdona.ui.screens.auth.AuthEntryScreen
import com.codecrafters.kehdona.ui.screens.auth.AuthScreen
import com.codecrafters.kehdona.ui.screens.auth.AuthViewModel
import com.codecrafters.kehdona.ui.screens.home.HomeScreen
import com.codecrafters.kehdona.ui.screens.home.HomeViewModel
import com.codecrafters.kehdona.ui.screens.onboarding.OnboardingScreen
import com.codecrafters.kehdona.ui.screens.onboarding.OnboardingViewModel
import com.codecrafters.kehdona.ui.screens.start.StartDestination
import com.codecrafters.kehdona.ui.screens.vibe.VibeScreen
import com.codecrafters.kehdona.ui.screens.vibe.VibeViewModel
import com.codecrafters.kehdona.ui.screens.welcome.WelcomeScreen

@Composable
fun KehDoNaNavHost(
    navController: NavHostController,
    startDestination: StartDestination,
    container: AppContainer,
    modifier: Modifier = Modifier,
) {
    val auth = container.authRepository
    val profiles = container.profileRepository

    /** Moves to an account stage and clears the back stack, so Back never re-enters a finished step. */
    fun goTo(stage: SessionStage, prefillName: String? = null) {
        val route: Any = when (stage) {
            SessionStage.SignedOut -> AuthEntryRoute
            SessionStage.VerifyEmail -> VerifyEmailRoute
            SessionStage.ProfileSetup -> ProfileSetupRoute(prefillName)
            SessionStage.DiscoverVibe -> VibeRoute()
            SessionStage.Home -> HomeRoute
        }
        navController.navigate(route) {
            popUpTo(navController.graph.id) { inclusive = true }
            launchSingleTop = true
        }
    }

    // Tweens follow the system animator scale, so "Remove animations" turns these off.
    NavHost(
        navController = navController,
        startDestination = when (startDestination) {
            StartDestination.Welcome -> WelcomeRoute
            StartDestination.AuthEntry -> AuthEntryRoute
            StartDestination.VerifyEmail -> VerifyEmailRoute
            StartDestination.ProfileSetup -> ProfileSetupRoute()
            StartDestination.DiscoverVibe -> VibeRoute()
            StartDestination.Home -> HomeRoute
        },
        modifier = modifier,
        enterTransition = { fadeIn(tween(350)) + slideInHorizontally(tween(350)) { it / 12 } },
        exitTransition = { fadeOut(tween(250)) },
        popEnterTransition = { fadeIn(tween(350)) },
        popExitTransition = { fadeOut(tween(250)) + slideOutHorizontally(tween(250)) { it / 12 } },
    ) {
        composable<WelcomeRoute> {
            WelcomeScreen(
                onGetStarted = { navController.navigate(OnboardingRoute) },
                onLogIn = { navController.navigate(AuthRoute(AuthMode.LogIn)) },
            )
        }
        composable<OnboardingRoute> {
            OnboardingScreen(
                viewModel = viewModel { OnboardingViewModel(container.onboardingRepository) },
                onFinished = {
                    // Onboarding is done: make the auth entry the only screen in the back stack.
                    navController.navigate(AuthEntryRoute) {
                        popUpTo(navController.graph.id) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }
        composable<AuthEntryRoute> {
            AuthEntryScreen(
                onCreateAccount = { navController.navigate(AuthRoute(AuthMode.SignUp)) },
                onLogIn = { navController.navigate(AuthRoute(AuthMode.LogIn)) },
                onReplayIntro = { navController.navigate(OnboardingRoute) },
            )
        }
        composable<AuthRoute> { entry ->
            val mode = entry.toRoute<AuthRoute>().mode
            AuthScreen(
                viewModel = viewModel { AuthViewModel(mode, auth, profiles, container.googleIdTokenProvider) },
                onBack = { navController.popBackStack() },
                onSwitchMode = {
                    val other = if (mode == AuthMode.SignUp) AuthMode.LogIn else AuthMode.SignUp
                    navController.navigate(AuthRoute(other)) {
                        popUpTo<AuthRoute> { inclusive = true }
                    }
                },
                onSignedIn = { goTo(it.stage, it.prefillName) },
            )
        }
        composable<VerifyEmailRoute> {
            VerifyEmailScreen(
                viewModel = viewModel { VerifyEmailViewModel(auth, profiles) },
                onNext = { goTo(it) },
            )
        }
        composable<ProfileSetupRoute> { entry ->
            val prefill = entry.toRoute<ProfileSetupRoute>().prefillName
            ProfileSetupScreen(
                viewModel = viewModel { ProfileSetupViewModel(prefill, auth, profiles) },
                onNext = { goTo(it) },
            )
        }
        composable<VibeRoute> { entry ->
            val updating = entry.toRoute<VibeRoute>().updating
            VibeScreen(
                viewModel = viewModel { VibeViewModel(auth, profiles) },
                updating = updating,
                onNext = { goTo(it) },
                onBack = { navController.popBackStack() },
            )
        }
        composable<HomeRoute> {
            HomeScreen(
                viewModel = viewModel { HomeViewModel(auth, profiles) },
                onUpdateVibe = { navController.navigate(VibeRoute(updating = true)) },
                onSignedOut = { goTo(SessionStage.SignedOut) },
            )
        }
    }
}
