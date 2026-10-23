package com.codecrafters.kehdona.ui

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.codecrafters.kehdona.data.auth.AuthError
import com.codecrafters.kehdona.data.profile.ProfileException
import com.codecrafters.kehdona.data.profile.UserProfile
import com.codecrafters.kehdona.navigation.AuthEntryRoute
import com.codecrafters.kehdona.navigation.AuthRoute
import com.codecrafters.kehdona.navigation.HomeRoute
import com.codecrafters.kehdona.navigation.MyProfileRoute
import com.codecrafters.kehdona.navigation.KehDoNaNavHost
import com.codecrafters.kehdona.navigation.OnboardingRoute
import com.codecrafters.kehdona.navigation.VibeRoute
import com.codecrafters.kehdona.navigation.WelcomeRoute
import com.codecrafters.kehdona.testutil.FakeAuthRepository
import com.codecrafters.kehdona.testutil.FakeGoogleIdTokenProvider
import com.codecrafters.kehdona.testutil.FakeOnboardingRepository
import com.codecrafters.kehdona.testutil.FakeProfileRepository
import com.codecrafters.kehdona.testutil.TestAppContainer
import com.codecrafters.kehdona.testutil.TestReplies
import com.codecrafters.kehdona.ui.screens.start.StartDestination
import com.codecrafters.kehdona.ui.theme.KehDoNaTheme
import kotlin.reflect.KClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w411dp-h891dp")
class NavigationTest {
    @get:Rule val composeRule = createComposeRule()

    private lateinit var navController: TestNavHostController
    private val repository = FakeOnboardingRepository()
    private var auth = FakeAuthRepository()
    private val profiles = FakeProfileRepository()

    private var google = FakeGoogleIdTokenProvider()

    private fun launch(start: StartDestination) {
        val container = TestAppContainer(repository, auth, profiles, google)
        composeRule.setContent {
            navController = TestNavHostController(LocalContext.current).apply {
                navigatorProvider.addNavigator(ComposeNavigator())
            }
            KehDoNaTheme { KehDoNaNavHost(navController, start, container) }
        }
    }

    private fun assertCurrentRoute(route: KClass<*>) {
        composeRule.waitForIdle()
        val destination = navController.currentBackStackEntry?.destination
        assertTrue("Expected $route but was ${destination?.route}", destination?.hasRoute(route) == true)
    }

    private fun clickText(text: String) {
        composeRule.onNodeWithText(text).performScrollTo().performClick()
    }

    @Test
    fun welcome_getStarted_opensOnboarding() {
        launch(StartDestination.Welcome)
        assertCurrentRoute(WelcomeRoute::class)

        clickText("Get started")

        assertCurrentRoute(OnboardingRoute::class)
        composeRule.onNodeWithText("Find people who see the world a little like you do.").assertIsDisplayed()
    }

    @Test
    fun welcome_existingAccount_opensLogIn() {
        launch(StartDestination.Welcome)

        clickText("I already have an account")

        assertCurrentRoute(AuthRoute::class)
        composeRule.onNodeWithText("Welcome back.").assertIsDisplayed()
    }

    @Test
    fun onboarding_skip_savesProgress_andClearsBackStack() {
        launch(StartDestination.Welcome)
        clickText("Get started")

        composeRule.onNodeWithText("Skip").performClick()

        assertCurrentRoute(AuthEntryRoute::class)
        assertTrue(repository.isCompleted)
        assertNull("Welcome/onboarding should not remain on the back stack", navController.previousBackStackEntry)
    }

    @Test
    fun onboarding_next_walksThroughPages_thenContinues() {
        launch(StartDestination.Welcome)
        clickText("Get started")

        composeRule.onNodeWithText("Next").performClick()
        composeRule.onNodeWithText("Some things are easier to say when your name stays out of it.").assertIsDisplayed()
        composeRule.onNodeWithText("Next").performClick()
        composeRule.onNodeWithText("Some feelings are better discovered together.").assertIsDisplayed()
        composeRule.onNodeWithText("Skip").assertDoesNotExist()

        composeRule.onNodeWithText("Continue").performClick()

        assertCurrentRoute(AuthEntryRoute::class)
        assertTrue(repository.isCompleted)
    }

