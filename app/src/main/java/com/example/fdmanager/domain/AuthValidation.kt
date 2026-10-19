package com.example.fdmanager.domain

object AuthValidation {

    private val EMAIL_REGEX = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

    fun validateEmail(email: String): String? {
        val e = email.trim()
        if (e.isEmpty()) return "Email is required"
        if (!EMAIL_REGEX.matches(e)) return "Enter a valid email"
        if (e.length > 254) return "Email too long"
        return null
    }

    fun validatePassword(password: String): String? {
        if (password.isEmpty()) return "Password is required"
        if (password.length < 6) return "Password must be at least 6 characters"
        if (password.length > 128) return "Password too long"
        return null
    }

    fun validateConfirm(password: String, confirm: String): String? {
        if (confirm.isEmpty()) return "Confirm your password"
        if (password != confirm) return "Passwords do not match"
        return null
    }
}
