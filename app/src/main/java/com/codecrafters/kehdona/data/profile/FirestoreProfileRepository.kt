package com.codecrafters.kehdona.data.profile

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FirebaseFirestoreException.Code
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

/**
 * Profiles live in `users/{uid}`; the security rules in `firestore.rules` let each
 * person read and write only their own document. Fields are mapped by hand so R8
 * never needs reflection keep rules.
 */
class FirestoreProfileRepository(
    private val firestore: () -> FirebaseFirestore = { FirebaseFirestore.getInstance() },
) : ProfileRepository {

    private fun doc(uid: String) = firestore().collection(Users).document(uid)

    override suspend fun getProfile(uid: String): UserProfile? = guard {
        doc(uid).get().await().takeIf { it.exists() }?.toProfile()
    }

    override suspend fun createProfile(profile: UserProfile) = guard {
        val ref = doc(profile.uid)
        // email / phoneNumber come from Firebase Auth; the rules require them to match the ID token.
        val data = buildMap {
            put(Name, profile.name)
            profile.email?.let { put(Email, it) }
            profile.phoneNumber?.let { put(Phone, it) }
            put(CreatedAt, FieldValue.serverTimestamp())
            put(UpdatedAt, FieldValue.serverTimestamp())
        }
        firestore().runTransaction { transaction ->
            if (transaction.get(ref).exists()) {
                // Already created (e.g. a retry after a network error): keep createdAt and contact
                // details, and apply only the name the person just typed.
                transaction.update(ref, mapOf(Name to profile.name, UpdatedAt to FieldValue.serverTimestamp()))
            } else {
                transaction.set(ref, data)
            }
        }.await()
        Unit
    }

    override suspend fun updateName(uid: String, name: String) = update(uid, mapOf(Name to name))

    override suspend fun savePersonality(uid: String, profileText: String) = update(
        uid,
        mapOf(
            Personality to profileText,
            PersonalityUpdatedAt to FieldValue.serverTimestamp(),
            PersonalitySkipped to false,
        ),
    )

    override suspend fun skipPersonality(uid: String) = update(uid, mapOf(PersonalitySkipped to true))

    private suspend fun update(uid: String, fields: Map<String, Any>) = guard {
        doc(uid).update(fields + (UpdatedAt to FieldValue.serverTimestamp())).await()
        Unit
    }

    private fun DocumentSnapshot.toProfile() = UserProfile(
        uid = id,
        name = getString(Name).orEmpty(),
        email = getString(Email),
        phoneNumber = getString(Phone),
        personalityProfile = getString(Personality),
        personalitySkipped = getBoolean(PersonalitySkipped) ?: false,
    )

    private suspend fun <T> guard(block: suspend () -> T): T = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: FirebaseFirestoreException) {
        throw ProfileException(
            when (e.code) {
                Code.UNAVAILABLE, Code.DEADLINE_EXCEEDED ->
                    "We couldn't reach the server. Check your connection and try again."
                Code.PERMISSION_DENIED, Code.UNAUTHENTICATED ->
                    "Your profile couldn't be accessed. Please sign in again."
                else -> "We couldn't save or load your profile. Please try again."
            },
            e,
        )
    } catch (e: Exception) {
        throw ProfileException("We couldn't save or load your profile. Please try again.", e)
    }

    private companion object {
        const val Users = "users"
        const val Name = "name"
        const val Email = "email"
        const val Phone = "phoneNumber"
        const val Personality = "personalityProfile"
        const val PersonalityUpdatedAt = "personalityUpdatedAt"
        const val PersonalitySkipped = "personalitySkipped"
        const val CreatedAt = "createdAt"
        const val UpdatedAt = "updatedAt"
    }
}
