package com.codecrafters.kehdona.di

import android.app.Application
import com.codecrafters.kehdona.data.auth.AuthRepository
import com.codecrafters.kehdona.data.auth.CredentialManagerGoogleIdTokenProvider
import com.codecrafters.kehdona.data.auth.FirebaseAuthRepository
import com.codecrafters.kehdona.data.auth.ForegroundActivityTracker
import com.codecrafters.kehdona.data.auth.GoogleIdTokenProvider
import com.codecrafters.kehdona.data.onboarding.DataStoreOnboardingRepository
import com.codecrafters.kehdona.data.onboarding.OnboardingRepository
import com.codecrafters.kehdona.data.profile.FirestoreProfileRepository
import com.codecrafters.kehdona.data.profile.ProfileRepository

/** App-wide dependencies. Kept as plain constructor injection until the app needs more. */
interface AppContainer {
    val onboardingRepository: OnboardingRepository
    val authRepository: AuthRepository
    val profileRepository: ProfileRepository
    val googleIdTokenProvider: GoogleIdTokenProvider
}

class DefaultAppContainer(application: Application) : AppContainer {
    private val activityTracker = ForegroundActivityTracker(application)

    override val onboardingRepository: OnboardingRepository by lazy {
        DataStoreOnboardingRepository(application)
    }

    override val authRepository: AuthRepository by lazy {
        FirebaseAuthRepository(application, activityTracker)
    }

    override val profileRepository: ProfileRepository by lazy { FirestoreProfileRepository() }

    override val googleIdTokenProvider: GoogleIdTokenProvider by lazy {
        CredentialManagerGoogleIdTokenProvider(application, activityTracker)
    }
}
