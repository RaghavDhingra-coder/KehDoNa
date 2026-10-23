package com.codecrafters.kehdona.testutil

/** Sample ChatGPT replies to the KehDoNa prompt. */
object TestReplies {
    val Valid = """
        **Communication style:** [Observed] You write in quick, playful bursts and ask lots of questions.

        **Interests:**
        - [Observed] Indie music and late-night playlists
        - [Inferred] Street food adventures

        **Values:** Not enough context

        **Relationship preferences:** [Inferred] You value steady, thoughtful partners who make time.

        **Strengths:** [Observed] You remember the small things people tell you.

        Confidence note: Based on a few months of casual chats.
    """.trimIndent()

    /** A real-looking reply that still names someone else; the name must never be applied. */
    val WithDifferentName = "Name mentioned in our chats: Priya\n\n$Valid"
}
