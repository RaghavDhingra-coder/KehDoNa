package com.codecrafters.kehdona.ui

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
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
import com.codecrafters.kehdona.data.profile.UserProfile
import com.codecrafters.kehdona.navigation.AuthEntryRoute
import com.codecrafters.kehdona.navigation.AuthRoute
import com.codecrafters.kehdona.navigation.KehDoNaNavHost
import com.codecrafters.kehdona.navigation.OnboardingRoute
import com.codecrafters.kehdona.navigation.VibeRoute
import com.codecrafters.kehdona.navigation.WelcomeRoute
import com.codecrafters.kehdona.testutil.FakeAuthRepository
import com.codecrafters.kehdona.testutil.FakeOnboardingRepository
import com.codecrafters.kehdona.testutil.FakeProfileRepository
import com.codecrafters.kehdona.testutil.TestAppContainer
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

    private fun launch(start: StartDestination) {
        val container = TestAppContainer(repository, auth, profiles)
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
    fun vibe_nameConflict_showsPrivateConfirmation() {
        auth = FakeAuthRepository(signedIn = FakeAuthRepository.user(uid = "u1"))
        profiles.profiles["u1"] = UserProfile("u1", "Riya Sharma", null, "+919876543210")
        launch(StartDestination.DiscoverVibe)

        clickText("I already have my profile")
        composeRule.onNodeWithText("ChatGPT's personality profile").performTextInput(
            "Name mentioned in our chats: Priya\n\nCommunication style: [Observed] Quick, playful and warm, " +
                "with a soft spot for long voice notes.",
        )
        clickText("Save my profile")

        composeRule.onNodeWithText("Quick check: is this you?").assertIsDisplayed()
        clickText("Keep “Riya Sharma”")

        composeRule.onNodeWithText("Your vibe is saved ✨").assertIsDisplayed()
        assertEquals("Riya Sharma", profiles.profiles.getValue("u1").name)
    }

    @Test
    fun home_logOut_returnsToAuthEntry() {
        auth = FakeAuthRepository(signedIn = FakeAuthRepository.user(uid = "u1"))
        profiles.profiles["u1"] = UserProfile("u1", "Riya Sharma", null, "+919876543210", personalityProfile = "Warm.")
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
