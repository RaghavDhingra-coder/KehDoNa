package com.codecrafters.kehdona.domain

/** The fixed prompt copied for ChatGPT, plus the check that a pasted reply is really its answer. */
object PersonalityPrompt {
    const val MinProfileLength = 80
    const val MaxProfileLength = 12_000

    /** Older replies may start with this line; it's an AI identity claim, so it is never shown as a trait. */
    const val NameLabel = "Name mentioned in our chats"

    val text: String = """
        I'm setting up my profile on KehDoNa, a campus app for meeting compatible people. Please write a personality profile of me based ONLY on what you can actually see in our conversations and any memory you have of me.

        Ground rules:
        - Do not invent facts about me. If there isn't enough context for a section, write "Not enough context" instead of guessing.
        - Clearly separate what you OBSERVED (directly supported by our chats) from what you INFER (a reasonable guess). Label every point as [Observed] or [Inferred].
        - Do not include sensitive details such as health, religion, caste, sexuality, political views, finances, exact location, phone numbers, emails, or anyone else's private information.
        - Do not include my name, age, gender, college, or appearance.
        - Be warm, honest and balanced, not flattering. Write in second person ("You…").
        - Keep it under 450 words.

        Use exactly this format:

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

    /** Sentences that only appear in the prompt itself, never in ChatGPT's reply. */
    private val PromptMarkers = listOf("Use exactly this format", "Ground rules:", "Reply with only the profile")

    /** Minimum number of the prompt's section headings a pasted reply must contain. */
    const val MinRecognizedSections = 3

    /**
     * Null when [raw] looks like ChatGPT's reply to this prompt. The profile is permanent
     * once saved, so a random paste must not be accepted.
     */
    fun profileError(raw: String): String? {
        val text = raw.trim()
        return when {
            text.isEmpty() -> "Paste the profile ChatGPT wrote for you."
            text.length < MinProfileLength -> "That looks too short. Paste ChatGPT's full reply."
            text.length > MaxProfileLength -> "That's longer than expected. Paste just the personality profile."
            // The clipboard still holds our own prompt (it has the headings too): never lock that in.
            PromptMarkers.any { text.contains(it, ignoreCase = true) } ->
                "That's the KehDoNa prompt, not ChatGPT's answer. Send it in ChatGPT, then copy the reply."
            PersonalityParser.recognizedHeadingCount(text) < MinRecognizedSections ->
                "This doesn't look like ChatGPT's reply to the KehDoNa prompt. Paste its full reply."
            else -> null
        }
    }
}
