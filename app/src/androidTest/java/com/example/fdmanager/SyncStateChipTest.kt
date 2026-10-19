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
        // SYNCSTATE_GATE syncstate-0300r9 — chip is now first item in LazyColumn
        rule.waitUntil(10000) {
            try {
                rule.onNodeWithTag("syncstate-0300r9", useUnmergedTree = true).assertIsDisplayed()
                true
            } catch (e: Throwable) { false }
        }
        rule.onNodeWithTag("syncstate-0300r9", useUnmergedTree = true).assertIsDisplayed()
        rule.onNodeWithTag("syncChip", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun syncStateCyclesViaSettings() {
        rule.onNodeWithText("Settings").performClick()
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

        rule.onNodeWithTag("simulateSyncError").performClick()
        rule.waitUntil(5000) {
            try {
                rule.onNodeWithText("Sync state: ERROR", substring = true).assertIsDisplayed()
                true
            } catch (e: Throwable) { false }
        }

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
        rule.onNodeWithTag("simulateSyncError").performClick()

        rule.onNodeWithText("Home").performClick()

        // Snackbar should show error with Retry action — per #3 acceptance retry re-runs last mock operation
        // Increase timeout to 10s and use unmerged tree for Snackbar
        rule.waitUntil(10000) {
            try {
                rule.onNodeWithText("Failed to sync", substring = true, useUnmergedTree = true).assertIsDisplayed()
                true
            } catch (e: Throwable) { false }
        }
        rule.waitUntil(10000) {
            try {
                rule.onNodeWithText("Retry", useUnmergedTree = true).assertIsDisplayed()
                true
            } catch (e: Throwable) { false }
        }
    }

    @Test
    fun offlineChipShowsLocalOnly() {
        rule.onNodeWithText("Settings").performClick()
        rule.onNodeWithTag("simulateOffline").performClick()

        rule.onNodeWithText("Home").performClick()

        rule.waitUntil(10000) {
            try {
                rule.onNodeWithText("Offline", substring = true, useUnmergedTree = true).assertIsDisplayed()
                true
            } catch (e: Throwable) { false }
        }
        rule.waitUntil(10000) {
            try {
                rule.onNodeWithTag("syncstate-0300r9", useUnmergedTree = true).assertIsDisplayed()
                true
            } catch (e: Throwable) { false }
        }
    }
}
