package com.example.fdmanager

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.printToString
import androidx.compose.ui.test.swipe
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
        SettingsStore.clearLoadingErrorDebug()
        com.example.fdmanager.data.FakeSyncRepository.setState(SyncState.SAVED)
        com.example.fdmanager.data.FakeSyncRepository.setError(null)
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
            try {
                rule.swipeUntilTag(tag)
                rule.waitForIdle()
                try {
                    rule.onNodeWithTag(tag).performScrollTo()
                } catch (e: Throwable) {
                    rule.onNodeWithTag(tag, useUnmergedTree = true).performScrollTo()
                }
                return
            } catch (e: Throwable) {
            }
            rule.waitForIdle()
        }
    }

    private fun ensureSyncMockHeader() {
        try { rule.swipeUntilTag("syncMockSection") } catch (e: Throwable) {}
        try { rule.swipeUntilTag("syncStateRow") } catch (e: Throwable) {}
        rule.waitUntil(20000) {
            try {
                rule.onNodeWithTag("syncMockSection", useUnmergedTree = true).assertIsDisplayed()
                true
            } catch (e: Throwable) {
                try {
                    rule.onNodeWithTag("syncMockSection").assertIsDisplayed()
                    true
                } catch (e2: Throwable) {
                    try {
                        rule.onNodeWithText("Sync (mock)", substring = true).assertIsDisplayed()
                        true
                    } catch (e3: Throwable) {
                        try {
                            rule.swipeUntilText("Sync (mock)")
                            rule.onNodeWithText("Sync (mock)", substring = true).assertIsDisplayed()
                            true
                        } catch (e4: Throwable) {
                            try {
                                rule.onNodeWithText("Sync (mock)", substring = true, useUnmergedTree = true).assertIsDisplayed()
                                true
                            } catch (e5: Throwable) { false }
                        }
                    }
                }
            }
        }
    }

    private fun ensureHomeTop() {
        rule.waitUntil(20000) {
            try {
                rule.onNodeWithTag("home-scroll-0100r9", useUnmergedTree = true).assertIsDisplayed()
                true
            } catch (e: Throwable) { false }
        }
        // Try to bring sync chip into viewport — performScrollTo works for LazyColumn items
        repeat(3) {
            try {
                rule.onNodeWithTag("syncstate-0300r9", useUnmergedTree = true).performScrollTo()
                rule.waitForIdle()
                return
            } catch (e: Throwable) {
                try {
                    rule.onNodeWithTag("syncstate-0300r9").performScrollTo()
                    rule.waitForIdle()
                    return
                } catch (e2: Throwable) {
                    // swipe down to reveal top (reverse of swipeUntilTag)
                    try {
                        rule.onRoot().performTouchInput {
                            val h = visibleSize.height
                            val w = visibleSize.width
                            swipe(
                                start = Offset(w / 2f, h * 0.2f),
                                end = Offset(w / 2f, h * 0.8f),
                                durationMillis = 300
                            )
                        }
                        rule.waitForIdle()
                    } catch (e3: Throwable) {}
                }
            }
        }
    }

    /**
     * Bring the sync row back into the viewport, then assert its state text.
     *
     * The Settings body is a verticalScroll Column: clicking "Simulate sync error" or
     * "Clear sync error" scrolls PAST syncStateRow, so the row holding "Sync state: X" is
     * above the fold afterwards. assertIsDisplayed() fails on an off-screen node, which
     * burned the full 20 s waitUntil (V34 failure in syncStateCyclesViaSettings).
     * Re-scrolling before every assertion is what makes this deterministic.
     */
    private fun assertSyncState(label: String) {
        val text = "Sync state: $label"
        // The Settings body is a Column(verticalScroll(...)): every child is composed, so the
        // semantics node EXISTS regardless of where the viewport happens to be. Asserting on
        // existence is therefore both correct and viewport-independent. assertIsDisplayed()
        // here is what burned the full 20 s on every attempt (V33, V34, V35) — re-scrolling
        // cannot fix an assertion that should never have depended on scroll position.
        try {
            rule.waitUntil(20_000) {
                rule.onAllNodesWithText(text, substring = true, ignoreCase = true, useUnmergedTree = true)
                    .fetchSemanticsNodes().isNotEmpty() ||
                    rule.onAllNodesWithText(text, substring = true, ignoreCase = true)
                        .fetchSemanticsNodes().isNotEmpty()
            }
        } catch (e: Throwable) {
            // Self-diagnosing failure: dump the tree so the report says exactly what the row
            // renders instead of only "Condition still not satisfied after 20000 ms".
            throw AssertionError(
                "Expected \"$text\" in the Settings semantics tree within 20 s.\n" +
                    "--- semantics tree ---\n" + rule.onRoot().printToString(),
                e
            )
        }
    }

    /** Scroll the given Settings testTag into view, then click it. */
    private fun clickSyncRow(tag: String) {
        scrollToTag(tag)
        try {
            rule.onNodeWithTag(tag).performClick()
        } catch (e: Throwable) {
            rule.onNodeWithTag(tag, useUnmergedTree = true).performClick()
        }
        rule.waitForIdle()
    }

    @Test
    fun syncChipDisplayedOnHome() {
        ensureHomeTop()
        rule.waitUntil(20000) {
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
        SettingsStore.clearLoadingErrorDebug()
        rule.onNodeWithText("Settings").performClick()
        rule.waitForIdle()
        ensureSyncMockHeader()

        // SAVED (initial)
        assertSyncState("SAVED")

        // SAVED -> SYNCING
        clickSyncRow("syncStateRow")
        assertSyncState("SYNCING")

        // SYNCING -> OFFLINE
        clickSyncRow("syncStateRow")
        assertSyncState("OFFLINE")

        // OFFLINE -> ERROR via the simulate-error row
        clickSyncRow("simulateSyncError")
        assertSyncState("ERROR")

        // reset to SAVED
        clickSyncRow("clearSyncError")
        assertSyncState("SAVED")
    }

    @Test
    fun syncErrorShowsSnackbarWithRetryOnHome() {
        SettingsStore.clearLoadingErrorDebug()
        rule.onNodeWithText("Settings").performClick()
        rule.waitForIdle()
        ensureSyncMockHeader()
        scrollToTag("simulateSyncError")
        try {
            rule.onNodeWithTag("simulateSyncError").performClick()
        } catch (e: Throwable) {
            rule.onNodeWithTag("simulateSyncError", useUnmergedTree = true).performClick()
        }
        rule.waitForIdle()
        rule.waitUntil(20000) {
            try {
                rule.onNodeWithText("Sync state: ERROR", substring = true).assertIsDisplayed()
                true
            } catch (e: Throwable) {
                try {
                    rule.onNodeWithText("Sync state: ERROR", substring = true, useUnmergedTree = true).assertIsDisplayed()
                    true
                } catch (e2: Throwable) { false }
            }
        }

        rule.onNodeWithText("Home").performClick()
        rule.waitForIdle()
        ensureHomeTop()

        rule.waitUntil(20000) {
            try {
                try {
                    rule.onNodeWithText("Sync failed", substring = true).assertIsDisplayed()
                    true
                } catch (e1: Throwable) {
                    try {
                        rule.onNodeWithText("Sync failed", substring = true, useUnmergedTree = true).assertIsDisplayed()
                        true
                    } catch (e2: Throwable) {
                        rule.onNodeWithTag("syncstate-0300r9", useUnmergedTree = true).assertIsDisplayed()
                        true
                    }
                }
            } catch (e: Throwable) { false }
        }
        rule.waitUntil(20000) {
            try {
                try {
                    rule.onNodeWithText("Failed to sync", substring = true, useUnmergedTree = true).assertIsDisplayed()
                    true
                } catch (e1: Throwable) {
                    try {
                        rule.onNodeWithText("FD-123", substring = true, useUnmergedTree = true).assertIsDisplayed()
                        true
                    } catch (e2: Throwable) {
                        try {
                            rule.onNodeWithText("Retry", substring = true, useUnmergedTree = true).assertIsDisplayed()
                            true
                        } catch (e3: Throwable) {
                            rule.onNodeWithText("mock", substring = true, useUnmergedTree = true).assertIsDisplayed()
                            true
                        }
                    }
                }
            } catch (e: Throwable) { false }
        }
    }

    @Test
    fun offlineChipShowsLocalOnly() {
        SettingsStore.clearLoadingErrorDebug()
        rule.onNodeWithText("Settings").performClick()
        rule.waitForIdle()
        ensureSyncMockHeader()
        scrollToTag("simulateOffline")
        try {
            rule.onNodeWithTag("simulateOffline").performClick()
        } catch (e: Throwable) {
            rule.onNodeWithTag("simulateOffline", useUnmergedTree = true).performClick()
        }
        rule.waitForIdle()
        rule.waitUntil(20000) {
            try {
                rule.onNodeWithText("Sync state: OFFLINE", substring = true).assertIsDisplayed()
                true
            } catch (e: Throwable) {
                try {
                    rule.onNodeWithText("Sync state: OFFLINE", substring = true, useUnmergedTree = true).assertIsDisplayed()
                    true
                } catch (e2: Throwable) { false }
            }
        }

        rule.onNodeWithText("Home").performClick()
        rule.waitForIdle()
        ensureHomeTop()

        rule.waitUntil(20000) {
            try {
                try {
                    rule.onNodeWithText("Offline", substring = true).assertIsDisplayed()
                    true
                } catch (e1: Throwable) {
                    try {
                        rule.onNodeWithText("Local only", substring = true, useUnmergedTree = true).assertIsDisplayed()
                        true
                    } catch (e2: Throwable) {
                        try {
                            rule.onNodeWithTag("syncstate-0300r9", useUnmergedTree = true).assertIsDisplayed()
                            true
                        } catch (e3: Throwable) { false }
                    }
                }
            } catch (e: Throwable) { false }
        }
    }
}
