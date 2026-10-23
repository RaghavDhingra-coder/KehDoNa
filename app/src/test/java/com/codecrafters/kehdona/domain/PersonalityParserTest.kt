package com.codecrafters.kehdona.domain

import com.codecrafters.kehdona.domain.PersonalitySectionKind.CommunicationStyle
import com.codecrafters.kehdona.domain.PersonalitySectionKind.ConfidenceNote
import com.codecrafters.kehdona.domain.PersonalitySectionKind.Interests
import com.codecrafters.kehdona.domain.PersonalitySectionKind.RelationshipPreferences
import com.codecrafters.kehdona.domain.PersonalitySectionKind.Strengths
import com.codecrafters.kehdona.domain.PersonalitySectionKind.Values
import com.codecrafters.kehdona.testutil.TestReplies
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PersonalityParserTest {
    @Test
    fun showsOnlySectionsTheReplySupports() {
        val kinds = PersonalityParser.parse(TestReplies.Valid).map { it.kind }

        assertEquals(listOf(CommunicationStyle, Interests, RelationshipPreferences, Strengths, ConfidenceNote), kinds)
        assertFalse("'Not enough context' sections are hidden", Values in kinds)
    }

    @Test
    fun splitsBullets_andReadsEvidenceTags() {
        val interests = PersonalityParser.parse(TestReplies.Valid).first { it.kind == Interests }.items

        assertEquals(2, interests.size)
        assertEquals(PersonalityItem("Indie music and late-night playlists", Evidence.Observed), interests[0])
        assertEquals(PersonalityItem("Street food adventures", Evidence.Inferred), interests[1])
    }

    @Test
    fun nameLine_isNeverATrait() {
        val sections = PersonalityParser.parse(TestReplies.WithDifferentName)

        assertTrue(sections.none { section -> section.items.any { "Priya" in it.text } })
        assertEquals(PersonalityParser.parse(TestReplies.Valid), sections)
    }

    @Test
    fun handlesHeadingStyles_andParentheticals() {
        val reply = """
            ## Communication style
            Direct but kind.
            3. Compatibility traits (who I'd likely click with): Curious people
            - Sense of humor: dry
        """.trimIndent()

        val sections = PersonalityParser.parse(reply)

        assertEquals(listOf("Communication style", "Compatibility", "Sense of humour"), sections.map { it.kind.title })
        assertEquals("Curious people", sections[1].items.single().text)
        assertNull(sections[0].items.single().evidence)
    }

    @Test
    fun proseMentioningAHeadingWord_isNotAHeading() {
        val sections = PersonalityParser.parse("Interests: music\nInterests are broad and varied.")

        assertEquals(listOf("music", "Interests are broad and varied."), sections.single().items.map { it.text })
    }

    @Test
    fun freeFormReply_hasNoSections() {
        assertTrue(PersonalityParser.parse("You seem warm and thoughtful overall.").isEmpty())
    }
}
