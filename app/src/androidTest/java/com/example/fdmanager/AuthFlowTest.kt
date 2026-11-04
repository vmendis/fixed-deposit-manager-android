package com.example.fdmanager

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.fdmanager.data.DevicePrefs
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

    companion object {
        // #9 — must run BEFORE the activity launches (the compose rule launches MainActivity
        // before @Before executes). Forces the fake auth path even on machines where
        // google-services.json exists: these flows stay 0-quota mock, no Firebase traffic.
        @JvmStatic
        @org.junit.BeforeClass
        fun forceFakeAuth() {
            com.example.fdmanager.data.AuthRepositoryProvider.forceFakeForTests = true
            // #10 — belt-and-braces: the provider ALSO auto-detects instrumentation
            // (it probes for this very class), but the explicit flag documents intent.
            com.example.fdmanager.data.FdRepositoryProvider.forceFakeForTests = true
        }
    }

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Before
    fun resetState() {
        SettingsStore.setHasSeenOnboarding(true)
        SettingsStore.clearLoadingErrorDebug()
        SettingsStore.setDemoMode(false)
        SettingsStore.clearMockLoggedIn()
        // #9 round 3 — DevicePrefs is a process-wide singleton persisted to SharedPreferences;
        // never let a locked-state test leak into other classes.
        DevicePrefs.setHasAccountOnDevice(false)
        FdRepository.get().reset(SampleData.seed(LocalDate.now()))
    }

    // roadmap #30 — the eye toggle flips between masked and revealed on the sign-in field.
    // Asserted via the icon's contentDescription, which is the toggle's state signal.
    @Test
    fun passwordToggleRevealsAndHides() {
        rule.onNodeWithText("Settings").performClick()
        rule.onNodeWithTag("accountSignIn").performClick()

        rule.onNodeWithTag("passwordField").performTextInput("secret123")
        rule.onNodeWithContentDescription("Show password").assertIsDisplayed()

        rule.onNodeWithTag("passwordToggle").performClick()
        rule.onNodeWithContentDescription("Hide password").assertIsDisplayed()

        rule.onNodeWithTag("passwordToggle").performClick()
        rule.onNodeWithContentDescription("Show password").assertIsDisplayed()
    }

    // #9 round 3 — while session-locked, the Settings data rows must REDIRECT to sign-in,
    // not export/delete/reset. (TI-009 §4 acceptance finding: signed-out export was possible.)
    @Test
    fun lockedSettingsDataRowsRedirectToSignIn() {
        DevicePrefs.setHasAccountOnDevice(true)
        try {
            rule.onNodeWithText("Settings").performClick()

            rule.onNodeWithTag("exportRow").performScrollTo()
            rule.onNodeWithText("Signed out - sign in to export").assertIsDisplayed()

            rule.onNodeWithTag("deleteDataRow").performScrollTo()
            rule.onNodeWithText("Signed out - sign in to manage data").assertIsDisplayed()

            // Clicking a locked row lands on the Sign in screen instead of opening its dialog
            rule.onNodeWithTag("exportRow").performScrollTo()
            rule.onNodeWithTag("exportRow").performClick()
            rule.onNodeWithTag("auth-0100r9").assertIsDisplayed()
        } finally {
            DevicePrefs.setHasAccountOnDevice(false)
        }
    }

    @Test
    fun settingsShowsSignInWhenLoggedOut() {
        rule.onNodeWithText("Settings").performClick()
        rule.onNodeWithTag("accountSignIn").assertIsDisplayed()
        rule.onNodeWithText("Mock auth - fake session, 0 quota. Phase 2 -> Firebase Auth", substring = true).assertIsDisplayed()
    }

    @Test
    fun signInFlowValidatesAndCreatesMockSession() {
        rule.onNodeWithText("Settings").performClick()
        rule.onNodeWithTag("accountSignIn").performClick()

        rule.onNodeWithTag("auth-0100r9").assertIsDisplayed()
        rule.onNodeWithTag("vssboxLogoAuth").assertIsDisplayed()

        // Empty submit → email required
        rule.onNodeWithTag("authSubmit").performClick()
        rule.onNodeWithText("Email is required").assertIsDisplayed()

        // Invalid email
        rule.onNodeWithTag("emailField").performTextInput("bad")
        rule.onNodeWithTag("authSubmit").performClick()
        rule.onNodeWithText("Enter a valid email").assertIsDisplayed()

        // Now fix email to valid (field currently contains "bad", appending makes "badtest@example.com" which is valid)
        // and test short password validation - stay on same screen, don't navigate to Settings (bottom nav hidden on auth screens)
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

        // After sign-in, SignIn pops back to Settings. Wait for sign-out row first.
        rule.waitUntil(15000) {
            try {
                rule.onNodeWithTag("accountSignOut").assertIsDisplayed()
                true
            } catch (e: Throwable) {
                false
            }
        }
        rule.onNodeWithTag("accountSignOut").assertIsDisplayed()

        // Email appears as title of account row (accountEmail tag) + SectionHeader subtitle.
        // Use unmerged tree because Row with clickable merges child semantics.
        rule.waitUntil(15000) {
            try {
                try {
                    rule.onNodeWithTag("accountEmail", useUnmergedTree = true).assertIsDisplayed()
                    true
                } catch (e2: Throwable) {
                    rule.onNodeWithText("vssbox.com@gmail.com", substring = true, useUnmergedTree = true).assertIsDisplayed()
                    true
                }
            } catch (e: Throwable) {
                false
            }
        }
        // Final asserts with unmerged tree
        try {
            rule.onNodeWithTag("accountEmail", useUnmergedTree = true).assertIsDisplayed()
        } catch (e: Throwable) {
            rule.onNodeWithText("vssbox.com@gmail.com", substring = true, useUnmergedTree = true).assertIsDisplayed()
        }
        rule.onNodeWithTag("accountSignOut").assertIsDisplayed()
    }
}
