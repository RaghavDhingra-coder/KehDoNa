package com.codecrafters.kehdona.testutil

import com.codecrafters.kehdona.data.auth.AuthError
import com.codecrafters.kehdona.data.auth.AuthException
import com.codecrafters.kehdona.data.auth.AuthProvider
import com.codecrafters.kehdona.data.auth.AuthRepository
import com.codecrafters.kehdona.data.auth.AuthUser
import com.codecrafters.kehdona.data.auth.GoogleIdTokenProvider
import com.codecrafters.kehdona.data.auth.PhoneCodeResult
import com.codecrafters.kehdona.data.auth.PhoneResendToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** Scriptable in-memory [AuthRepository]. Only the code [validCode] verifies. */
class FakeAuthRepository(
    override val isConfigured: Boolean = true,
    signedIn: AuthUser? = null,
) : AuthRepository {
    private val user = MutableStateFlow(signedIn)

    var validCode = "123456"
    var autoVerify = false
    var failNextWith: AuthError? = null
    var failNextCode: String? = null
    var emailVerifiedOnReload = false
    var googleUser = user(uid = "google-uid", provider = AuthProvider.Google, email = "riya@gmail.com", displayName = "Riya G")

    val sentCodesTo = mutableListOf<String>()
    var resendTokensUsed = 0
        private set
    var verificationEmailsSent = 0
        private set
    var passwordResetsSent = 0
        private set
    var calls = 0
        private set

    override val currentUser: AuthUser? get() = user.value

    override fun observeUser(): Flow<AuthUser?> = user

    override suspend fun sendPhoneCode(phoneNumber: String, resendToken: PhoneResendToken?): PhoneCodeResult {
        maybeFail()
        sentCodesTo += phoneNumber
        if (resendToken != null) resendTokensUsed++
        if (autoVerify) return PhoneCodeResult.AutoVerified(signIn(phoneUser(phoneNumber)))
        return PhoneCodeResult.CodeSent("verification-${sentCodesTo.size}", Token)
    }

    override suspend fun verifyPhoneCode(verificationId: String, code: String): AuthUser {
        maybeFail()
        if (code != validCode) throw AuthException(AuthError.InvalidCode)
        return signIn(phoneUser(sentCodesTo.last()))
    }

    override suspend fun signUpWithEmail(email: String, password: String): AuthUser {
        maybeFail()
        verificationEmailsSent++
        return signIn(user(uid = "email-uid", provider = AuthProvider.Email, email = email))
    }

    override suspend fun signInWithEmail(email: String, password: String): AuthUser {
        maybeFail()
        return signIn(user(uid = "email-uid", provider = AuthProvider.Email, email = email, verified = true))
    }

    override suspend fun sendEmailVerification() {
        maybeFail()
        verificationEmailsSent++
    }

    override suspend fun reloadUser(): AuthUser? {
        maybeFail()
        user.value = user.value?.copy(isEmailVerified = emailVerifiedOnReload)
        return user.value
    }

    override suspend fun sendPasswordReset(email: String) {
        maybeFail()
        passwordResetsSent++
    }

    override suspend fun signInWithGoogle(idToken: String): AuthUser {
        maybeFail()
        return signIn(googleUser)
    }

    override suspend fun signOut() {
        user.value = null
    }

    private fun signIn(newUser: AuthUser): AuthUser {
        user.value = newUser
        return newUser
    }

    private fun maybeFail() {
        calls++
        if (!isConfigured) throw AuthException(AuthError.NotConfigured)
        failNextWith?.let {
            failNextWith = null
            throw AuthException(it, failNextCode)
        }
    }

    private fun phoneUser(number: String) = user(uid = "phone-uid", provider = AuthProvider.Phone, phone = number)

    private object Token : PhoneResendToken

    companion object {
        fun user(
            uid: String = "uid-1",
            provider: AuthProvider = AuthProvider.Phone,
            email: String? = null,
            phone: String? = null,
            displayName: String? = null,
            verified: Boolean = false,
        ) = AuthUser(uid, email, phone, displayName, verified, provider)
    }
}

/** Google token provider that succeeds, or fails with [error] (and optional [code]). */
class FakeGoogleIdTokenProvider(
    var error: AuthError? = null,
    var code: String? = null,
    override val isConfigured: Boolean = true,
) : GoogleIdTokenProvider {
    var requests = 0
        private set

    override suspend fun requestIdToken(): String {
        requests++
        error?.let { throw AuthException(it, code) }
        return "google-id-token"
    }
}
