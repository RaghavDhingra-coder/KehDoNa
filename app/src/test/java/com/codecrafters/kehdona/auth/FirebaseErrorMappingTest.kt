package com.codecrafters.kehdona.auth

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.codecrafters.kehdona.data.auth.AuthError
import com.codecrafters.kehdona.data.auth.AuthException
import com.codecrafters.kehdona.data.auth.toAuthException
import com.codecrafters.kehdona.data.profile.profileException
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FirebaseFirestoreException.Code
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Regression tests: real Firebase failures must map to specific, honest messages with their codes. */
@RunWith(AndroidJUnit4::class) // Firebase exception types need the Android runtime.
class FirebaseErrorMappingTest {

    @Test
    fun providerDisabled_isMethodNotEnabled_andShowsTheRealCode() {
        val mapped = FirebaseAuthException("ERROR_OPERATION_NOT_ALLOWED", "disabled").toAuthException()

        assertEquals(AuthError.MethodNotEnabled, mapped.error)
        assertTrue(mapped.message.endsWith("(ERROR_OPERATION_NOT_ALLOWED)"))
    }

    @Test
    fun unregisteredAppOrFailedIntegrity_isAppNotAuthorized_notMethodNotEnabled() {
        listOf("ERROR_APP_NOT_AUTHORIZED", "ERROR_MISSING_CLIENT_IDENTIFIER", "ERROR_INVALID_APP_CREDENTIAL").forEach { code ->
            val mapped = FirebaseAuthException(code, "not authorised").toAuthException()
            assertEquals(code, AuthError.AppNotAuthorized, mapped.error)
            assertTrue(mapped.message.contains(code))
        }
    }

    @Test
    fun phoneBillingError_inInternalMessage_isRecognisedByItsCode() {
        val mapped = FirebaseException("An internal error has occurred. [ BILLING_NOT_ENABLED ]").toAuthException()

        assertEquals(AuthError.PhoneUnavailable, mapped.error)
        assertEquals("BILLING_NOT_ENABLED", mapped.code)
    }

    @Test
    fun unknownFailures_stillShowTheirCode() {
        val coded = FirebaseAuthException("ERROR_SOMETHING_NEW", "?").toAuthException()
        assertEquals(AuthError.Unknown, coded.error)
        assertTrue(coded.message.contains("ERROR_SOMETHING_NEW"))

        val uncoded = IllegalStateException("boom").toAuthException()
        assertTrue(uncoded.message.contains("IllegalStateException"))
    }

    @Test
    fun ordinaryInputMistakes_keepFriendlyTextWithoutCodes() {
        val wrongCode = FirebaseAuthInvalidCredentialsException("ERROR_INVALID_VERIFICATION_CODE", "bad").toAuthException()
        assertEquals(AuthError.InvalidCode, wrongCode.error)
        assertEquals(AuthError.InvalidCode.message, wrongCode.message)

        val wrongPassword = FirebaseAuthInvalidCredentialsException("ERROR_INVALID_CREDENTIAL", "bad").toAuthException()
        assertEquals(AuthError.InvalidCredentials, wrongPassword.error)

        val taken = FirebaseAuthUserCollisionException("ERROR_EMAIL_ALREADY_IN_USE", "taken").toAuthException()
        assertEquals(AuthError.EmailInUse, taken.error)
    }

    @Test
    fun network_isNetwork() {
        assertEquals(AuthError.Network, FirebaseNetworkException("offline").toAuthException().error)
    }

    @Test
    fun googleNotConfigured_isNotTheGlobalFirebaseMessage() {
        val e = AuthException(AuthError.GoogleNotConfigured, code = "MISSING_DEFAULT_WEB_CLIENT_ID")

        assertFalse(e.message.contains(AuthError.NotConfigured.message))
        assertTrue(e.message.contains("Google"))
        assertTrue(e.message.contains("MISSING_DEFAULT_WEB_CLIENT_ID"))
    }

    @Test
    fun firestorePermissionDenied_showsCode_andDoesNotBlameTheSession() {
        val e = profileException(Code.PERMISSION_DENIED)

        assertEquals("PERMISSION_DENIED", e.code)
        assertTrue(e.message!!.endsWith("(Firestore: PERMISSION_DENIED)"))
        assertFalse(e.message!!.contains("sign in again"))
    }

    @Test
    fun firestoreUnauthenticated_asksToSignInAgain() {
        val e = profileException(Code.UNAUTHENTICATED)

        assertTrue(e.message!!.contains("sign in again"))
        assertTrue(e.message!!.contains("UNAUTHENTICATED"))
    }
}
