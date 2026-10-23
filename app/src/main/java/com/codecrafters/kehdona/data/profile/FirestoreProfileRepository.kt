package com.codecrafters.kehdona.data.profile

import android.util.Log
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

    override suspend fun getPersonality(uid: String): PersonalityProfile? = guard {
        personalityDoc(uid).get().await().takeIf { it.exists() }?.toPersonality()
    }

    override suspend fun finalizePersonality(uid: String, response: String): PersonalityProfile = guard {
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

    private suspend fun update(uid: String, fields: Map<String, Any>) = guard {
        doc(uid).update(fields + (UpdatedAt to FieldValue.serverTimestamp())).await()
        Unit
    }

    private fun DocumentSnapshot.toProfile() = UserProfile(
        uid = id,
        name = getString(Name).orEmpty(),
        email = getString(Email),
        phoneNumber = getString(Phone),
        personalitySkipped = getBoolean(PersonalitySkipped) ?: false,
    )

    private fun DocumentSnapshot.toPersonality() = PersonalityProfile(
        response = getString(Response).orEmpty(),
        importedAtMillis = getTimestamp(ImportedAt)?.toDate()?.time,
    )

    private suspend fun <T> guard(block: suspend () -> T): T = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: FirebaseFirestoreException) {
        // Status code only; profile contents are never logged.
        Log.w(LogTag, "Firestore profile call failed: ${e.code.name}")
        throw profileException(e.code, e)
    } catch (e: Exception) {
        Log.w(LogTag, "Firestore profile call failed: ${e.javaClass.simpleName}")
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
