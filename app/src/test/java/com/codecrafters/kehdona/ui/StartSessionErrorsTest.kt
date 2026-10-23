package com.codecrafters.kehdona.ui

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.codecrafters.kehdona.data.profile.UserProfile
import com.codecrafters.kehdona.data.profile.profileException
import com.codecrafters.kehdona.domain.SessionResolver
import com.codecrafters.kehdona.testutil.FakeAuthRepository
import com.codecrafters.kehdona.testutil.FakeAuthRepository.Companion.user
import com.codecrafters.kehdona.testutil.FakeOnboardingRepository
import com.codecrafters.kehdona.testutil.FakeProfileRepository
import com.codecrafters.kehdona.testutil.MainDispatcherRule
import com.codecrafters.kehdona.ui.screens.start.StartDestination
import com.codecrafters.kehdona.ui.screens.start.StartUiState
import com.codecrafters.kehdona.ui.screens.start.StartViewModel
import com.google.firebase.firestore.FirebaseFirestoreException.Code
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Regression: app start for a signed-in member, with the real SessionResolver. Each Firestore
 * failure must be reported as itself (not "check your connection"), and a missing personality
 * profile is incomplete onboarding, not an error.
 */
@RunWith(AndroidJUnit4::class) // Firestore's Code enum needs the Android runtime.
class StartSessionErrorsTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private val auth = FakeAuthRepository(signedIn = user(uid = "u1"))
    private val profiles = FakeProfileRepository(UserProfile("u1", "Riya", null, "+919876543210"))

    private fun start() = StartViewModel(
        repository = FakeOnboardingRepository(completed = true),
        signOut = auth::signOut,
        resolveSession = SessionResolver(auth, profiles)::resolve,
    )

    @Test
    fun missingPersonalityProfile_resumesOnboarding_notAnError() {
        assertEquals(StartUiState.Ready(StartDestination.DiscoverVibe), start().uiState.value)
    }

    @Test
    fun missingUserProfile_goesToProfileSetup() {
        profiles.profiles.clear()

        assertEquals(StartUiState.Ready(StartDestination.ProfileSetup), start().uiState.value)
    }

    @Test
    fun personalityReadDenied_showsPermissionDenied_notAConnectionProblem() {
        // Exactly what the device logged: users/{uid} readable, personalityProfiles/{uid} denied.
        profiles.failPersonalityReadWith = profileException(Code.PERMISSION_DENIED)

        val state = start().uiState.value as StartUiState.Error

        assertTrue(state.message.endsWith("(Firestore: PERMISSION_DENIED)"))
        assertFalse(state.message.contains("connection"))
        assertTrue(state.canSignOut)
        assertTrue("A denied read must not be treated as 'no profile yet'", profiles.personalities.isEmpty())
    }

    @Test
    fun networkFailure_isReportedAsNetwork() {
        profiles.failWith = profileException(Code.UNAVAILABLE)

        val state = start().uiState.value as StartUiState.Error

        assertTrue(state.message.contains("connection"))
        assertTrue(state.message.contains("UNAVAILABLE"))
    }

    @Test
    fun expiredSession_signsOut_andShowsSignIn_insteadOfAnError() {
        profiles.failWith = profileException(Code.UNAUTHENTICATED)

        val vm = start()

        assertEquals(StartUiState.Ready(StartDestination.AuthEntry), vm.uiState.value)
        assertNull(auth.currentUser)
    }

    @Test
    fun logOutFromTheError_signsOut_andRestartsAtSignIn() {
        profiles.failWith = profileException(Code.PERMISSION_DENIED)
        val vm = start()
        assertTrue(vm.uiState.value is StartUiState.Error)

        vm.signOutAndRestart()

        assertNull(auth.currentUser)
        assertEquals(StartUiState.Ready(StartDestination.AuthEntry), vm.uiState.value)
    }

    @Test
    fun retryAfterTheCauseIsFixed_proceeds() {
        profiles.failPersonalityReadWith = profileException(Code.PERMISSION_DENIED)
        val vm = start()

        profiles.failPersonalityReadWith = null // e.g. the updated rules were published
        vm.retry()

        assertEquals(StartUiState.Ready(StartDestination.DiscoverVibe), vm.uiState.value)
    }
}
