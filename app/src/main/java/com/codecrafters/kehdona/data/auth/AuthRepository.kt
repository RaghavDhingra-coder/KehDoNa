package com.codecrafters.kehdona.data.auth

import kotlinx.coroutines.flow.Flow

/** The signed-in Firebase user, reduced to what the app needs. */
data class AuthUser(
    val uid: String,
    val email: String?,
    val phoneNumber: String?,
    val displayName: String?,
    val isEmailVerified: Boolean,
    val provider: AuthProvider,
)

enum class AuthProvider(val id: String) { Phone("phone"), Email("password"), Google("google.com") }

/** Opaque handle that lets Firebase resend an SMS without a new reCAPTCHA. */
interface PhoneResendToken

sealed interface PhoneCodeResult {
    /** An SMS was sent; the user types the code. */
    data class CodeSent(val verificationId: String, val resendToken: PhoneResendToken?) : PhoneCodeResult

    /** Android verified the number on its own (instant verification / SMS retriever). */
    data class AutoVerified(val user: AuthUser) : PhoneCodeResult
}

/** Why an auth call failed, already phrased for people. */
enum class AuthError(val message: String) {
    NotConfigured("Sign-in isn't set up in this build yet. Firebase needs to be configured before accounts can work."),
    Network("We couldn't reach the server. Check your connection and try again."),
    InvalidPhone("That phone number doesn't look right. Include your country code."),
    InvalidCode("That code isn't right. Check the SMS and try again."),
    CodeExpired("That code has expired. Request a new one."),
    TooManyRequests("Too many attempts. Please wait a little before trying again."),
    EmailInUse("An account already uses this email. Try logging in instead."),
    WeakPassword("Choose a stronger password: at least 8 characters with letters and numbers."),
    InvalidCredentials("That email and password don't match an account."),
    AccountExistsWithOtherMethod("This email is already linked to another sign-in method. Log in the way you signed up."),
    UserDisabled("This account has been disabled."),
    MethodNotEnabled("This sign-in method isn't enabled for KehDoNa yet. Try another way to sign in."),
    GoogleUnavailable("Google sign-in isn't available on this device right now."),
    NoGoogleAccount("No Google account found on this device. Add one in Settings, or use phone or email."),
    Cancelled("Sign-in was cancelled."),
    NotSignedIn("Your session ended. Please sign in again."),
    Unknown("Something went wrong. Please try again."),
}

class AuthException(val error: AuthError, cause: Throwable? = null) : Exception(error.message, cause)

/**
 * Authentication backed by Firebase. Every suspend function throws [AuthException]
 * on failure. Passwords and OTP codes are handed straight to Firebase and never stored.
 */
interface AuthRepository {
    /** False when the build has no Firebase configuration (no google-services.json). */
    val isConfigured: Boolean

    val currentUser: AuthUser?

    fun observeUser(): Flow<AuthUser?>

    suspend fun sendPhoneCode(phoneNumber: String, resendToken: PhoneResendToken? = null): PhoneCodeResult

    suspend fun verifyPhoneCode(verificationId: String, code: String): AuthUser

    /** Creates the account and sends a verification email. */
    suspend fun signUpWithEmail(email: String, password: String): AuthUser

    suspend fun signInWithEmail(email: String, password: String): AuthUser

    suspend fun sendEmailVerification()

    /** Refreshes the user from the server, e.g. to pick up a just-verified email. */
    suspend fun reloadUser(): AuthUser?

    suspend fun sendPasswordReset(email: String)

    suspend fun signInWithGoogle(idToken: String): AuthUser

    suspend fun signOut()
}

/** Obtains a Google ID token via Credential Manager. Throws [AuthException]. */
fun interface GoogleIdTokenProvider {
    suspend fun requestIdToken(): String
}
