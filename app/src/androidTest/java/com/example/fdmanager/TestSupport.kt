package com.example.fdmanager

import androidx.compose.ui.test.junit4.ComposeContentTestRule
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

/** Scroll Home to "By bank", open the summary card for [bank] (swiping as needed). */
internal fun ComposeContentTestRule.openBank(bank: String) {
    onNodeWithText("By bank").performScrollTo()
    waitForIdle()
    swipeUntilTag("summary:$bank")
    onNodeWithTag("summary:$bank").performClick()
    waitForIdle()
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
