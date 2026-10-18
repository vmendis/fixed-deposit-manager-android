package com.example.fdmanager

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
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
        rule.onNodeWithText("Settings").performClick()
        // Account section tag + subtitle proves logged-out state
        rule.onNodeWithTag("accountSignIn").assertIsDisplayed()
        rule.onNodeWithText("Mock auth - fake session, 0 quota. Phase 2 -> Firebase Auth", substring = true).assertIsDisplayed()
    }

    @Test
    fun signInFlowValidatesAndCreatesMockSession() {
        rule.onNodeWithText("Settings").performClick()
        rule.onNodeWithTag("accountSignIn").performClick()

        rule.onNodeWithTag("auth-0100r9").assertIsDisplayed()
        rule.onNodeWithTag("vssboxLogoAuth").assertIsDisplayed()

        rule.onNodeWithTag("authSubmit").performClick()
        rule.onNodeWithText("Email is required").assertIsDisplayed()

        rule.onNodeWithTag("emailField").performTextInput("bad")
        rule.onNodeWithTag("authSubmit").performClick()
        rule.onNodeWithText("Enter a valid email").assertIsDisplayed()

        // Clear and enter valid email + short password
        SettingsStore.clearMockLoggedIn()
        rule.onNodeWithText("Settings").performClick()
        rule.onNodeWithTag("accountSignIn").performClick()
        rule.onNodeWithTag("emailField").performTextInput("test@example.com")
        rule.onNodeWithTag("passwordField").performTextInput("123")
        rule.onNodeWithTag("authSubmit").performClick()
        rule.onNodeWithText("Password must be at least 6 characters").assertIsDisplayed()
    }

    @Test
    fun signUpAndForgotLinksNavigable() {
        rule.onNodeWithText("Settings").performClick()
        rule.onNodeWithTag("accountSignIn").performClick()

        rule.onNodeWithTag("signUpLink").performClick()
        // Use tag to avoid duplicate text (title + button both say Create account)
        rule.onNodeWithTag("confirmPasswordField").assertIsDisplayed()
        rule.onNodeWithTag("authSubmit").assertIsDisplayed()

        rule.onNodeWithTag("signInLink").performClick()
        rule.onNodeWithTag("auth-0100r9").assertIsDisplayed()

        rule.onNodeWithTag("forgotLink").performClick()
        rule.onNodeWithText("Forgot password").assertIsDisplayed()
        rule.onNodeWithTag("emailField").assertIsDisplayed()
    }

    @Test
    fun successfulSignInShowsEmailInSettings() {
        rule.onNodeWithText("Settings").performClick()
        rule.onNodeWithTag("accountSignIn").performClick()

        rule.onNodeWithTag("emailField").performTextInput("vssbox.com@gmail.com")
        rule.onNodeWithTag("passwordField").performTextInput("password123")
        rule.onNodeWithTag("authSubmit").performClick()

        // Pop back to Settings — email should be visible as title of account row
        rule.waitUntil(7000) {
            try {
                rule.onNodeWithText("vssbox.com@gmail.com").assertIsDisplayed()
                true
            } catch (e: Throwable) {
                false
            }
        }
        rule.onNodeWithTag("accountSignOut").assertIsDisplayed()
    }
}
