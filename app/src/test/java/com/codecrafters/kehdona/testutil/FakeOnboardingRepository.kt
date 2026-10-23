package com.codecrafters.kehdona.testutil

import com.codecrafters.kehdona.data.onboarding.OnboardingRepository
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** In-memory [OnboardingRepository] whose reads and writes can be made to fail. */
class FakeOnboardingRepository(completed: Boolean = false) : OnboardingRepository {
    var isCompleted = completed
        private set
    var failReads = false
    var failWrites = false
    var writeCount = 0
        private set

    override val hasCompletedOnboarding: Flow<Boolean> = flow {
        if (failReads) throw IOException("Simulated read failure")
        emit(isCompleted)
    }

    override suspend fun setOnboardingCompleted() {
        writeCount++
        if (failWrites) throw IOException("Simulated write failure")
        isCompleted = true
    }
}
