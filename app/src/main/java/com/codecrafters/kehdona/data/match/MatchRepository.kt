package com.codecrafters.kehdona.data.match

import kotlinx.coroutines.flow.Flow

/** Where the signed-in member is in matching. Written only by the backend. */
sealed interface MatchStatus {
    data object Idle : MatchStatus
    data object Waiting : MatchStatus
    data class Matched(val matchId: String) : MatchStatus
}

/** A confirmed match as one participant sees it: no contact details, no profile text. */
data class Match(
    val id: String,
    val partnerFirstName: String,
    /** 0..1 from the server's compatibility scoring. */
    val score: Double,
    /** Words both personality profiles share. */
    val sharedThemes: List<String>,
)

enum class MatchFailure {
    /** Gender or match preference not set. */
    DetailsMissing,
    PersonalityMissing,
    ProfileMissing,
    NotSignedIn,
    Network,
    /** The matching functions aren't deployed / reachable for this Firebase project. */
    BackendUnavailable,
    PermissionDenied,
    Unknown,
}

/** A matching failure; [code] is the real Functions / Firestore code, shown to the user. */
class MatchException(val failure: MatchFailure, message: String, val code: String? = null, cause: Throwable? = null) :
    Exception(if (code == null) message else "$message ($code)", cause) {
    override val message: String get() = super.message!!
}

/**
 * Server-side matching via Cloud Functions; the client only asks, cancels and reads results.
 * Every function throws [MatchException].
 */
interface MatchRepository {
    /** Live status from `matchState/{uid}`; a missing document is [MatchStatus.Idle]. */
    fun observeStatus(uid: String): Flow<MatchStatus>

    suspend fun requestMatch(): MatchStatus

    suspend fun cancelRequest(): MatchStatus

    suspend fun getMatch(uid: String, matchId: String): Match
}
