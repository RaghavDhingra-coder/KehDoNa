package com.codecrafters.kehdona.navigation

import com.codecrafters.kehdona.model.AuthMode
import kotlinx.serialization.Serializable

/** Type-safe destinations for Navigation Compose. */
@Serializable
data object WelcomeRoute

@Serializable
data object OnboardingRoute

@Serializable
data object AuthEntryRoute

/** Sign-up or log-in form (phone OTP, email, Google). */
@Serializable
data class AuthRoute(val mode: AuthMode)

@Serializable
data object VerifyEmailRoute

/** Asks for the profile name; [prefillName] is only a suggestion the person can edit. */
@Serializable
data class ProfileSetupRoute(val prefillName: String? = null)

/** "Let's discover your vibe". [updating] when reopened from home to refresh the profile. */
@Serializable
data class VibeRoute(val updating: Boolean = false)

@Serializable
data object HomeRoute
