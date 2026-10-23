package com.codecrafters.kehdona.data.onboarding

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Remembers whether this device has already seen the introduction. */
interface OnboardingRepository {
    /** Emits `true` once onboarding is finished. May throw [java.io.IOException]. */
    val hasCompletedOnboarding: Flow<Boolean>

    suspend fun setOnboardingCompleted()
}

private val Context.appPreferences: DataStore<Preferences> by preferencesDataStore(name = "kehdona_preferences")

class DataStoreOnboardingRepository(context: Context) : OnboardingRepository {
    private val dataStore = context.applicationContext.appPreferences

    override val hasCompletedOnboarding: Flow<Boolean> =
        dataStore.data.map { preferences -> preferences[OnboardingCompletedKey] ?: false }

    override suspend fun setOnboardingCompleted() {
        dataStore.edit { preferences -> preferences[OnboardingCompletedKey] = true }
    }

    private companion object {
        val OnboardingCompletedKey = booleanPreferencesKey("onboarding_completed")
    }
}
