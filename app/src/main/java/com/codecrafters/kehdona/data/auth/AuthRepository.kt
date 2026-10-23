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

/**
 * Why an auth call failed, already phrased for people. [showsCode] is false for ordinary
 * input mistakes (wrong code, wrong password), where the raw error code would only be noise.
 */
enum class AuthError(val message: String, val showsCode: Boolean = true) {
    NotConfigured("Sign-in isn't set up in this build yet. Firebase needs to be configured before accounts can work."),
    GoogleNotConfigured("Google sign-in isn't set up for KehDoNa yet. Use phone or email for now."),
    Network("We couldn't reach the server. Check your connection and try again."),
    InvalidPhone("That phone number doesn't look right. Include your country code.", showsCode = false),
    InvalidCode("That code isn't right. Check the SMS and try again.", showsCode = false),
    CodeExpired("That code has expired. Request a new one.", showsCode = false),
    TooManyRequests("Too many attempts. Please wait a little before trying again."),
    EmailInUse("An account already uses this email. Try logging in instead.", showsCode = false),
    WeakPassword("Choose a stronger password: at least 8 characters with letters and numbers.", showsCode = false),
    InvalidCredentials("That email and password don't match an account.", showsCode = false),
    AccountExistsWithOtherMethod("This email is already linked to another sign-in method. Log in the way you signed up."),
    UserDisabled("This account has been disabled."),
    MethodNotEnabled("This sign-in method is switched off for KehDoNa right now. Try another way to sign in."),
    AppNotAuthorized("This copy of the app isn't authorised for KehDoNa sign-in yet. Try email instead."),
    PhoneUnavailable("Phone sign-in isn't available for KehDoNa right now. Try email instead."),
    GoogleUnavailable("Google sign-in isn't available on this device right now."),
    NoGoogleAccount("No Google account found on this device. Add one in Settings, or use phone or email."),
    Cancelled("Sign-in was cancelled.", showsCode = false),
    NotSignedIn("Your session ended. Please sign in again."),
    Unknown("Something went wrong. Please try again."),
}

/**
 * An auth failure. [code] is the underlying Firebase / Credential Manager error code (never
 * a secret) and is appended to the message, so real problems aren't hidden behind generic text.
 */
class AuthException(
    val error: AuthError,
    val code: String? = null,
    cause: Throwable? = null,
) : Exception(if (code == null || !error.showsCode) error.message else "${error.message} ($code)", cause) {
    override val message: String get() = super.message!!
}

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

/** Obtains a Google ID token via Credential Manager. */
interface GoogleIdTokenProvider {
    /** False when the build has no OAuth web client ID (Google not enabled in Firebase). */
    val isConfigured: Boolean

    /** Throws [AuthException]. */
    suspend fun requestIdToken(): String
}
