package com.codecrafters.kehdona.testutil

import com.codecrafters.kehdona.data.profile.PersonalityProfile
import com.codecrafters.kehdona.data.profile.ProfileException
import com.codecrafters.kehdona.data.profile.ProfileRepository
import com.codecrafters.kehdona.data.profile.UserProfile

/**
 * In-memory [ProfileRepository] standing in for Firestore. It outlives ViewModels and sign-outs,
 * like server data does, and enforces the same create-once rule for personality profiles.
 */
class FakeProfileRepository(vararg initial: UserProfile) : ProfileRepository {
    val profiles = initial.associateBy { it.uid }.toMutableMap()
    val personalities = mutableMapOf<String, PersonalityProfile>()
    var failAll = false
    var finalizeAttempts = 0
        private set

    /** Thrown instead of the generic failure when set, e.g. a PERMISSION_DENIED from Firestore. */
    var failWith: ProfileException? = null

    /** Makes only getPersonality fail, e.g. live rules that don't cover personalityProfiles. */
    var failPersonalityReadWith: ProfileException? = null

    /** Makes only finalizePersonality fail, e.g. to simulate going offline mid-save. */
    var failFinalizeWith: ProfileException? = null

    fun withPersonality(uid: String, response: String = TestReplies.Valid) = apply {
        personalities[uid] = PersonalityProfile(response, importedAtMillis = ImportedAt)
    }

    override suspend fun getProfile(uid: String): UserProfile? {
        check()
        return profiles[uid]
    }

    override suspend fun createProfile(profile: UserProfile) {
        check()
        // Mirrors the Firestore transaction: an existing profile only takes the new name.
        profiles[profile.uid] = profiles[profile.uid]?.copy(name = profile.name) ?: profile
    }

    override suspend fun getPersonality(uid: String): PersonalityProfile? {
        failPersonalityReadWith?.let { throw it }
        check()
        return personalities[uid]
    }

    override suspend fun finalizePersonality(uid: String, response: String): PersonalityProfile {
        finalizeAttempts++
        failFinalizeWith?.let { throw it }
        check()
        // Create-once, like the rules: an existing profile is returned and never replaced.
        return personalities.getOrPut(uid) { PersonalityProfile(response, importedAtMillis = ImportedAt) }
    }

    override suspend fun skipPersonality(uid: String) {
        check()
        profiles[uid] = profiles.getValue(uid).copy(personalitySkipped = true)
    }

    private fun check() {
        failWith?.let { throw it }
        if (failAll) throw ProfileException("Simulated profile failure")
    }

    private companion object {
        const val ImportedAt = 1_761_000_000_000L
    }
}
