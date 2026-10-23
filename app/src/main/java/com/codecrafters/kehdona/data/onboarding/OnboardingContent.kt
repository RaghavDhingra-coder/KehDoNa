package com.codecrafters.kehdona.data.onboarding

import com.codecrafters.kehdona.model.OnboardingIllustration
import com.codecrafters.kehdona.model.OnboardingPage

/** Static copy for the onboarding flow. */
object OnboardingContent {
    val pages = listOf(
        OnboardingPage(
            eyebrow = "Discover your vibe",
            title = "Find people who see the world a little like you do.",
            body = "Share what you love — the songs on repeat, your favourite corner of campus — and meet people who get it.",
            illustration = OnboardingIllustration.Vibes,
        ),
        OnboardingPage(
            eyebrow = "Anonymous confessions",
            title = "Some things are easier to say when your name stays out of it.",
            body = "Leave a note for the person who made your week, without having to sign it.",
            illustration = OnboardingIllustration.Confessions,
        ),
        OnboardingPage(
            eyebrow = "Secret crush",
            title = "Some feelings are better discovered together.",
            body = "Add a secret crush. It's only revealed if the feeling is mutual.",
            illustration = OnboardingIllustration.SecretCrush,
        ),
    )
}
