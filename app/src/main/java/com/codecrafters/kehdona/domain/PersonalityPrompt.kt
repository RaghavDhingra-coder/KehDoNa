package com.codecrafters.kehdona.domain

import java.text.Normalizer

/** The fixed prompt copied for ChatGPT, plus checks on the profile pasted back. */
object PersonalityPrompt {
    const val MinProfileLength = 80
    const val MaxProfileLength = 12_000

    /** Label the prompt asks ChatGPT to use, so the app can spot a name it inferred. */
    const val NameLabel = "Name mentioned in our chats"

    val text: String = """
        I'm setting up my profile on KehDoNa, a campus app for meeting compatible people. Please write a personality profile of me based ONLY on what you can actually see in our conversations and any memory you have of me.

        Ground rules:
        - Do not invent facts about me. If there isn't enough context for a section, write "Not enough context" instead of guessing.
        - Clearly separate what you OBSERVED (directly supported by our chats) from what you INFER (a reasonable guess). Label every point as [Observed] or [Inferred].
        - Do not include sensitive details such as health, religion, caste, sexuality, political views, finances, exact location, phone numbers, emails, or anyone else's private information.
        - Do not state my age, gender, college, or appearance unless I explicitly told you, and even then keep it out of the profile.
        - Be warm, honest and balanced, not flattering. Write in second person ("You…").
        - Keep it under 450 words.

        Use exactly this format:

        $NameLabel: <the name I've used for myself in our chats, or "Not mentioned">

        Communication style:
        Interests:
        Values:
        Emotional tendencies:
        Sense of humour:
        Social energy:
        Relationship preferences:
        Strengths:
        Compatibility traits (who I'd likely click with):
        Confidence note: <one line on how much context this is based on>

        Reply with only the profile, no introduction.
    """.trimIndent()

    fun profileError(raw: String): String? {
        val text = raw.trim()
        return when {
            text.isEmpty() -> "Paste the profile ChatGPT wrote for you."
            text.length < MinProfileLength -> "That looks too short. Paste ChatGPT's full reply."
            text.length > MaxProfileLength -> "That's longer than expected. Paste just the personality profile."
            else -> null
        }
    }

    /**
     * Returns the name ChatGPT reported, or null if it didn't report one. This is an
     * unverified claim and is only ever shown back to the person to confirm.
     */
    fun extractSuggestedName(profile: String): String? {
        val pattern = Regex(
            "^[\\s*_#>-]*" + Regex.escape(NameLabel) + "[\\s*_]*:[\\s*_]*(.+)$",
            setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE),
        )
        val value = pattern.find(profile)?.groupValues?.get(1)
            ?.trim()
            ?.trim('*', '_', '"', '“', '”', '\'', '<', '>', '.', ' ')
            ?: return null
        val placeholder = Regex("^(not mentioned|none|unknown|n/?a|not (enough|available).*)$", RegexOption.IGNORE_CASE)
        if (value.isEmpty() || placeholder.matches(value)) return null
        return Validation.normalizeName(value).takeIf { Validation.nameError(it) == null }
    }

    /**
     * True when [suggested] isn't simply (part of) the verified [profileName], e.g.
     * "Riya" for "Riya Sharma" is fine, "Priya" is a conflict worth asking about.
     */
    fun isNameConflict(profileName: String, suggested: String?): Boolean {
        if (suggested == null) return false
        val known = nameTokens(profileName)
        val claimed = nameTokens(suggested)
        return claimed.isNotEmpty() && !known.containsAll(claimed)
    }

    private fun nameTokens(name: String): Set<String> =
        Normalizer.normalize(name, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}"), "")
            .lowercase()
            .split(Regex("[\\s.'-]+"))
            .filter { it.isNotEmpty() }
            .toSet()
}
