package com.example.fdmanager

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.fdmanager.data.FdRepository
import com.example.fdmanager.data.SampleData
import com.example.fdmanager.data.SettingsStore
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * #5 Trust UX — export chooser, delete-all confirmation gate, app lock, privacy line.
 *
 * Three rules this class obeys, each learned from a failed run:
 *
 * 1. **Navigate to Settings first.** The app starts on Home. The bottom-bar "Settings" label is how
 *    every other UI test gets there.
 *
 * 2. **createEmptyComposeRule + an explicit ActivityScenario.** `createAndroidComposeRule` launches
 *    the activity *before* `@Before` runs, so the start destination was computed from whatever the
 *    previous test left in the SettingsStore singleton. And `scenario.recreate()` restores the
 *    NavController back stack, so `startDest` is never re-evaluated. A fresh ActivityScenario is
 *    the only honest cold start.
 *
 * 3. **ComposeTestRule, not ComposeContentTestRule.** The shared `swipeUntilTag` helper extends
 *    ComposeContentTestRule, which `createEmptyComposeRule` does not return. This file carries its
 *    own scroller that works on the base type.
 */
@RunWith(AndroidJUnit4::class)
class TrustUxUiTest {

    @get:Rule
    val rule = createEmptyComposeRule()

    private lateinit var scenario: ActivityScenario<MainActivity>

    @Before
    fun launchWithKnownState() {
        resetStore()
        scenario = ActivityScenario.launch(MainActivity::class.java)
        // The bottom bar is always present on Home — wait for the first frame.
        rule.waitUntil(15_000) {
            rule.onAllNodesWithText("Settings").fetchSemanticsNodes().isNotEmpty()
        }
        rule.waitForIdle()
    }

    @After
    fun tearDown() {
        if (::scenario.isInitialized) scenario.close()
    }

    private fun resetStore() {
        SettingsStore.setHasSeenOnboarding(true)
        SettingsStore.setDemoMode(false)
        SettingsStore.clearMockLoggedIn()
        SettingsStore.setBiometricEnabled(false)
        SettingsStore.clearLoadingErrorDebug()
        FdRepository.get().reset(SampleData.seed(LocalDate.now()))
    }

