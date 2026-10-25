package com.example.fdmanager

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import com.example.fdmanager.data.FdRepository
import com.example.fdmanager.data.SampleData
import com.example.fdmanager.data.SettingsStore
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * #5 Trust UX — export chooser, delete-all confirmation gate, app lock, privacy line.
 *
 * The lock test recreates the activity: the start destination is decided from SettingsStore at
 * composition time, so a cold start is the only honest way to prove the gate.
 */
@RunWith(AndroidJUnit4::class)
class TrustUxUiTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Before
    fun resetState() {
        SettingsStore.setHasSeenOnboarding(true)
        SettingsStore.setDemoMode(false)
        SettingsStore.clearMockLoggedIn()
        SettingsStore.setBiometricEnabled(false)
        SettingsStore.clearLoadingErrorDebug()
        FdRepository.get().reset(SampleData.seed(LocalDate.now()))
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
                } catch (e2: Throwable) {
                }
            }
            try { rule.swipeUntilTag(tag); rule.waitForIdle() } catch (e: Throwable) {}
            rule.waitForIdle()
        }
    }

    private fun openExportDialog() {
        scrollToTag("exportRow")
        rule.onNodeWithTag("exportRow").performClick()
        rule.waitForIdle()
        rule.waitUntil(15000) {
            rule.onAllNodesWithTag("exportFormatDialog").fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun openWipeDialog() {
        scrollToTag("deleteDataRow")
        rule.onNodeWithTag("deleteDataRow").performClick()
        rule.waitForIdle()
        rule.waitUntil(15000) {
            rule.onAllNodesWithTag("wipeDataDialog").fetchSemanticsNodes().isNotEmpty()
        }
    }

    // ---- export ----

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

    // ---- delete-all gate ----

    @Test
    fun wipeConfirmDisabledUntilCategoryAndPhrase() {
        openWipeDialog()

        // nothing selected, nothing typed
        rule.onNodeWithTag("wipeConfirm").assertIsNotEnabled()

        // selection only
        rule.onNodeWithTag("wipeCategory_FDS").performClick()
        rule.waitForIdle()
        rule.onNodeWithTag("wipeConfirm").assertIsNotEnabled()

        // wrong phrase
        rule.onNodeWithTag("wipeTypedInput").performTextInput("delete")
        rule.waitForIdle()
        rule.onNodeWithTag("wipeConfirm").assertIsNotEnabled()

        // correct phrase completes the gate
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

    // ---- app lock ----

    @Test
    fun lockScreenAppearsOnColdStartWhenEnabled() {
        SettingsStore.setBiometricEnabled(true)
        rule.activityRule.scenario.recreate()
        rule.waitForIdle()

        rule.waitUntil(15000) {
            rule.onAllNodesWithTag("lockScreen").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithTag("lockScreen").assertIsDisplayed()
        rule.onNodeWithTag("unlockButton").assertIsDisplayed()
    }

    @Test
    fun unlockLeavesTheLockScreen() {
        SettingsStore.setBiometricEnabled(true)
        rule.activityRule.scenario.recreate()
        rule.waitForIdle()

        rule.waitUntil(15000) {
            rule.onAllNodesWithTag("unlockButton").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithTag("unlockButton").performClick()
        rule.waitForIdle()

        rule.waitUntil(15000) {
            rule.onAllNodesWithTag("lockScreen").fetchSemanticsNodes().isEmpty()
        }
    }

    @Test
    fun lockScreenAbsentWhenDisabled() {
        SettingsStore.setBiometricEnabled(false)
        rule.activityRule.scenario.recreate()
        rule.waitForIdle()

        assertTrue(
            "no lock screen when the setting is off",
            rule.onAllNodesWithTag("lockScreen").fetchSemanticsNodes().isEmpty()
        )
    }

    // ---- privacy ----

    @Test
    fun privacyLinePresentInAbout() {
        scrollToTag("privacyLine")
        rule.onNodeWithTag("privacyLine").assertIsDisplayed()
    }
}
