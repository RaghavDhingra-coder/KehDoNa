package com.codecrafters.kehdona.domain

/** How a point in the profile is supported, as labelled by ChatGPT. */
enum class Evidence { Observed, Inferred }

data class PersonalityItem(val text: String, val evidence: Evidence?)

enum class PersonalitySectionKind(val heading: String, val title: String) {
    CommunicationStyle("Communication style", "Communication style"),
    Interests("Interests", "Interests"),
    Values("Values", "Values"),
    EmotionalTendencies("Emotional tendencies", "Emotional tendencies"),
    Humour("Sense of humou?r", "Sense of humour"),
    SocialEnergy("Social energy", "Social energy"),
    RelationshipPreferences("Relationship preferences", "Relationship preferences"),
    Strengths("Strengths", "Strengths"),
    Compatibility("Compatibility traits", "Compatibility"),
    ConfidenceNote("Confidence note", "Confidence note"),
}

data class PersonalitySection(val kind: PersonalitySectionKind, val items: List<PersonalityItem>)

/**
 * Reads the sections out of ChatGPT's reply for display. Only sections the reply actually
 * fills in are returned ("Not enough context" sections are dropped), and the name line is
 * never treated as a trait. The original reply is always kept and shown in full as well.
 */
object PersonalityParser {
    private val options = setOf(RegexOption.IGNORE_CASE)

    // A heading line: optional markdown/bullets, the label, an optional "(…)", then ":" or end of line.
    private fun headingRegex(label: String) =
        Regex("""^[\s>#*_\-•\d.)]*$label(\s*\([^)\n]*\))?[\s*_]*(:|$)[\s*_]*(.*)$""", options)

    private val headings = PersonalitySectionKind.entries.map { it to headingRegex(it.heading) }
    private val nameHeading = headingRegex(Regex.escape(PersonalityPrompt.NameLabel))
    private val notEnoughContext = Regex("""^\W*(not enough context|insufficient context|n/?a)\W*$""", options)
    private val evidenceTag = Regex("""[\[(]\s*(observed|inferred)\s*[\])]\s*:?""", options)
    private val bullet = Regex("""^[\s\-*•]+|^\d+[.)]\s+""")

    fun recognizedHeadingCount(response: String): Int =
        response.lines().count { line -> headings.any { (_, regex) -> regex.matches(line.trim()) } }

    fun parse(response: String): List<PersonalitySection> {
        val sections = mutableListOf<Pair<PersonalitySectionKind?, MutableList<String>>>()
        for (rawLine in response.lines()) {
            val line = rawLine.trim()
            val match = headings.firstNotNullOfOrNull { (kind, regex) -> regex.matchEntire(line)?.let { kind to it } }
            when {
                match != null -> sections += match.first to mutableListOf(match.second.groupValues[3])
                nameHeading.matches(line) -> sections += null to mutableListOf()
                else -> sections.lastOrNull()?.second?.add(line)
            }
        }
        return sections.mapNotNull { (kind, lines) ->
            kind ?: return@mapNotNull null
            val items = lines.map(::cleanLine).filter { it.isNotEmpty() }
            if (items.isEmpty() || items.all { notEnoughContext.matches(it) }) return@mapNotNull null
            PersonalitySection(kind, items.filterNot { notEnoughContext.matches(it) }.map(::toItem))
        }
            // If ChatGPT repeated a heading, keep the first occurrence.
            .distinctBy { it.kind }
    }

    private fun cleanLine(line: String): String =
        line.replace(bullet, "").replace("**", "").replace("__", "").trim()

    private fun toItem(text: String): PersonalityItem {
        val tag = evidenceTag.find(text)
        val evidence = when (tag?.groupValues?.get(1)?.lowercase()) {
            "observed" -> Evidence.Observed
            "inferred" -> Evidence.Inferred
            else -> null
        }
        return PersonalityItem(text.replace(evidenceTag, "").trim().trimStart('-', ':', ' '), evidence)
    }
}
