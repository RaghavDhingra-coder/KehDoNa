package com.codecrafters.kehdona.testutil

import com.codecrafters.kehdona.data.match.Match
import com.codecrafters.kehdona.data.match.MatchException
import com.codecrafters.kehdona.data.match.MatchRepository
import com.codecrafters.kehdona.data.match.MatchStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Stands in for the Cloud Functions + matchState listener. [nextRequestResult] scripts what the
 * server answers; [serverMatches] simulates the other person's request matching us.
 */
class FakeMatchRepository : MatchRepository {
    val status = MutableStateFlow<MatchStatus>(MatchStatus.Idle)
    val matches = mutableMapOf<String, Match>()
    var nextRequestResult: MatchStatus = MatchStatus.Waiting
    var failRequestWith: MatchException? = null
    var requests = 0
        private set
    var cancels = 0
        private set

    override fun observeStatus(uid: String): Flow<MatchStatus> = status

    override suspend fun requestMatch(): MatchStatus {
        requests++
        failRequestWith?.let { throw it }
        status.value = nextRequestResult
        return nextRequestResult
    }

    override suspend fun cancelRequest(): MatchStatus {
        cancels++
        if (status.value is MatchStatus.Matched) return status.value
        status.value = MatchStatus.Idle
        return MatchStatus.Idle
    }

    override suspend fun getMatch(uid: String, matchId: String): Match = matches.getValue(matchId)

    /** Another member's request just matched us (the server wrote matchState). */
    fun serverMatches(match: Match) {
        matches[match.id] = match
        status.value = MatchStatus.Matched(match.id)
    }
}
