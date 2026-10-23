package com.codecrafters.kehdona.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PersonalityPromptTest {
    private val body = "\n\nCommunication style: [Observed] You write in short, warm bursts and ask lots of questions.\n"

    @Test
    fun prompt_coversEveryTrait_andGuardsAgainstInvention() {
        val text = PersonalityPrompt.text
        listOf(
            "Communication style", "Interests", "Values", "Emotional tendencies", "Sense of humour",
            "Social energy", "Relationship preferences", "Strengths", "Compatibility traits",
        ).forEach { assertTrue("Prompt should ask for $it", text.contains(it)) }
        assertTrue(text.contains("Do not invent facts"))
        assertTrue(text.contains("[Observed]") && text.contains("[Inferred]"))
        assertTrue(text.contains(PersonalityPrompt.NameLabel))
    }

    @Test
    fun extractsName_fromPlainAndMarkdownFormats() {
        assertEquals("Priya", PersonalityPrompt.extractSuggestedName("Name mentioned in our chats: Priya$body"))
        assertEquals("Priya Kapoor", PersonalityPrompt.extractSuggestedName("**Name mentioned in our chats:** Priya Kapoor$body"))
        assertEquals("Priya", PersonalityPrompt.extractSuggestedName("- name mentioned in our chats: \"Priya\".$body"))
    }

    @Test
    fun noName_whenNotMentionedMissingOrInvalid() {
        assertNull(PersonalityPrompt.extractSuggestedName("Name mentioned in our chats: Not mentioned$body"))
        assertNull(PersonalityPrompt.extractSuggestedName("Name mentioned in our chats: Unknown$body"))
        assertNull(PersonalityPrompt.extractSuggestedName(body))
        assertNull(PersonalityPrompt.extractSuggestedName("Name mentioned in our chats: user_123$body"))
    }

    @Test
    fun conflict_onlyWhenSuggestedNameIsNotPartOfProfileName() {
        assertFalse(PersonalityPrompt.isNameConflict("Riya Sharma", null))
        assertFalse(PersonalityPrompt.isNameConflict("Riya Sharma", "Riya"))
        assertFalse(PersonalityPrompt.isNameConflict("Riya Sharma", "riya sharma"))
        assertFalse(PersonalityPrompt.isNameConflict("Zoë", "Zoe"))
        assertTrue(PersonalityPrompt.isNameConflict("Riya Sharma", "Priya"))
        assertTrue(PersonalityPrompt.isNameConflict("Riya", "Riya Kapoor"))
    }

    @Test
    fun profileText_lengthChecks() {
        assertNotNull(PersonalityPrompt.profileError(""))
        assertNotNull(PersonalityPrompt.profileError("too short"))
        assertNotNull(PersonalityPrompt.profileError("x".repeat(PersonalityPrompt.MaxProfileLength + 1)))
        assertNull(PersonalityPrompt.profileError("x".repeat(PersonalityPrompt.MinProfileLength)))
    }
}
