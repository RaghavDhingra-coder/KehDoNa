package com.codecrafters.kehdona.domain

import com.codecrafters.kehdona.testutil.TestReplies
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PersonalityPromptTest {
    @Test
    fun prompt_coversEveryTrait_andGuardsAgainstInvention() {
        val text = PersonalityPrompt.text
        PersonalitySectionKind.entries.forEach {
            assertTrue("Prompt should ask for ${it.title}", Regex(it.heading, RegexOption.IGNORE_CASE).containsMatchIn(text))
        }
        assertTrue(text.contains("Do not invent facts"))
        assertTrue(text.contains("[Observed]") && text.contains("[Inferred]"))
    }

    @Test
    fun prompt_noLongerAsksForTheUsersName() {
        assertFalse(PersonalityPrompt.text.contains(PersonalityPrompt.NameLabel))
        assertTrue(PersonalityPrompt.text.contains("Do not include my name"))
    }

    @Test
    fun validReply_isAccepted() {
        assertNull(PersonalityPrompt.profileError(TestReplies.Valid))
        assertNull(PersonalityPrompt.profileError(TestReplies.WithDifferentName))
    }

    @Test
    fun theCopiedPromptItself_isNeverAcceptedAsAProfile() {
        val error = PersonalityPrompt.profileError(PersonalityPrompt.text)

        assertNotNull(error)
        assertTrue(error!!.contains("prompt"))
    }

    @Test
    fun unrelatedTextOrBadLengths_areRejected() {
        assertNotNull(PersonalityPrompt.profileError(""))
        assertNotNull(PersonalityPrompt.profileError("too short"))
        assertNotNull(PersonalityPrompt.profileError("Lorem ipsum ".repeat(20)))
        assertNotNull(PersonalityPrompt.profileError(TestReplies.Valid + "x".repeat(PersonalityPrompt.MaxProfileLength)))
    }
}
