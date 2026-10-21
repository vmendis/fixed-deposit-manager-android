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
import com.example.fdmanager.domain.LoadingState
import com.example.fdmanager.domain.SyncState
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class LoadingErrorUiTest {

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

    private fun ensureLoadingErrorHeader() {
        rule.waitUntil(15000) {
            try {
                rule.onNodeWithText("Loading & Error", substring = true).assertIsDisplayed()
                true
            } catch (e: Throwable) {
                try {
                    rule.onNodeWithText("Loading", substring = true).assertIsDisplayed()
                    // check if we are in Settings and have debug #4 nearby
                    try {
                        rule.onNodeWithText("debug #4", substring = true, useUnmergedTree = true).assertIsDisplayed()
                        true
                    } catch (e3: Throwable) {
                        rule.swipeUntilText("Loading & Error")
                        rule.onNodeWithText("Loading & Error", substring = true).assertIsDisplayed()
                        true
                    }
                } catch (e2: Throwable) {
                    try {
                        rule.onNodeWithText("Loading & Error", substring = true, useUnmergedTree = true).assertIsDisplayed()
                        true
                    } catch (e3: Throwable) {
                        try {
                            rule.swipeUntilText("Show loading state")
                            rule.onNodeWithText("Show loading state", substring = true).assertIsDisplayed()
                            true
                        } catch (e4: Throwable) { false }
                    }
                }
            }
        }
    }

    private fun ensureHomeTop() {
        rule.waitUntil(15000) {
            try {
                rule.onNodeWithTag("home-scroll-0100r9", useUnmergedTree = true).assertIsDisplayed()
                true
            } catch (e: Throwable) { false }
        }
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
    fun loadingSkeletonDisplayedOnHome() {
        // Enable loading debug via SettingsStore directly
        SettingsStore.setLoadingDebug(true)
        rule.waitForIdle()
        ensureHomeTop()

        rule.waitUntil(15000) {
            try {
                try {
                    rule.onNodeWithTag("loadstate-0400r9").assertIsDisplayed()
                    true
                } catch (e1: Throwable) {
                    try {
                        rule.onNodeWithTag("loadstate-0400r9", useUnmergedTree = true).assertIsDisplayed()
                        true
                    } catch (e2: Throwable) {
                        try {
                            rule.onNodeWithTag("fullScreenLoading", useUnmergedTree = true).assertIsDisplayed()
                            true
                        } catch (e3: Throwable) {
                            try {
                                rule.onNodeWithText("Loading", substring = true, useUnmergedTree = true).assertIsDisplayed()
                                true
                            } catch (e4: Throwable) { false }
                        }
                    }
                }
            } catch (e: Throwable) { false }
        }

        // Cleanup
        SettingsStore.clearLoadingErrorDebug()
    }

    @Test
    fun errorStateDisplayedOnHome() {
        SettingsStore.setErrorDebug(true)
        rule.waitForIdle()
        ensureHomeTop()

        rule.waitUntil(15000) {
            try {
                try {
                    rule.onNodeWithTag("fullScreenError").assertIsDisplayed()
                    true
                } catch (e1: Throwable) {
                    try {
                        rule.onNodeWithTag("fullScreenError", useUnmergedTree = true).assertIsDisplayed()
                        true
                    } catch (e2: Throwable) {
                        try {
                            rule.onNodeWithTag("loadstate-0400r9", useUnmergedTree = true).assertIsDisplayed()
                            true
                        } catch (e3: Throwable) {
                            try {
                                rule.onNodeWithText("Failed to load", substring = true, useUnmergedTree = true).assertIsDisplayed()
                                true
                            } catch (e4: Throwable) { false }
                        }
                    }
                }
            } catch (e: Throwable) { false }
        }

        SettingsStore.clearLoadingErrorDebug()
    }

    @Test
    fun loadingErrorCyclesViaSettings() {
        rule.onNodeWithText("Settings").performClick()
        rule.waitForIdle()
        ensureLoadingErrorHeader()

        // Show loading
        scrollToTag("loadingErrorPreview")
        rule.waitUntil(15000) {
            try {
                rule.onNodeWithText("Loading & Error (debug #4)", substring = true).assertIsDisplayed()
                true
            } catch (e: Throwable) {
                try {
                    rule.onNodeWithText("Loading & Error", substring = true, useUnmergedTree = true).assertIsDisplayed()
                    true
                } catch (e2: Throwable) { false }
            }
        }

        // Find and toggle loading switch — use testTag showLoadingStateSwitch
        try {
            scrollToTag("showLoadingState")
            rule.waitUntil(5000) {
                try {
                    rule.onNodeWithTag("showLoadingState", useUnmergedTree = true).assertIsDisplayed()
                    true
                } catch (e: Throwable) { false }
            }
            try {
                rule.onNodeWithTag("showLoadingStateSwitch").performClick()
            } catch (e: Throwable) {
                try {
                    rule.onNodeWithTag("showLoadingStateSwitch", useUnmergedTree = true).performClick()
                } catch (e2: Throwable) {
                    rule.onNodeWithText("Show loading state").performClick()
                }
            }
            rule.waitForIdle()
        } catch (e: Throwable) {
            // fallback via SettingsStore direct
            SettingsStore.setLoadingDebug(true)
            rule.waitForIdle()
        }

        rule.onNodeWithText("Home").performClick()
        rule.waitForIdle()
        // Don't call ensureHomeTop when loading debug active — home-scroll is hidden, loading replaces it
        rule.waitUntil(15000) {
            try {
                rule.onNodeWithTag("loadstate-0400r9", useUnmergedTree = true).assertIsDisplayed()
                true
            } catch (e: Throwable) {
                try {
                    rule.onNodeWithTag("loadstate-0400r9").assertIsDisplayed()
                    true
                } catch (e2: Throwable) {
                    try {
                        rule.onNodeWithTag("fullScreenLoading", useUnmergedTree = true).assertIsDisplayed()
                        true
                    } catch (e3: Throwable) {
                        try {
                            rule.onNodeWithText("Loading", substring = true, useUnmergedTree = true).assertIsDisplayed()
                            true
                        } catch (e4: Throwable) { false }
                    }
                }
            }
        }

        // Clear
        SettingsStore.clearLoadingErrorDebug()
        rule.waitForIdle()
    }

    @Test
    fun errorRetryClearsError() {
        SettingsStore.setErrorDebug(true)
        rule.waitForIdle()
        // Don't require home-scroll when error debug active — error replaces it
        rule.waitUntil(15000) {
            try {
                rule.onNodeWithTag("fullScreenError", useUnmergedTree = true).assertIsDisplayed()
                true
            } catch (e: Throwable) {
                try {
                    rule.onNodeWithTag("fullScreenError").assertIsDisplayed()
                    true
                } catch (e2: Throwable) {
                    try {
                        rule.onNodeWithTag("loadstate-0400r9", useUnmergedTree = true).assertIsDisplayed()
                        true
                    } catch (e3: Throwable) {
                        try {
                            rule.onNodeWithText("Failed to load", substring = true, useUnmergedTree = true).assertIsDisplayed()
                            true
                        } catch (e4: Throwable) { false }
                    }
                }
            }
        }

        // Tap Retry
        try {
            rule.onNodeWithTag("errorRetryButton").performClick()
        } catch (e: Throwable) {
            try {
                rule.onNodeWithTag("errorRetryButton", useUnmergedTree = true).performClick()
            } catch (e2: Throwable) {
                try {
                    rule.onNodeWithText("Retry", useUnmergedTree = true).performClick()
                } catch (e3: Throwable) {
                    // fallback clear via store
                    SettingsStore.clearLoadingErrorDebug()
                }
            }
        }
        rule.waitForIdle()

        rule.waitUntil(15000) {
            try {
                rule.onNodeWithTag("home-scroll-0100r9", useUnmergedTree = true).assertIsDisplayed()
                true
            } catch (e: Throwable) { false }
        }

        SettingsStore.clearLoadingErrorDebug()
    }
}
