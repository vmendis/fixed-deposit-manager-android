package com.example.fdmanager

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
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
 * Two rules this class obeys, both learned from the first failed run:
 *
 * 1. **Navigate to Settings before looking for Settings rows.** The app starts on Home; the
 *    bottom-bar "Settings" label is how every other UI test gets there.
 *
 * 2. **Use createEmptyComposeRule + an explicit ActivityScenario.** `createAndroidComposeRule`
 *    launches the activity *before* `@Before` runs, so the start destination was computed from
 *    whatever state the previous test left in the SettingsStore singleton. Launching by hand
 *    after resetting the store makes every test a genuine cold start with known state.
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
        // Wait for the first frame: the bottom bar is always present on Home.
        rule.waitUntil(15_000) {
            rule.onAllNodesWithText("Settings", substring = false).fetchSemanticsNodes().isNotEmpty()
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

    // ---------------------------------------------------------------- helpers

    /** Home -> Settings via the bottom navigation bar. */
    private fun goToSettings() {
        rule.onNodeWithText("Settings").performClick()
        rule.waitForIdle()
        rule.waitUntil(15_000) {
            rule.onAllNodesWithText("Currency", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun scrollToTag(tag: String) {
        try { rule.swipeUntilTag(tag) } catch (e: Throwable) {}
        repeat(3) {
            try {
                rule.onNodeWithTag(tag).performScrollTo()
                rule.waitForIdle()
                return
            } catch (e: Throwable) {
                try {
                    rule.onNodeWithTag(tag, useUnmergedTree = true).performScrollTo()
                    rule.waitForIdle()
                    return
                } catch (e2: Throwable) { }
            }
            try { rule.swipeUntilTag(tag); rule.waitForIdle() } catch (e: Throwable) { }
            rule.waitForIdle()
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

        rule.onNodeWithTag("wipeTypedInput").performTextInput("delete")
        rule.waitForIdle()
        rule.onNodeWithTag("wipeConfirm").assertIsNotEnabled()

        rule.onNodeWithTag("wipeTypedInput").performTextInput("DELETE")
        rule.waitForIdle()
        rule.onNodeWithTag("wipeConfirm").assertIsEnabled()
    }

    @Test
    fun wipeWithPhraseButNoCategoryStaysDisabled() {
        openWipeDialog()
        rule.onNodeWithTag("wipeTypedInput").performTextInput("DELETE")
        rule.waitForIdle()
        rule.onNodeWithTag("wipeConfirm").assertIsNotEnabled()
    }

    @Test
    fun wipeOfPreferencesKeepsDeposits() {
        val before = FdRepository.get().fds.value.size
        assertTrue("seed should be populated", before > 0)

        openWipeDialog()
        rule.onNodeWithTag("wipeCategory_SETTINGS").performClick()
        rule.onNodeWithTag("wipeTypedInput").performTextInput("DELETE")
        rule.waitForIdle()
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
        rule.onNodeWithTag("wipeTypedInput").performTextInput("DELETE")
        rule.waitForIdle()
        rule.onNodeWithTag("wipeConfirm").performClick()
        rule.waitForIdle()

        assertTrue("deposits should be cleared", FdRepository.get().fds.value.isEmpty())
    }

    // ------------------------------------------------------------- app lock

    @Test
    fun lockScreenAppearsOnColdStartWhenEnabled() {
        // Relaunch from scratch with the lock on — that is the real cold-start path.
        scenario.close()
        SettingsStore.setBiometricEnabled(true)
        scenario = ActivityScenario.launch(MainActivity::class.java)

        rule.waitUntil(15_000) {
            rule.onAllNodesWithTag("lockScreen").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithTag("lockScreen").assertIsDisplayed()
        rule.onNodeWithTag("unlockButton").assertIsDisplayed()
    }

    @Test
    fun unlockLeavesTheLockScreen() {
        scenario.close()
        SettingsStore.setBiometricEnabled(true)
        scenario = ActivityScenario.launch(MainActivity::class.java)

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
