package com.example.fdmanager

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.fdmanager.data.FdRepository
import com.example.fdmanager.data.SampleData
import com.example.fdmanager.data.SettingsStore
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * #2 Mock auth flow — UI only, fake session, 0 quota.
 * Verifies sign-in / sign-up / forgot navigable end-to-end with inline validation.
 */
@RunWith(AndroidJUnit4::class)
class AuthFlowTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Before
    fun resetState() {
        SettingsStore.setHasSeenOnboarding(true)
        SettingsStore.setDemoMode(false)
        SettingsStore.clearMockLoggedIn()
        FdRepository.get().reset(SampleData.seed(LocalDate.now()))
    }

    @Test
    fun settingsShowsSignInWhenLoggedOut() {
        // Open Settings tab
        rule.onNodeWithText("Settings").performClick()
        rule.onNodeWithText("Sign in").assertIsDisplayed()
        rule.onNodeWithTag("accountSignIn").assertIsDisplayed()
    }

    @Test
    fun signInFlowValidatesAndCreatesMockSession() {
        rule.onNodeWithText("Settings").performClick()
        rule.onNodeWithText("Sign in").performClick()

        // AUTH_GATE auth-0100r9
        rule.onNodeWithTag("auth-0100r9").assertIsDisplayed()
        rule.onNodeWithTag("vssboxLogoAuth").assertIsDisplayed()

        // Empty submit → validation errors
        rule.onNodeWithTag("authSubmit").performClick()
        rule.onNodeWithText("Email is required").assertIsDisplayed()

        // Invalid email
        rule.onNodeWithTag("emailField").performTextInput("bad")
        rule.onNodeWithTag("authSubmit").performClick()
        rule.onNodeWithText("Enter a valid email").assertIsDisplayed()

        // Valid email + short password → password error
        rule.onNodeWithTag("emailField").performTextInput("test@example.com")
        rule.onNodeWithTag("passwordField").performTextInput("123")
        rule.onNodeWithTag("authSubmit").performClick()
        rule.onNodeWithText("Password must be at least 6 characters").assertIsDisplayed()
    }

    @Test
    fun signUpAndForgotLinksNavigable() {
        rule.onNodeWithText("Settings").performClick()
        rule.onNodeWithText("Sign in").performClick()

        // Go to sign up
        rule.onNodeWithTag("signUpLink").performClick()
        rule.onNodeWithText("Create account").assertIsDisplayed()
        rule.onNodeWithTag("confirmPasswordField").assertIsDisplayed()

        // Back to sign in via link
        rule.onNodeWithTag("signInLink").performClick()
        rule.onNodeWithText("Sign in").assertIsDisplayed()

        // Forgot password
        rule.onNodeWithTag("forgotLink").performClick()
        rule.onNodeWithText("Forgot password").assertIsDisplayed()
        rule.onNodeWithTag("emailField").assertIsDisplayed()
    }

    @Test
    fun successfulSignInShowsEmailInSettings() {
        rule.onNodeWithText("Settings").performClick()
        rule.onNodeWithText("Sign in").performClick()

        rule.onNodeWithTag("emailField").performTextInput("vssbox.com@gmail.com")
        rule.onNodeWithTag("passwordField").performTextInput("password123")
        rule.onNodeWithTag("authSubmit").performClick()

        // Should pop back to Settings and show email + sign out
        rule.waitUntil(5000) { 
            try {
                rule.onNodeWithText("vssbox.com@gmail.com").assertIsDisplayed()
                true
            } catch (e: Throwable) { false }
        }
        rule.onNodeWithTag("accountSignOut").assertIsDisplayed()
    }
}