    @Test
    fun authEntry_createAccount_showsSignUpForm_andBackReturns() {
        launch(StartDestination.AuthEntry)
        composeRule.onNodeWithText("Ready when you are.").assertIsDisplayed()

        clickText("Create an account")

        assertCurrentRoute(AuthRoute::class)
        composeRule.onNodeWithText("Say it, softly.").assertIsDisplayed()
        composeRule.onNodeWithText("Sign up with Google").performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Back").performClick()

        assertCurrentRoute(AuthEntryRoute::class)
    }

    @Test
    fun phoneSignUp_otp_thenLandsOnDiscoverVibe_withCleanBackStack() {
        launch(StartDestination.AuthEntry)
        clickText("Create an account")

        composeRule.onNodeWithText("Your name").performTextInput("Riya Sharma")
        composeRule.onNodeWithText("Phone number").performTextInput("9876543210")
        clickText("Send OTP")

        composeRule.onNodeWithText("Check your messages").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Verification code", substring = true).performTextInput("123456")

        assertCurrentRoute(VibeRoute::class)
        composeRule.onNodeWithText("Let's discover your vibe ✨").assertIsDisplayed()
        composeRule.onNodeWithText("Discover My Personality").performScrollTo().assertIsDisplayed()
        assertNull(navController.previousBackStackEntry)
    }

    @Test
    fun signUp_whenFirebaseIsNotConfigured_saysSo_andDoesNotProceed() {
        auth = FakeAuthRepository(isConfigured = false)
        launch(StartDestination.AuthEntry)
        clickText("Create an account")

        composeRule.onNodeWithText(AuthError.NotConfigured.message).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Send OTP").performScrollTo().assertIsNotEnabled()
        assertCurrentRoute(AuthRoute::class)
    }

    @Test
    fun signUp_withoutGoogleOAuthClient_disablesOnlyGoogle_withHonestNote() {
        google = FakeGoogleIdTokenProvider(isConfigured = false)
        launch(StartDestination.AuthEntry)
        clickText("Create an account")

        composeRule.onNodeWithText("Sign up with Google").performScrollTo().assertIsNotEnabled()
        composeRule.onNodeWithText(AuthError.GoogleNotConfigured.message).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(AuthError.NotConfigured.message).assertDoesNotExist()
        composeRule.onNodeWithText("Send OTP").performScrollTo().assertIsEnabled()
    }

    @Test
    fun vibe_validPaste_autoSavesLocks_andLandsOnHome_withCleanBackStack() {
        auth = FakeAuthRepository(signedIn = FakeAuthRepository.user(uid = "u1"))
        profiles.profiles["u1"] = UserProfile("u1", "Riya Sharma", null, "+919876543210")
        launch(StartDestination.DiscoverVibe)

        clickText("I already have my profile")
        composeRule.onNodeWithText("Or paste ChatGPT's reply here").performTextInput(TestReplies.WithDifferentName)

        assertCurrentRoute(HomeRoute::class)
        composeRule.onNodeWithText("Welcome, Riya 💗").assertIsDisplayed()
        assertNull("Vibe flow must not be reachable with Back", navController.previousBackStackEntry)
        assertEquals(TestReplies.WithDifferentName, profiles.personalities.getValue("u1").response)
        assertEquals("Riya Sharma", profiles.profiles.getValue("u1").name)
    }

