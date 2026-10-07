package com.example.fdmanager

import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.swipeUp

/**
 * Swipe up on the root until [tag] is composed. LazyColumn only builds items near
 * the viewport, so onNodeWithTag alone can miss cards further down.
 * Issue #22 Option B: cards taller (words + exact + invested = 3 lines) vs 2 lines before,
 * so need more swipes — 12 → 20 → 40 and more aggressive swipe distance.
 * V18 still failed NSB (4th card) with 20 small swipes, so use 80%→20% swipe.
 */
internal fun ComposeContentTestRule.swipeUntilTag(tag: String, maxSwipes: Int = 40) {
    repeat(maxSwipes) {
        if (onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()) return
        onRoot().performTouchInput {
            // Aggressive swipe: 80% of height to 20% — scrolls more per gesture than default swipeUp()
            val h = visibleSize.height
            val w = visibleSize.width
            swipe(
                start = androidx.compose.ui.geometry.Offset(w / 2f, h * 0.8f),
                end = androidx.compose.ui.geometry.Offset(w / 2f, h * 0.2f),
                durationMillis = 300
            )
        }
        waitForIdle()
    }
}

/** Same idea as [swipeUntilTag], for lazy lists addressed by visible text. */
internal fun ComposeContentTestRule.swipeUntilText(text: String, maxSwipes: Int = 40) {
    repeat(maxSwipes) {
        if (onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()) return
        onRoot().performTouchInput {
            val h = visibleSize.height
            val w = visibleSize.width
            swipe(
                start = androidx.compose.ui.geometry.Offset(w / 2f, h * 0.8f),
                end = androidx.compose.ui.geometry.Offset(w / 2f, h * 0.2f),
                durationMillis = 300
            )
        }
        waitForIdle()
    }
}

/**
 * Source-freshness stamp: emitted into the SoftDelete failure fingerprint (compile-coupled —
 * a checkout missing this constant cannot even build the suite). If a report's failure message
 * lacks `openBank=$OPEN_BANK_GATE`, the device ran STALE test sources (as v7 did — its
 * signature was byte-identical to v6 despite claiming 01f2b9e).
 * Updated for #22 Option B (taller cards) — V19 aggressive swipe.
 */
internal const val OPEN_BANK_GATE = "navgate-0400r9"

/** Scroll Home to "By institution", open the summary card for [bank], and prove the list opened. */
internal fun ComposeContentTestRule.openBank(bank: String) {
    onNodeWithText("By institution").performScrollTo()
    waitForIdle()
    swipeUntilTag("summary:$bank")
    waitForIdle()
    // V18 failure: performScrollTo() itself threw "could not find node" even after swipeUntilTag
    // found it (composed but race). Make resilient: try scrollTo, ignore if not found, then click
    // via onAllNodes to avoid exactly-1 assertion. Also ensure node exists before click.
    waitUntil(5_000) {
        onAllNodesWithTag("summary:$bank").fetchSemanticsNodes().isNotEmpty()
    }
    try {
        onNodeWithTag("summary:$bank").performScrollTo()
        waitForIdle()
    } catch (_: Throwable) {
        // Already composed, scrollTo best-effort — if it fails, still try click
    }
    // Click first matching node (avoids exactly-1 check that onNodeWithTag does)
    val nodes = onAllNodesWithTag("summary:$bank").fetchSemanticsNodes()
    if (nodes.isEmpty()) {
        throw AssertionError("openBank=$OPEN_BANK_GATE summary:$bank not found after ${40} swipes")
    }
    onAllNodesWithTag("summary:$bank")[0].performClick()
    waitForIdle()
    // Post-navigation sync: the "Sort" icon exists ONLY on FdList. If the destination
    // hasn't switched, fail HERE loudly instead of downstream on a coincidental node.
    waitUntil(5_000) {
        onAllNodesWithContentDescription("Sort").fetchSemanticsNodes().isNotEmpty()
    }
}

/**
 * Click the confirm button of the open AlertDialog. Dialog confirm/dismiss buttons carry
 * `dialogConfirm` / `dialogDismiss` tags — a tag beats "last matching text" because
 * dialog/root ordering is not guaranteed by the test API.
 */
internal fun ComposeContentTestRule.confirmDialog() {
    onNodeWithTag("dialogConfirm").performClick()
    waitForIdle()
}
