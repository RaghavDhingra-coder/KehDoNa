package com.codecrafters.kehdona.auth

import com.codecrafters.kehdona.data.auth.AuthProvider
import com.codecrafters.kehdona.data.profile.UserProfile
import com.codecrafters.kehdona.domain.SessionStage
import com.codecrafters.kehdona.testutil.FakeAuthRepository
import com.codecrafters.kehdona.testutil.FakeAuthRepository.Companion.user
import com.codecrafters.kehdona.testutil.FakeProfileRepository
import com.codecrafters.kehdona.testutil.MainDispatcherRule
import com.codecrafters.kehdona.ui.screens.account.ProfileSetupViewModel
import com.codecrafters.kehdona.ui.screens.account.VerifyEmailViewModel
import com.codecrafters.kehdona.ui.screens.home.HomeUiState
import com.codecrafters.kehdona.ui.screens.home.HomeViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AccountViewModelsTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private val emailUser = user(uid = "u1", provider = AuthProvider.Email, email = "riya@college.edu")

    @Test
    fun verifyEmail_notYetVerified_explainsAndStays() {
        val auth = FakeAuthRepository(signedIn = emailUser)
        val vm = VerifyEmailViewModel(auth, FakeProfileRepository())

        vm.checkVerified()

        assertNotNull(vm.uiState.value.errorMessage)
        assertNull(vm.uiState.value.next)
    }

    @Test
    fun verifyEmail_verified_movesOnToVibe() {
        val auth = FakeAuthRepository(signedIn = emailUser).apply { emailVerifiedOnReload = true }
        val vm = VerifyEmailViewModel(auth, FakeProfileRepository(UserProfile("u1", "Riya", "riya@college.edu", null)))

        vm.checkVerified()

        assertEquals(SessionStage.DiscoverVibe, vm.uiState.value.next)
    }

    @Test
    fun verifyEmail_resend_startsCooldown() {
        val auth = FakeAuthRepository(signedIn = emailUser)
        val vm = VerifyEmailViewModel(auth, FakeProfileRepository())

        vm.resend()
        vm.resend()

        assertEquals(1, auth.verificationEmailsSent)
        assertTrue(vm.uiState.value.resendSecondsLeft > 0)
    }

    @Test
    fun profileSetup_prefillsSuggestion_validates_andSavesTheConfirmedName() {
        val auth = FakeAuthRepository(signedIn = user(uid = "g1", provider = AuthProvider.Google, displayName = "Riya G"))
        val profiles = FakeProfileRepository()
        val vm = ProfileSetupViewModel(prefillName = null, auth = auth, profiles = profiles)
        assertEquals("Riya G", vm.uiState.value.name)

        vm.onNameChange("")
        vm.save()
        assertNotNull(vm.uiState.value.nameError)
        assertTrue(profiles.profiles.isEmpty())

        vm.onNameChange("Riya Sharma")
        vm.save()

        assertEquals("Riya Sharma", profiles.profiles.getValue("g1").name)
        assertEquals(SessionStage.DiscoverVibe, vm.uiState.value.next)
    }

    @Test
    fun profileSetup_onExistingProfile_onlyRenames_keepingContactAndPersonality() {
        val existing = UserProfile("uid-1", "Riya", null, "+919876543210", personalityProfile = "Warm.")
        val profiles = FakeProfileRepository(existing)
        val vm = ProfileSetupViewModel("Riya Sharma", FakeAuthRepository(signedIn = user(phone = "+910000000000")), profiles)

        vm.save()

        assertEquals(existing.copy(name = "Riya Sharma"), profiles.profiles.getValue("uid-1"))
        assertEquals(SessionStage.Home, vm.uiState.value.next)
    }

    @Test
    fun profileSetup_saveFailure_showsError() {
        val profiles = FakeProfileRepository().apply { failAll = true }
        val vm = ProfileSetupViewModel("Riya", FakeAuthRepository(signedIn = user()), profiles)

        vm.save()

        assertNotNull(vm.uiState.value.errorMessage)
        assertNull(vm.uiState.value.next)
    }

    @Test
    fun home_loadsProfile_andLogOutSignsOut() {
        val auth = FakeAuthRepository(signedIn = user())
        val vm = HomeViewModel(auth, FakeProfileRepository(UserProfile("uid-1", "Riya", null, "+91987")))
        assertEquals("Riya", (vm.uiState.value as HomeUiState.Ready).profile.name)

        vm.signOut()

        assertEquals(HomeUiState.SignedOut, vm.uiState.value)
        assertNull(auth.currentUser)
    }
}
