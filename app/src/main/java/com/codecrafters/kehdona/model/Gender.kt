package com.codecrafters.kehdona.model

import androidx.annotation.Keep

/** A member's gender exactly as they stated it. [wire] is the value stored in Firestore. */
@Keep
enum class Gender(val wire: String, val label: String) {
    Man("man", "Man"),
    Woman("woman", "Woman"),
    NonBinary("non_binary", "Non-binary"),
    SelfDescribe("self_describe", "Self-describe"),
    PreferNotToSay("prefer_not_to_say", "Prefer not to say"),
    ;

    companion object {
        fun fromWire(value: String?): Gender? = entries.firstOrNull { it.wire == value }
    }
}

/** Who a member would like to be matched with. */
@Keep
enum class MatchPreference(val wire: String, val label: String) {
    Men("men", "Men"),
    Women("women", "Women"),
    All("all", "All genders"),
    ;

    companion object {
        fun fromWire(value: String?): MatchPreference? = entries.firstOrNull { it.wire == value }

        /**
         * The suggested starting choice once a gender is picked: opposite gender for men and
         * women, all genders otherwise. Only a suggestion; the person always confirms it.
         */
        fun defaultFor(gender: Gender): MatchPreference = when (gender) {
            Gender.Man -> Women
            Gender.Woman -> Men
            Gender.NonBinary, Gender.SelfDescribe, Gender.PreferNotToSay -> All
        }
    }
}
