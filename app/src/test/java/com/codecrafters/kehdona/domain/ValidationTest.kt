package com.codecrafters.kehdona.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ValidationTest {
    @Test
    fun names_acceptRealNames_includingNonLatinScripts() {
        listOf("Riya Sharma", "D'Souza", "Anne-Marie", "Dr. K. Rao", "राघव ढींगरा", "Zoë").forEach {
            assertNull("Expected '$it' to be valid", Validation.nameError(it))
        }
    }

    @Test
    fun names_rejectBlankShortSymbolsAndTooLong() {
        assertNotNull(Validation.nameError("   "))
        assertNotNull(Validation.nameError("R"))
        assertNotNull(Validation.nameError("Riya123"))
        assertNotNull(Validation.nameError("<script>"))
        assertNotNull(Validation.nameError("a".repeat(Validation.NameMaxLength + 1)))
    }

    @Test
    fun normalizeName_trimsAndCollapsesSpaces() {
        assertEquals("Riya Sharma", Validation.normalizeName("  Riya    Sharma "))
    }

    @Test
    fun phone_buildsE164_andRejectsNonsense() {
        assertEquals("+919876543210", Validation.toE164("+91", "98765 43210"))
        assertEquals("+919876543210", Validation.toE164("91", "098765 43210"))
        assertNull(Validation.toE164("+91", "123"))
        assertNull(Validation.toE164("", "9876543210"))
        assertNotNull(Validation.phoneError("+91", ""))
    }

    @Test
    fun email_andPassword_rules() {
        assertNull(Validation.emailError("riya@college.edu.in"))
        assertNotNull(Validation.emailError("riya@college"))
        assertNotNull(Validation.passwordError("short1"))
        assertNotNull(Validation.passwordError("onlyletters"))
        assertNull(Validation.passwordError("letters123"))
    }

    @Test
    fun otp_mustBeSixDigits() {
        assertNull(Validation.otpError("123456"))
        assertNotNull(Validation.otpError("12345"))
        assertNotNull(Validation.otpError("12a456"))
    }
}
