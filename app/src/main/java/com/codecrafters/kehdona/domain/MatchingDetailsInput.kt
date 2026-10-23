package com.codecrafters.kehdona.domain

import com.codecrafters.kehdona.model.Gender
import com.codecrafters.kehdona.model.MatchPreference

/**
 * Form state for gender + match preference. Gender starts empty and is never guessed; picking
 * one suggests the opposite-gender preference (all genders for other answers) until the person
 * picks a preference themselves.
 */
data class MatchingDetailsInput(
    val gender: Gender? = null,
    val selfDescription: String = "",
    val preference: MatchPreference? = null,
    val preferenceChosen: Boolean = false,
    val genderError: String? = null,
    val selfDescriptionError: String? = null,
    val preferenceError: String? = null,
) {
    val isComplete: Boolean get() = gender != null && preference != null

    /** The free-text description, only when "Self-describe" is chosen and something was typed. */
    val cleanSelfDescription: String?
        get() = selfDescription.trim().takeIf { gender == Gender.SelfDescribe && it.isNotEmpty() }

    fun withGender(value: Gender) = copy(
        gender = value,
        preference = if (preferenceChosen) preference else MatchPreference.defaultFor(value),
        genderError = null,
        preferenceError = null,
    )

    fun withPreference(value: MatchPreference) = copy(preference = value, preferenceChosen = true, preferenceError = null)

    fun withSelfDescription(value: String) =
        copy(selfDescription = value.take(Validation.SelfDescriptionMaxLength + 10), selfDescriptionError = null)

    /** Same input with errors filled in for anything missing or invalid. */
    fun validated() = copy(
        genderError = if (gender == null) "Choose an option. \"Prefer not to say\" is fine." else null,
        selfDescriptionError = if (gender == Gender.SelfDescribe) Validation.selfDescriptionError(selfDescription) else null,
        preferenceError = if (preference == null) "Choose who you'd like to match with." else null,
    )

    val hasErrors: Boolean get() = genderError != null || selfDescriptionError != null || preferenceError != null

    companion object {
        /** Prefills from what a member already saved (the preference counts as chosen). */
        fun from(gender: Gender?, selfDescription: String?, preference: MatchPreference?) = MatchingDetailsInput(
            gender = gender,
            selfDescription = selfDescription.orEmpty(),
            preference = preference,
            preferenceChosen = preference != null,
        )
    }
}
