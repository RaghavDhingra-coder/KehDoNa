package com.codecrafters.kehdona.testutil

import com.codecrafters.kehdona.data.profile.ProfileException
import com.codecrafters.kehdona.data.profile.ProfileRepository
import com.codecrafters.kehdona.data.profile.UserProfile

/** In-memory [ProfileRepository] that records name updates and can be made to fail. */
class FakeProfileRepository(vararg initial: UserProfile) : ProfileRepository {
    val profiles = initial.associateBy { it.uid }.toMutableMap()
    val nameUpdates = mutableListOf<String>()
    var failAll = false

    override suspend fun getProfile(uid: String): UserProfile? {
        check()
        return profiles[uid]
    }

    override suspend fun createProfile(profile: UserProfile) {
        check()
        // Mirrors the Firestore transaction: an existing profile only takes the new name.
        profiles[profile.uid] = profiles[profile.uid]?.copy(name = profile.name) ?: profile
    }

    override suspend fun updateName(uid: String, name: String) {
        check()
        nameUpdates += name
        profiles[uid] = profiles.getValue(uid).copy(name = name)
    }

    override suspend fun savePersonality(uid: String, profileText: String) {
        check()
        profiles[uid] = profiles.getValue(uid).copy(personalityProfile = profileText, personalitySkipped = false)
    }

    override suspend fun skipPersonality(uid: String) {
        check()
        profiles[uid] = profiles.getValue(uid).copy(personalitySkipped = true)
    }

    private fun check() {
        if (failAll) throw ProfileException("Simulated profile failure")
    }
}