    /** Relaunch the app from scratch with the lock enabled. */
    private fun relaunchWithLockOn() {
        scenario.close()
        SettingsStore.setBiometricEnabled(true)
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    // ---------------------------------------------------------------- helpers

    /** Home -> Settings via the bottom navigation bar. */
    private fun goToSettings() {
        rule.onNodeWithText("Settings").performClick()
        rule.waitForIdle()
        rule.waitUntil(15_000) {
            rule.onAllNodesWithText("Currency").fetchSemanticsNodes().isNotEmpty()
        }
    }

    /**
     * Scroll a verticalScroll Column until [tag] is on screen.
     * Works on ComposeTestRule, so it does not depend on the shared ComposeContentTestRule helper.
     */
    private fun scrollToTag(tag: String, maxSwipes: Int = 25) {
        repeat(maxSwipes) {
            try {
                rule.onNodeWithTag(tag).performScrollTo()
                rule.waitForIdle()
                return
            } catch (e: Throwable) { }

            try {
                rule.onNodeWithTag(tag, useUnmergedTree = true).performScrollTo()
                rule.waitForIdle()
                return
            } catch (e: Throwable) { }

            try {
                rule.onRoot().performTouchInput {
                    val h = visibleSize.height
                    val w = visibleSize.width
                    swipe(
                        start = Offset(w / 2f, h * 0.8f),
                        end = Offset(w / 2f, h * 0.2f),
                        durationMillis = 300
                    )
                }
                rule.waitForIdle()
            } catch (e: Throwable) {
                return
            }
        }
    }

    private fun openExportDialog() {
        goToSettings()
        scrollToTag("exportRow")
        rule.onNodeWithTag("exportRow").performClick()
        rule.waitForIdle()
        rule.waitUntil(15_000) {
            rule.onAllNodesWithTag("exportFormatDialog").fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun openWipeDialog() {
        goToSettings()
        scrollToTag("deleteDataRow")
        rule.onNodeWithTag("deleteDataRow").performClick()
        rule.waitForIdle()
        rule.waitUntil(15_000) {
            rule.onAllNodesWithTag("wipeDataDialog").fetchSemanticsNodes().isNotEmpty()
        }
    }

    /** Type the confirmation phrase into a field that may already hold text. */
    private fun typePhrase(text: String) {
        rule.onNodeWithTag("wipeTypedInput").performTextClearance()
        rule.onNodeWithTag("wipeTypedInput").performTextInput(text)
        rule.waitForIdle()
    }

    // ---------------------------------------------------------------- export

    @Test
    fun exportDialogOffersBothFormats() {
        openExportDialog()
        rule.onNodeWithTag("exportFormat_CSV").assertIsDisplayed()
        rule.onNodeWithTag("exportFormat_JSON").assertIsDisplayed()
        rule.onNodeWithTag("exportConfirm").assertIsDisplayed()
        rule.onNodeWithTag("exportCancel").assertIsDisplayed()
    }

    @Test
    fun exportDialogCancels() {
        openExportDialog()
        rule.onNodeWithTag("exportCancel").performClick()
        rule.waitForIdle()
        assertTrue(
            "dialog should be gone after Cancel",
            rule.onAllNodesWithTag("exportFormatDialog").fetchSemanticsNodes().isEmpty()
        )
    }

    // ------------------------------------------------------------ delete-all

    @Test
    fun wipeConfirmDisabledUntilCategoryAndPhrase() {
        openWipeDialog()

        rule.onNodeWithTag("wipeConfirm").assertIsNotEnabled()

        rule.onNodeWithTag("wipeCategory_FDS").performClick()
        rule.waitForIdle()
        rule.onNodeWithTag("wipeConfirm").assertIsNotEnabled()

        typePhrase("delete")
        rule.onNodeWithTag("wipeConfirm").assertIsNotEnabled()

        // Replace, not append — performTextClearance prevents "deleteDELETE".
        typePhrase("DELETE")
        rule.onNodeWithTag("wipeConfirm").assertIsEnabled()
    }

    @Test
    fun wipeWithPhraseButNoCategoryStaysDisabled() {
        openWipeDialog()
        typePhrase("DELETE")
        rule.onNodeWithTag("wipeConfirm").assertIsNotEnabled()
    }

    @Test
    fun wipeOfPreferencesKeepsDeposits() {
        val before = FdRepository.get().fds.value.size
        assertTrue("seed should be populated", before > 0)

        openWipeDialog()
        rule.onNodeWithTag("wipeCategory_SETTINGS").performClick()
        typePhrase("DELETE")
        rule.onNodeWithTag("wipeConfirm").performClick()
        rule.waitForIdle()

        assertEquals(
            "preferences wipe must not touch deposits",
            before,
            FdRepository.get().fds.value.size
        )
    }

    @Test
    fun wipeOfDepositsClearsTheStore() {
        assertTrue("seed should be populated", FdRepository.get().fds.value.isNotEmpty())

        openWipeDialog()
        rule.onNodeWithTag("wipeCategory_FDS").performClick()
        typePhrase("DELETE")
        rule.onNodeWithTag("wipeConfirm").performClick()
        rule.waitForIdle()

        assertTrue("deposits should be cleared", FdRepository.get().fds.value.isEmpty())
    }

    // ------------------------------------------------------------- app lock

    @Test
    fun lockScreenAppearsOnColdStartWhenEnabled() {
        relaunchWithLockOn()
        rule.waitUntil(15_000) {
            rule.onAllNodesWithTag("lockScreen").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithTag("lockScreen").assertIsDisplayed()
        rule.onNodeWithTag("unlockButton").assertIsDisplayed()
    }

    @Test
    fun unlockLeavesTheLockScreen() {
        relaunchWithLockOn()
        rule.waitUntil(15_000) {
            rule.onAllNodesWithTag("unlockButton").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithTag("unlockButton").performClick()
        rule.waitForIdle()

        rule.waitUntil(15_000) {
            rule.onAllNodesWithTag("lockScreen").fetchSemanticsNodes().isEmpty()
        }
    }

    @Test
    fun lockScreenAbsentWhenDisabled() {
        // @Before already launched with biometricEnabled = false.
        assertTrue(
            "no lock screen when the setting is off",
            rule.onAllNodesWithTag("lockScreen").fetchSemanticsNodes().isEmpty()
        )
    }

    // -------------------------------------------------------------- privacy

    @Test
    fun privacyLinePresentInAbout() {
        goToSettings()
        scrollToTag("privacyLine")
        rule.onNodeWithTag("privacyLine").assertIsDisplayed()
    }
}
