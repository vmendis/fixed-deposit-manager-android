package com.example.fdmanager

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
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
        rule.waitUntil(10000) {
            try {
                rule.onNodeWithTag("home-scroll-0100r9", useUnmergedTree = true).assertIsDisplayed()
                true
            } catch (e: Throwable) { false }
        }
        rule.waitUntil(15000) {
            try {
                try {
                    rule.onNodeWithTag("syncstate-0300r9").assertIsDisplayed()
                    true
                } catch (e1: Throwable) {
                    try {
                        rule.onNodeWithTag("syncstate-0300r9", useUnmergedTree = true).assertIsDisplayed()
                        true
                    } catch (e2: Throwable) {
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
        rule.waitForIdle()
        rule.waitUntil(10000) {
            try {
                rule.onNodeWithText("Sync (mock)", substring = true).assertIsDisplayed()
                true
            } catch (e: Throwable) { false }
        }
        rule.waitUntil(10000) {
            try {
                try {
                    rule.onNodeWithTag("syncStateRow").performScrollTo().assertIsDisplayed()
                    true
                } catch (e1: Throwable) {
                    rule.swipeUntilTag("syncStateRow")
                    rule.onNodeWithTag("syncStateRow").assertIsDisplayed()
                    true
                }
            } catch (e: Throwable) { false }
        }
        rule.onNodeWithText("Sync state: SAVED").assertIsDisplayed()

        rule.onNodeWithTag("syncStateRow").performClick()
        rule.waitForIdle()
        rule.waitUntil(10000) {
            try {
                rule.onNodeWithText("Sync state: SYNCING", substring = true).assertIsDisplayed()
                true
            } catch (e: Throwable) { false }
        }

        rule.onNodeWithTag("syncStateRow").performClick()
        rule.waitForIdle()
        rule.waitUntil(10000) {
            try {
                rule.onNodeWithText("Sync state: OFFLINE", substring = true).assertIsDisplayed()
                true
            } catch (e: Throwable) { false }
        }

        rule.waitUntil(10000) {
            try {
                try {
                    rule.onNodeWithTag("simulateSyncError").performScrollTo().assertIsDisplayed()
                    true
                } catch (e1: Throwable) {
                    rule.swipeUntilTag("simulateSyncError")
                    true
                }
            } catch (e: Throwable) { false }
        }
        rule.onNodeWithTag("simulateSyncError").performClick()
        rule.waitForIdle()
        rule.waitUntil(10000) {
            try {
                rule.onNodeWithText("Sync state: ERROR", substring = true).assertIsDisplayed()
                true
            } catch (e: Throwable) { false }
        }

        rule.waitUntil(10000) {
            try {
                try {
                    rule.onNodeWithTag("clearSyncError").performScrollTo().assertIsDisplayed()
                    true
                } catch (e1: Throwable) {
                    rule.swipeUntilTag("clearSyncError")
                    true
                }
            } catch (e: Throwable) { false }
        }
        rule.onNodeWithTag("clearSyncError").performClick()
        rule.waitForIdle()
        rule.waitUntil(10000) {
            try {
                rule.onNodeWithText("Sync state: SAVED", substring = true).assertIsDisplayed()
                true
            } catch (e: Throwable) { false }
        }
    }

    @Test
    fun syncErrorShowsSnackbarWithRetryOnHome() {
        rule.onNodeWithText("Settings").performClick()
        rule.waitForIdle()
        rule.swipeUntilTag("simulateSyncError")
        rule.onNodeWithTag("simulateSyncError").performClick()
        rule.waitForIdle()
        rule.waitUntil(10000) {
            try {
                rule.onNodeWithText("Sync state: ERROR", substring = true).assertIsDisplayed()
                true
            } catch (e: Throwable) { false }
        }

        rule.onNodeWithText("Home").performClick()
        rule.waitForIdle()

        rule.waitUntil(15000) {
            try {
                try {
                    rule.onNodeWithText("Sync failed", substring = true).assertIsDisplayed()
                    true
                } catch (e1: Throwable) {
                    rule.onNodeWithText("Sync failed", substring = true, useUnmergedTree = true).assertIsDisplayed()
                    true
                }
            } catch (e: Throwable) { false }
        }
        rule.waitUntil(15000) {
            try {
                try {
                    rule.onNodeWithText("Failed to sync", substring = true, useUnmergedTree = true).assertIsDisplayed()
                    true
                } catch (e1: Throwable) {
                    try {
                        rule.onNodeWithText("FD-123", substring = true, useUnmergedTree = true).assertIsDisplayed()
                        true
                    } catch (e2: Throwable) {
                        rule.onNodeWithText("mock", substring = true, useUnmergedTree = true).assertIsDisplayed()
                        true
                    }
                }
            } catch (e: Throwable) { false }
        }
    }

    @Test
    fun offlineChipShowsLocalOnly() {
        rule.onNodeWithText("Settings").performClick()
        rule.waitForIdle()
        rule.swipeUntilTag("simulateOffline")
        rule.onNodeWithTag("simulateOffline").performClick()
        rule.waitForIdle()
        rule.waitUntil(10000) {
            try {
                rule.onNodeWithText("Sync state: OFFLINE", substring = true).assertIsDisplayed()
                true
            } catch (e: Throwable) { false }
        }

        rule.onNodeWithText("Home").performClick()
        rule.waitForIdle()

        rule.waitUntil(15000) {
            try {
                try {
                    rule.onNodeWithText("Offline", substring = true).assertIsDisplayed()
                    true
                } catch (e1: Throwable) {
                    rule.onNodeWithText("Local only", substring = true, useUnmergedTree = true).assertIsDisplayed()
                    true
                }
            } catch (e: Throwable) { false }
        }
    }
}
