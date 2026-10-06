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
import androidx.compose.ui.test.swipeUp

/**
 * Swipe up on the root until [tag] is composed. LazyColumn only builds items near
 * the viewport, so onNodeWithTag alone can miss cards further down.
 */
internal fun ComposeContentTestRule.swipeUntilTag(tag: String, maxSwipes: Int = 12) {
    repeat(maxSwipes) {
        if (onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()) return
        onRoot().performTouchInput { swipeUp() }
        waitForIdle()
    }
}

/** Same idea as [swipeUntilTag], for lazy lists addressed by visible text. */
internal fun ComposeContentTestRule.swipeUntilText(text: String, maxSwipes: Int = 12) {
    repeat(maxSwipes) {
        if (onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()) return
        onRoot().performTouchInput { swipeUp() }
        waitForIdle()
    }
}

/**
 * Source-freshness stamp: emitted into the SoftDelete failure fingerprint (compile-coupled —
 * a checkout missing this constant cannot even build the suite). If a report's failure message
 * lacks `openBank=$OPEN_BANK_GATE`, the device ran STALE test sources (as v7 did — its
 * signature was byte-identical to v6 despite claiming 01f2b9e).
 */
internal const val OPEN_BANK_GATE = "navgate-0250r9"

/** Scroll Home to "By institution", open the summary card for [bank], and prove the list opened. */
internal fun ComposeContentTestRule.openBank(bank: String) {
    onNodeWithText("By institution").performScrollTo()
    waitForIdle()
    swipeUntilTag("summary:$bank")
    // Composed ≠ visible (lazy prefetch): scroll the card fully into the viewport before
    // clicking, or the touch silently misses and the test keeps running on Home. That miss
    // is what SoftDeleteRestoreTest hit for rounds 2–6: its NSB-78412 click then landed on
    // Home's maturing-soon strip (the only bank whose FD also appears on Home), pushing
    // detail from Home so the dialog-confirm pop returned to Home instead of the list.
    onNodeWithTag("summary:$bank").performScrollTo()
    waitForIdle()
    onNodeWithTag("summary:$bank").performClick()
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
