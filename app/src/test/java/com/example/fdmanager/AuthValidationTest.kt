package com.example.fdmanager

import com.example.fdmanager.domain.AuthValidation
import org.junit.Assert.*
import org.junit.Test

class AuthValidationTest {

    @Test
    fun `email validation`() {
        assertEquals("Email is required", AuthValidation.validateEmail(""))
        assertEquals("Enter a valid email", AuthValidation.validateEmail("bad"))
        assertEquals("Enter a valid email", AuthValidation.validateEmail("a@b"))
        assertNull(AuthValidation.validateEmail("test@example.com"))
        assertNull(AuthValidation.validateEmail("vssbox.com@gmail.com"))
    }

    @Test
    fun `password validation`() {
        assertEquals("Password is required", AuthValidation.validatePassword(""))
        assertEquals("Password must be at least 6 characters", AuthValidation.validatePassword("12345"))
        assertNull(AuthValidation.validatePassword("123456"))
        assertNull(AuthValidation.validatePassword("securePass123"))
    }

    @Test
    fun `confirm password validation`() {
        assertEquals("Confirm your password", AuthValidation.validateConfirm("abc123", ""))
        assertEquals("Passwords do not match", AuthValidation.validateConfirm("abc123", "xyz"))
        assertNull(AuthValidation.validateConfirm("abc123", "abc123"))
    }

    @Test
    fun `email trimming`() {
        assertNull(AuthValidation.validateEmail("  test@example.com  "))
    }
}
