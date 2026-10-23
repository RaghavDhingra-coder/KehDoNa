package com.codecrafters.kehdona.data.profile

import android.util.Log
import com.codecrafters.kehdona.model.Gender
import com.codecrafters.kehdona.model.MatchPreference
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

    private fun personalityDoc(uid: String) = firestore().collection(PersonalityProfiles).document(uid)

    override suspend fun getProfile(uid: String): UserProfile? = guard("get users") {
        doc(uid).get().await().takeIf { it.exists() }?.toProfile()
    }

    override suspend fun createProfile(profile: UserProfile) = guard("create users") {
        val ref = doc(profile.uid)
        // email / phoneNumber come from Firebase Auth; the rules require them to match the ID token.
        val data = buildMap {
            put(Name, profile.name)
            profile.email?.let { put(Email, it) }
            profile.phoneNumber?.let { put(Phone, it) }
            profile.gender?.let { put(GenderField, it.wire) }
            profile.genderSelfDescription?.takeIf { profile.gender == Gender.SelfDescribe }
                ?.let { put(GenderSelfDescription, it) }
            profile.matchPreference?.let { put(MatchPreferenceField, it.wire) }
            put(CreatedAt, FieldValue.serverTimestamp())
            put(UpdatedAt, FieldValue.serverTimestamp())
        }
        firestore().runTransaction { transaction ->
            if (transaction.get(ref).exists()) {
                // Already created (e.g. a retry after a network error): keep createdAt and contact
                // details, and apply only what the person just typed.
                val updates = buildMap<String, Any> {
                    put(Name, profile.name)
                    put(UpdatedAt, FieldValue.serverTimestamp())
                    // Matching details the person just chose (never contact details or createdAt).
                    if (profile.gender != null && profile.matchPreference != null) {
                        put(GenderField, profile.gender.wire)
                        put(MatchPreferenceField, profile.matchPreference.wire)
                        put(
                            GenderSelfDescription,
                            profile.genderSelfDescription?.takeIf { profile.gender == Gender.SelfDescribe }
                                ?: FieldValue.delete(),
                        )
                    }
                }
                transaction.update(ref, updates)
            } else {
                transaction.set(ref, data)
            }
        }.await()
        Unit
    }

    override suspend fun getPersonality(uid: String): PersonalityProfile? = guard("get personalityProfiles") {
        personalityDoc(uid).get().await().takeIf { it.exists() }?.toPersonality()
    }

    override suspend fun finalizePersonality(uid: String, response: String): PersonalityProfile =
        guard("finalize personalityProfiles") {
        val ref = personalityDoc(uid)
        // A transaction (not a plain set) so an offline save fails instead of queueing, and a
        // retry after an ambiguous failure finds the already-locked profile instead of overwriting.
        val existing = firestore().runTransaction { transaction ->
            val snapshot = transaction.get(ref)
            if (snapshot.exists()) {
                snapshot.toPersonality()
            } else {
                transaction.set(
                    ref,
                    mapOf(
                        Response to response,
                        PromptVersion to PersonalityPromptVersion,
                        ImportedAt to FieldValue.serverTimestamp(),
                    ),
                )
                null
            }
        }.await()
        // The commit succeeded, so the profile is locked; the server timestamp is read later.
        existing ?: PersonalityProfile(response = response, importedAtMillis = null)
    }

    override suspend fun skipPersonality(uid: String) = update(uid, mapOf(PersonalitySkipped to true))

    override suspend fun updateMatchingDetails(
        uid: String,
        gender: Gender,
        genderSelfDescription: String?,
        matchPreference: MatchPreference,
    ) = update(
        uid,
        mapOf(
            GenderField to gender.wire,
            MatchPreferenceField to matchPreference.wire,
            // The free-text description only exists alongside "Self-describe"; remove it otherwise.
            GenderSelfDescription to (genderSelfDescription?.takeIf { gender == Gender.SelfDescribe } ?: FieldValue.delete()),
        ),
    )

    private suspend fun update(uid: String, fields: Map<String, Any>) = guard("update users") {
        doc(uid).update(fields + (UpdatedAt to FieldValue.serverTimestamp())).await()
        Unit
    }

    private fun DocumentSnapshot.toProfile() = UserProfile(
        uid = id,
        name = getString(Name).orEmpty(),
        email = getString(Email),
        phoneNumber = getString(Phone),
        personalitySkipped = getBoolean(PersonalitySkipped) ?: false,
        gender = Gender.fromWire(getString(GenderField)),
        genderSelfDescription = getString(GenderSelfDescription),
        matchPreference = MatchPreference.fromWire(getString(MatchPreferenceField)),
    )

    private fun DocumentSnapshot.toPersonality() = PersonalityProfile(
        response = getString(Response).orEmpty(),
        importedAtMillis = getTimestamp(ImportedAt)?.toDate()?.time,
    )

    /** [operation] names the call and collection for logs; uids and profile content are never logged. */
    private suspend fun <T> guard(operation: String, block: suspend () -> T): T = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: FirebaseFirestoreException) {
        // Status code only; profile contents are never logged.
        Log.w(LogTag, "Firestore $operation failed: ${e.code.name}")
        throw profileException(e.code, e)
    } catch (e: Exception) {
        Log.w(LogTag, "Firestore $operation failed: ${e.javaClass.simpleName}")
        throw ProfileException(
            "We couldn't save or load your profile. Please try again. (${e.javaClass.simpleName})",
            code = e.javaClass.simpleName,
            cause = e,
        )
    }

    private companion object {
        const val LogTag = "KehDoNaProfile"
        const val Users = "users"
        const val Name = "name"
        const val Email = "email"
        const val Phone = "phoneNumber"
        const val PersonalitySkipped = "personalitySkipped"
        const val CreatedAt = "createdAt"
        const val UpdatedAt = "updatedAt"
        const val GenderField = "gender"
        const val GenderSelfDescription = "genderSelfDescription"
        const val MatchPreferenceField = "matchPreference"
        const val PersonalityProfiles = "personalityProfiles"
        const val Response = "response"
        const val PromptVersion = "promptVersion"
        const val ImportedAt = "importedAt"
        const val PersonalityPromptVersion = 1L
    }
}

/**
 * Turns a Firestore status into an honest message that ends with the real code, e.g.
 * "… (Firestore: PERMISSION_DENIED)", instead of guessing that the session expired.
 */
internal fun profileException(code: Code, cause: Throwable? = null): ProfileException {
    val message = when (code) {
        Code.UNAVAILABLE, Code.DEADLINE_EXCEEDED ->
            "We couldn't reach the server. Check your connection and try again."
        Code.UNAUTHENTICATED -> "Your session ended. Please sign in again."
        // Security rules refused the request (e.g. the project's rules aren't the KehDoNa ones yet).
        Code.PERMISSION_DENIED -> "The server refused access to your profile, so it couldn't be loaded or saved."
        Code.NOT_FOUND, Code.FAILED_PRECONDITION -> "KehDoNa's profile storage isn't available yet."
        else -> "We couldn't save or load your profile. Please try again."
    }
    return ProfileException("$message (Firestore: ${code.name})", code = code.name, cause = cause)
}
