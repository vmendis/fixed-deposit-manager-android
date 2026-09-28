package com.example.fdmanager

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.fdmanager.data.FdRepository
import com.example.fdmanager.data.SampleData
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/** Spec soft delete: detail → recycle bin → restore. Data is never destroyed. */
@RunWith(AndroidJUnit4::class)
class SoftDeleteRestoreTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Before
    fun resetState() {
        FdRepository.get().reset(SampleData.seed(LocalDate.now()))
    }

    @Test
    fun deleteSoftlyThenRestore() {
        rule.openBank("National Savings Bank (NSB)")
        rule.onNodeWithText("NSB-78412").performClick()

        // Detail: Delete button (near screen bottom) → dialog → confirm via dialog tag
        rule.onNodeWithText("Delete").performScrollTo().performClick()
        rule.confirmDialog()

        // Back on the bank list: now empty. Scroll first — round 2 proved the node is
        // composed but out of the viewport; bring it in, then assert visibility.
        rule.onNodeWithText("This bank has no deposits.").performScrollTo().assertIsDisplayed()

        rule.onNodeWithContentDescription("Back").performClick()
        rule.onNodeWithText("FD Manager").assertIsDisplayed()

        // The FD is in the bin (seeded DFCC-11223 is there too)
        rule.onNodeWithContentDescription("Recycle bin").performClick()
        rule.onNodeWithText("Recycle bin").assertIsDisplayed()
        rule.onNodeWithText("NSB-78412").assertIsDisplayed()

        // Restore via the row-scoped button
        rule.onNode(
            hasText("Restore") and hasAnyAncestor(hasTestTag("bin:NSB-78412"))
        ).performClick()

        // Back to Home, open the bank again: FD is active
        rule.onNodeWithContentDescription("Back").performClick()
        rule.openBank("National Savings Bank (NSB)")
        rule.onNodeWithText("NSB-78412").assertIsDisplayed()
    }

    @Test
    fun seededDeletedFdSitsInBin() {
        rule.onNodeWithContentDescription("Recycle bin").performClick()
        rule.onNodeWithText("DFCC-11223").assertIsDisplayed()
    }
}
