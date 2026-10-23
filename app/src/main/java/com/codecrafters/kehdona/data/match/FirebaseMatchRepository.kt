package com.codecrafters.kehdona.data.match

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/** Calls the `requestMatch` / `cancelMatchRequest` functions and reads match documents. */
class FirebaseMatchRepository(
    private val functions: () -> FirebaseFunctions = { FirebaseFunctions.getInstance(Region) },
    private val firestore: () -> FirebaseFirestore = { FirebaseFirestore.getInstance() },
) : MatchRepository {

    override fun observeStatus(uid: String): Flow<MatchStatus> = callbackFlow {
        val registration = firestore().collection("matchState").document(uid).addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w(LogTag, "Firestore listen matchState failed: ${error.code.name}")
                close(firestoreFailure(error))
                return@addSnapshotListener
            }
            val status = when (snapshot?.getString("status")) {
                "waiting" -> MatchStatus.Waiting
                "matched" -> snapshot.getString("matchId")?.let { MatchStatus.Matched(it) } ?: MatchStatus.Idle
                else -> MatchStatus.Idle
            }
            trySend(status)
        }
        awaitClose { registration.remove() }
    }

    override suspend fun requestMatch(): MatchStatus = call("requestMatch")

    override suspend fun cancelRequest(): MatchStatus = call("cancelMatchRequest")

    override suspend fun getMatch(uid: String, matchId: String): Match {
        val snapshot = try {
            firestore().collection("matches").document(matchId).get().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: FirebaseFirestoreException) {
            Log.w(LogTag, "Firestore get matches failed: ${e.code.name}")
            throw firestoreFailure(e)
        }
        if (!snapshot.exists()) throw MatchException(MatchFailure.Unknown, "We couldn't find your match.", "NOT_FOUND")
        val partner = (snapshot.get("userIds") as? List<*>)?.filterIsInstance<String>()?.firstOrNull { it != uid }
        @Suppress("UNCHECKED_CAST")
        val participants = snapshot.get("participants") as? Map<String, Map<String, Any?>>
        return Match(
            id = snapshot.id,
            partnerFirstName = participants?.get(partner)?.get("firstName") as? String ?: "Your match",
            score = snapshot.getDouble("score") ?: 0.0,
            sharedThemes = (snapshot.get("sharedThemes") as? List<*>)?.filterIsInstance<String>().orEmpty(),
        )
    }

    private suspend fun call(name: String): MatchStatus {
        val data = try {
            functions().getHttpsCallable(name).call().await().getData()
        } catch (e: CancellationException) {
            throw e
        } catch (e: FirebaseFunctionsException) {
            // Code and our own reason only: never request data or tokens.
            val reason = (e.details as? Map<*, *>)?.get("reason") as? String
            Log.w(LogTag, "Function $name failed: ${e.code.name}${reason?.let { " / $it" }.orEmpty()}")
            throw functionsFailure(e, reason)
        } catch (e: Exception) {
            Log.w(LogTag, "Function $name failed: ${e.javaClass.simpleName}")
            throw MatchException(MatchFailure.Unknown, "Matching didn't work. Please try again.", e.javaClass.simpleName, e)
        }
        val map = data as? Map<*, *>
        return when (map?.get("status")) {
            "matched" -> MatchStatus.Matched(map["matchId"] as String)
            "waiting" -> MatchStatus.Waiting
            else -> MatchStatus.Idle
        }
    }

    private companion object {
        const val LogTag = "KehDoNaMatch"

        /** Must match setGlobalOptions({ region }) in functions/src/index.ts. */
        const val Region = "us-central1"
    }
}

internal fun functionsFailure(e: FirebaseFunctionsException, reason: String?): MatchException {
    val code = e.code.name
    return when (e.code) {
        FirebaseFunctionsException.Code.FAILED_PRECONDITION -> when (reason) {
            "MATCHING_DETAILS_MISSING" -> MatchException(
                MatchFailure.DetailsMissing, "Add your gender and who you'd like to match with first.", reason, e,
            )
            "PERSONALITY_MISSING" -> MatchException(
                MatchFailure.PersonalityMissing, "Discover your vibe first: matching uses your personality profile.", reason, e,
            )
            "PROFILE_NOT_FOUND" -> MatchException(MatchFailure.ProfileMissing, "Create your profile before matching.", reason, e)
            else -> MatchException(MatchFailure.Unknown, "Matching isn't possible right now.", code, e)
        }
        FirebaseFunctionsException.Code.UNAUTHENTICATED ->
            MatchException(MatchFailure.NotSignedIn, "Your session ended. Please sign in again.", code, e)
        FirebaseFunctionsException.Code.UNAVAILABLE, FirebaseFunctionsException.Code.DEADLINE_EXCEEDED ->
            MatchException(MatchFailure.Network, "We couldn't reach the server. Check your connection and try again.", code, e)
        // NOT_FOUND / UNIMPLEMENTED: the functions aren't deployed for this project.
        FirebaseFunctionsException.Code.NOT_FOUND, FirebaseFunctionsException.Code.UNIMPLEMENTED ->
            MatchException(MatchFailure.BackendUnavailable, "Matching isn't switched on for KehDoNa yet.", code, e)
        FirebaseFunctionsException.Code.PERMISSION_DENIED ->
            MatchException(MatchFailure.PermissionDenied, "The server refused the matching request.", code, e)
        else -> MatchException(MatchFailure.Unknown, "Matching didn't work. Please try again.", code, e)
    }
}

internal fun firestoreFailure(e: FirebaseFirestoreException): MatchException = when (e.code) {
    FirebaseFirestoreException.Code.PERMISSION_DENIED ->
        MatchException(MatchFailure.PermissionDenied, "The server refused access to your match.", "Firestore: PERMISSION_DENIED", e)
    FirebaseFirestoreException.Code.UNAVAILABLE ->
        MatchException(MatchFailure.Network, "We couldn't reach the server. Check your connection and try again.", "Firestore: UNAVAILABLE", e)
    FirebaseFirestoreException.Code.UNAUTHENTICATED ->
        MatchException(MatchFailure.NotSignedIn, "Your session ended. Please sign in again.", "Firestore: UNAUTHENTICATED", e)
    else -> MatchException(MatchFailure.Unknown, "We couldn't load your match.", "Firestore: ${e.code.name}", e)
}
