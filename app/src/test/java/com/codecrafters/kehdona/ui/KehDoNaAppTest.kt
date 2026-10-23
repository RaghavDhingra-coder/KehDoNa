package com.codecrafters.kehdona.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.codecrafters.kehdona.data.profile.UserProfile
import com.codecrafters.kehdona.data.profile.profileException
import com.google.firebase.firestore.FirebaseFirestoreException.Code
import com.codecrafters.kehdona.testutil.FakeAuthRepository
import com.codecrafters.kehdona.testutil.FakeAuthRepository.Companion.user
import com.codecrafters.kehdona.testutil.FakeOnboardingRepository
import com.codecrafters.kehdona.testutil.FakeProfileRepository
import com.codecrafters.kehdona.testutil.TestAppContainer
import com.codecrafters.kehdona.ui.theme.KehDoNaTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w411dp-h891dp")
class KehDoNaAppTest {
    @get:Rule val composeRule = createComposeRule()

    private fun launch(
        repository: FakeOnboardingRepository,
        auth: FakeAuthRepository = FakeAuthRepository(),
        profiles: FakeProfileRepository = FakeProfileRepository(),
    ) {
        val container = TestAppContainer(repository, auth, profiles)
        composeRule.setContent { KehDoNaTheme { KehDoNaApp(container) } }
    }

    @Test
    fun firstLaunch_showsWelcome() {
        launch(FakeOnboardingRepository(completed = false))

        composeRule.onNodeWithText("Get started").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun returningUser_opensAuthEntry() {
        launch(FakeOnboardingRepository(completed = true))

        composeRule.onNodeWithText("Ready when you are.").assertIsDisplayed()
    }

    @Test
    fun settingsReadFailure_showsError_andRetryRecovers() {
        val repository = FakeOnboardingRepository().apply { failReads = true }
        launch(repository)

        composeRule.onNodeWithText("Something went wrong").assertIsDisplayed()

        repository.failReads = false
        composeRule.onNodeWithText("Try again").performScrollTo().performClick()

        composeRule.onNodeWithText("Get started").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun signedInMember_withoutPersonality_resumesAtDiscoverVibe() {
        launch(
            FakeOnboardingRepository(completed = true),
            auth = FakeAuthRepository(signedIn = user(uid = "u1")),
            profiles = FakeProfileRepository(UserProfile("u1", "Riya", null, "+919876543210")),
        )

        composeRule.onNodeWithText("Let's discover your vibe ✨").assertIsDisplayed()
    }

    @Test
    fun signedInMember_withPersonality_opensHome() {
        launch(
            FakeOnboardingRepository(completed = true),
            auth = FakeAuthRepository(signedIn = user(uid = "u1")),
            profiles = FakeProfileRepository(
                UserProfile("u1", "Riya Sharma", null, "+919876543210"),
            ).withPersonality("u1"),
        )

        composeRule.onNodeWithText("Welcome, Riya 💗").assertIsDisplayed()
    }

    @Test
    fun appRestart_reopensAtHome_andProfileIsReadable() {
        // A fresh app start against the same stored data (Firestore is the source of truth, so
        // the same holds after a reinstall).
        val profiles = FakeProfileRepository(UserProfile("u1", "Riya Sharma", null, "+919876543210")).withPersonality("u1")
        launch(FakeOnboardingRepository(completed = true), FakeAuthRepository(signedIn = user(uid = "u1")), profiles)

        composeRule.onNodeWithText("Open My Profile").performScrollTo().performClick()

        composeRule.onNodeWithText("Relationship preferences").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun personalityReadDenied_showsRealCode_andLogOutEscapes() {
        val profiles = FakeProfileRepository(UserProfile("u1", "Riya", null, "+919876543210")).apply {
            failPersonalityReadWith = profileException(Code.PERMISSION_DENIED)
        }
        val auth = FakeAuthRepository(signedIn = user(uid = "u1"))
        launch(FakeOnboardingRepository(completed = true), auth, profiles)

        composeRule.onNodeWithText("(Firestore: PERMISSION_DENIED)", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Check your connection", substring = true).assertDoesNotExist()

        composeRule.onNodeWithText("Log out").performScrollTo().performClick()

        composeRule.onNodeWithText("Ready when you are.").assertIsDisplayed()
    }
}
