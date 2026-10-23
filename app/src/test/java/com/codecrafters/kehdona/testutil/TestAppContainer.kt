package com.codecrafters.kehdona.testutil

import com.codecrafters.kehdona.di.AppContainer

/** [AppContainer] built from fakes, for UI and navigation tests. */
class TestAppContainer(
    override val onboardingRepository: FakeOnboardingRepository = FakeOnboardingRepository(),
    override val authRepository: FakeAuthRepository = FakeAuthRepository(),
    override val profileRepository: FakeProfileRepository = FakeProfileRepository(),
    override val googleIdTokenProvider: FakeGoogleIdTokenProvider = FakeGoogleIdTokenProvider(),
) : AppContainer
