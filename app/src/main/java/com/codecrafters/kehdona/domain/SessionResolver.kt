package com.codecrafters.kehdona.domain

import com.codecrafters.kehdona.data.auth.AuthProvider
import com.codecrafters.kehdona.data.auth.AuthRepository
import com.codecrafters.kehdona.data.auth.AuthUser
import com.codecrafters.kehdona.data.profile.ProfileRepository

/** Where a person belongs in the account flow right now. */
enum class SessionStage { SignedOut, VerifyEmail, ProfileSetup, DiscoverVibe, Home }

/** Works out the next screen from the auth user and their stored profile. */
class SessionResolver(
    private val auth: AuthRepository,
    private val profiles: ProfileRepository,
) {
    /** Throws [com.codecrafters.kehdona.data.profile.ProfileException] if the profile can't be loaded. */
    suspend fun resolve(): SessionStage {
        val user = auth.currentUser ?: return SessionStage.SignedOut
        if (needsEmailVerification(user)) return SessionStage.VerifyEmail
        val profile = profiles.getProfile(user.uid) ?: return SessionStage.ProfileSetup
        return if (profile.personalityProfile.isNullOrBlank() && !profile.personalitySkipped) {
            SessionStage.DiscoverVibe
        } else {
            SessionStage.Home
        }
    }

    /** Email/password accounts must confirm their address; Google and phone are already verified. */
    fun needsEmailVerification(user: AuthUser): Boolean =
        user.provider == AuthProvider.Email && !user.isEmailVerified
}
