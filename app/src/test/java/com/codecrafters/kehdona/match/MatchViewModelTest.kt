package com.codecrafters.kehdona.match

import com.codecrafters.kehdona.data.match.Match
import com.codecrafters.kehdona.data.match.MatchException
import com.codecrafters.kehdona.data.match.MatchFailure
import com.codecrafters.kehdona.data.match.MatchStatus
import com.codecrafters.kehdona.data.profile.UserProfile
import com.codecrafters.kehdona.domain.SessionStage
import com.codecrafters.kehdona.model.Gender
import com.codecrafters.kehdona.model.MatchPreference
import com.codecrafters.kehdona.testutil.FakeAuthRepository
import com.codecrafters.kehdona.testutil.FakeAuthRepository.Companion.user
import com.codecrafters.kehdona.testutil.FakeMatchRepository
import com.codecrafters.kehdona.testutil.FakeProfileRepository
import com.codecrafters.kehdona.testutil.MainDispatcherRule
import com.codecrafters.kehdona.ui.screens.match.MatchStep
import com.codecrafters.kehdona.ui.screens.match.MatchViewModel
import com.codecrafters.kehdona.ui.screens.match.compatibilityLabel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class MatchViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private val auth = FakeAuthRepository(signedIn = user(uid = "u1"))
    private val complete = UserProfile("u1", "Riya", null, "+919876543210", gender = Gender.Woman, matchPreference = MatchPreference.Men)
    private val profiles = FakeProfileRepository(complete).withPersonality("u1")
    private val matches = FakeMatchRepository()
    private val vm by lazy { MatchViewModel(auth, profiles, matches) }
    private val state get() = vm.uiState.value
    private val arjun = Match("arjun_u1", "Arjun", 0.31, listOf("music", "travel"))

    @Test
    fun existingUserWithoutGender_mustCompleteProfile_nothingIsGuessed() {
        profiles.profiles["u1"] = complete.copy(gender = null, matchPreference = null)

        assertEquals(MatchStep.NeedsDetails, state.step)
        assertNull(state.details.gender)
        assertNull(state.details.preference)

        vm.saveDetails()
        assertNotNull(state.details.genderError)
        assertTrue(profiles.matchingUpdates.isEmpty())
        assertEquals(0, matches.requests)
    }

    @Test
    fun completingDetails_savesThem_andContinuesToMatch() {
        profiles.profiles["u1"] = complete.copy(gender = null, matchPreference = null)

        vm.onGenderChange(Gender.Man)
        assertEquals(MatchPreference.Women, state.details.preference) // opposite-gender default
        vm.onPreferenceChange(MatchPreference.All) // ...which the person can change
        vm.saveDetails()

        assertEquals(Gender.Man, profiles.profiles.getValue("u1").gender)
        assertEquals(MatchPreference.All, profiles.profiles.getValue("u1").matchPreference)
        assertEquals(MatchStep.Ready, state.step)
    }

    @Test
    fun withoutPersonalityProfile_asksForItFirst() {
        profiles.personalities.clear()

        assertEquals(MatchStep.NeedsPersonality, state.step)
    }

    @Test
    fun matchButton_immediateMatch_showsTheSameMatchBothSeeStored() {
        matches.matches[arjun.id] = arjun
        matches.nextRequestResult = MatchStatus.Matched(arjun.id)
        assertEquals(MatchStep.Ready, state.step)

        vm.findMatch()

        assertEquals(MatchStep.Matched, state.step)
        assertEquals(arjun, state.match)
        assertEquals(1, matches.requests)
    }

    @Test
    fun noOneAvailable_waits_thenTheOtherPersonsRequestMatchesUs_live() {
        vm.findMatch()
        assertEquals(MatchStep.Waiting, state.step)

        matches.serverMatches(arjun)

        assertEquals(MatchStep.Matched, state.step)
        assertEquals("Arjun", state.match?.partnerFirstName)
    }

    @Test
    fun cancellingTheSearch_returnsToReady_withConfirmation() {
        vm.findMatch()

        vm.cancelSearch()

        assertEquals(MatchStep.Ready, state.step)
        assertNotNull(state.infoMessage)
        assertEquals(MatchStatus.Idle, matches.status.value)
    }

    @Test
    fun cancelArrivingAfterAMatch_keepsAndShowsTheMatch() {
        vm.findMatch()
        matches.matches[arjun.id] = arjun
        matches.status.value = MatchStatus.Matched(arjun.id)

        vm.cancelSearch()

        assertEquals(MatchStep.Matched, state.step)
    }

    @Test
    fun reopeningWhileQueued_showsWaiting_andLooksAgain() {
        matches.status.value = MatchStatus.Waiting

        assertEquals(MatchStep.Waiting, state.step)
        assertEquals(1, matches.requests)
    }

    @Test
    fun alreadyMatched_opensStraightOnTheMatch() {
        matches.serverMatches(arjun)

        assertEquals(MatchStep.Matched, state.step)
        assertEquals(0, matches.requests)
    }

    @Test
    fun backendNotDeployed_showsAnErrorWithTheRealCode() {
        matches.failRequestWith = MatchException(MatchFailure.BackendUnavailable, "Matching isn't switched on for KehDoNa yet.", "NOT_FOUND")

        vm.findMatch()

        assertEquals(MatchStep.Error, state.step)
        assertTrue(state.errorMessage!!.endsWith("(NOT_FOUND)"))
    }

    @Test
    fun serverSaysDetailsMissing_reloadsIntoCompleteProfile() {
        matches.failRequestWith = MatchException(MatchFailure.DetailsMissing, "Add your gender.", "MATCHING_DETAILS_MISSING")
        assertEquals(MatchStep.Ready, state.step)
        profiles.profiles["u1"] = complete.copy(matchPreference = null)

        vm.findMatch()

        assertEquals(MatchStep.NeedsDetails, state.step)
        assertEquals(Gender.Woman, state.details.gender) // prefilled from what they saved, not guessed
    }

    @Test
    fun expiredSession_goesToSignIn() {
        matches.failRequestWith = MatchException(MatchFailure.NotSignedIn, "Your session ended.", "UNAUTHENTICATED")

        vm.findMatch()

        assertEquals(SessionStage.SignedOut, state.next)
    }

    @Test
    fun compatibilityLabels_areBanded() {
        assertEquals("Strong compatibility", compatibilityLabel(0.3))
        assertEquals("Good compatibility", compatibilityLabel(0.15))
        assertTrue(compatibilityLabel(0.01).startsWith("Some common ground"))
    }
}
