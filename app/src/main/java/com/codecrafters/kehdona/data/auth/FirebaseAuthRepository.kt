package com.codecrafters.kehdona.data.auth

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await

class FirebaseAuthRepository(
    context: Context,
    private val activityTracker: ForegroundActivityTracker,
) : AuthRepository {

    override val isConfigured: Boolean = FirebaseApp.getApps(context).isNotEmpty()

    private val auth: FirebaseAuth by lazy {
        if (!isConfigured) throw AuthException(AuthError.NotConfigured)
        FirebaseAuth.getInstance()
    }

    override val currentUser: AuthUser?
        get() = if (isConfigured) auth.currentUser?.toAuthUser() else null

    override fun observeUser(): Flow<AuthUser?> {
        if (!isConfigured) return flowOf(null)
        return callbackFlow {
            val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser?.toAuthUser()) }
            auth.addAuthStateListener(listener)
            awaitClose { auth.removeAuthStateListener(listener) }
        }
    }

    override suspend fun sendPhoneCode(phoneNumber: String, resendToken: PhoneResendToken?): PhoneCodeResult =
        guard {
            val activity = activityTracker.activity ?: throw AuthException(AuthError.Unknown)
            suspendCancellableCoroutine { continuation ->
                val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                        if (!continuation.isActive) return
                        auth.signInWithCredential(credential)
                            .addOnSuccessListener { result ->
                                val user = result.user?.toAuthUser()
                                if (!continuation.isActive) return@addOnSuccessListener
                                if (user != null) {
                                    continuation.resume(PhoneCodeResult.AutoVerified(user))
                                } else {
                                    continuation.resumeWithException(AuthException(AuthError.Unknown))
                                }
                            }
                            .addOnFailureListener { if (continuation.isActive) continuation.resumeWithException(it) }
                    }

                    override fun onVerificationFailed(e: FirebaseException) {
                        if (continuation.isActive) continuation.resumeWithException(e)
                    }

                    override fun onCodeSent(id: String, token: PhoneAuthProvider.ForceResendingToken) {
                        if (continuation.isActive) {
                            continuation.resume(PhoneCodeResult.CodeSent(id, FirebaseResendToken(token)))
                        }
                    }
                }
                val options = PhoneAuthOptions.newBuilder(auth)
                    .setPhoneNumber(phoneNumber)
                    .setTimeout(60L, TimeUnit.SECONDS)
                    .setActivity(activity)
                    .setCallbacks(callbacks)
                    .apply { (resendToken as? FirebaseResendToken)?.let { setForceResendingToken(it.token) } }
                    .build()
                PhoneAuthProvider.verifyPhoneNumber(options)
            }
        }

    override suspend fun verifyPhoneCode(verificationId: String, code: String): AuthUser = guard {
        signIn(PhoneAuthProvider.getCredential(verificationId, code))
    }

    override suspend fun signUpWithEmail(email: String, password: String): AuthUser = guard {
        val user = auth.createUserWithEmailAndPassword(email, password).await().user
            ?: throw AuthException(AuthError.Unknown)
        user.sendEmailVerification().await()
        user.toAuthUser()
    }

    override suspend fun signInWithEmail(email: String, password: String): AuthUser = guard {
        auth.signInWithEmailAndPassword(email, password).await().user?.toAuthUser()
            ?: throw AuthException(AuthError.Unknown)
    }

    override suspend fun sendEmailVerification() = guard {
        val user = auth.currentUser ?: throw AuthException(AuthError.NotSignedIn)
        user.sendEmailVerification().await()
        Unit
    }

    override suspend fun reloadUser(): AuthUser? = guard {
        val user = auth.currentUser ?: return@guard null
        user.reload().await()
        auth.currentUser?.toAuthUser()
    }

    override suspend fun sendPasswordReset(email: String) = guard {
        auth.sendPasswordResetEmail(email).await()
        Unit
    }

    override suspend fun signInWithGoogle(idToken: String): AuthUser = guard {
        signIn(GoogleAuthProvider.getCredential(idToken, null))
    }

    override suspend fun signOut() {
        if (isConfigured) auth.signOut()
    }

    private suspend fun signIn(credential: AuthCredential): AuthUser =
        auth.signInWithCredential(credential).await().user?.toAuthUser()
            ?: throw AuthException(AuthError.Unknown)

    /** Runs a Firebase call and converts its failures into [AuthException]. */
    private suspend fun <T> guard(block: suspend () -> T): T {
        if (!isConfigured) throw AuthException(AuthError.NotConfigured)
        return try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: AuthException) {
            throw e
        } catch (e: Exception) {
            throw AuthException(e.toAuthError(), e)
        }
    }

    private class FirebaseResendToken(val token: PhoneAuthProvider.ForceResendingToken) : PhoneResendToken
}

internal fun FirebaseUser.toAuthUser(): AuthUser {
    val providers = providerData.map { it.providerId }
    val provider = when {
        GoogleAuthProvider.PROVIDER_ID in providers -> AuthProvider.Google
        PhoneAuthProvider.PROVIDER_ID in providers -> AuthProvider.Phone
        else -> AuthProvider.Email
    }
    return AuthUser(
        uid = uid,
        email = email?.takeIf { it.isNotBlank() },
        phoneNumber = phoneNumber?.takeIf { it.isNotBlank() },
        displayName = displayName?.takeIf { it.isNotBlank() },
        isEmailVerified = isEmailVerified,
        provider = provider,
    )
}

internal fun Throwable.toAuthError(): AuthError = when (this) {
    is FirebaseNetworkException -> AuthError.Network
    is FirebaseTooManyRequestsException -> AuthError.TooManyRequests
    is FirebaseAuthWeakPasswordException -> AuthError.WeakPassword
    is FirebaseAuthUserCollisionException -> when (errorCode) {
        "ERROR_EMAIL_ALREADY_IN_USE" -> AuthError.EmailInUse
        else -> AuthError.AccountExistsWithOtherMethod
    }
    is FirebaseAuthInvalidCredentialsException -> when (errorCode) {
        "ERROR_INVALID_PHONE_NUMBER", "ERROR_MISSING_PHONE_NUMBER" -> AuthError.InvalidPhone
        "ERROR_INVALID_VERIFICATION_CODE" -> AuthError.InvalidCode
        "ERROR_SESSION_EXPIRED" -> AuthError.CodeExpired
        else -> AuthError.InvalidCredentials
    }
    is FirebaseAuthInvalidUserException -> when (errorCode) {
        "ERROR_USER_DISABLED" -> AuthError.UserDisabled
        else -> AuthError.InvalidCredentials
    }
    is FirebaseAuthException -> when (errorCode) {
        "ERROR_OPERATION_NOT_ALLOWED", "ERROR_APP_NOT_AUTHORIZED" -> AuthError.MethodNotEnabled
        else -> AuthError.Unknown
    }
    else -> AuthError.Unknown
}
