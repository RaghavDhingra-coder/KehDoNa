package com.codecrafters.kehdona.auth

import com.codecrafters.kehdona.data.profile.UserProfile
import com.codecrafters.kehdona.domain.SessionStage
import com.codecrafters.kehdona.testutil.FakeAuthRepository
import com.codecrafters.kehdona.testutil.FakeAuthRepository.Companion.user
import com.codecrafters.kehdona.testutil.FakeProfileRepository
import com.codecrafters.kehdona.testutil.MainDispatcherRule
import com.codecrafters.kehdona.ui.screens.vibe.ChatGptTarget
import com.codecrafters.kehdona.ui.screens.vibe.VibeStep
import com.codecrafters.kehdona.ui.screens.vibe.VibeViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class VibeViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private val auth = FakeAuthRepository(signedIn = user(uid = "u1"))
    private val profiles = FakeProfileRepository(UserProfile("u1", "Riya Sharma", null, "+919876543210"))
    private val vm by lazy { VibeViewModel(auth, profiles) }
    private val state get() = vm.uiState.value

    private fun aiProfile(name: String) = """
        Name mentioned in our chats: $name

        Communication style: [Observed] You write in quick, playful bursts and use lots of emoji.
        Interests: [Observed] Indie music, late-night food runs.
    """.trimIndent()

    private fun pasteAndSave(text: String) {
        vm.onPromptHandedOff(ChatGptTarget.App)
        vm.onProfileTextChange(text)
        vm.saveProfile()
    }

    @Test
    fun loadsVerifiedName_andStartsAtIntro() {
        assertEquals(VibeStep.Intro, state.step)
        assertEquals("Riya Sharma", state.profileName)
    }

    @Test
    fun handoff_movesToPasteStep_rememberingWhereChatGptOpened() {
        vm.onPromptHandedOff(ChatGptTarget.Browser)

        assertEquals(VibeStep.Paste, state.step)
        assertEquals(ChatGptTarget.Browser, state.openedIn)
    }

    @Test
    fun tooShortProfile_isRejected_andNothingSaved() {
        pasteAndSave("Nice person.")

        assertNotNull(state.profileError)
        assertEquals(null, profiles.profiles.getValue("u1").personalityProfile)
    }

    @Test
    fun matchingOrMissingName_savesAndFinishes_withoutAnyNameChange() {
        pasteAndSave(aiProfile("Riya"))

        assertEquals(VibeStep.Done, state.step)
        assertTrue(profiles.profiles.getValue("u1").personalityProfile!!.contains("playful"))
        assertTrue(profiles.nameUpdates.isEmpty())
    }

    @Test
    fun differentName_asksPrivately_andNeverChangesNameSilently() {
        pasteAndSave(aiProfile("Priya"))

        assertEquals(VibeStep.ConfirmName, state.step)
        assertEquals("Priya", state.suggestedName)
        assertEquals("Riya Sharma", profiles.profiles.getValue("u1").name)
        assertTrue(profiles.nameUpdates.isEmpty())
        // The personality profile itself is still saved.
        assertNotNull(profiles.profiles.getValue("u1").personalityProfile)
    }

    @Test
    fun keepVerifiedName_leavesProfileNameUntouched() {
        pasteAndSave(aiProfile("Priya"))

        vm.keepVerifiedName()

        assertEquals(VibeStep.Done, state.step)
        assertEquals("Riya Sharma", profiles.profiles.getValue("u1").name)
        assertTrue(profiles.nameUpdates.isEmpty())
    }

    @Test
    fun explicitConfirmation_updatesName() {
        pasteAndSave(aiProfile("Priya"))

        vm.confirmSuggestedName()

        assertEquals(listOf("Priya"), profiles.nameUpdates)
        assertEquals("Priya", state.profileName)
        assertEquals(VibeStep.Done, state.step)
    }

    @Test
    fun customName_isValidated_thenSaved() {
        pasteAndSave(aiProfile("Priya"))

        vm.onCustomNameChange("R2D2")
        vm.confirmCustomName()
        assertNotNull(state.customNameError)
        assertTrue(profiles.nameUpdates.isEmpty())

        vm.onCustomNameChange("Riya S. Sharma")
        vm.confirmCustomName()
        assertEquals(listOf("Riya S. Sharma"), profiles.nameUpdates)
    }

    @Test
    fun saveFailure_staysOnPaste_withError() {
        profiles.failAll = true
        pasteAndSave(aiProfile("Riya"))

        assertEquals(VibeStep.Paste, state.step)
        assertNotNull(state.errorMessage)
    }

    @Test
    fun maybeLater_recordsSkip_andGoesHome() {
        vm.skipForNow()

        assertTrue(profiles.profiles.getValue("u1").personalitySkipped)
        assertEquals(SessionStage.Home, state.next)
    }

    @Test
    fun signedOutUser_isSentBackToSignIn() {
        val vm = VibeViewModel(FakeAuthRepository(), profiles)

        assertEquals(SessionStage.SignedOut, vm.uiState.value.next)
    }
}
