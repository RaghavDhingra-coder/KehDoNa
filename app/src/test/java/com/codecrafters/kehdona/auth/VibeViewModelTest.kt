package com.codecrafters.kehdona.auth

import androidx.lifecycle.SavedStateHandle
import com.codecrafters.kehdona.data.profile.ProfileException
import com.codecrafters.kehdona.data.profile.UserProfile
import com.codecrafters.kehdona.domain.PersonalityPrompt
import com.codecrafters.kehdona.domain.SessionStage
import com.codecrafters.kehdona.testutil.FakeAuthRepository
import com.codecrafters.kehdona.testutil.FakeAuthRepository.Companion.user
import com.codecrafters.kehdona.testutil.FakeProfileRepository
import com.codecrafters.kehdona.testutil.MainDispatcherRule
import com.codecrafters.kehdona.testutil.TestReplies
import com.codecrafters.kehdona.ui.screens.vibe.ChatGptTarget
import com.codecrafters.kehdona.ui.screens.vibe.VibeStep
import com.codecrafters.kehdona.ui.screens.vibe.VibeViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class VibeViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private val auth = FakeAuthRepository(signedIn = user(uid = "u1"))
    private val profiles = FakeProfileRepository(UserProfile("u1", "Riya Sharma", null, "+919876543210"))
    private val savedState = SavedStateHandle()
    private val vm by lazy { VibeViewModel(auth, profiles, savedState) }
    private val state get() = vm.uiState.value

    @Test
    fun startsAtIntro_whenNothingImportedYet() {
        assertEquals(VibeStep.Intro, state.step)
    }

    @Test
    fun handoff_movesToPasteStep_rememberingWhereChatGptOpened() {
        vm.onPromptHandedOff(ChatGptTarget.Browser)

        assertEquals(VibeStep.Paste, state.step)
        assertEquals(ChatGptTarget.Browser, state.openedIn)
    }

    @Test
    fun pastingAValidReply_savesLocksAndGoesHome_withNoConfirmation() {
        vm.onPromptHandedOff(ChatGptTarget.App)

        vm.onProfileTextChange(TestReplies.Valid) // one paste, no button tap

        assertEquals(TestReplies.Valid, profiles.personalities.getValue("u1").response)
        assertEquals(SessionStage.Home, state.next)
        assertNull(state.errorMessage)
    }

    @Test
    fun pasteFromClipboard_savesImmediately() {
        vm.onPasteFromClipboard(TestReplies.Valid)

        assertEquals(SessionStage.Home, state.next)
        assertEquals(1, profiles.finalizeAttempts)
    }

    @Test
    fun typingCharacterByCharacter_neverAutoSaves() {
        TestReplies.Valid.forEachIndexed { i, _ -> vm.onProfileTextChange(TestReplies.Valid.take(i + 1)) }

        assertEquals(0, profiles.finalizeAttempts)
        assertNull(state.next)

        vm.saveProfile()
        assertEquals(SessionStage.Home, state.next)
    }

    @Test
    fun invalidPastes_areRejected_andNothingIsLocked() {
        listOf("Nice person, very kind and thoughtful, loves music and long walks in the evening by the lake.", PersonalityPrompt.text)
            .forEach { paste ->
                vm.onPasteFromClipboard(paste)
                assertNotNull(paste.take(20), state.profileError)
            }
        vm.onPasteFromClipboard("")
        assertNotNull(state.profileError)

        assertTrue(profiles.personalities.isEmpty())
        assertNull(state.next)
    }

    @Test
    fun saveFailure_doesNotLock_keepsTheReply_andRetrySucceeds() {
        profiles.failFinalizeWith = ProfileException("We couldn't reach the server. (Firestore: UNAVAILABLE)", "UNAVAILABLE")

        vm.onPasteFromClipboard(TestReplies.Valid)

        assertTrue(profiles.personalities.isEmpty())
        assertNull(state.next)
        assertEquals(TestReplies.Valid, state.profileText)
        assertTrue(state.errorMessage!!.contains("UNAVAILABLE"))
        assertEquals(TestReplies.Valid, savedState.get<String>("personalityDraft"))

        profiles.failFinalizeWith = null
        vm.saveProfile()

        assertEquals(TestReplies.Valid, profiles.personalities.getValue("u1").response)
        assertEquals(SessionStage.Home, state.next)
        assertNull(savedState.get<String>("personalityDraft"))
    }

    @Test
    fun draftSurvivesProcessDeath_viaSavedState() {
        profiles.failFinalizeWith = ProfileException("offline")
        vm.onPasteFromClipboard(TestReplies.Valid)

        val restored = VibeViewModel(auth, profiles, savedState)

        assertEquals(TestReplies.Valid, restored.uiState.value.profileText)
        assertEquals(VibeStep.Paste, restored.uiState.value.step)
    }

    @Test
    fun alreadyFinalized_cannotReImport_andIsSentHome() {
        profiles.withPersonality("u1")

        assertEquals(SessionStage.Home, state.next)
    }

    @Test
    fun aiSuggestedName_isNeverApplied() {
        vm.onPasteFromClipboard(TestReplies.WithDifferentName)

        assertEquals(SessionStage.Home, state.next)
        assertEquals("Riya Sharma", profiles.profiles.getValue("u1").name)
    }

    @Test
    fun maybeLater_recordsSkip_andGoesHome_withoutLocking() {
        vm.skipForNow()

        assertTrue(profiles.profiles.getValue("u1").personalitySkipped)
        assertTrue(profiles.personalities.isEmpty())
        assertEquals(SessionStage.Home, state.next)
    }

    @Test
    fun signedOutUser_isSentBackToSignIn() {
        val vm = VibeViewModel(FakeAuthRepository(), profiles, SavedStateHandle())

        assertEquals(SessionStage.SignedOut, vm.uiState.value.next)
    }
}
