package com.codecrafters.kehdona.auth

import com.codecrafters.kehdona.data.auth.AuthError
import com.codecrafters.kehdona.data.auth.AuthProvider
import com.codecrafters.kehdona.data.profile.UserProfile
import com.codecrafters.kehdona.domain.SessionStage
import com.codecrafters.kehdona.model.AuthMode
import com.codecrafters.kehdona.model.Gender
import com.codecrafters.kehdona.model.MatchPreference
import com.codecrafters.kehdona.testutil.FakeAuthRepository
import com.codecrafters.kehdona.testutil.FakeGoogleIdTokenProvider
import com.codecrafters.kehdona.testutil.FakeProfileRepository
import com.codecrafters.kehdona.testutil.MainDispatcherRule
import com.codecrafters.kehdona.ui.screens.auth.AuthMethod
import com.codecrafters.kehdona.ui.screens.auth.AuthOutcome
import com.codecrafters.kehdona.ui.screens.auth.AuthStep
import com.codecrafters.kehdona.ui.screens.auth.AuthViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AuthViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private val auth = FakeAuthRepository()
    private val profiles = FakeProfileRepository()
    private val google = FakeGoogleIdTokenProvider()

    /** Sign-up forms in these tests state a gender (now required) unless [statedGender] is false. */
    private fun viewModel(mode: AuthMode, repo: FakeAuthRepository = auth, statedGender: Boolean = true) =
        AuthViewModel(mode, repo, profiles, google).apply {
            if (mode == AuthMode.SignUp && statedGender) onGenderChange(Gender.Woman)
        }

    private val state get() = vm.uiState.value
    private lateinit var vm: AuthViewModel

    @Test
    fun phoneSignUp_validatesBeforeContactingFirebase() {
        vm = viewModel(AuthMode.SignUp)
        vm.onPhoneChange("12")
        vm.submit()

        assertNotNull(state.nameError)
        assertNotNull(state.phoneError)
        assertEquals(0, auth.calls)
        assertEquals(AuthStep.Form, state.step)
    }

    @Test
    fun phoneSignUp_sendsOtp_thenVerifies_andCreatesProfileWithTypedName() {
        vm = viewModel(AuthMode.SignUp)
        vm.onNameChange("  Riya   Sharma ")
        vm.onPhoneChange("98765 43210")
        vm.submit()

        assertEquals(listOf("+919876543210"), auth.sentCodesTo)
        assertEquals(AuthStep.Otp, state.step)
        assertEquals("+919876543210", state.codeSentTo)
        assertEquals(60, state.resendSecondsLeft)

        vm.onOtpChange("123456") // completing the code verifies automatically

        assertEquals(AuthOutcome(SessionStage.DiscoverVibe, "Riya Sharma"), state.outcome)
        assertEquals("Riya Sharma", profiles.profiles.getValue("phone-uid").name)
        assertEquals("+919876543210", profiles.profiles.getValue("phone-uid").phoneNumber)
    }

    @Test
    fun wrongOtp_showsError_clearsCode_andStaysSignedOut() {
        vm = viewModel(AuthMode.SignUp)
        vm.onNameChange("Riya")
        vm.onPhoneChange("9876543210")
        vm.submit()

        vm.onOtpChange("000000")

        assertEquals(AuthError.InvalidCode.message, state.errorMessage)
        assertEquals("", state.otp)
        assertNull(state.outcome)
        assertNull(auth.currentUser)
    }

    @Test
    fun changeNumber_returnsToForm_keepingWhatWasTyped() {
        vm = viewModel(AuthMode.LogIn)
        vm.onPhoneChange("9876543210")
        vm.submit()

        vm.backToForm()

        assertEquals(AuthStep.Form, state.step)
        assertEquals("9876543210", state.phone)
    }

    @Test
    fun resend_isBlockedDuringCooldown() {
        vm = viewModel(AuthMode.LogIn)
        vm.onPhoneChange("9876543210")
        vm.submit()

        vm.resendCode()

        assertEquals(1, auth.sentCodesTo.size)
    }

    @Test
    fun autoVerifiedPhone_logInForExistingMember_goesHome_withoutTouchingProfile() {
        profiles.profiles["phone-uid"] = UserProfile("phone-uid", "Riya", null, "+919876543210")
        profiles.withPersonality("phone-uid")
        auth.autoVerify = true
        vm = viewModel(AuthMode.LogIn)
        vm.onPhoneChange("9876543210")
        vm.submit()

        assertEquals(SessionStage.Home, state.outcome?.stage)
        assertEquals("Riya", profiles.profiles.getValue("phone-uid").name)
    }

    @Test
    fun phoneSignUp_withExistingProfile_neverOverwritesTheVerifiedName() {
        profiles.profiles["phone-uid"] = UserProfile("phone-uid", "Riya", null, "+919876543210")
        vm = viewModel(AuthMode.SignUp)
        vm.onNameChange("Someone Else")
        vm.onPhoneChange("9876543210")
        vm.submit()
        vm.onOtpChange("123456")

        assertEquals("Riya", profiles.profiles.getValue("phone-uid").name)
    }

    @Test
    fun emailSignUp_createsAccountAndProfile_thenRequiresVerification() {
        vm = viewModel(AuthMode.SignUp)
        vm.onMethodChange(AuthMethod.Email)
        vm.onNameChange("Riya")
        vm.onEmailChange("riya@college.edu")
        vm.onPasswordChange("secret123")
        vm.submit()

        assertEquals(SessionStage.VerifyEmail, state.outcome?.stage)
        assertEquals(1, auth.verificationEmailsSent)
        assertEquals("Riya", profiles.profiles.getValue("email-uid").name)
        assertEquals("", state.password) // not kept in memory after use
    }

    @Test
    fun emailSignUp_weakPassword_isRejectedLocally() {
        vm = viewModel(AuthMode.SignUp)
        vm.onMethodChange(AuthMethod.Email)
        vm.onNameChange("Riya")
        vm.onEmailChange("riya@college.edu")
        vm.onPasswordChange("password")
        vm.submit()

        assertNotNull(state.passwordError)
        assertEquals(0, auth.calls)
    }

    @Test
    fun emailLogIn_wrongPassword_showsFriendlyError_andClearsPassword() {
        auth.failNextWith = AuthError.InvalidCredentials
        vm = viewModel(AuthMode.LogIn)
        vm.onMethodChange(AuthMethod.Email)
        vm.onEmailChange("riya@college.edu")
        vm.onPasswordChange("wrong-pass1")
        vm.submit()

        assertEquals(AuthError.InvalidCredentials.message, state.errorMessage)
        assertEquals("", state.password)
        assertNull(state.outcome)
        assertTrue(!state.isBusy)
    }

    @Test
    fun forgotPassword_needsValidEmail_thenConfirms() {
        vm = viewModel(AuthMode.LogIn)
        vm.onMethodChange(AuthMethod.Email)
        vm.sendPasswordReset()
        assertNotNull(state.emailError)

        vm.onEmailChange("riya@college.edu")
        vm.sendPasswordReset()

        assertEquals(1, auth.passwordResetsSent)
        assertNotNull(state.infoMessage)
    }

    @Test
    fun googleLogIn_newUser_goesToProfileSetup_prefilledWithGoogleName() {
        vm = viewModel(AuthMode.LogIn)
        vm.signInWithGoogle()

        assertEquals(AuthOutcome(SessionStage.ProfileSetup, "Riya G"), state.outcome)
        assertTrue(profiles.profiles.isEmpty())
    }

    @Test
    fun googleSignUp_withTypedName_usesTypedNameNotGoogleName() {
        vm = viewModel(AuthMode.SignUp)
        vm.onNameChange("Riya Sharma")
        vm.signInWithGoogle()

        assertEquals("Riya Sharma", profiles.profiles.getValue("google-uid").name)
        assertEquals(SessionStage.DiscoverVibe, state.outcome?.stage)
    }

    @Test
    fun googleCancelled_isSilent_andStaysOnForm() {
        google.error = AuthError.Cancelled
        vm = viewModel(AuthMode.LogIn)
        vm.signInWithGoogle()

        assertNull(state.errorMessage)
        assertNull(state.outcome)
        assertTrue(!state.isBusy)
    }

    @Test
    fun profileSaveFailure_afterSignIn_stillLetsPersonFinishSetup() {
        profiles.failAll = true
        vm = viewModel(AuthMode.SignUp)
        vm.onNameChange("Riya")
        vm.onPhoneChange("9876543210")
        vm.submit()
        vm.onOtpChange("123456")

        assertEquals(AuthOutcome(SessionStage.ProfileSetup, "Riya"), state.outcome)
    }

    @Test
    fun unconfiguredFirebase_isReportedHonestly_andNeverFakesSuccess() {
        val unconfigured = FakeAuthRepository(isConfigured = false)
        vm = viewModel(AuthMode.SignUp, unconfigured)

        assertTrue(!state.isConfigured)
        assertEquals(AuthError.NotConfigured.message, state.errorMessage)

        vm.onNameChange("Riya")
        vm.onPhoneChange("9876543210")
        vm.submit()
        vm.signInWithGoogle()

        assertEquals(0, unconfigured.calls)
        assertNull(state.outcome)
        assertNull(unconfigured.currentUser)
    }

    @Test
    fun emailUser_signedInViaEmail_isEmailProvider() {
        vm = viewModel(AuthMode.LogIn)
        vm.onMethodChange(AuthMethod.Email)
        vm.onEmailChange("riya@college.edu")
        vm.onPasswordChange("secret123")
        vm.submit()

        assertEquals(AuthProvider.Email, auth.currentUser?.provider)
        assertEquals(SessionStage.ProfileSetup, state.outcome?.stage)
    }

    @Test
    fun googleNotConfigured_disablesGoogleOnly_withSpecificMessage() {
        val vm = AuthViewModel(AuthMode.SignUp, auth, profiles, FakeGoogleIdTokenProvider(isConfigured = false))
        assertTrue(vm.uiState.value.isConfigured)
        assertTrue(!vm.uiState.value.isGoogleConfigured)
        assertNull(vm.uiState.value.errorMessage) // phone and email stay usable

        vm.signInWithGoogle()

        val message = vm.uiState.value.errorMessage!!
        assertTrue(message.startsWith(AuthError.GoogleNotConfigured.message))
        assertTrue(message.contains("MISSING_DEFAULT_WEB_CLIENT_ID"))
        assertTrue(!message.contains(AuthError.NotConfigured.message))
        assertEquals(0, auth.calls)
    }

    @Test
    fun phoneProviderDisabled_showsSpecificMessageWithFirebaseCode() {
        auth.failNextWith = AuthError.MethodNotEnabled
        auth.failNextCode = "ERROR_OPERATION_NOT_ALLOWED"
        vm = viewModel(AuthMode.SignUp)
        vm.onNameChange("Riya")
        vm.onPhoneChange("9876543210")
        vm.submit()

        assertEquals("${AuthError.MethodNotEnabled.message} (ERROR_OPERATION_NOT_ALLOWED)", state.errorMessage)
        assertEquals(AuthStep.Form, state.step)
    }

    @Test
    fun phoneAppNotAuthorized_isReportedSeparately() {
        auth.failNextWith = AuthError.AppNotAuthorized
        auth.failNextCode = "ERROR_APP_NOT_AUTHORIZED"
        vm = viewModel(AuthMode.LogIn)
        vm.onPhoneChange("9876543210")
        vm.submit()

        assertEquals("${AuthError.AppNotAuthorized.message} (ERROR_APP_NOT_AUTHORIZED)", state.errorMessage)
    }

    @Test
    fun signUp_requiresAStatedGender_andNeverGuessesOne() {
        vm = viewModel(AuthMode.SignUp, statedGender = false)
        vm.onNameChange("Riya")
        vm.onPhoneChange("9876543210")
        vm.submit()

        assertNotNull(state.matching.genderError)
        assertNotNull(state.matching.preferenceError)
        assertEquals(0, auth.calls)
    }

    @Test
    fun signUp_savesStatedGenderAndPreference_onTheProfile() {
        vm = viewModel(AuthMode.SignUp, statedGender = false)
        vm.onNameChange("Alex")
        vm.onGenderChange(Gender.SelfDescribe)
        vm.onSelfDescriptionChange("  genderfluid ")
        vm.onPreferenceChange(MatchPreference.All)
        vm.onPhoneChange("9876543210")
        vm.submit()
        vm.onOtpChange("123456")

        val saved = profiles.profiles.getValue("phone-uid")
        assertEquals(Gender.SelfDescribe, saved.gender)
        assertEquals("genderfluid", saved.genderSelfDescription)
        assertEquals(MatchPreference.All, saved.matchPreference)
    }

    @Test
    fun googleSignUp_withoutGender_isAllowed_profileIsCompletedLater() {
        vm = viewModel(AuthMode.SignUp, statedGender = false)
        vm.signInWithGoogle()

        assertEquals(SessionStage.ProfileSetup, state.outcome?.stage)
        assertNull(profiles.profiles["google-uid"]?.gender)
    }
}
