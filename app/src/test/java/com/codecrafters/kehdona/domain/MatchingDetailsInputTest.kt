package com.codecrafters.kehdona.domain

import com.codecrafters.kehdona.model.Gender
import com.codecrafters.kehdona.model.MatchPreference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MatchingDetailsInputTest {
    @Test
    fun nothingIsPreselected() {
        val input = MatchingDetailsInput()
        assertNull(input.gender)
        assertNull(input.preference)
        assertTrue(input.validated().hasErrors)
    }

    @Test
    fun defaultsToOppositeGender_andAllGendersOtherwise() {
        assertEquals(MatchPreference.Women, MatchingDetailsInput().withGender(Gender.Man).preference)
        assertEquals(MatchPreference.Men, MatchingDetailsInput().withGender(Gender.Woman).preference)
        for (g in listOf(Gender.NonBinary, Gender.SelfDescribe, Gender.PreferNotToSay)) {
            assertEquals(g.name, MatchPreference.All, MatchingDetailsInput().withGender(g).preference)
        }
    }

    @Test
    fun aChosenPreferenceIsNeverOverwrittenByChangingGender() {
        val input = MatchingDetailsInput().withPreference(MatchPreference.All).withGender(Gender.Man)
        assertEquals(MatchPreference.All, input.preference)
    }

    @Test
    fun selfDescription_isOnlyKeptForSelfDescribe() {
        val described = MatchingDetailsInput().withGender(Gender.SelfDescribe).withSelfDescription("  agender ")
        assertEquals("agender", described.cleanSelfDescription)
        assertNull(described.withGender(Gender.Woman).cleanSelfDescription)
        assertTrue(described.withSelfDescription("x".repeat(41)).validated().selfDescriptionError != null)
        assertFalse(described.validated().hasErrors)
    }

    @Test
    fun savedValuesPrefill_withPreferenceTreatedAsChosen() {
        val input = MatchingDetailsInput.from(Gender.Woman, null, MatchPreference.All).withGender(Gender.Man)
        assertEquals(MatchPreference.All, input.preference)
    }
}
