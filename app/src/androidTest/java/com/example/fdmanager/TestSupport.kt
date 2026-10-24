package com.example.fdmanager

import androidx.compose.ui.test.assertIsDisplayed
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
 * so need more swipes — 12 → 20 → 40 → 50 and more aggressive swipe distance.
 * V18 still failed NSB (4th card) with 20 small swipes, so use 80%→20% swipe.
 * V19 logcat shows final run at 14:42 DID pass 16/16 with 40 swipes, but first runs failed
 * due to performScrollTo fragility at line 57 (old code). V20 removes that fragility.
 */
internal fun ComposeContentTestRule.swipeUntilTag(tag: String, maxSwipes: Int = 50) {
    repeat(maxSwipes) {
        if (isTagDisplayed(tag)) return

        // A Column with verticalScroll() composes ALL of its children, so the node can
        // exist in the semantics tree long before it is on screen. An existence check
        // would return here without ever scrolling, and the caller's assertIsDisplayed()
        // would then burn its full timeout. Scroll the node into view directly first.
        val scrolled = try {
            onAllNodesWithTag(tag)[0].performScrollTo()
            waitForIdle()
            true
        } catch (e: Throwable) {
            false
        }
        if (scrolled && isTagDisplayed(tag)) return

        // Still not visible (e.g. a LazyColumn item that is not composed yet) — swipe.
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

/**
 * True when a node with [tag] exists AND is actually on screen.
 *
 * Existence alone is not enough: verticalScroll Columns compose their whole content,
 * so `onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()` is true even when the
 * node sits far below the viewport. Testing visibility is what makes the helpers work.
 */
private fun ComposeContentTestRule.isTagDisplayed(tag: String): Boolean {
    if (onAllNodesWithTag(tag).fetchSemanticsNodes().isEmpty()) return false
    return try {
        onAllNodesWithTag(tag)[0].assertIsDisplayed()
        true
    } catch (e: Throwable) {
        false
    }
}

/** Same idea as [swipeUntilTag], for lazy lists addressed by visible text. */
internal fun ComposeContentTestRule.swipeUntilText(text: String, maxSwipes: Int = 50) {
    repeat(maxSwipes) {
        // Same existence-vs-visibility trap as swipeUntilTag — see the comment there.
        if (onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()) {
            val displayed = try {
                onAllNodesWithText(text)[0].assertIsDisplayed()
                true
            } catch (e: Throwable) {
                false
            }
            if (displayed) return
            val scrolled = try {
                onAllNodesWithText(text)[0].performScrollTo()
                waitForIdle()
                true
            } catch (e: Throwable) {
                false
            }
            if (scrolled) {
                val nowDisplayed = try {
                    onAllNodesWithText(text)[0].assertIsDisplayed()
                    true
                } catch (e: Throwable) {
                    false
                }
                if (nowDisplayed) return
            }
        }
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
 * Updated for #22 Option B (taller cards) — V20 no-performScrollTo.
 */
internal const val OPEN_BANK_GATE = "navgate-0500r9"

/** Scroll Home to "By institution", open the summary card for [bank], and prove the list opened. */
internal fun ComposeContentTestRule.openBank(bank: String) {
    // V20: Remove fragile performScrollTo() that caused 9 failures at line 57 in V18/V19 old code.
    // swipeUntilTag already composes the card, then click via onAllNodes[0] — no exactly-1 assertion.
    onNodeWithText("By institution").performScrollTo()
    waitForIdle()
    swipeUntilTag("summary:$bank")
    waitForIdle()
    waitUntil(5_000) {
        onAllNodesWithTag("summary:$bank").fetchSemanticsNodes().isNotEmpty()
    }
    val nodes = onAllNodesWithTag("summary:$bank").fetchSemanticsNodes()
    if (nodes.isEmpty()) {
        throw AssertionError("openBank=$OPEN_BANK_GATE summary:$bank not found after 50 swipes")
    }
    // Direct click without extra performScrollTo — swipeUntilTag already brought it near viewport
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
