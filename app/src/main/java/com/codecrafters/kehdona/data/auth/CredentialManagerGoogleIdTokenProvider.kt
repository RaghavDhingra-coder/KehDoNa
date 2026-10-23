package com.codecrafters.kehdona.data.auth

import android.annotation.SuppressLint
import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException

/** "Sign in with Google" through Credential Manager, producing an ID token for Firebase. */
class CredentialManagerGoogleIdTokenProvider(
    private val context: Context,
    private val activityTracker: ForegroundActivityTracker,
) : GoogleIdTokenProvider {

    /** Only true once Google is enabled in Firebase and google-services.json has the web OAuth client. */
    override val isConfigured: Boolean by lazy { webClientId() != null }

    override suspend fun requestIdToken(): String {
        val clientId = webClientId()
            ?: throw AuthException(AuthError.GoogleNotConfigured, code = "MISSING_DEFAULT_WEB_CLIENT_ID")
        val activity = activityTracker.activity ?: throw AuthException(AuthError.GoogleUnavailable, code = "NO_ACTIVITY")
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(GetSignInWithGoogleOption.Builder(clientId).build())
            .build()
        val credential = try {
            CredentialManager.create(activity).getCredential(activity, request).credential
        } catch (_: GetCredentialCancellationException) {
            throw AuthException(AuthError.Cancelled)
        } catch (_: NoCredentialException) {
            throw AuthException(AuthError.NoGoogleAccount)
        } catch (e: GetCredentialException) {
            throw AuthException(AuthError.GoogleUnavailable, code = e.type.substringAfterLast('.'), cause = e)
        }
        if (credential !is CustomCredential ||
            credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            throw AuthException(AuthError.GoogleUnavailable, code = "UNEXPECTED_CREDENTIAL_TYPE")
        }
        return try {
            GoogleIdTokenCredential.createFrom(credential.data).idToken
        } catch (e: GoogleIdTokenParsingException) {
            throw AuthException(AuthError.GoogleUnavailable, code = "ID_TOKEN_PARSING_FAILED", cause = e)
        }
    }

    /**
     * The OAuth web client ID is generated from google-services.json, so it only exists
     * once Firebase is configured; look it up by name instead of through R.
     */
    @SuppressLint("DiscouragedApi")
    private fun webClientId(): String? {
        val id = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        return if (id == 0) null else context.getString(id).takeIf { it.isNotBlank() }
    }
}
