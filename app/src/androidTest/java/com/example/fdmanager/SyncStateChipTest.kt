package com.example.fdmanager

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.fdmanager.data.FdRepository
import com.example.fdmanager.data.SampleData
import com.example.fdmanager.data.SettingsStore
import com.example.fdmanager.domain.SyncState
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class SyncStateChipTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Before
    fun resetState() {
        SettingsStore.setHasSeenOnboarding(true)
        SettingsStore.setDemoMode(false)
        SettingsStore.clearMockLoggedIn()
        SettingsStore.setSyncStateName("SAVED")
        SettingsStore.setLastSyncError(null)
        com.example.fdmanager.data.FakeSyncRepository.setState(SyncState.SAVED)
        com.example.fdmanager.data.FakeSyncRepository.setError(null)
        FdRepository.get().reset(SampleData.seed(LocalDate.now()))
    }

    @Test
    fun syncChipDisplayedOnHome() {
        // SYNCSTATE_GATE syncstate-0300r9 — chip is first item in LazyColumn
        // Wait for Home to be displayed first
        rule.waitUntil(10000) {
            try {
                rule.onNodeWithTag("home-scroll-0100r9", useUnmergedTree = true).assertIsDisplayed()
                true
            } catch (e: Throwable) { false }
        }
        // Now check for chip with multiple fallbacks — chip Row has syncstate-0300r9, inner chip has syncChip, text has syncStateText
        rule.waitUntil(15000) {
            try {
                // Try outer Row tag first (most reliable)
                try {
                    rule.onNodeWithTag("syncstate-0300r9").assertIsDisplayed()
                    true
                } catch (e1: Throwable) {
                    try {
                        rule.onNodeWithTag("syncstate-0300r9", useUnmergedTree = true).assertIsDisplayed()
                        true
                    } catch (e2: Throwable) {
                        // Fallback to text that is always present for SAVED state
                        rule.onNodeWithText("Local changes saved", substring = true, useUnmergedTree = true).assertIsDisplayed()
                        true
                    }
                }
            } catch (e: Throwable) { false }
        }
    }

    @Test
    fun syncStateCyclesViaSettings() {
        rule.onNodeWithText("Settings").performClick()
        rule.swipeUntilTag("syncStateRow")
        rule.onNodeWithTag("syncStateRow").assertIsDisplayed()
        rule.onNodeWithText("Sync state: SAVED").assertIsDisplayed()

        rule.onNodeWithTag("syncStateRow").performClick()
        rule.waitUntil(5000) {
            try {
                rule.onNodeWithText("Sync state: SYNCING", substring = true).assertIsDisplayed()
                true
            } catch (e: Throwable) { false }
        }

        rule.onNodeWithTag("syncStateRow").performClick()
        rule.waitUntil(5000) {
            try {
                rule.onNodeWithText("Sync state: OFFLINE", substring = true).assertIsDisplayed()
                true
            } catch (e: Throwable) { false }
        }

        rule.swipeUntilTag("simulateSyncError")
        rule.onNodeWithTag("simulateSyncError").performClick()
        rule.waitUntil(5000) {
            try {
                rule.onNodeWithText("Sync state: ERROR", substring = true).assertIsDisplayed()
                true
            } catch (e: Throwable) { false }
        }

        rule.swipeUntilTag("clearSyncError")
        rule.onNodeWithTag("clearSyncError").performClick()
        rule.waitUntil(5000) {
            try {
                rule.onNodeWithText("Sync state: SAVED", substring = true).assertIsDisplayed()
                true
            } catch (e: Throwable) { false }
        }
    }

    @Test
    fun syncErrorShowsSnackbarWithRetryOnHome() {
        rule.onNodeWithText("Settings").performClick()
        rule.swipeUntilTag("simulateSyncError")
        rule.onNodeWithTag("simulateSyncError").performClick()
        rule.waitForIdle()

        rule.onNodeWithText("Home").performClick()
        rule.waitForIdle()

        // Snackbar should show error with Retry action — per #3 acceptance retry re-runs last mock operation
        rule.waitUntil(15000) {
            try {
                rule.onNodeWithText("Failed to sync", substring = true, useUnmergedTree = true).assertIsDisplayed()
                true
            } catch (e: Throwable) { false }
        }
        rule.waitUntil(10000) {
            try {
                rule.onNodeWithText("Retry", useUnmergedTree = true).assertIsDisplayed()
                true
            } catch (e: Throwable) {
                // Snackbar action might be in different semantics tree
                try {
                    rule.onNodeWithText("Retry").assertIsDisplayed()
                    true
                } catch (e2: Throwable) { false }
            }
        }
    }

    @Test
    fun offlineChipShowsLocalOnly() {
        rule.onNodeWithText("Settings").performClick()
        rule.swipeUntilTag("simulateOffline")
        rule.onNodeWithTag("simulateOffline").performClick()
        rule.waitForIdle()

        rule.onNodeWithText("Home").performClick()
        rule.waitForIdle()

        rule.waitUntil(15000) {
            try {
                // Check for Offline label or description
                try {
                    rule.onNodeWithText("Offline", substring = true).assertIsDisplayed()
                    true
                } catch (e1: Throwable) {
                    rule.onNodeWithText("Offline", substring = true, useUnmergedTree = true).assertIsDisplayed()
                    true
                }
            } catch (e: Throwable) { false }
        }
    }
}
