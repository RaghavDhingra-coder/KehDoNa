package com.codecrafters.kehdona.auth

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.codecrafters.kehdona.data.auth.AuthProvider
import com.codecrafters.kehdona.data.profile.UserProfile
import com.codecrafters.kehdona.data.profile.profileException
import com.google.firebase.firestore.FirebaseFirestoreException.Code
import com.codecrafters.kehdona.domain.SessionStage
import com.codecrafters.kehdona.testutil.FakeAuthRepository
import com.codecrafters.kehdona.testutil.FakeAuthRepository.Companion.user
import com.codecrafters.kehdona.testutil.FakeProfileRepository
import com.codecrafters.kehdona.testutil.MainDispatcherRule
import com.codecrafters.kehdona.testutil.TestReplies
import com.codecrafters.kehdona.ui.screens.account.ProfileSetupViewModel
import com.codecrafters.kehdona.ui.screens.account.VerifyEmailViewModel
import com.codecrafters.kehdona.ui.screens.home.HomeUiState
import com.codecrafters.kehdona.ui.screens.home.HomeViewModel
import com.codecrafters.kehdona.ui.screens.profile.MyProfileUiState
import com.codecrafters.kehdona.ui.screens.profile.MyProfileViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class) // Firebase exception types need the Android runtime.
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
    fun verifyEmail_thenFirestoreDenies_showsRealCode_andStaysPut() {
        val auth = FakeAuthRepository(signedIn = emailUser).apply { emailVerifiedOnReload = true }
        val profiles = FakeProfileRepository().apply { failWith = profileException(Code.PERMISSION_DENIED) }
        val vm = VerifyEmailViewModel(auth, profiles)

        vm.checkVerified()

        val message = vm.uiState.value.errorMessage!!
        assertTrue(message.contains("PERMISSION_DENIED"))
        assertTrue(!message.contains("sign in again"))
        assertNull(vm.uiState.value.next)
        assertNotNull(auth.currentUser) // the person stays signed in and can retry

        profiles.failWith = null
        vm.checkVerified()
        assertEquals(SessionStage.ProfileSetup, vm.uiState.value.next)
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
        val existing = UserProfile("uid-1", "Riya", null, "+919876543210")
        val profiles = FakeProfileRepository(existing).withPersonality("uid-1")
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

    @Test
    fun myProfile_showsSavedReplyAndSections_andSurvivesSignOutSignIn() {
        val profiles = FakeProfileRepository(UserProfile("uid-1", "Riya", null, "+91987")).withPersonality("uid-1")
        val auth = FakeAuthRepository(signedIn = user())

        val first = MyProfileViewModel(auth, profiles).uiState.value as MyProfileUiState.Ready
        assertEquals(TestReplies.Valid, first.personality!!.response)
        assertTrue(first.sections.isNotEmpty())

        HomeViewModel(auth, profiles).signOut()
        assertEquals(MyProfileUiState.SignedOut, MyProfileViewModel(auth, profiles).uiState.value)

        val again = FakeAuthRepository(signedIn = user()) // same account, new session
        val second = MyProfileViewModel(again, profiles).uiState.value as MyProfileUiState.Ready
        assertEquals(first, second)
    }

    @Test
    fun myProfile_withoutImport_isReadyWithNoPersonality() {
        val profiles = FakeProfileRepository(UserProfile("uid-1", "Riya", null, "+91987"))

        val state = MyProfileViewModel(FakeAuthRepository(signedIn = user()), profiles).uiState.value as MyProfileUiState.Ready

        assertNull(state.personality)
        assertTrue(state.sections.isEmpty())
    }

    @Test
    fun home_exposesPersonality_whenFinalized() {
        val profiles = FakeProfileRepository(UserProfile("uid-1", "Riya", null, "+91987")).withPersonality("uid-1")

        val state = HomeViewModel(FakeAuthRepository(signedIn = user()), profiles).uiState.value as HomeUiState.Ready

        assertNotNull(state.personality)
    }
}
