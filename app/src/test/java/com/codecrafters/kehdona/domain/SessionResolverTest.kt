package com.codecrafters.kehdona.domain

import com.codecrafters.kehdona.data.auth.AuthProvider
import com.codecrafters.kehdona.data.profile.UserProfile
import com.codecrafters.kehdona.testutil.FakeAuthRepository
import com.codecrafters.kehdona.testutil.FakeAuthRepository.Companion.user
import com.codecrafters.kehdona.testutil.FakeProfileRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SessionResolverTest {
    private suspend fun stage(auth: FakeAuthRepository, profiles: FakeProfileRepository = FakeProfileRepository()) =
        SessionResolver(auth, profiles).resolve()

    private val profile = UserProfile("uid-1", "Riya", null, "+919876543210")

    @Test
    fun noUser_isSignedOut() = runTest {
        assertEquals(SessionStage.SignedOut, stage(FakeAuthRepository()))
    }

    @Test
    fun unconfiguredFirebase_isSignedOut() = runTest {
        assertEquals(SessionStage.SignedOut, stage(FakeAuthRepository(isConfigured = false)))
    }

    @Test
    fun unverifiedEmail_mustVerifyFirst() = runTest {
        val auth = FakeAuthRepository(signedIn = user(provider = AuthProvider.Email, email = "a@b.co"))
        assertEquals(SessionStage.VerifyEmail, stage(auth, FakeProfileRepository(profile)))
    }

    @Test
    fun noProfile_needsSetup() = runTest {
        assertEquals(SessionStage.ProfileSetup, stage(FakeAuthRepository(signedIn = user())))
    }

    @Test
    fun profileWithoutPersonality_discoversVibe_unlessSkipped() = runTest {
        val auth = FakeAuthRepository(signedIn = user())
        assertEquals(SessionStage.DiscoverVibe, stage(auth, FakeProfileRepository(profile)))
        assertEquals(SessionStage.Home, stage(auth, FakeProfileRepository(profile.copy(personalitySkipped = true))))
        assertEquals(SessionStage.Home, stage(auth, FakeProfileRepository(profile).withPersonality("uid-1")))
    }
}
