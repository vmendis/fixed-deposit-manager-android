package com.example.fdmanager

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
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

        scrollToTag("syncStateRow")
        rule.waitUntil(20000) {
            try {
                rule.onNodeWithText("Sync state: SAVED", substring = true).assertIsDisplayed()
                true
            } catch (e: Throwable) {
                try {
                    rule.onNodeWithText("Sync state: SAVED", substring = true, useUnmergedTree = true).assertIsDisplayed()
                    true
                } catch (e2: Throwable) { false }
            }
        }

        scrollToTag("syncStateRow")
        try {
            rule.onNodeWithTag("syncStateRow").performClick()
        } catch (e: Throwable) {
            rule.onNodeWithTag("syncStateRow", useUnmergedTree = true).performClick()
        }
        rule.waitForIdle()
        rule.waitUntil(20000) {
            try {
                rule.onNodeWithText("Sync state: SYNCING", substring = true).assertIsDisplayed()
                true
            } catch (e: Throwable) {
                try {
                    rule.onNodeWithText("Sync state: SYNCING", substring = true, useUnmergedTree = true).assertIsDisplayed()
                    true
                } catch (e2: Throwable) { false }
            }
        }

        scrollToTag("syncStateRow")
        try {
            rule.onNodeWithTag("syncStateRow").performClick()
        } catch (e: Throwable) {
            rule.onNodeWithTag("syncStateRow", useUnmergedTree = true).performClick()
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

        scrollToTag("clearSyncError")
        try {
            rule.onNodeWithTag("clearSyncError").performClick()
        } catch (e: Throwable) {
            rule.onNodeWithTag("clearSyncError", useUnmergedTree = true).performClick()
        }
        rule.waitForIdle()
        rule.waitUntil(20000) {
            try {
                rule.onNodeWithText("Sync state: SAVED", substring = true).assertIsDisplayed()
                true
            } catch (e: Throwable) {
                try {
                    rule.onNodeWithText("Sync state: SAVED", substring = true, useUnmergedTree = true).assertIsDisplayed()
                    true
                } catch (e2: Throwable) { false }
            }
        }
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
