package com.codecrafters.kehdona.model

/** Which illustration an onboarding page shows. */
enum class OnboardingIllustration { Vibes, Confessions, SecretCrush }

/** One page of the introductory onboarding flow. */
data class OnboardingPage(
    val eyebrow: String,
    val title: String,
    val body: String,
    val illustration: OnboardingIllustration,
)
