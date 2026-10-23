package com.codecrafters.kehdona.domain

/** Form validation for sign-up and log-in. Each function returns an error message, or null when valid. */
object Validation {
    const val NameMaxLength = 50
    const val PasswordMinLength = 8
    const val OtpLength = 6

    private val NamePattern = Regex("^[\\p{L}\\p{M}][\\p{L}\\p{M} .'-]*$")
    private val EmailPattern = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    /** Trims and collapses inner whitespace, e.g. "  Riya   Sharma " → "Riya Sharma". */
    fun normalizeName(raw: String): String = raw.trim().replace(Regex("\\s+"), " ")

    fun nameError(raw: String): String? {
        val name = normalizeName(raw)
        return when {
            name.isEmpty() -> "Enter your name."
            name.length < 2 -> "Your name needs at least 2 letters."
            name.length > NameMaxLength -> "Keep your name under $NameMaxLength characters."
            !NamePattern.matches(name) -> "Use letters, spaces, apostrophes, hyphens or dots only."
            else -> null
        }
    }

    fun emailError(raw: String): String? = when {
        raw.isBlank() -> "Enter your email."
        !EmailPattern.matches(raw.trim()) -> "That email doesn't look right."
        else -> null
    }

    fun passwordError(password: String): String? = when {
        password.length < PasswordMinLength -> "Use at least $PasswordMinLength characters."
        password.none { it.isLetter() } || password.none { it.isDigit() } -> "Mix letters and numbers."
        else -> null
    }

    /** Joins a country code and local number into E.164 ("+919876543210"), or null if invalid. */
    fun toE164(countryCode: String, number: String): String? {
        val code = countryCode.filter { it.isDigit() }
        val local = number.filter { it.isDigit() }.trimStart('0')
        if (code.isEmpty() || code.length > 3 || local.length < 6) return null
        val digits = code + local
        return if (digits.length in 8..15) "+$digits" else null
    }

    fun phoneError(countryCode: String, number: String): String? = when {
        number.isBlank() -> "Enter your phone number."
        toE164(countryCode, number) == null -> "That phone number doesn't look right."
        else -> null
    }

    fun otpError(code: String): String? =
        if (code.length == OtpLength && code.all { it.isDigit() }) null else "Enter the $OtpLength-digit code."
}