    @Test
    fun vibe_saveFailure_staysOnPaste_withReplyKept() {
        auth = FakeAuthRepository(signedIn = FakeAuthRepository.user(uid = "u1"))
        profiles.profiles["u1"] = UserProfile("u1", "Riya Sharma", null, "+919876543210")
        profiles.failFinalizeWith = ProfileException("We couldn't reach the server. (Firestore: UNAVAILABLE)", "UNAVAILABLE")
        launch(StartDestination.DiscoverVibe)

        clickText("I already have my profile")
        composeRule.onNodeWithText("Or paste ChatGPT's reply here").performTextInput(TestReplies.Valid)

        assertCurrentRoute(VibeRoute::class)
        composeRule.onNodeWithText("We couldn't reach the server. (Firestore: UNAVAILABLE)").performScrollTo().assertIsDisplayed()
        assertTrue(profiles.personalities.isEmpty())

        profiles.failFinalizeWith = null
        clickText("Try saving again")
        assertCurrentRoute(HomeRoute::class)
    }

    @Test
    fun myProfile_showsSupportedSections_fullResponse_andNoWayToEdit() {
        auth = FakeAuthRepository(signedIn = FakeAuthRepository.user(uid = "u1"))
        profiles.profiles["u1"] = UserProfile("u1", "Riya Sharma", null, "+919876543210")
        profiles.withPersonality("u1")
        launch(StartDestination.Home)

        clickText("Open My Profile")

        assertCurrentRoute(MyProfileRoute::class)
        composeRule.onNodeWithText("Final · read-only".uppercase()).assertIsDisplayed()
        listOf("Communication style", "Interests", "Relationship preferences", "Strengths").forEach {
            composeRule.onNodeWithText(it).performScrollTo().assertIsDisplayed()
        }
        composeRule.onNodeWithText("Indie music and late-night playlists").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Values").assertDoesNotExist() // "Not enough context" in the reply
        clickText("Show full response")
        composeRule.onNodeWithText(TestReplies.Valid).performScrollTo().assertIsDisplayed()
        listOf("Update my vibe", "Edit", "Replace", "Regenerate", "Discover my vibe ✨", "Save my profile").forEach {
            composeRule.onNodeWithText(it, substring = true).assertDoesNotExist()
        }

        composeRule.onNodeWithContentDescription("Back").performScrollTo().performClick()
        assertCurrentRoute(HomeRoute::class)
        composeRule.onNodeWithText("Update my vibe").assertDoesNotExist()
    }

    @Test
    fun profile_isStillReadable_afterLogOut_andLogInAgain() {
        auth = FakeAuthRepository(signedIn = FakeAuthRepository.user(uid = "phone-uid", phone = "+919876543210"))
        profiles.profiles["phone-uid"] = UserProfile("phone-uid", "Riya Sharma", null, "+919876543210")
        profiles.withPersonality("phone-uid")
        launch(StartDestination.Home)

        clickText("Log out")
        assertCurrentRoute(AuthEntryRoute::class)
        clickText("Log in")
        composeRule.onNodeWithText("Phone number").performTextInput("9876543210")
        clickText("Send OTP")
        composeRule.onNodeWithContentDescription("Verification code", substring = true).performTextInput("123456")

        assertCurrentRoute(HomeRoute::class)
        clickText("Open My Profile")
        composeRule.onNodeWithText("Indie music and late-night playlists").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun home_logOut_returnsToAuthEntry() {
        auth = FakeAuthRepository(signedIn = FakeAuthRepository.user(uid = "u1"))
        profiles.profiles["u1"] = UserProfile("u1", "Riya Sharma", null, "+919876543210")
        profiles.withPersonality("u1")
        launch(StartDestination.Home)

        clickText("Log out")

        assertCurrentRoute(AuthEntryRoute::class)
        assertNull(auth.currentUser)
    }

    @Test
    fun authEntry_replayIntro_opensOnboarding() {
        launch(StartDestination.AuthEntry)

        clickText("Replay the introduction")

        assertCurrentRoute(OnboardingRoute::class)
    }
}
